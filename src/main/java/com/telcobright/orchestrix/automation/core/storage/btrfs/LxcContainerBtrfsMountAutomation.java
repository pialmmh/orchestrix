package com.telcobright.orchestrix.automation.core.storage.btrfs;

import com.telcobright.orchestrix.automation.devices.server.linux.base.AbstractLinuxAutomation;
import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxDistribution;
import com.telcobright.orchestrix.automation.core.storage.base.*;
import com.telcobright.orchestrix.automation.core.device.SshDevice;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.io.FileInputStream;
import java.util.Properties;

/**
 * Automation for creating and managing BTRFS mounts for LXC containers
 */
public class LxcContainerBtrfsMountAutomation extends AbstractLinuxAutomation {

    private final StorageVolumeConfig volumeConfig;
    private final Map<String, StorageLocation> storageLocations;
    private final String containerName;

    public LxcContainerBtrfsMountAutomation(LinuxDistribution distribution,
                                           StorageVolumeConfig volumeConfig,
                                           String containerName,
                                           boolean useSudo) {
        super(distribution, useSudo);
        this.volumeConfig = volumeConfig;
        this.containerName = containerName;
        this.storageLocations = new HashMap<>();
    }

    /**
     * Load storage locations from system configuration
     */
    public boolean loadStorageLocations(SshDevice device) throws Exception {
        String configPath = "/etc/orchestrix/storage-locations.conf";

        // Check if config file exists
        String result = executeCommand(device, "test -f " + configPath + " && echo exists");
        if (result == null || !result.contains("exists")) {
            logger.warning("Storage locations config not found at " + configPath);
            return false;
        }

        // Read the config file
        result = executeCommand(device, "cat " + configPath);
        if (result == null) {
            return false;
        }

        // Parse the configuration
        Properties props = new Properties();
        props.load(new java.io.StringReader(result));

        Map<String, Map<String, String>> locationData = new HashMap<>();

        // Group properties by location ID
        for (String key : props.stringPropertyNames()) {
            if (key.contains(".")) {
                String[] parts = key.split("\\.", 2);
                String locationId = parts[0];
                String property = parts[1];

                locationData.computeIfAbsent(locationId, k -> new HashMap<>())
                    .put(property, props.getProperty(key));
            }
        }

        // Create StorageLocation objects
        for (Map.Entry<String, Map<String, String>> entry : locationData.entrySet()) {
            String locationId = entry.getKey();
            Map<String, String> props2 = entry.getValue();

            StorageLocation location = new StorageLocation.Builder()
                .id(locationId)
                .path(props2.get("path"))
                .technology(StorageTechnology.fromCode(props2.getOrDefault("provider", "btrfs")))
                .storageType(StorageLocation.StorageType.fromCode(props2.getOrDefault("type", "sata")))
                .description(props2.get("description"))
                .build();

            storageLocations.put(locationId, location);
        }

        logger.info("Loaded " + storageLocations.size() + " storage locations");
        return !storageLocations.isEmpty();
    }

    @Override
    public boolean execute(SshDevice device) throws Exception {
        logger.info("Setting up BTRFS mount for container: " + containerName);

        // Load storage locations
        if (!loadStorageLocations(device)) {
            logger.severe("Failed to load storage locations");
            return false;
        }

        // Get the storage location
        StorageLocation location = storageLocations.get(volumeConfig.getLocationId());
        if (location == null) {
            logger.severe("Storage location not found: " + volumeConfig.getLocationId());
            return false;
        }

        // Verify BTRFS is available
        BtrfsInstallAutomation btrfsInstaller = new BtrfsInstallAutomation(supportedDistribution, useSudo);
        if (!btrfsInstaller.verify(device)) {
            logger.info("BTRFS not installed, installing...");
            if (!btrfsInstaller.execute(device)) {
                logger.severe("Failed to install BTRFS");
                return false;
            }
        }

        // Create the BTRFS subvolume
        String volumePath = location.getPath() + "/containers/" + volumeConfig.getContainerRoot();
        if (!createBtrfsSubvolume(device, volumePath)) {
            logger.severe("Failed to create BTRFS subvolume");
            return false;
        }

        // Set quota if specified
        if (volumeConfig.getQuotaBytes() > 0) {
            if (!setBtrfsQuota(device, volumePath, volumeConfig.getQuotaBytes())) {
                logger.warning("Failed to set BTRFS quota");
            }
        }

        // Enable compression if requested
        if (volumeConfig.isCompression()) {
            if (!enableBtrfsCompression(device, volumePath)) {
                logger.warning("Failed to enable compression");
            }
        }

        // Configure LXC container mount
        if (!configureLxcMount(device, volumePath)) {
            logger.severe("Failed to configure LXC mount");
            return false;
        }

        // Setup snapshot schedule if enabled
        if (volumeConfig.isSnapshotEnabled()) {
            if (!setupSnapshotSchedule(device, volumePath)) {
                logger.warning("Failed to setup snapshot schedule");
            }
        }

        return true;
    }

