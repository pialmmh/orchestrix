#!/bin/bash
# Setup Orchestrix commands in PATH

ORCHESTRIX_ROOT="$(cd "$(dirname "$0")" && pwd)"
BIN_DIR="$ORCHESTRIX_ROOT/bin"

# Create symlink in /usr/local/bin
echo "Setting up Orchestrix commands..."

# Ensure bin directory exists
mkdir -p "$BIN_DIR"

# Create scaffold symlink
if [ -f "/usr/local/bin/scaffold" ]; then
    echo "Warning: /usr/local/bin/scaffold already exists"
    read -p "Overwrite? (y/N): " OVERWRITE
    if [[ "$OVERWRITE" =~ ^[Yy]$ ]]; then
        sudo rm /usr/local/bin/scaffold
    else
        echo "Skipping scaffold command"
        exit 0
    fi
fi

sudo ln -s "$BIN_DIR/scaffold" /usr/local/bin/scaffold
echo "✅ Created /usr/local/bin/scaffold"

echo ""
echo "You can now use: scaffold <container-name>"
echo ""
echo "Example workflow:"
echo "  1. scaffold redis-cache      # Creates directory and template"
echo "  2. Edit REQUIREMENTS.md      # Define what to build"
echo "  3. scaffold redis-cache      # Reads requirements and scaffolds"
echo "  4. Review generated files    # Edit as needed"
echo "  5. Build the container       # Automatic prompt or manual"