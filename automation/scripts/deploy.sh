#!/bin/bash
#
# Generic SSH-Based Artifact Deployment
# MANDATORY: All production deployments MUST use SSH
# Usage: deploy.sh <deployment-yaml>
#

set -e

YAML_FILE="$1"

if [ -z "$YAML_FILE" ]; then
    echo "Usage: $0 <deployment-yaml>"
    echo ""
    echo "Example:"
    echo "  $0 /path/to/deployments/published-v1.yml"
    echo ""
    echo "IMPORTANT: Production deployments MUST specify terminal.type: ssh"
    exit 1
fi

if [ ! -f "$YAML_FILE" ]; then
    echo "ERROR: Deployment YAML not found: $YAML_FILE"
    exit 1
fi

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AUTOMATION_DIR="$(dirname "$SCRIPT_DIR")"

# Compile Java if needed
JAVA_SRC="$AUTOMATION_DIR/api/deployment/ArtifactDeploymentManager.java"
JAVA_CLASS="$AUTOMATION_DIR/api/deployment/ArtifactDeploymentManager.class"

if [ ! -f "$JAVA_CLASS" ] || [ "$JAVA_SRC" -nt "$JAVA_CLASS" ]; then
    echo "Compiling ArtifactDeploymentManager..."
    cd "$AUTOMATION_DIR/api/deployment"
    javac -cp "/usr/share/java/mysql-connector-java.jar:/usr/share/java/jsch.jar:/usr/share/java/snakeyaml.jar:." \
        ArtifactDeploymentManager.java
    cd - > /dev/null
fi

# Run Java deployment manager (SSH-based)
echo "Starting SSH-based deployment from: $(basename "$YAML_FILE")"
echo ""

java -cp "/usr/share/java/mysql-connector-java.jar:/usr/share/java/jsch.jar:/usr/share/java/snakeyaml.jar:$AUTOMATION_DIR/api" \
    automation.api.deployment.ArtifactDeploymentManager \
    "$YAML_FILE"
