#!/bin/bash
# Configure Go-ID containers to use Consul
# Assumes Consul cluster is already running

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}=== Configure Go-ID with Consul ===${NC}\n"

# Consul configuration
CONSUL_SERVER="192.168.1.100:8500"  # Any Consul server

# Go-ID shard configuration
# Format: "host_ip:container_name:container_ip:shard_id"
SHARDS=(
  "192.168.1.100:go-id-shard-0:10.200.100.50:0"
  "192.168.1.101:go-id-shard-1:10.200.100.51:1"
  "192.168.1.102:go-id-shard-2:10.200.100.52:2"
)

TOTAL_SHARDS=${#SHARDS[@]}

echo "Configuration:"
echo "  Consul Server: ${CONSUL_SERVER}"
echo "  Total Shards: ${TOTAL_SHARDS}"
echo ""
echo "Shards to configure:"
for shard in "${SHARDS[@]}"; do
  IFS=':' read -r host container ip shard_id <<< "$shard"
  echo "  - ${container} on ${host} (shard ${shard_id})"
done
echo ""

read -p "Proceed with configuration? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
  echo -e "${YELLOW}Configuration cancelled${NC}"
  exit 0
fi

# Configure each shard
for shard in "${SHARDS[@]}"; do
  IFS=':' read -r host_ip container_name container_ip shard_id <<< "$shard"

  echo -e "\n${GREEN}Configuring ${container_name}...${NC}"

  ssh root@${host_ip} bash <<EOF
    echo "  Setting Consul configuration..."
    lxc config set ${container_name} environment.CONSUL_URL "http://${CONSUL_SERVER}"
    lxc config set ${container_name} environment.SERVICE_NAME "go-id"

    echo "  Setting shard configuration..."
    lxc config set ${container_name} environment.SHARD_ID "${shard_id}"
    lxc config set ${container_name} environment.TOTAL_SHARDS "${TOTAL_SHARDS}"
    lxc config set ${container_name} environment.CONTAINER_IP "${container_ip}"
    lxc config set ${container_name} environment.SERVICE_PORT "7010"

    echo "  Restarting container..."
    lxc restart ${container_name}

    echo -e "  ${GREEN}✓ ${container_name} configured and restarted${NC}"
EOF
done

echo -e "\n${YELLOW}Waiting for services to register with Consul...${NC}"
sleep 10

echo -e "\n${GREEN}=== Verifying Consul Registration ===${NC}\n"

# Check service registration
echo "Querying Consul for go-id services..."
curl -s "http://${CONSUL_SERVER}/v1/catalog/service/go-id" | jq -r '.[] | "  - \(.ServiceID): \(.ServiceAddress):\(.ServicePort) [\(.ServiceTags | join(", "))]"'

echo ""
echo -e "${GREEN}Checking health status...${NC}"
HEALTHY=$(curl -s "http://${CONSUL_SERVER}/v1/health/service/go-id?passing" | jq '. | length')
echo "  Healthy shards: ${HEALTHY}/${TOTAL_SHARDS}"

if [ "$HEALTHY" -eq "$TOTAL_SHARDS" ]; then
  echo -e "\n${GREEN}=== All shards healthy and registered! ===${NC}\n"
else
  echo -e "\n${YELLOW}⚠ Some shards are not healthy. Check container logs.${NC}\n"
  echo "Debug commands:"
  for shard in "${SHARDS[@]}"; do
    IFS=':' read -r host container ip shard_id <<< "$shard"
    echo "  ssh root@${host} 'lxc exec ${container} -- journalctl -u go-id -n 50'"
  done
  echo ""
fi

echo "Consul UI: http://${CONSUL_SERVER}/ui/dc1/services/go-id"
echo ""
echo "Test service discovery:"
echo "  curl http://${CONSUL_SERVER}/v1/health/service/go-id?passing | jq"
echo ""
