#!/bin/bash

# Parse command line arguments
ENTITY_NAME=""
FORCE=false
NEW_VALUE=""
NEW_TYPE=""
ACTION="reset"

while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--entity)
            ENTITY_NAME="$2"
            shift 2
            ;;
        -v|--value)
            NEW_VALUE="$2"
            ACTION="update"
            shift 2
            ;;
        -t|--type)
            NEW_TYPE="$2"
            ACTION="update"
            shift 2
            ;;
        -f|--force)
            FORCE=true
            shift
            ;;
        -h|--help)
            echo "Usage: sequence-reset [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  -e, --entity <name>   Entity to operate on"
            echo "  -v, --value <number>  Set new counter value (int/long types only)"
            echo "  -t, --type <type>     Change entity type (int|long|uuid8|uuid12|uuid16|uuid22)"
            echo "  -f, --force           Skip confirmation prompt"
            echo "  -h, --help            Show this help message"
            echo ""
            echo "Examples:"
            echo "  sequence-reset                          # Reset all entities"
            echo "  sequence-reset -f                       # Reset all (no confirmation)"
            echo "  sequence-reset -e users                 # Reset 'users' entity"
            echo "  sequence-reset -e users -v 1000         # Set 'users' counter to 1000"
            echo "  sequence-reset -e users -t long         # Change 'users' to long type"
            echo "  sequence-reset -e tokens -t uuid16      # Change 'tokens' to uuid16 type"
            echo ""
            echo "Available types:"
            echo "  int     - Sequential 32-bit integer"
            echo "  long    - Sequential 64-bit integer"
            echo "  uuid8   - Random 8-character alphanumeric"
            echo "  uuid12  - Random 12-character alphanumeric"
            echo "  uuid16  - Random 16-character alphanumeric"
            echo "  uuid22  - Random 22-character alphanumeric"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use -h or --help for usage information"
            exit 1
            ;;
    esac
done

# Function to reset all entities
reset_all() {
    if [ "$FORCE" != true ]; then
        echo "WARNING: This will delete all sequence entities!"
        read -p "Are you sure you want to continue? (yes/no): " confirmation

        if [ "$confirmation" != "yes" ]; then
            echo "Operation cancelled."
            exit 0
        fi
    fi

    rm -f /var/lib/sequence-service/state.json
    echo "All entities have been deleted."

    if systemctl is-active --quiet sequence-service 2>/dev/null; then
        echo "Restarting service..."
        systemctl restart sequence-service
    elif systemctl is-active --quiet auto-increment 2>/dev/null; then
        # Fallback for old service name
        systemctl restart auto-increment
    fi

    echo "Reset complete - all entities cleared."
}

# Function to reset specific entity
reset_entity() {
    local entity="$1"

    if [ "$FORCE" != true ]; then
        echo "WARNING: This will reset the entity '$entity'!"
        read -p "Are you sure you want to continue? (yes/no): " confirmation

        if [ "$confirmation" != "yes" ]; then
            echo "Operation cancelled."
            exit 0
        fi
    fi

    # Check if state file exists
    if [ ! -f /var/lib/sequence-service/state.json ] && [ ! -f /var/lib/auto-increment/state.json ]; then
        echo "No state file found. Nothing to reset."
        exit 0
    fi

    # Determine state file location
    STATE_FILE="/var/lib/sequence-service/state.json"
    if [ ! -f "$STATE_FILE" ]; then
        STATE_FILE="/var/lib/auto-increment/state.json"
    fi

    # Create a temporary Python script to manipulate JSON
    python3 - <<EOF
import json
import sys

entity_to_remove = "$entity"
state_file = "$STATE_FILE"

try:
    with open(state_file, 'r') as f:
        state = json.load(f)

    # Find and remove the entity
    original_length = len(state)
    state = [s for s in state if s.get('entityName') != entity_to_remove]

    if len(state) == original_length:
        print(f"Entity '{entity_to_remove}' not found.")
        sys.exit(1)

    # Write back the modified state
    with open(state_file, 'w') as f:
        json.dump(state, f, indent=2)

    print(f"Entity '{entity_to_remove}' has been reset.")

except FileNotFoundError:
    print("State file not found.")
    sys.exit(1)
except json.JSONDecodeError:
    print("Invalid state file format.")
    sys.exit(1)
except Exception as e:
    print(f"Error: {e}")
    sys.exit(1)
EOF

    if [ $? -eq 0 ]; then
        restart_service
        echo "Reset complete - entity '$entity' cleared."
    fi
}

