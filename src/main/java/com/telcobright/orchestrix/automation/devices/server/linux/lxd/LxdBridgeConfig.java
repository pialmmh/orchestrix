package com.telcobright.orchestrix.automation.devices.server.linux.lxd;

/**
 * Configuration for LXD bridge setup
 */
public class LxdBridgeConfig {
    private String bridgeName = "lxdbr0";
    private String ipv4Address = "10.0.3.1";
    private String ipv4Netmask = "24";
    private String ipv4Network = "10.0.3.1/24";
    private boolean ipv4Nat = true;
    private String ipv4DhcpRange = "10.0.3.2,10.0.3.254";
    private boolean ipv6Enabled = false;
    private String storagePool = "default";
    private String storageBackend = "dir";
    private String storageSize = "30GB";

    public LxdBridgeConfig() {
    }

    // Builder pattern for flexible configuration
    public static class Builder {
        private LxdBridgeConfig config = new LxdBridgeConfig();

        public Builder bridgeName(String bridgeName) {
            config.bridgeName = bridgeName;
            return this;
        }

        public Builder ipv4Network(String network) {
            config.ipv4Network = network;
            // Parse network to extract address and netmask
            if (network.contains("/")) {
                String[] parts = network.split("/");
                config.ipv4Address = parts[0];
                config.ipv4Netmask = parts[1];
            }
            return this;
        }

        public Builder ipv4Nat(boolean enabled) {
            config.ipv4Nat = enabled;
            return this;
        }

        public Builder ipv4DhcpRange(String range) {
            config.ipv4DhcpRange = range;
            return this;
        }

        public Builder ipv6Enabled(boolean enabled) {
            config.ipv6Enabled = enabled;
            return this;
        }

        public Builder storagePool(String pool) {
            config.storagePool = pool;
            return this;
        }

        public Builder storageBackend(String backend) {
            config.storageBackend = backend;
            return this;
        }

        public Builder storageSize(String size) {
            config.storageSize = size;
            return this;
        }

        public LxdBridgeConfig build() {
            return config;
        }
    }

    // Getters
    public String getBridgeName() { return bridgeName; }
    public String getIpv4Address() { return ipv4Address; }
    public String getIpv4Netmask() { return ipv4Netmask; }
    public String getIpv4Network() { return ipv4Network; }
    public boolean isIpv4Nat() { return ipv4Nat; }
    public String getIpv4DhcpRange() { return ipv4DhcpRange; }
    public boolean isIpv6Enabled() { return ipv6Enabled; }
    public String getStoragePool() { return storagePool; }
    public String getStorageBackend() { return storageBackend; }
    public String getStorageSize() { return storageSize; }

    /**
     * Generate preseed YAML for LXD init
     */
    public String toPreseedYaml() {
        StringBuilder yaml = new StringBuilder();
        yaml.append("config:\n");
        yaml.append("  core.https_address: '[::]:8443'\n");
        yaml.append("networks:\n");
        yaml.append("- config:\n");
        yaml.append("    ipv4.address: ").append(ipv4Network).append("\n");
        yaml.append("    ipv4.nat: \"").append(ipv4Nat).append("\"\n");
        if (ipv4DhcpRange != null && !ipv4DhcpRange.isEmpty()) {
            yaml.append("    ipv4.dhcp.ranges: ").append(ipv4DhcpRange).append("\n");
        }
        yaml.append("    ipv6.address: ");
        if (ipv6Enabled) {
            yaml.append("auto\n");
        } else {
            yaml.append("none\n");
        }
        yaml.append("  description: LXD internal network\n");
        yaml.append("  name: ").append(bridgeName).append("\n");
        yaml.append("  type: bridge\n");
        yaml.append("storage_pools:\n");
        yaml.append("- config:\n");
        if ("btrfs".equals(storageBackend) || "zfs".equals(storageBackend)) {
            yaml.append("    size: ").append(storageSize).append("\n");
        }
        yaml.append("  description: Default storage pool\n");
        yaml.append("  name: ").append(storagePool).append("\n");
        yaml.append("  driver: ").append(storageBackend).append("\n");
        yaml.append("profiles:\n");
        yaml.append("- config: {}\n");
        yaml.append("  description: Default LXD profile\n");
        yaml.append("  devices:\n");
        yaml.append("    eth0:\n");
        yaml.append("      name: eth0\n");
        yaml.append("      network: ").append(bridgeName).append("\n");
        yaml.append("      type: nic\n");
        yaml.append("    root:\n");
        yaml.append("      path: /\n");
        yaml.append("      pool: ").append(storagePool).append("\n");
        yaml.append("      type: disk\n");
        yaml.append("  name: default\n");

        return yaml.toString();
    }
}