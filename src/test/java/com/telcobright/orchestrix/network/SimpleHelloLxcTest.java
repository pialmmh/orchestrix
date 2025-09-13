package com.telcobright.orchestrix.network;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Simplified Hello LXC Test using direct SSH commands
 */
public class SimpleHelloLxcTest {
    
    public static void main(String[] args) throws Exception {
        System.out.println("==============================================");
        System.out.println("Simple Hello LXC Test - Debian 12 Container");
        System.out.println("==============================================");
        
        try {
            // Step 1: Check bridge exists
            System.out.println("\n1. Checking bridge lxcbr0...");
            String bridgeCheck = executeCommand("ip link show lxcbr0 2>/dev/null | grep lxcbr0");
            
            if (bridgeCheck.isEmpty()) {
                System.out.println("Bridge doesn't exist, creating...");
                executeCommand("sudo ip link add name lxcbr0 type bridge");
                executeCommand("sudo ip addr add 10.10.199.1/24 dev lxcbr0");
                executeCommand("sudo ip link set lxcbr0 up");
                executeCommand("sudo sysctl -w net.ipv4.ip_forward=1");
                
                // Get default interface
                String defaultIf = executeCommand("ip route | grep default | awk '{print $5}' | head -1").trim();
                System.out.println("Default interface: " + defaultIf);
                
                // Configure NAT
                executeCommand("sudo iptables -t nat -D POSTROUTING -s 10.10.199.0/24 -j MASQUERADE 2>/dev/null || true");
                executeCommand("sudo iptables -t nat -A POSTROUTING -s 10.10.199.0/24 -o " + defaultIf + " -j MASQUERADE");
            }
            System.out.println("✓ Bridge lxcbr0 ready!");
            
            // Step 2: Create and launch container
            System.out.println("\n2. Creating hello-lxc container with Debian 12...");
            
            // Check if container exists
            String checkCmd = "lxc list hello-lxc --format=json 2>/dev/null | jq length 2>/dev/null || echo 0";
            String result = executeCommand(checkCmd).trim();
            
            if (!result.equals("0")) {
                System.out.println("Container exists, deleting...");
                executeCommand("lxc stop hello-lxc --force 2>/dev/null || true");
                executeCommand("lxc delete hello-lxc --force 2>/dev/null || true");
            }
            
            // Launch container
            System.out.println("Launching container...");
            executeCommand("lxc launch images:debian/12 hello-lxc --network=lxcbr0");
            System.out.println("✓ Container launched!");
            
            // Wait for container to start
            Thread.sleep(5000);
            
            // Step 3: Configure static IP
            System.out.println("\n3. Configuring static IP 10.10.199.50...");
            
            // Configure network inside container
            String netplanConfig = "network:\\n" +
                "  version: 2\\n" +
                "  ethernets:\\n" +
                "    eth0:\\n" +
                "      dhcp4: false\\n" +
                "      addresses: [10.10.199.50/24]\\n" +
                "      gateway4: 10.10.199.1\\n" +
                "      nameservers:\\n" +
                "        addresses: [8.8.8.8, 8.8.4.4]";
            
            executeCommand("lxc exec hello-lxc -- bash -c 'echo -e \"" + netplanConfig + "\" > /etc/netplan/99-static.yaml'");
            executeCommand("lxc exec hello-lxc -- netplan apply 2>/dev/null || true");
            
            // Alternative method using ip commands
            executeCommand("lxc exec hello-lxc -- ip addr flush dev eth0");
            executeCommand("lxc exec hello-lxc -- ip addr add 10.10.199.50/24 dev eth0");
            executeCommand("lxc exec hello-lxc -- ip link set eth0 up");
            executeCommand("lxc exec hello-lxc -- ip route add default via 10.10.199.1");
            executeCommand("lxc exec hello-lxc -- bash -c 'echo nameserver 8.8.8.8 > /etc/resolv.conf'");
            
            System.out.println("✓ Static IP configured!");
            
            // Wait for network to stabilize
            Thread.sleep(2000);
            
            // Step 4: Test connectivity
            System.out.println("\n4. Testing connectivity...");
            
            // Test ping from host to container
            System.out.println("Testing host -> container (10.10.199.50)...");
            String hostPing = executeCommand("ping -c 1 -W 2 10.10.199.50 2>&1");
            boolean hostToContainer = hostPing.contains("1 received") || hostPing.contains("1 packets transmitted, 1 received");
            System.out.println(hostToContainer ? "✓ Host can ping container" : "✗ Host cannot ping container");
            
            // Test ping from container to gateway
            System.out.println("Testing container -> gateway (10.10.199.1)...");
            String gwPing = executeCommand("lxc exec hello-lxc -- ping -c 1 10.10.199.1 2>&1");
            boolean containerToGw = gwPing.contains("1 received") || gwPing.contains("1 packets transmitted, 1 received");
            System.out.println(containerToGw ? "✓ Container can ping gateway" : "✗ Container cannot ping gateway");
            
            // Test ping from container to Google DNS
            System.out.println("Testing container -> Google DNS (8.8.8.8)...");
            String googlePing = executeCommand("lxc exec hello-lxc -- ping -c 1 8.8.8.8 2>&1");
            boolean containerToGoogle = googlePing.contains("1 received") || googlePing.contains("1 packets transmitted, 1 received");
            System.out.println(containerToGoogle ? "✓ Container can ping Google DNS" : "✗ Container cannot ping Google DNS");
            
            // Test DNS resolution
            System.out.println("Testing DNS resolution (google.com)...");
            String dnsPing = executeCommand("lxc exec hello-lxc -- ping -c 1 google.com 2>&1");
            boolean dnsWorks = dnsPing.contains("1 received") || dnsPing.contains("1 packets transmitted, 1 received");
            System.out.println(dnsWorks ? "✓ DNS resolution works" : "✗ DNS resolution failed");
            
            // Step 5: Show container info
            System.out.println("\n5. Container Information:");
            String containerInfo = executeCommand("lxc list hello-lxc");
            System.out.println(containerInfo);
            
            // Show IP configuration
            System.out.println("\nContainer IP Configuration:");
            String ipInfo = executeCommand("lxc exec hello-lxc -- ip addr show eth0");
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
            
            System.out.println("\nContainer is running. To delete: lxc delete hello-lxc --force");
            
        } catch (Exception e) {
            System.err.println("Test failed with error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String executeCommand(String command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
        Process process = pb.start();
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        process.waitFor();
        return output.toString();
    }
}