package com.telcobright.orchestrix.network.configurator;

import com.telcobright.orchestrix.network.entity.Bridge;
import com.telcobright.orchestrix.network.entity.NetworkInterface;

import java.util.concurrent.CompletableFuture;

/**
 * Base interface for network configuration automation
 * Can be implemented for different platforms (LXC, Docker, Kubernetes, etc.)
 */
public interface NetworkConfigurator {
    
    /**
     * Configure a network bridge
     * @param bridge Bridge configuration entity
     * @return CompletableFuture indicating success/failure
     */
    CompletableFuture<Boolean> configureBridge(Bridge bridge);
    
    /**
     * Remove a network bridge
     * @param bridgeName Name of the bridge to remove
     * @return CompletableFuture indicating success/failure
     */
    CompletableFuture<Boolean> removeBridge(String bridgeName);
    
    /**
     * Configure a network interface
     * @param networkInterface Network interface configuration
     * @return CompletableFuture indicating success/failure
     */
    CompletableFuture<Boolean> configureInterface(NetworkInterface networkInterface);
    
    /**
     * Enable IP forwarding on the host
     * @return CompletableFuture indicating success/failure
     */
    CompletableFuture<Boolean> enableIpForwarding();
    
    /**
     * Configure NAT/Masquerade for a network
     * @param sourceNetwork Source network (e.g., "10.10.199.0/24")
     * @param outputInterface Output interface (e.g., "wlo1", "eth0")
     * @return CompletableFuture indicating success/failure
     */
    CompletableFuture<Boolean> configureNat(String sourceNetwork, String outputInterface);
    
    /**
     * Test network connectivity to an IP address
     * @param ipAddress IP address to test
     * @return CompletableFuture indicating connectivity status
     */
    CompletableFuture<Boolean> testConnectivity(String ipAddress);
    
    /**
     * Get the default network interface (gateway interface)
     * @return CompletableFuture with interface name
     */
    CompletableFuture<String> getDefaultInterface();
    
    /**
     * Check if a bridge exists
     * @param bridgeName Name of the bridge
     * @return CompletableFuture indicating existence
     */
    CompletableFuture<Boolean> bridgeExists(String bridgeName);
}