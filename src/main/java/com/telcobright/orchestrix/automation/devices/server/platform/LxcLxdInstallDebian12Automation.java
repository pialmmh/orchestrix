package com.telcobright.orchestrix.automation.devices.server.platform;

import com.telcobright.orchestrix.automation.core.device.SshDevice;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Automation class for installing LXC/LXD on Debian 12 systems
 * Handles package installation, initialization, and basic configuration
 */
public class LxcLxdInstallDebian12Automation {

    private static final Logger logger = Logger.getLogger(LxcLxdInstallDebian12Automation.class.getName());

    private final SshDevice sshDevice;
    private final boolean useSudo;
    private final boolean installLxc;
    private final boolean installLxd;
    private final boolean installSnapd;
    private final String lxdInitPreseed;

    /**
     * Configuration builder for LXC/LXD installation
     */
    public static class Config {
        private boolean useSudo = true;
        private boolean installLxc = true;
        private boolean installLxd = true;
        private boolean installSnapd = false;  // Use snap for LXD
        private String lxdInitPreseed = null;  // Custom preseed for lxd init

        public Config useSudo(boolean useSudo) {
            this.useSudo = useSudo;
            return this;
        }

        public Config installLxc(boolean install) {
            this.installLxc = install;
            return this;
        }

        public Config installLxd(boolean install) {
            this.installLxd = install;
            return this;
        }

        public Config installSnapd(boolean install) {
            this.installSnapd = install;
            return this;
        }

        public Config lxdInitPreseed(String preseed) {
            this.lxdInitPreseed = preseed;
            return this;
        }

        public LxcLxdInstallDebian12Automation build(SshDevice device) {
            return new LxcLxdInstallDebian12Automation(device, this);
        }
    }

    private LxcLxdInstallDebian12Automation(SshDevice device, Config config) {
        this.sshDevice = device;
        this.useSudo = config.useSudo;
        this.installLxc = config.installLxc;
        this.installLxd = config.installLxd;
        this.installSnapd = config.installSnapd;
        this.lxdInitPreseed = config.lxdInitPreseed;
    }

