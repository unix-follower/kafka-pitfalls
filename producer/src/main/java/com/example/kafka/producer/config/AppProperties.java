package com.example.kafka.producer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties("app")
public record AppProperties(
  @NestedConfigurationProperty KafkaConfigProperties kafka
) {
}
