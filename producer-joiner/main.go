package main

import (
	"log"
	"os"
	"os/signal"
	"time"

	"github.com/Shopify/sarama"
)

var (
	kakfaBrokers = []string{"localhost:30092"}
	kakfaTopic   = "sarama_topic"
	enqueued     int
)

func main() {
	producer, err := setupProducer()
	if err != nil {
		panic(err)
	}
	log.Println("Kafka Asyncproducer up and running!")

	// for a graceful shutdown
	signals := make(chan os.Signal, 1)
	signal.Notify(signals, os.Interrupt)

	produceMessages(producer, signals)

	log.Printf("Kafka AsyncProducer finished with %d messages produced.", enqueued)
}

func setupProducer() (sarama.AsyncProducer, error) {
	config := sarama.NewConfig()
	return sarama.NewAsyncProducer(kakfaBrokers, config)
}

func produceMessages(producer sarama.AsyncProducer, signals chan os.Signal) {
	for {
		time.Sleep(time.Second)
		message := &sarama.ProducerMessage{Topic: kakfaTopic, Value: sarama.StringEncoder("testing123")}
		select {
		case producer.Input() <- message:
			enqueued++
			log.Println("New message produced")
		case <-signals:
			producer.AsyncClose()
			return
		}
	}
}
