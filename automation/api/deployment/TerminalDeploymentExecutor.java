package automation.api.deployment;

import java.io.*;
import java.util.*;
import java.sql.*;
import com.jcraft.jsch.*;  // SSH library
import org.apache.commons.net.telnet.TelnetClient;  // Telnet library
import org.yaml.snakeyaml.Yaml;
import java.util.logging.*;

/**
 * Terminal-based Deployment Executor
 * Deploys via SSH/Telnet by pulling artifacts from published locations
 */
public class TerminalDeploymentExecutor {

    private static final Logger logger = Logger.getLogger(TerminalDeploymentExecutor.class.getName());

    public enum TerminalType {
        SSH, TELNET, LOCAL
    }

    public enum PublishType {
        GOOGLEDRIVE, S3, HTTP, FTP
    }

    private Connection dbConnection;
    private Map<String, Object> config;

    public TerminalDeploymentExecutor(Connection dbConnection) {
        this.dbConnection = dbConnection;
    }

    /**
     * Main deployment method
     */
    public DeploymentResult deploy(String yamlConfigFile) throws Exception {
        logger.info("Starting terminal-based deployment from: " + yamlConfigFile);

        // 1. Load YAML configuration
        this.config = loadYamlConfig(yamlConfigFile);

        // 2. Get terminal configuration
        Map<String, Object> terminal = (Map<String, Object>) config.get("terminal");
        TerminalType terminalType = TerminalType.valueOf(
            terminal.get("type").toString().toUpperCase()
        );

        // 3. Get artifact publication info
        Map<String, Object> artifact = (Map<String, Object>) config.get("artifact");
        String publishUrl = getPublishUrl(artifact);

        if (publishUrl == null) {
            throw new Exception("No valid publish URL found for artifact");
        }

        // 4. Execute deployment based on terminal type
        switch (terminalType) {
            case SSH:
                return deployViaSSH(terminal, artifact, publishUrl);
            case TELNET:
                return deployViaTelnet(terminal, artifact, publishUrl);
            case LOCAL:
                return deployLocal(artifact, publishUrl);
            default:
                throw new Exception("Unsupported terminal type: " + terminalType);
        }
    }

