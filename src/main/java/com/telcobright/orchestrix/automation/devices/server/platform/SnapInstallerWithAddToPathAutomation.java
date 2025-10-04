package com.telcobright.orchestrix.automation.devices.server.platform;

import com.telcobright.orchestrix.automation.core.device.SshDevice;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Automation for installing Snap package manager and adding it to PATH
 * Supports Debian, Ubuntu, and other systemd-based Linux distributions
 */
public class SnapInstallerWithAddToPathAutomation {

    private static final Logger logger = Logger.getLogger(SnapInstallerWithAddToPathAutomation.class.getName());

    private final SshDevice sshDevice;
    private final boolean useSudo;
    private final boolean addToPath;
    private final boolean updateSystemPath;
    private final String sudoCmd;

    /**
     * Builder pattern configuration
     */
    public static class Config {
        private boolean useSudo = true;
        private boolean addToPath = true;
        private boolean updateSystemPath = true;

        public Config useSudo(boolean use) {
            this.useSudo = use;
            return this;
        }

        public Config addToPath(boolean add) {
            this.addToPath = add;
            return this;
        }

        public Config updateSystemPath(boolean update) {
            this.updateSystemPath = update;
            return this;
        }

        public SnapInstallerWithAddToPathAutomation build(SshDevice device) {
            return new SnapInstallerWithAddToPathAutomation(device, this);
        }
    }

    private SnapInstallerWithAddToPathAutomation(SshDevice device, Config config) {
        this.sshDevice = device;
        this.useSudo = config.useSudo;
        this.addToPath = config.addToPath;
        this.updateSystemPath = config.updateSystemPath;
        this.sudoCmd = useSudo ? "sudo " : "";
    }

    /**
     * Check if snap is already installed
     */
    public boolean isSnapInstalled() {
        try {
            // Check if snap command exists
            String result = sshDevice.sendAndReceive("which snap 2>/dev/null").get();
            if (result != null && !result.isEmpty()) {
                logger.info("Snap found at: " + result);
                return true;
            }

            // Check if snap binary exists in /snap/bin
            result = sshDevice.sendAndReceive("test -f /snap/bin/snap && echo found").get();
            if ("found".equals(result.trim())) {
                logger.info("Snap found at /snap/bin/snap");
                return true;
            }

            return false;
        } catch (Exception e) {
            logger.warning("Error checking snap installation: " + e.getMessage());
            return false;
        }
    }

    /**
     * Detect the Linux distribution
     */
    private String detectDistribution() {
        try {
            String result = sshDevice.sendAndReceive("cat /etc/os-release | grep ^ID= | cut -d= -f2 | tr -d '\"'").get();
            return result.trim().toLowerCase();
        } catch (Exception e) {
            logger.warning("Could not detect distribution: " + e.getMessage());
            return "unknown";
        }
    }

