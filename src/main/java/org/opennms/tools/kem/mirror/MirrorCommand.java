/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2018 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2018 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.tools.kem.mirror;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.kohsuke.args4j.Option;
import org.opennms.core.ipc.sink.api.Message;
import org.opennms.core.ipc.sink.model.SinkMessageProtos;
import org.opennms.tools.kem.Command;
import org.opennms.tools.kem.config.KemConfig;
import org.opennms.tools.kem.config.KemConfigDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.google.protobuf.ByteString;

public class MirrorCommand implements Command {

    private static final Logger LOG = LoggerFactory.getLogger(MirrorCommand.class);

    @Option(name = "-c", usage = "yaml configuration", metaVar = "CONFIG")
    private File configFile = Paths.get(System.getProperty("user.home"), ".kem", "config.yaml").toFile();

    private final MetricRegistry metrics = new MetricRegistry();
    private final Meter errors = metrics.meter("errors");

    private List<XmlSinkModuleMirrorer<? extends Message>> mirrorers = new ArrayList<>();

    private Map<XmlSinkModuleMirrorer<? extends Message>, String> lastXmlMessage = new ConcurrentHashMap<>();

    @Override
    public void execute() {
        final KemConfigDao configDao = new KemConfigDao(configFile);
        final KemConfig config = configDao.getConfig();

        if (config.getTraps().isEnabled()) {
            mirrorers.add(new TrapSinkModuleMirrorer(metrics, config.getTraps()));
        }
        if (config.getSyslog().isEnabled()) {
            mirrorers.add(new SyslogSinkModuleMirrorer(metrics, config.getSyslog()));
        }

        if (mirrorers.size() < 1) {
            System.out.println("No modules enabled. Exiting.");
            return;
        }

        final boolean shouldDecodeFromProtobuf = config.getCompatibility().isDecodeFromProtobuf();
        final boolean shouldEncodeInProtobuf = config.getCompatibility().isEncodeInProtobuf();

        final Properties sourceConfiguration = config.getKafka().getEffectiveSourceConfiguration();
        // Specify default (de)serializers for record keys and for record values.
        sourceConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        sourceConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.ByteArray().getClass().getName());

        final Properties targetConfiguration = config.getKafka().getEffectiveTargetConfiguration();
        targetConfiguration.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName());
        targetConfiguration.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getCanonicalName());
        final KafkaProducer<String,byte[]> producer = new KafkaProducer<>(targetConfiguration);

        final StreamsBuilder builder = new StreamsBuilder();
        for (XmlSinkModuleMirrorer<?> mirrorer : mirrorers) {
            final KStream<String, byte[]> inputStream = builder.stream(mirrorer.getSourceTopic());
            final KStream<String, Message> messageStream = inputStream.mapValues((k,payload) -> {
                try {
                    final String xml;
                    if (shouldDecodeFromProtobuf) {
                        SinkMessageProtos.SinkMessage msg = SinkMessageProtos.SinkMessage.parseFrom(payload);
                        xml = msg.getContent().toStringUtf8();
                    } else {
                        xml = new String(payload, StandardCharsets.UTF_8);
                    }

                    final Message m = mirrorer.unmarshal(xml);
                    lastXmlMessage.put(mirrorer, xml);
                    return mirrorer.mapIfNeedsForwarding(m);
                } catch (Exception e) {
                    errors.mark();
                    LOG.error("Failed to parse payload. Skipping record. Value: {}", Arrays.toString(payload), e);
                    return null;
                }
            });
            messageStream.filter((k,m) -> m != null)
                    .foreach((k,m) -> {
                        byte[] payload = mirrorer.marshal(m).getBytes(StandardCharsets.UTF_8);
                        if (shouldEncodeInProtobuf) {
                            final String messageId = UUID.randomUUID().toString();
                            payload = wrapMessageInProto(messageId, payload);
                        }
                        producer.send(new ProducerRecord<>(mirrorer.getTargetTopic(), k, payload), (metadata, exception) -> {
                            if (exception == null) {
                                mirrorer.messagesForwarded.mark(mirrorer.getNumMessagesIn(m));
                            } else {
                                errors.mark();
                                LOG.error("Error writing to topic: {}. Message: {}", mirrorer.getTargetTopic(), m, exception);
                            }
                        });
                    });
        }

        final KafkaStreams streams = new KafkaStreams(builder.build(), sourceConfiguration);
        streams.setUncaughtExceptionHandler((t, e) -> {
            errors.mark();
            LOG.error(String.format("Stream error on thread: %s", t.getName()), e);
        });
        streams.cleanUp();
        streams.start();

        final Slf4jReporter reporter = Slf4jReporter.forRegistry(metrics)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(10, TimeUnit.SECONDS);

        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                lastXmlMessage.forEach((m,xml) -> {
                    LOG.debug("Last message in '{}': {}", m.getSourceTopic(), xml);
                });
            }
        }, TimeUnit.SECONDS.toMillis(10), TimeUnit.SECONDS.toMillis(10));

        // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            streams.close();
            producer.close();
            reporter.close();
            timer.cancel();
        }));
    }

    private byte[] wrapMessageInProto(String messageId, byte[] messageBytes) {
        SinkMessageProtos.SinkMessage sinkMessage = SinkMessageProtos.SinkMessage.newBuilder()
                .setMessageId(messageId)
                .setContent(ByteString.copyFrom(messageBytes))
                .build();
        return sinkMessage.toByteArray();
    }
}