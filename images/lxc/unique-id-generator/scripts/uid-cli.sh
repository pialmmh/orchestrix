#!/bin/bash
# CLI tool for Unique ID Generator management
# Supports resetting counters and other operations

set -e

# Default values
DEFAULT_HOST="localhost"
DEFAULT_PORT="7001"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Parse command line arguments
HOST="${UID_HOST:-$DEFAULT_HOST}"
PORT="${UID_PORT:-$DEFAULT_PORT}"
BASE_URL="http://${HOST}:${PORT}"

# Function to print colored output
print_success() { echo -e "${GREEN}✓${NC} $1"; }
print_error() { echo -e "${RED}✗${NC} $1"; }
print_info() { echo -e "${BLUE}ℹ${NC} $1"; }
print_warning() { echo -e "${YELLOW}⚠${NC} $1"; }

# Function to display usage
usage() {
    cat << EOF
Unique ID Generator CLI
Usage: $0 [OPTIONS] COMMAND [ARGS]

OPTIONS:
    -h, --host HOST       Server host (default: localhost)
    -p, --port PORT       Server port (default: 7001)
    --help                Display this help message

COMMANDS:
    init ENTITY TYPE [VALUE]  Initialize new entity with type and optional start value
    reset ENTITY VALUE        Reset counter for entity to specified value
    status [ENTITY]           Show status of entity or all entities
    next ENTITY TYPE          Get next ID for entity with specified type
    batch ENTITY TYPE N       Get batch of N IDs for entity
    list                      List all registered entities
    types                     Show available data types
    health                    Check server health
    shard-info                Show shard configuration

EXAMPLES:
    # Initialize new entity 'partner' with type 'int'
    $0 init partner int

    # Initialize 'customer' starting at 1001
    $0 init customer int 1001

    # Initialize UUID-based entity
    $0 init session uuid16

    # Reset customer entity counter to 1001
    $0 reset customer 1001

    # Reset with specific host/port
    $0 -h 10.10.199.50 -p 7001 reset customer 1001

    # Using environment variables
    UID_HOST=10.10.199.50 UID_PORT=7001 $0 reset customer 1001

    # Get status of an entity
    $0 status customer

    # Get next ID for entity
    $0 next customer int

    # Get batch of IDs
    $0 batch order long 100

    # List all entities
    $0 list

ENVIRONMENT VARIABLES:
    UID_HOST    Override default host
    UID_PORT    Override default port

EOF
}

# Function to check if server is reachable
check_server() {
    if ! curl -s "${BASE_URL}/health" > /dev/null 2>&1; then
        print_error "Cannot connect to server at ${BASE_URL}"
        print_info "Please check if the server is running and accessible"
        exit 1
    fi
}

