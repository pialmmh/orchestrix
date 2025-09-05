#!/bin/bash

# Read the active profile from global config
PROFILE=$(cat orchestrix.config 2>/dev/null || echo "dev")

echo "Starting Orchestrix backend with profile: $PROFILE"

# Set the Spring profile environment variable
export ORCHESTRIX_PROFILE=$PROFILE

# Start the Spring Boot application
if [ -f "./mvnw" ]; then
    ./mvnw spring-boot:run
elif command -v mvn &> /dev/null; then
    mvn spring-boot:run
else
    echo "❌ Error: Neither Maven wrapper (./mvnw) nor system Maven (mvn) found"
    echo "Please install Maven or create a Maven wrapper"
    exit 1
fi