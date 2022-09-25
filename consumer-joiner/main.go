package main

import (
	"context"
	"log"
	"os"

	"github.com/Shopify/sarama"
)

var (
	kakfaBrokers    = []string{"localhost:30092"}
	kakfaTopics     = []string{"sarama_topic"}
	consumerGroupID = "sarama_consumer"
)

func main() {
	// Init config, specify appropriate version
	config := sarama.NewConfig()
	sarama.Logger = log.New(os.Stderr, "[sarama_logger]", log.LstdFlags)
	config.Version = sarama.V2_1_0_0

	// start with a client
	client, err := sarama.NewClient(kakfaBrokers, config)
	if err != nil {
		panic(err)
	}

	defer func() { _ = client.Close() }()

	// start a new consumer group
	group, err := sarama.NewConsumerGroupFromClient(consumerGroupID, client)
	if err != nil {
		panic(err)
	}
	defer func() { _ = group.Close() }()

	// track errors
	go func() {
		for err := range group.Errors() {
			log.Println("ERROR:", err)
		}
	}()

	// iterate over consumer sessions
	ctx := context.Background()
	for {
		handler := ConsumerGroupHandler{}

		err := group.Consume(ctx, kakfaTopics, handler)
		if err != nil {
			panic(err)
		}
	}
}
