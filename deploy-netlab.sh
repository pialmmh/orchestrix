#!/bin/bash
# One-click deployment script for netlab environment
# Deploys LXD + WireGuard + FRR to netlab01/02/03

cd "$(dirname "$0")"

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║         Netlab One-Click Deployment Launcher                  ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven not found. Installing..."
    sudo apt-get update && sudo apt-get install -y maven
fi

# Build project
echo "📦 Building project..."
mvn clean compile -q

if [ $? -ne 0 ]; then
    echo "❌ Build failed"
    exit 1
fi

echo "✅ Build complete"
echo ""

# Run deployment
echo "🚀 Starting deployment..."
echo ""

mvn exec:java \
    -Dexec.mainClass="com.telcobright.orchestrix.automation.example.NetlabDeployment" \
    -Dexec.cleanupDaemonThreads=false \
    -q

if [ $? -eq 0 ]; then
    echo ""
    echo "╔════════════════════════════════════════════════════════════════╗"
    echo "║              ✅ Deployment Successful!                        ║"
    echo "╚════════════════════════════════════════════════════════════════╝"
else
    echo ""
    echo "╔════════════════════════════════════════════════════════════════╗"
    echo "║              ❌ Deployment Failed                             ║"
    echo "╚════════════════════════════════════════════════════════════════╝"
    exit 1
fi