    /**
     * Install snap package manager
     */
    public boolean install() {
        logger.info("Starting Snap installation");

        if (isSnapInstalled() && isInPath()) {
            logger.info("Snap is already installed and in PATH");
            return true;
        }

        String distro = detectDistribution();
        logger.info("Detected distribution: " + distro);

        boolean installed = false;

        try {
            // Update package list
            logger.info("Updating package list...");
            sshDevice.sendAndReceive(sudoCmd + "apt update").get();

            // Install snapd based on distribution
            switch (distro) {
                case "debian":
                case "ubuntu":
                    installed = installOnDebian();
                    break;
                case "centos":
                case "rhel":
                case "fedora":
                    installed = installOnRedHat();
                    break;
                case "arch":
                    installed = installOnArch();
                    break;
                default:
                    // Try Debian method as default
                    logger.info("Unknown distribution, trying Debian method...");
                    installed = installOnDebian();
            }

            if (installed && addToPath) {
                return addSnapToPath();
            }

            return installed;

        } catch (Exception e) {
            logger.severe("Installation failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Install snap on Debian/Ubuntu
     */
    private boolean installOnDebian() throws Exception {
        logger.info("Installing snapd on Debian/Ubuntu...");

        // Install snapd package
        String result = sshDevice.sendAndReceive(sudoCmd + "apt install -y snapd").get();
        if (result.toLowerCase().contains("error")) {
            logger.severe("Failed to install snapd package");
            return false;
        }

        // Enable and start snapd service
        logger.info("Enabling snapd service...");
        sshDevice.sendAndReceive(sudoCmd + "systemctl enable --now snapd").get();

        // Enable snapd socket
        sshDevice.sendAndReceive(sudoCmd + "systemctl enable --now snapd.socket").get();

        // Create symlink for classic snap support
        logger.info("Creating classic snap support symlink...");
        sshDevice.sendAndReceive(sudoCmd + "ln -sf /var/lib/snapd/snap /snap").get();

        // Wait for snapd to be ready
        Thread.sleep(3000);

        return true;
    }

    /**
     * Install snap on RedHat-based systems
     */
    private boolean installOnRedHat() throws Exception {
        logger.info("Installing snapd on RedHat/CentOS/Fedora...");

        // Enable EPEL repository for CentOS/RHEL
        String distro = detectDistribution();
        if ("centos".equals(distro) || "rhel".equals(distro)) {
            logger.info("Enabling EPEL repository...");
            sshDevice.sendAndReceive(sudoCmd + "yum install -y epel-release").get();
        }

        // Install snapd
        String result = sshDevice.sendAndReceive(sudoCmd + "yum install -y snapd").get();
        if (result.toLowerCase().contains("error")) {
            logger.severe("Failed to install snapd package");
            return false;
        }

        // Enable and start snapd
        logger.info("Enabling snapd service...");
        sshDevice.sendAndReceive(sudoCmd + "systemctl enable --now snapd.socket").get();

        // Create symlink
        sshDevice.sendAndReceive(sudoCmd + "ln -sf /var/lib/snapd/snap /snap").get();

        return true;
    }

    /**
     * Install snap on Arch Linux
     */
    private boolean installOnArch() throws Exception {
        logger.info("Installing snapd on Arch Linux...");

        // Install snapd from AUR
        String result = sshDevice.sendAndReceive(sudoCmd + "pacman -S --noconfirm snapd").get();
        if (result.toLowerCase().contains("error")) {
            logger.severe("Failed to install snapd package");
            return false;
        }

        // Enable systemd service
        sshDevice.sendAndReceive(sudoCmd + "systemctl enable --now snapd.socket").get();

        // Create symlink
        sshDevice.sendAndReceive(sudoCmd + "ln -sf /var/lib/snapd/snap /snap").get();

        return true;
    }

    /**
     * Check if snap is in PATH
     */
    private boolean isInPath() {
        try {
            String result = sshDevice.sendAndReceive("which snap 2>/dev/null").get();
            return result != null && !result.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Add snap binaries to PATH
     */
    private boolean addSnapToPath() {
        logger.info("Adding snap to PATH...");

        try {
            // Add to current user's PATH
            String homeDir = sshDevice.sendAndReceive("echo $HOME").get().trim();

            // Check which shell is being used
            String shell = sshDevice.sendAndReceive("echo $SHELL").get().trim();
            String rcFile = "";

            if (shell.contains("bash")) {
                rcFile = homeDir + "/.bashrc";
            } else if (shell.contains("zsh")) {
                rcFile = homeDir + "/.zshrc";
            } else {
                rcFile = homeDir + "/.profile";
            }

            logger.info("Adding snap to PATH in " + rcFile);

            // Check if already in rc file
            String checkResult = sshDevice.sendAndReceive("grep '/snap/bin' " + rcFile + " 2>/dev/null").get();
            if (checkResult == null || checkResult.isEmpty()) {
                // Add to rc file
                String pathLine = "export PATH=\"$PATH:/snap/bin\"";
                sshDevice.sendAndReceive("echo '" + pathLine + "' >> " + rcFile).get();
                logger.info("Added snap to user PATH in " + rcFile);
            } else {
                logger.info("Snap already in user PATH");
            }

            // Update system-wide PATH if requested
            if (updateSystemPath && useSudo) {
                addSnapToSystemPath();
            }

            // Export for current session
            sshDevice.sendAndReceive("export PATH=\"$PATH:/snap/bin\"").get();

            return true;

        } catch (Exception e) {
            logger.severe("Failed to add snap to PATH: " + e.getMessage());
            return false;
        }
    }

    /**
     * Add snap to system-wide PATH
     */
    private void addSnapToSystemPath() {
        try {
            logger.info("Adding snap to system-wide PATH...");

            // Create profile.d script
            String scriptContent = "#!/bin/sh\n" +
                                 "# Added by SnapInstallerWithAddToPathAutomation\n" +
                                 "export PATH=\"$PATH:/snap/bin\"\n";

            String command = "echo '" + scriptContent + "' | " + sudoCmd + "tee /etc/profile.d/snap-path.sh";
            sshDevice.sendAndReceive(command).get();

            // Make it executable
            sshDevice.sendAndReceive(sudoCmd + "chmod +x /etc/profile.d/snap-path.sh").get();

            logger.info("Added snap to system-wide PATH");

        } catch (Exception e) {
            logger.warning("Could not add snap to system PATH: " + e.getMessage());
        }
    }

    /**
     * Verify snap installation
     */
    public boolean verify() {
        logger.info("Verifying snap installation...");

        try {
            // Check snap version
            String version = sshDevice.sendAndReceive("/snap/bin/snap version 2>/dev/null || snap version").get();
            if (version != null && version.contains("snap")) {
                logger.info("Snap verified: " + version.split("\n")[0]);
                return true;
            }

            return false;

        } catch (Exception e) {
            logger.severe("Verification failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get installation status
     */
    public Map<String, String> getStatus() {
        Map<String, String> status = new HashMap<>();

        try {
            // Check if installed
            status.put("snap.installed", String.valueOf(isSnapInstalled()));

            // Check if in PATH
            status.put("snap.in.path", String.valueOf(isInPath()));

            // Get version if installed
            if (isSnapInstalled()) {
                String version = sshDevice.sendAndReceive("/snap/bin/snap version 2>/dev/null | head -1").get();
                status.put("snap.version", version.trim());
            }

            // Check service status
            String serviceStatus = sshDevice.sendAndReceive(sudoCmd + "systemctl is-active snapd 2>/dev/null").get();
            status.put("snapd.service", serviceStatus.trim());

            // Get distribution
            status.put("distribution", detectDistribution());

        } catch (Exception e) {
            status.put("error", e.getMessage());
        }

        return status;
    }
}