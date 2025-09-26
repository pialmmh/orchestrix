#!/bin/bash
# Storage monitor script for automatic log rotation based on quota usage
# Triggers rotation when storage exceeds threshold (default 80%)

set -e

# Configuration from environment or defaults
STORAGE_THRESHOLD="${STORAGE_THRESHOLD:-80}"
CHECK_INTERVAL="${CHECK_INTERVAL:-300}"  # 5 minutes default
LOG_FILE="/var/log/storage-monitor.log"
LOKI_DATA_DIR="/var/lib/loki"
FORCE_CLEANUP_THRESHOLD="${FORCE_CLEANUP_THRESHOLD:-90}"  # Force aggressive cleanup at 90%

# Function to get current storage usage percentage
get_storage_usage() {
    # Get filesystem usage for the root mount (where BTRFS quota applies)
    local usage=$(df / | awk 'NR==2 {gsub("%",""); print $5}')
    echo "$usage"
}

# Function to get BTRFS quota usage if available
get_btrfs_quota_usage() {
    # Try to get BTRFS quota information
    if command -v btrfs &> /dev/null; then
        # Get the mount point
        local mount_point=$(df / | awk 'NR==2 {print $6}')

        # Get quota usage
        local quota_info=$(btrfs qgroup show "$mount_point" 2>/dev/null | grep "^0/" | tail -1)
        if [ -n "$quota_info" ]; then
            local used=$(echo "$quota_info" | awk '{print $2}')
            local limit=$(echo "$quota_info" | awk '{print $3}')

            if [ "$limit" != "none" ] && [ "$limit" != "-" ] && [ "$limit" -gt 0 ]; then
                local percentage=$((used * 100 / limit))
                echo "$percentage"
                return
            fi
        fi
    fi

    # Fall back to df
    get_storage_usage
}

# Function to trigger log rotation
trigger_rotation() {
    local usage=$1
    local reason=$2

    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Storage at ${usage}% - Triggering rotation: $reason" >> "$LOG_FILE"

    # Force logrotate for system logs
    logrotate -f /etc/logrotate.d/grafana-loki

    # Trigger Loki compaction immediately
    if systemctl is-active --quiet loki; then
        # Send SIGHUP to trigger immediate compaction
        systemctl reload loki || true

        # Clean old chunks more aggressively
        find "$LOKI_DATA_DIR/chunks" -type f -mtime +1 -delete 2>/dev/null || true
        find "$LOKI_DATA_DIR/boltdb-shipper-cache" -type f -mtime +1 -delete 2>/dev/null || true
    fi

    # Clear Grafana query cache if needed
    if [ "$usage" -ge "$FORCE_CLEANUP_THRESHOLD" ]; then
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] Critical storage level - Forcing aggressive cleanup" >> "$LOG_FILE"

        # Clear Grafana cache
        if [ -d "/var/lib/grafana/png" ]; then
            find /var/lib/grafana/png -type f -delete 2>/dev/null || true
        fi

        # Clear old journal logs
        journalctl --vacuum-size=100M 2>/dev/null || true

        # Clear package cache
        apt-get clean 2>/dev/null || true

        # Reduce Loki retention temporarily
        if [ -f "/etc/loki/config.yaml" ]; then
            # Create emergency config with shorter retention
            sed -i.bak 's/retention_period:.*/retention_period: 24h/' /etc/loki/config.yaml
            systemctl restart loki

            # Restore after cleanup
            (sleep 3600 && mv /etc/loki/config.yaml.bak /etc/loki/config.yaml && systemctl restart loki) &
        fi
    fi
}

# Function to check and cleanup old data
cleanup_old_data() {
    local usage=$1

    # Progressive cleanup based on usage
    if [ "$usage" -ge 85 ]; then
        # Delete debug logs first
        find /var/log -name "*.debug" -type f -delete 2>/dev/null || true
    fi

    if [ "$usage" -ge 90 ]; then
        # Delete older compressed logs
        find /var/log -name "*.gz" -type f -mtime +3 -delete 2>/dev/null || true
    fi

    if [ "$usage" -ge 95 ]; then
        # Emergency: Delete all but most recent logs
        find "$LOKI_DATA_DIR" -type f -mtime +0 -delete 2>/dev/null || true
    fi
}

