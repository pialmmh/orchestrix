package com.telcobright.orchestrix.network;

import com.telcobright.orchestrix.network.configurator.LxcNetworkConfigurator;
import com.telcobright.orchestrix.network.entity.Bridge;
import com.telcobright.orchestrix.network.entity.lxc.LxcContainer;
import com.telcobright.orchestrix.network.entity.lxc.LxcNetworkConfig;

/**
 * Simple Hello LXC Test - Creates a Debian 12 container and tests connectivity
 */
public class HelloLxcTest {
    
    private static final String USERNAME = "mustafa";
    private static final String PASSWORD = ""; // Using SSH key
    
    public static void main(String[] args) throws Exception {
        System.out.println("==============================================");
        System.out.println("Hello LXC Test - Debian 12 Container");
        System.out.println("==============================================");
        
        LxcNetworkConfigurator configurator = new LxcNetworkConfigurator();
        
        try {
            // Step 1: Connect to localhost
            System.out.println("\n1. Connecting to localhost via SSH...");
            boolean connected = configurator.connectToLocalHost(USERNAME, PASSWORD).get();
            
            if (!connected) {
                System.err.println("Failed to connect to localhost");
                System.exit(1);
            }
            System.out.println("✓ Connected successfully!");
            
            // Step 2: Set up bridge if it doesn't exist
            System.out.println("\n2. Checking/Creating bridge lxcbr0...");
            
            Bridge bridge = new Bridge();
            bridge.setInterfaceName("lxcbr0");
            bridge.setIpAddress("10.10.199.1");
            bridge.setNetmask(24);
            bridge.setStpEnabled(false);
            bridge.setIpForwardEnabled(true);
            bridge.setMasqueradeEnabled(true);
            
            // Check if bridge exists
            boolean bridgeExists = configurator.bridgeExists("lxcbr0").get();
            
            if (!bridgeExists) {
                System.out.println("Bridge doesn't exist, creating...");
                boolean bridgeCreated = configurator.configureBridge(bridge).get();
                
                if (!bridgeCreated) {
                    System.err.println("Failed to create bridge");
                    System.exit(1);
                }
                
                // Get default interface and configure NAT
                String defaultIf = configurator.getDefaultInterface().get();
                System.out.println("Default interface: " + defaultIf);
                
                boolean natConfigured = configurator.configureNat("10.10.199.0/24", defaultIf).get();
                if (!natConfigured) {
                    System.err.println("Failed to configure NAT");
                }
            }
            System.out.println("✓ Bridge lxcbr0 ready!");
            
            // Step 3: Create and launch container
            System.out.println("\n3. Creating hello-lxc container with Debian 12...");
            
            // Check if container exists and delete if needed
            String checkCmd = "lxc list hello-lxc --format=json | jq length";
            String result = configurator.executeCommand(checkCmd).get();
            
            if (!result.trim().equals("0")) {
                System.out.println("Container exists, deleting...");
                configurator.executeCommand("lxc stop hello-lxc --force").get();
                configurator.executeCommand("lxc delete hello-lxc --force").get();
            }
            
            // Launch container
            System.out.println("Launching container...");
            String launchCmd = "lxc launch images:debian/12 hello-lxc --network=lxcbr0";
            configurator.executeCommand(launchCmd).get();
            
            System.out.println("✓ Container launched!");
            
            // Wait for container to start
            Thread.sleep(3000);
            
            // Step 4: Configure static IP
            System.out.println("\n4. Configuring static IP 10.10.199.50...");
            
            boolean ipConfigured = configurator.configureStaticIp(
                "hello-lxc", 
                "10.10.199.50/24", 
                "10.10.199.1", 
                "8.8.8.8,8.8.4.4"
            ).get();
            
            if (!ipConfigured) {
                System.err.println("Failed to configure static IP");
            }
            System.out.println("✓ Static IP configured!");
            
            // Wait for network to stabilize
            Thread.sleep(2000);
            
            // Step 5: Test connectivity
            System.out.println("\n5. Testing connectivity...");
            
            // Test ping from host to container
            System.out.println("Testing host -> container (10.10.199.50)...");
            boolean hostToContainer = configurator.testConnectivity("10.10.199.50").get();
            System.out.println(hostToContainer ? "✓ Host can ping container" : "✗ Host cannot ping container");
            
            // Test ping from container to gateway
            System.out.println("Testing container -> gateway (10.10.199.1)...");
            String gwPing = configurator.executeCommand(
                "lxc exec hello-lxc -- ping -c 1 10.10.199.1"
            ).get();
            boolean containerToGw = gwPing.contains("1 received");
            System.out.println(containerToGw ? "✓ Container can ping gateway" : "✗ Container cannot ping gateway");
            
            // Test ping from container to Google DNS
            System.out.println("Testing container -> Google DNS (8.8.8.8)...");
            String googlePing = configurator.executeCommand(
                "lxc exec hello-lxc -- ping -c 1 8.8.8.8"
            ).get();
            boolean containerToGoogle = googlePing.contains("1 received");
            System.out.println(containerToGoogle ? "✓ Container can ping Google DNS" : "✗ Container cannot ping Google DNS");
            
            // Test DNS resolution
            System.out.println("Testing DNS resolution (google.com)...");
            String dnsPing = configurator.executeCommand(
                "lxc exec hello-lxc -- ping -c 1 google.com"
            ).get();
            boolean dnsWorks = dnsPing.contains("1 received");
            System.out.println(dnsWorks ? "✓ DNS resolution works" : "✗ DNS resolution failed");
            
            // Step 6: Show container info
            System.out.println("\n6. Container Information:");
            String containerInfo = configurator.executeCommand("lxc list hello-lxc").get();
            System.out.println(containerInfo);
            
            // Show IP configuration
            System.out.println("\nContainer IP Configuration:");
            String ipInfo = configurator.executeCommand(
                "lxc exec hello-lxc -- ip addr show eth0"
            ).get();
            System.out.println(ipInfo);
            
            // Summary
            System.out.println("\n==============================================");
            System.out.println("TEST SUMMARY");
            System.out.println("==============================================");
            System.out.println("Container: hello-lxc (Debian 12)");
            System.out.println("IP Address: 10.10.199.50");
            System.out.println("Gateway: 10.10.199.1");
            System.out.println("Bridge: lxcbr0");
            System.out.println("");
            System.out.println("Connectivity Tests:");
            System.out.println("  Host -> Container: " + (hostToContainer ? "✓ PASS" : "✗ FAIL"));
            System.out.println("  Container -> Gateway: " + (containerToGw ? "✓ PASS" : "✗ FAIL"));
            System.out.println("  Container -> Internet: " + (containerToGoogle ? "✓ PASS" : "✗ FAIL"));
            System.out.println("  DNS Resolution: " + (dnsWorks ? "✓ PASS" : "✗ FAIL"));
            
            if (hostToContainer && containerToGw && containerToGoogle && dnsWorks) {
                System.out.println("\n✓✓✓ ALL TESTS PASSED! ✓✓✓");
            } else {
                System.out.println("\n⚠ Some tests failed. Check network configuration.");
            }
            
            // Optional: Clean up
            System.out.println("\nDo you want to keep the container? (It will be kept by default)");
            System.out.println("To delete: lxc delete hello-lxc --force");
            
        } catch (Exception e) {
            System.err.println("Test failed with error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                configurator.disconnectSsh();
                System.out.println("\nDisconnected from SSH");
            } catch (Exception e) {
                // Ignore disconnect errors
            }
        }
    }
}