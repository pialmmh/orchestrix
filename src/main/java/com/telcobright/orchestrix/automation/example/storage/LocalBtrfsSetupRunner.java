package com.telcobright.orchestrix.automation.example.storage;

import com.telcobright.orchestrix.automation.core.storage.btrfs.BtrfsStorageProvider;
import com.telcobright.orchestrix.automation.core.storage.base.StorageVolumeConfig;
import com.telcobright.orchestrix.device.LocalSshDevice;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Runner for setting up BTRFS storage on local machine
 * Uses Java automation classes instead of shell scripts
 */
public class LocalBtrfsSetupRunner {

    private static final Logger logger = Logger.getLogger(LocalBtrfsSetupRunner.class.getName());

    // Configuration
    private static final String BTRFS_DIR = "/home/telcobright/btrfs";
    private static final String BTRFS_IMAGE = BTRFS_DIR + "/btrfs.img";
    private static final String BTRFS_SIZE = "20G";
    private static final String CONFIG_DIR = "/etc/orchestrix";
    private static final String CONFIG_FILE = CONFIG_DIR + "/storage-locations.conf";

    private LocalSshDevice device;
    private BtrfsStorageProvider storageProvider;

    public LocalBtrfsSetupRunner() {
        // Initialize local SSH device
        this.device = new LocalSshDevice();
        this.storageProvider = new BtrfsStorageProvider(true); // use sudo
    }

    /**
     * Main setup method - coordinates all setup steps
     */
    public void setupBtrfs() throws Exception {
        logger.info("========================================");
        logger.info("BTRFS Setup for Orchestrix (Java)");
        logger.info("========================================");

        // Step 1: Install BTRFS tools
        installBtrfsTools();

        // Step 2: Create directory structure
        createDirectoryStructure();

        // Step 3: Check if already mounted
        if (!isBtrfsMounted()) {
            // Step 4: Create and mount BTRFS image
            createBtrfsImage();
            mountBtrfsImage();
        } else {
            logger.info("BTRFS already mounted at " + BTRFS_DIR);
        }

        // Step 5: Enable quota
        enableQuota();

        // Step 6: Create container directories
        createContainerDirectories();

        // Step 7: Set permissions
        setPermissions();

        // Step 8: Create storage configuration
        createStorageConfiguration();

        // Step 9: Verify setup
        verifySetup();

        logger.info("========================================");
        logger.info("BTRFS Setup Complete!");
        logger.info("========================================");
        logger.info("Storage location: " + BTRFS_DIR);
        logger.info("Config file: " + CONFIG_FILE);
        logger.info("");
        logger.info("Now you can build containers using Java automation:");
        logger.info("  java -cp orchestrix.jar com.telcobright.orchestrix.automation.storage.runners.GrafanaLokiBuildRunner");
        logger.info("========================================");
    }

    private void installBtrfsTools() throws Exception {
        logger.info("Installing BTRFS tools...");

        // Install btrfs-progs package (assuming Debian/Ubuntu)
        device.executeCommand("sudo apt-get update");
        device.executeCommand("sudo apt-get install -y btrfs-progs");
        logger.info("BTRFS tools installed successfully");
    }

    private void createDirectoryStructure() throws Exception {
        logger.info("Creating directory structure...");

        device.executeCommand("sudo mkdir -p " + BTRFS_DIR);
        device.executeCommand("sudo mkdir -p " + CONFIG_DIR);
    }

    private boolean isBtrfsMounted() throws Exception {
        String mountOutput = device.executeCommand("mount | grep " + BTRFS_DIR);
        return mountOutput != null && !mountOutput.isEmpty();
    }

    private void createBtrfsImage() throws Exception {
        logger.info("Creating BTRFS image (" + BTRFS_SIZE + ")...");

        // Check if image already exists
        String checkFile = device.executeCommand("ls -la " + BTRFS_IMAGE + " 2>/dev/null || echo 'not found'");
        if (!checkFile.contains("not found")) {
            logger.info("BTRFS image already exists");
            return;
        }

        // Create image file
        device.executeCommand("sudo truncate -s " + BTRFS_SIZE + " " + BTRFS_IMAGE);
        device.executeCommand("sudo mkfs.btrfs " + BTRFS_IMAGE);
    }

    private void mountBtrfsImage() throws Exception {
        logger.info("Mounting BTRFS image...");
        device.executeCommand("sudo mount -o loop,compress=lzo " + BTRFS_IMAGE + " " + BTRFS_DIR);
    }

    private void enableQuota() throws Exception {
        logger.info("Enabling BTRFS quota...");
        device.executeCommand("sudo btrfs quota enable " + BTRFS_DIR + " 2>/dev/null || true");
    }

    private void createContainerDirectories() throws Exception {
        logger.info("Creating container directories...");

        device.executeCommand("sudo mkdir -p " + BTRFS_DIR + "/containers");
        device.executeCommand("sudo mkdir -p " + BTRFS_DIR + "/snapshots");
        device.executeCommand("sudo mkdir -p " + BTRFS_DIR + "/backups");
    }

    private void setPermissions() throws Exception {
        logger.info("Setting permissions...");

        String currentUser = System.getProperty("user.name");
        device.executeCommand("sudo chown -R " + currentUser + ":" + currentUser + " " + BTRFS_DIR);
        device.executeCommand("sudo chmod 755 " + BTRFS_DIR);
    }

    private void createStorageConfiguration() throws Exception {
        logger.info("Creating storage configuration...");

        String config = String.join("\n",
            "# Orchestrix Storage Locations Configuration",
            "# Generated by Java automation",
            "",
            "# Local BTRFS storage for development",
            "btrfs_local_main.path=" + BTRFS_DIR,
            "btrfs_local_main.type=local",
            "btrfs_local_main.filesystem=btrfs",
            "btrfs_local_main.compression=true",
            "btrfs_local_main.auto_mount=true",
            "btrfs_local_main.mount_options=loop,compress=lzo",
            ""
        );

        // Write config file using sudo
        device.executeCommand("echo '" + config + "' | sudo tee " + CONFIG_FILE);
    }

    private void verifySetup() throws Exception {
        logger.info("Verifying setup...");

        // Check filesystem
        String fsShow = device.executeCommand("sudo btrfs filesystem show " + BTRFS_DIR);
        logger.info("Filesystem info:\n" + fsShow);

        // Check quota
        String quotaShow = device.executeCommand("sudo btrfs qgroup show " + BTRFS_DIR);
        logger.info("Quota info:\n" + quotaShow);

        // Check disk usage
        String dfShow = device.executeCommand("sudo btrfs filesystem df " + BTRFS_DIR);
        logger.info("Disk usage:\n" + dfShow);
    }

    /**
     * Main method for standalone execution
     */
    public static void main(String[] args) {
        try {
            LocalBtrfsSetupRunner runner = new LocalBtrfsSetupRunner();
            runner.setupBtrfs();
            System.exit(0);
        } catch (Exception e) {
            logger.severe("Setup failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}