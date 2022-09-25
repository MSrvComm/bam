# BAM: An Asynchronous Microservices Load Balancer

## Kafka

**NOTE:** Keep an eye out for KRAFT - Kafka with Raft instead of zookeeper.

## Consumer

## Producer

## Async Sidecar LoadBalancer

## confluent-kafka-go prerequisite

```bash
sudo apt-get install -y librdkafka-dev
```

## Building Producer and Consumer

```bash
sudo docker build -t ratnadeepb/kafka-producer .
```

## Referecens
[Kafka and Zookeeper yaml files](https://github.com/mmohamed/kafka-kubernetes)</br>
[Producer-Consumer Example using Kafka](https://medium.com/swlh/apache-kafka-with-golang-227f9f2eb818)</br>
[Producer-Consumer Example using Nats](https://shijuvar.medium.com/building-event-driven-distributed-systems-in-go-with-grpc-nats-jetstream-and-cockroachdb-c4b899c8636d)
[Rust producer-consumer](https://itnext.io/getting-started-with-kafka-and-rust-part-1-e0074961ec6b)
[Kafka Consumer Group](https://medium.com/@ronnansouza/setting-up-a-kafka-broker-using-docker-creating-a-producer-and-consumer-group-with-multiple-384b724cd324)
[Kafka Operating Modes](https://medium.com/swlh/how-to-consume-kafka-efficiently-in-golang-264f7fe2155b)