# BAM: An Asynchronous Microservices Load Balancer

## Kafka

The Kafka config files are in `k8s/clusters` folder. The deployment should work as:

```bash
kubectl apply -f zookeeper.yaml
kubectl apply -f kafka.yaml
kubectl apply -f kafka-svc.yaml
```

**NOTE:** Keep an eye out for KRAFT - Kafka with Raft instead of zookeeper.

## Consumer

This is in the `java/consumer` folder.

## Producer

This is in the `java/producer` folder.

## The data (order)

This is in the `java/order` folder.

## Compiling

The docker images are built with the Google jib tool and compiled with:

```bash
mvn compile jib:build
```

The docker config file (`~/.docker/config.json`) has to be set up for this to work:

```json
{
        "auths": {
                "https://index.docker.io/v1/": {
                        "auth": "YWRtaW46cGFzc3dvcmQ="
                }
        },
        "HttpHeaders": {
                "User-Agent": "Docker-Client/20.10.12 (linux)"
        }
}
```

`YWRtaW46cGFzc3dvcmQ=` is the base64 encoding of the docker user id and password - `echo -n admin:password | base64`.

## Maven

### Create a project

```bash
mvn archetype:generate -DgroupId=com.github.ratnadeepb.eda -DartifactId=producer -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
```

### Execute a program

```bash
mvn exec:java -Dexec.mainClass=com.github.ratnadeepb.kafka.OrderProducer
```

### Add order to the producer and consumer projects

In the order directory,

```bash
mvn install # install into local maven repo
```

In the `pom.xml` of producer and consumer:

```xml
<dependency>
    <groupId>com.github.ratnadeepb.kafka</groupId>
    <artifactId>order</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### Performing a clean reinstall

```bash
mvn clean install # clean previous install and reinstall
```


### Update the project

```bash
mvn update project # update the project
```

### Generate sources

```bash
mvn generate-sources # generate source if using avro and some such
```

## Async Sidecar LoadBalancer

## Kafka

### Kafka on Mac

```bash
brew install kafka
```

This installs Kafka in `/usr/local/Cellar/kafka`. Manually, start the services.

```bash
zookeeper-server-start /usr/local/etc/kafka/zookeeper.properties
kafka-server-start /usr/local/etc/kafka/server.properties
```

### Topics

#### List topics

```bash
kafka-topics --list --bootstrap-server localhost:9092
```

#### Describe topic

```bash
kafka-topics --describe --bootstrap-server localhost:9092 --topic first-topic
```

#### Delete topic

```bash
kafka-topics --delete --bootstrap-server localhost:9092 --topic first-topic
```

#### Create topic

```bash
kafka-topics --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 10 --topic first-topic
```

#### Listen on topic

```bash
kafka-console-consumer --bootstrap-server localhost:9092 --topic first-topic
```

#### Produce to topic

```bash
kafka-console-producer --broker-list localhost:9092 --topic first-topic
```

### Confluent

#### Java 11

Find Java 11 with, `/usr/libexec/java_home -v 11`

#### confluent-kafka-go prerequisite

```bash
sudo apt-get install -y librdkafka-dev
```

#### Starting Kafka

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/adoptopenjdk-11.jdk/Contents/Home confluent local services start
```

#### Stopping the services

```bash
confluent local services stop
```

### Kubernetes Kafka

```bash
kubectl exec -it kafka-0 -- kafka-topics.sh --zookeeper zk-cs.default.svc.cluster.local:2181 --list
```

```bash
kubectl exec -it kafka-0 -- kafka-topics.sh --zookeeper zk-cs.default.svc.cluster.local:2181 --describe --topic OrderTopic
```

```bash
kubectl exec -it kafka-0 -- kafka-topics.sh --zookeeper zk-cs.default.svc.cluster.local:2181 --delete --topic OrderTopic
```

```bash
kubectl exec -it kafka-0 -- kafka-topics.sh --zookeeper zk-cs.default.svc.cluster.local:2181 --create --replication-factor 2 --partitions 10 --topic OrderTopic
```

## Building Producer and Consumer

```bash
sudo docker build -t ratnadeepb/kafka-producer .
```

## Referecens
[Kafka and Zookeeper yaml files](https://github.com/mmohamed/kafka-kubernetes)</br>
[Producer-Consumer Example using Kafka](https://medium.com/swlh/apache-kafka-with-golang-227f9f2eb818)</br>
[Producer-Consumer Example using Nats](https://shijuvar.medium.com/building-event-driven-distributed-systems-in-go-with-grpc-nats-jetstream-and-cockroachdb-c4b899c8636d)</br>
[Rust producer-consumer](https://itnext.io/getting-started-with-kafka-and-rust-part-1-e0074961ec6b)</br>
[Kafka Consumer Group](https://medium.com/@ronnansouza/setting-up-a-kafka-broker-using-docker-creating-a-producer-and-consumer-group-with-multiple-384b724cd324)</br>
[Kafka Operating Modes](https://medium.com/swlh/how-to-consume-kafka-efficiently-in-golang-264f7fe2155b)</br>
[Java Producer Consumer](https://medium.com/pharos-production/kafka-using-java-e10bfeec8638)</br>
[Weighted Consumer Group Assignor - Java](https://blog.devgenius.io/diving-into-kafka-partitioning-by-building-a-custom-partition-assignor-656eb99bf885)</br>
[Lag based assignor](https://github.com/grantneale/kafka-lag-based-assignor)</br>
[Install Influx](https://docs.influxdata.com/influxdb/v2.4/install/?t=CLI+Setup)
[InfluxDB Java Client](https://www.influxdata.com/blog/getting-started-java-influxdb/)
[Java Main](https://github.com/rvsathe/JavaInfluxDBExample/blob/main/src/main/java/com/example/influxdbexample/App.java)
[Java Influx](https://github.com/rvsathe/JavaInfluxDBExample/blob/main/src/main/java/com/example/influxdbexample/InfluxDBConnectionClass.java)