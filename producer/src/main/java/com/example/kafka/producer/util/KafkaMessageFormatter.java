package com.example.kafka.producer.util;

import org.apache.kafka.clients.producer.ProducerRecord;

public class KafkaMessageFormatter {
  private final String descriptor;

  public KafkaMessageFormatter(String descriptor) {
    if (descriptor != null) {
      this.descriptor = String.format("(%s)", descriptor);
    } else {
      this.descriptor = "";
    }
  }

  public String format(ProducerRecord<?, ?> record) {
    var value = record.value();
    if (value instanceof byte[] bytes) {
      value = byteArrayToString(bytes);
    }
    var header = ">> produce message";
    if (isDescriptorNotEmpty()) {
      header = header + " " + descriptor;
    }
    return header + '\n' +
      "topic: " + record.topic() + '\n' +
      "key: " + record.key() + '\n' +
      "partition: " + record.partition() + '\n' +
      "headers: " + record.headers() + '\n' +
      "value: " + value + '\n';
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
