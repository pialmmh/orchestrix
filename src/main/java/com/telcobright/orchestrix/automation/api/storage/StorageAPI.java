package com.telcobright.orchestrix.automation.api.storage;

import com.telcobright.orchestrix.automation.api.model.AutomationOperationResult;

/**
 * API for storage automation operations.
 *
 * <p>This interface provides methods for managing storage systems including:
 * <ul>
 *   <li>BTRFS filesystem creation and management</li>
 *   <li>Storage volume provisioning and quota management</li>
 *   <li>Container storage mounting and configuration</li>
 *   <li>Snapshot creation and management</li>
 *   <li>Storage compression and optimization</li>
 * </ul>
 *
 * <p>Supports multiple storage technologies:
 * <ul>
 *   <li>BTRFS - Copy-on-write filesystem with snapshots and compression</li>
 *   <li>ZFS - Advanced filesystem with data integrity (future)</li>
 *   <li>LVM - Logical volume management (future)</li>
 * </ul>
 */
public interface StorageAPI {

    /**
     * Install and configure BTRFS filesystem support on a Linux system.
     *
     * <p>This operation:
     * <ul>
     *   <li>Installs BTRFS utilities (btrfs-progs)</li>
     *   <li>Loads BTRFS kernel module</li>
     *   <li>Configures BTRFS to load on boot</li>
     *   <li>Verifies installation and module loading</li>
     * </ul>
     *
     * @param configFile Path to configuration file with installation parameters
     * @return Result of the installation including version and status
     * @throws Exception if installation fails or system is incompatible
     *
     * @example
     * <pre>
     * StorageAPI api = new StorageAPIImpl();
     * AutomationOperationResult result = api.installBtrfs("/path/to/btrfs-install.conf");
     * if (result.isSuccess()) {
     *     System.out.println("BTRFS installed: " + result.getMessage());
     * }
     * </pre>
     */
    AutomationOperationResult installBtrfs(String configFile) throws Exception;

    /**
     * Create a storage volume with specified configuration.
     *
     * <p>Storage volumes provide:
     * <ul>
     *   <li>Quota limits (size restrictions)</li>
     *   <li>Compression (lzo, zstd, zlib)</li>
     *   <li>Snapshot capability</li>
     *   <li>Automatic rotation and cleanup</li>
     * </ul>
     *
     * <p>Volume configuration includes:
     * <ul>
     *   <li>Location ID (storage backend identifier)</li>
     *   <li>Quota size (e.g., "10G", "500M")</li>
     *   <li>Container root path</li>
     *   <li>Compression settings</li>
     *   <li>Snapshot schedule</li>
     * </ul>
     *
     * @param locationId Storage location identifier (e.g., "btrfs_local_main")
     * @param containerName Name of container for this volume
     * @param quotaSize Size quota in human-readable format (e.g., "10G")
     * @param enableCompression Whether to enable compression
     * @return Result including volume path and configuration
     * @throws Exception if volume creation fails or quota cannot be set
     *
     * @example
     * <pre>
     * StorageAutomationAPI api = new StorageAutomationAPIImpl();
     * AutomationOperationResult result = api.createVolume(
     *     "btrfs_local_main",
     *     "my-container",
     *     "15G",
     *     true
     * );
     * if (result.isSuccess()) {
     *     System.out.println("Volume created: " + result.getMessage());
     * }
     * </pre>
     */
    AutomationOperationResult createVolume(
        String locationId,
        String containerName,
        String quotaSize,
        boolean enableCompression
    ) throws Exception;

    /**
     * Mount storage volume to an LXC container.
     *
     * <p>This operation:
     * <ul>
     *   <li>Creates BTRFS subvolume for container</li>
     *   <li>Sets quota limits on subvolume</li>
     *   <li>Enables compression if requested</li>
     *   <li>Configures LXC container to use the volume</li>
     *   <li>Sets up snapshot schedule</li>
     * </ul>
     *
     * @param containerName Name of LXC container
     * @param volumeConfigFile Path to volume configuration file
     * @return Result of mount operation
     * @throws Exception if mount fails or container not found
     *
     * @example
     * <pre>
     * StorageAutomationAPI api = new StorageAutomationAPIImpl();
     * AutomationOperationResult result = api.mountVolumeToContainer(
     *     "quarkus-app-v1",
     *     "/path/to/volume.conf"
     * );
     * </pre>
     */
    AutomationOperationResult mountVolumeToContainer(
        String containerName,
        String volumeConfigFile
    ) throws Exception;

    /**
     * Create a snapshot of a storage volume.
     *
     * <p>Snapshots are read-only copies of the volume at a point in time.
     * They use copy-on-write, so they consume minimal space initially.
     *
     * @param volumePath Path to the volume to snapshot
     * @param snapshotName Name for the snapshot (e.g., "backup-20251003")
     * @return Result including snapshot path and size
     * @throws Exception if snapshot creation fails
     *
     * @example
     * <pre>
     * StorageAutomationAPI api = new StorageAutomationAPIImpl();
     * AutomationOperationResult result = api.createSnapshot(
     *     "/btrfs/containers/my-container",
     *     "backup-" + System.currentTimeMillis()
     * );
     * </pre>
     */
    AutomationOperationResult createSnapshot(
        String volumePath,
        String snapshotName
    ) throws Exception;

    /**
     * List all snapshots for a volume.
     *
     * @param volumePath Path to the volume
     * @return Result containing list of snapshot names and timestamps
     * @throws Exception if listing fails
     */
    AutomationOperationResult listSnapshots(String volumePath) throws Exception;

    /**
     * Delete a snapshot.
     *
     * @param snapshotPath Path to the snapshot to delete
     * @return Result of deletion operation
     * @throws Exception if deletion fails
     */
    AutomationOperationResult deleteSnapshot(String snapshotPath) throws Exception;

    /**
     * Get storage volume usage and statistics.
     *
     * @param volumePath Path to the volume
     * @return Result containing size, used space, compression ratio, etc.
     * @throws Exception if query fails
     *
     * @example
     * <pre>
     * StorageAutomationAPI api = new StorageAutomationAPIImpl();
     * AutomationOperationResult result = api.getVolumeInfo("/btrfs/containers/my-container");
     * System.out.println("Volume info: " + result.getMessage());
     * </pre>
     */
    AutomationOperationResult getVolumeInfo(String volumePath) throws Exception;

    /**
     * Set or update quota for a storage volume.
     *
     * @param volumePath Path to the volume
     * @param quotaSize New quota size in human-readable format (e.g., "20G")
     * @return Result of quota update
     * @throws Exception if quota update fails
     */
    AutomationOperationResult setVolumeQuota(
        String volumePath,
        String quotaSize
    ) throws Exception;

    /**
     * Delete a storage volume and all its snapshots.
     *
     * @param volumePath Path to the volume
     * @param deleteSnapshots Whether to delete snapshots as well
     * @return Result of deletion
     * @throws Exception if deletion fails
     */
    AutomationOperationResult deleteVolume(
        String volumePath,
        boolean deleteSnapshots
    ) throws Exception;
}
