#!/bin/bash

# Kafka Topic Recreation Script - Single Node Configuration
# Adapted for kafka-kafka-1 container with localhost:9092 bootstrap server

set -e

# Configuration
KAFKA_CONTAINER="kafka-kafka-1"
BOOTSTRAP_SERVERS="localhost:9092"

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored messages
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if Kafka container is running
check_kafka_container() {
    print_info "Checking if Kafka container '$KAFKA_CONTAINER' is running..."
    if ! sudo docker ps | grep -q "$KAFKA_CONTAINER"; then
        print_error "Kafka container '$KAFKA_CONTAINER' is not running!"
        exit 1
    fi
    print_info "Kafka container is running"
}

# Function to delete a topic if it exists
delete_topic() {
    local topic=$1
    print_info "Checking if topic '$topic' exists..."

    if sudo docker exec $KAFKA_CONTAINER kafka-topics --bootstrap-server $BOOTSTRAP_SERVERS --list | grep -q "^${topic}$"; then
        print_warning "Deleting existing topic: $topic"
        sudo docker exec $KAFKA_CONTAINER kafka-topics \
            --bootstrap-server $BOOTSTRAP_SERVERS \
            --delete \
            --topic "$topic"

        # Wait for deletion to complete
        sleep 2
        print_info "Topic '$topic' deleted successfully"
    else
        print_info "Topic '$topic' does not exist, skipping deletion"
    fi
}

# Function to create a topic
create_topic() {
    local topic=$1
    local partitions=$2

    # Single node can only have replication factor of 1
    local replication_factor=1

    print_info "Creating topic '$topic' with $partitions partition(s) and replication factor $replication_factor"

    sudo docker exec $KAFKA_CONTAINER kafka-topics \
        --bootstrap-server $BOOTSTRAP_SERVERS \
        --create \
        --topic "$topic" \
        --partitions "$partitions" \
        --replication-factor "$replication_factor"

    print_info "Topic '$topic' created successfully"
}

# Function to describe a topic
describe_topic() {
    local topic=$1
    print_info "Describing topic '$topic':"
    sudo docker exec $KAFKA_CONTAINER kafka-topics \
        --bootstrap-server $BOOTSTRAP_SERVERS \
        --describe \
        --topic "$topic"
}

# Function to recreate a topic
recreate_topic() {
    local topic=$1
    local partitions=$2

    echo ""
    echo "=========================================="
    print_info "Processing topic: $topic"
    echo "=========================================="

    delete_topic "$topic"
    create_topic "$topic" "$partitions"
    describe_topic "$topic"

    echo ""
}

# Main execution
main() {
    print_info "Starting Kafka topic recreation for single-node setup..."
    echo ""

    check_kafka_container

    echo ""
    print_info "Current topics before recreation:"
    sudo docker exec $KAFKA_CONTAINER kafka-topics --bootstrap-server $BOOTSTRAP_SERVERS --list
    echo ""

    # Recreate all topics with their partition counts
    # Note: Single node Kafka can only have replication factor of 1

    recreate_topic "FAILED_SMS" 1
    recreate_topic "PLAINTEXT" 1
    recreate_topic "PLAINTEXT2" 1
    recreate_topic "borak-1" 1
    recreate_topic "borak-2" 1
    recreate_topic "borak-3" 1
    recreate_topic "defaultSmsQueue" 1
    recreate_topic "khaja-1" 1
    recreate_topic "khaja-2" 1
    recreate_topic "sriResponseForRest" 1
    recreate_topic "mtFsmResponseForRest" 1
    recreate_topic "config_event_loader" 1
    recreate_topic "all-db-changes" 1

    echo ""
    echo "=========================================="
    print_info "All topics recreated successfully!"
    echo "=========================================="
    echo ""

    print_info "Final topic list:"
    sudo docker exec $KAFKA_CONTAINER kafka-topics --bootstrap-server $BOOTSTRAP_SERVERS --list

    echo ""
    print_info "Topic recreation complete for single-node Kafka setup"
}

# Run main function
main
