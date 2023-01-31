package com.example.kafka.producer.config;

public record KafkaConfigProperties(
  KafkaTopicProperties topics,
  KafkaScheduleProperties schedule
) {
}
