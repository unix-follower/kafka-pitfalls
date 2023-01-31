package com.example.kafka.producer.component;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletionStage;

public interface KafkaProducer<K, V> {
  CompletionStage<SendResult<K, V>> produce(ProducerRecord<K, V> record);
}
