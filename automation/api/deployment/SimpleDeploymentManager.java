package automation.api.deployment;

import java.sql.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import org.yaml.snakeyaml.Yaml;
import java.util.logging.*;
import java.time.LocalDateTime;

/**
 * Simple Deployment Manager for Orchestrix
 * - Single YAML file per deployment
 * - Checks orchestrix database for artifacts
 * - Executes deployment (single or multi-instance)
 */
public class SimpleDeploymentManager {

    private static final Logger logger = Logger.getLogger(SimpleDeploymentManager.class.getName());
    private static final String DB_URL = "jdbc:mysql://127.0.0.1:3306/orchestrix";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "123456";

    private Connection db;
    private Map<String, Object> config;
    private String deploymentId;

    public SimpleDeploymentManager() throws SQLException {
        this.db = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        initializeTables();
    }

    private void initializeTables() throws SQLException {
        Statement stmt = db.createStatement();

        // Create artifacts table
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS artifacts (" +
            "  id INT AUTO_INCREMENT PRIMARY KEY," +
            "  artifact_name VARCHAR(100) NOT NULL," +
            "  artifact_version VARCHAR(50) NOT NULL," +
            "  artifact_type VARCHAR(50) NOT NULL," +  // container, binary
            "  artifact_path VARCHAR(500) NOT NULL," +
            "  checksum VARCHAR(64)," +
            "  published BOOLEAN DEFAULT FALSE," +
            "  publish_date TIMESTAMP NULL," +
            "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "  UNIQUE KEY unique_artifact (artifact_name, artifact_version)" +
            ")"
        );

        // Create deployments table
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS deployments (" +
            "  id INT AUTO_INCREMENT PRIMARY KEY," +
            "  deployment_id VARCHAR(100) UNIQUE NOT NULL," +
            "  config_file VARCHAR(500)," +
            "  artifact_name VARCHAR(100)," +
            "  artifact_version VARCHAR(50)," +
            "  deployment_type VARCHAR(50)," +  // single, multi-instance, cluster
            "  status VARCHAR(50)," +  // pending, running, success, failed
            "  started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "  completed_at TIMESTAMP NULL," +
            "  error_message TEXT," +
            "  deployment_info JSON" +
            ")"
        );

        logger.info("Database tables initialized");
    }

    /**
     * Deploy from YAML config file
     */
    public boolean deploy(String yamlFile) throws Exception {
        logger.info("Starting deployment from: " + yamlFile);
        this.deploymentId = UUID.randomUUID().toString();

        // 1. Load YAML config
        this.config = loadYamlConfig(yamlFile);

        // 2. Extract deployment info
        Map<String, Object> deployment = (Map<String, Object>) config.get("deployment");
        String deploymentName = (String) deployment.get("name");
        String deploymentType = (String) deployment.get("type");

        Map<String, Object> artifact = (Map<String, Object>) config.get("artifact");
        String artifactName = (String) artifact.get("name");
        String artifactVersion = (String) artifact.get("version");
        String artifactPath = (String) artifact.get("path");

        // 3. Check artifact in database
        if (!checkArtifact(artifactName, artifactVersion, artifactPath)) {
            logger.warning("Artifact not found or not published, registering new artifact");
            registerArtifact(artifactName, artifactVersion, "container", artifactPath);
        }

        // 4. Record deployment start
        recordDeploymentStart(yamlFile, artifactName, artifactVersion, deploymentType);

        // 5. Execute based on type
        boolean success = false;
        try {
            switch (deploymentType) {
                case "single":
                    success = deploySingle();
                    break;
                case "multi-instance":
                    success = deployMultiInstance();
                    break;
                case "cluster":
                    success = deployCluster();
                    break;
                default:
                    throw new Exception("Unknown deployment type: " + deploymentType);
            }
        } catch (Exception e) {
            recordDeploymentFailure(e.getMessage());
            throw e;
        }

        // 6. Record result
        if (success) {
            recordDeploymentSuccess();
            logger.info("Deployment successful: " + deploymentId);
        } else {
            recordDeploymentFailure("Deployment failed");
        }

        return success;
    }

    /**
     * Deploy single instance
     */
    private boolean deploySingle() throws Exception {
        logger.info("Executing single instance deployment");

        List<Map<String, Object>> targets = (List<Map<String, Object>>) config.get("targets");
        Map<String, Object> target = targets.get(0);
        List<Map<String, Object>> instances = (List<Map<String, Object>>) target.get("instances");
        Map<String, Object> instance = instances.get(0);

        String name = (String) instance.get("name");
        Map<String, Object> params = (Map<String, Object>) instance.get("params");

        // For Go-ID container
        if (name.contains("go-id")) {
            return deployGoIdContainer(name, params);
        }

        // Generic container deployment
        return deployContainer(name, params);
    }

