#!/bin/bash
# Wrapper - delegates to build/build.sh
exec "$(dirname "$0")/build/build.sh" "$@"