    @Override
    public boolean verify(SshDevice device) throws Exception {
        // Get the storage location
        if (!loadStorageLocations(device)) {
            return false;
        }

        StorageLocation location = storageLocations.get(volumeConfig.getLocationId());
        if (location == null) {
            return false;
        }

        // Check if subvolume exists
        String volumePath = location.getPath() + "/containers/" + volumeConfig.getContainerRoot();
        String result = executeCommand(device, "btrfs subvolume show " + volumePath + " 2>/dev/null");

        return result != null && result.contains(volumePath);
    }

    /**
     * Create a BTRFS subvolume for the container
     */
    private boolean createBtrfsSubvolume(SshDevice device, String path) throws Exception {
        // Create parent directory if it doesn't exist
        String parentDir = path.substring(0, path.lastIndexOf('/'));
        executeCommand(device, "mkdir -p " + parentDir);

        // Check if subvolume already exists
        String result = executeCommand(device, "btrfs subvolume show " + path + " 2>/dev/null");
        if (result != null && result.contains(path)) {
            logger.info("Subvolume already exists: " + path);
            return true;
        }

        // Create the subvolume
        result = executeCommand(device, "btrfs subvolume create " + path);
        return result != null && !result.contains("ERROR");
    }

    /**
     * Set BTRFS quota for a subvolume
     */
    private boolean setBtrfsQuota(SshDevice device, String path, long quotaBytes) throws Exception {
        // Enable quota on the filesystem
        String mountPoint = getMountPoint(device, path);
        String result = executeCommand(device, "btrfs quota enable " + mountPoint);

        // Create quota group for the subvolume
        result = executeCommand(device, "btrfs subvolume show " + path + " | grep 'Subvolume ID:' | awk '{print $3}'");
        if (result == null) {
            return false;
        }

        String subvolumeId = result.trim();

        // Set the quota limit
        String quotaCmd = String.format("btrfs qgroup limit %d 0/%s %s",
            quotaBytes, subvolumeId, mountPoint);
        result = executeCommand(device, quotaCmd);

        return result != null && !result.contains("ERROR");
    }

    /**
     * Enable compression on a BTRFS subvolume
     */
    private boolean enableBtrfsCompression(SshDevice device, String path) throws Exception {
        // Set compression property
        String result = executeCommand(device, "btrfs property set " + path + " compression lzo");
        return result != null && !result.contains("ERROR");
    }

    /**
     * Configure LXC container to use the BTRFS subvolume
     */
    private boolean configureLxcMount(SshDevice device, String volumePath) throws Exception {
        // Check if container exists
        String result = executeCommand(device, "lxc-ls | grep -w " + containerName);
        if (result == null || result.isEmpty()) {
            logger.info("Container does not exist yet: " + containerName);
            return true; // Not an error, container will be created later
        }

        // Add disk device to LXC container
        String mountCmd = String.format("lxc config device add %s root disk source=%s path=/",
            containerName, volumePath);
        result = executeCommand(device, mountCmd);

        return result != null && !result.contains("error");
    }

    /**
     * Setup automatic snapshot schedule
     */
    private boolean setupSnapshotSchedule(SshDevice device, String volumePath) throws Exception {
        // Create snapshot directory
        String snapshotDir = volumePath.replace("/containers/", "/snapshots/");
        executeCommand(device, "mkdir -p " + snapshotDir);

        // Create snapshot script
        String scriptPath = "/usr/local/bin/btrfs-snapshot-" + containerName + ".sh";
        String scriptContent = generateSnapshotScript(volumePath, snapshotDir);

        String createScriptCmd = String.format("cat > %s << 'EOF'\n%s\nEOF",
            scriptPath, scriptContent);
        executeCommand(device, createScriptCmd);
        executeCommand(device, "chmod +x " + scriptPath);

        // Add to crontab based on frequency
        String cronSchedule = getCronSchedule(volumeConfig.getSnapshotFrequency());
        String cronEntry = String.format("%s %s", cronSchedule, scriptPath);

        // Add to crontab
        String addCronCmd = String.format(
            "(crontab -l 2>/dev/null | grep -v '%s'; echo '%s') | crontab -",
            scriptPath, cronEntry);
        String result = executeCommand(device, addCronCmd);

        return result != null;
    }

