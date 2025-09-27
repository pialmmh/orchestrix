package com.telcobright.orchestrix.automation.storage.btrfs;

import com.telcobright.orchestrix.automation.devices.server.linux.base.AbstractLinuxAutomation;
import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxDistribution;
import com.telcobright.orchestrix.device.SshDevice;
import java.util.Map;
import java.util.HashMap;

/**
 * Automation for installing and configuring BTRFS on various Linux distributions
 */
public class BtrfsInstallAutomation extends AbstractLinuxAutomation {

    public BtrfsInstallAutomation(LinuxDistribution distribution, boolean useSudo) {
        super(distribution, useSudo);
    }

    @Override
    public boolean execute(SshDevice device) throws Exception {
        logger.info("Installing BTRFS on " + distribution);

        if (!isPackageManagerAvailable(device)) {
            logger.severe("Package manager not available");
            return false;
        }

        // Update package lists
        if (!updatePackageLists(device)) {
            logger.warning("Failed to update package lists");
        }

        // Install BTRFS tools
        if (!installBtrfsTools(device)) {
            logger.severe("Failed to install BTRFS tools");
            return false;
        }

        // Load BTRFS kernel module
        if (!loadBtrfsModule(device)) {
            logger.severe("Failed to load BTRFS kernel module");
            return false;
        }

        // Configure BTRFS for persistence
        if (!configurePersistence(device)) {
            logger.warning("Failed to configure BTRFS persistence");
        }

        return verify(device);
    }

    @Override
    public boolean verify(SshDevice device) throws Exception {
        // Check if BTRFS tools are installed
        String result = executeCommand(device, "which btrfs");
        if (result == null || result.isEmpty()) {
            return false;
        }

        // Check if kernel module is loaded
        result = executeCommand(device, "lsmod | grep btrfs");
        return result != null && result.contains("btrfs");
    }

    /**
     * Install BTRFS tools based on distribution
     */
    private boolean installBtrfsTools(SshDevice device) throws Exception {
        String installCmd;

        switch (distribution) {
            case DEBIAN:
            case UBUNTU:
                installCmd = "apt-get install -y btrfs-progs btrfs-tools";
                break;

            case REDHAT:
            case CENTOS:
            case FEDORA:
                installCmd = "yum install -y btrfs-progs";
                break;

            case SUSE:
                installCmd = "zypper install -y btrfsprogs";
                break;

            case ARCH:
                installCmd = "pacman -S --noconfirm btrfs-progs";
                break;

            default:
                // Try common package names
                installCmd = "apt-get install -y btrfs-progs || yum install -y btrfs-progs";
        }

        String result = executeCommand(device, installCmd);
        return result != null && !result.contains("error");
    }

    /**
     * Load BTRFS kernel module
     */
    private boolean loadBtrfsModule(SshDevice device) throws Exception {
        String result = executeCommand(device, "modprobe btrfs");
        return result != null && !result.contains("error");
    }

    /**
     * Configure BTRFS to load on boot
     */
    private boolean configurePersistence(SshDevice device) throws Exception {
        // Add btrfs to modules-load.d for systemd systems
        String cmd = "echo 'btrfs' > /etc/modules-load.d/btrfs.conf";
        String result = executeCommand(device, cmd);

        // Also add to /etc/modules for older systems
        cmd = "grep -q '^btrfs$' /etc/modules || echo 'btrfs' >> /etc/modules";
        executeCommand(device, cmd);

        return result != null;
    }

    /**
     * Update package lists
     */
    private boolean updatePackageLists(SshDevice device) throws Exception {
        String updateCmd;

        switch (distribution) {
            case DEBIAN:
            case UBUNTU:
                updateCmd = "apt-get update";
                break;

            case REDHAT:
            case CENTOS:
            case FEDORA:
                updateCmd = "yum makecache";
                break;

            case SUSE:
                updateCmd = "zypper refresh";
                break;

            case ARCH:
                updateCmd = "pacman -Sy";
                break;

            default:
                updateCmd = "apt-get update || yum makecache";
        }

        String result = executeCommand(device, updateCmd);
        return result != null;
    }

    /**
     * Check if a BTRFS filesystem exists at the given path
     */
    public boolean isBtrfsFilesystem(SshDevice device, String path) throws Exception {
        String result = executeCommand(device, "df -T " + path + " | grep btrfs");
        return result != null && !result.isEmpty();
    }

    /**
     * Create a BTRFS filesystem on a device
     */
    public boolean createBtrfsFilesystem(SshDevice device, String devicePath, String label)
            throws Exception {
        String cmd = String.format("mkfs.btrfs -f -L %s %s", label, devicePath);
        String result = executeCommand(device, cmd);
        return result != null && !result.contains("error");
    }

    /**
     * Mount a BTRFS filesystem
     */
    public boolean mountBtrfsFilesystem(SshDevice device, String devicePath, String mountPoint)
            throws Exception {
        // Create mount point if it doesn't exist
        executeCommand(device, "mkdir -p " + mountPoint);

        // Mount the filesystem
        String cmd = String.format("mount -t btrfs %s %s", devicePath, mountPoint);
        String result = executeCommand(device, cmd);
        return result != null && !result.contains("error");
    }

    /**
     * Add BTRFS mount to fstab for persistence
     */
    public boolean addToFstab(SshDevice device, String devicePath, String mountPoint)
            throws Exception {
        String fstabEntry = String.format("%s %s btrfs defaults,compress=lzo,space_cache 0 0",
            devicePath, mountPoint);

        // Check if entry already exists
        String checkCmd = String.format("grep -q '%s' /etc/fstab", mountPoint);
        String result = executeCommand(device, checkCmd);

        if (result == null || result.isEmpty()) {
            // Add entry to fstab
            String addCmd = String.format("echo '%s' >> /etc/fstab", fstabEntry);
            result = executeCommand(device, addCmd);
            return result != null;
        }

        return true; // Entry already exists
    }

    @Override
    protected boolean isPackageInstalled(SshDevice device, String packageName) throws Exception {
        String result = null;

        switch (distribution) {
            case DEBIAN:
            case UBUNTU:
                result = executeCommand(device, "dpkg -l | grep -E '^ii.*" + packageName + "'");
                break;

            case REDHAT:
            case CENTOS:
            case FEDORA:
                result = executeCommand(device, "rpm -qa | grep " + packageName);
                break;

            default:
                result = executeCommand(device, "which " + packageName);
        }

        return result != null && !result.isEmpty();
    }

    @Override
    protected String getPackageManagerCommand() {
        switch (distribution) {
            case DEBIAN:
            case UBUNTU:
                return "apt-get";

            case REDHAT:
            case CENTOS:
            case FEDORA:
                return "yum";

            case SUSE:
                return "zypper";

            case ARCH:
                return "pacman";

            default:
                return "apt-get";
        }
    }

    @Override
    public Map<String, String> getStatus(SshDevice device) {
        Map<String, String> status = new HashMap<>();
        try {
            status.put("installed", String.valueOf(verify(device)));

            String result = executeCommand(device, "btrfs --version");
            if (result != null && !result.isEmpty()) {
                status.put("version", result.trim());
            }

            result = executeCommand(device, "lsmod | grep btrfs");
            status.put("module_loaded", String.valueOf(result != null && !result.isEmpty()));

        } catch (Exception e) {
            status.put("error", e.getMessage());
        }
        return status;
    }

    @Override
    public String getName() {
        return "BTRFS Installation";
    }

    @Override
    public String getDescription() {
        return "Install and configure BTRFS filesystem support";
    }
}