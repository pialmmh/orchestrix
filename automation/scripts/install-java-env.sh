#!/bin/bash
# Install Java Environment Dependencies
# This script installs JDK, Maven, and sets up JAVA_HOME

set -e

# Default versions
JDK_VERSION="${JDK_VERSION:-21}"
INSTALL_MAVEN="${INSTALL_MAVEN:-true}"

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Print colored messages
print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1" >&2
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_section() {
    echo ""
    echo "=========================================="
    echo "$1"
    echo "=========================================="
}

# Detect OS
detect_os() {
    if [ -f /etc/os-release ]; then
        . /etc/os-release
        OS=$NAME
        VERSION=$VERSION_ID
    elif [ "$(uname)" = "Darwin" ]; then
        OS="macOS"
        VERSION=$(sw_vers -productVersion)
    else
        OS=$(uname -s)
        VERSION=$(uname -r)
    fi

    echo "Detected OS: $OS $VERSION"
}

# Check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check Java installation
check_java() {
    print_section "Checking Java Environment"

    if command_exists java; then
        JAVA_VERSION=$(java -version 2>&1 | head -n1)
        print_success "Java found: $JAVA_VERSION"
    else
        print_error "Java not found"
        return 1
    fi

    if command_exists javac; then
        JAVAC_VERSION=$(javac -version 2>&1)
        print_success "Java compiler found: $JAVAC_VERSION"
    else
        print_error "Java compiler (javac) not found"
        return 1
    fi

    if [ -n "$JAVA_HOME" ]; then
        print_success "JAVA_HOME is set: $JAVA_HOME"
    else
        print_warning "JAVA_HOME is not set"
    fi

    if [ "$INSTALL_MAVEN" = "true" ] && command_exists mvn; then
        MVN_VERSION=$(mvn --version 2>&1 | head -n1)
        print_success "Maven found: $MVN_VERSION"
    fi

    return 0
}

# Install JDK on Ubuntu/Debian
install_jdk_debian() {
    print_section "Installing OpenJDK $JDK_VERSION on Debian/Ubuntu"

    # Update package list
    echo "Updating package list..."
    sudo apt-get update

    # Install JDK
    echo "Installing OpenJDK $JDK_VERSION..."
    sudo apt-get install -y openjdk-${JDK_VERSION}-jdk

    # Install Maven if requested
    if [ "$INSTALL_MAVEN" = "true" ]; then
        echo "Installing Maven..."
        sudo apt-get install -y maven
    fi

    print_success "JDK installation completed"
}

# Install JDK on RHEL/Fedora
install_jdk_rhel() {
    print_section "Installing OpenJDK $JDK_VERSION on RHEL/Fedora"

    # Install JDK
    echo "Installing OpenJDK $JDK_VERSION..."
    sudo dnf install -y java-${JDK_VERSION}-openjdk-devel

    # Install Maven if requested
    if [ "$INSTALL_MAVEN" = "true" ]; then
        echo "Installing Maven..."
        sudo dnf install -y maven
    fi

    print_success "JDK installation completed"
}

# Install JDK on macOS
install_jdk_macos() {
    print_section "Installing OpenJDK $JDK_VERSION on macOS"

    # Check if Homebrew is installed
    if ! command_exists brew; then
        print_error "Homebrew is not installed. Please install it first:"
        echo "  /bin/bash -c \"\$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)\""
        exit 1
    fi

    # Install JDK
    echo "Installing OpenJDK $JDK_VERSION..."
    brew install openjdk@${JDK_VERSION}

    # Install Maven if requested
    if [ "$INSTALL_MAVEN" = "true" ]; then
        echo "Installing Maven..."
        brew install maven
    fi

    # Create symlinks for macOS
    echo "Creating symlinks..."
    sudo ln -sfn $(brew --prefix)/opt/openjdk@${JDK_VERSION}/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-${JDK_VERSION}.jdk

    print_success "JDK installation completed"
}