    /**
     * Generate snapshot script content
     */
    private String generateSnapshotScript(String volumePath, String snapshotDir) {
        return String.format(
            "#!/bin/bash\n" +
            "# BTRFS snapshot script for %s\n" +
            "TIMESTAMP=$(date +%%Y%%m%%d_%%H%%M%%S)\n" +
            "SNAPSHOT_NAME=\"%s_${TIMESTAMP}\"\n" +
            "SNAPSHOT_PATH=\"%s/${SNAPSHOT_NAME}\"\n" +
            "\n" +
            "# Create snapshot\n" +
            "btrfs subvolume snapshot -r %s \"${SNAPSHOT_PATH}\"\n" +
            "\n" +
            "# Clean old snapshots (keep last %d)\n" +
            "ls -dt %s/* 2>/dev/null | tail -n +%d | xargs -r btrfs subvolume delete\n",
            containerName, containerName, snapshotDir, volumePath,
            volumeConfig.getSnapshotRetention(), snapshotDir,
            volumeConfig.getSnapshotRetention() + 1);
    }

    /**
     * Get cron schedule based on frequency
     */
    private String getCronSchedule(String frequency) {
        switch (frequency.toLowerCase()) {
            case "hourly":
                return "0 * * * *";
            case "daily":
                return "0 2 * * *";
            case "weekly":
                return "0 2 * * 0";
            case "monthly":
                return "0 2 1 * *";
            default:
                return "0 2 * * *"; // Default to daily
        }
    }

    /**
     * Get mount point for a path
     */
    private String getMountPoint(SshDevice device, String path) throws Exception {
        String result = executeCommand(device, "df " + path + " | tail -1 | awk '{print $6}'");
        return result != null ? result.trim() : "/";
    }

    /**
     * Create a snapshot manually
     */
    public boolean createSnapshot(SshDevice device, String snapshotName) throws Exception {
        if (!loadStorageLocations(device)) {
            return false;
        }

        StorageLocation location = storageLocations.get(volumeConfig.getLocationId());
        if (location == null) {
            return false;
        }

        String volumePath = location.getPath() + "/containers/" + volumeConfig.getContainerRoot();
        String snapshotPath = volumePath.replace("/containers/", "/snapshots/") + "/" + snapshotName;

        // Create snapshot directory if it doesn't exist
        String snapshotDir = snapshotPath.substring(0, snapshotPath.lastIndexOf('/'));
        executeCommand(device, "mkdir -p " + snapshotDir);

        // Create read-only snapshot
        String result = executeCommand(device, "btrfs subvolume snapshot -r " + volumePath + " " + snapshotPath);
        return result != null && !result.contains("ERROR");
    }

    /**
     * List snapshots for the container
     */
    public List<String> listSnapshots(SshDevice device) throws Exception {
        List<String> snapshots = new ArrayList<>();

        if (!loadStorageLocations(device)) {
            return snapshots;
        }

        StorageLocation location = storageLocations.get(volumeConfig.getLocationId());
        if (location == null) {
            return snapshots;
        }

        String snapshotDir = location.getPath() + "/snapshots/" + volumeConfig.getContainerRoot();
        String result = executeCommand(device, "ls -1 " + snapshotDir + " 2>/dev/null");

        if (result != null) {
            for (String line : result.split("\n")) {
                if (!line.trim().isEmpty()) {
                    snapshots.add(line.trim());
                }
            }
        }

        return snapshots;
    }

    @Override
    protected boolean isPackageInstalled(SshDevice device, String packageName) throws Exception {
        String result = executeCommand(device, "which " + packageName);
        return result != null && !result.isEmpty();
    }

    @Override
    protected String getPackageManagerCommand() {
        return "apt-get";
    }

    @Override
    public Map<String, String> getStatus(SshDevice device) {
        Map<String, String> status = new HashMap<>();
        try {
            status.put("container", containerName);
            status.put("storage_location", volumeConfig.getLocationId());
            status.put("container_root", volumeConfig.getContainerRoot());
            status.put("verified", String.valueOf(verify(device)));

            // Get volume usage if it exists
            if (loadStorageLocations(device)) {
                StorageLocation location = storageLocations.get(volumeConfig.getLocationId());
                if (location != null) {
                    String volumePath = location.getPath() + "/containers/" + volumeConfig.getContainerRoot();
                    String result = executeCommand(device, "btrfs qgroup show " + volumePath + " 2>/dev/null | tail -1");
                    if (result != null && !result.isEmpty()) {
                        status.put("quota_info", result.trim());
                    }
                }
            }

        } catch (Exception e) {
            status.put("error", e.getMessage());
        }
        return status;
    }

    @Override
    public String getName() {
        return "LXC Container BTRFS Mount";
    }

    @Override
    public String getDescription() {
        return "Configure BTRFS storage mount for LXC container: " + containerName;
    }
}