@echo off
REM Wait for Kafka to be ready
echo Waiting for Kafka to be ready...
timeout /t 10 /nobreak

REM Kafka bootstrap server
set KAFKA_BOOTSTRAP=localhost:9092

REM Create topics
echo Creating Kafka topics...

docker exec kafka kafka-topics --create --if-not-exists ^
  --bootstrap-server localhost:9092 ^
  --topic kitchen.commands ^
  --partitions 3 ^
  --replication-factor 1

docker exec kafka kafka-topics --create --if-not-exists ^
  --bootstrap-server localhost:9092 ^
  --topic kitchen.events ^
  --partitions 3 ^
  --replication-factor 1

docker exec kafka kafka-topics --create --if-not-exists ^
  --bootstrap-server localhost:9092 ^
  --topic delivery.commands ^
  --partitions 3 ^
  --replication-factor 1

docker exec kafka kafka-topics --create --if-not-exists ^
  --bootstrap-server localhost:9092 ^
  --topic delivery.events ^
  --partitions 3 ^
  --replication-factor 1

docker exec kafka kafka-topics --create --if-not-exists ^
  --bootstrap-server localhost:9092 ^
  --topic order.events ^
  --partitions 3 ^
  --replication-factor 1

echo Topics created successfully!

REM List all topics
echo Listing topics:
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092
