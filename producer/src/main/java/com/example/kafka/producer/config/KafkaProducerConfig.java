package com.example.kafka.producer.config;

import com.example.kafka.producer.model.SampleMsgDto;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
@Configuration
public class KafkaProducerConfig {
  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper()
      .registerModule(new JavaTimeModule())
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
      .configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true)
      .setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));
  }

  @Bean
  public KafkaTemplate<String, SampleMsgDto> kafkaTemplate(
    KafkaProperties kafkaProperties,
    ObjectMapper objectMapper
  ) {
    final var props = new HashMap<>(
      Map.of(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers(),
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
        ProducerConfig.CLIENT_ID_CONFIG, kafkaProperties.getClientId()
      )
    );

    final var producerFactory = new DefaultKafkaProducerFactory<String, SampleMsgDto>(props);
    @SuppressWarnings("PMD.CloseResource")
    final var jsonSerializer = new JsonSerializer<SampleMsgDto>(objectMapper);
    jsonSerializer.setAddTypeInfo(false);
    producerFactory.setValueSerializer(jsonSerializer);

    return new KafkaTemplate<>(producerFactory);
  }
}
