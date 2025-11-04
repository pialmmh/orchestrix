#!/bin/bash
# Orchestrix Quarkus Container Management
# Helper script for building base and app containers

set -e

ORCHESTRIX_HOME="/home/mustafa/telcobright-projects/orchestrix"
QUARKUS_DIR="${ORCHESTRIX_HOME}/images/containers/lxc/quarkus-runner"

function show_usage() {
    cat << EOF
Orchestrix Quarkus Container Management

Usage: $0 <command> [options]

Commands:
    build-base              Build the Quarkus runner base container (one-time)
    build-app               Build an app container from base
    list                    List all Quarkus containers
    start                   Start an app container
    stop                    Stop an app container
    logs                    View app logs
    health                  Check app health
    export                  Export app container

Examples:
    # Build base container (first time)
    sudo $0 build-base

    # Build app container
    sudo $0 build-app myapp 1.0.0 /path/to/myapp-runner.jar

    # List containers
    $0 list

    # Start app
    $0 start myapp-v1.0.0

    # View logs
    $0 logs myapp-v1.0.0

    # Check health
    $0 health myapp-v1.0.0

Options:
    --config <file>         Specify configuration file
    --help                  Show this help message

EOF
}

function compile_if_needed() {
    cd "$ORCHESTRIX_HOME"
    if [ ! -d "target/classes" ]; then
        echo "Compiling Java automation..."
        mvn compile -DskipTests
    fi
}

function build_base() {
    echo "========================================="
    echo "Building Quarkus Runner Base Container"
    echo "========================================="

    cd "$QUARKUS_DIR"
    sudo ./build/build.sh build/build.conf

    echo ""
    echo "========================================="
    echo "Base container built successfully!"
    echo "========================================="
    echo "Image: quarkus-runner-base"
    echo ""
    echo "Next: Build your app container"
    echo "  sudo $0 build-app myapp 1.0.0 /path/to/app.jar"
    echo "========================================="
}

function build_app() {
    local app_name=$1
    local app_version=$2
    local jar_path=$3

    if [ -z "$app_name" ] || [ -z "$app_version" ] || [ -z "$jar_path" ]; then
        echo "Error: Missing required arguments"
        echo "Usage: $0 build-app <app-name> <version> <jar-path>"
        exit 1
    fi

    if [ ! -f "$jar_path" ]; then
        echo "Error: JAR file not found: $jar_path"
        exit 1
    fi

    echo "========================================="
    echo "Building App Container"
    echo "========================================="
    echo "App: $app_name"
    echo "Version: $app_version"
    echo "JAR: $jar_path"
    echo "========================================="

    # Check if base exists
    if ! lxc image list | grep -q "quarkus-runner-base"; then
        echo "Error: Base container not found!"
        echo "Build it first: sudo $0 build-base"
        exit 1
    fi

    # Create config file
    local config_file="${QUARKUS_DIR}/configs/${app_name}-build.conf"
    mkdir -p "${QUARKUS_DIR}/configs"

    if [ ! -f "$config_file" ]; then
        echo "Creating configuration file: $config_file"
        cp "${QUARKUS_DIR}/sample-config.conf" "$config_file"

        # Update config
        sed -i "s|APP_NAME=\"myapp\"|APP_NAME=\"$app_name\"|g" "$config_file"
        sed -i "s|APP_VERSION=\"1.0.0\"|APP_VERSION=\"$app_version\"|g" "$config_file"
        sed -i "s|APP_JAR_PATH=\"/path/to/myapp/target/myapp-runner.jar\"|APP_JAR_PATH=\"$jar_path\"|g" "$config_file"

        echo "Configuration created. Edit if needed: $config_file"
    fi

    # Build app container
    cd "$QUARKUS_DIR"
    sudo ./build/build.sh "$config_file"

    echo ""
    echo "========================================="
    echo "App container built successfully!"
    echo "========================================="
    echo "Container: ${app_name}-v${app_version}"
    echo ""
    echo "Start: $0 start ${app_name}-v${app_version}"
    echo "Logs: $0 logs ${app_name}-v${app_version}"
    echo "========================================="
}

function list_containers() {
    echo "Quarkus Containers:"
    echo "==================="
    lxc list | grep -E "quarkus|NAME" || echo "No Quarkus containers found"

    echo ""
    echo "Quarkus Images:"
    echo "==============="
    lxc image list | grep -E "quarkus|ALIAS" || echo "No Quarkus images found"
}

function start_container() {
    local container=$1

    if [ -z "$container" ]; then
        echo "Error: Container name required"
        echo "Usage: $0 start <container-name>"
        exit 1
    fi

    echo "Starting container: $container"
    lxc start "$container"

    echo "Waiting for app to start..."
    sleep 5

    echo "Container started. Check health:"
    echo "  $0 health $container"
}

function stop_container() {
    local container=$1

    if [ -z "$container" ]; then
        echo "Error: Container name required"
        echo "Usage: $0 stop <container-name>"
        exit 1
    fi

    echo "Stopping container: $container"
    lxc stop "$container"
}

function view_logs() {
    local container=$1

    if [ -z "$container" ]; then
        echo "Error: Container name required"
        echo "Usage: $0 logs <container-name>"
        exit 1
    fi

    echo "Viewing logs for: $container"
    echo "Press Ctrl+C to exit"
    echo "---"
    lxc exec "$container" -- tail -f /var/log/quarkus/application.log
}

function check_health() {
    local container=$1

    if [ -z "$container" ]; then
        echo "Error: Container name required"
        echo "Usage: $0 health <container-name>"
        exit 1
    fi

    echo "Checking health for: $container"
    echo "================================"

    echo "Liveness:"
    lxc exec "$container" -- curl -s http://localhost:8080/q/health/live || echo "FAILED"

    echo ""
    echo "Readiness:"
    lxc exec "$container" -- curl -s http://localhost:8080/q/health/ready || echo "FAILED"

    echo ""
    echo "Full health:"
    lxc exec "$container" -- curl -s http://localhost:8080/q/health || echo "FAILED"
}

function export_container() {
    local container=$1
    local export_path=${2:-/tmp}

    if [ -z "$container" ]; then
        echo "Error: Container name required"
        echo "Usage: $0 export <container-name> [export-path]"
        exit 1
    fi

    local filename="${export_path}/${container}-$(date +%Y%m%d-%H%M%S).tar.gz"

    echo "Exporting container: $container"
    echo "To: $filename"

    lxc stop "$container" 2>/dev/null || true
    lxc export "$container" "$filename"
    lxc start "$container" 2>/dev/null || true

    echo ""
    echo "Export complete: $filename"
}

# Main
COMMAND="${1:-}"

case "$COMMAND" in
    build-base)
        build_base
        ;;
    build-app)
        shift
        build_app "$@"
        ;;
    list)
        list_containers
        ;;
    start)
        start_container "$2"
        ;;
    stop)
        stop_container "$2"
        ;;
    logs)
        view_logs "$2"
        ;;
    health)
        check_health "$2"
        ;;
    export)
        export_container "$2" "$3"
        ;;
    --help|-h|help|"")
        show_usage
        exit 0
        ;;
    *)
        echo "Unknown command: $COMMAND"
        show_usage
        exit 1
        ;;
esac
