package main

import (
	"log"
	"os"
	"os/signal"
	"syscall"

	"github.com/Shopify/sarama"
)

func main() {
	topic := "comments"
	worker, err := connectConsumer([]string{"kafka-service:9092"})
	// worker, err := connectConsumer([]string{"localhost:9092"})
	// worker, err := connectConsumer([]string{"localhost:31738"})
	if err != nil {
		panic(err)
	}

	// Calling ConsumePartition. It will open one connection per broker
	// and share it for all partitions that live on it.
	consumer, err := worker.ConsumePartition(topic, 0, sarama.OffsetOldest)
	if err != nil {
		panic(err)
	}

	log.Println("Consumer started")

	sigchan := make(chan os.Signal, 1)
	signal.Notify(sigchan, syscall.SIGINT, syscall.SIGTERM)
	// count how many messages processed
	msgCount := 0

	// Get signal for finish
	doneCh := make(chan struct{})
	go func() {
		for {
			select {
			case err := <-consumer.Errors():
				log.Println(err)
			case msg := <-consumer.Messages():
				msgCount++
				log.Printf("Received message Count %d: | Topic(%s) | Message(%s) \n", msgCount, string(msg.Topic), string(msg.Value))
			case <-sigchan:
				log.Println("Interrupt is detected")
				doneCh <- struct{}{}
			}
		}
	}()
	<-doneCh
	log.Println("Processed", msgCount, "messages")

	if err := worker.Close(); err != nil {
		panic(err)
	}
}

func connectConsumer(brokersUrl []string) (sarama.Consumer, error) {
	config := sarama.NewConfig()
	config.Consumer.Return.Errors = true

	// Create a new consumer
	conn, err := sarama.NewConsumer(brokersUrl, config)
	if err != nil {
		return nil, err
	}
	return conn, nil
}
