package com.github.ratnadeepb.kafka;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consumer class.
 */
public class Consumer {
    private final Logger mLogger = LoggerFactory.getLogger(Consumer.class.getName());
    private final String mBootstrapServer;
    private final String mGroupId;
    private final String mTopic;

    /**
     * Consumer.
     */
    Consumer(String bootstrapServer, String groupId, String topic) {
        mBootstrapServer = bootstrapServer;
        mGroupId = groupId;
        mTopic = topic;
    }

    /**
     * Main func.
     */
    public static void main(String[] args) {
        String server = "localhost:30092";
        String groupId = "some_app";
        String topic = "user_registered";

        new Consumer(server, groupId, topic).run();
    }

    // Inner class
    private class ConsumerRunnable implements Runnable {
        private CountDownLatch mLatch;
        private KafkaConsumer<String, String> mConsumer;

        ConsumerRunnable(String bootstrapServer, String groupId, String topic, CountDownLatch latch) {
            mLatch = latch;

            Properties props = consumerProps(bootstrapServer, groupId);
            mConsumer = new KafkaConsumer<>(props);
            mConsumer.subscribe(Collections.singletonList(topic));
        }

        @Override
        public void run() {
            final int interval = 100;
            try {
                while (true) {
                    ConsumerRecords<String, String> records = mConsumer.poll(Duration.ofMillis(interval));

                    for (ConsumerRecord<String, String> record : records) {
                        mLogger.info("Key: " + record.key() + ", Value: " + record.value());
                        mLogger.info("Partition: " + record.partition() + ", Offset: " + record.offset());
                    }
                }
            } catch (WakeupException e) {
                mLogger.info("Received shutdown signal!");
            } finally {
                mConsumer.close();
                mLatch.countDown();
            }
        }

        void shutdown() {
            mConsumer.wakeup();
        }

        private Properties consumerProps(String bootstrapServer, String groupId) {
            String deserializer = StringDeserializer.class.getName();
            Properties props = new Properties();
            props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
            props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, deserializer);
            props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, deserializer);
            props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            return props;
        }
    }

    /**
     * Run.
     */
    void run() {
        mLogger.info("Creating consumer thread");

        CountDownLatch latch = new CountDownLatch(1);

        ConsumerRunnable consumerRunnable = new ConsumerRunnable(mBootstrapServer, mGroupId, mTopic, latch);
        Thread thread = new Thread(consumerRunnable);
        thread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            mLogger.info("Caugth shutdown hook");
            consumerRunnable.shutdown();
            await(latch);

            mLogger.info("Application has exited");
        }));
        await(latch);
    }

    /**
     * await.
     */
    void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            mLogger.error("Application got interrupted", e);
        } finally {
            mLogger.info("Application is closing");
        }
    }
}
