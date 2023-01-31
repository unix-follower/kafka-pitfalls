package com.example.kafka.consumer;

import com.example.kafka.consumer.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
@EnableConfigurationProperties(AppProperties.class)
public class KafkaConsumerApp {
  public static void main(String[] args) {
    SpringApplication.run(KafkaConsumerApp.class, args);
  }
}
