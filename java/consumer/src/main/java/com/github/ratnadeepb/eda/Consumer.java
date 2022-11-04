package com.github.ratnadeepb.eda;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.CooperativeStickyAssignor;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.influxdb.client.InfluxDBClient;

/**
 * Consumer
 */
public class Consumer {
    final Logger mLogger = LoggerFactory.getLogger(Consumer.class.getName());

    String token = "E_HOFq8n1wkbEjuEeqi_fb4tqElN-GBH_VaTRAhxQfamMnQZWMIKS1ADzjCYBFl_26JFJWO4rYfOMKhIjud95w==";
    String bucket = "consumer";
    String org = "com.github.ratnadeepb";
    String url = "http://influxdb:8086";

    InfluxDBConnection dbconn = new InfluxDBConnection();
    InfluxDBClient dbclient = dbconn.buildConnection(url, token, bucket, org, mLogger);

    private class ConsumerRunnable implements Runnable {
        private CountDownLatch mLatch;
        private KafkaConsumer<Integer, Order> mConsumer;

        private String containerIP = "container_ID:" + System.getenv("POD_IP");

        ConsumerRunnable(CountDownLatch latch) {
            mLatch = latch;
            mConsumer = new KafkaConsumer<>(consumerProps());
            mConsumer.subscribe(Collections.singletonList("OrderTopic"));
        }

        @Override
        public void run() {
            final int timeout = 20;
            try {
                while (true) {
                    ConsumerRecords<Integer, Order> records = mConsumer.poll(Duration.ofSeconds(timeout));
                    for (ConsumerRecord<Integer, Order> rcrd : records) {
                        for (Entry<MetricName, ? extends Metric> metric : mConsumer.metrics().entrySet()) {
                            if ("bytes-consumed-rate".equals(metric.getKey().name())) {
                                Double value = (Double) metric.getValue().metricValue();
                                mLogger.info("Reporting Metric: {}: {}", metric.getKey().name(),
                                        value);
                                mLogger.info("container IP: {}", containerIP);
                                boolean res = dbconn.singlePointWrite(dbclient, containerIP, value);
                                if (!res) {
                                    mLogger.info("failed to load db");
                                }
                            }
                        }

                        if (mLogger.isInfoEnabled()) {
                            Order order = rcrd.value();
                            String customerName = order.getCustomerName();
                            Integer key = rcrd.key();
                            int quantity = order.getQuantity();
                            String product = order.getProduct().toString();

                            Integer sleepMS = 100;

                            if (key % 3 == 0) {
                                if (Math.random() > 0.1) {
                                    sleepMS = 500;
                                }
                            } else {
                                if (Math.random() > 0.6) {
                                    sleepMS = 500;
                                }
                            }

                            Thread.sleep(sleepMS);

                            if (quantity < 2 && product != "Windows")
                                mLogger.info("Key: {}, Customer {} ordered {} {}", key, customerName, quantity,
                                        product);
                            else
                                mLogger.info("Key: {}, Customer {} ordered {} {}s", key, customerName,
                                        quantity, product);
                        }
                    }
                }
            } catch (WakeupException | InterruptedException w) {
                mLogger.info("Received shutdown signal");
            } finally {
                mConsumer.close();
                mLatch.countDown();
            }
        }

        void shutdown() {
            mConsumer.wakeup();
        }

        Properties consumerProps() {
            Properties props = new Properties();
            props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka-service:9092");
            props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
            props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, OrderDeserializer.class.getName());
            props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "OrderGroup");
            props.setProperty(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG,
                    CooperativeStickyAssignor.class.getName());
            // props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
            // props.setProperty(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
            // props.setProperty(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "20000");
            // props.setProperty(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, "22000");
            // props.setProperty(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "5000");
            return props;
        }
    }

    void run() {
        mLogger.info("Creating consumer threads");
        CountDownLatch latch = new CountDownLatch(1);
        ConsumerRunnable consumerRunnable = new ConsumerRunnable(latch);
        Thread thread = new Thread(consumerRunnable);
        thread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            mLogger.info("Caught shutdown hook");
            consumerRunnable.shutdown();
            await(latch);

            mLogger.info("Application was exited");
        }));
    }

    void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            mLogger.error("Application got interrupted:", e);
        } finally {
            mLogger.info("Application is closing");
        }
    }

    public static void main(String[] args) {
        new Consumer().run();
    }
}
