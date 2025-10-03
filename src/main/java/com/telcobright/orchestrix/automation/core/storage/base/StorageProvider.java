package com.telcobright.orchestrix.automation.core.storage.base;

import com.telcobright.orchestrix.device.SshDevice;
import java.util.List;
import java.util.Map;

/**
 * Base interface for storage providers (BTRFS, LVM, Ceph, etc.)
 */
public interface StorageProvider {

    /**
     * Check if the storage provider is installed on the device
     */
    boolean isInstalled(SshDevice device) throws Exception;

    /**
     * Install the storage provider on the device
     */
    boolean install(SshDevice device) throws Exception;

    /**
     * Create a storage volume
     */
    StorageVolume createVolume(SshDevice device, StorageVolumeConfig config) throws Exception;

    /**
     * Delete a storage volume
     */
    boolean deleteVolume(SshDevice device, String volumePath) throws Exception;

    /**
     * Set quota for a volume
     */
    boolean setQuota(SshDevice device, String volumePath, long quotaBytes) throws Exception;

    /**
     * Create a snapshot of a volume
     */
    boolean createSnapshot(SshDevice device, String volumePath, String snapshotName) throws Exception;

    /**
     * List snapshots for a volume
     */
    List<String> listSnapshots(SshDevice device, String volumePath) throws Exception;

    /**
     * Delete a snapshot
     */
    boolean deleteSnapshot(SshDevice device, String volumePath, String snapshotName) throws Exception;

    /**
     * Restore from snapshot
     */
    boolean restoreSnapshot(SshDevice device, String volumePath, String snapshotName) throws Exception;

    /**
     * Get volume statistics
     */
    Map<String, Object> getVolumeStats(SshDevice device, String volumePath) throws Exception;

    /**
     * Check if volume exists
     */
    boolean volumeExists(SshDevice device, String volumePath) throws Exception;

    /**
     * Get storage technology type
     */
    StorageTechnology getType();

    /**
     * Get provider name
     */
    String getName();

    /**
     * Verify provider health
     */
    boolean verifyHealth(SshDevice device) throws Exception;
}