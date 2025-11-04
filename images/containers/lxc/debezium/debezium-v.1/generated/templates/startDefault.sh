#!/bin/bash
#
# Quick Start Script for Debezium Container
# Uses sample.conf for default MySQL configuration
#

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Starting Debezium MySQL Connector..."
echo "Using configuration: ${SCRIPT_DIR}/sample.conf"
echo ""

"${SCRIPT_DIR}/../launch.sh" "${SCRIPT_DIR}/sample.conf"
