package com.example.kafka.producer.config;

public record KafkaScheduleProperties(
  String floodTaskFixedRateSec
) {
  public static final String FLOOD_TASK_FIXED_RATE_SEC = "app.kafka.schedule.flood-task-fixed-rate-sec";
}