# Function to initialize entity
init_entity() {
    local entity="$1"
    local type="$2"
    local start_value="$3"

    if [ -z "$entity" ] || [ -z "$type" ]; then
        print_error "Entity name and type are required"
        echo "Usage: $0 init ENTITY TYPE [START_VALUE]"
        echo "Available types: int, long, uuid8, uuid12, uuid16, uuid22"
        exit 1
    fi

    print_info "Initializing entity '${entity}' with type '${type}'..."

    # Build JSON body
    if [ -n "$start_value" ]; then
        json_body="{\"dataType\": \"${type}\", \"startValue\": ${start_value}}"
    else
        json_body="{\"dataType\": \"${type}\"}"
    fi

    response=$(curl -s -X POST "${BASE_URL}/api/init/${entity}" \
        -H "Content-Type: application/json" \
        -d "$json_body" \
        -w "\n%{http_code}")

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)

    if [ "$http_code" == "201" ]; then
        print_success "Entity initialized successfully!"

        # Check if value was adjusted
        if echo "$body" | jq -e '.adjusted' > /dev/null 2>&1; then
            print_warning "$(echo "$body" | jq -r '.adjustmentReason')"
        fi

        if echo "$body" | jq -e '.actualStartValue' > /dev/null 2>&1; then
            echo "$body" | jq -r '
                "Entity: " + .entityName,
                "Type: " + .dataType,
                "Shard: " + (.shard | tostring),
                (if .requestedStartValue then "Requested start: " + (.requestedStartValue | tostring) else "" end),
                "Actual start: " + (.actualStartValue | tostring),
                "Next value: " + (.nextValue | tostring),
                "Pattern: " + .pattern
            ' 2>/dev/null | grep -v "^$" || echo "$body"
        else
            echo "$body" | jq -r '
                "Entity: " + .entityName,
                "Type: " + .dataType,
                "Shard: " + (.shard | tostring),
                "Message: " + .message
            ' 2>/dev/null || echo "$body"
        fi
    elif [ "$http_code" == "409" ]; then
        print_warning "Entity already exists"
        echo "$body" | jq -r '.message' 2>/dev/null || echo "$body"
        echo "$body" | jq -r '
            "Current status:",
            "  Type: " + .currentStatus.dataType,
            "  Iteration: " + (.currentStatus.currentIteration | tostring),
            "  Shard: " + (.currentStatus.shard | tostring)
        ' 2>/dev/null
    else
        print_error "Failed to initialize entity (HTTP $http_code)"
        echo "$body" | jq -r '.error + ": " + .message' 2>/dev/null || echo "$body"
        if echo "$body" | jq -e '.suggestedValue' > /dev/null 2>&1; then
            suggested=$(echo "$body" | jq -r '.suggestedValue')
            print_info "Suggested start value for this shard: $suggested"
        fi
        exit 1
    fi
}

# Function to reset counter
reset_counter() {
    local entity="$1"
    local value="$2"

    if [ -z "$entity" ] || [ -z "$value" ]; then
        print_error "Entity name and value are required"
        echo "Usage: $0 reset ENTITY VALUE"
        exit 1
    fi

    print_info "Resetting counter for entity '${entity}' to ${value}..."

    response=$(curl -s -X PUT "${BASE_URL}/api/reset/${entity}" \
        -H "Content-Type: application/json" \
        -d "{\"value\": ${value}}" \
        -w "\n%{http_code}")

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)

    if [ "$http_code" == "200" ]; then
        print_success "Counter reset successfully!"

        # Check if value was adjusted
        if echo "$body" | jq -e '.adjusted' > /dev/null 2>&1; then
            print_warning "$(echo "$body" | jq -r '.adjustmentReason')"
        fi

        echo "$body" | jq -r '
            "Entity: " + .entityName,
            "Type: " + .dataType,
            "Shard: " + (.shard | tostring),
            "Previous value: " + (.reset.previousValue // "none" | tostring),
            (if .adjusted then "Requested value: " + (.reset.requestedValue | tostring) else "" end),
            "Reset to: " + (.reset.actualValue | tostring),
            "Next value will be: " + .nextValue
        ' 2>/dev/null | grep -v "^$" || echo "$body"
    else
        print_error "Failed to reset counter (HTTP $http_code)"
        echo "$body" | jq -r '.error + ": " + .message' 2>/dev/null || echo "$body"
        if echo "$body" | jq -e '.suggestedValue' > /dev/null 2>&1; then
            suggested=$(echo "$body" | jq -r '.suggestedValue')
            print_info "Suggested value for this shard: $suggested"
        fi
        exit 1
    fi
}

