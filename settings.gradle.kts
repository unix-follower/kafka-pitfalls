rootProject.name = "kafka-pitfalls"

pluginManagement {
  plugins {
    id("org.springframework.boot") version "3.1.2"
    id("io.spring.dependency-management") version "1.1.2"
  }
}

include(
  "consumer",
  "producer"
)
