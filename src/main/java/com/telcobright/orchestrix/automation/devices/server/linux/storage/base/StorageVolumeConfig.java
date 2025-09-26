package com.telcobright.orchestrix.automation.devices.server.linux.storage.base;

import java.util.Properties;
import java.io.IOException;
import java.io.FileInputStream;

/**
 * Configuration for creating a storage volume
 */
public class StorageVolumeConfig {
    private String locationId;
    private String containerRoot;
    private long quotaBytes;
    private boolean compression = false;
    private boolean snapshotEnabled = false;
    private String snapshotFrequency = "daily";
    private int snapshotRetention = 7;

    public StorageVolumeConfig() {}

    public static class Builder {
        private StorageVolumeConfig config = new StorageVolumeConfig();

        public Builder locationId(String locationId) {
            config.locationId = locationId;
            return this;
        }

        public Builder containerRoot(String containerRoot) {
            config.containerRoot = containerRoot;
            return this;
        }

        public Builder quotaBytes(long quotaBytes) {
            config.quotaBytes = quotaBytes;
            return this;
        }

        public Builder quotaHumanSize(String humanSize) {
            config.quotaBytes = parseHumanSize(humanSize);
            return this;
        }

        public Builder compression(boolean compression) {
            config.compression = compression;
            return this;
        }

        public Builder snapshotEnabled(boolean enabled) {
            config.snapshotEnabled = enabled;
            return this;
        }

        public Builder snapshotFrequency(String frequency) {
            config.snapshotFrequency = frequency;
            return this;
        }

        public Builder snapshotRetention(int days) {
            config.snapshotRetention = days;
            return this;
        }

        public StorageVolumeConfig build() {
            return config;
        }

        private long parseHumanSize(String size) {
            size = size.toUpperCase().trim();
            long multiplier = 1;

            if (size.endsWith("G") || size.endsWith("GB")) {
                multiplier = 1024L * 1024L * 1024L;
                size = size.replaceAll("[GB]", "");
            } else if (size.endsWith("M") || size.endsWith("MB")) {
                multiplier = 1024L * 1024L;
                size = size.replaceAll("[MB]", "");
            } else if (size.endsWith("K") || size.endsWith("KB")) {
                multiplier = 1024L;
                size = size.replaceAll("[KB]", "");
            } else if (size.endsWith("T") || size.endsWith("TB")) {
                multiplier = 1024L * 1024L * 1024L * 1024L;
                size = size.replaceAll("[TB]", "");
            }

            return (long)(Double.parseDouble(size) * multiplier);
        }
    }

    /**
     * Load configuration from properties file
     */
    public static StorageVolumeConfig fromProperties(String path) throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(path)) {
            props.load(fis);
        }

        Builder builder = new Builder();

        // Required fields
        String locationId = props.getProperty("storage.location.id");
        if (locationId == null) {
            throw new IllegalArgumentException("storage.location.id is required");
        }
        builder.locationId(locationId);

        String containerRoot = props.getProperty("storage.container.root");
        if (containerRoot == null) {
            throw new IllegalArgumentException("storage.container.root is required");
        }
        builder.containerRoot(containerRoot);

        String quotaSize = props.getProperty("storage.quota.size");
        if (quotaSize == null) {
            throw new IllegalArgumentException("storage.quota.size is required");
        }
        builder.quotaHumanSize(quotaSize);

        // Optional fields
        if (props.containsKey("storage.compression")) {
            builder.compression(Boolean.parseBoolean(props.getProperty("storage.compression")));
        }

        if (props.containsKey("storage.snapshot.enabled")) {
            builder.snapshotEnabled(Boolean.parseBoolean(props.getProperty("storage.snapshot.enabled")));
        }

        if (props.containsKey("storage.snapshot.frequency")) {
            builder.snapshotFrequency(props.getProperty("storage.snapshot.frequency"));
        }

        if (props.containsKey("storage.snapshot.retain")) {
            builder.snapshotRetention(Integer.parseInt(props.getProperty("storage.snapshot.retain")));
        }

        return builder.build();
    }

    // Getters and setters
    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getContainerRoot() {
        return containerRoot;
    }

    public void setContainerRoot(String containerRoot) {
        this.containerRoot = containerRoot;
    }

    public long getQuotaBytes() {
        return quotaBytes;
    }

    public void setQuotaBytes(long quotaBytes) {
        this.quotaBytes = quotaBytes;
    }

    public boolean isCompression() {
        return compression;
    }

    public void setCompression(boolean compression) {
        this.compression = compression;
    }

    public boolean isSnapshotEnabled() {
        return snapshotEnabled;
    }

    public void setSnapshotEnabled(boolean snapshotEnabled) {
        this.snapshotEnabled = snapshotEnabled;
    }

    public String getSnapshotFrequency() {
        return snapshotFrequency;
    }

    public void setSnapshotFrequency(String snapshotFrequency) {
        this.snapshotFrequency = snapshotFrequency;
    }

    public int getSnapshotRetention() {
        return snapshotRetention;
    }

    public void setSnapshotRetention(int snapshotRetention) {
        this.snapshotRetention = snapshotRetention;
    }
}