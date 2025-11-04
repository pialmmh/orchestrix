#!/bin/bash
# Grafana-Loki Deployment Script (Generated)
# Version: v1.0
# Auto-generated from templates - DO NOT EDIT MANUALLY
# Location: grafana-loki-v.1/generated/deploy.sh

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Determine script location
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VERSION_DIR="$(dirname "$SCRIPT_DIR")"
CONTAINER_DIR="$(dirname "$VERSION_DIR")"
PROJECT_ROOT="$(cd "$CONTAINER_DIR/../../../.." && pwd)"

# Default configuration
DEPLOYMENT_CONFIG="${1:-$SCRIPT_DIR/deployments/production.yml}"
DRY_RUN=false

# Parse options
while [[ $# -gt 0 ]]; do
    case $1 in
        --production|-p)
            DEPLOYMENT_CONFIG="$SCRIPT_DIR/deployments/production.yml"
            shift
            ;;
        --staging|-s)
            DEPLOYMENT_CONFIG="$SCRIPT_DIR/deployments/staging.yml"
            shift
            ;;
        --local|-l)
            DEPLOYMENT_CONFIG="$SCRIPT_DIR/deployments/local-dev.yml"
            shift
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --help|-h)
            cat << EOF
Grafana-Loki Deployment Script (v1.0)

Usage:
  $0 [config-file] [options]

Quick Deploy:
  $0 --production          # Deploy production config
  $0 --staging             # Deploy staging config
  $0 --local               # Deploy local dev config

Custom Config:
  $0 path/to/config.yml    # Deploy custom configuration

Options:
  --dry-run                # Simulate deployment
  --help, -h               # Show this help

Examples:
  # Deploy to production
  $0 --production

  # Deploy to staging
  $0 --staging

  # Deploy with custom config
  $0 deployments/production.yml

  # Dry run
  $0 --production --dry-run

Directory Structure:
  $SCRIPT_DIR/
  ├── deploy.sh              ← This script
  ├── deployments/
  │   ├── production.yml
  │   ├── staging.yml
  │   └── local-dev.yml
  └── artifact/              ← Built container images

EOF
            exit 0
            ;;
        *)
            DEPLOYMENT_CONFIG="$1"
            shift
            ;;
    esac
done

# Banner
echo -e "${BLUE}"
cat << "EOF"
╔═══════════════════════════════════════════════════════════╗
║                                                           ║
║     Grafana-Loki Deployment (v1.0)                        ║
║     Java-Based Automation                                 ║
║                                                           ║
╚═══════════════════════════════════════════════════════════╝
EOF
echo -e "${NC}"

# Verify config exists
if [ ! -f "$DEPLOYMENT_CONFIG" ]; then
    echo -e "${RED}Error: Deployment configuration not found: $DEPLOYMENT_CONFIG${NC}"
    echo ""
    echo "Available configurations:"
    ls -1 "$SCRIPT_DIR/deployments/"*.yml 2>/dev/null || echo "  No configurations found"
    exit 1
fi

echo -e "${GREEN}✓${NC} Configuration: $DEPLOYMENT_CONFIG"
echo -e "${GREEN}✓${NC} Project root: $PROJECT_ROOT"

# Check Java automation is available
if [ ! -d "$PROJECT_ROOT/automation" ]; then
    echo -e "${RED}✗ Automation framework not found${NC}"
    echo "Expected: $PROJECT_ROOT/automation"
    exit 1
fi

# Navigate to automation module
cd "$PROJECT_ROOT/automation"

# Check prerequisites
echo ""
echo "Checking prerequisites..."

if ! command -v java &> /dev/null; then
    echo -e "${RED}✗ Java not found${NC}"
    exit 1
fi
echo -e "${GREEN}✓${NC} Java $(java -version 2>&1 | awk -F '"' '/version/ {print $2}')"

if ! command -v mvn &> /dev/null; then
    echo -e "${RED}✗ Maven not found${NC}"
    exit 1
fi
echo -e "${GREEN}✓${NC} Maven $(mvn -version | head -1 | awk '{print $3}')"

# Compile if needed
echo ""
if [ ! -d "target/classes" ]; then
    echo -e "${YELLOW}Compiling automation framework...${NC}"
    mvn compile -DskipTests -q
    echo -e "${GREEN}✓${NC} Compilation complete"
fi

# Execute deployment
echo ""
echo "========================================="
echo "Deploying Grafana-Loki"
echo "========================================="
echo "Config: $(basename $DEPLOYMENT_CONFIG)"
if [ "$DRY_RUN" = true ]; then
    echo -e "${YELLOW}Mode: DRY RUN${NC}"
fi
echo "========================================="
echo ""

# Run Java automation
JAVA_ARGS="$(realpath $DEPLOYMENT_CONFIG)"
if [ "$DRY_RUN" = true ]; then
    JAVA_ARGS="$JAVA_ARGS --dry-run"
fi

mvn exec:java \
    -Dexec.mainClass="com.telcobright.orchestrix.automation.runners.grafanaloki.GrafanaLokiDeployer" \
    -Dexec.args="$JAVA_ARGS" \
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
    exit $DEPLOY_EXIT_CODE
fi
