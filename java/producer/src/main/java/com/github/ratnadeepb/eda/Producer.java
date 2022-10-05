package com.github.ratnadeepb.eda;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.influxdb.client.InfluxDBClient;

import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;

/**
 * Hello world!
 *
 */
public class Producer {
    static final Gauge records = Gauge.build().name("kafka_producer_records_total").help("Total records processed")
            .register();

    private static String token = "3Ghjv-1exJYtV8U0no_u5zA9ikoa463f6B2Q5wUc2KN_n1dSuZnT3pVwfN57ZFs1eG6RnlWuHuOMJ1ze9qh2lw==";
    private static String bucket = "consumer";
    private static String org = "com.github.ratnadeepb";
    private static String url = "http://influxdb:8086";

    private static DBConn dbconn = new DBConn();
    private static InfluxDBClient dbclient = dbconn.buildConnection(url, token, bucket, org);

    public static void main(String[] args) throws InterruptedException {
        final Logger mLogger = LoggerFactory.getLogger(Producer.class.getName());
        Properties props = new Properties();
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka-service:9092");
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, OrderSerializer.class.getName());
        props.setProperty(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "order-producer");

        try {
            new HTTPServer.Builder().withPort(9102).build();
        } catch (IOException e) {
            e.printStackTrace();
        }

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
        producer.initTransactions();
        // ProducerRecord<Integer, Order> rcrd1 = new ProducerRecord<>("OrderTopic",
        // order1);
        // ProducerRecord<Integer, Order> rcrd2 = new ProducerRecord<>("OrderTopic",
        // order2);
        // ProducerRecord<Integer, Order> rcrd3 = new ProducerRecord<>("OrderTopic",
        // order3);

        int i = 0;
        try {
            // do this for 10 seconds
            // for (long stop = System.nanoTime() + TimeUnit.SECONDS.toNanos(60); stop >
            // System.nanoTime();) {
            while (true) {
                ProducerRecord<Integer, Order> rcrd;
                if (i % 3 == 0) {
                    rcrd = new ProducerRecord<>("OrderTopic", i, order3);
                } else if (i % 3 == 2) {
                    rcrd = new ProducerRecord<>("OrderTopic", i, order2);
                } else {
                    rcrd = new ProducerRecord<>("OrderTopic", i, order1);
                }
                try {
                    producer.beginTransaction();
                    producer.send(rcrd, new OrderCallback(mLogger));
                    producer.commitTransaction();
                    records.inc();
                    dbconn.queryData(dbclient);
                } catch (Exception e) {
                    producer.abortTransaction();
                }
                // producer.send(rcrd2, new OrderCallback(mLogger));
                // producer.send(rcrd3, new OrderCallback(mLogger));
                // Thread.sleep(100);
                i++;
            }
        } finally {
            mLogger.info("Producer created {} records", i);
            producer.close();
        }
    }
}
