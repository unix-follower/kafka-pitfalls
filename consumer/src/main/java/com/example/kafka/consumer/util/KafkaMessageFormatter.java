package com.example.kafka.consumer.util;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public class KafkaMessageFormatter {
  private final String descriptor;

  public KafkaMessageFormatter() {
    this(null);
  }

  public KafkaMessageFormatter(String descriptor) {
    if (descriptor != null) {
      this.descriptor = String.format("(%s)", descriptor);
    } else {
      this.descriptor = "";
    }
  }

  public String format(ConsumerRecord<?, ?> record) {
    var value = record.value();
    if (value instanceof byte[] bytes) {
      value = byteArrayToString(bytes);
    }
    var header = ">> consume message";
    if (isDescriptorNotEmpty()) {
      header = header + " " + descriptor;
    }
    final var leaderEpoch = record.leaderEpoch().orElse(null);
    return header + '\n' +
      "topic: " + record.topic() + '\n' +
      "key: " + record.key() + '\n' +
      "partition: " + record.partition() + '\n' +
      "offset: " + record.offset() + '\n' +
      "timestamp: " + record.timestamp() + '\n' +
      "timestampType: " + record.timestampType().name + '\n' +
      "serializedKeySize: " + record.serializedKeySize() + '\n' +
      "serializedValueSize: " + record.serializedValueSize() + '\n' +
      "leaderEpoch: " + leaderEpoch + '\n' +
      "headers: " + record.headers() + '\n' +
      "value: " + value;
  }

  private boolean isDescriptorNotEmpty() {
    return !descriptor.isEmpty();
  }

  private String byteArrayToString(byte[] data) {
    var result = "";
    if (data != null) {
      result = new String(data);
    }
    return result;
  }
}
