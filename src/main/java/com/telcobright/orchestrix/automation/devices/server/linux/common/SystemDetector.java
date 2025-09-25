package com.telcobright.orchestrix.automation.devices.server.linux.common;

import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxDistribution;
import com.telcobright.orchestrix.device.SshDevice;
import java.util.logging.Logger;

/**
 * Utility class for detecting Linux distribution and version
 */
public class SystemDetector {

    private static final Logger logger = Logger.getLogger(SystemDetector.class.getName());

    /**
     * Detect the Linux distribution from the SSH device
     */
    public static LinuxDistribution detectDistribution(SshDevice device) {
        try {
            // Try to read /etc/os-release
            String osRelease = device.sendAndReceive("cat /etc/os-release 2>/dev/null").get();

            if (osRelease != null && !osRelease.isEmpty()) {
                return parseOsRelease(osRelease);
            }

            // Fallback to other detection methods
            return detectFromOtherMethods(device);

        } catch (Exception e) {
            logger.severe("Failed to detect distribution: " + e.getMessage());
            return LinuxDistribution.UNKNOWN;
        }
    }

    private static LinuxDistribution parseOsRelease(String osRelease) {
        String id = null;
        String versionId = null;
        String versionCodename = null;

        for (String line : osRelease.split("\n")) {
            if (line.startsWith("ID=")) {
                id = line.substring(3).replace("\"", "").toLowerCase();
            } else if (line.startsWith("VERSION_ID=")) {
                versionId = line.substring(11).replace("\"", "");
            } else if (line.startsWith("VERSION_CODENAME=")) {
                versionCodename = line.substring(17).replace("\"", "");
            }
        }

        if (id == null || versionId == null) {
            return LinuxDistribution.UNKNOWN;
        }

        // Match to known distributions
        switch (id) {
            case "debian":
                if ("12".equals(versionId)) return LinuxDistribution.DEBIAN_12;
                if ("11".equals(versionId)) return LinuxDistribution.DEBIAN_11;
                return new LinuxDistribution("debian", versionId, versionCodename, LinuxDistribution.Family.DEBIAN);

            case "ubuntu":
                if ("24.04".equals(versionId)) return LinuxDistribution.UBUNTU_2404;
                if ("22.04".equals(versionId)) return LinuxDistribution.UBUNTU_2204;
                if ("20.04".equals(versionId)) return LinuxDistribution.UBUNTU_2004;
                return new LinuxDistribution("ubuntu", versionId, versionCodename, LinuxDistribution.Family.DEBIAN);

            case "centos":
                if ("7".equals(versionId)) return LinuxDistribution.CENTOS_7;
                if ("8".equals(versionId)) return LinuxDistribution.CENTOS_8;
                return new LinuxDistribution("centos", versionId, versionCodename, LinuxDistribution.Family.RHEL);

            case "rhel":
                if ("8".equals(versionId)) return LinuxDistribution.RHEL_8;
                if ("9".equals(versionId)) return LinuxDistribution.RHEL_9;
                return new LinuxDistribution("rhel", versionId, versionCodename, LinuxDistribution.Family.RHEL);

            case "rocky":
                if ("8".equals(versionId)) return LinuxDistribution.ROCKY_8;
                if ("9".equals(versionId)) return LinuxDistribution.ROCKY_9;
                return new LinuxDistribution("rocky", versionId, versionCodename, LinuxDistribution.Family.RHEL);

            case "almalinux":
                if ("8".equals(versionId)) return LinuxDistribution.ALMA_8;
                if ("9".equals(versionId)) return LinuxDistribution.ALMA_9;
                return new LinuxDistribution("almalinux", versionId, versionCodename, LinuxDistribution.Family.RHEL);

            case "arch":
                return LinuxDistribution.ARCH;

            default:
                // Try to determine family
                LinuxDistribution.Family family = determineFamily(id);
                return new LinuxDistribution(id, versionId, versionCodename, family);
        }
    }

    private static LinuxDistribution detectFromOtherMethods(SshDevice device) {
        try {
            // Try lsb_release
            String lsbRelease = device.sendAndReceive("lsb_release -a 2>/dev/null").get();
            if (lsbRelease != null && !lsbRelease.isEmpty()) {
                return parseLsbRelease(lsbRelease);
            }

            // Try /etc/redhat-release
            String redhatRelease = device.sendAndReceive("cat /etc/redhat-release 2>/dev/null").get();
            if (redhatRelease != null && !redhatRelease.isEmpty()) {
                return parseRedhatRelease(redhatRelease);
            }

            // Try /etc/debian_version
            String debianVersion = device.sendAndReceive("cat /etc/debian_version 2>/dev/null").get();
            if (debianVersion != null && !debianVersion.isEmpty()) {
                return parseDebianVersion(debianVersion);
            }

        } catch (Exception e) {
            logger.warning("Failed alternative detection: " + e.getMessage());
        }

        return LinuxDistribution.UNKNOWN;
    }

    private static LinuxDistribution parseLsbRelease(String lsbRelease) {
        // Parse lsb_release output
        // TODO: Implement if needed
        return LinuxDistribution.UNKNOWN;
    }

    private static LinuxDistribution parseRedhatRelease(String redhatRelease) {
        // Parse /etc/redhat-release
        // TODO: Implement if needed
        return LinuxDistribution.UNKNOWN;
    }

    private static LinuxDistribution parseDebianVersion(String debianVersion) {
        // Parse /etc/debian_version
        // TODO: Implement if needed
        return LinuxDistribution.UNKNOWN;
    }

    private static LinuxDistribution.Family determineFamily(String id) {
        if (id.contains("debian") || id.contains("ubuntu") || id.contains("mint")) {
            return LinuxDistribution.Family.DEBIAN;
        } else if (id.contains("rhel") || id.contains("centos") || id.contains("fedora") ||
                   id.contains("rocky") || id.contains("alma") || id.contains("oracle")) {
            return LinuxDistribution.Family.RHEL;
        } else if (id.contains("arch") || id.contains("manjaro")) {
            return LinuxDistribution.Family.ARCH;
        } else if (id.contains("suse") || id.contains("opensuse")) {
            return LinuxDistribution.Family.SUSE;
        } else if (id.contains("alpine")) {
            return LinuxDistribution.Family.ALPINE;
        } else {
            return LinuxDistribution.Family.UNKNOWN;
        }
    }
}