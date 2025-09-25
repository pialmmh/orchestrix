package com.telcobright.orchestrix.automation.devices.server.platform;

import com.telcobright.orchestrix.device.SshDevice;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Automation for enabling IP forwarding on various Linux distributions
 * Supports multiple Linux versions with distribution-specific implementations
 */
public class LinuxIpForwardEnablerAutomation {

    private static final Logger logger = Logger.getLogger(LinuxIpForwardEnablerAutomation.class.getName());

    public enum LinuxDistribution {
        DEBIAN_11("debian", "11"),
        DEBIAN_12("debian", "12"),
        UBUNTU_20_04("ubuntu", "20.04"),
        UBUNTU_22_04("ubuntu", "22.04"),
        UBUNTU_24_04("ubuntu", "24.04"),
        CENTOS_7("centos", "7"),
        CENTOS_8("centos", "8"),
        RHEL_8("rhel", "8"),
        RHEL_9("rhel", "9"),
        ROCKY_8("rocky", "8"),
        ROCKY_9("rocky", "9"),
        ALMA_8("almalinux", "8"),
        ALMA_9("almalinux", "9"),
        FEDORA_38("fedora", "38"),
        FEDORA_39("fedora", "39"),
        ARCH("arch", "rolling"),
        AUTO_DETECT("auto", "detect");

        private final String distro;
        private final String version;

        LinuxDistribution(String distro, String version) {
            this.distro = distro;
            this.version = version;
        }

        public String getDistro() {
            return distro;
        }

        public String getVersion() {
            return version;
        }
    }

    private final SshDevice sshDevice;
    private final boolean useSudo;
    private final boolean enableIpv4;
    private final boolean enableIpv6;
    private final boolean makePersistent;
    private final LinuxDistribution distribution;
    private final String sudoCmd;
    private LinuxDistribution detectedDistribution = null;

    /**
     * Builder pattern configuration
     */
    public static class Config {
        private boolean useSudo = true;
        private boolean enableIpv4 = true;
        private boolean enableIpv6 = false;
        private boolean makePersistent = true;
        private LinuxDistribution distribution = LinuxDistribution.AUTO_DETECT;

        public Config useSudo(boolean use) {
            this.useSudo = use;
            return this;
        }

        public Config enableIpv4(boolean enable) {
            this.enableIpv4 = enable;
            return this;
        }

        public Config enableIpv6(boolean enable) {
            this.enableIpv6 = enable;
            return this;
        }

        public Config makePersistent(boolean persistent) {
            this.makePersistent = persistent;
            return this;
        }

        public Config distribution(LinuxDistribution dist) {
            this.distribution = dist;
            return this;
        }

        public LinuxIpForwardEnablerAutomation build(SshDevice device) {
            return new LinuxIpForwardEnablerAutomation(device, this);
        }
    }

    private LinuxIpForwardEnablerAutomation(SshDevice device, Config config) {
        this.sshDevice = device;
        this.useSudo = config.useSudo;
        this.enableIpv4 = config.enableIpv4;
        this.enableIpv6 = config.enableIpv6;
        this.makePersistent = config.makePersistent;
        this.distribution = config.distribution;
        this.sudoCmd = useSudo ? "sudo " : "";
    }

