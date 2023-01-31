rootProject.name = "kafka-pitfalls"

pluginManagement {
  plugins {
    id("org.springframework.boot") version "3.0.1"
    id("io.spring.dependency-management") version "1.1.0"
  }
}

include("consumer")
include("producer")
