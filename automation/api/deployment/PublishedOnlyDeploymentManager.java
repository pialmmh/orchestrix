package automation.api.deployment;

import java.sql.*;
import java.util.*;
import java.io.*;
import org.yaml.snakeyaml.Yaml;
import java.util.logging.*;
import com.jcraft.jsch.*;

/**
 * Published-Only Deployment Manager
 * ENFORCES: Only published artifacts can be deployed
 * NO local file deployments allowed
 */
public class PublishedOnlyDeploymentManager {

    private static final Logger logger = Logger.getLogger(PublishedOnlyDeploymentManager.class.getName());

    private Connection dbConnection;
    private static final String DB_URL = "jdbc:mysql://127.0.0.1:3306/orchestrix";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "123456";

    public PublishedOnlyDeploymentManager() throws SQLException {
        this.dbConnection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        initializeDatabase();
    }

    private void initializeDatabase() throws SQLException {
        Statement stmt = dbConnection.createStatement();

        // Create artifacts table
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS artifacts (" +
            "  artifact_id VARCHAR(50) PRIMARY KEY," +
            "  artifact_name VARCHAR(100) NOT NULL," +
            "  version VARCHAR(50) NOT NULL," +
            "  build_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "  published BOOLEAN DEFAULT FALSE," +
            "  publish_id VARCHAR(100) UNIQUE," +
            "  INDEX idx_publish (publish_id)" +
            ")"
        );

        // Create publications table
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS artifact_publications (" +
            "  publish_id VARCHAR(100) PRIMARY KEY," +
            "  artifact_id VARCHAR(50) NOT NULL," +
            "  artifact_name VARCHAR(100) NOT NULL," +
            "  version VARCHAR(50) NOT NULL," +
            "  publish_url TEXT NOT NULL," +
            "  publish_type VARCHAR(50)," +  // googledrive, s3, http
            "  checksum VARCHAR(256) NOT NULL," +
            "  file_size VARCHAR(20)," +
            "  publish_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "  available BOOLEAN DEFAULT TRUE," +
            "  download_count INT DEFAULT 0," +
            "  FOREIGN KEY (artifact_id) REFERENCES artifacts(artifact_id)," +
            "  INDEX idx_artifact (artifact_id)" +
            ")"
        );

        // Create deployments table - ONLY references published artifacts
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS deployments (" +
            "  deployment_id VARCHAR(100) PRIMARY KEY," +
            "  publish_id VARCHAR(100) NOT NULL," +
            "  deployment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "  target_host VARCHAR(255)," +
            "  terminal_type VARCHAR(50)," +
            "  deployment_type VARCHAR(50)," +
            "  status VARCHAR(50)," +
            "  deployment_log TEXT," +
            "  completed_at TIMESTAMP NULL," +
            "  FOREIGN KEY (publish_id) REFERENCES artifact_publications(publish_id)," +
            "  INDEX idx_status (status)," +
            "  INDEX idx_date (deployment_date)" +
            ")"
        );

        logger.info("Database tables initialized for published-only deployment");
    }

