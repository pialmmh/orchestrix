#!/bin/bash
# Example Usage Guide for Kafka Cluster Deployment
# This script demonstrates common workflows

echo "=========================================="
echo "Kafka Cluster Deployment - Usage Examples"
echo "=========================================="
echo ""
echo "This script shows common workflows. Do not run this script directly."
echo "Copy and paste the commands you need."
echo ""
echo "=========================================="
echo ""

cat << 'EOF'

## WORKFLOW 1: Fresh Deployment
## ========================================

# Step 1: Edit configuration
nano cluster-config.conf

# Step 2: Deploy cluster
bash deploy-cluster.sh

# Step 3: Verify deployment
bash verify-cluster.sh


## WORKFLOW 2: Check Cluster Status
## ========================================

# Show all node status
bash manage-cluster.sh status

# Show specific node logs
bash manage-cluster.sh logs 1


## WORKFLOW 3: Restart a Node
## ========================================

# Restart all nodes (rolling restart recommended)
bash manage-cluster.sh restart 1
sleep 30
bash manage-cluster.sh restart 2
sleep 30
bash manage-cluster.sh restart 3

# Or restart specific node
bash manage-cluster.sh restart 2


## WORKFLOW 4: Stop/Start Cluster
## ========================================

# Stop all nodes
bash manage-cluster.sh stop

# Start all nodes
bash manage-cluster.sh start

# Check status
bash manage-cluster.sh status


## WORKFLOW 5: Test Cluster with Topic
## ========================================

# Run on any BDCOM server (connects to node 1)
ssh -p 15605 bdcom@10.255.246.173

# Create topic
sudo docker exec kafka-node kafka-topics \
  --create \
  --topic my-test-topic \
  --bootstrap-server 10.10.199.20:9092,10.10.198.20:9092,10.10.197.20:9092 \
  --replication-factor 3 \
  --partitions 6

# Describe topic
sudo docker exec kafka-node kafka-topics \
  --describe \
  --topic my-test-topic \
  --bootstrap-server 10.10.199.20:9092

# Produce messages
echo "Hello Kafka" | sudo docker exec -i kafka-node kafka-console-producer \
  --topic my-test-topic \
  --bootstrap-server 10.10.199.20:9092

# Consume messages
sudo docker exec kafka-node kafka-console-consumer \
  --topic my-test-topic \
  --from-beginning \
  --max-messages 10 \
  --bootstrap-server 10.10.199.20:9092


## WORKFLOW 6: Reconfigure a Single Node
## ========================================

# Edit config for node changes
nano cluster-config.conf

# Stop the node
bash manage-cluster.sh stop 2

# Remove old configuration
ssh -p 15605 bdcom@10.255.246.174
sudo ip addr del 10.10.198.20/24 dev lxdbr0
exit

# Reconfigure node
bash setup-node.sh 2

# Verify
bash verify-cluster.sh


## WORKFLOW 7: Complete Cleanup and Redeploy
## ========================================

# Clean up (keep data for faster restart)
bash cleanup-cluster.sh
# When prompted: yes to cleanup, no to delete data

# Make configuration changes
nano cluster-config.conf

# Redeploy
bash deploy-cluster.sh

# Verify
bash verify-cluster.sh


## WORKFLOW 8: Monitor Cluster Health
## ========================================

# Watch cluster status (run in terminal)
watch -n 5 'bash manage-cluster.sh status'

# Check broker connectivity
bash verify-cluster.sh

# Tail logs from specific node
bash manage-cluster.sh logs 1 | tail -f


## WORKFLOW 9: Troubleshooting Network Issues
## ========================================

# Check if Kafka IPs are assigned
ssh -p 15605 bdcom@10.255.246.173 'ip addr show lxdbr0 | grep inet'
ssh -p 15605 bdcom@10.255.246.174 'ip addr show lxdbr0 | grep inet'
ssh -p 15605 bdcom@10.255.246.175 'ip addr show lxdbr0 | grep inet'

# Test cross-host connectivity
ssh -p 15605 bdcom@10.255.246.173 'ping -c 3 10.10.198.20'
ssh -p 15605 bdcom@10.255.246.173 'ping -c 3 10.10.197.20'

# Check BGP routes
ssh -p 15605 bdcom@10.255.246.173 'ip route show proto bgp'

# Check if Kafka ports are listening
ssh -p 15605 bdcom@10.255.246.173 'nc -zv 10.10.199.20 9092'
ssh -p 15605 bdcom@10.255.246.174 'nc -zv 10.10.198.20 9092'
ssh -p 15605 bdcom@10.255.246.175 'nc -zv 10.10.197.20 9092'


## WORKFLOW 10: Production Operations
## ========================================

# Backup data from a node (before maintenance)
ssh -p 15605 bdcom@10.255.246.173
sudo tar -czf ~/kafka-node-1-backup-$(date +%Y%m%d).tar.gz /var/lib/kafka/data
exit

# Take node offline for maintenance
bash manage-cluster.sh stop 1

# Perform maintenance...

# Bring node back online
bash manage-cluster.sh start 1

# Verify cluster health
bash verify-cluster.sh

# List all topics
ssh -p 15605 bdcom@10.255.246.173 \
  'sudo docker exec kafka-node kafka-topics --list --bootstrap-server 10.10.199.20:9092'

# Check consumer groups
ssh -p 15605 bdcom@10.255.246.173 \
  'sudo docker exec kafka-node kafka-consumer-groups --list --bootstrap-server 10.10.199.20:9092'

# Describe consumer group
ssh -p 15605 bdcom@10.255.246.173 \
  'sudo docker exec kafka-node kafka-consumer-groups --describe --group my-group --bootstrap-server 10.10.199.20:9092'


## USEFUL ONE-LINERS
## ========================================

# Quick health check
bash verify-cluster.sh 2>&1 | grep -E "(OK|FAILED|✓|✗)"

# Get all broker IPs
grep KAFKA_IP cluster-config.conf | grep -v "^#" | cut -d'=' -f2 | tr -d '"'

# Check all containers status
for i in 1 2 3; do echo "Node $i:"; bash manage-cluster.sh status 2>/dev/null | grep kafka-node; done

# Show bootstrap servers string
echo "Bootstrap servers: $(grep KAFKA_IP cluster-config.conf | grep -v '^#' | cut -d'=' -f2 | tr -d '"' | tr '\n' ',' | sed 's/,$//' | sed 's/,/:9092,/g'):9092"

EOF

echo ""
echo "=========================================="
echo "End of Examples"
echo "=========================================="
