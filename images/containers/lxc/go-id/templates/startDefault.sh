#!/bin/bash
#
# Quick start script for Go-ID Alpine container
# Launches a test instance with default configuration
#

cd "$(dirname "$0")"

# Use sample config
cp sample.conf test.conf

# Update config for test instance
sed -i 's/CONTAINER_NAME=.*/CONTAINER_NAME="go-id-test"/' test.conf

# Launch container
../../launchGoId.sh test.conf

echo ""
echo "Test container launched: go-id-test"
echo "Check health: curl http://localhost:7001/health"
echo "Stop with: lxc stop go-id-test --force && lxc delete go-id-test"
