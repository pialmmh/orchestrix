package automation.api.deployment;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.*;
import com.jcraft.jsch.*;
import org.yaml.snakeyaml.Yaml;

/**
 * Reusable SSH-Based Artifact Deployment Manager
 * Deploys ANY published LXC container artifact to remote servers via SSH
 *
 * MANDATORY: All deployments MUST be SSH-based (no local deployments in production)
 */
public class ArtifactDeploymentManager {

    private static final Logger logger = Logger.getLogger(ArtifactDeploymentManager.class.getName());

    private Connection dbConnection;
    private static final String DB_URL = "jdbc:mysql://127.0.0.1:3306/orchestrix";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "123456";

    public ArtifactDeploymentManager() throws SQLException {
        this.dbConnection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    /**
     * Main deployment method - SSH-based deployment from published artifact
     * @param yamlConfigFile Path to deployment YAML
     * @return DeploymentResult with status and details
     */
    public DeploymentResult deploy(String yamlConfigFile) throws Exception {
        logger.info("Starting deployment from: " + yamlConfigFile);

        // Load YAML configuration
        Map<String, Object> config = loadYamlConfig(yamlConfigFile);
        Map<String, Object> artifact = (Map<String, Object>) config.get("artifact");
        Map<String, Object> terminal = (Map<String, Object>) config.get("terminal");

        // ENFORCEMENT 1: Source must be "published"
        String source = (String) artifact.get("source");
        if (!"published".equals(source)) {
            throw new IllegalStateException(
                "DEPLOYMENT REJECTED: Only published artifacts can be deployed.\n" +
                "Current source: '" + source + "'\n" +
                "Required source: 'published'\n" +
                "Please publish your artifact first using publish.sh"
            );
        }

        // ENFORCEMENT 2: Must have publish_id
        String publishId = (String) artifact.get("publish_id");
        if (publishId == null || publishId.trim().isEmpty()) {
            throw new IllegalStateException(
                "DEPLOYMENT REJECTED: No publish_id found in configuration.\n" +
                "Artifacts must be published before deployment.\n" +
                "Run publish.sh to get a publish_id"
            );
        }

        // ENFORCEMENT 3: Terminal type must be specified
        String terminalType = (String) terminal.get("type");
        if (terminalType == null) {
            throw new IllegalStateException(
                "DEPLOYMENT REJECTED: No terminal type specified.\n" +
                "terminal.type must be 'ssh', 'telnet', or 'local' (dev only)"
            );
        }

        // ENFORCEMENT 4: SSH required for production
        Map<String, Object> deployment = (Map<String, Object>) config.get("deployment");
        String environment = (String) deployment.get("environment");
        if ("production".equals(environment) && !"ssh".equals(terminalType)) {
            throw new IllegalStateException(
                "DEPLOYMENT REJECTED: Production deployments MUST use SSH.\n" +
                "Current terminal type: '" + terminalType + "'\n" +
                "Change terminal.type to 'ssh'"
            );
        }

        // Fetch published artifact from database
        ArtifactPublication publication = getPublishedArtifact(publishId);
        if (publication == null) {
            throw new IllegalStateException(
                "DEPLOYMENT REJECTED: Published artifact not found.\n" +
                "Publish ID: " + publishId + "\n" +
                "Please verify the artifact is published"
            );
        }

        if (!publication.available) {
            throw new IllegalStateException(
                "DEPLOYMENT REJECTED: Published artifact is not available.\n" +
                "Publish ID: " + publishId + "\n" +
                "URL may be expired or deleted"
            );
        }

        logger.info("Deploying: " + publication.artifactName + " v" + publication.version);
        logger.info("Publish ID: " + publishId);
        logger.info("Terminal: " + terminalType);

        // Record deployment start
        String deploymentId = "DEP_" + System.currentTimeMillis();
        recordDeploymentStart(deploymentId, publishId, config);

        DeploymentResult result;

        try {
            switch (terminalType.toLowerCase()) {
                case "ssh":
                    result = deployViaSSH(terminal, publication, config, deploymentId);
                    break;
                case "telnet":
                    result = deployViaTelnet(terminal, publication, config, deploymentId);
                    break;
                case "local":
                    if ("production".equals(environment)) {
                        throw new IllegalStateException("Local deployment not allowed in production");
                    }
                    result = deployLocal(publication, config, deploymentId);
                    break;
                default:
                    throw new Exception("Unsupported terminal type: " + terminalType);
            }

            // Record deployment completion
            recordDeploymentComplete(deploymentId, result.success, result.message);

            // Update download count
            updateDownloadCount(publishId);

            return result;

        } catch (Exception e) {
            recordDeploymentComplete(deploymentId, false, e.getMessage());
            throw e;
        }
    }

    /**
     * Deploy via SSH - Downloads from published URL and deploys on remote server
     */
    private DeploymentResult deployViaSSH(Map<String, Object> terminal,
                                          ArtifactPublication publication,
                                          Map<String, Object> config,
                                          String deploymentId) throws Exception {

        Map<String, Object> connection = (Map<String, Object>) terminal.get("connection");
        String host = (String) connection.get("host");
        Integer port = (Integer) connection.getOrDefault("port", 22);
        String user = (String) connection.get("user");

        logger.info("Deploying via SSH to " + user + "@" + host + ":" + port);
        logger.info("Artifact will be pulled from: " + publication.publishType);

        JSch jsch = new JSch();
        Session session = jsch.getSession(user, host, port);

        // Setup authentication
        String authMethod = (String) connection.get("auth_method");
        if ("key".equals(authMethod)) {
            String keyPath = expandPath((String) connection.get("ssh_key"));
            jsch.addIdentity(keyPath);
        } else {
            String password = (String) connection.get("password");
            if (password != null) {
                session.setPassword(password);
            }
        }

        Properties sshConfig = new Properties();
        sshConfig.put("StrictHostKeyChecking", "no");
        session.setConfig(sshConfig);

        logger.info("Connecting to " + host + "...");
        session.connect(30000); // 30 second timeout

        try {
            // Build deployment script that runs on remote server
            String deployScript = buildDeploymentScript(publication, config, deploymentId);

            logger.info("Executing deployment script on remote server...");

            // Execute deployment
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand("bash -s");
            channel.setInputStream(new ByteArrayInputStream(deployScript.getBytes()));

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();
            channel.setOutputStream(output);
            channel.setErrStream(errorOutput);

            channel.connect();

            // Wait for completion
            while (!channel.isClosed()) {
                Thread.sleep(500);
            }

            int exitStatus = channel.getExitStatus();
            String outputStr = output.toString();
            String errorStr = errorOutput.toString();

            logger.info("Deployment completed with exit code: " + exitStatus);

            boolean success = exitStatus == 0;
            String message = success ? "Deployment successful" : "Deployment failed";

            DeploymentResult result = new DeploymentResult();
            result.success = success;
            result.message = message;
            result.output = outputStr;
            result.error = errorStr;
            result.deploymentId = deploymentId;
            result.publishId = publication.publishId;

            return result;

        } finally {
            session.disconnect();
            logger.info("SSH session closed");
        }
    }

    /**
     * Build deployment script that executes on remote server
     * This script downloads from rclone and deploys containers
     */
    private String buildDeploymentScript(ArtifactPublication pub,
                                        Map<String, Object> config,
                                        String deploymentId) {
        StringBuilder script = new StringBuilder();

        script.append("#!/bin/bash\n");
        script.append("set -e\n\n");

        script.append("echo '═══════════════════════════════════════════════════════'\n");
        script.append("echo 'SSH-Based Deployment from Published Artifact'\n");
        script.append("echo '═══════════════════════════════════════════════════════'\n");
        script.append("echo 'Deployment ID: ").append(deploymentId).append("'\n");
        script.append("echo 'Publish ID: ").append(pub.publishId).append("'\n");
        script.append("echo 'Artifact: ").append(pub.artifactName).append(" v").append(pub.version).append("'\n");
        script.append("echo 'Source: ").append(pub.publishType).append("'\n");
        script.append("echo ''\n\n");

        // Extract publish location info from database
        script.append("# Query artifact location from database\n");
        script.append("DB_RESULT=$(mysql -h 127.0.0.1 -P 3306 -u root -p123456 orchestrix -N -B << 'SQL'\n");
        script.append("SELECT n.rclone_remote, n.base_path, l.publish_url\n");
        script.append("FROM artifact_publish_locations l\n");
        script.append("JOIN publish_nodes n ON l.node_id = n.node_id\n");
        script.append("WHERE l.publish_id = '").append(pub.publishId).append("'\n");
        script.append("  AND l.publish_status = 'success'\n");
        script.append("  AND l.available = TRUE\n");
        script.append("LIMIT 1;\n");
        script.append("SQL\n");
        script.append(")\n\n");

        script.append("if [ -z \"$DB_RESULT\" ]; then\n");
        script.append("    echo 'ERROR: Could not fetch artifact location from database'\n");
        script.append("    exit 1\n");
        script.append("fi\n\n");

        script.append("RCLONE_REMOTE=$(echo \"$DB_RESULT\" | awk '{print $1}')\n");
        script.append("BASE_PATH=$(echo \"$DB_RESULT\" | awk '{print $2}')\n\n");

        // Download artifact via rclone
        script.append("echo 'Downloading artifact from published location...'\n");
        script.append("cd /tmp\n");
        script.append("rm -f ").append(pub.artifactName).append("-*.tar.gz\n\n");

        // Build remote path maintaining directory structure
        script.append("REMOTE_PATH=\"$RCLONE_REMOTE:$BASE_PATH/").append(pub.artifactName).append("/")
              .append(pub.artifactName).append("-v.").append(pub.version.replace("v", ""))
              .append("/generated/artifact\"\n\n");

        script.append("rclone copy \"$REMOTE_PATH/\" . --include \"").append(pub.artifactName).append("-*.tar.gz\"\n\n");

        script.append("ARTIFACT=$(ls -t ").append(pub.artifactName).append("-*.tar.gz 2>/dev/null | head -1)\n\n");

        script.append("if [ -z \"$ARTIFACT\" ]; then\n");
        script.append("    echo 'ERROR: Failed to download artifact'\n");
        script.append("    exit 1\n");
        script.append("fi\n\n");

        script.append("echo \"✓ Downloaded: $ARTIFACT\"\n");
        script.append("echo ''\n\n");

        // Verify checksum
        script.append("echo 'Verifying checksum...'\n");
        script.append("echo '").append(pub.checksum.replace("sha256:", "")).append("  $ARTIFACT' | sha256sum -c\n");
        script.append("echo '✓ Checksum verified'\n");
        script.append("echo ''\n\n");

        // Import to LXC
        script.append("echo 'Importing container image...'\n");
        script.append("IMAGE_ALIAS=\"").append(pub.artifactName).append("-published\"\n");
        script.append("lxc image delete \"$IMAGE_ALIAS\" 2>/dev/null || true\n");
        script.append("lxc image import \"/tmp/$ARTIFACT\" --alias \"$IMAGE_ALIAS\"\n");
        script.append("echo '✓ Image imported'\n");
        script.append("echo ''\n\n");

        // Deploy instances based on config
        Map<String, Object> deployment = (Map<String, Object>) config.get("deployment");
        String deployType = (String) deployment.get("type");

        script.append("echo 'Deploying containers...'\n");
        script.append("echo ''\n\n");

        if ("multi-instance".equals(deployType) || "cluster".equals(deployType)) {
            List<Map<String, Object>> targets = (List<Map<String, Object>>) config.get("targets");
            if (targets != null && !targets.isEmpty()) {
                List<Map<String, Object>> instances = (List<Map<String, Object>>) targets.get(0).get("instances");

                for (Map<String, Object> instance : instances) {
                    String name = (String) instance.get("name");
                    Map<String, Object> params = (Map<String, Object>) instance.get("params");

                    script.append("echo 'Deploying ").append(name).append("...'\n");
                    script.append("lxc launch \"$IMAGE_ALIAS\" \"").append(name).append("\"\n");

                    if (params != null) {
                        if (params.get("memory") != null) {
                            script.append("lxc config set \"").append(name).append("\" limits.memory ")
                                  .append(params.get("memory")).append("\n");
                        }
                        if (params.get("cpu") != null) {
                            script.append("lxc config set \"").append(name).append("\" limits.cpu ")
                                  .append(params.get("cpu")).append("\n");
                        }
                    }

                    script.append("echo '✓ ").append(name).append(" deployed'\n");
                    script.append("echo ''\n");
                }
            }
        } else {
            // Single instance
            String containerName = (String) deployment.get("name");
            script.append("lxc launch \"$IMAGE_ALIAS\" \"").append(containerName).append("\"\n");
            script.append("echo '✓ Container deployed'\n");
        }

        script.append("\necho ''\n");
        script.append("echo '═══════════════════════════════════════════════════════'\n");
        script.append("echo 'Deployment Complete!'\n");
        script.append("echo '═══════════════════════════════════════════════════════'\n");
        script.append("lxc list | grep ").append(pub.artifactName).append("\n");

        return script.toString();
    }

    /**
     * Deploy via Telnet (future implementation)
     */
    private DeploymentResult deployViaTelnet(Map<String, Object> terminal,
                                             ArtifactPublication publication,
                                             Map<String, Object> config,
                                             String deploymentId) throws Exception {
        throw new UnsupportedOperationException("Telnet deployment not yet implemented");
    }

    /**
     * Local deployment (development only)
     */
    private DeploymentResult deployLocal(ArtifactPublication publication,
                                        Map<String, Object> config,
                                        String deploymentId) throws Exception {
        logger.warning("Local deployment - NOT for production use!");

        DeploymentResult result = new DeploymentResult();
        result.success = false;
        result.message = "Local deployment not implemented - use SSH deployment";
        result.deploymentId = deploymentId;
        return result;
    }

    // Database methods
    private ArtifactPublication getPublishedArtifact(String publishId) throws SQLException {
        PreparedStatement stmt = dbConnection.prepareStatement(
            "SELECT p.*, l.publish_url, n.rclone_remote, n.base_path " +
            "FROM artifact_publications p " +
            "JOIN artifact_publish_locations l ON p.publish_id = l.publish_id " +
            "JOIN publish_nodes n ON l.node_id = n.node_id " +
            "WHERE p.publish_id = ? AND l.publish_status = 'success' AND l.available = TRUE " +
            "LIMIT 1"
        );
        stmt.setString(1, publishId);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            ArtifactPublication pub = new ArtifactPublication();
            pub.publishId = rs.getString("publish_id");
            pub.artifactId = rs.getString("artifact_id");
            pub.artifactName = rs.getString("artifact_name");
            pub.version = rs.getString("version");
            pub.checksum = rs.getString("checksum");
            pub.publishUrl = rs.getString("publish_url");
            pub.publishType = "googledrive"; // TODO: from database
            pub.available = true;
            return pub;
        }

        return null;
    }

