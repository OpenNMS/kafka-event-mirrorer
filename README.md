# Kafka Event Mirrorer [![CircleCI](https://circleci.com/gh/OpenNMS/kafka-event-mirrorer.svg?style=svg)](https://circleci.com/gh/OpenNMS/kafka-event-mirrorer)

This utility is used to mirror a subset of the events (syslogs & traps) from one Kafka cluster (or topic) to another.

## Building

Compile the project with:

```
mvn clean package
```

## Creating Packages

First, build using `mvn clean package` as noted above.  Then:

### Debian

```
cd target
tar -xvzf kafka-event-mirrorer-1.0.0-SNAPSHOT.tar.gz
cd kafka-event-mirrorer-1.0.0-SNAPSHOT
dpkg-buildpackage
```

### RPM

```
rpmbuild -tb target/kafka-event-mirrorer-1.0.0-SNAPSHOT.tar.gz
```

## Running

Run using:

```
java -XX:+UseG1GC -Xms2g -Xmx2g -jar target/kafka-event-mirrorer-1.0.0-SNAPSHOT-jar-with-dependencies.jar mirror
```

### Configure logging

Logging configuration can be modified by passing a reference to an external logback confile file using a system property:

```
java -Dlogback.configurationFile=overridingConfigFile.xml -jar kafka-event-mirrorer-1.0.0-SNAPSHOT-jar-with-dependencies.jar mirror
```

An example config looks as follows:

```
<configuration>
<appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
<encoder>
<pattern>%d

{HH:mm:ss.SSS}
[%thread] %-5level %logger

{36}
- %msg%n</pattern>
</encoder>
</appender>

<root level="debug">
<appender-ref ref="STDOUT" />
</root>
</configuration>
```

## Notes

If you are restarting the client after a long period of being offline, you can reset the offsets using something like:

```
/opt/kafka_2.11-1.1.0/bin/kafka-consumer-groups.sh --group opennms-kafka-event-mirrorer --topic OpenNMS.Sink.Syslog --reset-offsets --to-latest  --bootstrap-server kafka1:9092
/opt/kafka_2.11-1.1.0/bin/kafka-consumer-groups.sh --group opennms-kafka-event-mirrorer --topic OpenNMS.Sink.Trap --reset-offsets --to-latest  --bootstrap-server kafka1:9092
```
