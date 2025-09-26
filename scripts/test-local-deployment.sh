#!/bin/bash
# Test script for local BTRFS container deployment on Ubuntu 24.04

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "========================================="
echo "Local BTRFS Container Deployment Test"
echo "========================================="
echo "System: Ubuntu 24.04 LTS"
echo "User: $(whoami)"
echo "========================================="

# Check if running on Ubuntu 24.04
UBUNTU_VERSION=$(lsb_release -rs)
if [ "$UBUNTU_VERSION" != "24.04" ]; then
    echo "Warning: This script is designed for Ubuntu 24.04 (found: $UBUNTU_VERSION)"
    read -p "Continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Function to check prerequisites
check_prerequisites() {
    echo "Checking prerequisites..."

    # Check BTRFS
    if ! command -v btrfs &> /dev/null; then
        echo "✗ BTRFS not installed"
        echo "  Run: sudo $SCRIPT_DIR/setup-btrfs-local.sh"
        return 1
    else
        echo "✓ BTRFS installed"
    fi

    # Check storage configuration
    if [ ! -f "/etc/orchestrix/storage-locations.conf" ]; then
        echo "✗ Storage configuration not found"
        echo "  Run: sudo $SCRIPT_DIR/setup-btrfs-local.sh"
        return 1
    else
        echo "✓ Storage configuration exists"
    fi

    # Check LXD
    if ! command -v lxc &> /dev/null; then
        echo "✗ LXD not installed"
        echo "  Run: sudo $SCRIPT_DIR/setup-btrfs-local.sh"
        return 1
    else
        echo "✓ LXD installed"
    fi

    # Check SSH
    if ! systemctl is-active --quiet ssh && ! systemctl is-active --quiet sshd; then
        echo "✗ SSH service not running"
        echo "  Run: sudo systemctl start ssh"
        return 1
    else
        echo "✓ SSH service running"
    fi

    # Check SSH key
    if [ ! -f "$HOME/.ssh/id_rsa" ]; then
        echo "✗ SSH key not found"
        echo "  Generate with: ssh-keygen -t rsa"
        return 1
    else
        echo "✓ SSH key exists"
    fi

    # Check if key is in authorized_keys
    if [ ! -f "$HOME/.ssh/authorized_keys" ] || ! grep -q "$(cat $HOME/.ssh/id_rsa.pub)" "$HOME/.ssh/authorized_keys" 2>/dev/null; then
        echo "✗ SSH key not in authorized_keys"
        echo "  Run: cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys"
        return 1
    else
        echo "✓ SSH key authorized"
    fi

    return 0
}

# Function to setup prerequisites
setup_prerequisites() {
    echo ""
    echo "Setting up prerequisites..."

    # Run BTRFS setup script
    if [ ! -f "/etc/orchestrix/storage-locations.conf" ]; then
        echo "Running BTRFS setup script..."
        sudo "$SCRIPT_DIR/setup-btrfs-local.sh"
    fi

    # Ensure SSH service is running
    if ! systemctl is-active --quiet ssh && ! systemctl is-active --quiet sshd; then
        echo "Starting SSH service..."
        sudo systemctl start ssh || sudo systemctl start sshd
    fi

    # Generate SSH key if needed
    if [ ! -f "$HOME/.ssh/id_rsa" ]; then
        echo "Generating SSH key..."
        ssh-keygen -t rsa -N "" -f "$HOME/.ssh/id_rsa"
    fi

    # Add to authorized_keys
    if [ ! -f "$HOME/.ssh/authorized_keys" ] || ! grep -q "$(cat $HOME/.ssh/id_rsa.pub)" "$HOME/.ssh/authorized_keys" 2>/dev/null; then
        echo "Adding SSH key to authorized_keys..."
        mkdir -p "$HOME/.ssh"
        chmod 700 "$HOME/.ssh"
        cat "$HOME/.ssh/id_rsa.pub" >> "$HOME/.ssh/authorized_keys"
        chmod 600 "$HOME/.ssh/authorized_keys"
    fi

    # Test SSH connection
    echo "Testing SSH connection to localhost..."
    if ssh -o StrictHostKeyChecking=no localhost whoami > /dev/null 2>&1; then
        echo "✓ SSH connection successful"
    else
        echo "✗ SSH connection failed"
        return 1
    fi
}

