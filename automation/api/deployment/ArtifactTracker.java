package automation.api.deployment;

import java.sql.*;
import java.util.*;
import java.util.logging.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class ArtifactTracker {

    private static final Logger logger = Logger.getLogger(ArtifactTracker.class.getName());
    private final Connection dbConnection;
    private final Gson gson = new Gson();

    public ArtifactTracker(Connection dbConnection) {
        this.dbConnection = dbConnection;
        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            // Create artifacts table if not exists
            Statement stmt = dbConnection.createStatement();
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS artifacts (" +
                "  artifact_id VARCHAR(50) PRIMARY KEY," +
                "  type VARCHAR(20) NOT NULL," +
                "  name VARCHAR(100) NOT NULL," +
                "  version VARCHAR(20) NOT NULL," +
                "  path VARCHAR(500) NOT NULL," +
                "  checksum VARCHAR(64)," +
                "  metadata JSON," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  published BOOLEAN DEFAULT FALSE," +
                "  publish_date TIMESTAMP NULL," +
                "  INDEX idx_name_version (name, version)," +
                "  INDEX idx_published (published)" +
                ")"
            );

            // Create deployments table if not exists
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS deployments (" +
                "  deployment_id VARCHAR(50) PRIMARY KEY," +
                "  artifact_id VARCHAR(50)," +
                "  config_file VARCHAR(500)," +
                "  environment VARCHAR(20)," +
                "  status VARCHAR(20)," +
                "  started_at TIMESTAMP," +
                "  completed_at TIMESTAMP NULL," +
                "  deployment_log TEXT," +
                "  FOREIGN KEY (artifact_id) REFERENCES artifacts(artifact_id)," +
                "  INDEX idx_status (status)," +
                "  INDEX idx_environment (environment)" +
                ")"
            );

            logger.info("Database tables initialized");
        } catch (SQLException e) {
            logger.severe("Failed to initialize database: " + e.getMessage());
        }
    }

    public Artifact getArtifact(String artifactId) {
        try {
            PreparedStatement stmt = dbConnection.prepareStatement(
                "SELECT * FROM artifacts WHERE artifact_id = ?"
            );
            stmt.setString(1, artifactId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new Artifact(
                    rs.getString("artifact_id"),
                    rs.getString("type"),
                    rs.getString("name"),
                    rs.getString("version"),
                    rs.getString("path"),
                    rs.getString("checksum"),
                    rs.getString("metadata"),
                    rs.getTimestamp("created_at"),
                    rs.getBoolean("published"),
                    rs.getTimestamp("publish_date")
                );
            }
        } catch (SQLException e) {
            logger.severe("Failed to get artifact: " + e.getMessage());
        }
        return null;
    }

    public String registerArtifact(String type, String name, String version, String path) {
        return registerArtifact(type, name, version, path, null);
    }

    public String registerArtifact(String type, String name, String version, String path,
                                   Map<String, Object> metadata) {
        String artifactId = generateArtifactId(name, version);

        try {
            // Check if artifact already exists
            if (artifactExists(artifactId)) {
                logger.warning("Artifact already exists: " + artifactId);
                return artifactId;
            }

            PreparedStatement stmt = dbConnection.prepareStatement(
                "INSERT INTO artifacts (artifact_id, type, name, version, path, metadata, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, NOW())"
            );
            stmt.setString(1, artifactId);
            stmt.setString(2, type);
            stmt.setString(3, name);
            stmt.setString(4, version);
            stmt.setString(5, path);
            stmt.setString(6, metadata != null ? gson.toJson(metadata) : null);
            stmt.executeUpdate();

            logger.info("Registered artifact: " + artifactId);
            return artifactId;

        } catch (SQLException e) {
            logger.severe("Failed to register artifact: " + e.getMessage());
            return null;
        }
    }

    public boolean publishArtifact(String artifactId) {
        try {
            PreparedStatement stmt = dbConnection.prepareStatement(
                "UPDATE artifacts SET published = TRUE, publish_date = NOW() " +
                "WHERE artifact_id = ?"
            );
            stmt.setString(1, artifactId);
            int affected = stmt.executeUpdate();

            if (affected > 0) {
                logger.info("Published artifact: " + artifactId);
                return true;
            }
        } catch (SQLException e) {
            logger.severe("Failed to publish artifact: " + e.getMessage());
        }
        return false;
    }

    public List<Artifact> getPublishedArtifacts(String name) {
        List<Artifact> artifacts = new ArrayList<>();
        try {
            PreparedStatement stmt = dbConnection.prepareStatement(
                "SELECT * FROM artifacts WHERE name = ? AND published = TRUE " +
                "ORDER BY created_at DESC"
            );
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                artifacts.add(new Artifact(
                    rs.getString("artifact_id"),
                    rs.getString("type"),
                    rs.getString("name"),
                    rs.getString("version"),
                    rs.getString("path"),
                    rs.getString("checksum"),
                    rs.getString("metadata"),
                    rs.getTimestamp("created_at"),
                    rs.getBoolean("published"),
                    rs.getTimestamp("publish_date")
                ));
            }
        } catch (SQLException e) {
            logger.severe("Failed to get published artifacts: " + e.getMessage());
        }
        return artifacts;
    }

    public Artifact getLatestArtifact(String name) {
        try {
            PreparedStatement stmt = dbConnection.prepareStatement(
                "SELECT * FROM artifacts WHERE name = ? AND published = TRUE " +
                "ORDER BY created_at DESC LIMIT 1"
            );
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new Artifact(
                    rs.getString("artifact_id"),
                    rs.getString("type"),
                    rs.getString("name"),
                    rs.getString("version"),
                    rs.getString("path"),
                    rs.getString("checksum"),
                    rs.getString("metadata"),
                    rs.getTimestamp("created_at"),
                    rs.getBoolean("published"),
                    rs.getTimestamp("publish_date")
                );
            }
        } catch (SQLException e) {
            logger.severe("Failed to get latest artifact: " + e.getMessage());
        }
        return null;
    }

    public List<DeploymentHistory> getDeploymentHistory(String artifactId) {
        List<DeploymentHistory> history = new ArrayList<>();
        try {
            PreparedStatement stmt = dbConnection.prepareStatement(
                "SELECT * FROM deployments WHERE artifact_id = ? " +
                "ORDER BY started_at DESC"
            );
            stmt.setString(1, artifactId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                history.add(new DeploymentHistory(
                    rs.getString("deployment_id"),
                    rs.getString("artifact_id"),
                    rs.getString("environment"),
                    rs.getString("status"),
                    rs.getTimestamp("started_at"),
                    rs.getTimestamp("completed_at")
                ));
            }
        } catch (SQLException e) {
            logger.severe("Failed to get deployment history: " + e.getMessage());
        }
        return history;
    }

    private boolean artifactExists(String artifactId) throws SQLException {
        PreparedStatement stmt = dbConnection.prepareStatement(
            "SELECT COUNT(*) FROM artifacts WHERE artifact_id = ?"
        );
        stmt.setString(1, artifactId);
        ResultSet rs = stmt.executeQuery();
        rs.next();
        return rs.getInt(1) > 0;
    }

    private String generateArtifactId(String name, String version) {
        return name.toUpperCase().replace("-", "_") + "_" + version.toUpperCase().replace(".", "_");
    }

    // CLI integration for artifact registration
    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Usage: java ArtifactTracker <action> <type> <name> <version> [path]");
            System.err.println("Actions: register, publish, list");
            System.exit(1);
        }

        try {
            String dbUrl = System.getenv("DB_URL");
            if (dbUrl == null) {
                dbUrl = "jdbc:mysql://127.0.0.1:3306/orchestrix";
            }

            Connection conn = DriverManager.getConnection(
                dbUrl,
                System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "root",
                System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "123456"
            );

            ArtifactTracker tracker = new ArtifactTracker(conn);

            String action = args[0];
            String type = args[1];
            String name = args[2];
            String version = args[3];

            switch (action.toLowerCase()) {
                case "register":
                    if (args.length < 5) {
                        System.err.println("Path required for registration");
                        System.exit(1);
                    }
                    String artifactId = tracker.registerArtifact(type, name, version, args[4]);
                    System.out.println("Registered artifact: " + artifactId);
                    break;

                case "publish":
                    String id = tracker.generateArtifactId(name, version);
                    if (tracker.publishArtifact(id)) {
                        System.out.println("Published artifact: " + id);
                    } else {
                        System.err.println("Failed to publish artifact");
                        System.exit(1);
                    }
                    break;

                case "list":
                    List<Artifact> artifacts = tracker.getPublishedArtifacts(name);
                    System.out.println("Published artifacts for " + name + ":");
                    for (Artifact a : artifacts) {
                        System.out.println("  - " + a.getVersion() + " (ID: " + a.getArtifactId() + ")");
                    }
                    break;

                default:
                    System.err.println("Unknown action: " + action);
                    System.exit(1);
            }

            conn.close();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

// Artifact class
class Artifact {
    private String artifactId;
    private String type;
    private String name;
    private String version;
    private String path;
    private String checksum;
    private String metadata;
    private Timestamp createdAt;
    private boolean published;
    private Timestamp publishDate;

    public Artifact(String artifactId, String type, String name, String version, String path,
                   String checksum, String metadata, Timestamp createdAt, boolean published,
                   Timestamp publishDate) {
        this.artifactId = artifactId;
        this.type = type;
        this.name = name;
        this.version = version;
        this.path = path;
        this.checksum = checksum;
        this.metadata = metadata;
        this.createdAt = createdAt;
        this.published = published;
        this.publishDate = publishDate;
    }

    // Getters
    public String getArtifactId() { return artifactId; }
    public String getType() { return type; }
    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getPath() { return path; }
    public boolean isPublished() { return published; }
    // ... other getters
}

// Deployment History class
class DeploymentHistory {
    private String deploymentId;
    private String artifactId;
    private String environment;
    private String status;
    private Timestamp startedAt;
    private Timestamp completedAt;

    public DeploymentHistory(String deploymentId, String artifactId, String environment,
                            String status, Timestamp startedAt, Timestamp completedAt) {
        this.deploymentId = deploymentId;
        this.artifactId = artifactId;
        this.environment = environment;
        this.status = status;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }
    // Getters
}