    /**
     * Detect Linux distribution and version
     */
    private LinuxDistribution detectLinuxDistribution() throws Exception {
        if (detectedDistribution != null) {
            return detectedDistribution;
        }

        logger.info("Detecting Linux distribution...");

        // Try to read /etc/os-release
        String osRelease = sshDevice.sendAndReceive("cat /etc/os-release 2>/dev/null").get();

        if (osRelease != null && !osRelease.isEmpty()) {
            String distroId = "";
            String version = "";

            for (String line : osRelease.split("\n")) {
                if (line.startsWith("ID=")) {
                    distroId = line.substring(3).replace("\"", "").toLowerCase();
                } else if (line.startsWith("VERSION_ID=")) {
                    version = line.substring(11).replace("\"", "");
                }
            }

            logger.info("Detected: " + distroId + " " + version);

            // Match to known distributions
            if ("debian".equals(distroId)) {
                if (version.startsWith("12")) {
                    detectedDistribution = LinuxDistribution.DEBIAN_12;
                } else if (version.startsWith("11")) {
                    detectedDistribution = LinuxDistribution.DEBIAN_11;
                }
            } else if ("ubuntu".equals(distroId)) {
                if (version.startsWith("24.04")) {
                    detectedDistribution = LinuxDistribution.UBUNTU_24_04;
                } else if (version.startsWith("22.04")) {
                    detectedDistribution = LinuxDistribution.UBUNTU_22_04;
                } else if (version.startsWith("20.04")) {
                    detectedDistribution = LinuxDistribution.UBUNTU_20_04;
                }
            } else if ("centos".equals(distroId)) {
                if (version.startsWith("7")) {
                    detectedDistribution = LinuxDistribution.CENTOS_7;
                } else if (version.startsWith("8")) {
                    detectedDistribution = LinuxDistribution.CENTOS_8;
                }
            } else if ("rhel".equals(distroId)) {
                if (version.startsWith("8")) {
                    detectedDistribution = LinuxDistribution.RHEL_8;
                } else if (version.startsWith("9")) {
                    detectedDistribution = LinuxDistribution.RHEL_9;
                }
            } else if ("rocky".equals(distroId)) {
                if (version.startsWith("8")) {
                    detectedDistribution = LinuxDistribution.ROCKY_8;
                } else if (version.startsWith("9")) {
                    detectedDistribution = LinuxDistribution.ROCKY_9;
                }
            } else if ("almalinux".equals(distroId)) {
                if (version.startsWith("8")) {
                    detectedDistribution = LinuxDistribution.ALMA_8;
                } else if (version.startsWith("9")) {
                    detectedDistribution = LinuxDistribution.ALMA_9;
                }
            } else if ("fedora".equals(distroId)) {
                if (version.startsWith("38")) {
                    detectedDistribution = LinuxDistribution.FEDORA_38;
                } else if (version.startsWith("39")) {
                    detectedDistribution = LinuxDistribution.FEDORA_39;
                }
            } else if ("arch".equals(distroId)) {
                detectedDistribution = LinuxDistribution.ARCH;
            }
        }

        // Fallback to Debian 12 if can't detect
        if (detectedDistribution == null) {
            logger.warning("Could not detect distribution, defaulting to Debian 12");
            detectedDistribution = LinuxDistribution.DEBIAN_12;
        }

        return detectedDistribution;
    }

    /**
     * Enable IP forwarding based on distribution
     */
    public boolean enable() throws Exception {
        logger.info("Starting IP forwarding configuration");

        // Detect distribution if needed
        LinuxDistribution targetDist = distribution;
        if (targetDist == LinuxDistribution.AUTO_DETECT) {
            targetDist = detectLinuxDistribution();
        }

        logger.info("Using distribution: " + targetDist.getDistro() + " " + targetDist.getVersion());

        // Execute based on distribution
        switch (targetDist) {
            case DEBIAN_11:
            case DEBIAN_12:
                return executeDebian(targetDist.getVersion());

            case UBUNTU_20_04:
            case UBUNTU_22_04:
            case UBUNTU_24_04:
                return executeUbuntu(targetDist.getVersion());

            case CENTOS_7:
            case CENTOS_8:
                return executeCentOS(targetDist.getVersion());

            case RHEL_8:
            case RHEL_9:
            case ROCKY_8:
            case ROCKY_9:
            case ALMA_8:
            case ALMA_9:
                return executeRHEL(targetDist.getVersion());

            case FEDORA_38:
            case FEDORA_39:
                return executeFedora(targetDist.getVersion());

            case ARCH:
                return executeArch();

            default:
                logger.warning("Unsupported distribution, trying generic method");
                return executeGeneric();
        }
    }

    /**
     * Execute for Debian systems
     */
    private boolean executeDebian(String version) throws Exception {
        logger.info("Executing IP forwarding configuration for Debian " + version);

        boolean success = true;

        // Enable IPv4 forwarding if requested
        if (enableIpv4) {
            success = enableIpv4Forwarding() && success;
        }

        // Enable IPv6 forwarding if requested
        if (enableIpv6) {
            success = enableIpv6Forwarding() && success;
        }

        // Make persistent
        if (makePersistent && success) {
            success = makePersistentDebian() && success;
        }

        return success;
    }

    /**
     * Execute for Ubuntu systems
     */
    private boolean executeUbuntu(String version) throws Exception {
        logger.info("Executing IP forwarding configuration for Ubuntu " + version);

        // Ubuntu uses the same method as Debian
        return executeDebian(version);
    }

    /**
     * Execute for CentOS systems
     */
    private boolean executeCentOS(String version) throws Exception {
        logger.info("Executing IP forwarding configuration for CentOS " + version);

        boolean success = true;

        // Enable IPv4 forwarding if requested
        if (enableIpv4) {
            success = enableIpv4Forwarding() && success;
        }

        // Enable IPv6 forwarding if requested
        if (enableIpv6) {
            success = enableIpv6Forwarding() && success;
        }

        // Make persistent
        if (makePersistent && success) {
            if ("7".equals(version)) {
                success = makePersistentCentOS7() && success;
            } else {
                success = makePersistentRHEL8() && success;
            }
        }

        return success;
    }

