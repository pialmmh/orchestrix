# Kafka Cluster - Java Client Usage Guide

## Connection Strings

After deployment, you have two connection strings:

```
Zookeeper Ensemble: 123.200.0.50:2181,123.200.0.117:2181,123.200.0.51:2181
Kafka Bootstrap:    123.200.0.50:9092,123.200.0.117:9092,123.200.0.51:9092
```

## Which Connection String Should You Use?

### ✅ Use KAFKA Bootstrap Servers (Recommended)

**For 99% of applications, use the Kafka broker addresses:**

- **Kafka Producers** - to send messages
- **Kafka Consumers** - to read messages
- **Kafka Streams** - for stream processing
- **Most Admin Operations** - topic management, etc.

### ⚠️ Use Zookeeper (Rare Cases Only)

**Only use Zookeeper connection for:**

- **Legacy applications** that directly interact with Zookeeper
- **Kafka Streams** (if explicitly configured to use Zookeeper metadata)
- **Direct Zookeeper operations** (very rare)

**Note:** Modern Kafka (2.8+) is moving away from Zookeeper dependency for clients.

---

## Java Client Examples

### 1. Kafka Producer (Most Common)

```java
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import java.util.Properties;

public class KafkaProducerExample {

    public static void main(String[] args) {
        // Configuration
        Properties props = new Properties();

        // ✅ USE KAFKA BOOTSTRAP SERVERS
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                  "123.200.0.50:9092,123.200.0.117:9092,123.200.0.51:9092");

        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                  StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                  StringSerializer.class.getName());

        // Optional: Reliability settings
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);

        // Create producer
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {

            // Send message
            ProducerRecord<String, String> record =
                new ProducerRecord<>("my-topic", "key1", "Hello Kafka Cluster!");

            // Synchronous send
            producer.send(record).get();
            System.out.println("Message sent successfully");

            // OR Asynchronous send with callback
            producer.send(record, (metadata, exception) -> {
                if (exception == null) {
                    System.out.printf("Message sent to partition %d at offset %d%n",
                                    metadata.partition(), metadata.offset());
                } else {
                    exception.printStackTrace();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### 2. Kafka Consumer

```java
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import java.time.Duration;
import java.util.*;

public class KafkaConsumerExample {

    public static void main(String[] args) {
        // Configuration
        Properties props = new Properties();

        // ✅ USE KAFKA BOOTSTRAP SERVERS
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                  "123.200.0.50:9092,123.200.0.117:9092,123.200.0.51:9092");

