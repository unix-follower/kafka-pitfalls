package com.example.kafka.consumer.config;

import java.time.Duration;

public record KafkaConfigProperties(
  KafkaTopicProperties topics,

  Duration processingDelay,

  boolean immediateOffsetCommitEnabled,

  boolean asyncProcessingEnabled,

  String asyncProcessingMode,

  boolean doNotCommitOffsetEnabled,

  boolean emulateProcessingErrorEnabled,

  Duration emulateProcessingErrorInterval,

  int errorHandlerAttempts,

  Duration errorHandlerInterval,

  boolean logMessage
) {
  public static final String LISTENER_TYPE = "spring.kafka.listener.type";

  public boolean isImmediateOffsetCommitDisabled() {
    return !this.immediateOffsetCommitEnabled;
  }
}
