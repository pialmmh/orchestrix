package com.telcobright.orchestrix.automation.devices.server.linux.base;

/**
 * Represents a Linux distribution with version information
 */
public class LinuxDistribution {

    public enum Family {
        DEBIAN("debian"),
        RHEL("rhel"),
        ARCH("arch"),
        SUSE("suse"),
        ALPINE("alpine"),
        UNKNOWN("unknown");

        private final String name;

        Family(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private final String name;
    private final String version;
    private final String codename;
    private final Family family;

    public LinuxDistribution(String name, String version, String codename, Family family) {
        this.name = name;
        this.version = version;
        this.codename = codename;
        this.family = family;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getCodename() {
        return codename;
    }

    public Family getFamily() {
        return family;
    }

    public String getIdentifier() {
        return name + "-" + version;
    }

    @Override
    public String toString() {
        return String.format("%s %s (%s)", name, version, codename != null ? codename : "");
    }

    // Common distributions
    public static final LinuxDistribution DEBIAN_11 = new LinuxDistribution("debian", "11", "bullseye", Family.DEBIAN);
    public static final LinuxDistribution DEBIAN_12 = new LinuxDistribution("debian", "12", "bookworm", Family.DEBIAN);
    public static final LinuxDistribution UBUNTU_2004 = new LinuxDistribution("ubuntu", "20.04", "focal", Family.DEBIAN);
    public static final LinuxDistribution UBUNTU_2204 = new LinuxDistribution("ubuntu", "22.04", "jammy", Family.DEBIAN);
    public static final LinuxDistribution UBUNTU_2404 = new LinuxDistribution("ubuntu", "24.04", "noble", Family.DEBIAN);
    public static final LinuxDistribution CENTOS_7 = new LinuxDistribution("centos", "7", null, Family.RHEL);
    public static final LinuxDistribution CENTOS_8 = new LinuxDistribution("centos", "8", null, Family.RHEL);
    public static final LinuxDistribution RHEL_8 = new LinuxDistribution("rhel", "8", null, Family.RHEL);
    public static final LinuxDistribution RHEL_9 = new LinuxDistribution("rhel", "9", null, Family.RHEL);
    public static final LinuxDistribution ROCKY_8 = new LinuxDistribution("rocky", "8", null, Family.RHEL);
    public static final LinuxDistribution ROCKY_9 = new LinuxDistribution("rocky", "9", null, Family.RHEL);
    public static final LinuxDistribution ALMA_8 = new LinuxDistribution("almalinux", "8", null, Family.RHEL);
    public static final LinuxDistribution ALMA_9 = new LinuxDistribution("almalinux", "9", null, Family.RHEL);
    public static final LinuxDistribution ARCH = new LinuxDistribution("arch", "rolling", null, Family.ARCH);
    public static final LinuxDistribution UNKNOWN = new LinuxDistribution("unknown", "unknown", null, Family.UNKNOWN);
}