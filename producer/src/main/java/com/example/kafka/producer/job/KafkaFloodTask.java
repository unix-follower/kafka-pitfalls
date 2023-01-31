package com.example.kafka.producer.job;

import com.example.kafka.producer.component.KafkaProducer;
import com.example.kafka.producer.config.AppProperties;
import com.example.kafka.producer.config.KafkaScheduleProperties;
import com.example.kafka.producer.model.SampleMsgDto;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
@Component
public class KafkaFloodTask implements Runnable {
  private final AppProperties appProperties;

  private final KafkaProducer<String, SampleMsgDto> kafkaProducer;

  @Component
  public static class Builder {
    private AppProperties appProperties;

    private KafkaProducer<String, SampleMsgDto> kafkaProducer;

    @Autowired
    public Builder setAppProperties(AppProperties appProperties) {
      this.appProperties = appProperties;
      return this;
    }

    @Autowired
    public Builder setKafkaProducer(KafkaProducer<String, SampleMsgDto> kafkaProducer) {
      this.kafkaProducer = kafkaProducer;
      return this;
    }

    public KafkaFloodTask build() {
      return new KafkaFloodTask(this);
    }
  }

  public KafkaFloodTask(Builder builder) {
    appProperties = builder.appProperties;
    kafkaProducer = builder.kafkaProducer;
  }

  @Override
  @Scheduled(
    fixedRateString = "${" + KafkaScheduleProperties.FLOOD_TASK_FIXED_RATE_SEC + "}",
    timeUnit = TimeUnit.SECONDS
  )
  public void run() {
    final var msg = createHelloWorldMsg();

    final var record = new ProducerRecord<String, SampleMsgDto>(
      appProperties.kafka().topics().dataBusTopic(), msg
    );
    kafkaProducer.produce(record).toCompletableFuture().join();
  }

  private SampleMsgDto createHelloWorldMsg() {
    return new SampleMsgDto(
      "Hello, world!",
      OffsetDateTime.now()
    );
  }
}
