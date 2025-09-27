package com.telcobright.orchestrix.automation.storage.btrfs;

import com.telcobright.orchestrix.automation.storage.base.*;
import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxDistribution;
import com.telcobright.orchestrix.automation.devices.server.linux.common.SystemDetector;
import com.telcobright.orchestrix.device.SshDevice;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * BTRFS implementation of StorageProvider
 */
public class BtrfsStorageProvider implements StorageProvider {

    private static final Logger logger = Logger.getLogger(BtrfsStorageProvider.class.getName());
    private final BtrfsInstallAutomation installer;
    private final boolean useSudo;

    public BtrfsStorageProvider(boolean useSudo) {
        this.useSudo = useSudo;
        this.installer = new BtrfsInstallAutomation(LinuxDistribution.UNKNOWN, useSudo);
    }

    public BtrfsStorageProvider(LinuxDistribution distribution, boolean useSudo) {
        this.useSudo = useSudo;
        this.installer = new BtrfsInstallAutomation(distribution, useSudo);
    }

    @Override
    public boolean isInstalled(SshDevice device) throws Exception {
        return installer.verify(device);
    }

    @Override
    public boolean install(SshDevice device) throws Exception {
        return installer.execute(device);
    }

    @Override
    public StorageVolume createVolume(SshDevice device, StorageVolumeConfig config) throws Exception {
        // Get storage location path from system configuration
        String locationPath = getLocationPath(device, config.getLocationId());
        if (locationPath == null) {
            throw new IllegalArgumentException("Storage location not found: " + config.getLocationId());
        }

        String volumePath = locationPath + "/containers/" + config.getContainerRoot();

        // Create parent directory
        String parentDir = volumePath.substring(0, volumePath.lastIndexOf('/'));
        executeCommand(device, "mkdir -p " + parentDir);

        // Check if subvolume already exists
        String result = executeCommand(device, "btrfs subvolume show " + volumePath + " 2>/dev/null");
        if (result != null && result.contains(volumePath)) {
            logger.info("Subvolume already exists: " + volumePath);
        } else {
            // Create the subvolume
            result = executeCommand(device, "btrfs subvolume create " + volumePath);
            if (result == null || result.contains("ERROR")) {
                throw new RuntimeException("Failed to create BTRFS subvolume: " + volumePath);
            }
        }

        // Set quota if specified
        if (config.getQuotaBytes() > 0) {
            setQuota(device, volumePath, config.getQuotaBytes());
        }

        // Enable compression if requested
        if (config.isCompression()) {
            executeCommand(device, "btrfs property set " + volumePath + " compression lzo");
        }

        // Create and return StorageVolume object
        StorageLocation location = new StorageLocation.Builder()
            .id(config.getLocationId())
            .path(locationPath)
            .technology(StorageTechnology.BTRFS)
            .build();

        StorageVolume volume = new StorageVolume(volumePath, config.getContainerRoot(),
            location, config.getQuotaBytes());
        volume.setCompression(config.isCompression());
        volume.setContainerName(config.getContainerRoot());

        return volume;
    }

    @Override
    public boolean deleteVolume(SshDevice device, String volumePath) throws Exception {
        // First try to delete as subvolume
        String result = executeCommand(device, "btrfs subvolume delete " + volumePath);
        return result != null && !result.contains("ERROR");
    }

