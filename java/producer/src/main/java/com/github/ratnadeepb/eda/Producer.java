package com.github.ratnadeepb.eda;

import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.influxdb.client.InfluxDBClient;

/**
 * Producer
 *
 */
public class Producer {
    // private static String token =
    // "E_HOFq8n1wkbEjuEeqi_fb4tqElN-GBH_VaTRAhxQfamMnQZWMIKS1ADzjCYBFl_26JFJWO4rYfOMKhIjud95w==";
    // private static String bucket = "consumer";
    // private static String org = "com.github.ratnadeepb";
    // private static String url = "http://influxdb:8086";

    // private static DBConn dbconn = new DBConn();
    // private static InfluxDBClient dbclient = dbconn.buildConnection(url, token,
    // bucket, org);

    public static void main(String[] args) throws InterruptedException {
        final Logger mLogger = LoggerFactory.getLogger(Producer.class.getName());
        Integer sendRate = Integer.parseInt(System.getenv("SEND_RATE"));
        // Integer wait = Integer.parseInt(System.getenv("WAIT"));
        // Integer waitNS = Integer.parseInt(System.getenv("WAIT_NS"));
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

                while (true) {
                    int i = 0;
                    int requestsSent = 0;
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
                    if (requestsSent > sendRate)
                        break;
                    try {
                        Thread.sleep(sleepMS, sleepNS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        }, 0, 1000);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            mLogger.info("Producer created {} records", i);
            producer.close();
        }));

        // try {
        // // do this for 10 seconds
        // // for (long stop = System.nanoTime() + TimeUnit.SECONDS.toNanos(60); stop >
        // // System.nanoTime();) {
        // while (true) {

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
        // Thread.sleep(wait, waitNS);
        // }
        // } finally {
        // mLogger.info("Producer created {} records", i);
        // producer.close();
        // }
    }
}
