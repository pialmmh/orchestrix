package com.telcobright.orchestrix.automation.runner.storage;

import com.telcobright.orchestrix.automation.storage.base.*;
import com.telcobright.orchestrix.automation.storage.btrfs.*;
import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxDistribution;
import com.telcobright.orchestrix.device.SshDevice;
import com.telcobright.orchestrix.device.SshDeviceImpl;

import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.ConsoleHandler;
import java.util.logging.SimpleFormatter;

/**
 * Test runner for storage deployment automation
 * Provides interactive testing of BTRFS storage and container deployment
 */
public class StorageDeploymentTest {

    private static final Logger logger = Logger.getLogger(StorageDeploymentTest.class.getName());
    private static Scanner scanner = new Scanner(System.in);

    static {
        // Configure logging
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        consoleHandler.setFormatter(new SimpleFormatter());

        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.INFO);
        rootLogger.addHandler(consoleHandler);
    }

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("Storage Deployment Automation Test");
        System.out.println("========================================");

        try {
            // Get connection details
            SshDevice device = getConnectionDetails();

            // Connect to device
            System.out.println("\nConnecting to " + device.getHost() + "...");
            if (!device.connect()) {
                System.err.println("Failed to connect to device");
                return;
            }
            System.out.println("Connected successfully!");

            // Main test menu
            boolean exit = false;
            while (!exit) {
                System.out.println("\n========================================");
                System.out.println("Select Test Option:");
                System.out.println("1. Test BTRFS Installation");
                System.out.println("2. Test Storage Volume Creation");
                System.out.println("3. Test Container Deployment with Storage");
                System.out.println("4. Run Full Deployment from Config File");
                System.out.println("5. Test Storage Snapshots");
                System.out.println("6. View Storage Status");
                System.out.println("0. Exit");
                System.out.print("Choice: ");

                int choice = scanner.nextInt();
                scanner.nextLine(); // consume newline

                switch (choice) {
                    case 1:
                        testBtrfsInstallation(device);
                        break;
                    case 2:
                        testStorageVolumeCreation(device);
                        break;
                    case 3:
                        testContainerDeployment(device);
                        break;
                    case 4:
                        testFullDeployment();
                        break;
                    case 5:
                        testSnapshots(device);
                        break;
                    case 6:
                        viewStorageStatus(device);
                        break;
                    case 0:
                        exit = true;
                        break;
                    default:
                        System.out.println("Invalid choice");
                }
            }

            // Disconnect
            device.disconnect();
            System.out.println("\nDisconnected. Goodbye!");

        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get SSH connection details from user
     */
    private static SshDevice getConnectionDetails() {
        System.out.print("SSH Host [localhost]: ");
        String host = scanner.nextLine();
        if (host.isEmpty()) host = "localhost";

        System.out.print("SSH Port [22]: ");
        String portStr = scanner.nextLine();
        int port = portStr.isEmpty() ? 22 : Integer.parseInt(portStr);

        System.out.print("SSH Username: ");
        String username = scanner.nextLine();

        System.out.print("Use password authentication? (y/n) [y]: ");
        String usePassword = scanner.nextLine();

        SshDeviceImpl device = new SshDeviceImpl();
        device.setHost(host);
        device.setPort(port);
        device.setUsername(username);

        if (usePassword.isEmpty() || usePassword.toLowerCase().startsWith("y")) {
            System.out.print("SSH Password: ");
            String password = scanner.nextLine();
            device.setPassword(password);
        } else {
            System.out.print("SSH Key Path [~/.ssh/id_rsa]: ");
            String keyPath = scanner.nextLine();
            if (keyPath.isEmpty()) keyPath = System.getProperty("user.home") + "/.ssh/id_rsa";
            device.setPrivateKeyPath(keyPath);
        }

        return device;
    }

    /**
     * Test BTRFS installation
     */
    private static void testBtrfsInstallation(SshDevice device) throws Exception {
        System.out.println("\n=== Testing BTRFS Installation ===");

        System.out.print("Use sudo? (y/n) [y]: ");
        String useSudoStr = scanner.nextLine();
        boolean useSudo = useSudoStr.isEmpty() || useSudoStr.toLowerCase().startsWith("y");

        BtrfsInstallAutomation installer = new BtrfsInstallAutomation(
            LinuxDistribution.DEBIAN, useSudo);

        System.out.println("Checking if BTRFS is installed...");
        if (installer.verify(device)) {
            System.out.println("BTRFS is already installed!");
        } else {
            System.out.println("BTRFS not found. Installing...");
            if (installer.execute(device)) {
                System.out.println("BTRFS installed successfully!");
            } else {
                System.err.println("Failed to install BTRFS");
            }
        }
    }

    /**
     * Test storage volume creation
     */
    private static void testStorageVolumeCreation(SshDevice device) throws Exception {
        System.out.println("\n=== Testing Storage Volume Creation ===");

        System.out.print("Storage Location ID [btrfs_ssd_main]: ");
        String locationId = scanner.nextLine();
        if (locationId.isEmpty()) locationId = "btrfs_ssd_main";

        System.out.print("Container Root Name: ");
        String containerRoot = scanner.nextLine();

        System.out.print("Quota Size (e.g., 10G): ");
        String quotaSize = scanner.nextLine();

        System.out.print("Enable Compression? (y/n) [n]: ");
        String compression = scanner.nextLine();

        System.out.print("Use sudo? (y/n) [y]: ");
        String useSudoStr = scanner.nextLine();
        boolean useSudo = useSudoStr.isEmpty() || useSudoStr.toLowerCase().startsWith("y");

        // Create storage configuration
        StorageVolumeConfig config = new StorageVolumeConfig.Builder()
            .locationId(locationId)
            .containerRoot(containerRoot)
            .quotaHumanSize(quotaSize)
            .compression(compression.toLowerCase().startsWith("y"))
            .build();

        // Create storage provider
        BtrfsStorageProvider provider = new BtrfsStorageProvider(useSudo);

        // Ensure BTRFS is installed
        if (!provider.isInstalled(device)) {
            System.out.println("Installing BTRFS first...");
            provider.install(device);
        }

        // Create volume
        System.out.println("Creating storage volume...");
        StorageVolume volume = provider.createVolume(device, config);

        System.out.println("Volume created successfully!");
        System.out.println("  Path: " + volume.getPath());
        System.out.println("  Quota: " + volume.getQuotaHumanReadable());
        System.out.println("  Compression: " + volume.isCompression());
    }

    /**
     * Test container deployment with storage
     */
    private static void testContainerDeployment(SshDevice device) throws Exception {
        System.out.println("\n=== Testing Container Deployment with Storage ===");

        System.out.print("Container Name: ");
        String containerName = scanner.nextLine();

        System.out.print("Storage Location ID [btrfs_ssd_main]: ");
        String locationId = scanner.nextLine();
        if (locationId.isEmpty()) locationId = "btrfs_ssd_main";

        System.out.print("Storage Quota [10G]: ");
        String quota = scanner.nextLine();
        if (quota.isEmpty()) quota = "10G";

        System.out.print("Use sudo? (y/n) [y]: ");
        String useSudoStr = scanner.nextLine();
        boolean useSudo = useSudoStr.isEmpty() || useSudoStr.toLowerCase().startsWith("y");

        // Create storage volume config
        StorageVolumeConfig volumeConfig = new StorageVolumeConfig.Builder()
            .locationId(locationId)
            .containerRoot(containerName)
            .quotaHumanSize(quota)
            .compression(true)
            .build();

        // Create and execute mount automation
        LxcContainerBtrfsMountAutomation mountAutomation =
            new LxcContainerBtrfsMountAutomation(
                LinuxDistribution.DEBIAN, volumeConfig, containerName, useSudo);

        System.out.println("Setting up BTRFS mount for container...");
        if (mountAutomation.execute(device)) {
            System.out.println("Storage mount configured successfully!");
        } else {
            System.err.println("Failed to configure storage mount");
        }
    }

    /**
     * Test full deployment from config file
     */
    private static void testFullDeployment() throws Exception {
        System.out.println("\n=== Testing Full Deployment from Config ===");

        System.out.print("Config file path: ");
        String configPath = scanner.nextLine();

        BtrfsContainerDeploymentRunner runner = new BtrfsContainerDeploymentRunner(configPath);

        System.out.println("Starting deployment...");
        if (runner.execute()) {
            System.out.println("Deployment completed successfully!");
        } else {
            System.err.println("Deployment failed!");
        }
    }

    /**
     * Test snapshot operations
     */
    private static void testSnapshots(SshDevice device) throws Exception {
        System.out.println("\n=== Testing Storage Snapshots ===");

        System.out.print("Volume Path: ");
        String volumePath = scanner.nextLine();

        System.out.print("Use sudo? (y/n) [y]: ");
        String useSudoStr = scanner.nextLine();
        boolean useSudo = useSudoStr.isEmpty() || useSudoStr.toLowerCase().startsWith("y");

        BtrfsStorageProvider provider = new BtrfsStorageProvider(useSudo);

        System.out.println("\n1. Create Snapshot");
        System.out.println("2. List Snapshots");
        System.out.println("3. Delete Snapshot");
        System.out.print("Choice: ");

        int choice = scanner.nextInt();
        scanner.nextLine();

        switch (choice) {
            case 1:
                System.out.print("Snapshot name: ");
                String snapName = scanner.nextLine();
                if (provider.createSnapshot(device, volumePath, snapName)) {
                    System.out.println("Snapshot created successfully!");
                } else {
                    System.err.println("Failed to create snapshot");
                }
                break;

            case 2:
                System.out.println("Snapshots:");
                for (String snap : provider.listSnapshots(device, volumePath)) {
                    System.out.println("  - " + snap);
                }
                break;

            case 3:
                System.out.print("Snapshot name to delete: ");
                String delSnapName = scanner.nextLine();
                if (provider.deleteSnapshot(device, volumePath, delSnapName)) {
                    System.out.println("Snapshot deleted successfully!");
                } else {
                    System.err.println("Failed to delete snapshot");
                }
                break;
        }
    }

    /**
     * View storage status
     */
    private static void viewStorageStatus(SshDevice device) throws Exception {
        System.out.println("\n=== Storage Status ===");

        System.out.print("Volume Path: ");
        String volumePath = scanner.nextLine();

        System.out.print("Use sudo? (y/n) [y]: ");
        String useSudoStr = scanner.nextLine();
        boolean useSudo = useSudoStr.isEmpty() || useSudoStr.toLowerCase().startsWith("y");

        BtrfsStorageProvider provider = new BtrfsStorageProvider(useSudo);

        System.out.println("\nVolume Statistics:");
        for (Map.Entry<String, Object> entry : provider.getVolumeStats(device, volumePath).entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }
    }
}