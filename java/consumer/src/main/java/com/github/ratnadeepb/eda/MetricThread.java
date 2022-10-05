package com.github.ratnadeepb.eda;

import java.util.Set;
import java.util.Map.Entry;

import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.slf4j.Logger;

public class MetricThread extends Thread {
    private Set<Entry<MetricName, ? extends Metric>> entrySet;

    // private Logger mLogger =
    // LoggerFactory.getLogger(MetricThread.class.getName());
    private Logger mLogger;

    MetricThread(Logger logger) {
        this.mLogger = logger;
    }

    public void setEntrySetObj(Set<?> set) {
        this.entrySet = (Set<Entry<MetricName, ? extends Metric>>) set;
    }

    public void run() {
        while (true) {
            for (Entry<MetricName, ? extends Metric> metric : entrySet) {
                if ("bytes-consumed-rate".equals(metric.getKey().name())) {
                    mLogger.info("Reporting Metric: {}: {}", metric.getKey().name(), metric.getValue());
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
