package com.example.kafka.producer.model;

import java.time.OffsetDateTime;

public record SampleMsgDto(
  String msg,
  OffsetDateTime dateTime
) {
}
