#!/bin/bash
#
# Generic Artifact Publisher
# Can be used by any LXC container
# Usage: publish.sh <artifact-file-path>
#

set -e

ARTIFACT_PATH="$1"

if [ -z "$ARTIFACT_PATH" ]; then
    echo "Usage: $0 <artifact-file-path>"
    echo ""
    echo "Example:"
    echo "  $0 /path/to/orchestrix/images/containers/lxc/consul/consul-v.1/generated/artifact/consul-v1-*.tar.gz"
    exit 1
fi

if [ ! -f "$ARTIFACT_PATH" ]; then
    echo "ERROR: Artifact file not found: $ARTIFACT_PATH"
    exit 1
fi

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AUTOMATION_DIR="$(dirname "$SCRIPT_DIR")"

# Compile Java if needed
JAVA_SRC="$AUTOMATION_DIR/api/publish/ArtifactPublishManager.java"
JAVA_CLASS="$AUTOMATION_DIR/api/publish/ArtifactPublishManager.class"

if [ ! -f "$JAVA_CLASS" ] || [ "$JAVA_SRC" -nt "$JAVA_CLASS" ]; then
    echo "Compiling ArtifactPublishManager..."
    cd "$AUTOMATION_DIR/api/publish"
    javac -cp "/usr/share/java/mysql-connector-java.jar:." ArtifactPublishManager.java
    cd - > /dev/null
fi

# Run Java publish manager
echo "Publishing artifact: $(basename "$ARTIFACT_PATH")"
echo ""

java -cp "/usr/share/java/mysql-connector-java.jar:$AUTOMATION_DIR/api" \
    automation.api.publish.ArtifactPublishManager \
    "$ARTIFACT_PATH"
