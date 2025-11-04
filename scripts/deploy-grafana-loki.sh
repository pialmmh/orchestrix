#!/bin/bash
# Grafana-Loki One-Click Deployment Script
# Usage: ./deploy-grafana-loki.sh [config-file] [options]
# Location: /home/mustafa/telcobright-projects/orchestrix/scripts/deploy-grafana-loki.sh

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Determine script location and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Default configuration
DEFAULT_VERSION="v1.0"
CONFIG_FILE="${1:-$PROJECT_ROOT/deployment/grafana-loki/$DEFAULT_VERSION/production.yaml}"
DRY_RUN=""

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --dry-run)
            DRY_RUN="--dry-run"
            shift
            ;;
        --version|-v)
            VERSION="$2"
            CONFIG_FILE="$PROJECT_ROOT/deployment/grafana-loki/$VERSION/production.yaml"
            shift 2
            ;;
        --help|-h)
            cat << EOF
╔═══════════════════════════════════════════════════════════╗
║  Grafana-Loki One-Click Deployment (Java-Based)          ║
╚═══════════════════════════════════════════════════════════╝

Usage:
  $0 [config-file] [options]

Arguments:
  config-file    Path to deployment configuration YAML file
                 Default: deployment/grafana-loki/v1.0/production.yaml

Options:
  --version, -v  VERSION    Use specific version config
  --dry-run                 Simulate deployment without making changes
  --help, -h                Show this help message

Examples:
  # Deploy with default config (v1.0)
  $0

  # Deploy with custom config
  $0 deployment/grafana-loki/v1.0/staging.yaml

  # Deploy v2.0 configuration
  $0 --version v2.0

  # Dry run to see what would happen
  $0 --dry-run

Directory Structure:
  $PROJECT_ROOT/
  ├── scripts/deploy-grafana-loki.sh      ← This script
  ├── deployment/grafana-loki/
  │   ├── v1.0/
  │   │   ├── production.yaml
  │   │   └── staging.yaml
  │   └── v2.0/                            ← Future versions
  ├── automation/                          ← Java automation code
  └── images/containers/lxc/grafana-loki/            ← Container definition

Requirements:
  - Maven 3.6+
  - Java 17 or 21
  - SSH access to target server
  - Target server: Ubuntu 22.04+ (will auto-install LXD/BTRFS)

Documentation:
  - Container README: images/containers/lxc/grafana-loki/README.md
  - Scaffolding Guide: images/containers/lxc/CONTAINER_SCAFFOLDING_STANDARD.md
  - Deployment Guide: deployment/grafana-loki/README.md

EOF
            exit 0
            ;;
        *)
            CONFIG_FILE="$1"
            shift
            ;;
    esac
done

# Banner
echo -e "${BLUE}"
cat << "EOF"
╔═══════════════════════════════════════════════════════════╗
║                                                           ║
║     Grafana-Loki One-Click Deployment                     ║
║     Java-Based Automation Framework                       ║
║                                                           ║
╚═══════════════════════════════════════════════════════════╝
EOF
echo -e "${NC}"

echo "Project Root: $PROJECT_ROOT"
echo ""

# Verify we're in Orchestrix project
if [ ! -f "$PROJECT_ROOT/pom.xml" ]; then
    echo -e "${RED}Error: Not in Orchestrix project root${NC}"
    echo "Expected to find pom.xml in: $PROJECT_ROOT"
    exit 1
fi

# Check if config file exists
if [ ! -f "$CONFIG_FILE" ]; then
    # Try relative to project root
    if [ -f "$PROJECT_ROOT/$CONFIG_FILE" ]; then
        CONFIG_FILE="$PROJECT_ROOT/$CONFIG_FILE"
    else
        echo -e "${RED}Error: Configuration file not found: $CONFIG_FILE${NC}"
        echo ""
        echo "Please create a configuration file:"
        echo "  mkdir -p $PROJECT_ROOT/deployment/grafana-loki/$DEFAULT_VERSION"
        echo "  cp $PROJECT_ROOT/deployment/grafana-loki/production.yaml.example \\"
        echo "     $PROJECT_ROOT/deployment/grafana-loki/$DEFAULT_VERSION/production.yaml"
        echo "  vi $PROJECT_ROOT/deployment/grafana-loki/$DEFAULT_VERSION/production.yaml"
        exit 1
    fi
