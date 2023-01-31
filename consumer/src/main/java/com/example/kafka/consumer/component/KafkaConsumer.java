package com.example.kafka.consumer.component;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

public interface KafkaConsumer<K, V> {
  void consume(ConsumerRecord<K, V> record, Acknowledgment ack);

  void consume(List<ConsumerRecord<K, V>> records, Acknowledgment ack);
}
