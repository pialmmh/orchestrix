#!/bin/bash

# Manual Build Trigger for Clarity LXC Container
# This script triggers a manual build with version parameters

set -e

# Load configuration from jenkins-config.yml
CONFIG_FILE="$(dirname "$0")/jenkins-config.yml"

if [ -f "$CONFIG_FILE" ]; then
    JENKINS_URL=$(grep "url:" "$CONFIG_FILE" | head -1 | awk '{print $2}' | tr -d '"')
    JENKINS_USER=$(grep "username:" "$CONFIG_FILE" | awk '{print $2}' | tr -d '"')
    JENKINS_TOKEN=$(grep "api_token:" "$CONFIG_FILE" | awk '{print $2}' | tr -d '"')
    echo -e "${GREEN}Loaded configuration from jenkins-config.yml${NC}"
else
    # Fallback to environment variables or defaults
    JENKINS_URL="${JENKINS_URL:-http://your-jenkins-server:8080}"
    JENKINS_USER="${JENKINS_USER:-admin}"
    JENKINS_TOKEN="${JENKINS_TOKEN:-your-api-token}"
    echo -e "${YELLOW}Using default configuration (jenkins-config.yml not found)${NC}"
fi

JOB_NAME="clarity-lxc-manual-build"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Default values
DEFAULT_VERSION="1.0.0"
DEFAULT_BUILD_TYPE="DEVELOPMENT"

echo -e "${BLUE}=== Clarity LXC Manual Build Trigger ===${NC}"
echo ""

# Function to show usage
show_usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -v, --version VERSION      Version number (default: ${DEFAULT_VERSION})"
    echo "  -t, --type TYPE           Build type: RELEASE|SNAPSHOT|DEVELOPMENT (default: ${DEFAULT_BUILD_TYPE})"
    echo "  -c, --clean               Clean build (delete existing container)"
    echo "  -u, --upload              Upload to Google Drive (default: true)"
    echo "  -n, --no-upload           Don't upload to Google Drive"
    echo "  -k, --keep-running        Keep container running after build (default: true)"
    echo "  -s, --stop                Stop container after build"
    echo "  -r, --release-notes TEXT  Release notes for this version"
    echo "  -w, --wait                Wait for build to complete"
    echo "  -h, --help                Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 -v 1.2.0 -t RELEASE -c -r \"Production release with monitoring improvements\""
    echo "  $0 -v 2.0.0-beta -t SNAPSHOT -n"
    echo "  $0 -v 1.0.1 -t DEVELOPMENT --no-upload"
    exit 0
}

# Parse arguments
VERSION="${DEFAULT_VERSION}"
BUILD_TYPE="${DEFAULT_BUILD_TYPE}"
CLEAN_BUILD="false"
UPLOAD_TO_GDRIVE="true"
KEEP_RUNNING="true"
RELEASE_NOTES=""
WAIT_FOR_BUILD="false"

while [[ $# -gt 0 ]]; do
    case $1 in
        -v|--version)
            VERSION="$2"
            shift 2
            ;;
        -t|--type)
            BUILD_TYPE="$2"
            if [[ ! "$BUILD_TYPE" =~ ^(RELEASE|SNAPSHOT|DEVELOPMENT)$ ]]; then
                echo -e "${RED}Invalid build type: $BUILD_TYPE${NC}"
                echo "Valid types: RELEASE, SNAPSHOT, DEVELOPMENT"
                exit 1
            fi
            shift 2
            ;;
        -c|--clean)
            CLEAN_BUILD="true"
            shift
            ;;
        -u|--upload)
            UPLOAD_TO_GDRIVE="true"
            shift
            ;;
        -n|--no-upload)
            UPLOAD_TO_GDRIVE="false"
            shift
            ;;
        -k|--keep-running)
            KEEP_RUNNING="true"
            shift
            ;;
        -s|--stop)
            KEEP_RUNNING="false"
            shift
            ;;
        -r|--release-notes)
            RELEASE_NOTES="$2"
            shift 2
            ;;
        -w|--wait)
            WAIT_FOR_BUILD="true"
            shift
            ;;
        -h|--help)
            show_usage
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            show_usage
            ;;
    esac
done

# Check Jenkins connectivity
echo -e "${YELLOW}Checking Jenkins connectivity...${NC}"
if ! curl -s -o /dev/null -w "%{http_code}" ${JENKINS_URL}/api/json | grep -q "200"; then
    echo -e "${RED}Cannot reach Jenkins at ${JENKINS_URL}${NC}"
    echo "Please update JENKINS_URL in this script or set it as environment variable"
    exit 1
fi

