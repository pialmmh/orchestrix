package com.telcobright.orchestrix.automation.devices.server.linux.storage.base;

import com.telcobright.orchestrix.automation.devices.server.linux.storage.btrfs.BtrfsStorageProvider;
import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxDistribution;
import com.telcobright.orchestrix.device.SshDevice;
import com.telcobright.orchestrix.automation.devices.server.linux.common.SystemDetector;

import java.util.logging.Logger;

/**
 * Factory for creating StorageProvider instances
 */
public class StorageProviderFactory {

    private static final Logger logger = Logger.getLogger(StorageProviderFactory.class.getName());

    /**
     * Create a storage provider for the specified technology
     */
    public static StorageProvider createStorageProvider(StorageTechnology technology,
                                                        boolean useSudo) {
        switch (technology) {
            case BTRFS:
                return new BtrfsStorageProvider(useSudo);

            case LVM:
                // Future implementation
                throw new UnsupportedOperationException("LVM storage provider not yet implemented");

            case ZFS:
                // Future implementation
                throw new UnsupportedOperationException("ZFS storage provider not yet implemented");

            case CEPH:
                // Future implementation
                throw new UnsupportedOperationException("Ceph storage provider not yet implemented");

            default:
                throw new IllegalArgumentException("Unsupported storage technology: " + technology);
        }
    }

    /**
     * Create a storage provider with distribution detection
     */
    public static StorageProvider createStorageProvider(StorageTechnology technology,
                                                        SshDevice device,
                                                        boolean useSudo) {
        LinuxDistribution distribution = SystemDetector.detectDistribution(device);

        switch (technology) {
            case BTRFS:
                return new BtrfsStorageProvider(distribution, useSudo);

            case LVM:
                // Future implementation
                throw new UnsupportedOperationException("LVM storage provider not yet implemented");

            case ZFS:
                // Future implementation
                throw new UnsupportedOperationException("ZFS storage provider not yet implemented");

            case CEPH:
                // Future implementation
                throw new UnsupportedOperationException("Ceph storage provider not yet implemented");

            default:
                throw new IllegalArgumentException("Unsupported storage technology: " + technology);
        }
    }

    /**
     * Auto-detect available storage provider on the device
     */
    public static StorageProvider detectStorageProvider(SshDevice device, boolean useSudo) {
        // Try BTRFS first (currently mandatory)
        StorageProvider btrfs = new BtrfsStorageProvider(
            SystemDetector.detectDistribution(device), useSudo);
        try {
            if (btrfs.isInstalled(device)) {
                logger.info("Detected BTRFS storage provider");
                return btrfs;
            }
        } catch (Exception e) {
            logger.warning("Failed to detect BTRFS: " + e.getMessage());
        }

        // Future: Try other providers
        // LVM, ZFS, etc.

        // Default to BTRFS (will need installation)
        logger.info("No storage provider detected, defaulting to BTRFS");
        return btrfs;
    }

    /**
     * Get storage provider from configuration
     */
    public static StorageProvider fromConfiguration(String configPath, SshDevice device,
                                                    boolean useSudo) throws Exception {
        // Read storage.provider from configuration
        String command = String.format(
            "grep '^storage.provider=' %s | cut -d'=' -f2", configPath);
        String result = device.sendAndReceive(command).get();

        if (result == null || result.trim().isEmpty()) {
            // Default to BTRFS if not specified
            logger.info("No storage provider specified, defaulting to BTRFS");
            return createStorageProvider(StorageTechnology.BTRFS, device, useSudo);
        }

        StorageTechnology technology = StorageTechnology.fromCode(result.trim());
        return createStorageProvider(technology, device, useSudo);
    }
}