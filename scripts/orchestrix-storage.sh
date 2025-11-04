#!/bin/bash
# Orchestrix Storage Management using Java Automation
# Main entry point for all storage operations

set -e

ORCHESTRIX_HOME="/home/mustafa/telcobright-projects/orchestrix"
cd "$ORCHESTRIX_HOME"

function show_usage() {
    cat << EOF
Orchestrix Storage Management (Java Automation)

Usage: $0 <command> [options]

Commands:
    setup-btrfs           Setup BTRFS filesystem on local machine
    build-container       Build a container with BTRFS storage
    create-volume         Create a BTRFS volume
    list-volumes          List all BTRFS volumes
    snapshot              Create a snapshot of a volume
    test-storage          Test storage automation

Examples:
    # Setup BTRFS on local machine
    sudo $0 setup-btrfs

    # Build Grafana-Loki container
    sudo $0 build-container grafana-loki

    # Create a storage volume
    sudo $0 create-volume --name myapp --size 10G --location btrfs_local_main

    # List all volumes
    $0 list-volumes

Options:
    --java                Use Java automation (default)
    --shell               Use shell scripts (legacy)
    --config <file>       Specify configuration file
    --help                Show this help message

EOF
}

function compile_java() {
    if [ ! -d "target/classes" ] || [ "$FORCE_COMPILE" = "true" ]; then
        echo "Compiling Java automation code..."
        mvn compile -DskipTests
    fi
}

function setup_btrfs() {
    echo "Setting up BTRFS using Java automation..."
    compile_java
    sudo mvn exec:java \
        -Dexec.mainClass="com.telcobright.orchestrix.automation.storage.runners.LocalBtrfsSetupRunner" \
        -Dexec.classpathScope=compile
}

function build_container() {
    local container=$1
    local config_file=""

    case "$container" in
        grafana-loki)
            config_file="images/containers/lxc/grafana-loki/build/build.conf"
            ;;
        *)
            echo "Unknown container: $container"
            echo "Available containers: grafana-loki"
            exit 1
            ;;
    esac

    if [ -n "$CONFIG_FILE" ]; then
        config_file="$CONFIG_FILE"
    fi

    echo "Building $container using Java automation..."
    echo "Config: $config_file"

    compile_java
    sudo mvn exec:java \
        -Dexec.mainClass="com.telcobright.orchestrix.automation.storage.runners.GrafanaLokiBuildRunner" \
        -Dexec.args="$config_file" \
        -Dexec.classpathScope=compile
}

function create_volume() {
    echo "Creating BTRFS volume using Java automation..."
    compile_java

    # Parse arguments
    local volume_name=""
    local volume_size=""
    local location="btrfs_local_main"

    while [[ $# -gt 0 ]]; do
        case $1 in
            --name) volume_name="$2"; shift 2 ;;
            --size) volume_size="$2"; shift 2 ;;
            --location) location="$2"; shift 2 ;;
            *) shift ;;
        esac
    done

    if [ -z "$volume_name" ] || [ -z "$volume_size" ]; then
        echo "Error: --name and --size are required"
        exit 1
    fi

    sudo mvn exec:java \
        -Dexec.mainClass="com.telcobright.orchestrix.automation.storage.runners.VolumeManagerRunner" \
        -Dexec.args="create $volume_name $volume_size $location" \
        -Dexec.classpathScope=compile
}

function list_volumes() {
    echo "Listing BTRFS volumes..."

    # Check default location
    local btrfs_path="/home/telcobright/btrfs"

    if [ -d "$btrfs_path/containers" ]; then
        echo "Volumes in $btrfs_path/containers:"
        sudo btrfs subvolume list "$btrfs_path" 2>/dev/null || echo "No BTRFS filesystem found"
    else
        echo "No BTRFS storage found at $btrfs_path"
        echo "Run 'sudo $0 setup-btrfs' to set up storage"
    fi
}

function test_storage() {
    echo "Testing storage automation..."
    compile_java

    mvn test -Dtest=LocalBtrfsDeploymentRunner
}

# Parse command line arguments
COMMAND=""
USE_JAVA="true"
CONFIG_FILE=""
FORCE_COMPILE=""

while [[ $# -gt 0 ]]; do
    case $1 in
        setup-btrfs)
            COMMAND="setup-btrfs"
            shift
            ;;
        build-container)
            COMMAND="build-container"
            CONTAINER_NAME="$2"
            shift 2
            ;;
        create-volume)
            COMMAND="create-volume"
            shift
            break  # Let create_volume parse remaining args
            ;;
        list-volumes)
            COMMAND="list-volumes"
            shift
            ;;
        test-storage)
            COMMAND="test-storage"
            shift
            ;;
        --java)
            USE_JAVA="true"
            shift
            ;;
        --shell)
            USE_JAVA="false"
            shift
            ;;
        --config)
            CONFIG_FILE="$2"
            shift 2
            ;;
        --compile)
            FORCE_COMPILE="true"
            shift
            ;;
        --help|-h)
            show_usage
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# Execute command
case "$COMMAND" in
    setup-btrfs)
        setup_btrfs
        ;;
    build-container)
        build_container "$CONTAINER_NAME"
        ;;
    create-volume)
        create_volume "$@"
        ;;
    list-volumes)
        list_volumes
        ;;
    test-storage)
        test_storage
        ;;
    *)
        echo "Error: No command specified"
        show_usage
        exit 1
        ;;
esac

echo ""
echo "Operation completed successfully!"