#!/bin/bash

# Wait for Kafka to be ready
echo "Waiting for Kafka to be ready..."
sleep 10

# Kafka bootstrap server
KAFKA_BOOTSTRAP=localhost:9092

# Create topics
echo "Creating Kafka topics..."

# Kitchen topics
kafka-topics.sh --create --if-not-exists \
  --bootstrap-server $KAFKA_BOOTSTRAP \
  --topic kitchen.commands \
  --partitions 3 \
  --replication-factor 1

kafka-topics.sh --create --if-not-exists \
  --bootstrap-server $KAFKA_BOOTSTRAP \
  --topic kitchen.events \
  --partitions 3 \
  --replication-factor 1

# Delivery topics
kafka-topics.sh --create --if-not-exists \
  --bootstrap-server $KAFKA_BOOTSTRAP \
  --topic delivery.commands \
  --partitions 3 \
  --replication-factor 1

kafka-topics.sh --create --if-not-exists \
  --bootstrap-server $KAFKA_BOOTSTRAP \
  --topic delivery.events \
  --partitions 3 \
  --replication-factor 1

# Order events topic
kafka-topics.sh --create --if-not-exists \
  --bootstrap-server $KAFKA_BOOTSTRAP \
  --topic order.events \
  --partitions 3 \
  --replication-factor 1

echo "Topics created successfully!"

# List all topics
echo "Listing topics:"
kafka-topics.sh --list --bootstrap-server $KAFKA_BOOTSTRAP
