#!/bin/bash
#
# Interactive Deployment Runner for Consul
# Allows selection of deployment scenario
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOYMENT_DIR="${SCRIPT_DIR}/deployments"
DEPLOY_SCRIPT="/home/mustafa/telcobright-projects/orchestrix/deployment/deploy-simple.sh"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

clear
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}           Consul Deployment Runner                    ${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo ""
echo "Available Deployment Scenarios:"
echo ""

# List deployment options
echo -e "${GREEN}1)${NC} Local Single Node    - Quick dev instance on port 8500"
echo -e "${GREEN}2)${NC} Local 3-Node Cluster - Full cluster (ports 8500,8510,8520)"
echo -e "${GREEN}3)${NC} Production Cluster   - Deploy to 3 servers"
echo -e "${GREEN}4)${NC} Custom YAML         - Select your own YAML file"
echo -e "${GREEN}5)${NC} List Containers     - Show currently running Consul containers"
echo -e "${GREEN}6)${NC} Stop All           - Stop all Consul containers"
echo ""
echo -e "${YELLOW}0)${NC} Exit"
echo ""
echo -n "Select deployment option [1-6]: "
read -r option

case $option in
    1)
        echo ""
        echo -e "${BLUE}→ Deploying Local Single Node...${NC}"
        echo "  Configuration: 1 node, port 8500, dev mode"
        echo ""
        read -p "Continue? (y/n): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            ${DEPLOY_SCRIPT} ${DEPLOYMENT_DIR}/local-single.yml
        fi
        ;;

    2)
        echo ""
        echo -e "${BLUE}→ Deploying Local 3-Node Cluster...${NC}"
        echo "  Configuration: 3 nodes, ports 8500/8510/8520"
        echo ""
        read -p "Continue? (y/n): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            ${DEPLOY_SCRIPT} ${DEPLOYMENT_DIR}/local-3node-cluster.yml
        fi
        ;;

    3)
        echo ""
        echo -e "${BLUE}→ Production Cluster Deployment${NC}"
        echo "  Target servers: 10.0.1.10, 10.0.1.11, 10.0.1.12"
        echo ""
        echo -e "${YELLOW}WARNING: This will deploy to production servers!${NC}"
        read -p "Are you sure? (yes/no): " confirm
        if [ "$confirm" = "yes" ]; then
            ${DEPLOY_SCRIPT} ${DEPLOYMENT_DIR}/production-cluster.yml
        else
            echo "Deployment cancelled."
        fi
        ;;

    4)
        echo ""
        echo "Available YAML files in ${DEPLOYMENT_DIR}:"
        echo ""
        select yaml_file in ${DEPLOYMENT_DIR}/*.yml; do
            if [ -n "$yaml_file" ]; then
                echo ""
                echo -e "${BLUE}→ Selected: $(basename $yaml_file)${NC}"
                echo ""
                cat $yaml_file | head -20
                echo "..."
                echo ""
                read -p "Deploy this configuration? (y/n): " -n 1 -r
                echo
                if [[ $REPLY =~ ^[Yy]$ ]]; then
                    ${DEPLOY_SCRIPT} $yaml_file
                fi
                break
            fi
        done
        ;;

    5)
        echo ""
        echo -e "${BLUE}Current Consul Containers:${NC}"
        echo ""
        lxc list | grep consul || echo "No Consul containers running"
        echo ""
        read -p "Press enter to continue..."
        $0  # Re-run this script
        ;;

    6)
        echo ""
        echo -e "${RED}Stopping all Consul containers...${NC}"
        echo ""
        containers=$(lxc list --format csv -c n | grep consul || true)
        if [ -z "$containers" ]; then
            echo "No Consul containers found."
        else
            echo "Found containers:"
            echo "$containers"
            echo ""
            read -p "Stop all these containers? (y/n): " -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                for container in $containers; do
                    echo "Stopping $container..."
                    lxc stop $container --force
                done
                echo -e "${GREEN}All containers stopped.${NC}"
            fi
        fi
        ;;

    0)
        echo "Exiting..."
        exit 0
        ;;

    *)
        echo -e "${RED}Invalid option${NC}"
        exit 1
        ;;
esac

echo ""
echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
echo "Deployment runner completed."

# Option to check cluster status after deployment
if [ "$option" = "2" ] || [ "$option" = "3" ]; then
    echo ""
    read -p "Check cluster status? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo ""
        echo "Cluster Members:"
        lxc exec consul-node-1 -- consul members 2>/dev/null || lxc exec consul-prod-1 -- consul members 2>/dev/null || echo "Could not check cluster status"
    fi
fi