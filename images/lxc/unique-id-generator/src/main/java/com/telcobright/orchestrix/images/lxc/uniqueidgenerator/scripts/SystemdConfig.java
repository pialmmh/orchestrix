package com.telcobright.orchestrix.images.lxc.uniqueidgenerator.scripts;

import com.telcobright.orchestrix.images.lxc.uniqueidgenerator.entities.UniqueIdConfig;

/**
 * Systemd configuration scripts for Unique ID Generator
 * This task is retryable independently
 */
public class SystemdConfig {

    private static final String SYSTEMD_SERVICE_TEMPLATE = """
        [Unit]
        Description=Unique ID Generator Service
        After=network.target

        [Service]
        Type=simple
        User=%s
        Group=%s
        WorkingDirectory=/opt/unique-id-generator
        ExecStart=/usr/bin/node /opt/unique-id-generator/server.js
        Restart=always
        RestartSec=10
        StandardOutput=append:%s
        StandardError=append:%s
        Environment="NODE_ENV=production"
        Environment="PORT=%d"
        Environment="DATA_DIR=%s"
        # Shard configuration will be set at runtime
        Environment="SHARD_ID=1"
        Environment="TOTAL_SHARDS=1"

        # Security settings
        NoNewPrivileges=true
        PrivateTmp=true
        ProtectHome=true
        ProtectSystem=strict
        ReadWritePaths=%s /var/log

        # Resource limits
        MemoryHigh=200M
        MemoryMax=256M
        CPUQuota=50%%

        [Install]
        WantedBy=multi-user.target
        """;

    private static final String CREATE_SERVICE_FILE_SCRIPT = """
        #!/bin/bash
        # Create systemd service file

        echo "Creating systemd service file..."

        cat > /etc/systemd/system/unique-id-generator.service << 'EOF'
        %s
        EOF

        if [ $? -eq 0 ]; then
            echo "✓ Service file created"
        else
            echo "✗ Failed to create service file"
            exit 1
        fi
        """;

    private static final String ENABLE_SERVICE_SCRIPT = """
        #!/bin/bash
        # Enable systemd service

        echo "Enabling service..."

        # Reload systemd daemon
        systemctl daemon-reload

        # Enable service for auto-start
        systemctl enable unique-id-generator.service

        if [ $? -eq 0 ]; then
            echo "✓ Service enabled"
            systemctl status unique-id-generator.service --no-pager || true
        else
            echo "✗ Failed to enable service"
            exit 1
        fi
        """;

    private static final String START_SERVICE_SCRIPT = """
        #!/bin/bash
        # Start the service

        echo "Starting service..."

        systemctl start unique-id-generator.service

        # Wait for service to start
        sleep 3

        # Check if service is active
        if systemctl is-active --quiet unique-id-generator.service; then
            echo "✓ Service started successfully"
            systemctl status unique-id-generator.service --no-pager
        else
            echo "✗ Service failed to start"
            journalctl -u unique-id-generator.service -n 20
            exit 1
        fi
        """;

    private static final String CONFIGURE_LOGROTATE_SCRIPT = """
        #!/bin/bash
        # Configure log rotation

        echo "Configuring log rotation..."

        cat > /etc/logrotate.d/unique-id-generator << 'EOF'
        %s {
            daily
            rotate 7
            compress
            delaycompress
            missingok
            notifempty
            create 0640 %s %s
            postrotate
                systemctl reload unique-id-generator.service > /dev/null 2>&1 || true
            endscript
        }
        EOF

        echo "✓ Log rotation configured"
        """;

    private static final String VERIFY_SERVICE_SCRIPT = """
        #!/bin/bash
        # Verify service is running correctly

        echo "Verifying service..."

        # Check if service is active
        if ! systemctl is-active --quiet unique-id-generator.service; then
            echo "✗ Service is not active"
            exit 1
        fi

        # Check if port is listening
        if netstat -tuln | grep -q ":%d "; then
            echo "✓ Service listening on port %d"
        else
            echo "✗ Service not listening on expected port"
            exit 1
        fi

        # Test health endpoint
        if curl -s -f http://localhost:%d/health > /dev/null 2>&1; then
            echo "✓ Health endpoint responding"
        else
            echo "✗ Health endpoint not responding"
            exit 1
        fi

        echo "✓ Service verification complete"
        """;

    private final UniqueIdConfig config;

    public SystemdConfig(UniqueIdConfig config) {
        this.config = config;
    }

    /**
     * Generate systemd service file content
     */
    public String getServiceFileContent() {
        return String.format(SYSTEMD_SERVICE_TEMPLATE,
            config.getServiceUser(),
            config.getServiceGroup(),
            config.getLogFile(),
            config.getLogFile(),
            config.getServicePort(),
            config.getDataDirectory(),
            config.getDataDirectory());
    }

    /**
     * Get service file creation script
     */
    public String getCreateServiceFileScript() {
        return String.format(CREATE_SERVICE_FILE_SCRIPT,
            getServiceFileContent());
    }

    /**
     * Get service enable script
     */
    public String getEnableServiceScript() {
        return ENABLE_SERVICE_SCRIPT;
    }

    /**
     * Get service start script
     */
    public String getStartServiceScript() {
        return START_SERVICE_SCRIPT;
    }

    /**
     * Get logrotate configuration script
     */
    public String getConfigureLogrotateScript() {
        return String.format(CONFIGURE_LOGROTATE_SCRIPT,
            config.getLogFile(),
            config.getServiceUser(),
            config.getServiceGroup());
    }

    /**
     * Get service verification script
     */
    public String getVerifyServiceScript() {
        return String.format(VERIFY_SERVICE_SCRIPT,
            config.getServicePort(),
            config.getServicePort(),
            config.getServicePort());
    }

    /**
     * Get all systemd scripts in order
     */
    public String[] getAllScripts() {
        return new String[] {
            getCreateServiceFileScript(),
            getEnableServiceScript(),
            getConfigureLogrotateScript()
            // Note: Start and verify are separate as they may be done later
        };
    }

    /**
     * Get runtime scripts (for starting service)
     */
    public String[] getRuntimeScripts() {
        return new String[] {
            getStartServiceScript(),
            getVerifyServiceScript()
        };
    }
}