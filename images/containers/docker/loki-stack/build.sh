#!/bin/bash

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

echo "======================================"
echo "Orchestrix Observability Stack Builder"
echo "======================================"
echo ""

ACTION="${1:-up}"
COMPOSE_PROJECT_NAME="orchestrix-observability"

check_docker() {
    if ! command -v docker &> /dev/null; then
        echo "ERROR: Docker is not installed or not in PATH"
        exit 1
    fi

    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        echo "ERROR: Docker Compose is not installed"
        exit 1
    fi

    echo "✓ Docker and Docker Compose are installed"
}

check_ports() {
    local ports=("7000" "3100" "9090" "3200" "9100")
    local ports_in_use=""

    for port in "${ports[@]}"; do
        if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
            ports_in_use="$ports_in_use $port"
        fi
    done

    if [ -n "$ports_in_use" ]; then
        echo "WARNING: The following ports are already in use:$ports_in_use"
        echo "This may cause conflicts with the observability stack."
        read -p "Continue anyway? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    else
        echo "✓ All required ports are available"
    fi
}

create_volumes() {
    echo "Creating Docker volumes..."
    docker volume create orchestrix-loki-data 2>/dev/null || true
    docker volume create orchestrix-prometheus-data 2>/dev/null || true
    docker volume create orchestrix-grafana-data 2>/dev/null || true
    docker volume create orchestrix-tempo-data 2>/dev/null || true
    echo "✓ Docker volumes created"
}

start_stack() {
    echo ""
    echo "Starting Orchestrix Observability Stack..."

    if docker compose version &> /dev/null; then
        docker compose -p "$COMPOSE_PROJECT_NAME" up -d
    else
        docker-compose -p "$COMPOSE_PROJECT_NAME" up -d
    fi

    echo ""
    echo "✓ Stack started successfully!"
    echo ""
    echo "======================================"
    echo "Access Points:"
    echo "======================================"
    echo "Grafana:     http://localhost:7000"
    echo "             Username: admin"
    echo "             Password: orchestrix123"
    echo ""
    echo "Loki API:    http://localhost:3100"
    echo "Prometheus:  http://localhost:9090"
    echo "Tempo:       http://localhost:3200"
    echo "======================================"
    echo ""
    echo "To view logs: docker compose -p $COMPOSE_PROJECT_NAME logs -f"
    echo "To stop:      ./build.sh down"
    echo "To restart:   ./build.sh restart"
}

stop_stack() {
    echo "Stopping Orchestrix Observability Stack..."

    if docker compose version &> /dev/null; then
        docker compose -p "$COMPOSE_PROJECT_NAME" down
    else
        docker-compose -p "$COMPOSE_PROJECT_NAME" down
    fi

    echo "✓ Stack stopped"
}

restart_stack() {
    stop_stack
    echo ""
    start_stack
}

show_status() {
    echo "Stack Status:"
    echo "-------------"

    if docker compose version &> /dev/null; then
        docker compose -p "$COMPOSE_PROJECT_NAME" ps
    else
        docker-compose -p "$COMPOSE_PROJECT_NAME" ps
    fi
}

clean_all() {
    echo "WARNING: This will remove all containers, volumes, and data!"
    read -p "Are you sure? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        stop_stack
        echo "Removing volumes..."
        docker volume rm orchestrix-loki-data orchestrix-prometheus-data orchestrix-grafana-data orchestrix-tempo-data 2>/dev/null || true
        echo "✓ Cleanup complete"
    else
        echo "Cleanup cancelled"
    fi
}

show_logs() {
    if docker compose version &> /dev/null; then
        docker compose -p "$COMPOSE_PROJECT_NAME" logs -f
    else
        docker-compose -p "$COMPOSE_PROJECT_NAME" logs -f
    fi
}

case "$ACTION" in
    up|start)
        check_docker
        check_ports
        create_volumes
        start_stack
        ;;
    down|stop)
        stop_stack
        ;;
    restart)
        restart_stack
        ;;
    status|ps)
        show_status
        ;;
    logs)
        show_logs
        ;;
    clean)
        clean_all
        ;;
    *)
        echo "Usage: $0 {up|down|restart|status|logs|clean}"
        echo ""
        echo "Commands:"
        echo "  up/start   - Start the observability stack"
        echo "  down/stop  - Stop the observability stack"
        echo "  restart    - Restart the observability stack"
        echo "  status/ps  - Show stack status"
        echo "  logs       - Show and follow logs"
        echo "  clean      - Remove all containers and volumes (WARNING: Deletes all data)"
        exit 1
        ;;
esac