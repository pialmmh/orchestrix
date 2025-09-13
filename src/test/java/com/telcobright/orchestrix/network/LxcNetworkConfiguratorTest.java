package com.telcobright.orchestrix.network;

import com.telcobright.orchestrix.network.entity.Bridge;
import com.telcobright.orchestrix.network.entity.lxc.LxcContainer;
import com.telcobright.orchestrix.network.entity.lxc.LxcNetworkConfig;
import com.telcobright.orchestrix.network.configurator.LxcNetworkConfigurator;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

/**
 * Test class for LXC Network Configurator using existing UniversalSshDevice
 */
@Slf4j
public class LxcNetworkConfiguratorTest {
    
    private static final String USERNAME = "mustafa";
    private static final String PASSWORD = ""; // Set if needed, or use SSH key
    
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("LXC Network Manager Test");
        System.out.println("Using existing UniversalSshDevice infrastructure");
        System.out.println("DO NOT RUN without explicit permission!");
        System.out.println("==================================================");
        
        // Uncomment to run test
        /*
        runTest();
        */
    }
    
    private static void runTest() {
        LxcNetworkConfigurator configurator = new LxcNetworkConfigurator();
        
        try {
            // Connect to localhost
            CompletableFuture<Boolean> connected = configurator.connectToLocalHost(USERNAME, PASSWORD);
            
            if (!connected.get()) {
                log.error("Failed to connect to localhost");
                return;
            }
            
            log.info("Connected successfully!");
            
            // Create bridge configuration
            Bridge bridge = new Bridge();
            bridge.setInterfaceName("lxcbr0");
            bridge.setIpAddress("10.10.199.1");
            bridge.setNetmask(24);
            bridge.setStpEnabled(false);
            bridge.setIpForwardEnabled(true);
            bridge.setMasqueradeEnabled(true);
            
            // Configure bridge
            CompletableFuture<Boolean> bridgeSetup = configurator.configureBridge(bridge);
            
            if (!bridgeSetup.get()) {
                log.error("Failed to setup bridge");
                return;
            }
            
            log.info("Bridge setup complete!");
            
            // Create container configuration
            LxcContainer container = new LxcContainer();
            container.setContainerName("test-static-ip");
            container.setBaseImage("images:debian/12");
            
            // Configure static network
            LxcNetworkConfig netConfig = new LxcNetworkConfig();
            netConfig.setBridgeName("lxcbr0");
            netConfig.setIpAddress("10.10.199.100");
            netConfig.setNetmask(24);
            netConfig.setGateway("10.10.199.1");
            netConfig.setDnsServers("8.8.8.8,8.8.4.4");
            container.setNetworkConfig(netConfig);
            
            // Launch container
            CompletableFuture<Boolean> containerLaunch = configurator.launchContainer(container);
            
            if (!containerLaunch.get()) {
                log.error("Failed to launch container");
                return;
            }
            
            log.info("Container launched successfully!");
            
            // Test connectivity
            Thread.sleep(2000); // Wait for network to stabilize
            CompletableFuture<Boolean> pingTest = configurator.testContainerConnectivity("10.10.199.100");
            
            if (pingTest.get()) {
                log.info("✓ Container connectivity test PASSED");
            } else {
                log.warn("✗ Container connectivity test FAILED");
            }
            
            // Show container status
            configurator.executeSshCommandViaExecChannel("lxc list test-static-ip")
                .thenAccept(result -> log.info("Container status:\n{}", result));
            
            Thread.sleep(1000);
            
        } catch (Exception e) {
            log.error("Test failed: ", e);
        } finally {
            try {
                configurator.disconnectSsh();
                log.info("Disconnected from SSH");
            } catch (Exception e) {
                log.error("Error disconnecting: ", e);
            }
        }
    }
    
    /**
     * Test to create FusionPBX container
     */
    private static void testFusionPBX() {
        LxcNetworkConfigurator configurator = new LxcNetworkConfigurator();
        
        try {
            // Connect
            configurator.connectToLocalHost(USERNAME, PASSWORD).get();
            
            // Setup bridge if needed
            Bridge bridge = new Bridge();
            bridge.setInterfaceName("lxcbr0");
            bridge.setIpAddress("10.10.199.1");
            bridge.setNetmask(24);
            bridge.setStpEnabled(false);
            bridge.setIpForwardEnabled(true);
            bridge.setMasqueradeEnabled(true);
            
            configurator.configureBridge(bridge).get();
            
            // Create FusionPBX container
            LxcContainer fusionPbx = new LxcContainer();
            fusionPbx.setContainerName("fusionpbx-01");
            fusionPbx.setBaseImage("fusion-pbx-base"); // Assuming you built this
            
            // Static IP for FusionPBX
            LxcNetworkConfig netConfig = new LxcNetworkConfig();
            netConfig.setBridgeName("lxcbr0");
            netConfig.setIpAddress("10.10.199.100");
            netConfig.setNetmask(24);
            netConfig.setGateway("10.10.199.1");
            netConfig.setDnsServers("8.8.8.8,8.8.4.4");
            fusionPbx.setNetworkConfig(netConfig);
            
            // Launch
            configurator.launchContainer(fusionPbx).get();
            
            log.info("FusionPBX container launched at 10.10.199.100");
            log.info("Access web interface at: http://10.10.199.100");
            
        } catch (Exception e) {
            log.error("Failed to create FusionPBX container: ", e);
        } finally {
            try {
                configurator.disconnectSsh();
            } catch (Exception e) {
                log.error("Error disconnecting: ", e);
            }
        }
    }
}