# Function to send alert (can be extended for actual alerting)
send_alert() {
    local usage=$1
    local message=$2

    echo "[ALERT] Storage at ${usage}%: $message" >> "$LOG_FILE"

    # Log to system journal for visibility
    logger -t storage-monitor -p warning "Storage at ${usage}%: $message"

    # Create a marker file for external monitoring
    echo "${usage}" > /tmp/storage-usage-alert
}

# Main monitoring loop
monitor_storage() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Storage monitor started (threshold: ${STORAGE_THRESHOLD}%)" >> "$LOG_FILE"

    local last_rotation_usage=0
    local rotation_cooldown=0

    while true; do
        # Get current usage
        local usage=$(get_btrfs_quota_usage)

        # Log current usage every hour
        if [ $(($(date +%s) % 3600)) -lt "$CHECK_INTERVAL" ]; then
            echo "[$(date '+%Y-%m-%d %H:%M:%S')] Current storage usage: ${usage}%" >> "$LOG_FILE"
        fi

        # Check if rotation is needed
        if [ "$usage" -ge "$STORAGE_THRESHOLD" ]; then
            # Avoid rotating too frequently (cooldown period)
            if [ "$rotation_cooldown" -eq 0 ] || [ "$usage" -ge "$FORCE_CLEANUP_THRESHOLD" ]; then
                trigger_rotation "$usage" "Threshold exceeded"
                cleanup_old_data "$usage"
                last_rotation_usage=$usage
                rotation_cooldown=12  # Wait 1 hour before next rotation (12 * 5min)

                # Send alerts at different thresholds
                if [ "$usage" -ge 95 ]; then
                    send_alert "$usage" "CRITICAL: Storage nearly full!"
                elif [ "$usage" -ge 90 ]; then
                    send_alert "$usage" "WARNING: Storage critically high"
                elif [ "$usage" -ge 85 ]; then
                    send_alert "$usage" "Storage high, rotation triggered"
                fi
            else
                rotation_cooldown=$((rotation_cooldown - 1))
            fi
        else
            # Reset cooldown if usage drops below threshold
            if [ "$usage" -lt $((STORAGE_THRESHOLD - 10)) ]; then
                rotation_cooldown=0
            fi
        fi

        # Sleep before next check
        sleep "$CHECK_INTERVAL"
    done
}

# Create systemd service for the monitor
create_systemd_service() {
    cat << 'EOF' > /etc/systemd/system/storage-monitor.service
[Unit]
Description=Storage Monitor for Log Rotation
After=loki.service

[Service]
Type=simple
ExecStart=/usr/local/bin/storage-monitor.sh
Restart=always
RestartSec=30
StandardOutput=append:/var/log/storage-monitor.log
StandardError=append:/var/log/storage-monitor.log
Environment="STORAGE_THRESHOLD=80"
Environment="CHECK_INTERVAL=300"
Environment="FORCE_CLEANUP_THRESHOLD=90"

[Install]
WantedBy=multi-user.target
EOF

    # Copy script to system location
    cp "$0" /usr/local/bin/storage-monitor.sh
    chmod +x /usr/local/bin/storage-monitor.sh

    # Enable and start service
    systemctl daemon-reload
    systemctl enable storage-monitor.service
    systemctl start storage-monitor.service
}

# Main execution
case "${1:-monitor}" in
    install)
        echo "Installing storage monitor service..."
        create_systemd_service
        echo "Storage monitor installed and started"
        ;;
    monitor)
        monitor_storage
        ;;
    check)
        usage=$(get_btrfs_quota_usage)
        echo "Current storage usage: ${usage}%"
        if [ "$usage" -ge "$STORAGE_THRESHOLD" ]; then
            echo "WARNING: Storage exceeds threshold of ${STORAGE_THRESHOLD}%"
            exit 1
        fi
        ;;
    *)
        echo "Usage: $0 {install|monitor|check}"
        exit 1
        ;;
esac