    private void recordDeploymentStart(String deploymentId, String publishId,
                                      Map<String, Object> config) throws SQLException {
        Map<String, Object> deployment = (Map<String, Object>) config.get("deployment");
        Map<String, Object> terminal = (Map<String, Object>) config.get("terminal");
        Map<String, Object> connection = (Map<String, Object>) terminal.get("connection");

        PreparedStatement stmt = dbConnection.prepareStatement(
            "INSERT INTO deployments (deployment_id, publish_id, target_host, " +
            "terminal_type, deployment_type, status) VALUES (?, ?, ?, ?, ?, 'running')"
        );
        stmt.setString(1, deploymentId);
        stmt.setString(2, publishId);
        stmt.setString(3, connection != null ? (String) connection.get("host") : "local");
        stmt.setString(4, (String) terminal.get("type"));
        stmt.setString(5, (String) deployment.get("type"));
        stmt.executeUpdate();
    }

    private void recordDeploymentComplete(String deploymentId, boolean success,
                                         String message) throws SQLException {
        PreparedStatement stmt = dbConnection.prepareStatement(
            "UPDATE deployments SET status = ?, deployment_log = ?, completed_at = NOW() " +
            "WHERE deployment_id = ?"
        );
        stmt.setString(1, success ? "success" : "failed");
        stmt.setString(2, message);
        stmt.setString(3, deploymentId);
        stmt.executeUpdate();
    }

