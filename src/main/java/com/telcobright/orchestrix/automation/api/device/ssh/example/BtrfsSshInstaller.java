package com.telcobright.orchestrix.automation.api.device.ssh.example;

import com.telcobright.orchestrix.automation.core.storage.btrfs.BtrfsInstallAutomation;
import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxDistribution;
import com.telcobright.orchestrix.device.SshDevice;

/**
 * Example runner for installing BTRFS on a remote Linux server via SSH.
 *
 * <p>This demonstrates:
 * <ul>
 *   <li>Connecting to remote server via SSH (password or key-based)</li>
 *   <li>Installing BTRFS tools using package manager</li>
 *   <li>Loading BTRFS kernel module</li>
 *   <li>Configuring BTRFS for persistence across reboots</li>
 *   <li>Verifying installation</li>
 * </ul>
 *
 * <p>Usage: Configure connection parameters and run main() method.
 *
 * @see com.telcobright.orchestrix.automation.core.storage.btrfs.BtrfsInstallAutomation
 */
public class BtrfsSshInstaller {

    public static void main(String[] args) {
        // SSH Connection parameters
        String hostname = "localhost";      // Remote server hostname/IP
        int port = 22;                      // SSH port
        String username = "mustafa";        // SSH username
        String password = "a";              // SSH password (or null for key auth)

        // BTRFS configuration
        LinuxDistribution distribution = LinuxDistribution.DEBIAN_12;
        boolean useSudo = true;             // User is in sudoers without password

        SshDevice device = null;

        try {
            System.out.println("========================================");
            System.out.println("BTRFS Installation via SSH");
            System.out.println("========================================");
            System.out.println("Target: " + hostname + ":" + port);
            System.out.println("User: " + username);
            System.out.println("Distribution: " + distribution);
            System.out.println("Use sudo: " + useSudo);
            System.out.println("========================================");
            System.out.println();

            // Create SSH device
            device = new SshDevice("btrfs-installer");

            // Connect to remote server
            System.out.println("Connecting to " + hostname + "...");
            Boolean connected = device.connect(hostname, port, username, password).get();

            if (!connected) {
                System.err.println("Failed to connect to " + hostname);
                System.exit(1);
            }

            System.out.println("✓ Connected successfully");
            System.out.println();

            // Create BTRFS automation
            BtrfsInstallAutomation btrfsAutomation = new BtrfsInstallAutomation(distribution, useSudo);

            // Execute installation
            System.out.println("Starting BTRFS installation...");
            System.out.println();

            boolean success = btrfsAutomation.execute(device);

            System.out.println();
            System.out.println("========================================");

            if (success) {
                System.out.println("✓ BTRFS Installation SUCCESSFUL");
                System.out.println("========================================");

                // Show status
                System.out.println();
                System.out.println("BTRFS Status:");
                btrfsAutomation.getStatus(device).forEach((key, value) ->
                    System.out.println("  " + key + ": " + value)
                );

            } else {
                System.out.println("✗ BTRFS Installation FAILED");
                System.out.println("========================================");
                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println();
            System.err.println("========================================");
            System.err.println("Error during BTRFS installation:");
            System.err.println("========================================");
            e.printStackTrace();
            System.exit(1);

        } finally {
            // Disconnect
            if (device != null && device.isConnected()) {
                try {
                    System.out.println();
                    System.out.println("Disconnecting from " + hostname + "...");
                    device.disconnect();
                    System.out.println("✓ Disconnected successfully");
                } catch (Exception e) {
                    System.err.println("Error disconnecting: " + e.getMessage());
                }
            }
        }

        System.out.println();
        System.out.println("========================================");
        System.out.println("BTRFS Installation Complete!");
        System.out.println("========================================");
    }
}
