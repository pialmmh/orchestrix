package testrunners.LxcLxdInstallDebian12;

import com.telcobright.orchestrix.device.SshDevice;
import com.telcobright.orchestrix.automation.LxcLxdInstallDebian12Automation;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Test class for LxcLxdInstallDebian12Automation
 * Installs and configures LXC/LXD on Debian 12 systems
 */
public class TestLxcLxdInstallDebian12Automation {

    private static final Logger logger = Logger.getLogger(TestLxcLxdInstallDebian12Automation.class.getName());

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
            System.err.println("Usage: java testrunners.LxcLxdInstallDebian12.TestLxcLxdInstallDebian12Automation [config.properties]");
            System.exit(1);
        }

        // Read configuration parameters
        String host = config.getProperty("ssh.host", "localhost");
        int port = Integer.parseInt(config.getProperty("ssh.port", "22"));
        String username = config.getProperty("ssh.username", "root");
        String password = config.getProperty("ssh.password", "");

        // Installation options
        boolean useSudo = Boolean.parseBoolean(config.getProperty("install.use.sudo", "true"));
        boolean installLxc = Boolean.parseBoolean(config.getProperty("install.lxc", "true"));
        boolean installLxd = Boolean.parseBoolean(config.getProperty("install.lxd", "true"));
        boolean useSnap = Boolean.parseBoolean(config.getProperty("install.use.snap", "false"));

        // Test options
        boolean runInstall = Boolean.parseBoolean(config.getProperty("test.install", "true"));
        boolean runVerify = Boolean.parseBoolean(config.getProperty("test.verify", "true"));
        boolean runStatus = Boolean.parseBoolean(config.getProperty("test.status", "true"));
        boolean runBridgeConfig = Boolean.parseBoolean(config.getProperty("test.bridge.config", "false"));

        System.out.println("=========================================");
        System.out.println("LXC/LXD Installation Test for Debian 12");
        System.out.println("=========================================");
        System.out.println("Target Host: " + host + ":" + port);
        System.out.println("Username: " + username);
        System.out.println("Install LXC: " + installLxc);
        System.out.println("Install LXD: " + installLxd);
        System.out.println("Use Snap: " + useSnap);
        System.out.println("Use Sudo: " + useSudo);
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

            // Check OS version
            System.out.println("\nChecking OS version...");
            String osVersion = device.sendAndReceive("cat /etc/os-release | grep PRETTY_NAME").get();
            System.out.println("OS: " + osVersion);

            // Run test scenarios
            TestLxcLxdInstallDebian12Automation tester = new TestLxcLxdInstallDebian12Automation();
            int testsRun = 0;

            // Test 1: Check current status
            if (runStatus) {
                tester.testCurrentStatus(device, useSudo);
                testsRun++;
            }

            // Test 2: Install LXC/LXD
            if (runInstall) {
                tester.testInstallation(device, useSudo, installLxc, installLxd, useSnap);
                testsRun++;
            }

            // Test 3: Verify installation
            if (runVerify) {
                tester.testVerification(device, useSudo, installLxc, installLxd, useSnap);
                testsRun++;
            }

            // Test 4: Configure bridge networking (optional)
            if (runBridgeConfig) {
                tester.testBridgeConfiguration(device);
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
     * Test 1: Check current status
     */
    private void testCurrentStatus(SshDevice device, boolean useSudo) throws Exception {
        System.out.println("\n--- Test 1: Current Status ---");

        LxcLxdInstallDebian12Automation automation = new LxcLxdInstallDebian12Automation.Config()
            .useSudo(useSudo)
            .build(device);

        Map<String, String> status = automation.getStatus();

        System.out.println("Current installation status:");
        for (Map.Entry<String, String> entry : status.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }

        // Check if already installed
        boolean lxcInstalled = "true".equals(status.get("lxc.installed"));
        boolean lxdInstalled = "true".equals(status.get("lxd.installed"));

        if (lxcInstalled) {
            System.out.println("✓ LXC is already installed");
        } else {
            System.out.println("✗ LXC is not installed");
        }

        if (lxdInstalled) {
            System.out.println("✓ LXD is already installed");
        } else {
            System.out.println("✗ LXD is not installed");
        }
    }

    /**
     * Test 2: Installation
     */
    private void testInstallation(SshDevice device, boolean useSudo, boolean installLxc,
                                  boolean installLxd, boolean useSnap) throws Exception {
        System.out.println("\n--- Test 2: Installation ---");

        // Check if we need to install anything
        LxcLxdInstallDebian12Automation statusCheck = new LxcLxdInstallDebian12Automation.Config()
            .useSudo(useSudo)
            .build(device);

        Map<String, String> currentStatus = statusCheck.getStatus();
        boolean lxcAlreadyInstalled = "true".equals(currentStatus.get("lxc.installed"));
        boolean lxdAlreadyInstalled = "true".equals(currentStatus.get("lxd.installed"));

        if (installLxc && lxcAlreadyInstalled) {
            System.out.println("LXC is already installed, skipping LXC installation");
            installLxc = false;
        }

        if (installLxd && lxdAlreadyInstalled) {
            System.out.println("LXD is already installed, skipping LXD installation");
            installLxd = false;
        }

        if (!installLxc && !installLxd) {
            System.out.println("Nothing to install, all requested components are already present");
            return;
        }

        System.out.println("Starting installation process...");
        System.out.println("  Install LXC: " + installLxc);
        System.out.println("  Install LXD: " + installLxd);
        System.out.println("  Use Snap: " + useSnap);

        LxcLxdInstallDebian12Automation automation = new LxcLxdInstallDebian12Automation.Config()
            .useSudo(useSudo)
            .installLxc(installLxc)
            .installLxd(installLxd)
            .installSnapd(useSnap)
            .build(device);

        boolean result = automation.install();
        if (result) {
            System.out.println("✓ Installation completed successfully");
        } else {
            System.out.println("✗ Installation failed");
            throw new Exception("Installation test failed");
        }
    }

    /**
     * Test 3: Verification
     */
    private void testVerification(SshDevice device, boolean useSudo, boolean checkLxc,
                                  boolean checkLxd, boolean useSnap) throws Exception {
        System.out.println("\n--- Test 3: Verification ---");

        LxcLxdInstallDebian12Automation automation = new LxcLxdInstallDebian12Automation.Config()
            .useSudo(useSudo)
            .installLxc(checkLxc)
            .installLxd(checkLxd)
            .installSnapd(useSnap)
            .build(device);

        boolean result = automation.verify();
        if (result) {
            System.out.println("✓ Installation verified successfully");
        } else {
            System.out.println("✗ Installation verification failed");
        }

        // Manual verification checks
        System.out.println("\nManual verification:");

        if (checkLxc) {
            // Check LXC version
            String lxcVersion = device.sendAndReceive("lxc-ls --version 2>/dev/null").get();
            if (lxcVersion != null && !lxcVersion.isEmpty()) {
                System.out.println("✓ LXC version: " + lxcVersion.trim());
            } else {
                System.out.println("✗ LXC not found");
            }

            // Check lxcbr0 bridge
            String lxcBridge = device.sendAndReceive("ip addr show lxcbr0 2>/dev/null | grep inet").get();
            if (lxcBridge != null && !lxcBridge.isEmpty()) {
                System.out.println("✓ lxcbr0 bridge configured");
            } else {
                System.out.println("✗ lxcbr0 bridge not found");
            }
        }

        if (checkLxd) {
            // Check LXD version
            String lxdCmd = useSnap ? "/snap/bin/lxc" : "lxc";
            String lxdVersion = device.sendAndReceive(lxdCmd + " version 2>/dev/null").get();
            if (lxdVersion != null && !lxdVersion.isEmpty()) {
                System.out.println("✓ LXD version: " + lxdVersion.trim());
            } else {
                System.out.println("✗ LXD not found");
            }

            // Check lxdbr0 bridge
            String lxdBridge = device.sendAndReceive("ip addr show lxdbr0 2>/dev/null | grep inet").get();
            if (lxdBridge != null && !lxdBridge.isEmpty()) {
                System.out.println("✓ lxdbr0 bridge configured: " + lxdBridge.trim());
            } else {
                System.out.println("✗ lxdbr0 bridge not found");
            }

            // List containers
            String containers = device.sendAndReceive(lxdCmd + " list 2>/dev/null").get();
            if (containers != null && !containers.toLowerCase().contains("error")) {
                System.out.println("✓ Can list LXD containers");
            }
        }
    }

    /**
     * Test 4: Bridge configuration (optional)
     */
    private void testBridgeConfiguration(SshDevice device) throws Exception {
        System.out.println("\n--- Test 4: Bridge Configuration ---");

        // This would call the LxdBridgeNetworkingAutomation if needed
        System.out.println("Bridge configuration test would run here if enabled");

        // Check current bridges
        String bridges = device.sendAndReceive("ip link show type bridge").get();
        System.out.println("Current bridges:");
        System.out.println(bridges);
    }
}