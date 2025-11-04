#!/bin/bash
#
# Main Deployment Script
# Usage: ./deploy.sh <config-file> [environment]
#

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname $(dirname $SCRIPT_DIR))"
JAVA_CLASS="automation.api.deployment.DeploymentManager"
JAR_PATH="${PROJECT_DIR}/build/libs/orchestrix-automation.jar"
LOG_DIR="${PROJECT_DIR}/deployment/logs"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Arguments
CONFIG_FILE="${1}"
ENVIRONMENT="${2:-dev}"

# Functions
print_usage() {
    echo "Usage: $0 <config-file> [environment]"
    echo ""
    echo "Arguments:"
    echo "  config-file   Path to deployment configuration (YAML)"
    echo "  environment   Deployment environment (dev|staging|production) [default: dev]"
    echo ""
    echo "Examples:"
    echo "  $0 ../configs/environments/dev/go-id-single.yaml"
    echo "  $0 ../configs/environments/production/consul-cluster.yaml production"
}

validate_config() {
    local config=$1

    if [ ! -f "$config" ]; then
        echo -e "${RED}ERROR: Config file not found: $config${NC}"
        return 1
    fi

    # Basic YAML validation
    if ! grep -q "^deployment:" "$config"; then
        echo -e "${RED}ERROR: Invalid config file - missing 'deployment' section${NC}"
        return 1
    fi

    if ! grep -q "^artifact:" "$config"; then
        echo -e "${RED}ERROR: Invalid config file - missing 'artifact' section${NC}"
        return 1
    fi

    return 0
}

setup_logging() {
    mkdir -p "$LOG_DIR/deployments"
    LOG_FILE="$LOG_DIR/deployments/deploy_${TIMESTAMP}.log"
    exec 1> >(tee -a "$LOG_FILE")
    exec 2>&1
}

check_database() {
    # Check if database is accessible
    mysql -h 127.0.0.1 -P 3306 -u root -p123456 -e "SELECT 1" >/dev/null 2>&1
    if [ $? -ne 0 ]; then
        echo -e "${YELLOW}WARNING: Database connection failed. Artifact tracking may not work.${NC}"
        return 1
    fi
    return 0
}

build_jar_if_needed() {
    if [ ! -f "$JAR_PATH" ]; then
        echo -e "${YELLOW}Building JAR file...${NC}"
        cd "$PROJECT_DIR"
        if [ -f "build.gradle" ]; then
            ./gradlew build
        elif [ -f "pom.xml" ]; then
            mvn clean package
        else
            # Compile manually
            mkdir -p build/libs
            javac -d build/classes automation/api/deployment/*.java
            jar cf "$JAR_PATH" -C build/classes .
        fi
    fi
}

run_deployment() {
    local config=$1
    local env=$2

    echo "========================================="
    echo "Starting Deployment"
    echo "========================================="
    echo "Config: $config"
    echo "Environment: $env"
    echo "Timestamp: $TIMESTAMP"
    echo "Log: $LOG_FILE"
    echo "========================================="
    echo ""

    # Set environment variables for database
    export DB_URL="jdbc:mysql://127.0.0.1:3306/orchestrix"
    export DB_USER="root"
    export DB_PASSWORD="123456"

    # Run the deployment
    java -cp "$JAR_PATH:lib/*" \
         -Djava.util.logging.config.file=logging.properties \
         "$JAVA_CLASS" \
         "$config"

    return $?
}

handle_result() {
    local exit_code=$1

    if [ $exit_code -eq 0 ]; then
        echo ""
        echo -e "${GREEN}========================================="
        echo "Deployment Completed Successfully"
        echo "=========================================${NC}"
        echo "Log file: $LOG_FILE"
    else
        echo ""
        echo -e "${RED}========================================="
        echo "Deployment Failed"
        echo "=========================================${NC}"
        echo "Exit code: $exit_code"
        echo "Check log file: $LOG_FILE"

        # Offer rollback option
        read -p "Do you want to rollback? (y/n): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            $SCRIPT_DIR/rollback.sh "$CONFIG_FILE"
        fi
    fi
}

# Main execution
main() {
    # Check arguments
    if [ $# -lt 1 ]; then
        print_usage
        exit 1
    fi

    # Validate config
    if ! validate_config "$CONFIG_FILE"; then
        exit 1
    fi

    # Setup
    setup_logging
    check_database
    build_jar_if_needed

    # Deploy
    run_deployment "$CONFIG_FILE" "$ENVIRONMENT"
    EXIT_CODE=$?

    # Handle result
    handle_result $EXIT_CODE

    exit $EXIT_CODE
}

# Run main function
main "$@"