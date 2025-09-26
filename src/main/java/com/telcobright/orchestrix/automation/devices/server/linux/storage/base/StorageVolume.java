package com.telcobright.orchestrix.automation.devices.server.linux.storage.base;

import java.time.LocalDateTime;

/**
 * Represents a storage volume
 */
public class StorageVolume {
    private String path;
    private String name;
    private StorageLocation location;
    private long quotaBytes;
    private long usedBytes;
    private boolean compression;
    private LocalDateTime created;
    private String containerName;

    public StorageVolume() {}

    public StorageVolume(String path, String name, StorageLocation location, long quotaBytes) {
        this.path = path;
        this.name = name;
        this.location = location;
        this.quotaBytes = quotaBytes;
        this.created = LocalDateTime.now();
    }

    // Getters and setters
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public StorageLocation getLocation() {
        return location;
    }

    public void setLocation(StorageLocation location) {
        this.location = location;
    }

    public long getQuotaBytes() {
        return quotaBytes;
    }

    public void setQuotaBytes(long quotaBytes) {
        this.quotaBytes = quotaBytes;
    }

    public long getUsedBytes() {
        return usedBytes;
    }

    public void setUsedBytes(long usedBytes) {
        this.usedBytes = usedBytes;
    }

    public boolean isCompression() {
        return compression;
    }

    public void setCompression(boolean compression) {
        this.compression = compression;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public double getUsagePercentage() {
        if (quotaBytes == 0) return 0;
        return (double) usedBytes / quotaBytes * 100;
    }

    public String getQuotaHumanReadable() {
        return humanReadableBytes(quotaBytes);
    }

    public String getUsedHumanReadable() {
        return humanReadableBytes(usedBytes);
    }

    private String humanReadableBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    @Override
    public String toString() {
        return String.format("StorageVolume[path=%s, quota=%s, used=%s (%.1f%%)]",
            path, getQuotaHumanReadable(), getUsedHumanReadable(), getUsagePercentage());
    }
}