# Display build parameters
echo -e "${BLUE}Build Parameters:${NC}"
echo "  Version: ${VERSION}"
echo "  Build Type: ${BUILD_TYPE}"
echo "  Clean Build: ${CLEAN_BUILD}"
echo "  Upload to GDrive: ${UPLOAD_TO_GDRIVE}"
echo "  Keep Running: ${KEEP_RUNNING}"
if [ -n "${RELEASE_NOTES}" ]; then
    echo "  Release Notes: ${RELEASE_NOTES}"
fi
echo ""

# Confirm build
echo -e "${YELLOW}Do you want to trigger this build? (y/n)${NC}"
read -r response

if [[ ! "$response" =~ ^[Yy]$ ]]; then
    echo "Build cancelled"
    exit 0
fi

# Trigger build
echo -e "${YELLOW}Triggering build...${NC}"

# Build parameters JSON
PARAMS=$(cat <<EOF
{
    "parameter": [
        {"name": "VERSION", "value": "${VERSION}"},
        {"name": "BUILD_TYPE", "value": "${BUILD_TYPE}"},
        {"name": "CLEAN_BUILD", "value": ${CLEAN_BUILD}},
        {"name": "UPLOAD_TO_GDRIVE", "value": ${UPLOAD_TO_GDRIVE}},
        {"name": "KEEP_CONTAINER_RUNNING", "value": ${KEEP_RUNNING}},
        {"name": "RELEASE_NOTES", "value": "${RELEASE_NOTES}"}
    ]
}
EOF
)

# Trigger the build
BUILD_RESPONSE=$(curl -X POST \
    -u ${JENKINS_USER}:${JENKINS_TOKEN} \
    -H "Content-Type: application/json" \
    -d "${PARAMS}" \
    -s -i \
    "${JENKINS_URL}/job/${JOB_NAME}/buildWithParameters")

# Check if build was triggered
if echo "${BUILD_RESPONSE}" | grep -q "201 Created"; then
    echo -e "${GREEN}✅ Build triggered successfully!${NC}"
    
    # Extract queue location
    QUEUE_URL=$(echo "${BUILD_RESPONSE}" | grep "Location:" | cut -d' ' -f2 | tr -d '\r')
    
    if [ -n "${QUEUE_URL}" ]; then
        echo -e "${YELLOW}Queue URL: ${QUEUE_URL}${NC}"
        
        # Wait for build to start
        echo -e "${YELLOW}Waiting for build to start...${NC}"
        sleep 5
        
        # Get build number
        BUILD_INFO=$(curl -s -u ${JENKINS_USER}:${JENKINS_TOKEN} "${QUEUE_URL}api/json")
        BUILD_NUMBER=$(echo "${BUILD_INFO}" | jq -r '.executable.number // empty')
        
        if [ -n "${BUILD_NUMBER}" ]; then
            BUILD_URL="${JENKINS_URL}/job/${JOB_NAME}/${BUILD_NUMBER}"
            echo -e "${GREEN}Build #${BUILD_NUMBER} started${NC}"
            echo -e "${GREEN}Build URL: ${BUILD_URL}${NC}"
            echo -e "${GREEN}Console: ${BUILD_URL}/console${NC}"
            
            if [ "${WAIT_FOR_BUILD}" = "true" ]; then
                echo -e "${YELLOW}Waiting for build to complete...${NC}"
                
                # Poll build status
                while true; do
                    STATUS=$(curl -s -u ${JENKINS_USER}:${JENKINS_TOKEN} \
                        "${BUILD_URL}/api/json" | jq -r '.result // "null"')
                    
                    if [ "${STATUS}" != "null" ]; then
                        if [ "${STATUS}" = "SUCCESS" ]; then
                            echo -e "${GREEN}✅ Build completed successfully!${NC}"
                        else
                            echo -e "${RED}❌ Build failed with status: ${STATUS}${NC}"
                        fi
                        break
                    fi
                    
                    echo -n "."
                    sleep 10
                done
            fi
        else
            echo -e "${YELLOW}Build queued. Check Jenkins for status.${NC}"
        fi
    fi
else
    echo -e "${RED}❌ Failed to trigger build${NC}"
    echo "Response: ${BUILD_RESPONSE}"
    exit 1
fi

echo ""
echo -e "${BLUE}=== Build Information ===${NC}"
echo "Version Tag: ${VERSION}-${BUILD_TYPE}"
echo "Expected backup: clarity-v${VERSION}-${BUILD_TYPE}-*.tar.gz"
if [ "${UPLOAD_TO_GDRIVE}" = "true" ]; then
    echo "Google Drive location: pialmmhtb:orchestrix/lxc-images/clarity/${VERSION}/"
fi