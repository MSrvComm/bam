package com.github.ratnadeepb.eda;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;

public class DBConn {
    private String token;
    private String bucket;
    private String org;

    final Logger mLogger = LoggerFactory.getLogger(Producer.class.getName());

    private String url;

    public InfluxDBClient buildConnection(String url, String token, String bucket, String org) {
        setToken(token);
        setBucket(bucket);
        setUrl(url);
        setOrg(org);
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

    public void queryData(InfluxDBClient influxDBClient) {
        String flux = "from(bucket:\"consumer\") |> range(start:0) |> filter(fn: (r) => r[\"_measurement\"] == \"sensor\") |> filter(fn: (r) => r[\"sensor_id\"] == \"TLM0100\"or r[\"sensor_id\"] == \"TLM0101\" or r[\"sensor_id\"] == \"TLM0103\" or r[\"sensor_id\"] == \"TLM0200\") |> sort() |> yield(name: \"sort\")";
        // from(bucket: "consumer")
        // |> range(start: v.timeRangeStart, stop: v.timeRangeStop)
        // |> filter(fn: (r) => r["_measurement"] == "sensor")
        // |> filter(fn: (r) => r["_field"] == "model_number")
        // |> filter(fn: (r) => r["sensor_id"] == "TLM0100" or r["sensor_id"] ==
        // "TLM0101" or r["sensor_id"] == "TLM0103" or r["sensor_id"] == "TLM0200")
        // |> sort()
        // |> yield(name: "sort")

        QueryApi queryApi = influxDBClient.getQueryApi();

        List<FluxTable> tables = queryApi.query(flux);
        for (FluxTable fluxTable : tables) {
            List<FluxRecord> records = fluxTable.getRecords();
            for (FluxRecord fluxRecord : records) {
                mLogger.info("{}", fluxRecord.getValueByKey("consumer_id"));
            }
        }
    }
}