    /**
     * Deploy multiple instances (like Consul cluster on same machine)
     */
    private boolean deployMultiInstance() throws Exception {
        logger.info("Executing multi-instance deployment");

        List<Map<String, Object>> targets = (List<Map<String, Object>>) config.get("targets");
        Map<String, Object> target = targets.get(0);
        List<Map<String, Object>> instances = (List<Map<String, Object>>) target.get("instances");

        // For Consul cluster
        if (config.toString().contains("consul")) {
            return deployConsulCluster(instances);
        }

        // Generic multi-instance
        boolean allSuccess = true;
        for (Map<String, Object> instance : instances) {
            String name = (String) instance.get("name");
            Map<String, Object> params = (Map<String, Object>) instance.get("params");

            if (!deployContainer(name, params)) {
                allSuccess = false;
                break;
            }
        }

        return allSuccess;
    }

    /**
     * Deploy cluster across multiple servers
     */
    private boolean deployCluster() throws Exception {
        logger.info("Executing cluster deployment");
        // TODO: Implement cross-server deployment
        return false;
    }

    /**
     * Deploy Consul cluster (3 nodes)
     */
    private boolean deployConsulCluster(List<Map<String, Object>> instances) throws Exception {
        logger.info("Deploying Consul cluster with " + instances.size() + " nodes");

        Map<String, Object> artifact = (Map<String, Object>) config.get("artifact");
        String imagePath = (String) artifact.get("path");

        // Import image once
        String imageAlias = "consul-v1";
        exec("lxc image import " + imagePath + " --alias " + imageAlias);

        Map<String, String> nodeIPs = new HashMap<>();

        for (int i = 0; i < instances.size(); i++) {
            Map<String, Object> instance = instances.get(i);
            String nodeName = (String) instance.get("name");
            Map<String, Object> params = (Map<String, Object>) instance.get("params");

            logger.info("Deploying " + nodeName);

            // Clean existing
            exec("lxc stop " + nodeName + " --force 2>/dev/null || true");
            exec("lxc delete " + nodeName + " --force 2>/dev/null || true");

            // Create container
            exec("lxc init " + imageAlias + " " + nodeName);

            // Set limits
            exec("lxc config set " + nodeName + " limits.memory 256M");
            exec("lxc config set " + nodeName + " limits.cpu 1");

            // Start container
            exec("lxc start " + nodeName);
            Thread.sleep(3000);

            // Get IP
            String ip = exec("lxc list " + nodeName + " -c 4 --format csv | cut -d' ' -f1").trim();
            nodeIPs.put(nodeName, ip);
            logger.info(nodeName + " IP: " + ip);

            // Create Consul config
            Map<String, Object> envVars = (Map<String, Object>) params.get("env_vars");
            String nodeId = (String) params.get("node_id");
            int httpPort = Integer.parseInt(envVars.get("HTTP_PORT").toString());
            int serfPort = Integer.parseInt(envVars.get("SERF_LAN_PORT").toString());

            String retryJoin = "[]";
            if (i == 1) {
                // Node 2 joins Node 1
                retryJoin = "[\"" + nodeIPs.get("consul-node-1") + ":8301\"]";
            } else if (i == 2) {
                // Node 3 joins Node 1 and 2
                retryJoin = "[\"" + nodeIPs.get("consul-node-1") + ":8301\",\"" +
                           nodeIPs.get("consul-node-2") + ":8311\"]";
            }

            String consulConfig = String.format(
                "{" +
                "\"node_name\":\"%s\"," +
                "\"datacenter\":\"dc1\"," +
                "\"data_dir\":\"/consul/data\"," +
                "\"server\":true," +
                "\"bootstrap_expect\":3," +
                "\"bind_addr\":\"0.0.0.0\"," +
                "\"client_addr\":\"0.0.0.0\"," +
                "\"retry_join\":%s," +
                "\"ui_config\":{\"enabled\":true}," +
                "\"ports\":{" +
                "  \"http\":%d," +
                "  \"dns\":8600," +
                "  \"server\":%d," +
                "  \"serf_lan\":%d," +
                "  \"serf_wan\":%d" +
                "}," +
                "\"log_level\":\"INFO\"" +
                "}",
                nodeId, retryJoin, httpPort,
                8300 + (i * 10), serfPort, 8302 + (i * 10)
            );

            // Write config to container
            exec("lxc exec " + nodeName + " -- sh -c 'echo " +
                 consulConfig.replace("\"", "\\\"") + " > /consul/config/consul.json'");

            // Start Consul service
            exec("lxc exec " + nodeName + " -- rc-service consul start");

            logger.info(nodeName + " deployed successfully");
        }

        // Wait for cluster formation
        Thread.sleep(10000);

        // Verify cluster
        String members = exec("lxc exec consul-node-1 -- consul members");
        logger.info("Cluster members:\n" + members);

        return members.contains("consul-node-1") &&
               members.contains("consul-node-2") &&
               members.contains("consul-node-3");
    }

