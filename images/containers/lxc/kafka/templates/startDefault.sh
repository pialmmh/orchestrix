#!/bin/bash

# Quick start script for Kafka
# Launches a single Kafka broker

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Starting Kafka Broker (Single Node)..."
"${SCRIPT_DIR}/../launch.sh" "${SCRIPT_DIR}/sample.conf"
