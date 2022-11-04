package com.github.ratnadeepb.eda;

import java.time.Instant;

import org.slf4j.Logger;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.exceptions.InfluxException;

public class InfluxDBConnection {
    private String token;
    private String bucket;
    private String org;

    private String url;

    Logger mLogger;

    public InfluxDBClient buildConnection(String url, String token, String bucket, String org, Logger mLogger) {
        setToken(token);
        setBucket(bucket);
        setUrl(url);
        setOrg(org);
        this.mLogger = mLogger;
        return InfluxDBClientFactory.create(getUrl(), getToken().toCharArray(), getOrg(), getBucket());
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getOrg() {
        return org;
    }

    public void setOrg(String org) {
        this.org = org;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean singlePointWrite(InfluxDBClient influxDBClient, String consumerId, Double records) {
        boolean flag = false;
        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            Point point = Point.measurement("consumer").addTag("consumer_id", consumerId).addField("records", records)
                    .time(Instant.now(), WritePrecision.MS);
            writeApi.writePoint(point);
            flag = true;
        } catch (InfluxException | NullPointerException e) {
            mLogger.error("Exception!!" + e.getMessage());
        }
        return flag;
    }

    public boolean writePointPojo(InfluxDBClient influxDBClient, String consumerId, int records) {
        boolean flag = false;
        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            ConsumerSensor sensor = new ConsumerSensor();
            sensor.consumerId = consumerId;
            sensor.key = records;
            sensor.lastInspected = Instant.now();

            writeApi.writeMeasurement(WritePrecision.MS, sensor);
            flag = true;
        } catch (InfluxException e) {
            System.out.println("Exception!!" + e.getMessage());
        }
        return flag;
    }

    @Measurement(name = "consumerSensor")
    private static class ConsumerSensor {
        @Column(tag = true)
        String consumerId;

        @Column
        int key;

        @Column(timestamp = true)
        Instant lastInspected;
    }
}