    /**
     * Deploy via SSH
     */
    private DeploymentResult deployViaSSH(Map<String, Object> terminal,
                                          Map<String, Object> artifact,
                                          String publishUrl) throws Exception {

        Map<String, Object> connection = (Map<String, Object>) terminal.get("connection");
        String host = (String) connection.get("host");
        int port = connection.get("port") != null ? (Integer) connection.get("port") : 22;
        String user = (String) connection.get("user");
        String authMethod = (String) connection.get("auth_method");

        logger.info("Deploying via SSH to " + user + "@" + host + ":" + port);

        JSch jsch = new JSch();
        Session session = null;

        try {
            // Setup SSH session
            session = jsch.getSession(user, host, port);

            if ("key".equals(authMethod)) {
                String keyPath = (String) connection.get("ssh_key");
                jsch.addIdentity(expandPath(keyPath));
            } else {
                String password = (String) connection.get("password");
                session.setPassword(password);
            }

            // SSH config
            Properties sshConfig = new Properties();
            sshConfig.put("StrictHostKeyChecking", "no");
            session.setConfig(sshConfig);
            session.connect();

            // Execute deployment commands
            List<String> commands = buildDeploymentCommands(artifact, publishUrl);
            StringBuilder output = new StringBuilder();

            for (String cmd : commands) {
                logger.info("Executing: " + cmd);
                String result = executeSSHCommand(session, cmd);
                output.append(result).append("\n");
            }

            // Record deployment in database
            recordDeployment(artifact, host, true, output.toString());

            return new DeploymentResult(true, "Deployment successful", output.toString());

        } catch (Exception e) {
            logger.severe("SSH deployment failed: " + e.getMessage());
            recordDeployment(artifact, host, false, e.getMessage());
            throw e;
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * Deploy via Telnet
     */
    private DeploymentResult deployViaTelnet(Map<String, Object> terminal,
                                             Map<String, Object> artifact,
                                             String publishUrl) throws Exception {

        Map<String, Object> connection = (Map<String, Object>) terminal.get("connection");
        String host = (String) connection.get("host");
        int port = connection.get("port") != null ? (Integer) connection.get("port") : 23;
        String user = (String) connection.get("user");
        String password = (String) connection.get("password");

        logger.info("Deploying via Telnet to " + host + ":" + port);

        TelnetClient telnet = new TelnetClient();

        try {
            telnet.connect(host, port);

            InputStream in = telnet.getInputStream();
            PrintStream out = new PrintStream(telnet.getOutputStream());

            // Login
            readUntil(in, "login:");
            out.println(user);
            readUntil(in, "Password:");
            out.println(password);

            // Execute deployment commands
            List<String> commands = buildDeploymentCommands(artifact, publishUrl);
            StringBuilder output = new StringBuilder();

            for (String cmd : commands) {
                logger.info("Executing: " + cmd);
                out.println(cmd);
                String result = readUntil(in, "$ ");
                output.append(result).append("\n");
            }

            // Logout
            out.println("exit");
            telnet.disconnect();

            recordDeployment(artifact, host, true, output.toString());
            return new DeploymentResult(true, "Deployment successful", output.toString());

        } catch (Exception e) {
            logger.severe("Telnet deployment failed: " + e.getMessage());
            recordDeployment(artifact, host, false, e.getMessage());
            throw e;
        }
    }

    /**
     * Deploy locally
     */
    private DeploymentResult deployLocal(Map<String, Object> artifact,
                                         String publishUrl) throws Exception {

        logger.info("Deploying locally from: " + publishUrl);

        List<String> commands = buildDeploymentCommands(artifact, publishUrl);
        StringBuilder output = new StringBuilder();

        for (String cmd : commands) {
            logger.info("Executing: " + cmd);
            Process process = Runtime.getRuntime().exec(new String[]{"bash", "-c", cmd});
            process.waitFor();

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        recordDeployment(artifact, "localhost", true, output.toString());
        return new DeploymentResult(true, "Deployment successful", output.toString());
    }

    /**
     * Build deployment commands based on artifact type
     */
    private List<String> buildDeploymentCommands(Map<String, Object> artifact,
                                                 String publishUrl) {
        List<String> commands = new ArrayList<>();

        String artifactName = (String) artifact.get("name");
        String version = (String) artifact.get("version");
        String checksum = (String) artifact.get("checksum");
        String filename = artifactName + "-" + version + ".tar.gz";

        // Download artifact from published location
        commands.add("cd /tmp");
        commands.add("rm -f " + filename);

        // Handle different publish types
        if (publishUrl.contains("drive.google.com")) {
            // Google Drive requires special handling
            commands.add("wget --no-check-certificate -O " + filename + " '" + publishUrl + "'");
        } else if (publishUrl.startsWith("s3://")) {
            // AWS S3
            commands.add("aws s3 cp " + publishUrl + " " + filename);
        } else {
            // Standard HTTP/HTTPS
            commands.add("wget -O " + filename + " '" + publishUrl + "'");
        }

        // Verify checksum if provided
        if (checksum != null && !checksum.isEmpty()) {
            commands.add("echo '" + checksum + "  " + filename + "' | sha256sum -c");
        }

        // Import to LXC
        commands.add("lxc image import " + filename + " --alias " + artifactName + "-" + version);

        // Deploy based on deployment type
        String deployType = ((Map<String, Object>) config.get("deployment")).get("type").toString();

        if ("multi-instance".equals(deployType)) {
            // Deploy multiple instances (e.g., Consul cluster)
            List<Map<String, Object>> targets = (List<Map<String, Object>>) config.get("targets");
            if (targets != null && !targets.isEmpty()) {
                List<Map<String, Object>> instances = (List<Map<String, Object>>)
                    targets.get(0).get("instances");

                for (Map<String, Object> instance : instances) {
                    String instanceName = (String) instance.get("name");
                    commands.add("lxc launch " + artifactName + "-" + version + " " + instanceName);

                    // Set resource limits
                    Map<String, Object> params = (Map<String, Object>) instance.get("params");
                    if (params != null) {
                        if (params.get("memory") != null) {
                            commands.add("lxc config set " + instanceName +
                                       " limits.memory " + params.get("memory"));
                        }
                        if (params.get("cpu") != null) {
                            commands.add("lxc config set " + instanceName +
                                       " limits.cpu " + params.get("cpu"));
                        }
                    }
                }
            }
        } else {
            // Single instance deployment
            commands.add("lxc launch " + artifactName + "-" + version + " " + artifactName + "-main");
        }

        // Verification
        commands.add("lxc list | grep " + artifactName);

        return commands;
    }

    /**
     * Get publish URL from artifact config or database
     */
    private String getPublishUrl(Map<String, Object> artifact) throws SQLException {
        String artifactId = (String) artifact.get("artifact_id");

        // First check if URL is in YAML
        List<Map<String, Object>> publishLocations =
            (List<Map<String, Object>>) artifact.get("publish_locations");

        if (publishLocations != null && !publishLocations.isEmpty()) {
            for (Map<String, Object> location : publishLocations) {
                String url = (String) location.get("url");
                if (url != null && !url.isEmpty()) {
                    // Test if URL is accessible
                    if (isUrlAccessible(url)) {
                        return url;
                    }
                }
            }
        }

        // Otherwise, query database for published URL
        PreparedStatement stmt = dbConnection.prepareStatement(
            "SELECT publish_url FROM artifact_publications " +
            "WHERE artifact_id = ? AND available = true " +
            "ORDER BY priority ASC LIMIT 1"
        );
        stmt.setString(1, artifactId);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            return rs.getString("publish_url");
        }

        return null;
    }

    /**
     * Test if URL is accessible
     */
    private boolean isUrlAccessible(String url) {
        try {
            Process process = Runtime.getRuntime().exec(
                new String[]{"curl", "-Is", url}
            );
            process.waitFor();
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Execute SSH command
     */
    private String executeSSHCommand(Session session, String command) throws Exception {
        ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
        channelExec.setCommand(command);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        channelExec.setOutputStream(outputStream);

        channelExec.connect();

        while (!channelExec.isClosed()) {
            Thread.sleep(100);
        }

        channelExec.disconnect();
        return outputStream.toString();
    }

    /**
     * Read telnet output until pattern
     */
    private String readUntil(InputStream in, String pattern) throws IOException {
        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[1024];

        while (true) {
            int len = in.read(buffer);
            if (len > 0) {
                String output = new String(buffer, 0, len);
                sb.append(output);
                if (output.contains(pattern)) {
                    break;
                }
            }
        }

        return sb.toString();
    }

    /**
     * Record deployment in database
     */
    private void recordDeployment(Map<String, Object> artifact, String host,
                                  boolean success, String output) throws SQLException {
        PreparedStatement stmt = dbConnection.prepareStatement(
            "INSERT INTO deployments (deployment_id, artifact_name, artifact_version, " +
            "host, status, deployment_log, created_at) VALUES (?, ?, ?, ?, ?, ?, NOW())"
        );
        stmt.setString(1, UUID.randomUUID().toString());
        stmt.setString(2, (String) artifact.get("name"));
        stmt.setString(3, (String) artifact.get("version"));
        stmt.setString(4, host);
        stmt.setString(5, success ? "success" : "failed");
        stmt.setString(6, output);
        stmt.executeUpdate();
    }

    /**
     * Load YAML configuration
     */
    private Map<String, Object> loadYamlConfig(String yamlFile) throws Exception {
        Yaml yaml = new Yaml();
        try (InputStream input = new FileInputStream(yamlFile)) {
            return yaml.load(input);
        }
    }

    /**
     * Expand path (handle ~)
     */
    private String expandPath(String path) {
        if (path.startsWith("~")) {
            return System.getProperty("user.home") + path.substring(1);
        }
        return path;
    }

    // Result class
    public static class DeploymentResult {
        private boolean success;
        private String message;
        private String output;

        public DeploymentResult(boolean success, String message, String output) {
            this.success = success;
            this.message = message;
            this.output = output;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getOutput() { return output; }
    }
}