package com.github.ratnadeepb.eda;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
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

import com.influxdb.LogLevel;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApi;
import com.influxdb.client.WriteOptions;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.exceptions.InfluxException;

import io.reactivex.rxjava3.core.BackpressureOverflowStrategy;

/**
 * Consumer
 */
public class Consumer {
    final Logger mLogger = LoggerFactory.getLogger(Consumer.class.getName());

    // String token =
    // "CzaB2UEQzhMS7rqWqWgvPOblplxbvXji1m5EzVvm4Uua3_zreTx85u-wdutwGV-uEu7h78cfqRMzI1hGEmQIFQ==";
    String token = System.getenv("INFLUXDB_TOKEN");
    String bucket = "consumer";
    String org = "com.github.ratnadeepb";
    String url = "http://influxdb:8086";

    InfluxDBConnection dbconn = new InfluxDBConnection();

    // InfluxDBClient dbclient = dbconn.buildConnection(url, token, bucket, org,
    // mLogger);

    private class ConsumerRunnable implements Runnable {
        private CountDownLatch mLatch;
        private KafkaConsumer<Integer, Order> mConsumer;

        private String containerIP = "consumer_ID:" + System.getenv("POD_IP");
        private long reportDuration = Integer.parseInt(System.getenv("REPORT_DURATION"));
        private Integer slowMS = Integer.parseInt(System.getenv("SLOW_MS"));
        private Integer fastMS = Integer.parseInt(System.getenv("FAST_MS"));

        ConsumerRunnable(CountDownLatch latch) {
            mLatch = latch;
            mConsumer = new KafkaConsumer<>(consumerProps());
            mConsumer.subscribe(Collections.singletonList("OrderTopic"));
        }

        @Override
        public void run() {
            final int timeout = 20;
            long start = System.currentTimeMillis();
            InfluxDBClient dbclient = InfluxDBClientFactory.create(url,
                    token.toCharArray(), org, bucket);
            dbclient.setLogLevel(LogLevel.BASIC);
            WriteApi writeApi = dbclient.makeWriteApi(WriteOptions.builder().flushInterval(5_000).build());
            try {
                while (true) {
                    ConsumerRecords<Integer, Order> records = mConsumer.poll(Duration.ofSeconds(timeout));
                    for (ConsumerRecord<Integer, Order> rcrd : records) {
                        if (System.currentTimeMillis() > start + reportDuration) {
                            start = System.currentTimeMillis();
                            for (Entry<MetricName, ? extends Metric> metric : mConsumer.metrics().entrySet()) {
                                String mName = metric.getKey().name();
                                // if (mName.equals("TotalTimeMs")) {
                                if (mName.equals("records-lag-max")) {
                                    Double value = (Double) metric.getValue().metricValue();
                                    mLogger.info("Records Lag Max: {}: {}", mName,
                                            value);
                                    Point point = Point.measurement("consumer").addTag("consumer_id", containerIP)
                                            .addField("lag-max", value)
                                            .time(Instant.now(), WritePrecision.NS);
                                    writeApi.writePoint(point);
                                }
                                // if (mName.equals("bytes-consumed-rate")) {
                                if (mName.equals("records-consumed-rate")) {
                                    Double value = (Double) metric.getValue().metricValue();
                                    mLogger.info("Reporting Metric: {}: {}", mName,
                                            value);
                                    Point point = Point.measurement("consumer").addTag("consumer_id", containerIP)
                                            .addField("records", value)
                                            .time(Instant.now(), WritePrecision.NS);
                                    writeApi.writePoint(point);
                                }
                                if (mName.equals("records-lag")) {
                                    Double value = (Double) metric.getValue().metricValue();
                                    mLogger.info("Records Lag: {}: {}", mName,
                                            value);
                                    String fieldTag = "lag" + Integer.toString(rcrd.partition());
                                    Point point = Point.measurement("consumer").addTag("consumer_id", containerIP)
                                            .addField(fieldTag, value)
                                            .time(Instant.now(), WritePrecision.NS);
                                    writeApi.writePoint(point);
                                }
                            }
                        }

                        Order order = rcrd.value();
                        String customerName = order.getCustomerName();
                        // Integer key = rcrd.key();
                        int quantity = order.getQuantity();
                        String product = order.getProduct().toString();

                        Integer sleepMS = fastMS;

                        // // if (key % 3 == 0) {
                        // if (customerName.equals("Deep")) {
                        // if (Math.random() > 0.1) {
                        // sleepMS = slowMS;
                        // }
                        // } else {
                        // if (Math.random() > 0.6) {
                        // sleepMS = slowMS;
                        // }
                        // }
                        if (customerName.equals("Deep")) {
                            sleepMS = slowMS;
                        } else {
                            sleepMS = fastMS;
                        }

                        Thread.sleep(sleepMS);

                        if (mLogger.isInfoEnabled()) {
                            if (quantity < 2 && product != "Windows")
                                mLogger.info("Customer {} ordered {} {}", customerName, quantity,
                                        product);
                            else
                                mLogger.info("Customer {} ordered {} {}s", customerName,
                                        quantity, product);
                        }
                    }
                }
            } catch (WakeupException | InterruptedException | InfluxException | NullPointerException w) {
                // } catch (WakeupException w) {
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
            String hostname = "1";
            try {
                hostname = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            Properties props = new Properties();
            props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka-service:9092");
            props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
            props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, OrderDeserializer.class.getName());
            props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "OrderGroup");
            props.setProperty(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG,
                    CooperativeStickyAssignor.class.getName());
            // props.setProperty(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, hostname);

            // props.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
            // props.setProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "2000");

            // props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            // props.setProperty(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
            // props.setProperty(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG,
            // Integer.toString(slowMS * 100));
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
