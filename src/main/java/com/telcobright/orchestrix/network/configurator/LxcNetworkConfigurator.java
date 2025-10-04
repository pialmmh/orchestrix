package com.telcobright.orchestrix.network.configurator;

import com.telcobright.orchestrix.automation.core.device.UniversalSshDevice;
import com.telcobright.orchestrix.network.entity.Bridge;
import com.telcobright.orchestrix.network.entity.lxc.LxcContainer;
import com.telcobright.orchestrix.network.entity.NetworkInterface;
import com.telcobright.orchestrix.network.entity.lxc.LxcNetworkConfig;
import com.telcobright.orchestrix.network.entity.lxc.LxcMount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * LXC Network Configurator - automates LXC network configuration via SSH
 * Implements ContainerNetworkConfigurator for container-specific operations
 */
public class LxcNetworkConfigurator extends UniversalSshDevice implements ContainerNetworkConfigurator {

    private static final Logger log = LoggerFactory.getLogger(LxcNetworkConfigurator.class);
    
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 22;
    
    public LxcNetworkConfigurator() {
        super("lxc-network-configurator");
    }
    
    /**
     * Connect to local host for LXC management
     */
    public CompletableFuture<Boolean> connectToLocalHost(String username, String password) {
        try {
            return connectSsh(DEFAULT_HOST, DEFAULT_PORT, username, password);
        } catch (Exception e) {
            log.error("Failed to connect to localhost: {}", e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * Execute command with sudo
     */
    public CompletableFuture<String> executeSudoCommand(String command) {
        return executeSshCommandViaExecChannel("sudo " + command);
    }
    
    /**
     * Configure a network bridge
     * @param bridge Bridge configuration entity
     * @return CompletableFuture indicating success/failure
     */
    @Override
    public CompletableFuture<Boolean> configureBridge(Bridge bridge) {
        log.info("Setting up bridge: {}", bridge.getInterfaceName());
        
        List<CompletableFuture<String>> futures = new ArrayList<>();
        
        // Check if bridge exists and delete if needed
        CompletableFuture<String> checkBridge = executeSshCommandViaExecChannel(
            "ip link show " + bridge.getInterfaceName() + " 2>/dev/null | wc -l")
            .thenCompose(result -> {
                if (!result.trim().equals("0")) {
                    log.info("Bridge {} exists, deleting...", bridge.getInterfaceName());
                    return executeSudoCommand("ip link delete " + bridge.getInterfaceName());
                }
                return CompletableFuture.completedFuture("");
            });
        
        futures.add(checkBridge);
        
        // Wait for cleanup, then create bridge
        return checkBridge.thenCompose(v -> {
            List<String> commands = new ArrayList<>();
            
            // Create bridge
            commands.add("ip link add " + bridge.getInterfaceName() + " type bridge");
            
            // Configure bridge settings
            if (!bridge.getStpEnabled()) {
                commands.add("echo 0 > /sys/class/net/" + bridge.getInterfaceName() + "/bridge/stp_state");
            }
            
            if (bridge.getForwardDelay() != null) {
                commands.add("echo " + bridge.getForwardDelay() + 
                    " > /sys/class/net/" + bridge.getInterfaceName() + "/bridge/forward_delay");
            }
            
            // Bring up bridge
            commands.add("ip link set " + bridge.getInterfaceName() + " up");
            
            // Add IP address to bridge (host's gateway IP)
            if (bridge.getIpAddress() != null && bridge.getNetmask() != null) {
                commands.add("ip addr add " + bridge.getCidr() + " dev " + bridge.getInterfaceName());
            }
            
            // Enable IP forwarding
            if (bridge.getIpForwardEnabled()) {
                commands.add("sysctl -w net.ipv4.ip_forward=1");
            }
            
            // Execute commands sequentially
            CompletableFuture<String> result = CompletableFuture.completedFuture("");
            
            for (String cmd : commands) {
                result = result.thenCompose(prev -> {
                    log.debug("Executing: {}", cmd);
                    return executeSudoCommand(cmd);
                });
            }
            
            // Set up NAT if enabled
            if (bridge.getMasqueradeEnabled()) {
                result = result.thenCompose(prev -> setupNat(bridge));
            }
            
            // Configure LXD
            result = result.thenCompose(prev -> configureLxdBridge(bridge));
            
            return result.thenApply(v2 -> {
                log.info("Bridge {} setup complete!", bridge.getInterfaceName());
                return true;
            });
        }).exceptionally(e -> {
            log.error("Failed to setup bridge: {}", e.getMessage());
            return false;
        });
    }
    
    /**
     * Set up NAT/Masquerade for bridge
     */
    private CompletableFuture<String> setupNat(Bridge bridge) {
        // Get default interface
        return executeSshCommandViaExecChannel("ip route | grep default | awk '{print $5}' | head -n1")
            .thenCompose(defaultIf -> {
                defaultIf = defaultIf.trim();
                log.info("Setting up NAT from {} to {}", bridge.getInterfaceName(), defaultIf);
                
                List<String> commands = new ArrayList<>();
                
                // Remove existing rule if any
                commands.add("iptables -t nat -D POSTROUTING -s " + bridge.getNetworkAddress() + 
                    " -j MASQUERADE 2>/dev/null || true");
                
                // Add NAT rule
                commands.add("iptables -t nat -A POSTROUTING -s " + bridge.getNetworkAddress() + 
                    " -o " + defaultIf + " -j MASQUERADE");
                
                // Allow forwarding
                commands.add("iptables -D FORWARD -i " + bridge.getInterfaceName() + " -j ACCEPT 2>/dev/null || true");
                commands.add("iptables -D FORWARD -o " + bridge.getInterfaceName() + " -j ACCEPT 2>/dev/null || true");
                commands.add("iptables -A FORWARD -i " + bridge.getInterfaceName() + " -j ACCEPT");
                commands.add("iptables -A FORWARD -o " + bridge.getInterfaceName() + " -j ACCEPT");
                
                // Execute NAT commands
                CompletableFuture<String> result = CompletableFuture.completedFuture("");
                for (String cmd : commands) {
                    result = result.thenCompose(prev -> executeSudoCommand(cmd));
                }
                
                return result;
            });
    }
    
    /**
     * Configure LXD to use the bridge
     */
    private CompletableFuture<String> configureLxdBridge(Bridge bridge) {
        log.info("Configuring LXD to use bridge {}", bridge.getInterfaceName());
        
        // Check if LXD network exists
        return executeSshCommandViaExecChannel("lxc network list | grep " + bridge.getInterfaceName() + " | wc -l")
            .thenCompose(result -> {
                CompletableFuture<String> deleteOld = CompletableFuture.completedFuture("");
                
                if (!result.trim().equals("0")) {
                    log.info("LXD network exists, deleting...");
                    deleteOld = executeSshCommandViaExecChannel("lxc network delete " + bridge.getInterfaceName() + " 2>/dev/null || true");
                }
                
                // Create LXD network without DHCP/NAT (we handle it manually)
                return deleteOld.thenCompose(v -> 
                    executeSshCommandViaExecChannel("lxc network create " + bridge.getInterfaceName() + 
                        " ipv4.address=none ipv6.address=none ipv4.nat=false ipv6.nat=false" +
                        " ipv4.dhcp=false ipv6.dhcp=false")
                );
            });
    }
    
    /**
     * Launch LXC container with static IP configuration
     */
    public CompletableFuture<Boolean> launchContainer(LxcContainer container) {
        log.info("Launching container: {}", container.getContainerName());
        
        LxcNetworkConfig netConfig = container.getNetworkConfig();
        if (netConfig == null) {
            log.error("Container must have network configuration");
            return CompletableFuture.completedFuture(false);
        }
        
        // Check if container exists and delete if needed
        return executeSshCommandViaExecChannel("lxc list " + container.getContainerName() + " --format=json | jq length")
            .thenCompose(result -> {
                CompletableFuture<String> cleanup = CompletableFuture.completedFuture("");
                
                if (!result.trim().equals("0")) {
                    log.info("Container exists, stopping and deleting...");
                    cleanup = executeSshCommandViaExecChannel("lxc stop " + container.getContainerName() + " --force 2>/dev/null || true")
                        .thenCompose(v -> executeSshCommandViaExecChannel("lxc delete " + container.getContainerName() + " --force"));
                }
                
                return cleanup;
            })
            .thenCompose(v -> {
                // Launch container
                String launchCmd = "lxc launch " + container.getBaseImage() + " " + 
                    container.getContainerName() + " --network=" + netConfig.getBridgeName();
                log.info("Launching: {}", launchCmd);
                return executeSshCommandViaExecChannel(launchCmd);
            })
            .thenCompose(v -> {
                // Wait a bit for container to start
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // Configure static IP
                log.info("Configuring static IP: {}", netConfig.getIpAddress());
                return configureContainerNetwork(container.getContainerName(), netConfig);
            })
            .thenCompose(v -> {
                // Configure mounts if any
                if (container.getMounts() != null && !container.getMounts().isEmpty()) {
                    return configureMounts(container);
                }
                return CompletableFuture.completedFuture("");
            })
            .thenApply(v -> {
                log.info("Container {} launched successfully!", container.getContainerName());
                return true;
            })
            .exceptionally(e -> {
                log.error("Failed to launch container: {}", e.getMessage());
                return false;
            });
    }
    
    /**
     * Configure static network in container
     */
    private CompletableFuture<String> configureContainerNetwork(String containerName, LxcNetworkConfig netConfig) {
        // Try netplan first
        String netplanConfig = netConfig.generateNetplanConfig()
            .replace("\n", "\\n")
            .replace("\"", "\\\"")
            .replace("'", "\\'");
        
        return executeSshCommandViaExecChannel(
            "lxc exec " + containerName + " -- bash -c \"echo '" + netplanConfig + "' > /etc/netplan/10-lxc.yaml\"")
            .thenCompose(v -> 
                executeSshCommandViaExecChannel("lxc exec " + containerName + " -- netplan apply 2>&1 || echo 'netplan-failed'")
            )
            .thenCompose(result -> {
                if (result.contains("netplan-failed")) {
                    log.info("Netplan not available, using ip commands...");
                    
                    // Use ip commands as fallback
                    String[] ipCommands = netConfig.generateIpCommands().split("\n");
                    CompletableFuture<String> cmdResult = CompletableFuture.completedFuture("");
                    
                    for (String cmd : ipCommands) {
                        if (!cmd.trim().isEmpty()) {
                            cmdResult = cmdResult.thenCompose(prev -> 
                                executeSshCommandViaExecChannel("lxc exec " + containerName + " -- bash -c \"" + cmd + "\"")
                            );
                        }
                    }
                    
                    return cmdResult;
                }
                return CompletableFuture.completedFuture(result);
            });
    }
    
    /**
     * Configure container mounts
     */
    private CompletableFuture<String> configureMounts(LxcContainer container) {
        if (container.getMounts() == null || container.getMounts().isEmpty()) {
            return CompletableFuture.completedFuture("");
        }
        
        CompletableFuture<String> chain = CompletableFuture.completedFuture("");
        
        for (LxcMount mount : container.getMounts()) {
            // Create directory if needed
            if (mount.getCreateIfMissing()) {
                chain = chain.thenCompose(prev -> 
                    executeSudoCommand("mkdir -p " + mount.getHostPath())
                );
            }
            
            // Add mount
            final String containerName = container.getContainerName();
            final String hostPath = mount.getHostPath();
            final String containerPath = mount.getContainerPath();
            final String mountName = mount.getMountName();
            
            chain = chain.thenCompose(prev -> 
                executeSshCommandViaExecChannel("lxc config device add " + containerName + 
                    " " + mountName + " disk source=" + hostPath + 
                    " path=" + containerPath)
            ).thenApply(v -> {
                log.info("Mounted: {} -> {}", hostPath, containerPath);
                return v;
            });
        }
        
        return chain;
    }
    
    /**
     * Test container connectivity
     */
    public CompletableFuture<Boolean> testContainerConnectivity(String containerIp) {
        return testConnectivity(containerIp);
    }
    
    // ========== NetworkConfigurator Interface Implementation ==========
    
    @Override
    public CompletableFuture<Boolean> removeBridge(String bridgeName) {
        return executeSudoCommand("ip link delete " + bridgeName)
            .thenApply(v -> true)
            .exceptionally(e -> {
                log.error("Failed to remove bridge {}: {}", bridgeName, e.getMessage());
                return false;
            });
    }
    
    @Override
    public CompletableFuture<Boolean> configureInterface(NetworkInterface networkInterface) {
        List<String> commands = new ArrayList<>();
        
        // Configure interface IP
        if (networkInterface.getIpAddress() != null && networkInterface.getNetmask() != null) {
            commands.add("ip addr add " + networkInterface.getIpAddress() + "/" + 
                networkInterface.getNetmask() + " dev " + networkInterface.getInterfaceName());
        }
        
        // Set interface state
        if (networkInterface.getState() == NetworkInterface.InterfaceState.UP) {
            commands.add("ip link set " + networkInterface.getInterfaceName() + " up");
        }
        
        // Execute commands
        CompletableFuture<String> result = CompletableFuture.completedFuture("");
        for (String cmd : commands) {
            result = result.thenCompose(prev -> executeSudoCommand(cmd));
        }
        
        return result.thenApply(v -> true).exceptionally(e -> false);
    }
    
    @Override
    public CompletableFuture<Boolean> enableIpForwarding() {
        return executeSudoCommand("sysctl -w net.ipv4.ip_forward=1")
            .thenApply(v -> true)
            .exceptionally(e -> false);
    }
    
    @Override
    public CompletableFuture<Boolean> configureNat(String sourceNetwork, String outputInterface) {
        List<String> commands = new ArrayList<>();
        
        // Remove existing rule if any
        commands.add("iptables -t nat -D POSTROUTING -s " + sourceNetwork + 
            " -j MASQUERADE 2>/dev/null || true");
        
        // Add NAT rule
        commands.add("iptables -t nat -A POSTROUTING -s " + sourceNetwork + 
            " -o " + outputInterface + " -j MASQUERADE");
        
        // Execute commands
        CompletableFuture<String> result = CompletableFuture.completedFuture("");
        for (String cmd : commands) {
            result = result.thenCompose(prev -> executeSudoCommand(cmd));
        }
        
        return result.thenApply(v -> true).exceptionally(e -> false);
    }
    
    @Override
    public CompletableFuture<Boolean> testConnectivity(String ipAddress) {
        return executeSshCommandViaExecChannel("ping -c 1 -W 2 " + ipAddress)
            .thenApply(result -> {
                boolean success = result.contains("1 received") || 
                    result.contains("1 packets transmitted, 1 received");
                log.info("Ping test to {}: {}", ipAddress, success ? "SUCCESS" : "FAILED");
                return success;
            })
            .exceptionally(e -> {
                log.error("Ping test failed: {}", e.getMessage());
                return false;
            });
    }
    
    @Override
    public CompletableFuture<String> getDefaultInterface() {
        return executeSshCommandViaExecChannel("ip route | grep default | awk '{print $5}' | head -n1")
            .thenApply(String::trim);
    }
    
    @Override
    public CompletableFuture<Boolean> bridgeExists(String bridgeName) {
        return executeSshCommandViaExecChannel("ip link show " + bridgeName + " 2>/dev/null | wc -l")
            .thenApply(result -> !result.trim().equals("0"));
    }
    
    // ========== NetworkingDevice Abstract Methods Implementation ==========
    
    @Override
    public CompletableFuture<String> send(String command) throws IOException {
        return sendSshCommand(command);
    }
    
    @Override
    public CompletableFuture<String> receive() throws IOException {
        return receiveSshResponse();
    }
    
    @Override
    public CompletableFuture<String> getSystemInfo() {
        return executeSshCommandViaExecChannel("uname -a && lsb_release -a 2>/dev/null || cat /etc/os-release");
    }
    
    @Override
    public CompletableFuture<String> getInterfaces() {
        return executeSshCommandViaExecChannel("ip addr show");
    }
    
    @Override
    public CompletableFuture<String> getRoutes() {
        return executeSshCommandViaExecChannel("ip route show");
    }
    
    @Override
    public CompletableFuture<String> backup(String backupName) {
        // Not applicable for network configurator
        return CompletableFuture.completedFuture("Backup not supported for network configurator");
    }
    
    @Override
    public CompletableFuture<String> reboot() {
        // Could reboot the host system, but probably not what we want
        return CompletableFuture.completedFuture("Reboot not supported for network configurator");
    }
    
    // ========== ContainerNetworkConfigurator Interface Implementation ==========
    
    @Override
    public CompletableFuture<Boolean> attachContainerToBridge(
            String containerName, String bridgeName, String interfaceName) {
        return executeSshCommandViaExecChannel(
            "lxc network attach " + bridgeName + " " + containerName + " " + interfaceName)
            .thenApply(v -> true)
            .exceptionally(e -> {
                log.error("Failed to attach container {} to bridge {}: {}", 
                    containerName, bridgeName, e.getMessage());
                return false;
            });
    }
    
    @Override
    public CompletableFuture<Boolean> configureStaticIp(
            String containerName, String ipAddress, String gateway, String dnsServers) {
        // Create a temporary network config
        LxcNetworkConfig config = new LxcNetworkConfig();
        String[] ipParts = ipAddress.split("/");
        config.setIpAddress(ipParts[0]);
        config.setNetmask(ipParts.length > 1 ? Integer.parseInt(ipParts[1]) : 24);
        config.setGateway(gateway);
        config.setDnsServers(dnsServers);
        
        return configureContainerNetwork(containerName, config)
            .thenApply(v -> true)
            .exceptionally(e -> false);
    }
    
    @Override
    public CompletableFuture<Boolean> configureMountPoint(
            String containerName, String hostPath, String containerPath, String mountName) {
        // Create directory if needed
        return executeSudoCommand("mkdir -p " + hostPath)
            .thenCompose(v -> executeSshCommandViaExecChannel(
                "lxc config device add " + containerName + " " + mountName + 
                " disk source=" + hostPath + " path=" + containerPath))
            .thenApply(v -> {
                log.info("Mounted {} -> {} in container {}", hostPath, containerPath, containerName);
                return true;
            })
            .exceptionally(e -> {
                log.error("Failed to configure mount: {}", e.getMessage());
                return false;
            });
    }
    
    /**
     * Public method to execute SSH commands via exec channel
     * Exposes the protected method from UniversalSshDevice for testing
     * @param command Command to execute
     * @return CompletableFuture with command output
     */
    public CompletableFuture<String> executeCommand(String command) {
        return executeSshCommandViaExecChannel(command);
    }
}