        props.put(ConsumerConfig.GROUP_ID_CONFIG, "my-consumer-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                  StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                  StringDeserializer.class.getName());

        // Optional: Consumer settings
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        // Create consumer
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {

            // Subscribe to topics
            consumer.subscribe(Collections.singletonList("my-topic"));

            // Poll for messages
            while (true) {
                ConsumerRecords<String, String> records =
                    consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, String> record : records) {
                    System.out.printf("Received: key=%s, value=%s, partition=%d, offset=%d%n",
                                    record.key(), record.value(),
                                    record.partition(), record.offset());
                }

                // Manual commit after processing
                consumer.commitSync();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### 3. Kafka Admin Client (Topic Management)

```java
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.config.TopicConfig;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class KafkaAdminExample {

    public static void main(String[] args) {
        // Configuration
        Properties props = new Properties();

        // ✅ USE KAFKA BOOTSTRAP SERVERS
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                  "123.200.0.50:9092,123.200.0.117:9092,123.200.0.51:9092");

        try (AdminClient admin = AdminClient.create(props)) {

            // Create topic
            createTopic(admin, "my-topic", 3, 3);

            // List topics
            listTopics(admin);

            // Describe topic
            describeTopic(admin, "my-topic");

            // Delete topic
            // deleteTopic(admin, "my-topic");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createTopic(AdminClient admin, String topicName,
                                   int partitions, int replicationFactor)
            throws ExecutionException, InterruptedException {

        Map<String, String> configs = new HashMap<>();
        configs.put(TopicConfig.RETENTION_MS_CONFIG, "604800000"); // 7 days
        configs.put(TopicConfig.COMPRESSION_TYPE_CONFIG, "lz4");

        NewTopic newTopic = new NewTopic(topicName, partitions, (short) replicationFactor)
                .configs(configs);

        CreateTopicsResult result = admin.createTopics(Collections.singleton(newTopic));
        result.all().get();

        System.out.println("Topic created: " + topicName);
    }

    private static void listTopics(AdminClient admin)
            throws ExecutionException, InterruptedException {

        ListTopicsResult topics = admin.listTopics();
        Set<String> topicNames = topics.names().get();

        System.out.println("Topics: " + topicNames);
    }

    private static void describeTopic(AdminClient admin, String topicName)
            throws ExecutionException, InterruptedException {

        DescribeTopicsResult result = admin.describeTopics(Collections.singleton(topicName));
        TopicDescription description = result.all().get().get(topicName);

        System.out.println("Topic: " + description.name());
        System.out.println("Partitions: " + description.partitions().size());
        System.out.println("Replication Factor: " +
                         description.partitions().get(0).replicas().size());
    }

    private static void deleteTopic(AdminClient admin, String topicName)
            throws ExecutionException, InterruptedException {

        DeleteTopicsResult result = admin.deleteTopics(Collections.singleton(topicName));
        result.all().get();

        System.out.println("Topic deleted: " + topicName);
    }
}
```

### 4. Kafka Streams Application

```java
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import java.util.Properties;

public class KafkaStreamsExample {

    public static void main(String[] args) {
        // Configuration
        Properties props = new Properties();

        // ✅ USE KAFKA BOOTSTRAP SERVERS
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,
                  "123.200.0.50:9092,123.200.0.117:9092,123.200.0.51:9092");

        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "my-streams-app");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG,
                  Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,
                  Serdes.String().getClass());

        // Optional: Processing settings
        props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 2);
        props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, 3);

        // Build topology
        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, String> sourceStream = builder.stream("input-topic");

        KStream<String, String> processedStream = sourceStream
            .filter((key, value) -> value != null && value.length() > 0)
            .mapValues(value -> value.toUpperCase());

        processedStream.to("output-topic");

        // Build and start
        Topology topology = builder.build();
        KafkaStreams streams = new KafkaStreams(topology, props);

        // Cleanup on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

        // Start processing
        streams.start();

        System.out.println("Kafka Streams application started");
    }
}
```

---

## Maven Dependencies

Add to your `pom.xml`:

```xml
<dependencies>
    <!-- Kafka Clients -->
    <dependency>
        <groupId>org.apache.kafka</groupId>
        <artifactId>kafka-clients</artifactId>
        <version>3.6.0</version>
    </dependency>

    <!-- Kafka Streams (if needed) -->
    <dependency>
        <groupId>org.apache.kafka</groupId>
        <artifactId>kafka-streams</artifactId>
        <version>3.6.0</version>
    </dependency>

    <!-- SLF4J for logging -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-simple</artifactId>
        <version>2.0.9</version>
    </dependency>
</dependencies>
```

---

## Important Configuration Tips

### 1. Connection String Format

Always use **all broker addresses** for high availability:

```java
// ✅ GOOD - All brokers listed
"123.200.0.50:9092,123.200.0.117:9092,123.200.0.51:9092"

// ❌ BAD - Only one broker
"123.200.0.50:9092"
```

Even if you list only one broker, Kafka will discover the others, but listing all provides better resilience during initial connection.

### 2. Producer Reliability Settings

For critical data:

```java
props.put(ProducerConfig.ACKS_CONFIG, "all");  // All replicas must acknowledge
props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);  // Ordering
props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);  // Exactly-once
```

### 3. Consumer Group Management

Each consumer instance in the same group should have:
- **Same `group.id`** - for load balancing
- **Different `client.id`** (optional) - for monitoring

```java
props.put(ConsumerConfig.GROUP_ID_CONFIG, "my-app-consumer-group");
props.put(ConsumerConfig.CLIENT_ID_CONFIG, "consumer-instance-1");
```

### 4. Topic Replication

When creating topics, use replication factor of **3** (or at least 2) for fault tolerance:

```java
NewTopic newTopic = new NewTopic("my-topic",
                                 3,    // partitions
                                 (short) 3);  // replication factor
```

---

## Testing Your Connection

Simple connectivity test:

```java
import org.apache.kafka.clients.admin.*;
import java.util.Properties;

public class ConnectionTest {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                  "123.200.0.50:9092,123.200.0.117:9092,123.200.0.51:9092");
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);

        try (AdminClient admin = AdminClient.create(props)) {
            System.out.println("Cluster ID: " +
                             admin.describeCluster().clusterId().get());
            System.out.println("Broker count: " +
                             admin.describeCluster().nodes().get().size());
            System.out.println("✅ Successfully connected to Kafka cluster!");
        } catch (Exception e) {
            System.err.println("❌ Failed to connect: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

---

## Summary

### Use Kafka Bootstrap Servers For:
✅ **Producers** - Sending messages
✅ **Consumers** - Reading messages
✅ **Streams** - Stream processing
✅ **Admin** - Topic management

### Connection String:
```
123.200.0.50:9092,123.200.0.117:9092,123.200.0.51:9092
```

### Do NOT Use Zookeeper String For:
❌ Regular application development
❌ Producers/Consumers
❌ Modern Kafka clients

The Zookeeper connection string is mainly for internal Kafka operations and legacy integrations.
