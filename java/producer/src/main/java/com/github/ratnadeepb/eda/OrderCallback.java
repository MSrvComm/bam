package com.github.ratnadeepb.eda;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;

public class OrderCallback implements Callback {

    private final Logger mLogger;

    public OrderCallback(Logger logger) {
        mLogger = logger;
    }

    @Override
    public void onCompletion(RecordMetadata metadata, Exception exception) {
        if (exception != null) {
            mLogger.error("Error while producing:", exception);
            exception.printStackTrace();
            // rethrow the interrupted exception
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return;
        }

        mLogger.info("Message sent successfully");

        if (mLogger.isInfoEnabled())
            mLogger.info(
                    "Received new meta. Topic: {}; Partition: {}; Offset: {}; Timestamp: {}",
                    metadata.topic(),
                    metadata.partition(), metadata.offset(),
                    metadata.timestamp());
    }

}
