package main

import (
	"encoding/json"
	"log"

	"github.com/Shopify/sarama"
	"github.com/gofiber/fiber/v2"
)

type Comment struct {
	Text string `form:"text" json:"text"`
}

func main() {
	app := fiber.New()
	api := app.Group("/api/v1") // /api
	api.Post("/comments", createComment)
	log.Println("Listening on :", 8080)
	log.Fatal(app.Listen(":8080"))
}

func createComment(c *fiber.Ctx) error {
	// create new comment
	cmt := new(Comment)

	// parse body into comment struct
	if err := c.BodyParser(cmt); err != nil {
		log.Println(err)
		c.Status(400).JSON(&fiber.Map{
			"success": false,
			"message": err,
		})
		return err
	}
	// convert body into bytes and send it to kafka
	cmtInBytes, err := json.Marshal(cmt)
	if err != nil {
		log.Println(err)
		c.Status(400).JSON(&fiber.Map{
			"success": false,
			"message": err,
		})
		return err
	}

	pushCommentToQueue("comments", cmtInBytes)

	// return comment in JSON format
	err = c.JSON(&fiber.Map{
		"success": true,
		"message": "Comment pushed successfully",
		"comment": cmt,
	})
	if err != nil {
		c.Status(500).JSON(&fiber.Map{
			"success": false,
			"message": "Error creating product",
		})
		return err
	}

	return err
}

func connectProducer(brokertUrl []string) (sarama.SyncProducer, error) {
	config := sarama.NewConfig()
	config.Producer.Return.Successes = true
	config.Producer.RequiredAcks = sarama.WaitForAll
	config.Producer.Retry.Max = 5

	conn, err := sarama.NewSyncProducer(brokertUrl, config)
	if err != nil {
		return nil, err
	}

	return conn, err
}

func pushCommentToQueue(topic string, message []byte) error {
	brokersUrl := []string{"kafka-service:9092"}
	// brokersUrl := []string{"localhost:9092"}
	// brokersUrl := []string{"localhost:31738"}
	producer, err := connectProducer(brokersUrl)
	if err != nil {
		return err
	}

	defer producer.Close()

	msg := &sarama.ProducerMessage{
		Topic: topic,
		Partition: 2,
		Value: sarama.StringEncoder(message),
	}

	partition, offset, err := producer.SendMessage(msg)
	if err != nil {
		return nil
	}

	log.Printf("Message is stored in topic(%s)/partition(%d)/offset(%d)\n", topic, partition, offset)
	return nil
}
