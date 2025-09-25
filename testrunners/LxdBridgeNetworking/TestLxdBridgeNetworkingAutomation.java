package testrunners.LxdBridgeNetworking;

import com.telcobright.orchestrix.device.SshDevice;
import com.telcobright.orchestrix.automation.LxdBridgeNetworkingAutomation;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * Test class for LxdBridgeNetworkingAutomation
 *
 * Usage:
 *   java testrunners.TestLxdBridgeNetworkingAutomation <host> <port> <username> <password>
 *
 * Example:
 *   java testrunners.TestLxdBridgeNetworkingAutomation 192.168.1.100 22 ubuntu mypassword
 */
public class TestLxdBridgeNetworkingAutomation {

    private static final Logger logger = Logger.getLogger(TestLxdBridgeNetworkingAutomation.class.getName());

    public static void main(String[] args) {
        // Load configuration from file
        String configFile = "config.properties";
        if (args.length > 0) {
            configFile = args[0];
        }

        Properties config = new Properties();
        try {
            config.load(new FileInputStream(configFile));
        } catch (IOException e) {
            System.err.println("Error loading configuration from " + configFile + ": " + e.getMessage());
            System.err.println("Usage: java testrunners.LxdBridgeNetworking.TestLxdBridgeNetworkingAutomation [config.properties]");
            System.exit(1);
        }

        // Read configuration parameters
        String host = config.getProperty("ssh.host", "localhost");
        int port = Integer.parseInt(config.getProperty("ssh.port", "22"));
        String username = config.getProperty("ssh.username", "root");
        String password = config.getProperty("ssh.password", "");

        // LXD Bridge configuration
        String bridgeName = config.getProperty("bridge.name", "lxdbr0");
        String gatewayIp = config.getProperty("bridge.gateway", "10.10.199.1");

        // Test configuration
        boolean runDefaultTest = Boolean.parseBoolean(config.getProperty("test.default", "true"));
        boolean runCustomTest = Boolean.parseBoolean(config.getProperty("test.custom", "true"));
        boolean runUpdateTest = Boolean.parseBoolean(config.getProperty("test.update", "false"));
        boolean runVerificationTest = Boolean.parseBoolean(config.getProperty("test.verification", "true"));
        boolean runGetConfigTest = Boolean.parseBoolean(config.getProperty("test.getconfig", "true"));
        boolean runCleanupTest = Boolean.parseBoolean(config.getProperty("test.cleanup", "false"));

        System.out.println("=========================================");
        System.out.println("LXD Bridge Networking Automation Test");
        System.out.println("=========================================");
        System.out.println("Target Host: " + host + ":" + port);
        System.out.println("Username: " + username);
        System.out.println("Bridge Name: " + bridgeName);
        System.out.println("Gateway IP: " + gatewayIp);
        System.out.println("");

        try {
            // Create SSH connection
            System.out.println("Connecting to " + host + "...");
            SshDevice device = new SshDevice();
            boolean connected = device.connect(host, port, username, password).get();

            if (!connected) {
                System.err.println("Failed to connect to SSH device");
                System.exit(1);
            }

            // Test connection
            String testResult = device.sendAndReceive("echo 'Connection successful'").get();
            System.out.println("SSH Connection: " + testResult);

            // Run test scenarios based on configuration
            TestLxdBridgeNetworkingAutomation tester = new TestLxdBridgeNetworkingAutomation();
            int testsRun = 0;

            // Test 1: Default configuration
            if (runDefaultTest) {
                tester.testDefaultConfiguration(device);
                testsRun++;
            }

            // Test 2: Custom configuration
            if (runCustomTest) {
                tester.testCustomConfiguration(device, bridgeName, gatewayIp);
                testsRun++;
            }

            // Test 3: Update existing bridge
            if (runUpdateTest) {
                tester.testUpdateConfiguration(device, bridgeName);
                testsRun++;
            }

            // Test 4: Verification
            if (runVerificationTest) {
                tester.testVerification(device, bridgeName, gatewayIp);
                testsRun++;
            }

            // Test 5: Get current configuration
            if (runGetConfigTest) {
                tester.testGetConfiguration(device, bridgeName, gatewayIp);
                testsRun++;
            }

            // Optional: Test cleanup
            if (runCleanupTest) {
                tester.testCleanup(device, bridgeName, gatewayIp);
                testsRun++;
            }

            if (testsRun == 0) {
                System.out.println("\nNo tests were enabled in the configuration file.");
                System.out.println("Enable tests by setting test.* properties to true.");
            }

            System.out.println("\n=========================================");
            System.out.println("All tests completed successfully!");
            System.out.println("=========================================");

        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Test 1: Default configuration
     */
    private void testDefaultConfiguration(SshDevice device) throws Exception {
        System.out.println("\n--- Test 1: Default Configuration ---");

        LxdBridgeNetworkingAutomation automation = new LxdBridgeNetworkingAutomation.Config()
            .build(device);

        boolean result = automation.configure();
        if (result) {
            System.out.println("✓ Default configuration successful");
        } else {
            System.out.println("✗ Default configuration failed");
            throw new Exception("Default configuration test failed");
        }

        // Verify
        result = automation.verify();
        if (result) {
            System.out.println("✓ Default configuration verified");
        } else {
            System.out.println("✗ Default configuration verification failed");
        }
    }

    /**
     * Test 2: Custom configuration
     */
    private void testCustomConfiguration(SshDevice device, String bridgeName, String gatewayIp) throws Exception {
        System.out.println("\n--- Test 2: Custom Configuration ---");
        System.out.println("Bridge: " + bridgeName);
        System.out.println("Gateway: " + gatewayIp);

        // Extract network from gateway IP
        String[] ipParts = gatewayIp.split("\\.");
        String networkBase = ipParts[0] + "." + ipParts[1] + "." + ipParts[2];
        String networkCidr = networkBase + ".0/24";
        String dhcpStart = networkBase + ".50";
        String dhcpEnd = networkBase + ".254";

        LxdBridgeNetworkingAutomation automation = new LxdBridgeNetworkingAutomation.Config()
            .bridgeName(bridgeName)
            .hostGatewayIp(gatewayIp)
            .networkCidr(networkCidr)
            .dhcpRange(dhcpStart, dhcpEnd)
            .enableNat(true)
            .enableDns(true)
            .build(device);

        boolean result = automation.configure();
        if (result) {
            System.out.println("✓ Custom configuration successful");
        } else {
            System.out.println("✗ Custom configuration failed");
            throw new Exception("Custom configuration test failed");
        }

        // Verify
        result = automation.verify();
        if (result) {
            System.out.println("✓ Custom configuration verified");
        } else {
            System.out.println("✗ Custom configuration verification failed");
        }
    }

    /**
     * Test 3: Update existing configuration
     */
    private void testUpdateConfiguration(SshDevice device, String bridgeName) throws Exception {
        System.out.println("\n--- Test 3: Update Configuration ---");

        // Update with different gateway
        String newGatewayIp = "10.10.200.1";
        String newNetworkCidr = "10.10.200.0/24";

        LxdBridgeNetworkingAutomation automation = new LxdBridgeNetworkingAutomation.Config()
            .bridgeName(bridgeName)
            .hostGatewayIp(newGatewayIp)
            .networkCidr(newNetworkCidr)
            .dhcpRange("10.10.200.50", "10.10.200.254")
            .build(device);

        boolean result = automation.configure();
        if (result) {
            System.out.println("✓ Configuration update successful");
            System.out.println("  New gateway: " + newGatewayIp);
        } else {
            System.out.println("✗ Configuration update failed");
        }

        // Change back to original
        String originalGateway = "10.10.199.1";
        automation = new LxdBridgeNetworkingAutomation.Config()
            .bridgeName(bridgeName)
            .hostGatewayIp(originalGateway)
            .networkCidr("10.10.199.0/24")
            .dhcpRange("10.10.199.50", "10.10.199.254")
            .build(device);

        automation.configure();
        System.out.println("  Reverted to original: " + originalGateway);
    }

    /**
     * Test 4: Verification
     */
    private void testVerification(SshDevice device, String bridgeName, String gatewayIp) throws Exception {
        System.out.println("\n--- Test 4: Verification ---");

        String networkBase = gatewayIp.substring(0, gatewayIp.lastIndexOf('.'));
        String networkCidr = networkBase + ".0/24";

        LxdBridgeNetworkingAutomation automation = new LxdBridgeNetworkingAutomation.Config()
            .bridgeName(bridgeName)
            .hostGatewayIp(gatewayIp)
            .networkCidr(networkCidr)
            .build(device);

        boolean result = automation.verify();
        if (result) {
            System.out.println("✓ Configuration verification passed");
        } else {
            System.out.println("✗ Configuration verification failed");
        }

        // Manual checks
        System.out.println("\nManual verification:");

        // Check bridge exists
        String bridgeList = device.sendAndReceive("lxc network list").get();
        if (bridgeList.contains(bridgeName)) {
            System.out.println("✓ Bridge " + bridgeName + " exists");
        } else {
            System.out.println("✗ Bridge " + bridgeName + " not found");
        }

        // Check IP forwarding
        String ipForward = device.sendAndReceive("sysctl net.ipv4.ip_forward").get();
        if (ipForward.contains("= 1")) {
            System.out.println("✓ IP forwarding enabled");
        } else {
            System.out.println("✗ IP forwarding not enabled");
        }

        // Check iptables NAT rules
        String natRules = device.sendAndReceive("iptables -t nat -L POSTROUTING -n").get();
        if (natRules.contains(networkCidr)) {
            System.out.println("✓ NAT rules configured for " + networkCidr);
        } else {
            System.out.println("✗ NAT rules not found for " + networkCidr);
        }
    }

    /**
     * Test 5: Get current configuration
     */
    private void testGetConfiguration(SshDevice device, String bridgeName, String gatewayIp) throws Exception {
        System.out.println("\n--- Test 5: Get Current Configuration ---");

        String networkBase = gatewayIp.substring(0, gatewayIp.lastIndexOf('.'));
        String networkCidr = networkBase + ".0/24";

        LxdBridgeNetworkingAutomation automation = new LxdBridgeNetworkingAutomation.Config()
            .bridgeName(bridgeName)
            .hostGatewayIp(gatewayIp)
            .networkCidr(networkCidr)
            .build(device);

        Map<String, String> config = automation.getCurrentConfiguration();

        System.out.println("Current configuration for " + bridgeName + ":");
        for (Map.Entry<String, String> entry : config.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }

        // Show full network details
        System.out.println("\nFull network details:");
        String networkShow = device.sendAndReceive("lxc network show " + bridgeName).get();
        System.out.println(networkShow);
    }

    /**
     * Test cleanup (optional)
     */
    private void testCleanup(SshDevice device, String bridgeName, String gatewayIp) throws Exception {
        System.out.println("\n--- Test 6: Cleanup ---");
        System.out.println("Removing bridge " + bridgeName + "...");

        // No interactive prompt when running from config
        String networkBase = gatewayIp.substring(0, gatewayIp.lastIndexOf('.'));
        String networkCidr = networkBase + ".0/24";

        LxdBridgeNetworkingAutomation automation = new LxdBridgeNetworkingAutomation.Config()
            .bridgeName(bridgeName)
            .hostGatewayIp(gatewayIp)
            .networkCidr(networkCidr)
            .build(device);

        boolean result = automation.remove();
        if (result) {
            System.out.println("✓ Bridge " + bridgeName + " removed successfully");
        } else {
            System.out.println("✗ Failed to remove bridge " + bridgeName);
        }
    }

    /**
     * Interactive test mode
     */
    public static void interactiveTest(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        System.out.println("\n=== LXD Bridge Networking Interactive Test ===\n");

        // Get connection details
        System.out.print("SSH Host: ");
        String host = scanner.nextLine();

        System.out.print("SSH Port [22]: ");
        String portStr = scanner.nextLine();
        int port = portStr.isEmpty() ? 22 : Integer.parseInt(portStr);

        System.out.print("Username: ");
        String username = scanner.nextLine();

        System.out.print("Password: ");
        String password = scanner.nextLine();

        // Get bridge configuration
        System.out.print("Bridge Name [lxdbr0]: ");
        String bridgeName = scanner.nextLine();
        if (bridgeName.isEmpty()) bridgeName = "lxdbr0";

        System.out.print("Gateway IP [10.10.199.1]: ");
        String gatewayIp = scanner.nextLine();
        if (gatewayIp.isEmpty()) gatewayIp = "10.10.199.1";

        System.out.print("Enable NAT [y/n]: ");
        boolean enableNat = !"n".equalsIgnoreCase(scanner.nextLine());

        System.out.print("Enable DNS [y/n]: ");
        boolean enableDns = !"n".equalsIgnoreCase(scanner.nextLine());

        // Run configuration
        System.out.println("\nConfiguring LXD bridge...");

        SshDevice device = new SshDevice();
        device.connect(host, port, username, password).get();

        String networkBase = gatewayIp.substring(0, gatewayIp.lastIndexOf('.'));
        String networkCidr = networkBase + ".0/24";

        LxdBridgeNetworkingAutomation automation = new LxdBridgeNetworkingAutomation.Config()
            .bridgeName(bridgeName)
            .hostGatewayIp(gatewayIp)
            .networkCidr(networkCidr)
            .dhcpRange(networkBase + ".50", networkBase + ".254")
            .enableNat(enableNat)
            .enableDns(enableDns)
            .build(device);

        if (automation.configure()) {
            System.out.println("\n✓ Configuration successful!");

            if (automation.verify()) {
                System.out.println("✓ Configuration verified!");
            }
        } else {
            System.out.println("\n✗ Configuration failed!");
        }
    }
}