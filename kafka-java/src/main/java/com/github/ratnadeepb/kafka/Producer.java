package com.github.ratnadeepb.kafka;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Producer class for kafka.
 */
public class Producer {
    private final Logger mLogger = LoggerFactory.getLogger(Producer.class.getName());
    private final KafkaProducer<String, String> mProducer;

    /**
     * Constructors.
     */
    Producer(String bootstrapServer) {
        Properties props = producerProps(bootstrapServer);
        mProducer = new KafkaProducer<>(props);
        mLogger.info("Producer initialized");
    }

    /**
     * Main method.
     */
    public static void main(String[] args) {
        String server = "127.0.0.1:30092";
        String topic = "user_registered";

        Producer producer = new Producer(server);
        try {
            producer.put(topic, "user1", "John");
        } catch (ExecutionException | InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            producer.put(topic, "user2", "Peter");
        } catch (ExecutionException | InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        producer.close();
    }

    public KafkaProducer<String, String> getmProducer() {
        return mProducer;
    }

    public Logger getmLogger() {
        return mLogger;
    }

    private Properties producerProps(String bootstrapServer) {
        String serializer = StringSerializer.class.getName();
        Properties props = new Properties();
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, serializer);
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, serializer);
        return props;
    }

    // Public
    /**
     * Put a message in the Kafka cluster.
     */
    void put(String topic, String key, String value) throws ExecutionException, InterruptedException {
        mLogger.info("Put value:" + value + ", for key:", key);

        ProducerRecord<String, String> record = new ProducerRecord<String, String>(topic, key, value);
        mProducer.send(record, (recordMetadata, e) -> {
            if (e != null) {
                mLogger.error("Error while producing:", e);
                return;
            }

            mLogger.info("Received new meta. Topic: " + recordMetadata.topic()
                    + "; Partition: " + recordMetadata.partition()
                    + "; Offset: " + recordMetadata.offset()
                    + "; Timestamp: " + recordMetadata.timestamp());
            // });
        }).get(); // turn "put" into async operation
    }

    // close the connection to kafka
    void close() {
        mLogger.info("Closing producer's conenction");
        mProducer.close();
    }
}