    /**
     * Execute the installation
     * @return true if installation successful
     */
    public boolean install() {
        try {
            logger.info("Starting LXC/LXD installation on Debian 12");

            // Update package list
            if (!updatePackages()) {
                logger.severe("Failed to update package list");
                return false;
            }

            // Install dependencies
            if (!installDependencies()) {
                logger.severe("Failed to install dependencies");
                return false;
            }

            // Install LXC if requested
            if (installLxc) {
                if (!installLxcPackages()) {
                    logger.severe("Failed to install LXC");
                    return false;
                }
            }

            // Install LXD
            if (installLxd) {
                if (installSnapd) {
                    if (!installLxdViaSnap()) {
                        logger.severe("Failed to install LXD via snap");
                        return false;
                    }
                } else {
                    if (!installLxdViaApt()) {
                        logger.severe("Failed to install LXD via apt");
                        return false;
                    }
                }

                // Initialize LXD
                if (!initializeLxd()) {
                    logger.severe("Failed to initialize LXD");
                    return false;
                }
            }

            // Configure user permissions
            if (!configureUserPermissions()) {
                logger.warning("Failed to configure user permissions");
            }

            // Enable and start services
            if (!enableServices()) {
                logger.warning("Failed to enable services");
            }

            logger.info("LXC/LXD installation completed successfully");
            return true;

        } catch (Exception e) {
            logger.severe("Failed to install LXC/LXD: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update package list
     */
    private boolean updatePackages() throws Exception {
        logger.info("Updating package list...");

        String cmd = getSudoPrefix() + "apt update";
        String result = sshDevice.sendAndReceive(cmd).get();

        if (result.toLowerCase().contains("error")) {
            logger.severe("Package update failed: " + result);
            return false;
        }

        return true;
    }

    /**
     * Install basic dependencies
     */
    private boolean installDependencies() throws Exception {
        logger.info("Installing dependencies...");

        String dependencies = "curl wget ca-certificates bridge-utils";
        String cmd = String.format("%sapt install -y %s", getSudoPrefix(), dependencies);
        String result = sshDevice.sendAndReceive(cmd).get();

        return !result.toLowerCase().contains("error");
    }

    /**
     * Install LXC packages
     */
    private boolean installLxcPackages() throws Exception {
        logger.info("Installing LXC packages...");

        String packages = "lxc lxc-templates libvirt0 libpam-cgfs bridge-utils uidmap";
        String cmd = String.format("%sapt install -y %s", getSudoPrefix(), packages);
        String result = sshDevice.sendAndReceive(cmd).get();

        if (result.toLowerCase().contains("error")) {
            logger.severe("LXC installation failed: " + result);
            return false;
        }

        // Configure LXC network
        configureLxcNetwork();

        return true;
    }

    /**
     * Install LXD via APT
     */
    private boolean installLxdViaApt() throws Exception {
        logger.info("Installing LXD via APT...");

        // Add backports repository for newer LXD version
        String addBackports = String.format(
            "%secho 'deb http://deb.debian.org/debian bookworm-backports main' | %stee /etc/apt/sources.list.d/backports.list",
            getSudoPrefix(), getSudoPrefix()
        );
        sshDevice.sendAndReceive(addBackports).get();

        // Update package list again
        sshDevice.sendAndReceive(getSudoPrefix() + "apt update").get();

        // Install LXD from backports
        String cmd = String.format("%sapt install -y -t bookworm-backports lxd lxd-client", getSudoPrefix());
        String result = sshDevice.sendAndReceive(cmd).get();

        if (result.toLowerCase().contains("error")) {
            logger.severe("LXD installation failed: " + result);
            return false;
        }

        return true;
    }

    /**
     * Install LXD via Snap
     */
    private boolean installLxdViaSnap() throws Exception {
        logger.info("Installing snapd...");

        // Install snapd
        String cmd = String.format("%sapt install -y snapd", getSudoPrefix());
        String result = sshDevice.sendAndReceive(cmd).get();

        if (result.toLowerCase().contains("error")) {
            logger.severe("Snapd installation failed: " + result);
            return false;
        }

        // Enable and start snapd
        sshDevice.sendAndReceive(getSudoPrefix() + "systemctl enable --now snapd.socket").get();
        sshDevice.sendAndReceive(getSudoPrefix() + "systemctl enable --now snapd.service").get();

        // Wait for snapd to be ready
        Thread.sleep(5000);

        // Install core snap
        logger.info("Installing snap core...");
        sshDevice.sendAndReceive(getSudoPrefix() + "snap install core").get();

        // Install LXD snap
        logger.info("Installing LXD via snap...");
        result = sshDevice.sendAndReceive(getSudoPrefix() + "snap install lxd").get();

        if (result.toLowerCase().contains("error")) {
            logger.severe("LXD snap installation failed: " + result);
            return false;
        }

        // Add /snap/bin to PATH if needed
        sshDevice.sendAndReceive("echo 'export PATH=$PATH:/snap/bin' >> ~/.bashrc").get();

        return true;
    }

    /**
     * Configure LXC network
     */
    private void configureLxcNetwork() throws Exception {
        logger.info("Configuring LXC network...");

        // Create default LXC network config
        String netConfig = "USE_LXC_BRIDGE=\"true\"\n" +
                          "LXC_BRIDGE=\"lxcbr0\"\n" +
                          "LXC_ADDR=\"10.0.3.1\"\n" +
                          "LXC_NETMASK=\"255.255.255.0\"\n" +
                          "LXC_NETWORK=\"10.0.3.0/24\"\n" +
                          "LXC_DHCP_RANGE=\"10.0.3.2,10.0.3.254\"\n" +
                          "LXC_DHCP_MAX=\"253\"\n" +
                          "LXC_DHCP_CONFILE=\"\"\n" +
                          "LXC_DOMAIN=\"\"";

        String cmd = String.format("echo '%s' | %stee /etc/default/lxc-net", netConfig, getSudoPrefix());
        sshDevice.sendAndReceive(cmd).get();

        // Restart LXC networking
        sshDevice.sendAndReceive(getSudoPrefix() + "systemctl restart lxc-net").get();
    }

    /**
     * Initialize LXD
     */
    private boolean initializeLxd() throws Exception {
        logger.info("Initializing LXD...");

        String preseed;
        if (lxdInitPreseed != null) {
            preseed = lxdInitPreseed;
        } else {
            // Default preseed configuration
            preseed = "config:\n" +
                     "  images.auto_update_interval: \"0\"\n" +
                     "networks:\n" +
                     "- config:\n" +
                     "    ipv4.address: 10.10.199.1/24\n" +
                     "    ipv4.nat: \"true\"\n" +
                     "    ipv6.address: none\n" +
                     "  description: \"\"\n" +
                     "  name: lxdbr0\n" +
                     "  type: bridge\n" +
                     "storage_pools:\n" +
                     "- config:\n" +
                     "    size: 30GB\n" +
                     "  description: \"\"\n" +
                     "  name: default\n" +
                     "  driver: dir\n" +
                     "profiles:\n" +
                     "- config: {}\n" +
                     "  description: \"\"\n" +
                     "  devices:\n" +
                     "    eth0:\n" +
                     "      name: eth0\n" +
                     "      network: lxdbr0\n" +
                     "      type: nic\n" +
                     "    root:\n" +
                     "      path: /\n" +
                     "      pool: default\n" +
                     "      type: disk\n" +
                     "  name: default\n" +
                     "cluster: null";
        }

        // Check which lxd command to use
        String lxdCmd = "lxd";
        String checkSnap = sshDevice.sendAndReceive("which /snap/bin/lxd 2>/dev/null").get();
        if (checkSnap != null && !checkSnap.isEmpty()) {
            lxdCmd = "/snap/bin/lxd";
        }

        // Run lxd init with preseed
        String cmd = String.format("cat << 'EOF' | %s%s init --preseed\n%s\nEOF",
                                  getSudoPrefix(), lxdCmd, preseed);
        String result = sshDevice.sendAndReceive(cmd).get();

        if (result.toLowerCase().contains("error") && !result.contains("already exists")) {
            logger.severe("LXD initialization failed: " + result);
            return false;
        }

        return true;
    }

    /**
     * Configure user permissions
     */
    private boolean configureUserPermissions() throws Exception {
        logger.info("Configuring user permissions...");

        // Get current username
        String username = sshDevice.sendAndReceive("whoami").get().trim();

        // Add user to lxd group
        String cmd = String.format("%susermod -aG lxd %s", getSudoPrefix(), username);
        sshDevice.sendAndReceive(cmd).get();

        // Apply group changes
        sshDevice.sendAndReceive("newgrp lxd").get();

        return true;
    }

    /**
     * Enable and start services
     */
    private boolean enableServices() throws Exception {
        logger.info("Enabling and starting services...");

        // Enable and start LXC service
        if (installLxc) {
            sshDevice.sendAndReceive(getSudoPrefix() + "systemctl enable lxc-net").get();
            sshDevice.sendAndReceive(getSudoPrefix() + "systemctl start lxc-net").get();
        }

        // Enable and start LXD service (if installed via apt)
        if (installLxd && !installSnapd) {
            sshDevice.sendAndReceive(getSudoPrefix() + "systemctl enable lxd").get();
            sshDevice.sendAndReceive(getSudoPrefix() + "systemctl start lxd").get();
        }

        return true;
    }

    /**
     * Verify installation
     */
    public boolean verify() {
        try {
            logger.info("Verifying LXC/LXD installation...");

            boolean success = true;

            // Check LXC
            if (installLxc) {
                String lxcVersion = sshDevice.sendAndReceive("lxc-ls --version 2>/dev/null").get();
                if (lxcVersion != null && !lxcVersion.isEmpty()) {
                    logger.info("LXC version: " + lxcVersion.trim());
                } else {
                    logger.warning("LXC not found or not in PATH");
                    success = false;
                }
            }

            // Check LXD
            if (installLxd) {
                String lxdCmd = installSnapd ? "/snap/bin/lxc" : "lxc";
                String lxdVersion = sshDevice.sendAndReceive(lxdCmd + " version 2>/dev/null").get();
                if (lxdVersion != null && !lxdVersion.isEmpty()) {
                    logger.info("LXD version: " + lxdVersion.trim());

                    // Check if can list containers
                    String containers = sshDevice.sendAndReceive(lxdCmd + " list 2>/dev/null").get();
                    if (containers != null && !containers.toLowerCase().contains("error")) {
                        logger.info("LXD is functional - can list containers");
                    }
                } else {
                    logger.warning("LXD not found or not in PATH");
                    success = false;
                }

                // Check lxdbr0 bridge
                String bridge = sshDevice.sendAndReceive("ip addr show lxdbr0 2>/dev/null").get();
                if (bridge != null && !bridge.isEmpty()) {
                    logger.info("lxdbr0 bridge is configured");
                } else {
                    logger.warning("lxdbr0 bridge not found");
                }
            }

            return success;

        } catch (Exception e) {
            logger.severe("Failed to verify installation: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get installation status
     */
    public Map<String, String> getStatus() {
        Map<String, String> status = new HashMap<>();

        try {
            // Check LXC
            String lxcVersion = sshDevice.sendAndReceive("lxc-ls --version 2>/dev/null").get();
            status.put("lxc.installed", lxcVersion != null && !lxcVersion.isEmpty() ? "true" : "false");
            status.put("lxc.version", lxcVersion != null ? lxcVersion.trim() : "not installed");

            // Check LXD
            String lxdVersion = sshDevice.sendAndReceive("lxc version 2>/dev/null || /snap/bin/lxc version 2>/dev/null").get();
            status.put("lxd.installed", lxdVersion != null && !lxdVersion.isEmpty() ? "true" : "false");
            status.put("lxd.version", lxdVersion != null ? lxdVersion.trim() : "not installed");

            // Check bridges
            String bridges = sshDevice.sendAndReceive("ip link show type bridge 2>/dev/null | grep -E '^[0-9]+:' | cut -d: -f2").get();
            status.put("bridges", bridges != null ? bridges.trim() : "none");

            // Check if user is in lxd group
            String groups = sshDevice.sendAndReceive("groups").get();
            status.put("user.in.lxd.group", groups != null && groups.contains("lxd") ? "true" : "false");

        } catch (Exception e) {
            logger.severe("Failed to get status: " + e.getMessage());
        }

        return status;
    }

    /**
     * Get sudo prefix based on configuration
     */
    private String getSudoPrefix() {
        return useSudo ? "sudo " : "";
    }
}