    /**
     * Deploy Go-ID container
     */
    private boolean deployGoIdContainer(String name, Map<String, Object> params) throws Exception {
        logger.info("Deploying Go-ID container: " + name);

        Map<String, Object> artifact = (Map<String, Object>) config.get("artifact");
        String imagePath = (String) artifact.get("path");

        // Import and deploy
        String imageAlias = "go-id-v1";
        exec("lxc image import " + imagePath + " --alias " + imageAlias + " 2>/dev/null || true");

        // Clean existing
        exec("lxc stop " + name + " --force 2>/dev/null || true");
        exec("lxc delete " + name + " --force 2>/dev/null || true");

        // Create and start
        exec("lxc launch " + imageAlias + " " + name);

        Thread.sleep(5000);

        // Test health
        String ip = exec("lxc list " + name + " -c 4 --format csv | cut -d' ' -f1").trim();
        String health = exec("curl -s http://" + ip + ":8080/health || echo 'FAIL'");

        return health.contains("ok") || health.contains("healthy");
    }

    /**
     * Generic container deployment
     */
    private boolean deployContainer(String name, Map<String, Object> params) throws Exception {
        logger.info("Deploying container: " + name);

        Map<String, Object> artifact = (Map<String, Object>) config.get("artifact");
        String imagePath = (String) artifact.get("path");
        String imageAlias = artifact.get("name") + "-" + artifact.get("version");

        // Import image
        exec("lxc image import " + imagePath + " --alias " + imageAlias + " 2>/dev/null || true");

        // Deploy container
        exec("lxc stop " + name + " --force 2>/dev/null || true");
        exec("lxc delete " + name + " --force 2>/dev/null || true");
        exec("lxc launch " + imageAlias + " " + name);

        return true;
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
     * Check if artifact exists and is published
     */
    private boolean checkArtifact(String name, String version, String path) throws SQLException {
        PreparedStatement stmt = db.prepareStatement(
            "SELECT published FROM artifacts WHERE artifact_name = ? AND artifact_version = ?"
        );
        stmt.setString(1, name);
        stmt.setString(2, version);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            boolean published = rs.getBoolean("published");
            if (!published) {
                // Auto-publish if path exists
                File file = new File(path);
                if (file.exists()) {
                    publishArtifact(name, version);
                    return true;
                }
            }
            return published;
        }
        return false;
    }

    /**
     * Register new artifact
     */
    private void registerArtifact(String name, String version, String type, String path) throws SQLException {
        PreparedStatement stmt = db.prepareStatement(
            "INSERT INTO artifacts (artifact_name, artifact_version, artifact_type, artifact_path, published, publish_date) " +
            "VALUES (?, ?, ?, ?, TRUE, NOW()) " +
            "ON DUPLICATE KEY UPDATE artifact_path = ?, published = TRUE, publish_date = NOW()"
        );
        stmt.setString(1, name);
        stmt.setString(2, version);
        stmt.setString(3, type);
        stmt.setString(4, path);
        stmt.setString(5, path);
        stmt.executeUpdate();
        logger.info("Registered artifact: " + name + " v" + version);
    }

    /**
     * Publish artifact
     */
    private void publishArtifact(String name, String version) throws SQLException {
        PreparedStatement stmt = db.prepareStatement(
            "UPDATE artifacts SET published = TRUE, publish_date = NOW() " +
            "WHERE artifact_name = ? AND artifact_version = ?"
        );
        stmt.setString(1, name);
        stmt.setString(2, version);
        stmt.executeUpdate();
    }

    /**
     * Record deployment start
     */
    private void recordDeploymentStart(String configFile, String artifactName, String artifactVersion,
                                       String deploymentType) throws SQLException {
        PreparedStatement stmt = db.prepareStatement(
            "INSERT INTO deployments (deployment_id, config_file, artifact_name, artifact_version, " +
            "deployment_type, status, started_at) VALUES (?, ?, ?, ?, ?, 'running', NOW())"
        );
        stmt.setString(1, deploymentId);
        stmt.setString(2, configFile);
        stmt.setString(3, artifactName);
        stmt.setString(4, artifactVersion);
        stmt.setString(5, deploymentType);
        stmt.executeUpdate();
    }

    /**
     * Record deployment success
     */
    private void recordDeploymentSuccess() throws SQLException {
        PreparedStatement stmt = db.prepareStatement(
            "UPDATE deployments SET status = 'success', completed_at = NOW() WHERE deployment_id = ?"
        );
        stmt.setString(1, deploymentId);
        stmt.executeUpdate();
    }

    /**
     * Record deployment failure
     */
    private void recordDeploymentFailure(String error) throws SQLException {
        PreparedStatement stmt = db.prepareStatement(
            "UPDATE deployments SET status = 'failed', completed_at = NOW(), error_message = ? WHERE deployment_id = ?"
        );
        stmt.setString(1, error);
        stmt.setString(2, deploymentId);
        stmt.executeUpdate();
    }

    /**
     * Execute shell command
     */
    private String exec(String command) throws Exception {
        Process process = Runtime.getRuntime().exec(new String[]{"bash", "-c", command});
        process.waitFor();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        return output.toString();
    }

    /**
     * Main method
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java SimpleDeploymentManager <yaml-config>");
            System.exit(1);
        }

        try {
            SimpleDeploymentManager manager = new SimpleDeploymentManager();
            boolean success = manager.deploy(args[0]);
            System.exit(success ? 0 : 1);
        } catch (Exception e) {
            System.err.println("Deployment failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}