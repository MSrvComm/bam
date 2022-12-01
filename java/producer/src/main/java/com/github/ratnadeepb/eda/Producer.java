package com.github.ratnadeepb.eda;

import java.time.Instant;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.influxdb.LogLevel;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApi;
import com.influxdb.client.WriteOptions;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;

/**
 * Producer
 *
 */
public class Producer {
    // private static DBConn dbconn = new DBConn();
    // private static InfluxDBClient dbclient = dbconn.buildConnection(url, token,
    // bucket, org);

    public static void main(String[] args) throws InterruptedException {
        // String token =
        // "CzaB2UEQzhMS7rqWqWgvPOblplxbvXji1m5EzVvm4Uua3_zreTx85u-wdutwGV-uEu7h78cfqRMzI1hGEmQIFQ==";
        String token = System.getenv("INFLUXDB_TOKEN");
        String bucket = "producer";
        String org = "com.github.ratnadeepb";
        String url = "http://influxdb:8086";
        final Logger mLogger = LoggerFactory.getLogger(Producer.class.getName());
        Integer sendRate = Integer.parseInt(System.getenv("SEND_RATE"));
        final String containerIP = "consumer_ID:" + System.getenv("POD_IP");
        // Integer wait = Integer.parseInt(System.getenv("WAIT"));
        // Integer waitNS = Integer.parseInt(System.getenv("WAIT_NS"));
        InfluxDBClient dbclient = InfluxDBClientFactory.create(url,
                token.toCharArray(), org, bucket);
        dbclient.setLogLevel(LogLevel.BASIC);
        WriteApi writeApi = dbclient.makeWriteApi(WriteOptions.builder().flushInterval(5_000).build());
        Properties props = new Properties();
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka-service:9092");
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, OrderSerializer.class.getName());
        // props.setProperty(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "order-producer");

        // Local order
        Order order1 = new Order();
        order1.setCustomerName("Deep");
        order1.setProduct("Dell");
        order1.setQuantity(10);

        Order order2 = new Order();
        order2.setCustomerName("Pedro");
        order2.setProduct("Mac");
        order2.setQuantity(20);

        Order order3 = new Order();
        order3.setCustomerName("Rishi");
        order3.setProduct("Windows");
        order3.setQuantity(15);

        KafkaProducer<Integer, Order> producer = new KafkaProducer<>(props);
        // producer.initTransactions();

        int i = 0;
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                Double sleepMSdb = 1000.0 / sendRate;
                mLogger.info("sleepMSdb: {}", sleepMSdb);
                Integer sleepMS = sleepMSdb.intValue();
                mLogger.info("sleepMS: {}", sleepMS);
                Double fraction = (sleepMSdb - sleepMSdb) * 1e6;
                Integer sleepNS = fraction.intValue();
                mLogger.info("sleepNS: {}", sleepNS);

                long start = System.currentTimeMillis();
                int requestsSent = 0;

