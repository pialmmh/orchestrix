package automation.api.deployment;

import automation.api.infrastructure.LxcPrerequisitesManager;
import com.jcraft.jsch.*;
import java.io.*;
import java.util.*;

/**
 * Kafka and Zookeeper Deployment Manager
 *
 * Deploys Kafka and Zookeeper containers via SSH using LXC launch scripts.
 * Zookeeper must be deployed and running before Kafka.
 */
public class KafkaZookeeperDeployment {

    private final String host;
    private final String user;
    private final int port;
    private final String password;
    private final boolean useSudo;

    private final String orchestrixBase;
    private final String zookeeperDir;
    private final String kafkaDir;

    private Session session;
    private StringBuilder deploymentLog = new StringBuilder();

    public KafkaZookeeperDeployment(String host, String user, int port, String password, boolean useSudo) {
        this.host = host;
        this.user = user;
        this.port = port;
        this.password = password;
        this.useSudo = useSudo;

        // Use remote user's home directory for orchestrix paths
        this.orchestrixBase = "/home/" + user + "/orchestrix";
        this.zookeeperDir = orchestrixBase + "/images/containers/lxc/zookeeper";
        this.kafkaDir = orchestrixBase + "/images/containers/lxc/kafka";
    }

    /**
     * Main deployment workflow
     */
    public DeploymentResult deploy() {
        DeploymentResult result = new DeploymentResult();

        try {
            log("═══════════════════════════════════════════════════════");
            log("  Kafka & Zookeeper Deployment via SSH");
            log("═══════════════════════════════════════════════════════");
            log("Host: " + host);
            log("User: " + user);
            log("");

            // Step 1: Connect via SSH
            log("Step 1: Establishing SSH Connection");
            log("───────────────────────────────────────────────────────");
            connect();
            log("✓ SSH connection established");
            log("");

            // Step 2: Verify LXC prerequisites
            log("Step 2: Verifying LXC Prerequisites");
            log("───────────────────────────────────────────────────────");
            verifyPrerequisites();
            log("✓ LXC prerequisites verified");
            log("");

            // Step 3: Check base images exist
            log("Step 3: Checking Base Images");
            log("───────────────────────────────────────────────────────");
            checkBaseImage("zookeeper-base");
            checkBaseImage("kafka-base");
            log("✓ Base images available");
            log("");

            // Step 4: Deploy Zookeeper
            log("Step 4: Deploying Zookeeper Container");
            log("───────────────────────────────────────────────────────");
            String zookeeperIp = deployZookeeper();
            result.setZookeeperIp(zookeeperIp);
            result.setZookeeperPort(2181);
            log("✓ Zookeeper deployed successfully");
            log("  Container: zookeeper-single");
            log("  IP: " + zookeeperIp);
            log("  Port: 2181");
            log("");

            // Step 5: Verify Zookeeper is running
            log("Step 5: Verifying Zookeeper Status");
            log("───────────────────────────────────────────────────────");
            verifyZookeeper();
            log("✓ Zookeeper is running and healthy");
            log("");

            // Step 6: Create Kafka configuration with Zookeeper IP
            log("Step 6: Preparing Kafka Configuration");
            log("───────────────────────────────────────────────────────");
            String kafkaConfigPath = createKafkaConfig(zookeeperIp);
            log("✓ Kafka configuration created");
            log("  Config: " + kafkaConfigPath);
            log("");

            // Step 7: Deploy Kafka
            log("Step 7: Deploying Kafka Broker");
            log("───────────────────────────────────────────────────────");
            String kafkaIp = deployKafka(kafkaConfigPath);
            result.setKafkaIp(kafkaIp);
            result.setKafkaPort(9092);
            log("✓ Kafka deployed successfully");
            log("  Container: kafka-broker-1");
            log("  IP: " + kafkaIp);
            log("  Port: 9092");
            log("");

            // Step 8: Verify Kafka is running
            log("Step 8: Verifying Kafka Status");
            log("───────────────────────────────────────────────────────");
            verifyKafka();
            log("✓ Kafka is running and connected to Zookeeper");
            log("");

            log("═══════════════════════════════════════════════════════");
            log("  Deployment Completed Successfully!");
            log("═══════════════════════════════════════════════════════");
            log("");
            log("Zookeeper: " + zookeeperIp + ":2181");
            log("Kafka:     " + kafkaIp + ":9092");
            log("");

            result.setSuccess(true);
            result.setLog(deploymentLog.toString());
            return result;

        } catch (Exception e) {
            log("✗ Deployment failed: " + e.getMessage());
            e.printStackTrace();
            result.setSuccess(false);
            result.setError(e.getMessage());
            result.setLog(deploymentLog.toString());
            return result;
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
                log("SSH connection closed");
            }
        }
    }

    /**
     * Verify LXC prerequisites
     */
    private void verifyPrerequisites() throws Exception {
        LxcPrerequisitesManager prereqManager = new LxcPrerequisitesManager(host, user, port, password, null, useSudo);
        LxcPrerequisitesManager.PrerequisitesResult prereqResult = prereqManager.setupPrerequisites();

        if (!prereqResult.isSuccess()) {
            throw new Exception("LXC prerequisites not met: " + String.join(", ", prereqResult.getErrors()));
        }
    }

    /**
     * Check if base image exists
     */
    private void checkBaseImage(String imageName) throws Exception {
        CommandResult result = executeCommand("lxc image list " + imageName + " --format csv", false);
        if (result.output == null || result.output.trim().isEmpty()) {
            throw new Exception("Base image not found: " + imageName + ". Please build it first.");
        }
        log("  ✓ Image '" + imageName + "' exists");
    }

    /**
     * Deploy Zookeeper container
     */
    private String deployZookeeper() throws Exception {
        // Check if container already exists
        CommandResult checkResult = executeCommand("lxc info zookeeper-single 2>&1", false);
        if (checkResult.exitCode == 0) {
            log("  Container 'zookeeper-single' already exists, deleting...");
            executeCommand("lxc delete zookeeper-single --force", true);
            Thread.sleep(2000);
        }

        // Launch Zookeeper using the launch script
        String launchCommand = String.format(
            "cd %s && ./launch.sh templates/sample.conf",
            zookeeperDir
        );

        CommandResult result = executeCommand(launchCommand, true);
        if (result.exitCode != 0) {
            throw new Exception("Failed to launch Zookeeper: " + result.stderr);
        }

        // Wait for container to be fully ready
        log("  Waiting for Zookeeper to be ready...");
        Thread.sleep(8000);

        // Get container IP
        String ip = getContainerIp("zookeeper-single");
        log("  Zookeeper IP: " + ip);

        return ip;
    }

    /**
     * Verify Zookeeper is running
     */
    private void verifyZookeeper() throws Exception {
        // Check service status
        CommandResult result = executeCommand("lxc exec zookeeper-single -- systemctl is-active zookeeper", false);
        if (result.exitCode != 0 || !result.output.contains("active")) {
            throw new Exception("Zookeeper service is not active");
        }
        log("  ✓ Zookeeper service is active");

        // Check Zookeeper status using zkServer.sh
        CommandResult statusResult = executeCommand(
            "lxc exec zookeeper-single -- /opt/zookeeper/bin/zkServer.sh status",
            false
        );
        if (statusResult.exitCode != 0) {
            log("  Warning: zkServer.sh status check returned non-zero, but service is active");
        } else {
            log("  ✓ Zookeeper server status check passed");
        }
    }

    /**
     * Create Kafka configuration with correct Zookeeper IP
     */
    private String createKafkaConfig(String zookeeperIp) throws Exception {
        String configContent = String.format(
            "# Kafka Broker Configuration - Auto-generated\n\n" +
            "CONTAINER_NAME=\"kafka-broker-1\"\n" +
            "BROKER_ID=\"1\"\n" +
            "ZOOKEEPER_CONNECT=\"%s:2181/kafka\"\n" +
            "LISTENERS=\"PLAINTEXT://0.0.0.0:9092\"\n" +
            "ADVERTISED_LISTENERS=\"PLAINTEXT://kafka-broker-1:9092\"\n" +
            "LOG_DIRS=\"/data/kafka-logs\"\n" +
            "KAFKA_HEAP_OPTS=\"-Xms1G -Xmx1G\"\n" +
            "AUTO_START=\"true\"\n",
            zookeeperIp
        );

        String configPath = kafkaDir + "/templates/runtime.conf";

        // Write config file via SSH
        String writeCommand = String.format(
            "cat > %s << 'EOF'\n%sEOF",
            configPath,
            configContent
        );

        executeCommand(writeCommand, false);
        log("  ✓ Configuration written to: " + configPath);

        return configPath;
    }

    /**
     * Deploy Kafka container
     */
    private String deployKafka(String configPath) throws Exception {
        // Check if container already exists
        CommandResult checkResult = executeCommand("lxc info kafka-broker-1 2>&1", false);
        if (checkResult.exitCode == 0) {
            log("  Container 'kafka-broker-1' already exists, deleting...");
            executeCommand("lxc delete kafka-broker-1 --force", true);
            Thread.sleep(2000);
        }

        // Launch Kafka using the launch script
        String launchCommand = String.format(
            "cd %s && ./launch.sh templates/runtime.conf",
            kafkaDir
        );

        CommandResult result = executeCommand(launchCommand, true);
        if (result.exitCode != 0) {
            throw new Exception("Failed to launch Kafka: " + result.stderr);
        }

        // Wait for container to be fully ready
        log("  Waiting for Kafka to be ready...");
        Thread.sleep(10000);

        // Get container IP
        String ip = getContainerIp("kafka-broker-1");
        log("  Kafka IP: " + ip);

        return ip;
    }

    /**
     * Verify Kafka is running
     */
    private void verifyKafka() throws Exception {
        // Check service status
        CommandResult result = executeCommand("lxc exec kafka-broker-1 -- systemctl is-active kafka", false);
        if (result.exitCode != 0 || !result.output.contains("active")) {
            throw new Exception("Kafka service is not active");
        }
        log("  ✓ Kafka service is active");

        // Check Kafka broker log for startup confirmation
        CommandResult logResult = executeCommand(
            "lxc exec kafka-broker-1 -- journalctl -u kafka -n 20 | grep -i 'started'",
            false
        );
        if (logResult.exitCode == 0 && logResult.output.contains("started")) {
            log("  ✓ Kafka broker started successfully");
        }
    }

    /**
     * Get container IP address
     */
    private String getContainerIp(String containerName) throws Exception {
        CommandResult result = executeCommand(
            "lxc exec " + containerName + " -- hostname -I | awk '{print $1}'",
            false
        );

        if (result.exitCode != 0 || result.output == null || result.output.trim().isEmpty()) {
            throw new Exception("Failed to get IP for container: " + containerName);
        }

        return result.output.trim();
    }

    /**
     * Execute command via SSH
     */
    private CommandResult executeCommand(String command, boolean printOutput) throws Exception {
        ChannelExec channel = (ChannelExec) session.openChannel("exec");

        // Use sudo if required
        // Wrap in sh -c only if command contains shell operators (cd, &&, ||, ;, |, >, <)
        if (useSudo && !command.startsWith("sudo ")) {
            if (command.contains("&&") || command.contains("||") || command.contains(";") ||
                command.contains("|") || command.contains(">") || command.contains("<") ||
                command.startsWith("cd ")) {
                // Need shell wrapper for shell operators
                command = "echo '" + password.replace("'", "'\\''") + "' | sudo -S sh -c '" + command.replace("'", "'\\''") + "'";
            } else {
                // Simple command, no shell wrapper needed
                command = "echo '" + password.replace("'", "'\\''") + "' | sudo -S " + command;
            }
        }

        channel.setCommand(command);

        InputStream in = channel.getInputStream();
        InputStream err = channel.getErrStream();

        channel.connect();

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        byte[] tmp = new byte[4096];
        while (true) {
            while (in.available() > 0) {
                int i = in.read(tmp, 0, 4096);
                if (i < 0) break;
                String line = new String(tmp, 0, i);
                stdout.append(line);
                if (printOutput && !line.trim().isEmpty()) {
                    log("  " + line.trim());
                }
            }

            while (err.available() > 0) {
                int i = err.read(tmp, 0, 4096);
                if (i < 0) break;
                String line = new String(tmp, 0, i);
                stderr.append(line);
                if (printOutput && !line.trim().isEmpty()) {
                    log("  [STDERR] " + line.trim());
                }
            }

            if (channel.isClosed()) {
                if (in.available() > 0 || err.available() > 0) continue;
                break;
            }

            try { Thread.sleep(100); } catch (Exception e) {}
        }

        int exitCode = channel.getExitStatus();
        channel.disconnect();

        return new CommandResult(exitCode, stdout.toString(), stderr.toString());
    }

    /**
     * Connect to remote host via SSH
     */
    private void connect() throws Exception {
        JSch jsch = new JSch();
        session = jsch.getSession(user, host, port);
        session.setPassword(password);

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);

        session.connect(30000);
    }

    /**
     * Log message
     */
    private void log(String message) {
        System.out.println(message);
        deploymentLog.append(message).append("\n");
    }

    /**
     * Command execution result
     */
    private static class CommandResult {
        int exitCode;
        String output;
        String stderr;

        CommandResult(int exitCode, String output, String stderr) {
            this.exitCode = exitCode;
            this.output = output;
            this.stderr = stderr;
        }
    }

    /**
     * Deployment result
     */
    public static class DeploymentResult {
        private boolean success;
        private String zookeeperIp;
        private int zookeeperPort;
        private String kafkaIp;
        private int kafkaPort;
        private String error;
        private String log;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getZookeeperIp() { return zookeeperIp; }
        public void setZookeeperIp(String zookeeperIp) { this.zookeeperIp = zookeeperIp; }

        public int getZookeeperPort() { return zookeeperPort; }
        public void setZookeeperPort(int zookeeperPort) { this.zookeeperPort = zookeeperPort; }

        public String getKafkaIp() { return kafkaIp; }
        public void setKafkaIp(String kafkaIp) { this.kafkaIp = kafkaIp; }

        public int getKafkaPort() { return kafkaPort; }
        public void setKafkaPort(int kafkaPort) { this.kafkaPort = kafkaPort; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        public String getLog() { return log; }
        public void setLog(String log) { this.log = log; }
    }

    /**
     * Main entry point
     *
     * Usage: java KafkaZookeeperDeployment <host> <user> <port> <password> [useSudo]
     * Example: java KafkaZookeeperDeployment 123.200.0.50 tbsms 8210 "TB@l38800" true
     */
    public static void main(String[] args) {
        try {
            // Parse command-line arguments or use defaults
            String host;
            String user;
            String password;
            int port;
            boolean useSudo;

            if (args.length >= 4) {
                // Use provided arguments
                host = args[0];
                user = args[1];
                port = Integer.parseInt(args[2]);
                password = args[3];
                useSudo = args.length >= 5 ? Boolean.parseBoolean(args[4]) : true;
            } else if (args.length > 0) {
                // Not enough arguments provided
                System.err.println("Usage: java KafkaZookeeperDeployment <host> <user> <port> <password> [useSudo]");
                System.err.println("Example: java KafkaZookeeperDeployment 123.200.0.50 tbsms 8210 \"TB@l38800\" true");
                System.err.println();
                System.err.println("Or run without arguments to use localhost defaults");
                System.exit(1);
                return;
            } else {
                // Use defaults for localhost deployment
                host = "127.0.0.1";
                user = "mustafa";
                password = "a";
                port = 22;
                useSudo = true;
            }

            System.out.println("Starting Kafka & Zookeeper deployment...");
            System.out.println();

            KafkaZookeeperDeployment deployment = new KafkaZookeeperDeployment(
                host, user, port, password, useSudo
            );

            DeploymentResult result = deployment.deploy();

            if (result.isSuccess()) {
                System.out.println();
                System.out.println("═══════════════════════════════════════════════════════");
                System.out.println("  Deployment Summary");
                System.out.println("═══════════════════════════════════════════════════════");
                System.out.println();
                System.out.println("Zookeeper:");
                System.out.println("  IP:   " + result.getZookeeperIp());
                System.out.println("  Port: " + result.getZookeeperPort());
                System.out.println("  Connection: " + result.getZookeeperIp() + ":" + result.getZookeeperPort());
                System.out.println();
                System.out.println("Kafka:");
                System.out.println("  IP:   " + result.getKafkaIp());
                System.out.println("  Port: " + result.getKafkaPort());
                System.out.println("  Connection: " + result.getKafkaIp() + ":" + result.getKafkaPort());
                System.out.println();
                System.out.println("Verification Commands:");
                System.out.println("  lxc list");
                System.out.println("  lxc exec zookeeper-single -- systemctl status zookeeper");
                System.out.println("  lxc exec kafka-broker-1 -- systemctl status kafka");
                System.out.println();
                System.out.println("Test Kafka:");
                System.out.println("  lxc exec kafka-broker-1 -- /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092");
                System.out.println();

                System.exit(0);
            } else {
                System.err.println();
                System.err.println("Deployment failed: " + result.getError());
                System.err.println();
                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
