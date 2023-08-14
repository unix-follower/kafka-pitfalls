package com.example.kafka.consumer.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("unused")
@Configuration
public class KafkaConsumerConfig {
  private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerConfig.class);

  @Autowired
  private KafkaProperties kafkaProperties;

  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
      .configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true)
      .configure(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES, true);
  }

  @Bean
  public ConsumerFactory<String, String> consumerFactory() {
    final var consumer = kafkaProperties.getConsumer();
    final var consumerProps = consumer.getProperties();

    final var props = new HashMap<>(
      Map.of(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers(),
        ConsumerConfig.CLIENT_ID_CONFIG, kafkaProperties.getClientId(),
        ConsumerConfig.GROUP_ID_CONFIG, consumer.getGroupId(),
        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, consumer.getEnableAutoCommit()
      )
    );

    final var maxPollIntervalMs = consumerProps.get(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG);
    Optional.ofNullable(maxPollIntervalMs)
      .ifPresent(value -> props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, value));

    Optional.ofNullable(consumer.getMaxPollRecords())
      .ifPresent(value -> props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, value));

    return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer());
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, String> batchFactory(
    ConsumerFactory<String, String> consumerFactory
  ) {
    final var listener = kafkaProperties.getListener();

    final var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
    factory.setConsumerFactory(consumerFactory);
    factory.setBatchListener(listener.getType() == KafkaProperties.Listener.Type.BATCH);
    return factory;
  }

  @Bean
  public KafkaTemplate<String, String> kafkaTemplate(KafkaProperties kafkaProperties) {
    final var props = new HashMap<>(
      Map.of(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers(),
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
        ProducerConfig.CLIENT_ID_CONFIG, kafkaProperties.getClientId()
      )
    );

    final var producerFactory = new DefaultKafkaProducerFactory<String, String>(props);
    producerFactory.setValueSerializer(new StringSerializer());

    return new KafkaTemplate<>(producerFactory);
  }

  @Bean
  @ConditionalOnProperty(
    value = "app.kafka.override-default-error-handler-enabled",
    havingValue = "true"
  )
  public CommonErrorHandler errorHandler(
    AppProperties appProperties,
    KafkaTemplate<String, String> kafkaTemplate
  ) {
    final var interval = appProperties.kafka().errorHandlerInterval().toMillis();
    final int attempts = appProperties.kafka().errorHandlerAttempts();

    final var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
    final var backOff = new FixedBackOff(interval, attempts);

    final var errorHandler = new DefaultErrorHandler(recoverer, backOff);
    errorHandler.setRetryListeners(new LoggingRetryListener());
    errorHandler.addRetryableExceptions(NullPointerException.class);
    errorHandler.addNotRetryableExceptions(CommitFailedException.class);
    return errorHandler;
  }

  private static class LoggingRetryListener implements RetryListener {
    @Override
    public void failedDelivery(ConsumerRecord<?, ?> consumerRecord, Exception ex, int deliveryAttempt) {
      logger.warn(
        "ConsumerRecord delivery failed. Attempt = {} for record offset = {}",
        deliveryAttempt, consumerRecord.offset()
      );
    }

    @Override
    public void failedDelivery(ConsumerRecords<?, ?> records, Exception ex, int deliveryAttempt) {
      logger.warn(
        "ConsumerRecords delivery failed. Attempt = {}. Records count = {}",
        deliveryAttempt,
        records.count()
      );
    }
  }
}