# Main menu
main_menu() {
    echo ""
    echo "Select operation:"
    echo "1. Check prerequisites"
    echo "2. Setup prerequisites (if needed)"
    echo "3. Run test deployment"
    echo "4. Deploy unique-id-generator"
    echo "5. Deploy custom container"
    echo "6. View deployed containers"
    echo "7. Clean up test containers"
    echo "0. Exit"
    echo ""
    read -p "Choice: " choice

    case $choice in
        1)
            check_prerequisites
            ;;
        2)
            setup_prerequisites
            ;;
        3)
            run_test_deployment
            ;;
        4)
            deploy_unique_id_generator
            ;;
        5)
            deploy_custom_container
            ;;
        6)
            view_containers
            ;;
        7)
            cleanup_containers
            ;;
        0)
            exit 0
            ;;
        *)
            echo "Invalid choice"
            ;;
    esac
}

# Run test deployment
run_test_deployment() {
    echo ""
    echo "Running test deployment..."

    if ! check_prerequisites; then
        echo "Prerequisites not met. Please run setup first."
        return 1
    fi

    cd "$PROJECT_ROOT"

    # Compile if needed
    if [ ! -d "target/classes" ] && [ ! -d "build/classes" ]; then
        echo "Compiling project..."
        if [ -f "pom.xml" ]; then
            mvn compile
        elif [ -f "build.gradle" ]; then
            gradle compileJava
        fi
    fi

    # Run the test
    CONFIG_FILE="$PROJECT_ROOT/src/test/resources/testrunner/local-btrfs-deployment.properties"

    if [ ! -f "$CONFIG_FILE" ]; then
        echo "Configuration file not found: $CONFIG_FILE"
        return 1
    fi

    echo "Using configuration: $CONFIG_FILE"
    echo ""

    # Set classpath
    if [ -d "target/classes" ]; then
        CLASSPATH="target/classes:target/test-classes"
    elif [ -d "build/classes" ]; then
        CLASSPATH="build/classes/java/main:build/classes/java/test"
    fi

    # Add resources
    CLASSPATH="$CLASSPATH:src/main/resources:src/test/resources"

    # Add libraries
    if [ -d "lib" ]; then
        CLASSPATH="$CLASSPATH:lib/*"
    fi

    export CLASSPATH

    # Run the deployment
    java com.telcobright.orchestrix.testrunner.storage.LocalBtrfsDeploymentRunner "$CONFIG_FILE"
}

# Deploy unique-id-generator
deploy_unique_id_generator() {
    echo ""
    echo "Deploying Unique ID Generator..."

    # First check if image exists
    if [ ! -f "/tmp/unique-id-generator-base.tar.gz" ]; then
        echo "Image not found. Building it first..."
        "$PROJECT_ROOT/images/lxc/unique-id-generator/buildUniqueIdGenerator.sh"
    fi

    CONFIG_FILE="$PROJECT_ROOT/src/test/resources/testrunner/unique-id-local-deployment.properties"

    # Run deployment
    cd "$PROJECT_ROOT"
    java com.telcobright.orchestrix.testrunner.storage.LocalBtrfsDeploymentRunner "$CONFIG_FILE"
}

# Deploy custom container
deploy_custom_container() {
    echo ""
    read -p "Enter configuration file path: " config_path

    if [ ! -f "$config_path" ]; then
        echo "Configuration file not found: $config_path"
        return 1
    fi

    cd "$PROJECT_ROOT"
    java com.telcobright.orchestrix.testrunner.storage.LocalBtrfsDeploymentRunner "$config_path"
}

# View containers
view_containers() {
    echo ""
    echo "Deployed containers:"
    echo "===================="
    lxc list

    echo ""
    echo "BTRFS volumes:"
    echo "=============="
    if [ -d "$HOME/telcobright/btrfs/containers" ]; then
        ls -la "$HOME/telcobright/btrfs/containers/" 2>/dev/null || echo "No volumes found"
    fi
}

# Clean up containers
cleanup_containers() {
    echo ""
    echo "WARNING: This will remove all test containers!"
    read -p "Continue? (y/N): " -n 1 -r
    echo

    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Removing test containers..."

        # Stop and delete test containers
        for container in $(lxc list --format csv | grep -E "^test-" | cut -d',' -f1); do
            echo "Removing $container..."
            lxc stop "$container" --force 2>/dev/null || true
            lxc delete "$container" --force
        done

        # Clean up BTRFS volumes
        if [ -d "$HOME/telcobright/btrfs/containers" ]; then
            echo "Cleaning BTRFS volumes..."
            for volume in "$HOME/telcobright/btrfs/containers"/test-*; do
                if [ -d "$volume" ]; then
                    echo "Removing volume: $volume"
                    sudo btrfs subvolume delete "$volume" 2>/dev/null || sudo rm -rf "$volume"
                fi
            done
        fi

        echo "Cleanup complete"
    fi
}

# Main loop
while true; do
    main_menu
done