fi

echo -e "${GREEN}✓${NC} Configuration: $CONFIG_FILE"

# Check prerequisites
echo ""
echo "Checking prerequisites..."

# Check Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}✗ Java not found${NC}"
    echo "Please install Java 17 or higher"
    echo "  sudo apt install openjdk-21-jdk"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo -e "${RED}✗ Java version too old: $JAVA_VERSION${NC}"
    echo "Please install Java 17 or higher"
    exit 1
fi
echo -e "${GREEN}✓${NC} Java $JAVA_VERSION"

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}✗ Maven not found${NC}"
    echo "Please install Maven 3.6+"
    echo "  sudo apt install maven"
    exit 1
fi
echo -e "${GREEN}✓${NC} Maven $(mvn -version | head -1 | awk '{print $3}')"

# Navigate to automation module
cd "$PROJECT_ROOT/automation"

# Check if we need to compile
echo ""
if [ ! -d "target/classes" ] || [ "$PROJECT_ROOT/automation/pom.xml" -nt "target/classes" ]; then
    echo -e "${YELLOW}Compiling automation framework...${NC}"
    mvn compile -DskipTests -q
    if [ $? -ne 0 ]; then
        echo -e "${RED}✗ Compilation failed${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓${NC} Compilation complete"
else
    echo -e "${GREEN}✓${NC} Using existing compiled classes"
fi

# Convert config file to absolute path
CONFIG_FILE_ABS=$(realpath "$CONFIG_FILE")

echo ""
echo "========================================="
echo "Starting Deployment"
echo "========================================="
echo "Config: $CONFIG_FILE_ABS"
if [ -n "$DRY_RUN" ]; then
    echo -e "${YELLOW}Mode: DRY RUN (no changes will be made)${NC}"
fi
echo "========================================="
echo ""

# Run the deployment
cd "$PROJECT_ROOT/automation"

mvn exec:java \
    -Dexec.mainClass="com.telcobright.orchestrix.automation.runners.grafanaloki.GrafanaLokiDeployer" \
    -Dexec.args="$CONFIG_FILE_ABS $DRY_RUN" \
    -Dexec.classpathScope=compile \
    -q

DEPLOY_EXIT_CODE=$?

echo ""
if [ $DEPLOY_EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}"
    cat << "EOF"
╔═══════════════════════════════════════════════════════════╗
║                                                           ║
║           ✓ DEPLOYMENT SUCCESSFUL                         ║
║                                                           ║
╚═══════════════════════════════════════════════════════════╝
EOF
    echo -e "${NC}"
    echo "Next steps:"
    echo "  1. Access Grafana: http://<server-ip>:3000"
    echo "  2. Login: admin/admin (change password immediately)"
    echo "  3. Loki is pre-configured as datasource"
    echo "  4. Deploy log shippers (Promtail) to your apps"
else
    echo -e "${RED}"
    cat << "EOF"
╔═══════════════════════════════════════════════════════════╗
║                                                           ║
║           ✗ DEPLOYMENT FAILED                             ║
║                                                           ║
╚═══════════════════════════════════════════════════════════╝
EOF
    echo -e "${NC}"
    echo "Please check the logs above for details"
    echo ""
    echo "Common issues:"
    echo "  1. SSH connection failed - check host/credentials"
    echo "  2. Storage setup failed - check disk space"
    echo "  3. LXD installation failed - check network connectivity"
    exit $DEPLOY_EXIT_CODE
fi
