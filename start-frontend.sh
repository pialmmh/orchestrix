#!/bin/bash

# Read the active profile from global config
PROFILE=$(cat orchestrix.config 2>/dev/null || echo "dev")

echo "Starting Orchestrix frontend with profile: $PROFILE"

# Change to frontend directory
cd orchestrix-ui

# Start the React application with the appropriate environment
npm run start:$PROFILE