    @Override
    public boolean setQuota(SshDevice device, String volumePath, long quotaBytes) throws Exception {
        // Get mount point
        String mountPoint = getMountPoint(device, volumePath);

        // Enable quota on the filesystem
        executeCommand(device, "btrfs quota enable " + mountPoint);

        // Get subvolume ID
        String result = executeCommand(device,
            "btrfs subvolume show " + volumePath + " | grep 'Subvolume ID:' | awk '{print $3}'");
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

    @Override
    public boolean createSnapshot(SshDevice device, String volumePath, String snapshotName)
            throws Exception {
        String snapshotPath = volumePath.replace("/containers/", "/snapshots/") + "/" + snapshotName;

        // Create snapshot directory if it doesn't exist
        String snapshotDir = snapshotPath.substring(0, snapshotPath.lastIndexOf('/'));
        executeCommand(device, "mkdir -p " + snapshotDir);

        // Create read-only snapshot
        String result = executeCommand(device,
            "btrfs subvolume snapshot -r " + volumePath + " " + snapshotPath);
        return result != null && !result.contains("ERROR");
    }

    @Override
    public List<String> listSnapshots(SshDevice device, String volumePath) throws Exception {
        List<String> snapshots = new ArrayList<>();
        String snapshotDir = volumePath.replace("/containers/", "/snapshots/");

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
    public boolean deleteSnapshot(SshDevice device, String volumePath, String snapshotName)
            throws Exception {
        String snapshotPath = volumePath.replace("/containers/", "/snapshots/") + "/" + snapshotName;
        return deleteVolume(device, snapshotPath);
    }

    @Override
    public boolean restoreSnapshot(SshDevice device, String volumePath, String snapshotName)
            throws Exception {
        String snapshotPath = volumePath.replace("/containers/", "/snapshots/") + "/" + snapshotName;

        // Rename current volume
        String backupPath = volumePath + ".backup_" + System.currentTimeMillis();
        executeCommand(device, "mv " + volumePath + " " + backupPath);

        // Create new subvolume from snapshot
        String result = executeCommand(device,
            "btrfs subvolume snapshot " + snapshotPath + " " + volumePath);

        if (result != null && !result.contains("ERROR")) {
            // Delete backup
            deleteVolume(device, backupPath);
            return true;
        } else {
            // Restore backup on failure
            executeCommand(device, "mv " + backupPath + " " + volumePath);
            return false;
        }
    }

    @Override
    public Map<String, Object> getVolumeStats(SshDevice device, String volumePath) throws Exception {
        Map<String, Object> stats = new HashMap<>();

        // Get basic subvolume info
        String result = executeCommand(device, "btrfs subvolume show " + volumePath);
        if (result != null) {
            stats.put("info", result);
        }

        // Get quota usage
        result = executeCommand(device,
            "btrfs qgroup show " + volumePath + " 2>/dev/null | tail -1");
        if (result != null && !result.isEmpty()) {
            String[] parts = result.split("\\s+");
            if (parts.length >= 3) {
                try {
                    long referenced = Long.parseLong(parts[1]);
                    long exclusive = Long.parseLong(parts[2]);
                    stats.put("referenced_bytes", referenced);
                    stats.put("exclusive_bytes", exclusive);
                } catch (NumberFormatException e) {
                    // Ignore parsing errors
                }
            }
        }

        // Get filesystem usage
        String mountPoint = getMountPoint(device, volumePath);
        result = executeCommand(device, "btrfs filesystem df " + mountPoint);
        if (result != null) {
            stats.put("filesystem_usage", result);
        }

        return stats;
    }

    @Override
    public boolean volumeExists(SshDevice device, String volumePath) throws Exception {
        String result = executeCommand(device, "btrfs subvolume show " + volumePath + " 2>/dev/null");
        return result != null && result.contains(volumePath);
    }

    @Override
    public StorageTechnology getType() {
        return StorageTechnology.BTRFS;
    }

    @Override
    public String getName() {
        return "BTRFS Storage Provider";
    }

    @Override
    public boolean verifyHealth(SshDevice device) throws Exception {
        // Check if BTRFS is installed and working
        if (!isInstalled(device)) {
            return false;
        }

        // Check if any BTRFS filesystems are mounted
        String result = executeCommand(device, "mount -t btrfs");
        return result != null && !result.isEmpty();
    }

    /**
     * Helper method to execute commands via SSH
     */
    private String executeCommand(SshDevice device, String command) throws Exception {
        if (useSudo && !command.startsWith("sudo")) {
            command = "sudo " + command;
        }
        return device.sendAndReceive(command).get();
    }

    /**
     * Get storage location path from system configuration
     */
    private String getLocationPath(SshDevice device, String locationId) throws Exception {
        String configPath = "/etc/orchestrix/storage-locations.conf";
        String result = executeCommand(device,
            "grep '^" + locationId + ".path=' " + configPath + " 2>/dev/null | cut -d'=' -f2");
        return (result != null && !result.trim().isEmpty()) ? result.trim() : null;
    }

    /**
     * Get mount point for a path
     */
    private String getMountPoint(SshDevice device, String path) throws Exception {
        String result = executeCommand(device, "df " + path + " | tail -1 | awk '{print $6}'");
        return result != null ? result.trim() : "/";
    }
}