package com.orchestrix.automation.vxlan.example;

import com.orchestrix.automation.vxlan.api.VXLANService;
import com.orchestrix.automation.vxlan.core.BaseVXLANService;
import com.orchestrix.automation.ssh.api.SSHClient;
import com.orchestrix.automation.ssh.core.BaseSSHClient;

import java.util.Arrays;
import java.util.List;

/**
 * Example VXLAN service implementation
 */
public class VXLANExample {
    
    /**
     * Concrete VXLAN service implementation
     */
    static class OrchestriXVXLANService extends BaseVXLANService {
        
        @Override
        protected SSHClient createSSHClient() {
            return new BaseSSHClient() {
                @Override
                protected void afterConnect() throws SSHException {
                    System.out.println("Connected to host for VXLAN operations");
                }
            };
        }
        
        @Override
        protected void afterInitializeHost(String host, VXLANConfig config) throws VXLANException {
            System.out.println("Host " + host + " initialized for VXLAN");
        }
        
        @Override
        protected void afterCreateVXLAN(String host, String interfaceName, int vxlanId) throws VXLANException {
            System.out.println("VXLAN " + interfaceName + " (ID: " + vxlanId + ") created on " + host);
        }
    }
    
    public static void main(String[] args) {
        // Example 1: Single host VXLAN setup
        singleHostExample();
        
        // Example 2: Multi-host mesh network
        meshNetworkExample();
        
        // Example 3: VXLAN with bridge
        bridgedVXLANExample();
        
        // Example 4: Monitoring and health check
        monitoringExample();
    }
    