    /**
     * MAIN DEPLOYMENT METHOD - ONLY ACCEPTS PUBLISHED ARTIFACTS
     */
    public DeploymentResult deploy(String yamlConfigFile) throws Exception {
        logger.info("Starting deployment from: " + yamlConfigFile);

        // Load configuration
        Map<String, Object> config = loadYamlConfig(yamlConfigFile);
        Map<String, Object> artifact = (Map<String, Object>) config.get("artifact");

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

        // ENFORCEMENT 3: Fetch from database (no local paths)
        ArtifactPublication publication = getPublishedArtifact(publishId);
        if (publication == null) {
            throw new IllegalStateException(
                "DEPLOYMENT REJECTED: Published artifact not found.\n" +
                "Publish ID: " + publishId + "\n" +
                "Please verify the artifact is published"
            );
        }

        // ENFORCEMENT 4: Verify publication is available
        if (!publication.available) {
            throw new IllegalStateException(
                "DEPLOYMENT REJECTED: Published artifact is not available.\n" +
                "Publish ID: " + publishId + "\n" +
                "URL may be expired or deleted"
            );
        }

        // Log deployment start
        String deploymentId = UUID.randomUUID().toString();
        recordDeploymentStart(deploymentId, publishId, config);

        try {
            // Get terminal configuration
            Map<String, Object> terminal = (Map<String, Object>) config.get("terminal");
            String terminalType = (String) terminal.get("type");

            DeploymentResult result;

            switch (terminalType.toUpperCase()) {
                case "SSH":
                    result = deployViaSSH(terminal, publication, config);
                    break;
                case "TELNET":
                    result = deployViaTelnet(terminal, publication, config);
                    break;
                case "LOCAL":
                    result = deployLocal(publication, config);
                    break;
                default:
                    throw new Exception("Unsupported terminal type: " + terminalType);
            }

            // Update deployment status
            recordDeploymentComplete(deploymentId, result.success, result.message);

            // Increment download count
            updateDownloadCount(publishId);

            return result;

        } catch (Exception e) {
            recordDeploymentComplete(deploymentId, false, e.getMessage());
            throw e;
        }
    }

    /**
     * Deploy via SSH - downloads from published URL
     */
    private DeploymentResult deployViaSSH(Map<String, Object> terminal,
                                          ArtifactPublication publication,
                                          Map<String, Object> config) throws Exception {

        Map<String, Object> connection = (Map<String, Object>) terminal.get("connection");
        String host = (String) connection.get("host");
        String user = (String) connection.get("user");

        logger.info("Deploying via SSH to " + user + "@" + host);
        logger.info("Pulling artifact from: " + publication.publishType);

        JSch jsch = new JSch();
        Session session = jsch.getSession(user, host, 22);

        // Setup authentication
        String authMethod = (String) connection.get("auth_method");
        if ("key".equals(authMethod)) {
            String keyPath = expandPath((String) connection.get("ssh_key"));
            jsch.addIdentity(keyPath);
        } else {
            session.setPassword((String) connection.get("password"));
        }

        Properties sshConfig = new Properties();
        sshConfig.put("StrictHostKeyChecking", "no");
        session.setConfig(sshConfig);
        session.connect();

        try {
            // Build deployment script
            String deployScript = buildDeploymentScript(publication, config);

            // Execute deployment
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand("bash -s");
            channel.setInputStream(new ByteArrayInputStream(deployScript.getBytes()));

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            channel.setOutputStream(output);
            channel.setErrStream(output);

            channel.connect();

            while (!channel.isClosed()) {
                Thread.sleep(100);
            }

            String result = output.toString();
            boolean success = channel.getExitStatus() == 0;

            return new DeploymentResult(success, success ? "Deployment successful" : "Deployment failed", result);

        } finally {
            session.disconnect();
        }
    }

