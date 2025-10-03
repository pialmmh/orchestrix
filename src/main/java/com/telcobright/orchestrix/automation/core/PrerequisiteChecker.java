package com.telcobright.orchestrix.automation.core;

import com.telcobright.orchestrix.device.LocalSshDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Checks system prerequisites before container build operations.
 *
 * <p>Validates:
 * <ul>
 *   <li>BTRFS installation and kernel module</li>
 *   <li>LXC/LXD installation and configuration</li>
 *   <li>Network bridge configuration</li>
 *   <li>Storage availability and quotas</li>
 *   <li>Required system packages</li>
 * </ul>
 */
public class PrerequisiteChecker {

    private static final Logger logger = Logger.getLogger(PrerequisiteChecker.class.getName());

    private final LocalSshDevice device;
    private final boolean useSudo;
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public PrerequisiteChecker(LocalSshDevice device, boolean useSudo) {
        this.device = device;
        this.useSudo = useSudo;
    }

    /**
     * Check all prerequisites for container building.
     *
     * @return true if all checks pass, false if any critical checks fail
     */
    public boolean checkAll() throws Exception {
        logger.info("========================================");
        logger.info("Checking Prerequisites");
        logger.info("========================================");

        boolean allPassed = true;

        // Check BTRFS
        if (!checkBtrfs()) {
            allPassed = false;
        }

        // Check LXC/LXD
        if (!checkLxc()) {
            allPassed = false;
        }

        // Check network bridge
        if (!checkBridge()) {
            allPassed = false;
        }

        // Check storage
        if (!checkStorage()) {
            allPassed = false;
        }

        // Print results
        logger.info("========================================");
        if (allPassed && errors.isEmpty()) {
            logger.info("✓ All prerequisite checks PASSED");
        } else {
            logger.severe("✗ Prerequisite checks FAILED");
        }

        if (!warnings.isEmpty()) {
            logger.warning("Warnings:");
            warnings.forEach(w -> logger.warning("  - " + w));
        }

        if (!errors.isEmpty()) {
            logger.severe("Errors:");
            errors.forEach(e -> logger.severe("  - " + e));
        }

        logger.info("========================================");

        return allPassed && errors.isEmpty();
    }

    /**
     * Check BTRFS installation and kernel module.
     */
    public boolean checkBtrfs() throws Exception {
        logger.info("Checking BTRFS...");

        boolean passed = true;

        // Check if btrfs command exists
        String cmd = useSudo ? "sudo which btrfs" : "which btrfs";
        String result = device.executeCommand(cmd);

        if (result == null || result.trim().isEmpty() || result.contains("not found")) {
            errors.add("BTRFS tools not installed (btrfs-progs package required)");
            logger.severe("  ✗ BTRFS tools: NOT FOUND");
            passed = false;
        } else {
            logger.info("  ✓ BTRFS tools: " + result.trim());

            // Get version
            cmd = useSudo ? "sudo btrfs --version" : "btrfs --version";
            String version = device.executeCommand(cmd);
            if (version != null && !version.isEmpty()) {
                logger.info("    Version: " + version.trim());
            }
        }

        // Check if btrfs kernel module is loaded
        cmd = useSudo ? "sudo lsmod | grep btrfs" : "lsmod | grep btrfs";
        result = device.executeCommand(cmd);

        if (result == null || result.trim().isEmpty()) {
            errors.add("BTRFS kernel module not loaded (run: sudo modprobe btrfs)");
            logger.severe("  ✗ BTRFS module: NOT LOADED");
            passed = false;
        } else {
            logger.info("  ✓ BTRFS module: LOADED");
        }

        return passed;
    }

    /**
     * Check LXC/LXD installation.
     */
    public boolean checkLxc() throws Exception {
        logger.info("Checking LXC/LXD...");

        boolean passed = true;

        // Check lxc command
        String cmd = useSudo ? "sudo which lxc" : "which lxc";
        String result = device.executeCommand(cmd);

        if (result == null || result.trim().isEmpty() || result.contains("not found")) {
            errors.add("LXC not installed (lxd package required)");
            logger.severe("  ✗ LXC: NOT FOUND");
            passed = false;
        } else {
            logger.info("  ✓ LXC: " + result.trim());

            // Get version
            cmd = "lxc version";
            String version = device.executeCommand(cmd);
            if (version != null && !version.isEmpty()) {
                String[] lines = version.split("\n");
                for (String line : lines) {
                    if (line.contains("Client version") || line.contains("Server version")) {
                        logger.info("    " + line.trim());
                    }
                }
            }
        }

        // Check lxd service status
        cmd = useSudo ? "sudo systemctl is-active lxd" : "systemctl is-active lxd";
        result = device.executeCommand(cmd);

        if (result == null || !result.trim().equals("active")) {
            warnings.add("LXD service not active (run: sudo systemctl start lxd)");
            logger.warning("  ⚠ LXD service: NOT ACTIVE");
        } else {
            logger.info("  ✓ LXD service: ACTIVE");
        }

        return passed;
    }

    /**
     * Check network bridge configuration.
     */
    public boolean checkBridge() throws Exception {
        logger.info("Checking Network Bridge...");

        boolean passed = true;

        // Check for lxcbr0 or lxdbr0
        String cmd = useSudo ? "sudo ip addr show" : "ip addr show";
        String result = device.executeCommand(cmd);

        if (result == null || result.isEmpty()) {
            errors.add("Cannot query network interfaces");
            logger.severe("  ✗ Network check: FAILED");
            return false;
        }

        boolean hasLxcBridge = result.contains("lxcbr0");
        boolean hasLxdBridge = result.contains("lxdbr0");

        if (!hasLxcBridge && !hasLxdBridge) {
            warnings.add("No LXC bridge found (lxcbr0 or lxdbr0). Containers may not have network connectivity.");
            logger.warning("  ⚠ LXC bridge: NOT FOUND");
        } else {
            if (hasLxcBridge) {
                logger.info("  ✓ Bridge: lxcbr0 found");
            }
            if (hasLxdBridge) {
                logger.info("  ✓ Bridge: lxdbr0 found");
            }
        }

        return passed;
    }

    /**
     * Check storage availability.
     */
    public boolean checkStorage() throws Exception {
        logger.info("Checking Storage...");

        boolean passed = true;

        // Check available disk space in common storage locations
        String[] paths = {"/var/lib/lxd", "/var/lib/lxc", "/btrfs"};

        for (String path : paths) {
            String cmd = useSudo ?
                "sudo df -h " + path + " 2>/dev/null | tail -1" :
                "df -h " + path + " 2>/dev/null | tail -1";

            String result = device.executeCommand(cmd);

            if (result != null && !result.isEmpty() && !result.contains("No such file")) {
                String[] parts = result.trim().split("\\s+");
                if (parts.length >= 5) {
                    String size = parts[1];
                    String used = parts[2];
                    String avail = parts[3];
                    String usePercent = parts[4];

                    logger.info("  ✓ " + path + ": " + avail + " available (" + usePercent + " used)");

                    // Warn if less than 10GB available
                    if (avail.endsWith("G")) {
                        try {
                            double availGB = Double.parseDouble(avail.replace("G", ""));
                            if (availGB < 10.0) {
                                warnings.add(path + " has less than 10GB available: " + avail);
                            }
                        } catch (NumberFormatException e) {
                            // Ignore parse errors
                        }
                    }
                }
            }
        }

        return passed;
    }

    /**
     * Get error messages.
     */
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    /**
     * Get warning messages.
     */
    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }

    /**
     * Check if there are any errors.
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Check if there are any warnings.
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}
