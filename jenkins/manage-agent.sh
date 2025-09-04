#!/bin/bash

# Jenkins Docker Agent Management Commands

CONTAINER_NAME="jenkins-orchestrix-agent"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

case "$1" in
    start)
        echo "Starting Jenkins agent..."
        cd ${SCRIPT_DIR} && docker-compose up -d
        ;;
    stop)
        echo "Stopping Jenkins agent..."
        cd ${SCRIPT_DIR} && docker-compose down
        ;;
    restart)
        echo "Restarting Jenkins agent..."
        cd ${SCRIPT_DIR} && docker-compose restart
        ;;
    status)
        echo "Agent Status:"
        docker ps -a | grep ${CONTAINER_NAME}
        ;;
    logs)
        docker-compose logs -f --tail=100
        ;;
    shell)
        echo "Entering agent container..."
        docker exec -it ${CONTAINER_NAME} /bin/bash
        ;;
    update)
        echo "Updating agent image..."
        cd ${SCRIPT_DIR}
        docker-compose pull
        docker-compose up -d
        ;;
    clean)
        echo "Cleaning up..."
        cd ${SCRIPT_DIR}
        docker-compose down -v
        docker system prune -f
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|status|logs|shell|update|clean}"
        exit 1
        ;;
esac