                while (requestsSent < sendRate) {
                    int i = 0;
                    requestsSent = 0;
                    ProducerRecord<Integer, Order> rcrd;
                    // mLogger.info("record started");
                    if (i % 3 == 0) {
                        // mLogger.info("creating new record rem=0");
                        rcrd = new ProducerRecord<>("OrderTopic", order3);
                        // mLogger.info("created new record rem=0");
                    } else if (i % 3 == 2) {
                        rcrd = new ProducerRecord<>("OrderTopic", order2);
                    } else {
                        // mLogger.info("creating new record");
                        rcrd = new ProducerRecord<>("OrderTopic", order1);
                    }

                    producer.send(rcrd, new OrderCallback(mLogger));
                    // mLogger.info("record sent");
                    i++;
                    requestsSent++;
                    // if (requestsSent > sendRate) {
                    // mLogger.info("Breaking: Sent {} requests", requestsSent);
                    // break;
                    // }

                    try {
                        Thread.sleep(sleepMS, sleepNS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (System.currentTimeMillis() > start + 1000) {
                        start = System.currentTimeMillis();
                        for (Entry<MetricName, ? extends Metric> metric : producer.metrics().entrySet()) {
                            String mName = metric.getKey().name();
                            if (mName.equals("request-rate")) {
                                Double value = (Double) metric.getValue().metricValue();
                                mLogger.info("Request Rate: {}: {}", mName,
                                        value);
                                Point point = Point.measurement("producer").addTag("consumer_id", containerIP)
                                        .addField("rps", value)
                                        .time(Instant.now(), WritePrecision.NS);
                                writeApi.writePoint(point);
                            }
                            if (mName.equals("response-rate")) {
                                Double value = (Double) metric.getValue().metricValue();
                                mLogger.info("Response Rate: {}: {}", mName,
                                        value);
                                Point point = Point.measurement("producer").addTag("consumer_id", "Producer")
                                        .addField("response_rate", value)
                                        .time(Instant.now(), WritePrecision.NS);
                                writeApi.writePoint(point);
                            }
                            if (mName.equals("request-latency-avg")) {
                                Double value = (Double) metric.getValue().metricValue();
                                mLogger.info("Request latency: {}: {}", mName,
                                        value);
                                Point point = Point.measurement("producer").addTag("consumer_id", "Producer")
                                        .addField("latency", value)
                                        .time(Instant.now(), WritePrecision.NS);
                                writeApi.writePoint(point);
                            }
                        }
                    }
                    // start = System.currentTimeMillis();
                    // }
                }
                // mLogger.info("Sent {} requests", requestsSent);
            }

        }, 0, 1000);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            mLogger.info("Producer created {} records", i);
            producer.close();
        }));

        // Double sleepMSdb = 1000.0 / sendRate;
        // mLogger.info("sleepMSdb: {}", sleepMSdb);
        // Integer sleepMS = sleepMSdb.intValue();
        // mLogger.info("sleepMS: {}", sleepMS);
        // Double fraction = (sleepMSdb - sleepMSdb) * 1e6;
        // Integer sleepNS = fraction.intValue();
        // mLogger.info("sleepNS: {}", sleepNS);

        // long start = System.currentTimeMillis();
        // int requestsSent = 0;

        // try {
        // // do this for 10 seconds
        // // for (long stop = System.nanoTime() + TimeUnit.SECONDS.toNanos(60); stop >
        // // System.nanoTime();) {
        // while (true) {
        // while (true) {
        // if ((System.currentTimeMillis() > start + 1000) || (requestsSent > sendRate))
        // {
        // break;
        // }
        // for (Entry<MetricName, ? extends Metric> metric :
        // producer.metrics().entrySet()) {
        // String mName = metric.getKey().name();
        // if (mName.equals("request-rate")) {
        // Double value = (Double) metric.getValue().metricValue();
        // mLogger.info("Request Rate: {}: {}", mName,
        // value);
        // Point point = Point.measurement("producer").addTag("consumer_id", "Producer")
        // .addField("rps", value)
        // .time(Instant.now(), WritePrecision.NS);
        // writeApi.writePoint(point);
        // }
        // if (mName.equals("response-rate")) {
        // Double value = (Double) metric.getValue().metricValue();
        // mLogger.info("Response Rate: {}: {}", mName,
        // value);
        // Point point = Point.measurement("producer").addTag("consumer_id", "Producer")
        // .addField("response_rate", value)
        // .time(Instant.now(), WritePrecision.NS);
        // writeApi.writePoint(point);
        // }
        // if (mName.equals("request-latency-avg")) {
        // Double value = (Double) metric.getValue().metricValue();
        // mLogger.info("Request latency: {}: {}", mName,
        // value);
        // Point point = Point.measurement("producer").addTag("consumer_id", "Producer")
        // .addField("latency", value)
        // .time(Instant.now(), WritePrecision.NS);
        // writeApi.writePoint(point);
        // }
        // }
        // ProducerRecord<Integer, Order> rcrd;
        // mLogger.info("record started");
        // if (i % 3 == 0) {
        // mLogger.info("creating new record rem=0");
        // rcrd = new ProducerRecord<>("OrderTopic", order3);
        // mLogger.info("created new record rem=0");
        // } else if (i % 3 == 2) {
        // rcrd = new ProducerRecord<>("OrderTopic", order2);
        // } else {
        // mLogger.info("creating new record");
        // rcrd = new ProducerRecord<>("OrderTopic", order1);
        // }

        // producer.send(rcrd, new OrderCallback(mLogger));
        // mLogger.info("record sent");

        // // dbconn.queryData(dbclient);

        // // try {
        // // // producer.beginTransaction();
        // // producer.send(rcrd, new OrderCallback(mLogger));
        // // // producer.commitTransaction();
        // // dbconn.queryData(dbclient);
        // // } catch (Exception e) {
        // // producer.abortTransaction();
        // // }
        // i++;
        // // Thread.sleep(sleepMS, sleepNS);
        // requestsSent++;
        // // mLogger.info("Sent {} requests", requestsSent);
        // // requestsSent = 0;
        // }
        // if (System.currentTimeMillis() < start + 1000) {
        // Thread.sleep(start + 1000 - System.currentTimeMillis());
        // }
        // mLogger.info("Sent {} requests", requestsSent);
        // start = System.currentTimeMillis();
        // requestsSent = 0;
        // i = 0;
        // }
        // } finally {
        // mLogger.info("Producer created {} records", i);
        // producer.close();
        // }
    }
}
