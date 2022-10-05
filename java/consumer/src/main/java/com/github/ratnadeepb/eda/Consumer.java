package com.github.ratnadeepb.eda;

import java.time.Duration;
import java.util.ArrayList;
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

import io.prometheus.client.Gauge;

/**
 * Hello world!
 * `
 */
public class Consumer {
    final Logger mLogger = LoggerFactory.getLogger(Consumer.class.getName());
    private ArrayList<Integer> keys = new ArrayList<>();
    static final Gauge requests = Gauge.build().name("kafka_consumer_records_total").help("Total records processed")
            .register();

    String token = "3Ghjv-1exJYtV8U0no_u5zA9ikoa463f6B2Q5wUc2KN_n1dSuZnT3pVwfN57ZFs1eG6RnlWuHuOMJ1ze9qh2lw==";
    String bucket = "consumer";
    String org = "com.github.ratnadeepb";
    String url = "http://influxdb:8086";

    InfluxDBConnection dbconn = new InfluxDBConnection();
    InfluxDBClient dbclient = dbconn.buildConnection(url, token, bucket, org);

    private class ConsumerRunnable implements Runnable {
        private CountDownLatch mLatch;
        private KafkaConsumer<Integer, Order> mConsumer;

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
                        if (keys.contains(rcrd.key())) {
                            continue;
                        } else {
                            keys.add(rcrd.key());
                            for (Entry<MetricName, ? extends Metric> metric : mConsumer.metrics().entrySet()) {
                                if ("bytes-consumed-rate".equals(metric.getKey().name())) {
                                    Double value = (Double) metric.getValue().metricValue();
                                    mLogger.info("Reporting Metric: {}: {}", metric.getKey().name(),
                                            value);
                                    // boolean res = dbconn.writePointPojo(dbclient, url, value);
                                    boolean res = dbconn.singlePointWrite(dbclient, "consumerID", value);
                                    if (!res) {
                                        mLogger.info("failed to load db");
                                    }
                                }
                            }
                            requests.inc();
                        }

                        if (mLogger.isInfoEnabled()) {
                            Order order = rcrd.value();
                            String customerName = order.getCustomerName();
                            Integer key = rcrd.key();
                            int quantity = order.getQuantity();
                            String product = order.getProduct().toString();

                            // if (key % 3 == 0) {
                            // Thread.sleep(500);
                            // }

                            if (quantity < 2)
                                mLogger.info("Key: {}, Customer {} ordered {} {}", key, customerName, quantity,
                                        product);
                            else
                                mLogger.info("Key: {}, Customer {} ordered {} {}s", key, customerName,
                                        quantity, product);
                        }
                    }
                }
            } catch (WakeupException w) {
                mLogger.info("Received shutdown signal");
                // } catch (InterruptedException e) {
                // e.printStackTrace();
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
            props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
            props.setProperty(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
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
        // try {
        // new HTTPServer.Builder().withPort(9102).build();
        // } catch (IOException e) {
        // e.printStackTrace();
        // }

        new Consumer().run();
        // final Logger mLogger = LoggerFactory.getLogger(Consumer.class.getName());
        // final int timeout = 20;
        // ArrayList<Integer> keys = new ArrayList<>();
        // Properties props = new Properties();
        // props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
        // "kafka-service:9092");
        // props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
        // IntegerDeserializer.class.getName());
        // props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
        // OrderDeserializer.class.getName());
        // props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "OrderGroup");
        // props.setProperty(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG,
        // CooperativeStickyAssignor.class.getName());
        // props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        // props.setProperty(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");

        // KafkaConsumer<Integer, Order> consumer = new KafkaConsumer<>(props);
        // consumer.subscribe(Collections.singletonList("OrderTopic"));

        // // Timer timer = new Timer();
        // // timer.schedule(new TimerTask() {
        // // @Override
        // // public void run() {
        // // for (Entry<MetricName, ? extends Metric> metric :
        // // consumer.metrics().entrySet()) {
        // // if ("bytes-consumed-rate".equals(metric.getKey().name()) ||
        // // "records-consumed-rate"
        // // .equals(metric.getKey().name()) ||
        // // "fetch-rate".equals(metric.getKey().name())) {
        // // mLogger.info("Reporting Metric: {}: {}", metric.getKey().name(),
        // // metric.getValue().metricValue());
        // // }
        // // }
        // // }
        // // }, 0, 1000);

        // try {
        // while (true) {
        // ConsumerRecords<Integer, Order> records =
        // consumer.poll(Duration.ofSeconds(timeout));
        // for (ConsumerRecord<Integer, Order> rcrd : records) {
        // if (keys.contains(rcrd.key())) {
        // continue;
        // } else {
        // keys.add(rcrd.key());
        // }

        // if (mLogger.isInfoEnabled()) {
        // Order order = rcrd.value();
        // String customerName = order.getCustomerName();
        // int quantity = order.getQuantity();
        // String product = order.getProduct().toString();
        // if (quantity < 2)
        // mLogger.info("Key: {}, Customer {} ordered {} {}", rcrd.key(), customerName,
        // quantity,
        // product);
        // else
        // mLogger.info("Key: {}, Customer {} ordered {} {}s", rcrd.key(), customerName,
        // quantity, product);
        // }
        // }

        // // m.setEntrySetObj(consumer.metrics().entrySet());

        // // for (Entry<MetricName, ? extends Metric> metric :
        // // consumer.metrics().entrySet()) {
        // // if ("bytes-consumed-rate".equals(metric.getKey().name())) {
        // // mLogger.info("Reporting Metric: {}: {}", metric.getKey().name(),
        // // metric.getValue().metricValue());
        // // }
        // // }
        // }
        // } finally {
        // consumer.close();
        // }
    }
}