    /**
     * Execute for RHEL/Rocky/AlmaLinux systems
     */
    private boolean executeRHEL(String version) throws Exception {
        logger.info("Executing IP forwarding configuration for RHEL-based " + version);

        boolean success = true;

        // Enable IPv4 forwarding if requested
        if (enableIpv4) {
            success = enableIpv4Forwarding() && success;
        }

        // Enable IPv6 forwarding if requested
        if (enableIpv6) {
            success = enableIpv6Forwarding() && success;
        }

        // Make persistent
        if (makePersistent && success) {
            success = makePersistentRHEL8() && success;
        }

        return success;
    }

    /**
     * Execute for Fedora systems
     */
    private boolean executeFedora(String version) throws Exception {
        logger.info("Executing IP forwarding configuration for Fedora " + version);

        // Fedora uses same method as modern RHEL
        return executeRHEL(version);
    }

    /**
     * Execute for Arch Linux
     */
    private boolean executeArch() throws Exception {
        logger.info("Executing IP forwarding configuration for Arch Linux");

        boolean success = true;

        // Enable IPv4 forwarding if requested
        if (enableIpv4) {
            success = enableIpv4Forwarding() && success;
        }

        // Enable IPv6 forwarding if requested
        if (enableIpv6) {
            success = enableIpv6Forwarding() && success;
        }

        // Make persistent
        if (makePersistent && success) {
            success = makePersistentArch() && success;
        }

        return success;
    }

    /**
     * Generic execution for unknown distributions
     */
    private boolean executeGeneric() throws Exception {
        logger.info("Executing generic IP forwarding configuration");

        boolean success = true;

        // Enable IPv4 forwarding if requested
        if (enableIpv4) {
            success = enableIpv4Forwarding() && success;
        }

        // Enable IPv6 forwarding if requested
        if (enableIpv6) {
            success = enableIpv6Forwarding() && success;
        }

        // Try generic persistent method
        if (makePersistent && success) {
            success = makePersistentGeneric() && success;
        }

        return success;
    }

    /**
     * Enable IPv4 forwarding (common for all distributions)
     */
    private boolean enableIpv4Forwarding() throws Exception {
        logger.info("Enabling IPv4 forwarding...");

        String result = sshDevice.sendAndReceive(sudoCmd + "sysctl -w net.ipv4.ip_forward=1").get();

        if (result != null && result.contains("net.ipv4.ip_forward = 1")) {
            logger.info("IPv4 forwarding enabled successfully");
            return true;
        } else {
            logger.severe("Failed to enable IPv4 forwarding");
            return false;
        }
    }

    /**
     * Enable IPv6 forwarding (common for all distributions)
     */
    private boolean enableIpv6Forwarding() throws Exception {
        logger.info("Enabling IPv6 forwarding...");

        String result = sshDevice.sendAndReceive(sudoCmd + "sysctl -w net.ipv6.conf.all.forwarding=1").get();

        if (result != null && result.contains("net.ipv6.conf.all.forwarding = 1")) {
            logger.info("IPv6 forwarding enabled successfully");
            sshDevice.sendAndReceive(sudoCmd + "sysctl -w net.ipv6.conf.default.forwarding=1").get();
            return true;
        } else {
            logger.severe("Failed to enable IPv6 forwarding");
            return false;
        }
    }

    /**
     * Make persistent on Debian/Ubuntu systems
     */
    private boolean makePersistentDebian() throws Exception {
        logger.info("Making IP forwarding persistent (Debian/Ubuntu method)...");

        // Create /etc/sysctl.d/99-ip-forward.conf
        StringBuilder config = new StringBuilder();
        if (enableIpv4) {
            config.append("net.ipv4.ip_forward=1\n");
        }
        if (enableIpv6) {
            config.append("net.ipv6.conf.all.forwarding=1\n");
            config.append("net.ipv6.conf.default.forwarding=1\n");
        }

        String writeCmd = String.format("echo '%s' | %stee /etc/sysctl.d/99-ip-forward.conf",
                                       config.toString(), sudoCmd);
        sshDevice.sendAndReceive(writeCmd).get();

        // Reload sysctl
        sshDevice.sendAndReceive(sudoCmd + "sysctl --system").get();

        logger.info("Persistent configuration applied");
        return true;
    }

    /**
     * Make persistent on CentOS 7 systems
     */
    private boolean makePersistentCentOS7() throws Exception {
        logger.info("Making IP forwarding persistent (CentOS 7 method)...");

        // Update /etc/sysctl.conf
        if (enableIpv4) {
            updateSysctlConf("net.ipv4.ip_forward", "1");
        }
        if (enableIpv6) {
            updateSysctlConf("net.ipv6.conf.all.forwarding", "1");
            updateSysctlConf("net.ipv6.conf.default.forwarding", "1");
        }

        // Reload sysctl
        sshDevice.sendAndReceive(sudoCmd + "sysctl -p").get();

        logger.info("Persistent configuration applied");
        return true;
    }

