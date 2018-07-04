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

package org.opennms.tools.kem;

import java.util.Properties;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.opennms.netmgt.syslogd.api.SyslogMessageLogDTO;
import org.opennms.netmgt.trapd.TrapLogDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        final String bootstrapServers = args.length > 0 ? args[0] : "localhost:9092";
        final Properties streamsConfiguration = new Properties();
        // Give the Streams application a unique name.  The name must be unique in the Kafka cluster
        // against which the application is run.
        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "opennms-kafka-event-mirrorer");
        streamsConfiguration.put(StreamsConfig.CLIENT_ID_CONFIG, "opennms-kafka-event-mirrorer-client2");
        // Where to find Kafka broker(s).
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // Specify default (de)serializers for record keys and for record values.
        streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.ByteArray().getClass().getName());

        TrapLogDTO trapLogDTO;
        SyslogMessageLogDTO syslogMessageLogDTO;

        /*

        final CollectionSetMapper mapper = new CollectionSetMapper();

        final StreamsBuilder builder = new StreamsBuilder();
        final KStream<String, byte[]> collectionSets = builder.stream("OpenNMS.");
        final KStream<String, List<Point>> points = collectionSets.mapValues((key, bytes) -> {
            try {
                final CollectionSetProtos.CollectionSet collectionSet = CollectionSetProtos.CollectionSet.parseFrom(bytes);

                return mapper.toPoints(collectionSet);
            } catch (InvalidProtocolBufferException e) {
                LOG.error("Failed to decode collection set from byte array: {}", Arrays.toString(bytes), e);
                return null;
            }
        });
        points.filter((k,v) -> v != null && v.size() > 0)
                .foreach((k,v) -> {
                    LOG.info("Writing {} points.", v.size());
                    BatchPoints batchPoints = BatchPoints
                            .database(dbName)
                            .retentionPolicy(rpName)
                            .consistency(InfluxDB.ConsistencyLevel.ALL)
                            .build();

                    v.forEach(batchPoints::point);
                    influxDB.write(batchPoints);
                    LOG.info("Wrote {} points.", v.size());
                });

        final KafkaStreams streams = new KafkaStreams(builder.build(), streamsConfiguration);
        streams.cleanUp();
        streams.start();

        // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
        */
    }
}
