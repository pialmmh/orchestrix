#!/bin/bash
# Convenience wrapper for building dev-env container
# This simply calls the build script in the build/ directory

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
exec "$SCRIPT_DIR/build/build.sh" "$@"