# Setup JAVA_HOME
setup_java_home() {
    print_section "Setting up JAVA_HOME"

    # Find Java installation
    if command_exists java; then
        if [ "$(uname)" = "Darwin" ]; then
            # macOS
            JAVA_HOME_PATH=$(/usr/libexec/java_home 2>/dev/null || echo "")
        else
            # Linux
            JAVA_EXEC=$(which java)
            if [ -L "$JAVA_EXEC" ]; then
                JAVA_EXEC=$(readlink -f "$JAVA_EXEC")
            fi
            JAVA_HOME_PATH=$(dirname $(dirname "$JAVA_EXEC"))
        fi

        if [ -n "$JAVA_HOME_PATH" ] && [ -d "$JAVA_HOME_PATH" ]; then
            echo "Found JAVA_HOME: $JAVA_HOME_PATH"

            # Determine shell config file
            SHELL_CONFIG=""
            if [ -n "$BASH_VERSION" ]; then
                SHELL_CONFIG="$HOME/.bashrc"
            elif [ -n "$ZSH_VERSION" ]; then
                SHELL_CONFIG="$HOME/.zshrc"
            else
                SHELL_CONFIG="$HOME/.profile"
            fi

            # Add JAVA_HOME to shell config if not already present
            if [ -f "$SHELL_CONFIG" ]; then
                if ! grep -q "JAVA_HOME" "$SHELL_CONFIG"; then
                    echo "" >> "$SHELL_CONFIG"
                    echo "# Java Environment" >> "$SHELL_CONFIG"
                    echo "export JAVA_HOME=\"$JAVA_HOME_PATH\"" >> "$SHELL_CONFIG"
                    echo "export PATH=\"\$JAVA_HOME/bin:\$PATH\"" >> "$SHELL_CONFIG"
                    print_success "Added JAVA_HOME to $SHELL_CONFIG"
                else
                    print_success "JAVA_HOME already configured in $SHELL_CONFIG"
                fi
            fi

            # Export for current session
            export JAVA_HOME="$JAVA_HOME_PATH"
            export PATH="$JAVA_HOME/bin:$PATH"

            print_success "JAVA_HOME set to: $JAVA_HOME"
        else
            print_warning "Could not determine JAVA_HOME automatically"
        fi
    else
        print_error "Java not found. Cannot set JAVA_HOME"
    fi
}

# Main installation flow
main() {
    print_section "Java Environment Installer"

    # Detect OS
    detect_os

    # Check if already installed
    if check_java; then
        print_success "Java environment is already configured!"
        read -p "Do you want to reinstall? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 0
        fi
    fi

    # Install based on OS
    case "$OS" in
        *Ubuntu*|*Debian*|*ubuntu*|*debian*)
            install_jdk_debian
            ;;
        *Fedora*|*CentOS*|*Red\ Hat*|*RHEL*|*rhel*)
            install_jdk_rhel
            ;;
        *macOS*|Darwin)
            install_jdk_macos
            ;;
        *)
            print_error "Unsupported OS: $OS"
            echo ""
            echo "Please install Java manually:"
            echo "  - Download from: https://adoptium.net/"
            echo "  - Or use your OS package manager"
            exit 1
            ;;
    esac

    # Setup JAVA_HOME
    setup_java_home

    # Verify installation
    print_section "Verifying Installation"
    if check_java; then
        print_success "Java environment successfully installed!"
        echo ""
        echo "Please reload your shell configuration:"
        echo "  source ~/.bashrc  # For Bash"
        echo "  source ~/.zshrc   # For Zsh"
        echo ""
        echo "Or open a new terminal window."
    else
        print_error "Installation verification failed"
        exit 1
    fi
}

# Show usage
usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -j, --jdk-version VERSION    JDK version to install (default: 21)"
    echo "  -m, --no-maven              Skip Maven installation"
    echo "  -h, --help                  Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                     # Install JDK 21 and Maven"
    echo "  $0 -j 17               # Install JDK 17 and Maven"
    echo "  $0 -j 21 -m            # Install only JDK 21, skip Maven"
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -j|--jdk-version)
            JDK_VERSION="$2"
            shift 2
            ;;
        -m|--no-maven)
            INSTALL_MAVEN="false"
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            usage
            exit 1
            ;;
    esac
done

# Run main installation
main