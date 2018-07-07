# Kafka Event Mirrorer

This utility is used to mirror a subset of the events (syslogs & traps) from one Kafka cluster (or topic) to another.

## Running

Compile the project with:

```
mvn clean package
```

Run using:

```
java -XX:+UseG1GC -Xms2g -Xmx2g -jar target/kafka-event-mirrorer-1.0.0-SNAPSHOT-jar-with-dependencies.jar mirror
```

## Notes

If you are restarting the client after a long period of being offline, you can reset the offsets using something like:

```
/opt/kafka_2.11-1.1.0/bin/kafka-consumer-groups.sh --group opennms-kafka-event-mirrorer --topic OpenNMS.Sink.Syslog --reset-offsets --to-latest  --bootstrap-server kafka1:9092
/opt/kafka_2.11-1.1.0/bin/kafka-consumer-groups.sh --group opennms-kafka-event-mirrorer --topic OpenNMS.Sink.Trap --reset-offsets --to-latest  --bootstrap-server kafka1:9092
```
