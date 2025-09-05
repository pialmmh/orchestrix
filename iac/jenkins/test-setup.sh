#!/bin/bash

# Test Script for Clarity LXC Jenkins Setup
# This script verifies all components are properly configured

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Test results
TESTS_PASSED=0
TESTS_FAILED=0

# Function to run a test
run_test() {
    local test_name="$1"
    local test_command="$2"
    
    echo -ne "${YELLOW}Testing: ${test_name}...${NC} "
    
    if eval "$test_command" &>/dev/null; then
        echo -e "${GREEN}✓ PASSED${NC}"
        ((TESTS_PASSED++))
        return 0
    else
        echo -e "${RED}✗ FAILED${NC}"
        ((TESTS_FAILED++))
        return 1
    fi
}

# Function to check command exists
check_command() {
    command -v "$1" &> /dev/null
}

echo -e "${BLUE}=== Clarity LXC Jenkins Setup Verification ===${NC}"
echo ""

# 1. Check required commands
echo -e "${BLUE}[1/6] Checking Required Tools${NC}"
run_test "Java installed" "check_command java"
run_test "LXC installed" "check_command lxc"
run_test "rclone installed" "check_command rclone"
run_test "curl installed" "check_command curl"
run_test "jq installed" "check_command jq"
echo ""

# 2. Check LXC/LXD setup
echo -e "${BLUE}[2/6] Checking LXC/LXD Configuration${NC}"
run_test "LXD is initialized" "sudo lxc list &>/dev/null"
run_test "Can create containers" "sudo lxc info | grep -q 'server_version'"
echo ""

# 3. Check build script
echo -e "${BLUE}[3/6] Checking Build Scripts${NC}"
BUILD_DIR="/home/mustafa/telcobright-projects/orchestrix/images/lxc/clarity"
run_test "Build directory exists" "[ -d ${BUILD_DIR} ]"
run_test "buildClarity.sh exists" "[ -f ${BUILD_DIR}/buildClarity.sh ]"
run_test "buildClarity.sh is executable" "[ -x ${BUILD_DIR}/buildClarity.sh ]"
run_test "Config file exists" "[ -f ${BUILD_DIR}/buildClarityConfig.cnf ]"
echo ""

# 4. Check rclone configuration
echo -e "${BLUE}[4/6] Checking rclone Configuration${NC}"
run_test "rclone config exists" "[ -f ~/.config/rclone/rclone.conf ]"
run_test "pialmmhtb remote configured" "rclone listremotes | grep -q 'pialmmhtb:'"

# Test rclone access (optional - may take time)
echo -ne "${YELLOW}Testing: Google Drive access...${NC} "
if timeout 10 rclone lsd pialmmhtb: &>/dev/null; then
    echo -e "${GREEN}✓ PASSED${NC}"
    ((TESTS_PASSED++))
else
    echo -e "${YELLOW}⚠ SKIPPED (may be offline)${NC}"
fi
echo ""

# 5. Check Jenkins files
echo -e "${BLUE}[5/6] Checking Jenkins Files${NC}"
JENKINS_DIR="/home/mustafa/telcobright-projects/orchestrix/jenkins"
run_test "Jenkins directory exists" "[ -d ${JENKINS_DIR} ]"
run_test "Jenkinsfile exists" "[ -f ${JENKINS_DIR}/Jenkinsfile ]"
run_test "Agent setup script exists" "[ -f ${JENKINS_DIR}/setup-jenkins-agent.sh ]"
run_test "Job install script exists" "[ -f ${JENKINS_DIR}/install-jenkins-job.sh ]"
echo ""

# 6. Check sudo permissions
echo -e "${BLUE}[6/6] Checking Permissions${NC}"
run_test "Can run lxc with sudo" "sudo -n lxc list &>/dev/null || sudo lxc list &>/dev/null"
run_test "Build script sudo access" "sudo -n ls ${BUILD_DIR}/buildClarity.sh &>/dev/null || sudo ls ${BUILD_DIR}/buildClarity.sh &>/dev/null"
echo ""

# 7. Optional: Test container build (takes time)
echo -e "${YELLOW}Would you like to test building the container? This will take 5-10 minutes. (y/n)${NC}"
read -r response

if [[ "$response" =~ ^[Yy]$ ]]; then
    echo -e "${BLUE}Testing Container Build${NC}"
    
    # Check if container exists
    if sudo lxc list | grep -q "clarity"; then
        echo -e "${YELLOW}Container 'clarity' already exists. Skipping build test.${NC}"
        echo -e "${YELLOW}To test rebuild, run: sudo lxc delete --force clarity${NC}"
    else
        echo -e "${YELLOW}Building container (this may take several minutes)...${NC}"
        if sudo ${BUILD_DIR}/buildClarity.sh; then
            echo -e "${GREEN}✓ Container built successfully${NC}"
            ((TESTS_PASSED++))
            
            # Check container status
            echo -e "${YELLOW}Checking container status...${NC}"
            sudo lxc list clarity
            
            # Get container IP
            CONTAINER_IP=$(sudo lxc list clarity --format=json | jq -r '.[0].state.network.eth0.addresses[] | select(.family=="inet").address')
            echo -e "${GREEN}Container IP: ${CONTAINER_IP}${NC}"
            
            # Test services
            echo -e "${YELLOW}Testing services...${NC}"
            sleep 5
            curl -f http://${CONTAINER_IP}:9090/-/healthy && echo -e "${GREEN}✓ Prometheus is running${NC}"
            curl -f http://${CONTAINER_IP}:3300/api/health && echo -e "${GREEN}✓ Grafana is running${NC}"
        else
            echo -e "${RED}✗ Container build failed${NC}"
            ((TESTS_FAILED++))
        fi
    fi
fi

# Summary
echo ""
echo -e "${BLUE}=== Test Summary ===${NC}"
echo -e "Tests Passed: ${GREEN}${TESTS_PASSED}${NC}"
echo -e "Tests Failed: ${RED}${TESTS_FAILED}${NC}"

if [ ${TESTS_FAILED} -eq 0 ]; then
    echo -e "${GREEN}✓ All tests passed! Your setup is ready.${NC}"
    echo ""
    echo -e "${BLUE}Next Steps:${NC}"
    echo "1. Setup Jenkins agent: ${JENKINS_DIR}/setup-jenkins-agent.sh"
    echo "2. Configure Jenkins credentials in the scripts"
    echo "3. Install Jenkins job: ${JENKINS_DIR}/install-jenkins-job.sh"
    echo "4. Monitor builds in Jenkins UI"
else
    echo -e "${RED}✗ Some tests failed. Please fix the issues above.${NC}"
    exit 1
fi