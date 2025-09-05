#!/bin/bash

# Read the active profile from global config
PROFILE=$(cat orchestrix.config 2>/dev/null || echo "dev")

echo "Building Orchestrix frontend with profile: $PROFILE"

# Change to frontend directory
cd orchestrix-ui

# Build the React application with the appropriate environment
npm run build:$PROFILE