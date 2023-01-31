package com.example.kafka.producer.component;

import com.example.kafka.producer.model.SampleMsgDto;
import com.example.kafka.producer.util.KafkaMessageFormatter;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@SuppressWarnings("unused")
@Component
public class KafkaProducerImpl implements KafkaProducer<String, SampleMsgDto> {
  private static final Logger logger = LoggerFactory.getLogger(KafkaProducerImpl.class);

  private final KafkaTemplate<String, SampleMsgDto> kafkaTemplate;

  private final KafkaMessageFormatter formatter = new KafkaMessageFormatter("flood");

  @Component
  public static class Builder {
    private KafkaTemplate<String, SampleMsgDto> kafkaTemplate;

    @Autowired
    public Builder setKafkaTemplate(KafkaTemplate<String, SampleMsgDto> kafkaTemplate) {
      this.kafkaTemplate = kafkaTemplate;
      return this;
    }

    public KafkaProducerImpl build() {
      return new KafkaProducerImpl(this);
    }
  }

  public KafkaProducerImpl(Builder builder) {
    kafkaTemplate = builder.kafkaTemplate;
  }

  @Override
  public CompletionStage<SendResult<String, SampleMsgDto>> produce(ProducerRecord<String, SampleMsgDto> record) {
    return CompletableFuture.runAsync(() -> {
      if (logger.isInfoEnabled()) {
        logger.info(formatter.format(record));
      }
    })
      .thenCompose(unused -> kafkaTemplate.send(record));
  }
}
