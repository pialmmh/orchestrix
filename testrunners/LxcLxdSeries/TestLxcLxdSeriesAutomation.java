package testrunners.LxcLxdSeries;

import com.telcobright.orchestrix.device.TerminalDevice;
import com.telcobright.orchestrix.device.SshDevice;
import com.telcobright.orchestrix.automation.TerminalSeriesAutomationRunner;
import com.telcobright.orchestrix.automation.TerminalRunner;
import com.telcobright.orchestrix.automation.runners.LxcLxdInstallTerminalRunner;
import com.telcobright.orchestrix.automation.runners.LxdBridgeNetworkingTerminalRunner;
import com.telcobright.orchestrix.automation.runners.LinuxIpForwardEnablerTerminalRunner;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Test class demonstrating TerminalSeriesAutomationRunner with multiple runners
 * This runs LXC/LXD installation followed by bridge networking configuration
 * using a single terminal connection for improved performance
 */
public class TestLxcLxdSeriesAutomation {

    private static final Logger logger = Logger.getLogger(TestLxcLxdSeriesAutomation.class.getName());

    public static void main(String[] args) {
        // Load configuration from file
        String configFile = "config.properties";
        if (args.length > 0) {
            configFile = args[0];
        }

        Properties props = new Properties();
        try {
            props.load(new FileInputStream(configFile));
        } catch (IOException e) {
            System.err.println("Error loading configuration from " + configFile + ": " + e.getMessage());
            System.err.println("Usage: java testrunners.LxcLxdSeries.TestLxcLxdSeriesAutomation [config.properties]");
            System.exit(1);
        }

        // Read SSH connection parameters
        String host = props.getProperty("ssh.host", "localhost");
        int port = Integer.parseInt(props.getProperty("ssh.port", "22"));
        String username = props.getProperty("ssh.username", "root");
        String password = props.getProperty("ssh.password", "");

        // Read series configuration
        boolean stopOnFailure = Boolean.parseBoolean(props.getProperty("series.stop.on.failure", "true"));

        // Read which runners to execute
        boolean runInstall = Boolean.parseBoolean(props.getProperty("run.install", "false"));
        boolean runBridgeConfig = Boolean.parseBoolean(props.getProperty("run.bridge.config", "false"));
        boolean runIpForward = Boolean.parseBoolean(props.getProperty("run.ipforward", "false"));

        System.out.println("=========================================");
        System.out.println("LXC/LXD Series Automation Test");
        System.out.println("=========================================");
        System.out.println("Target Host: " + host + ":" + port);
        System.out.println("Username: " + username);
        System.out.println("Stop on Failure: " + stopOnFailure);
        System.out.println("Run Install: " + runInstall);
        System.out.println("Run IP Forward: " + runIpForward);
        System.out.println("Run Bridge Config: " + runBridgeConfig);
        System.out.println("");

        try {
            // Create the series runner (device will be created based on type)
            TerminalSeriesAutomationRunner seriesRunner = new TerminalSeriesAutomationRunner();
            seriesRunner.setStopOnFailure(stopOnFailure);

            // Configure installation runner
            if (runInstall) {
                TerminalRunner installRunner = new LxcLxdInstallTerminalRunner();

                // Create config map for installation
                Map<String, String> installConfig = new HashMap<>();
                installConfig.put("install.use.sudo", props.getProperty("install.use.sudo", "true"));
                installConfig.put("install.lxc", props.getProperty("install.lxc", "true"));
                installConfig.put("install.lxd", props.getProperty("install.lxd", "true"));
                installConfig.put("install.use.snap", props.getProperty("install.use.snap", "false"));
                installConfig.put("force.execution", props.getProperty("install.force", "false"));

                seriesRunner.addRunner(installRunner, installConfig);
            }

            // Configure IP forwarding runner
            if (runIpForward) {
                TerminalRunner ipForwardRunner = new LinuxIpForwardEnablerTerminalRunner();

                // Create config map for IP forwarding
                Map<String, String> ipForwardConfig = new HashMap<>();
                ipForwardConfig.put("ipforward.use.sudo", props.getProperty("ipforward.use.sudo", "true"));
                ipForwardConfig.put("ipforward.enable.ipv4", props.getProperty("ipforward.enable.ipv4", "true"));
                ipForwardConfig.put("ipforward.enable.ipv6", props.getProperty("ipforward.enable.ipv6", "false"));
                ipForwardConfig.put("ipforward.make.persistent", props.getProperty("ipforward.make.persistent", "true"));
                ipForwardConfig.put("ipforward.distribution", props.getProperty("ipforward.distribution", "AUTO_DETECT"));
                ipForwardConfig.put("force.execution", props.getProperty("ipforward.force", "false"));

                seriesRunner.addRunner(ipForwardRunner, ipForwardConfig);
            }

            // Configure bridge networking runner
            if (runBridgeConfig) {
                TerminalRunner bridgeRunner = new LxdBridgeNetworkingTerminalRunner();

                // Create config map for bridge
                Map<String, String> bridgeConfig = new HashMap<>();
                bridgeConfig.put("bridge.name", props.getProperty("bridge.name", "lxdbr0"));
                bridgeConfig.put("bridge.gateway.ip", props.getProperty("bridge.gateway.ip", "10.0.8.1"));
                bridgeConfig.put("bridge.network.cidr", props.getProperty("bridge.network.cidr", "10.0.8.0/24"));
                bridgeConfig.put("bridge.enable.nat", props.getProperty("bridge.enable.nat", "true"));
                bridgeConfig.put("bridge.enable.dns", props.getProperty("bridge.enable.dns", "true"));
                bridgeConfig.put("bridge.dhcp.start", props.getProperty("bridge.dhcp.start", "10.0.8.2"));
                bridgeConfig.put("bridge.dhcp.end", props.getProperty("bridge.dhcp.end", "10.0.8.254"));
                bridgeConfig.put("bridge.use.sudo", props.getProperty("bridge.use.sudo", "true"));
                bridgeConfig.put("delete.existing", props.getProperty("bridge.delete.existing", "false"));
                bridgeConfig.put("force.execution", props.getProperty("bridge.force", "false"));

                seriesRunner.addRunner(bridgeRunner, bridgeConfig);
            }

            // Check if any runners configured
            if (seriesRunner.getRunnerCount() == 0) {
                System.err.println("No runners configured! Enable at least one runner in the configuration.");
                System.err.println("Set run.install=true or run.bridge.config=true");
                System.exit(1);
            }

            // Execute the series with connection parameters
            // Determine terminal type from config (default to SSH)
            String terminalType = props.getProperty("terminal.type", "SSH");
            TerminalDevice.DeviceType deviceType = TerminalDevice.DeviceType.valueOf(terminalType.toUpperCase());

            System.out.println("Starting series automation with " + seriesRunner.getRunnerCount() + " runners");
            System.out.println("Terminal Type: " + deviceType);
            System.out.println("-----------------------------------------");

            boolean success = seriesRunner.executeWithConnection(deviceType, host, port, username, password);

            // Get execution results
            Map<String, Boolean> results = seriesRunner.getExecutionResults();

            System.out.println("\n=========================================");
            if (success) {
                System.out.println("✓ Series automation completed successfully");
            } else {
                System.out.println("✗ Series automation failed");
            }
            System.out.println("=========================================");

            // Print detailed results
            if (!results.isEmpty()) {
                System.out.println("\nDetailed Results:");
                for (Map.Entry<String, Boolean> entry : results.entrySet()) {
                    String status = entry.getValue() ? "✓ SUCCESS" : "✗ FAILED";
                    System.out.println("  " + status + " - " + entry.getKey());
                }
            }

            System.exit(success ? 0 : 1);

        } catch (Exception e) {
            System.err.println("Series automation failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}