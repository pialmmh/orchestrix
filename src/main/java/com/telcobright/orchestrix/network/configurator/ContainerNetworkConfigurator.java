package com.telcobright.orchestrix.network.configurator;

/**
 * Extended interface for container-specific network configuration
 * Adds container-specific operations on top of basic network configuration
 */
public interface ContainerNetworkConfigurator extends NetworkConfigurator {
    
    /**
     * Attach a container to a bridge network
     * @param containerName Name of the container
     * @param bridgeName Name of the bridge
     * @param interfaceName Interface name in container (e.g., "eth0")
     * @return CompletableFuture indicating success/failure
     */
    java.util.concurrent.CompletableFuture<Boolean> attachContainerToBridge(
        String containerName, String bridgeName, String interfaceName);
    
    /**
     * Configure static IP for a container
     * @param containerName Name of the container
     * @param ipAddress IP address with CIDR (e.g., "10.10.199.100/24")
     * @param gateway Gateway IP address
     * @param dnsServers DNS servers (comma-separated)
     * @return CompletableFuture indicating success/failure
     */
    java.util.concurrent.CompletableFuture<Boolean> configureStaticIp(
        String containerName, String ipAddress, String gateway, String dnsServers);
    
    /**
     * Configure container mount point
     * @param containerName Name of the container
     * @param hostPath Path on the host
     * @param containerPath Path in the container
     * @param mountName Name for the mount
     * @return CompletableFuture indicating success/failure
     */
    java.util.concurrent.CompletableFuture<Boolean> configureMountPoint(
        String containerName, String hostPath, String containerPath, String mountName);
}