    private static void singleHostExample() {
        System.out.println("\n=== Single Host VXLAN Example ===");
        
        VXLANService vxlan = new OrchestriXVXLANService();
        
        try {
            String host = "127.0.0.1";
            
            // Configure VXLAN
            VXLANService.VXLANConfig config = new VXLANService.VXLANConfig();
            config.setVxlanId(100);
            config.setInterfaceName("vxlan100");
            config.setMulticastGroup("239.1.1.100");
            config.setPhysicalInterface("eth0");
            config.setMtu(1450);
            
            // Initialize host
            System.out.println("Initializing host...");
            vxlan.initializeHost(host, config);
            
            // Create VXLAN interface
            System.out.println("Creating VXLAN interface...");
            vxlan.createVXLAN(host, config.getInterfaceName(), config.getVxlanId(), config.getMulticastGroup());
            
            // Configure IP
            System.out.println("Configuring IP address...");
            vxlan.configureIP(host, config.getInterfaceName(), "10.200.1.1", 24);
            
            // Check status
            VXLANService.VXLANStatus status = vxlan.getStatus(host, config.getInterfaceName());
            System.out.println("VXLAN Status: " + status.getState());
            System.out.println("Active: " + status.isActive());
            
            // List all VXLANs
            List<VXLANService.VXLANInterface> interfaces = vxlan.listVXLANs(host);
            System.out.println("VXLAN interfaces on host:");
            for (VXLANService.VXLANInterface iface : interfaces) {
                System.out.println("  - " + iface.getName() + " (ID: " + iface.getVxlanId() + ", State: " + iface.getState() + ")");
            }
            
            // Cleanup
            System.out.println("Cleaning up...");
            vxlan.deleteVXLAN(host, config.getInterfaceName());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void meshNetworkExample() {
        System.out.println("\n=== Mesh Network Example ===");
        
        VXLANService vxlan = new OrchestriXVXLANService();
        
        try {
            // Define hosts (in real scenario, these would be different IPs)
            List<String> hosts = Arrays.asList(
                "127.0.0.1"
                // "192.168.1.101",
                // "192.168.1.102"
            );
            
            // Configure mesh network
            VXLANService.VXLANConfig config = new VXLANService.VXLANConfig();
            config.setVxlanId(200);
            config.setInterfaceName("vxmesh");
            config.setMulticastGroup("239.1.1.200");
            config.setSubnet("10.200.0.0/16");
            
            // Setup mesh
            System.out.println("Setting up VXLAN mesh network...");
            vxlan.setupMesh(hosts, config);
            
            System.out.println("Mesh network created successfully!");
            
            // Check health on all hosts
            for (String host : hosts) {
                VXLANService.HealthStatus health = vxlan.healthCheck(host, config.getInterfaceName());
                System.out.println("Host " + host + " health: " + health.getStatus());
                if (health.getIssues() != null && !health.getIssues().isEmpty()) {
                    System.out.println("  Issues: " + String.join(", ", health.getIssues()));
                }
            }
            
            // Teardown mesh
            System.out.println("Tearing down mesh network...");
            vxlan.teardownMesh(hosts, config.getInterfaceName());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void bridgedVXLANExample() {
        System.out.println("\n=== Bridged VXLAN Example ===");
        
        VXLANService vxlan = new OrchestriXVXLANService();
        
        try {
            String host = "127.0.0.1";
            
            // Configure VXLAN with bridge
            VXLANService.VXLANConfig config = new VXLANService.VXLANConfig();
            config.setVxlanId(300);
            config.setInterfaceName("vxlan300");
            config.setMulticastGroup("239.1.1.30");
            config.setBridgeName("br-vxlan300");
            
            // Initialize and create VXLAN
            vxlan.initializeHost(host, config);
            vxlan.createVXLAN(host, config.getInterfaceName(), config.getVxlanId(), config.getMulticastGroup());
            
            // Create bridge and attach VXLAN
            System.out.println("Creating bridge " + config.getBridgeName());
            vxlan.createBridge(host, config.getBridgeName(), config.getInterfaceName());
            
            // Configure IP on bridge
            vxlan.configureIP(host, config.getBridgeName(), "10.200.3.1", 24);
            
            // Add VTEP entries (in real scenario, these would be remote hosts)
            System.out.println("Adding VTEP entries...");
            vxlan.addVTEP(host, "192.168.1.101", "aa:bb:cc:dd:ee:01");
            vxlan.addVTEP(host, "192.168.1.102", "aa:bb:cc:dd:ee:02");
            
            // Get status
            VXLANService.VXLANStatus status = vxlan.getStatus(host, config.getBridgeName());
            System.out.println("Bridge status: " + status.getState());
            
            // Cleanup
            System.out.println("Cleaning up...");
            vxlan.deleteVXLAN(host, config.getInterfaceName());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void monitoringExample() {
        System.out.println("\n=== Monitoring Example ===");
        
        VXLANService vxlan = new OrchestriXVXLANService();
        
        try {
            String host = "127.0.0.1";
            String interfaceName = "vxlan400";
            
            // Create VXLAN for monitoring
            VXLANService.VXLANConfig config = new VXLANService.VXLANConfig();
            config.setVxlanId(400);
            config.setInterfaceName(interfaceName);
            
            vxlan.initializeHost(host, config);
            vxlan.createVXLAN(host, interfaceName, 400, "239.1.1.400");
            vxlan.configureIP(host, interfaceName, "10.200.4.1", 24);
            
            // Monitor for a period
            System.out.println("Monitoring VXLAN interface...");
            
            for (int i = 0; i < 3; i++) {
                Thread.sleep(2000);
                
                // Get status
                VXLANService.VXLANStatus status = vxlan.getStatus(host, interfaceName);
                System.out.println("\nStatus check #" + (i + 1));
                System.out.println("  State: " + status.getState());
                System.out.println("  Active: " + status.isActive());
                
                if (status.getDetails() != null) {
                    System.out.println("  Details:");
                    status.getDetails().forEach((k, v) -> 
                        System.out.println("    " + k + ": " + v));
                }
                
                // Health check
                VXLANService.HealthStatus health = vxlan.healthCheck(host, interfaceName);
                System.out.println("  Health: " + health.getStatus());
            }
            
            // Backup configuration
            System.out.println("\nBacking up configuration...");
            String backup = vxlan.backupConfiguration(host);
            System.out.println("Backup size: " + backup.length() + " bytes");
            
            // Cleanup
            vxlan.deleteVXLAN(host, interfaceName);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}