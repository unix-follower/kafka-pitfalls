### Prepare environment
```shell
cd docker/
docker-compose up -d
```
### Run consumer
```shell
cd consumer/build/libs
export SPRING_KAFKA_CONSUMER_PROPERTIES_MAX_POLL_INTERVAL_MS=30000 # 30 sec
export APP_KAFKA_PROCESSING_DELAY=1m
java -jar consumer-0.0.1-SNAPSHOT.jar
```
### List topics
```shell
./kafka-topics.sh --bootstrap-server localhost:9092 --list
```
### DLT topic commands
```shell
./kafka-console-consumer.sh --bootstrap-server localhost:9092 --group console --topic demo.DLT --from-beginning
./kafka-console-consumer.sh --bootstrap-server localhost:9092 --group console --topic demo-dlt --from-beginning
./kafka-console-consumer.sh --bootstrap-server localhost:9092 --group console --topic demo-retry-0 --from-beginning

./kafka-topics.sh --bootstrap-server localhost:9092 --delete --topic demo-dlt 
./kafka-topics.sh --bootstrap-server localhost:9092 --delete --topic demo-retry-0
```
### Describe topics
```shell
./kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic demo
```
### List consumer groups
```shell
./kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list
```
### Reset offset to the beginning
```shell
./kafka-consumer-groups.sh --bootstrap-server localhost:9092 --reset-offsets \
  --group kafka-consumer-group \
  --to-offset 0 \
  --topic demo \
  --execute
```
### Describe consumer group offsets
```shell
./kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe \
  --group kafka-consumer-group \
  --verbose \
  --offsets
```
### Describe consumer group state
```shell
./kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe \
  --group kafka-consumer-group \
  --verbose \
  --state
```
### Describe consumer group members
```shell
./kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe \
  --group kafka-consumer-group \
  --verbose \
  --members
```
### Scale up partition count
```shell
./kafka-topics.sh --bootstrap-server localhost:9092 --alter --partitions 2 --topic demo 
```
