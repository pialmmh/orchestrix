package com.telcobright.orchestrix.images.lxc.uniqueidgenerator.scripts;

import com.telcobright.orchestrix.images.lxc.uniqueidgenerator.entities.UniqueIdConfig;

/**
 * Node.js installation scripts for Unique ID Generator container
 * This task is retryable independently
 */
public class NodeJsInstaller {

    private static final String UPDATE_SYSTEM_SCRIPT = """
        #!/bin/bash
        # Update system packages

        echo "Updating package lists..."
        apt-get update

        if [ $? -eq 0 ]; then
            echo "✓ System updated"
        else
            echo "✗ Failed to update system"
            exit 1
        fi
        """;

    private static final String INSTALL_PREREQUISITES_SCRIPT = """
        #!/bin/bash
        # Install prerequisites for Node.js

        echo "Installing prerequisites..."

        DEBIAN_FRONTEND=noninteractive apt-get install -y \\
            curl \\
            ca-certificates \\
            gnupg \\
            lsb-release \\
            software-properties-common

        if [ $? -eq 0 ]; then
            echo "✓ Prerequisites installed"
        else
            echo "✗ Failed to install prerequisites"
            exit 1
        fi
        """;

    private static final String INSTALL_NODEJS_SCRIPT = """
        #!/bin/bash
        # Install Node.js from NodeSource repository

        NODE_VERSION="%s"

        echo "Installing Node.js ${NODE_VERSION}..."

        # Create keyrings directory
        mkdir -p /etc/apt/keyrings

        # Add NodeSource GPG key
        curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key | \\
            gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg

        # Add NodeSource repository
        echo "deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_${NODE_VERSION}.x nodistro main" > \\
            /etc/apt/sources.list.d/nodesource.list

        # Update and install Node.js
        apt-get update
        DEBIAN_FRONTEND=noninteractive apt-get install -y nodejs

        if [ $? -eq 0 ]; then
            echo "✓ Node.js installed"
        else
            echo "✗ Failed to install Node.js"
            exit 1
        fi
        """;

    private static final String VERIFY_NODEJS_SCRIPT = """
        #!/bin/bash
        # Verify Node.js installation

        NODE_VERSION="%s"

        echo "Verifying Node.js installation..."

        # Check node version
        NODE_INSTALLED=$(node --version 2>/dev/null)
        NPM_INSTALLED=$(npm --version 2>/dev/null)

        if [[ "$NODE_INSTALLED" =~ v${NODE_VERSION} ]]; then
            echo "✓ Node.js version: $NODE_INSTALLED"
        else
            echo "✗ Node.js version mismatch or not installed"
            echo "  Expected: v${NODE_VERSION}"
            echo "  Found: $NODE_INSTALLED"
            exit 1
        fi

        if [ -n "$NPM_INSTALLED" ]; then
            echo "✓ npm version: $NPM_INSTALLED"
        else
            echo "✗ npm not found"
            exit 1
        fi

        echo "✓ Node.js verification complete"
        """;

    private static final String INSTALL_ADDITIONAL_TOOLS_SCRIPT = """
        #!/bin/bash
        # Install additional development tools

        echo "Installing additional tools..."

        DEBIAN_FRONTEND=noninteractive apt-get install -y \\
            build-essential \\
            python3 \\
            git \\
            nano \\
            less \\
            systemd

        if [ $? -eq 0 ]; then
            echo "✓ Additional tools installed"
        else
            echo "⚠ Some tools may not have installed (non-critical)"
        fi
        """;

    private final UniqueIdConfig config;

    public NodeJsInstaller(UniqueIdConfig config) {
        this.config = config;
    }

    /**
     * Get system update script
     */
    public String getUpdateSystemScript() {
        return UPDATE_SYSTEM_SCRIPT;
    }

    /**
     * Get prerequisites installation script
     */
    public String getInstallPrerequisitesScript() {
        return INSTALL_PREREQUISITES_SCRIPT;
    }

    /**
     * Get Node.js installation script
     */
    public String getInstallNodeJsScript() {
        return String.format(INSTALL_NODEJS_SCRIPT, config.getNodeVersion());
    }

    /**
     * Get Node.js verification script
     */
    public String getVerifyNodeJsScript() {
        return String.format(VERIFY_NODEJS_SCRIPT, config.getNodeVersion());
    }

    /**
     * Get additional tools installation script
     */
    public String getInstallToolsScript() {
        return INSTALL_ADDITIONAL_TOOLS_SCRIPT;
    }

    /**
     * Get all installation scripts in order
     */
    public String[] getAllScripts() {
        return new String[] {
            getUpdateSystemScript(),
            getInstallPrerequisitesScript(),
            getInstallNodeJsScript(),
            getVerifyNodeJsScript(),
            getInstallToolsScript()
        };
    }

    /**
     * Get critical scripts only (exclude optional tools)
     */
    public String[] getCriticalScripts() {
        return new String[] {
            getUpdateSystemScript(),
            getInstallPrerequisitesScript(),
            getInstallNodeJsScript(),
            getVerifyNodeJsScript()
        };
    }
}