#!/bin/bash

# ===============================================
# ORCHESTRIX PROFILE SWITCHER
# ===============================================
# Script to switch between dev, staging, and prod profiles
# for both frontend and backend applications

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
CONFIG_FILE="$PROJECT_ROOT/config.properties"

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Function to display usage
usage() {
    echo "Usage: $0 <profile> [options]"
    echo ""
    echo "Profiles:"
    echo "  dev       - Development environment (localhost)"
    echo "  staging   - Staging environment (staging servers)"
    echo "  prod      - Production environment (production servers)"
    echo ""
    echo "Options:"
    echo "  --frontend-only    Switch only frontend configuration"
    echo "  --backend-only     Switch only backend configuration"
    echo "  --dry-run         Show what would be changed without making changes"
    echo "  --help            Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 dev                    # Switch both frontend and backend to dev"
    echo "  $0 staging --frontend-only # Switch only frontend to staging"
    echo "  $0 prod --dry-run         # Show changes for prod without applying"
}

# Function to update config file
update_config() {
    local profile=$1
    
    if [[ ! -f "$CONFIG_FILE" ]]; then
        print_error "Config file not found: $CONFIG_FILE"
        exit 1
    fi
    
    print_info "Updating main config file..."
    sed -i.bak "s/^ACTIVE_PROFILE=.*/ACTIVE_PROFILE=$profile/" "$CONFIG_FILE"
    print_success "Updated ACTIVE_PROFILE to $profile in config.properties"
}

# Function to update Spring Boot application properties
update_backend_config() {
    local profile=$1
    local app_props="$PROJECT_ROOT/src/main/resources/application.properties"
    
    if [[ ! -f "$app_props" ]]; then
        print_error "Backend application.properties not found: $app_props"
        return 1
    fi
    
    print_info "Updating backend configuration..."
    sed -i.bak "s/^spring.profiles.active=.*/spring.profiles.active=$profile/" "$app_props"
    print_success "Updated Spring Boot profile to $profile"
}

# Function to show current configuration
show_current_config() {
    print_info "Current Configuration:"
    echo ""
    
    # Main config
    if [[ -f "$CONFIG_FILE" ]]; then
        local active_profile=$(grep "^ACTIVE_PROFILE=" "$CONFIG_FILE" | cut -d'=' -f2)
        echo "📄 Main Config (config.properties): $active_profile"
    fi
    
    # Backend config
    local app_props="$PROJECT_ROOT/src/main/resources/application.properties"
    if [[ -f "$app_props" ]]; then
        local spring_profile=$(grep "^spring.profiles.active=" "$app_props" | cut -d'=' -f2 | sed 's/.*://g' | cut -d'}' -f1)
        echo "🖥️  Backend (Spring Boot): $spring_profile"
    fi
    
    # Frontend configs
    echo "🌐 Frontend environments:"
    for env_file in "$PROJECT_ROOT/orchestrix-ui/.env.dev" "$PROJECT_ROOT/orchestrix-ui/.env.staging" "$PROJECT_ROOT/orchestrix-ui/.env.prod"; do
        if [[ -f "$env_file" ]]; then
            local env_name=$(basename "$env_file" | sed 's/\.env\.//g')
            local api_url=$(grep "^REACT_APP_API_URL=" "$env_file" | cut -d'=' -f2-)
            echo "   - $env_name: $api_url"
        fi
    done
    echo ""
}

# Function to validate profile
validate_profile() {
    local profile=$1
    case $profile in
        dev|staging|prod)
            return 0
            ;;
        *)
            print_error "Invalid profile: $profile"
            print_error "Valid profiles are: dev, staging, prod"
            return 1
            ;;
    esac
}

# Function to check if services are running
check_running_services() {
    print_info "Checking for running services..."
    
    # Check for Spring Boot (port 8090)
    if lsof -Pi :8090 -sTCP:LISTEN -t >/dev/null 2>&1; then
        print_warning "Backend is running on port 8090. You may need to restart it."
    fi
    
    # Check for React dev server (port 3010)
    if lsof -Pi :3010 -sTCP:LISTEN -t >/dev/null 2>&1; then
        print_warning "Frontend dev server is running on port 3010. Restart needed for new config."
    fi
}

# Function to provide next steps
show_next_steps() {
    local profile=$1
    
    print_info "Next Steps:"
    echo ""
    echo "To apply the new configuration:"
    echo ""
    echo "🖥️  Backend (Spring Boot):"
    echo "   mvn spring-boot:run"
    echo "   OR"
    echo "   mvn spring-boot:run -Dspring-boot.run.profiles=$profile"
    echo ""
    echo "🌐 Frontend (React):"
    echo "   cd orchestrix-ui"
    echo "   npm run start:$profile"
    echo ""
    echo "📊 To verify configuration:"
    echo "   ./scripts/switch-profile.sh --show-config"
}

# Main script logic
main() {
    local profile=""
    local frontend_only=false
    local backend_only=false
    local dry_run=false
    local show_config=false
    
    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            dev|staging|prod)
                profile=$1
                shift
                ;;
            --frontend-only)
                frontend_only=true
                shift
                ;;
            --backend-only)
                backend_only=true
                shift
                ;;
            --dry-run)
                dry_run=true
                shift
                ;;
            --show-config)
                show_config=true
                shift
                ;;
            --help|-h)
                usage
                exit 0
                ;;
            *)
                print_error "Unknown option: $1"
                usage
                exit 1
                ;;
        esac
    done
    
    # Show current configuration if requested
    if [[ "$show_config" == "true" ]]; then
        show_current_config
        exit 0
    fi
    
    # Validate profile
    if [[ -z "$profile" ]]; then
        print_error "Profile is required"
        echo ""
        show_current_config
        echo ""
        usage
        exit 1
    fi
    
    validate_profile "$profile" || exit 1
    
    # Show what will be changed in dry-run mode
    if [[ "$dry_run" == "true" ]]; then
        print_info "DRY RUN - No changes will be made"
        echo ""
        print_info "Would switch to profile: $profile"
        if [[ "$frontend_only" == "true" ]]; then
            echo "  - Frontend configuration only"
        elif [[ "$backend_only" == "true" ]]; then
            echo "  - Backend configuration only"
        else
            echo "  - Both frontend and backend configurations"
        fi
        exit 0
    fi
    
    print_info "Switching to profile: $profile"
    echo ""
    
    # Check for running services
    check_running_services
    echo ""
    
    # Update configurations
    if [[ "$frontend_only" == "false" && "$backend_only" == "false" ]]; then
        # Update both
        update_config "$profile"
        update_backend_config "$profile"
    elif [[ "$backend_only" == "true" ]]; then
        # Backend only
        update_backend_config "$profile"
    elif [[ "$frontend_only" == "true" ]]; then
        # Frontend only (main config drives frontend selection)
        update_config "$profile"
    fi
    
    echo ""
    print_success "Profile switching completed!"
    echo ""
    show_next_steps "$profile"
}

# Run main function
main "$@"