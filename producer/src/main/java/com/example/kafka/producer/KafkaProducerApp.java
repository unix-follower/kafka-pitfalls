package com.example.kafka.producer;

import com.example.kafka.producer.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableKafka
@EnableScheduling
@EnableConfigurationProperties({AppProperties.class})
public class KafkaProducerApp {
  public static void main(String[] args) {
    SpringApplication.run(KafkaProducerApp.class, args);
  }
}
