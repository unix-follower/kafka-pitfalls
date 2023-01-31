package com.example.kafka.consumer.config;

public record KafkaTopicProperties(String dataBusTopic) {
  public static final String DATA_BUS_TOPIC = "app.kafka.topics.data-bus-topic";
}