# Function to update entity
update_entity() {
    local entity="$1"

    if [ -z "$entity" ]; then
        echo "ERROR: Entity name required for update operation"
        exit 1
    fi

    if [ -z "$NEW_VALUE" ] && [ -z "$NEW_TYPE" ]; then
        echo "ERROR: Either --value or --type must be specified for update"
        exit 1
    fi

    if [ "$FORCE" != true ]; then
        echo "WARNING: This will modify the entity '$entity'!"
        if [ -n "$NEW_VALUE" ]; then
            echo "  New value: $NEW_VALUE"
        fi
        if [ -n "$NEW_TYPE" ]; then
            echo "  New type: $NEW_TYPE"
        fi
        read -p "Are you sure you want to continue? (yes/no): " confirmation

        if [ "$confirmation" != "yes" ]; then
            echo "Operation cancelled."
            exit 0
        fi
    fi

    # Determine state file location
    STATE_FILE="/var/lib/sequence-service/state.json"
    if [ ! -f "$STATE_FILE" ]; then
        STATE_FILE="/var/lib/auto-increment/state.json"
    fi

    # Create or update entity
    python3 - <<EOF
import json
import sys

entity_name = "$entity"
new_value = "$NEW_VALUE"
new_type = "$NEW_TYPE"
state_file = "$STATE_FILE"

valid_types = ['int', 'long', 'uuid8', 'uuid12', 'uuid16', 'uuid22']

# Validate type if provided
if new_type and new_type not in valid_types:
    print(f"ERROR: Invalid type '{new_type}'. Valid types: {', '.join(valid_types)}")
    sys.exit(1)

# Validate value for numeric types
if new_value:
    try:
        value_num = int(new_value)
        if value_num < 1:
            print("ERROR: Value must be positive")
            sys.exit(1)
    except ValueError:
        print("ERROR: Value must be a valid number")
        sys.exit(1)

try:
    # Load existing state or create new
    try:
        with open(state_file, 'r') as f:
            state = json.load(f)
    except FileNotFoundError:
        state = []

    # Find existing entity
    entity_found = False
    for record in state:
        if record.get('entityName') == entity_name:
            entity_found = True

            # Update type if specified
            if new_type:
                old_type = record.get('dataType')
                record['dataType'] = new_type

                # Reset counter for type change from numeric to UUID
                if old_type in ['int', 'long'] and new_type.startswith('uuid'):
                    record['currentVal'] = None
                # Initialize counter for type change from UUID to numeric
                elif old_type and old_type.startswith('uuid') and new_type in ['int', 'long']:
                    record['currentVal'] = 1 if not new_value else int(new_value)

                print(f"Changed type from '{old_type}' to '{new_type}'")

            # Update value if specified (only for numeric types)
            if new_value:
                if record.get('dataType') in ['int', 'long']:
                    record['currentVal'] = int(new_value)
                    print(f"Set counter to {new_value}")
                else:
                    print(f"WARNING: Cannot set value for type '{record.get('dataType')}' (UUID types don't have counters)")

            break

    # Create new entity if not found
    if not entity_found:
        if not new_type:
            new_type = 'int'  # Default type

        new_record = {
            'entityName': entity_name,
            'dataType': new_type,
            'currentVal': None if new_type.startswith('uuid') else (int(new_value) if new_value else 1)
        }
        state.append(new_record)
        print(f"Created new entity '{entity_name}' with type '{new_type}'")
        if new_value and not new_type.startswith('uuid'):
            print(f"Set counter to {new_value}")

    # Write back the modified state
    import os
    os.makedirs('/var/lib/sequence-service', exist_ok=True)

    # Try new location first
    try:
        with open('/var/lib/sequence-service/state.json', 'w') as f:
            json.dump(state, f, indent=2)
    except:
        # Fallback to old location
        os.makedirs('/var/lib/auto-increment', exist_ok=True)
        with open('/var/lib/auto-increment/state.json', 'w') as f:
            json.dump(state, f, indent=2)

    print(f"Entity '{entity_name}' updated successfully.")

except json.JSONDecodeError:
    print("Invalid state file format.")
    sys.exit(1)
except Exception as e:
    print(f"Error: {e}")
    sys.exit(1)
EOF

    if [ $? -eq 0 ]; then
        restart_service
    fi
}

# Function to restart service
restart_service() {
    if systemctl is-active --quiet sequence-service 2>/dev/null; then
        echo "Reloading service..."
        systemctl reload-or-restart sequence-service
    elif systemctl is-active --quiet auto-increment 2>/dev/null; then
        # Fallback for old service name
        systemctl reload-or-restart auto-increment
    fi
}

# Main logic
if [ "$ACTION" = "update" ]; then
    update_entity "$ENTITY_NAME"
elif [ -n "$ENTITY_NAME" ]; then
    reset_entity "$ENTITY_NAME"
else
    reset_all
fi