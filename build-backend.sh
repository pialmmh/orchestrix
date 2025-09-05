#!/bin/bash

# Read the active profile from global config
PROFILE=$(cat orchestrix.config 2>/dev/null || echo "dev")

echo "Building Orchestrix backend with profile: $PROFILE"

# Set the Spring profile environment variable
export ORCHESTRIX_PROFILE=$PROFILE

# Build the Spring Boot application
./mvnw clean package -DskipTests