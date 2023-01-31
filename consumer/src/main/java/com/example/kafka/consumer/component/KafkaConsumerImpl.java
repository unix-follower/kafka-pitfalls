package com.example.kafka.consumer.component;

import com.example.kafka.consumer.config.AppProperties;
import com.example.kafka.consumer.config.KafkaConfigProperties;
import com.example.kafka.consumer.config.KafkaTopicProperties;
import com.example.kafka.consumer.util.KafkaMessageFormatter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@SuppressWarnings("unused")
@Component
public class KafkaConsumerImpl implements KafkaConsumer<String, String> {
  private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerImpl.class);

  private static final int FIRST_INDEX = 0;

  private static final int LAST_INDEX_SUBTRACT = 1;

  private static final String DATA_BUS_TOPIC_EXPR = "${" + KafkaTopicProperties.DATA_BUS_TOPIC + "}";

  private static final String AUTO_ACK_MODE_EXPR =
    "#{!'${spring.kafka.listener.ack-mode}'.equals('MANUAL') && " +
      "!'${spring.kafka.listener.ack-mode}'.equals('MANUAL_IMMEDIATE')}";

  private static final String RETRYABLE_AUTO_ACK_MODE_EXPR =
    "#{" +
      "(!'${spring.kafka.listener.ack-mode}'.equals('MANUAL') && " +
      "!'${spring.kafka.listener.ack-mode}'.equals('MANUAL_IMMEDIATE')) && " +
      "'${spring.kafka.retry.topic.enabled}'" +
      "}";

  private static final String MANUAL_ACK_MODE_EXPR =
    "#{'${spring.kafka.listener.ack-mode}'.equals('MANUAL') || " +
      "'${spring.kafka.listener.ack-mode}'.equals('MANUAL_IMMEDIATE')}";

  private static final String RETRYABLE_MANUAL_ACK_MODE_EXPR =
    "#{" +
      "('${spring.kafka.listener.ack-mode}'.equals('MANUAL') || " +
      "'${spring.kafka.listener.ack-mode}'.equals('MANUAL_IMMEDIATE')) && " +
      "'${spring.kafka.retry.topic.enabled}'" +
      "}";

  private final KafkaMessageFormatter kafkaMessageFormatter = new KafkaMessageFormatter();

  private final AppProperties appProperties;

  private final ObjectMapper objectMapper;

  private final AtomicLong lastCallMsAtomic = new AtomicLong(System.currentTimeMillis());

  @Component
  public static class Builder {
    private AppProperties appProperties;

    private ObjectMapper objectMapper;

    @Autowired
    public Builder setAppProperties(AppProperties appProperties) {
      this.appProperties = appProperties;
      return this;
    }

    @Autowired
    public Builder setObjectMapper(ObjectMapper objectMapper) {
      this.objectMapper = objectMapper;
      return this;
    }

    public KafkaConsumerImpl build() {
      return new KafkaConsumerImpl(this);
    }
  }

  public KafkaConsumerImpl(Builder builder) {
    appProperties = builder.appProperties;
    objectMapper = builder.objectMapper;
  }

  @Component
  @ConditionalOnProperty(value = KafkaConfigProperties.LISTENER_TYPE, havingValue = "SINGLE")
  @ConditionalOnExpression(AUTO_ACK_MODE_EXPR)
  @ConditionalOnMissingBean({RetryableListener.class, RetryableManualAckListener.class})
  private class Listener {
    @KafkaListener(topics = DATA_BUS_TOPIC_EXPR)
    public void consume(ConsumerRecord<String, String> record) {
      KafkaConsumerImpl.this.consume(record, null);
    }
  }

  @Component
  @ConditionalOnProperty(value = KafkaConfigProperties.LISTENER_TYPE, havingValue = "SINGLE")
  @ConditionalOnExpression(MANUAL_ACK_MODE_EXPR)
  @ConditionalOnMissingBean({RetryableListener.class, RetryableManualAckListener.class})
  private class ManualAckListener {
    @KafkaListener(topics = DATA_BUS_TOPIC_EXPR)
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
      KafkaConsumerImpl.this.consume(record, ack);
    }
  }

  @Component
  @ConditionalOnProperty(value = KafkaConfigProperties.LISTENER_TYPE, havingValue = "SINGLE")
  @ConditionalOnExpression(RETRYABLE_AUTO_ACK_MODE_EXPR)
  private class RetryableListener {
    @KafkaListener(topics = DATA_BUS_TOPIC_EXPR)
    @RetryableTopic(
      include = {
        NullPointerException.class,
        org.apache.kafka.clients.consumer.CommitFailedException.class // useless
      }
    )
    public void consume(ConsumerRecord<String, String> record) {
      KafkaConsumerImpl.this.consume(record, null);
    }

    @DltHandler
    public void retry(ConsumerRecord<String, String> record) {
      logger.info("Execute retry for record with offset = {}", record.offset());
      consume(record);
    }
  }

  @Component
  @ConditionalOnProperty(value = KafkaConfigProperties.LISTENER_TYPE, havingValue = "SINGLE")
  @ConditionalOnExpression(RETRYABLE_MANUAL_ACK_MODE_EXPR)
  private class RetryableManualAckListener {
    @KafkaListener(topics = DATA_BUS_TOPIC_EXPR)
    @RetryableTopic(
      include = {
        NullPointerException.class
      }
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
      KafkaConsumerImpl.this.consume(record, ack);
    }

    @DltHandler
    public void retry(ConsumerRecord<String, String> record, Acknowledgment ack) {
      logger.info("Execute retry for record with offset = {}", record.offset());
      consume(record, ack);
    }
  }

  @Component
  @ConditionalOnProperty(value = KafkaConfigProperties.LISTENER_TYPE, havingValue = "BATCH")
  @ConditionalOnExpression(AUTO_ACK_MODE_EXPR)
  private class BatchListener {
    @KafkaListener(topics = DATA_BUS_TOPIC_EXPR)
    public void consume(List<ConsumerRecord<String, String>> records) {
      KafkaConsumerImpl.this.consume(records, null);
    }
  }

  @Component
  @ConditionalOnProperty(value = KafkaConfigProperties.LISTENER_TYPE, havingValue = "BATCH")
  @ConditionalOnExpression(MANUAL_ACK_MODE_EXPR)
  private class ManualAckBatchListener {
    @KafkaListener(topics = DATA_BUS_TOPIC_EXPR)
    public void consume(List<ConsumerRecord<String, String>> records, Acknowledgment ack) {
      KafkaConsumerImpl.this.consume(records, ack);
    }
  }

  @Override
  public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
    logOffsets(Collections.singletonList(record));
    logRecord(() -> kafkaMessageFormatter.format(record));
    internalConsume(Collections.singletonList(record), ack);
  }

  @Override
  public void consume(List<ConsumerRecord<String, String>> records, Acknowledgment ack) {
    logOffsets(records);
    logRecord(() -> {
      final var sb = new StringBuilder();
      records.stream()
        .map(this.kafkaMessageFormatter::format)
        .map(msg -> msg + '\n')
        .forEach(sb::append);
      return sb.toString();
    });
    internalConsume(records, ack);
  }

  private void internalConsume(List<ConsumerRecord<String, String>> records, Acknowledgment ack) {
    final var kafkaConfigProps = appProperties.kafka();
    if (ack != null && kafkaConfigProps.immediateOffsetCommitEnabled()) {
      doAckIfEnabled(ack);
    }

    if (kafkaConfigProps.asyncProcessingEnabled()) {
      if (kafkaConfigProps.asyncProcessingMode().equals("ParallelStream")) {
        records.parallelStream().forEach(this::processRecord);
      } else {
        final var completableFutures = records.stream()
          .map(value -> CompletableFuture.runAsync(() -> processRecord(value)))
          .toList();
        CompletableFuture.allOf(completableFutures.toArray(CompletableFuture[]::new)).join();
      }
    } else {
      records.forEach(this::processRecord);
    }

    if (ack != null && kafkaConfigProps.isImmediateOffsetCommitDisabled()) {
      doAckIfEnabled(ack);
    }
  }

  private void doAckIfEnabled(Acknowledgment ack) {
    boolean isPerformCommit = !appProperties.kafka().doNotCommitOffsetEnabled();
    if (isPerformCommit) {
      ack.acknowledge();
    } else {
      logger.warn("Skip commit offset");
    }
  }

  private void processRecord(ConsumerRecord<String, String> record) {
    try {
      final var kafkaProps = appProperties.kafka();
      if (kafkaProps.emulateProcessingErrorEnabled()) {
        final long now = System.currentTimeMillis();
        final long lastCallMs = lastCallMsAtomic.get();

        boolean isExceeded = now - lastCallMs > kafkaProps.emulateProcessingErrorInterval().toMillis();
        if (isExceeded) {
          lastCallMsAtomic.set(now);
          throw new NullPointerException(
            String.format("NPE while processing (record offset = %d)", record.offset())
          );
        }
      }

      final var data = record.value();
      final JsonNode jsonNode;
      try {
        jsonNode = objectMapper.readTree(data);
      } catch (JsonProcessingException e) {
        throw new UncheckedIOException(e);
      }
      if (logger.isDebugEnabled()) {
        logger.debug("{}", jsonNode);
      }

      final var delay = kafkaProps.processingDelay();
      logger.info("Sleep for {} seconds (record offset = {})", delay.getSeconds(), record.offset());
      Thread.sleep(delay.toMillis());
      logger.info("Wake up (record offset = {})", record.offset());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private ConsumerRecord<String, String> findFirstRecord(List<ConsumerRecord<String, String>> records) {
    if (records instanceof LinkedList<ConsumerRecord<String, String>> linkedRecords) {
      return linkedRecords.getFirst();
    } else {
      return records.get(FIRST_INDEX);
    }
  }

  private ConsumerRecord<String, String> findLastRecord(List<ConsumerRecord<String, String>> records) {
    if (records instanceof LinkedList<ConsumerRecord<String, String>> linkedRecords) {
      return linkedRecords.getLast();
    } else {
      return records.get(records.size() - LAST_INDEX_SUBTRACT);
    }
  }

  private void logOffsets(List<ConsumerRecord<String, String>> records) {
    if (records.isEmpty() || !logger.isInfoEnabled()) {
      return;
    }

    final var firstRecord = findFirstRecord(records);
    final var lastRecord = findLastRecord(records);

    logger.info("Consume records with offset from {} to {} ", firstRecord.offset(), lastRecord.offset());
  }

  private void logRecord(Supplier<String> msgSupplier) {
    if (logger.isInfoEnabled() && appProperties.kafka().logMessage()) {
      logger.info("{}", msgSupplier.get());
    }
  }
}
