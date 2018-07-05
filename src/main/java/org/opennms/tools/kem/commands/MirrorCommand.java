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

package org.opennms.tools.kem.commands;

import java.io.File;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.collections4.Trie;
import org.apache.commons.lang.StringUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.kohsuke.args4j.Option;
import org.opennms.core.xml.XmlHandler;
import org.opennms.netmgt.trapd.TrapDTO;
import org.opennms.netmgt.trapd.TrapLogDTO;
import org.opennms.tools.kem.config.KemConfig;
import org.opennms.tools.kem.config.KemConfigDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MirrorCommand implements Command {

    private static final Logger LOG = LoggerFactory.getLogger(MirrorCommand.class);

    @Option(name = "-c", usage = "yaml configuration", metaVar = "CONFIG")
    private File configFile = new File("~/.kem/config.yaml");

    private KemConfig config;
    private String[] trapTypeOidPrefixes;

    /**
     * Unmarshalers are not thread-safe.
     */
    private final ThreadLocal<XmlHandler<TrapLogDTO>> trapLogXmlHandler = new ThreadLocal<>();

    private TrapLogDTO shouldForward(TrapLogDTO trapLogDTO) {
        // Filter the traps
        final List<TrapDTO> trapsToForward = trapLogDTO.getMessages().stream()
                // TODO: Optimize this using a radix tree or trie
                .filter(t -> StringUtils.startsWithAny(t.getTrapIdentity().getEnterpriseId(), trapTypeOidPrefixes))
                .collect(Collectors.toList());

        // Nothing to forward?
        if (trapsToForward.size() < 1) {
            return null;
        }

        // Rebuild the log
        final TrapLogDTO filteredTrapLogDTO = new TrapLogDTO();
        filteredTrapLogDTO.setLocation(trapLogDTO.getLocation());
        filteredTrapLogDTO.setSystemId(trapLogDTO.getSystemId());
        filteredTrapLogDTO.setTrapAddress(trapLogDTO.getTrapAddress());
        filteredTrapLogDTO.setMessages(trapsToForward);

        if (LOG.isInfoEnabled()) {
            final String traps = trapLogDTO.getMessages().stream()
                    .map(m -> String.format("Trap[enterprise: %s, generic: %s, specific: %s]",
                            m.getTrapIdentity().getEnterpriseId(), m.getTrapIdentity().getGeneric(), m.getTrapIdentity().getSpecific()))
                    .collect(Collectors.joining(","));
            LOG.info("Forwarding trap log from {}: {}", trapLogDTO.getTrapAddress(), traps);
        }

        return filteredTrapLogDTO;
    }

    @Override
    public void execute() {
        final KemConfigDao configDao = new KemConfigDao(configFile);
        config = configDao.getConfig();
        trapTypeOidPrefixes = config.getTraps().getTrapTypeOidPrefix().toArray(new String[0]);

        final Properties sourceConfiguration = config.getKafka().getEffectiveSourceConfiguration();
        // Specify default (de)serializers for record keys and for record values.
        sourceConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        sourceConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        final Properties targetConfiguration = config.getKafka().getEffectiveTargetConfiguration();
        targetConfiguration.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName());
        targetConfiguration.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName());
        final KafkaProducer<String,String> producer = new KafkaProducer<>(targetConfiguration);

        final StreamsBuilder builder = new StreamsBuilder();
        final KStream<String, String> trapXmlStream = builder.stream(config.getTraps().getSourceTopic());
        final KStream<String, TrapLogDTO> trapLogDtoStream = trapXmlStream.mapValues((k,xml) -> {
            try {
                return shouldForward(getTrapLogXmlHandler().unmarshal(xml));
            } catch (Exception e) {
                LOG.error("Failed to unmarshal XML. Skipping record. Value: {}", xml, e);
                return null;
            }
        });
        trapLogDtoStream.filter((k,log) -> log != null)
                .mapValues((k,log) -> getTrapLogXmlHandler().marshal(log))
                .foreach((k,xml) -> producer.send(new ProducerRecord<>(config.getTraps().getTargetTopic(), k, xml)));

        final KafkaStreams streams = new KafkaStreams(builder.build(), sourceConfiguration);
        streams.cleanUp();
        streams.start();

        // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    private XmlHandler<TrapLogDTO> getTrapLogXmlHandler() {
        XmlHandler<TrapLogDTO> xmlHandler = trapLogXmlHandler.get();
        if (xmlHandler == null) {
            xmlHandler = createXmlHandler(TrapLogDTO.class);
            trapLogXmlHandler.set(xmlHandler);
        }
        return xmlHandler;
    }

    private <W> XmlHandler<W> createXmlHandler(Class<W> clazz) {
        try {
            return new XmlHandler<>(clazz);
        } catch (Throwable t) {
            // NMS-8793: This is a work-around for some failure in the Minion container
            // When invoked for the first time, the creation may fail due to
            // errors of the form "invalid protocol handler: mvn", but subsequent
            // calls always seem to work
            LOG.warn("Creating the XmlHandler failed. Retrying.", t);
            return new XmlHandler<>(clazz);
        }
    }

}