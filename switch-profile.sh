#!/bin/bash

# Available profiles
PROFILES=("dev" "staging" "prod" "mock")

# Function to display usage
show_usage() {
    echo "Usage: ./switch-profile.sh [profile]"
    echo "Available profiles: ${PROFILES[*]}"
    echo "Current profile: $(cat orchestrix.config 2>/dev/null || echo 'none')"
}

# Check if profile argument is provided
if [ $# -eq 0 ]; then
    show_usage
    exit 1
fi

PROFILE=$1

# Validate profile
if [[ ! " ${PROFILES[@]} " =~ " ${PROFILE} " ]]; then
    echo "Error: Invalid profile '$PROFILE'"
    show_usage
    exit 1
fi

# Update the global config file
echo "$PROFILE" > orchestrix.config

echo "Profile switched to: $PROFILE"
echo ""
echo "To start with this profile:"
echo "  Backend:  ./start-backend.sh"
echo "  Frontend: ./start-frontend.sh"