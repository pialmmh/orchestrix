package com.orchestrix.automation.vxlan.example;

import com.orchestrix.automation.vxlan.core.VXLANOrchestrator;
import com.orchestrix.automation.vxlan.core.SubnetManager;

import java.util.Map;
import java.util.Scanner;

/**
 * Example usage of VXLAN Orchestrator for LXC containers
 */
public class VXLANLXCExample {
    
    public static void main(String[] args) {
        System.out.println("=== VXLAN LXC Network Orchestrator ===\n");
        
        VXLANOrchestrator orchestrator = new VXLANOrchestrator();
        
        try {
            // Initialize orchestrator
            System.out.println("1. Initializing orchestrator and connecting to host...");
            orchestrator.initialize();
            System.out.println("   ✓ Connected successfully\n");
            
            // Setup all subnets
            System.out.println("2. Setting up VXLAN subnets...");
            orchestrator.setupAllSubnets();
            System.out.println("   ✓ All subnets initialized\n");
            
            // Display subnet status
            System.out.println("3. Subnet Status:");
            displaySubnetStatus(orchestrator);
            System.out.println();
            
            // Configure specific containers
            System.out.println("4. Configuring containers:");
            
            // Configure dev-env container
            configureContainer(orchestrator, "dev-env-instance-01");
            
            // Configure Jenkins agent
            configureContainer(orchestrator, "jenkins-agent");
            
            // Example: Assign specific IP to a container
            System.out.println("\n5. Assigning specific IPs:");
            assignSpecificIP(orchestrator, "mysql-master", "database", "10.103.0.10");
            
            // Example: Move container to different subnet
            System.out.println("\n6. Moving container to different subnet:");
            moveContainer(orchestrator, "dev-env-instance-01", "production", null);
            
            // Interactive mode
            System.out.println("\n7. Interactive Mode");
            interactiveMode(orchestrator);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            orchestrator.shutdown();
            System.out.println("\nOrchestrator shut down");
        }
    }
    
    private static void displaySubnetStatus(VXLANOrchestrator orchestrator) {
        try {
            Map<String, VXLANOrchestrator.SubnetStatus> statuses = orchestrator.getAllSubnetStatus();
            
            System.out.println("   ┌─────────────┬────────┬────────┬─────────┬────────────┬───────────┐");
            System.out.println("   │ Subnet      │ VXLAN  │ Bridge │ Gateway │ Containers │ IPs Used  │");
            System.out.println("   ├─────────────┼────────┼────────┼─────────┼────────────┼───────────┤");
            
            for (Map.Entry<String, VXLANOrchestrator.SubnetStatus> entry : statuses.entrySet()) {
                VXLANOrchestrator.SubnetStatus status = entry.getValue();
                System.out.printf("   │ %-11s │ %-6s │ %-6s │ %-7s │ %-10d │ %-9d │\n",
                    status.name,
                    status.vxlanExists ? "✓" : "✗",
                    status.bridgeExists ? "✓" : "✗",
                    status.gatewayConfigured ? "✓" : "✗",
                    status.containerCount,
                    status.allocatedIPs
                );
            }
            
            System.out.println("   └─────────────┴────────┴────────┴─────────┴────────────┴───────────┘");
            
        } catch (Exception e) {
            System.err.println("   Failed to get subnet status: " + e.getMessage());
        }
    }
    
    private static void configureContainer(VXLANOrchestrator orchestrator, String containerName) {
        try {
            orchestrator.configureContainer(containerName);
            System.out.println("   ✓ " + containerName + " configured");
        } catch (Exception e) {
            System.out.println("   ✗ " + containerName + " failed: " + e.getMessage());
        }
    }
    
    private static void assignSpecificIP(VXLANOrchestrator orchestrator, 
                                         String container, String subnet, String ip) {
        try {
            orchestrator.assignIPToContainer(container, subnet, ip);
            System.out.println("   ✓ " + container + " assigned IP " + ip + " in subnet " + subnet);
        } catch (Exception e) {
            System.out.println("   ✗ Failed to assign IP: " + e.getMessage());
        }
    }
    
    private static void moveContainer(VXLANOrchestrator orchestrator,
                                      String container, String targetSubnet, String newIP) {
        try {
            orchestrator.moveContainerToSubnet(container, targetSubnet, newIP);
            System.out.println("   ✓ " + container + " moved to subnet " + targetSubnet);
        } catch (Exception e) {
            System.out.println("   ✗ Failed to move container: " + e.getMessage());
        }
    }
    
    private static void interactiveMode(VXLANOrchestrator orchestrator) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;
        
        System.out.println("\nAvailable commands:");
        System.out.println("  setup <subnet>         - Setup a specific subnet");
        System.out.println("  assign <container> <subnet> <ip> - Assign IP to container");
        System.out.println("  move <container> <subnet> - Move container to subnet");
        System.out.println("  config <container>     - Configure container network");
        System.out.println("  status                 - Show subnet status");
        System.out.println("  quit                   - Exit");
        
        while (running) {
            System.out.print("\n> ");
            String input = scanner.nextLine().trim();
            String[] parts = input.split("\\s+");
            
            if (parts.length == 0) continue;
            
            try {
                switch (parts[0].toLowerCase()) {
                    case "setup":
                        if (parts.length >= 2) {
                            orchestrator.setupSubnet(parts[1]);
                            System.out.println("Subnet " + parts[1] + " setup completed");
                        } else {
                            System.out.println("Usage: setup <subnet>");
                        }
                        break;
                        
                    case "assign":
                        if (parts.length >= 4) {
                            orchestrator.assignIPToContainer(parts[1], parts[2], parts[3]);
                            System.out.println("IP assigned successfully");
                        } else {
                            System.out.println("Usage: assign <container> <subnet> <ip>");
                        }
                        break;
                        
                    case "move":
                        if (parts.length >= 3) {
                            orchestrator.moveContainerToSubnet(parts[1], parts[2], null);
                            System.out.println("Container moved successfully");
                        } else {
                            System.out.println("Usage: move <container> <subnet>");
                        }
                        break;
                        
                    case "config":
                        if (parts.length >= 2) {
                            orchestrator.configureContainer(parts[1]);
                            System.out.println("Container configured successfully");
                        } else {
                            System.out.println("Usage: config <container>");
                        }
                        break;
                        
                    case "status":
                        displaySubnetStatus(orchestrator);
                        break;
                        
                    case "quit":
                    case "exit":
                        running = false;
                        break;
                        
                    default:
                        System.out.println("Unknown command: " + parts[0]);
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
        
        scanner.close();
    }
}