    /**
     * Make persistent on RHEL 8+ systems
     */
    private boolean makePersistentRHEL8() throws Exception {
        logger.info("Making IP forwarding persistent (RHEL 8+ method)...");

        // Use NetworkManager if available
        String nmCheck = sshDevice.sendAndReceive("which nmcli 2>/dev/null").get();
        if (nmCheck != null && !nmCheck.isEmpty()) {
            if (enableIpv4) {
                sshDevice.sendAndReceive(sudoCmd + "nmcli connection modify System ipv4.ip-forward yes 2>/dev/null || true").get();
            }
            if (enableIpv6) {
                sshDevice.sendAndReceive(sudoCmd + "nmcli connection modify System ipv6.ip-forward yes 2>/dev/null || true").get();
            }
        }

        // Also use sysctl.d method
        return makePersistentDebian();
    }

    /**
     * Make persistent on Arch Linux
     */
    private boolean makePersistentArch() throws Exception {
        logger.info("Making IP forwarding persistent (Arch Linux method)...");

        // Arch uses same sysctl.d method
        return makePersistentDebian();
    }

    /**
     * Generic persistent method
     */
    private boolean makePersistentGeneric() throws Exception {
        logger.info("Making IP forwarding persistent (generic method)...");

        // Try to use /etc/sysctl.d/ if it exists
        String checkDir = sshDevice.sendAndReceive("test -d /etc/sysctl.d && echo yes || echo no").get();
        if ("yes".equals(checkDir.trim())) {
            return makePersistentDebian();
        } else {
            // Fall back to updating /etc/sysctl.conf
            return makePersistentCentOS7();
        }
    }

    /**
     * Update sysctl.conf with a key-value pair
     */
    private void updateSysctlConf(String key, String value) throws Exception {
        // Check if key exists
        String checkCmd = String.format("grep '^%s' /etc/sysctl.conf 2>/dev/null", key);
        String existing = sshDevice.sendAndReceive(checkCmd).get();

        if (existing == null || existing.isEmpty()) {
            // Add new entry
            String addCmd = String.format("echo '%s=%s' | %stee -a /etc/sysctl.conf", key, value, sudoCmd);
            sshDevice.sendAndReceive(addCmd).get();
        } else {
            // Update existing entry
            String updateCmd = String.format("%ssed -i 's/^%s.*/%s=%s/' /etc/sysctl.conf",
                                            sudoCmd, key, key, value);
            sshDevice.sendAndReceive(updateCmd).get();
        }
    }

    /**
     * Verify IP forwarding configuration
     */
    public boolean verify() throws Exception {
        logger.info("Verifying IP forwarding configuration...");

        boolean verified = true;

        // Check IPv4 forwarding
        if (enableIpv4) {
            String ipv4Status = sshDevice.sendAndReceive(sudoCmd + "sysctl net.ipv4.ip_forward").get();
            if (ipv4Status != null && ipv4Status.contains("= 1")) {
                logger.info("IPv4 forwarding is enabled");
            } else {
                logger.warning("IPv4 forwarding is NOT enabled");
                verified = false;
            }
        }

        // Check IPv6 forwarding
        if (enableIpv6) {
            String ipv6Status = sshDevice.sendAndReceive(sudoCmd + "sysctl net.ipv6.conf.all.forwarding").get();
            if (ipv6Status != null && ipv6Status.contains("= 1")) {
                logger.info("IPv6 forwarding is enabled");
            } else {
                logger.warning("IPv6 forwarding is NOT enabled");
                verified = false;
            }
        }

        return verified;
    }

    /**
     * Get current status
     */
    public Map<String, String> getStatus() {
        Map<String, String> status = new HashMap<>();

        try {
            // Get detected distribution
            if (distribution == LinuxDistribution.AUTO_DETECT) {
                LinuxDistribution detected = detectLinuxDistribution();
                status.put("distribution", detected.getDistro());
                status.put("version", detected.getVersion());
            } else {
                status.put("distribution", distribution.getDistro());
                status.put("version", distribution.getVersion());
            }

            // Get IPv4 status
            String ipv4Status = sshDevice.sendAndReceive(sudoCmd + "sysctl net.ipv4.ip_forward 2>/dev/null").get();
            if (ipv4Status != null && ipv4Status.contains("= 1")) {
                status.put("ipv4.forwarding", "enabled");
            } else {
                status.put("ipv4.forwarding", "disabled");
            }

            // Get IPv6 status
            String ipv6Status = sshDevice.sendAndReceive(sudoCmd + "sysctl net.ipv6.conf.all.forwarding 2>/dev/null").get();
            if (ipv6Status != null && ipv6Status.contains("= 1")) {
                status.put("ipv6.forwarding", "enabled");
            } else {
                status.put("ipv6.forwarding", "disabled");
            }

        } catch (Exception e) {
            status.put("error", e.getMessage());
        }

        return status;
    }
}