    private void updateDownloadCount(String publishId) throws SQLException {
        PreparedStatement stmt = dbConnection.prepareStatement(
            "UPDATE artifact_publish_locations SET download_count = download_count + 1 " +
            "WHERE publish_id = ?"
        );
        stmt.setString(1, publishId);
        stmt.executeUpdate();
    }

    private String expandPath(String path) {
        if (path != null && path.startsWith("~")) {
            return System.getProperty("user.home") + path.substring(1);
        }
        return path;
    }

    private Map<String, Object> loadYamlConfig(String yamlFile) throws Exception {
        Yaml yaml = new Yaml();
        try (InputStream input = new FileInputStream(yamlFile)) {
            return yaml.load(input);
        }
    }

    // Supporting classes
    static class ArtifactPublication {
        String publishId;
        String artifactId;
        String artifactName;
        String version;
        String publishUrl;
        String publishType;
        String checksum;
        boolean available;
    }

    public static class DeploymentResult {
        public String deploymentId;
        public String publishId;
        public boolean success;
        public String message;
        public String output;
        public String error;
    }

    // Main method for command-line usage
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java ArtifactDeploymentManager <deployment-yaml>");
            System.exit(1);
        }

        try {
            ArtifactDeploymentManager manager = new ArtifactDeploymentManager();
            DeploymentResult result = manager.deploy(args[0]);

            System.out.println("\n═══════════════════════════════════════════════════════");
            System.out.println("      Deployment Result");
            System.out.println("═══════════════════════════════════════════════════════\n");
            System.out.println("Deployment ID: " + result.deploymentId);
            System.out.println("Publish ID: " + result.publishId);
            System.out.println("Status: " + (result.success ? "✓ SUCCESS" : "✗ FAILED"));
            System.out.println("Message: " + result.message);

            if (result.output != null && !result.output.isEmpty()) {
                System.out.println("\nOutput:");
                System.out.println(result.output);
            }

            if (result.error != null && !result.error.isEmpty()) {
                System.out.println("\nErrors:");
                System.out.println(result.error);
            }

            System.out.println("\n═══════════════════════════════════════════════════════");

            System.exit(result.success ? 0 : 1);

        } catch (IllegalStateException e) {
            System.err.println("\n" + e.getMessage());
            System.exit(2);
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
