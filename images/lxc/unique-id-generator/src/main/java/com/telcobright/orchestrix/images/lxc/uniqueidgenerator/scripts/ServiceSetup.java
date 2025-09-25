package com.telcobright.orchestrix.images.lxc.uniqueidgenerator.scripts;

import com.telcobright.orchestrix.images.lxc.uniqueidgenerator.entities.UniqueIdConfig;

/**
 * Service setup scripts for Unique ID Generator
 * This task is retryable independently
 */
public class ServiceSetup {

    private static final String CREATE_DIRECTORIES_SCRIPT = """
        #!/bin/bash
        # Create service directories

        echo "Creating service directories..."

        # Create application directory
        mkdir -p /opt/unique-id-generator

        # Create data directory
        mkdir -p %s

        # Create log directory
        mkdir -p $(dirname %s)

        echo "✓ Directories created"
        """;

    private static final String SET_PERMISSIONS_SCRIPT = """
        #!/bin/bash
        # Set proper permissions

        USER="%s"
        GROUP="%s"

        echo "Setting permissions..."

        # Application directory
        chown -R $USER:$GROUP /opt/unique-id-generator

        # Data directory
        chown $USER:$GROUP %s

        # Log file
        touch %s
        chown $USER:$GROUP %s

        echo "✓ Permissions set"
        """;

    private static final String INSTALL_NPM_DEPENDENCIES_SCRIPT = """
        #!/bin/bash
        # Install npm dependencies

        echo "Installing npm dependencies..."

        cd /opt/unique-id-generator

        # Install production dependencies only
        npm install --production

        if [ $? -eq 0 ]; then
            echo "✓ npm dependencies installed"

            # List installed packages
            npm list --depth=0
        else
            echo "✗ Failed to install npm dependencies"
            exit 1
        fi
        """;

    private static final String VERIFY_SERVICE_FILES_SCRIPT = """
        #!/bin/bash
        # Verify service files are in place

        echo "Verifying service files..."

        FILES=(
            "/opt/unique-id-generator/server.js"
            "/opt/unique-id-generator/package.json"
            "/opt/unique-id-generator/node_modules/express"
        )

        for FILE in "${FILES[@]}"; do
            if [ -e "$FILE" ]; then
                echo "✓ Found: $FILE"
            else
                echo "✗ Missing: $FILE"
                exit 1
            fi
        done

        echo "✓ All service files verified"
        """;

    private static final String CREATE_ENVIRONMENT_FILE_SCRIPT = """
        #!/bin/bash
        # Create environment configuration file

        echo "Creating environment configuration..."

        cat > /opt/unique-id-generator/.env << 'EOF'
        # Service Configuration
        NODE_ENV=production
        PORT=%d
        DATA_DIR=%s
        LOG_FILE=%s

        # These will be overridden at runtime
        SHARD_ID=1
        TOTAL_SHARDS=1
        EOF

        echo "✓ Environment file created"
        """;

    private static final String TEST_SERVICE_STARTUP_SCRIPT = """
        #!/bin/bash
        # Test if service can start (without systemd)

        echo "Testing service startup..."

        cd /opt/unique-id-generator

        # Try to start the service in background for quick test
        timeout 5 node server.js &
        PID=$!

        # Wait a bit for startup
        sleep 2

        # Check if process is running
        if kill -0 $PID 2>/dev/null; then
            echo "✓ Service starts successfully"

            # Kill the test process
            kill $PID 2>/dev/null
            wait $PID 2>/dev/null
        else
            echo "✗ Service failed to start"
            exit 1
        fi
        """;

    private final UniqueIdConfig config;

    public ServiceSetup(UniqueIdConfig config) {
        this.config = config;
    }

    /**
     * Get directory creation script
     */
    public String getCreateDirectoriesScript() {
        return String.format(CREATE_DIRECTORIES_SCRIPT,
            config.getDataDirectory(),
            config.getLogFile());
    }

    /**
     * Get permissions script
     */
    public String getSetPermissionsScript() {
        return String.format(SET_PERMISSIONS_SCRIPT,
            config.getServiceUser(),
            config.getServiceGroup(),
            config.getDataDirectory(),
            config.getLogFile(),
            config.getLogFile());
    }

    /**
     * Get npm dependencies installation script
     */
    public String getInstallDependenciesScript() {
        return INSTALL_NPM_DEPENDENCIES_SCRIPT;
    }

    /**
     * Get service files verification script
     */
    public String getVerifyFilesScript() {
        return VERIFY_SERVICE_FILES_SCRIPT;
    }

    /**
     * Get environment file creation script
     */
    public String getCreateEnvFileScript() {
        return String.format(CREATE_ENVIRONMENT_FILE_SCRIPT,
            config.getServicePort(),
            config.getDataDirectory(),
            config.getLogFile());
    }

    /**
     * Get service startup test script
     */
    public String getTestStartupScript() {
        return TEST_SERVICE_STARTUP_SCRIPT;
    }

    /**
     * Get all service setup scripts in order
     */
    public String[] getAllScripts() {
        return new String[] {
            getCreateDirectoriesScript(),
            getInstallDependenciesScript(),
            getSetPermissionsScript(),
            getCreateEnvFileScript(),
            getVerifyFilesScript(),
            getTestStartupScript()
        };
    }
}