# Function to get entity status
get_status() {
    local entity="$1"

    if [ -z "$entity" ]; then
        # List all entities
        response=$(curl -s "${BASE_URL}/api/list")
        echo "$response" | jq -r '
            .entities[] |
            "Entity: " + .entityName,
            "  Type: " + .dataType,
            "  Current: " + (.currentValue // "N/A" | tostring),
            "  Shard: " + (.shard | tostring),
            ""
        ' 2>/dev/null || echo "$response"
    else
        response=$(curl -s "${BASE_URL}/api/status/${entity}" -w "\n%{http_code}")
        http_code=$(echo "$response" | tail -n1)
        body=$(echo "$response" | head -n-1)

        if [ "$http_code" == "200" ]; then
            echo "$body" | jq -r '
                "Entity: " + .entityName,
                "Type: " + .dataType,
                "Current value: " + (.currentValue // "N/A" | tostring),
                "Next value: " + (.nextValue // "N/A" | tostring),
                "Shard: " + (.shard | tostring),
                "Iteration: " + (.currentIteration | tostring)
            ' 2>/dev/null || echo "$body"
        elif [ "$http_code" == "404" ]; then
            print_warning "Entity '$entity' not found"
        else
            print_error "Failed to get status (HTTP $http_code)"
            echo "$body"
        fi
    fi
}

# Function to get next ID
get_next_id() {
    local entity="$1"
    local type="$2"

    if [ -z "$entity" ] || [ -z "$type" ]; then
        print_error "Entity name and type are required"
        echo "Usage: $0 next ENTITY TYPE"
        echo "Available types: int, long, uuid8, uuid12, uuid16, uuid22"
        exit 1
    fi

    response=$(curl -s "${BASE_URL}/api/next-id/${entity}?dataType=${type}")
    echo "$response" | jq -r '"Next ID: " + (.value | tostring)' 2>/dev/null || echo "$response"
}

# Function to get batch of IDs
get_batch() {
    local entity="$1"
    local type="$2"
    local size="$3"

    if [ -z "$entity" ] || [ -z "$type" ] || [ -z "$size" ]; then
        print_error "Entity name, type, and batch size are required"
        echo "Usage: $0 batch ENTITY TYPE SIZE"
        exit 1
    fi

    response=$(curl -s "${BASE_URL}/api/next-batch/${entity}?dataType=${type}&batchSize=${size}")
    echo "$response" | jq 2>/dev/null || echo "$response"
}

# Function to list entities
list_entities() {
    response=$(curl -s "${BASE_URL}/api/list")
    echo "Registered Entities:"
    echo "$response" | jq -r '
        .entities[] |
        "  • " + .entityName + " (" + .dataType + ") - Current: " + (.currentValue // "N/A" | tostring)
    ' 2>/dev/null || echo "$response"
}

# Function to show types
show_types() {
    response=$(curl -s "${BASE_URL}/api/types")
    echo "$response" | jq 2>/dev/null || echo "$response"
}

# Function to check health
check_health() {
    response=$(curl -s "${BASE_URL}/health")
    echo "$response" | jq 2>/dev/null || echo "$response"
}

# Function to show shard info
shard_info() {
    response=$(curl -s "${BASE_URL}/shard-info")
    echo "$response" | jq 2>/dev/null || echo "$response"
}

# Main script logic
main() {
    # Parse options
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--host)
                HOST="$2"
                BASE_URL="http://${HOST}:${PORT}"
                shift 2
                ;;
            -p|--port)
                PORT="$2"
                BASE_URL="http://${HOST}:${PORT}"
                shift 2
                ;;
            --help)
                usage
                exit 0
                ;;
            init)
                shift
                check_server
                init_entity "$1" "$2" "$3"
                exit 0
                ;;
            reset)
                shift
                check_server
                reset_counter "$1" "$2"
                exit 0
                ;;
            status)
                shift
                check_server
                get_status "$1"
                exit 0
                ;;
            next)
                shift
                check_server
                get_next_id "$1" "$2"
                exit 0
                ;;
            batch)
                shift
                check_server
                get_batch "$1" "$2" "$3"
                exit 0
                ;;
            list)
                check_server
                list_entities
                exit 0
                ;;
            types)
                check_server
                show_types
                exit 0
                ;;
            health)
                check_server
                check_health
                exit 0
                ;;
            shard-info)
                check_server
                shard_info
                exit 0
                ;;
            *)
                print_error "Unknown command: $1"
                echo "Use --help for usage information"
                exit 1
                ;;
        esac
    done

    # If no command provided, show usage
    usage
}

# Run main function
main "$@"