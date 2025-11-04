#!/bin/bash
#
# LXC Prerequisites Setup Script
# Checks and creates all LXC/LXD prerequisites
#
# Usage:
#   ./setup-lxc.sh                                    # Local setup
#   ./setup-lxc.sh <host> <user> <port> <password>  # Remote SSH setup
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AUTOMATION_DIR="$(dirname "$SCRIPT_DIR")"
ORCHESTRIX_DIR="$(dirname "$AUTOMATION_DIR")"

# Find jsch.jar
JSCH_JAR="$HOME/.m2/repository/com/github/mwiede/jsch/0.2.20/jsch-0.2.20.jar"
if [ ! -f "$JSCH_JAR" ]; then
    JSCH_JAR="$HOME/.m2/repository/com/jcraft/jsch/0.1.55/jsch-0.1.55.jar"
fi

if [ ! -f "$JSCH_JAR" ]; then
    echo "ERROR: jsch.jar not found in Maven repository"
    echo "Please install: mvn dependency:get -Dartifact=com.github.mwiede:jsch:0.2.20"
    exit 1
fi

# Compile Java if needed
JAVA_SRC="$AUTOMATION_DIR/api/infrastructure/LxcPrerequisitesManager.java"
JAVA_CLASS="$AUTOMATION_DIR/api/infrastructure/LxcPrerequisitesManager.class"

if [ ! -f "$JAVA_CLASS" ] || [ "$JAVA_SRC" -nt "$JAVA_CLASS" ]; then
    echo "Compiling LxcPrerequisitesManager..."
    cd "$AUTOMATION_DIR/api/infrastructure"
    javac -cp "$JSCH_JAR:$AUTOMATION_DIR/api" \
        LxcPrerequisitesManager.java
    cd - > /dev/null
    echo "✓ Compilation complete"
    echo ""
fi

# Run prerequisites manager
if [ $# -eq 0 ]; then
    echo "═══════════════════════════════════════════════════════"
    echo "  LXC Prerequisites Setup - Local Execution"
    echo "═══════════════════════════════════════════════════════"
    echo ""
    java -cp "$JSCH_JAR:$ORCHESTRIX_DIR" \
        automation.api.infrastructure.LxcPrerequisitesManager
else
    echo "═══════════════════════════════════════════════════════"
    echo "  LXC Prerequisites Setup - Remote SSH Execution"
    echo "═══════════════════════════════════════════════════════"
    echo ""
    java -cp "$JSCH_JAR:$ORCHESTRIX_DIR" \
        automation.api.infrastructure.LxcPrerequisitesManager \
        "$@"
fi
