#!/bin/bash
# One-Click FRR Deployment to Link3 Cluster
# Deploys and configures FRR BGP mesh on all 3 Link3 nodes

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Link3 node configurations
declare -A NODE1=(
    [name]="node1"
    [hostname]="SMSAppDBmaster"
    [host]="123.200.0.50"
    [port]="8210"
    [user]="tbsms"
    [password]="TB@l38800"
    [router_id]="123.200.0.50"
    [bgp_asn]="65196"
    [subnet]="10.10.196.0/24"
)

declare -A NODE2=(
    [name]="node2"
    [hostname]="spark"
    [host]="123.200.0.117"
    [port]="8210"
    [user]="tbsms"
    [password]="TB@l38800"
    [router_id]="123.200.0.117"
    [bgp_asn]="65195"
    [subnet]="10.10.195.0/24"
)

declare -A NODE3=(
    [name]="node3"
    [hostname]="SMSApplication"
    [host]="123.200.0.51"
    [port]="8210"
    [user]="tbsms"
    [password]="TB@l38800"
    [router_id]="123.200.0.51"
    [bgp_asn]="65194"
    [subnet]="10.10.194.0/24"
)

echo "========================================"
echo "FRR BGP Deployment - Link3 Cluster"
echo "========================================"
echo ""

# Function to deploy FRR to a single node
deploy_to_node() {
    local -n node=$1

    echo "Deploying to ${node[hostname]} (${node[host]})..."
    echo ""

    # Generate install script
    local install_script="/tmp/frr-install-${node[name]}.sh"
    cat "$SCRIPT_DIR/images/shell-automations/frr/v1/templates/frr-install.sh.template" | \
        sed "s/{{TIMESTAMP}}/$TIMESTAMP/g" | \
        sed "s/{{HOSTNAME}}/${node[hostname]}/g" | \
        sed "s/{{HOST_IP}}/${node[host]}/g" > "$install_script"

    # Generate BGP neighbor configuration
    local neighbor_config=""
    for peer_name in NODE1 NODE2 NODE3; do
        local -n peer=$peer_name
        if [ "${peer[name]}" != "${node[name]}" ]; then
            neighbor_config+=" neighbor ${peer[router_id]} peer-group BGP_PEERS\n"
            neighbor_config+=" neighbor ${peer[router_id]} remote-as ${peer[bgp_asn]}\n"
        fi
    done

    # Generate configure script
    local config_script="/tmp/frr-configure-${node[name]}.sh"
    cat "$SCRIPT_DIR/images/shell-automations/frr/v1/templates/frr-configure.sh.template" | \
        sed "s/{{TIMESTAMP}}/$TIMESTAMP/g" | \
        sed "s/{{HOSTNAME}}/${node[hostname]}/g" | \
        sed "s/{{HOST_IP}}/${node[host]}/g" | \
        sed "s/{{ROUTER_ID}}/${node[router_id]}/g" | \
        sed "s/{{BGP_ASN}}/${node[bgp_asn]}/g" | \
        sed "s|{{BGP_NEIGHBOR_CONFIG}}|$neighbor_config|g" | \
        sed "s|{{NETWORK_CONFIG}}| network ${node[subnet]}|g" > "$config_script"

    # Upload and execute install script
    echo "[${node[name]}] Installing FRR..."
    sshpass -p "${node[password]}" scp -P "${node[port]}" \
        -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o LogLevel=ERROR \
        "$install_script" "${node[user]}@${node[host]}:/tmp/frr-install.sh"

    sshpass -p "${node[password]}" ssh -p "${node[port]}" \
        -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o LogLevel=ERROR \
        "${node[user]}@${node[host]}" \
        "chmod +x /tmp/frr-install.sh && echo '${node[password]}' | sudo -S /tmp/frr-install.sh" | sed "s/^/[${node[name]}] /"

    echo ""

    # Upload and execute configure script
    echo "[${node[name]}] Configuring BGP..."
    sshpass -p "${node[password]}" scp -P "${node[port]}" \
        -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o LogLevel=ERROR \
        "$config_script" "${node[user]}@${node[host]}:/tmp/frr-configure.sh"

    sshpass -p "${node[password]}" ssh -p "${node[port]}" \
        -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o LogLevel=ERROR \
        "${node[user]}@${node[host]}" \
        "chmod +x /tmp/frr-configure.sh && echo '${node[password]}' | sudo -S /tmp/frr-configure.sh" | sed "s/^/[${node[name]}] /"

    echo ""
    echo "[${node[name]}] ✓ Deployment complete"
    echo ""

    # Cleanup local temp files
    rm -f "$install_script" "$config_script"
}

# Deploy to all nodes
for node_ref in NODE1 NODE2 NODE3; do
    deploy_to_node $node_ref
    echo "=========================================="
    echo ""
done

echo "========================================"
echo "All Deployments Complete!"
echo "========================================"
echo ""
echo "Verify BGP peering:"
echo "  ssh -p 8210 tbsms@123.200.0.50 \"sudo vtysh -c 'show ip bgp summary'\""
echo "  ssh -p 8210 tbsms@123.200.0.117 \"sudo vtysh -c 'show ip bgp summary'\""
echo "  ssh -p 8210 tbsms@123.200.0.51 \"sudo vtysh -c 'show ip bgp summary'\""
echo ""