    /**
     * Build deployment script that downloads from published URL
     */
    private String buildDeploymentScript(ArtifactPublication pub, Map<String, Object> config) {
        StringBuilder script = new StringBuilder();

        script.append("#!/bin/bash\n");
        script.append("set -e\n\n");

        script.append("echo '=== Deployment from Published Artifact ==='\n");
        script.append("echo 'Publish ID: ").append(pub.publishId).append("'\n");
        script.append("echo 'Artifact: ").append(pub.artifactName).append(" v").append(pub.version).append("'\n");
        script.append("echo 'Source: ").append(pub.publishType).append("'\n\n");

        // Download from published URL
        String filename = pub.artifactName + "-" + pub.version + ".tar.gz";
        script.append("cd /tmp\n");
        script.append("rm -f ").append(filename).append("\n\n");

        script.append("echo 'Downloading from published location...'\n");

        // Handle different publish types
        if ("googledrive".equals(pub.publishType)) {
            script.append("wget --no-check-certificate -O ").append(filename);
            script.append(" '").append(pub.publishUrl).append("'\n");
        } else if ("s3".equals(pub.publishType)) {
            script.append("aws s3 cp ").append(pub.publishUrl).append(" ").append(filename).append("\n");
        } else {
            script.append("wget -O ").append(filename).append(" '").append(pub.publishUrl).append("'\n");
        }

        // Verify checksum
        script.append("\necho 'Verifying checksum...'\n");
        script.append("echo '").append(pub.checksum).append("  ").append(filename).append("' | sha256sum -c\n");

        // Import to LXC
        script.append("\necho 'Importing container image...'\n");
        script.append("lxc image import ").append(filename);
        script.append(" --alias ").append(pub.artifactName).append("-").append(pub.version).append("\n");

        // Deploy instances
        Map<String, Object> deployment = (Map<String, Object>) config.get("deployment");
        String deployType = (String) deployment.get("type");

        if ("multi-instance".equals(deployType)) {
            List<Map<String, Object>> targets = (List<Map<String, Object>>) config.get("targets");
            List<Map<String, Object>> instances = (List<Map<String, Object>>) targets.get(0).get("instances");

            for (Map<String, Object> instance : instances) {
                String name = (String) instance.get("name");
                script.append("\necho 'Deploying ").append(name).append("...'\n");
                script.append("lxc launch ").append(pub.artifactName).append("-").append(pub.version);
                script.append(" ").append(name).append("\n");

                // Set resource limits
                Map<String, Object> params = (Map<String, Object>) instance.get("params");
                if (params != null) {
                    if (params.get("memory") != null) {
                        script.append("lxc config set ").append(name);
                        script.append(" limits.memory ").append(params.get("memory")).append("\n");
                    }
                    if (params.get("cpu") != null) {
                        script.append("lxc config set ").append(name);
                        script.append(" limits.cpu ").append(params.get("cpu")).append("\n");
                    }
                }
            }
        }

        script.append("\necho 'Deployment complete!'\n");
        script.append("lxc list | grep ").append(pub.artifactName).append("\n");

        return script.toString();
    }

    /**
     * Get published artifact from database
     */
    private ArtifactPublication getPublishedArtifact(String publishId) throws SQLException {
        PreparedStatement stmt = dbConnection.prepareStatement(
            "SELECT * FROM artifact_publications WHERE publish_id = ?"
        );
        stmt.setString(1, publishId);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            ArtifactPublication pub = new ArtifactPublication();
            pub.publishId = rs.getString("publish_id");
            pub.artifactId = rs.getString("artifact_id");
            pub.artifactName = rs.getString("artifact_name");
            pub.version = rs.getString("version");
            pub.publishUrl = rs.getString("publish_url");
            pub.publishType = rs.getString("publish_type");
            pub.checksum = rs.getString("checksum");
            pub.available = rs.getBoolean("available");
            return pub;
        }

        return null;
    }

    /**
     * Record deployment start
     */
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
        stmt.setString(3, (String) connection.get("host"));
        stmt.setString(4, (String) terminal.get("type"));
        stmt.setString(5, (String) deployment.get("type"));
        stmt.executeUpdate();
    }

    /**
     * Record deployment completion
     */
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

    /**
     * Update download count for published artifact
     */
    private void updateDownloadCount(String publishId) throws SQLException {
        PreparedStatement stmt = dbConnection.prepareStatement(
            "UPDATE artifact_publications SET download_count = download_count + 1 " +
            "WHERE publish_id = ?"
        );
        stmt.setString(1, publishId);
        stmt.executeUpdate();
    }

    // Other methods (deployViaTelnet, deployLocal) follow same pattern...

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

    // Main method
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java PublishedOnlyDeploymentManager <yaml-config>");
            System.exit(1);
        }

        try {
            PublishedOnlyDeploymentManager manager = new PublishedOnlyDeploymentManager();
            DeploymentResult result = manager.deploy(args[0]);

            if (result.success) {
                System.out.println("✓ Deployment successful");
            } else {
                System.err.println("✗ Deployment failed");
            }

            System.exit(result.success ? 0 : 1);

        } catch (IllegalStateException e) {
            System.err.println("\n" + e.getMessage());
            System.exit(2);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
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

    static class DeploymentResult {
        boolean success;
        String message;
        String output;

        DeploymentResult(boolean success, String message, String output) {
            this.success = success;
            this.message = message;
            this.output = output;
        }
    }
}