package automation.api.publish;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.*;

/**
 * Reusable Artifact Publishing Manager
 * Publishes any LXC container artifact to all enabled publish nodes
 */
public class ArtifactPublishManager {

    private static final Logger logger = Logger.getLogger(ArtifactPublishManager.class.getName());

    private Connection dbConnection;
    private static final String DB_URL = "jdbc:mysql://127.0.0.1:3306/orchestrix";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "123456";

    public ArtifactPublishManager() throws SQLException {
        this.dbConnection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    /**
     * Main publish method - works with any artifact
     * @param artifactPath Full path to artifact file
     * @return PublishResult with publish_id and locations
     */
    public PublishResult publish(String artifactPath) throws Exception {
        File artifactFile = new File(artifactPath);

        if (!artifactFile.exists()) {
            throw new FileNotFoundException("Artifact not found: " + artifactPath);
        }

        // Extract metadata from path and filename
        ArtifactMetadata metadata = extractMetadata(artifactPath);

        logger.info("Publishing artifact: " + metadata.artifactName + " v" + metadata.version);
        logger.info("File: " + artifactFile.getName());
        logger.info("Size: " + formatFileSize(artifactFile.length()));

        // Calculate checksum
        String checksum = calculateChecksum(artifactFile);
        logger.info("Checksum: sha256:" + checksum);

        // Query enabled publish nodes
        List<PublishNode> enabledNodes = getEnabledPublishNodes();

        if (enabledNodes.isEmpty()) {
            throw new IllegalStateException("No enabled publish nodes found in database");
        }

        logger.info("Found " + enabledNodes.size() + " enabled publish node(s)");

        // Generate publish_id
        String publishId = generatePublishId(metadata.artifactName, metadata.version);
        String artifactId = metadata.artifactName.toUpperCase() + "_" + metadata.version.toUpperCase();

        logger.info("Publish ID: " + publishId);

        // Create artifact record
        createArtifactRecord(artifactId, metadata.artifactName, metadata.version);

        // Create publication record
        createPublicationRecord(publishId, artifactId, metadata.artifactName, metadata.version,
                                checksum, formatFileSize(artifactFile.length()));

        // Upload to each enabled node
        List<PublishLocation> locations = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (PublishNode node : enabledNodes) {
            logger.info("Uploading to: " + node.nodeName);

            try {
                PublishLocation location = uploadToNode(artifactFile, metadata, node, publishId);
                locations.add(location);

                if ("success".equals(location.status)) {
                    successCount++;
                    logger.info("✓ Upload successful: " + location.publishUrl);
                } else {
                    failCount++;
                    logger.warning("✗ Upload failed: " + location.errorMessage);
                }
            } catch (Exception e) {
                failCount++;
                logger.log(Level.SEVERE, "Upload failed to " + node.nodeName, e);

                // Record failure
                recordPublishLocation(publishId, node.nodeId, "", "failed", false, e.getMessage());
            }
        }

        if (successCount == 0) {
            throw new Exception("All uploads failed!");
        }

        PublishResult result = new PublishResult();
        result.publishId = publishId;
        result.successCount = successCount;
        result.failCount = failCount;
        result.locations = locations;
        result.artifactName = metadata.artifactName;
        result.version = metadata.version;

        return result;
    }

    /**
     * Extract metadata from artifact path
     * Expected format: .../images/containers/lxc/{container}/{version}/generated/artifact/{file}.tar.gz
     */
    private ArtifactMetadata extractMetadata(String artifactPath) throws Exception {
        Path path = Paths.get(artifactPath).toAbsolutePath();
        String pathStr = path.toString();

        // Extract relative path from images/containers/lxc/
        int imagesIdx = pathStr.indexOf("images/containers/lxc/");
        if (imagesIdx == -1) {
            throw new IllegalArgumentException("Artifact must be under images/containers/lxc/ directory");
        }

        String relativePath = pathStr.substring(imagesIdx + "images/containers/lxc/".length());
        String[] parts = relativePath.split("/");

        if (parts.length < 4) {
            throw new IllegalArgumentException("Invalid artifact path structure");
        }

        ArtifactMetadata metadata = new ArtifactMetadata();
        metadata.containerName = parts[0];  // e.g., "consul"
        metadata.versionDir = parts[1];     // e.g., "consul-v.1"

        // Extract artifact name and version from filename
        // Expected: {name}-{version}-{timestamp}.tar.gz
        String filename = path.getFileName().toString();
        String nameWithoutExt = filename.replaceAll("\\.tar\\.gz$", "");
        String[] fileParts = nameWithoutExt.split("-");

        if (fileParts.length >= 2) {
            metadata.artifactName = fileParts[0];
            metadata.version = fileParts[1];
        } else {
            metadata.artifactName = metadata.containerName;
            metadata.version = "v1";
        }

        // Build relative path for remote (maintains directory structure)
        metadata.relativePath = metadata.containerName + "/" + metadata.versionDir + "/generated/artifact";

        return metadata;
    }

    /**
     * Upload artifact to a specific publish node
     */
    private PublishLocation uploadToNode(File artifactFile, ArtifactMetadata metadata,
                                         PublishNode node, String publishId) throws Exception {

        PublishLocation location = new PublishLocation();
        location.nodeId = node.nodeId;
        location.nodeName = node.nodeName;

        if ("googledrive".equals(node.nodeType)) {
            // Build remote path maintaining directory structure
            String remotePath = node.rcloneRemote + ":" + node.basePath + "/" + metadata.relativePath;
            String remoteFile = remotePath + "/" + artifactFile.getName();

            logger.info("  Remote: " + remoteFile);

            // Create directory via rclone
            executeRclone("mkdir", remotePath);

            // Upload file
            logger.info("  Uploading...");
            int exitCode = executeRclone("copy", artifactFile.getAbsolutePath(), remotePath);

            if (exitCode == 0) {
                // Get shareable link
                String shareUrl = getRcloneLink(remoteFile);

                location.publishUrl = shareUrl;
                location.status = "success";
                location.available = true;

                // Record in database
                recordPublishLocation(publishId, node.nodeId, shareUrl, "success", true, null);
            } else {
                location.status = "failed";
                location.available = false;
                location.errorMessage = "rclone copy failed with exit code: " + exitCode;

                // Record failure
                recordPublishLocation(publishId, node.nodeId, "", "failed", false, location.errorMessage);
            }
        } else {
            throw new UnsupportedOperationException("Node type not supported: " + node.nodeType);
        }

        return location;
    }

    /**
     * Execute rclone command
     */
    private int executeRclone(String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("rclone");
        command.addAll(Arrays.asList(args));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Consume output to prevent blocking
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Suppress progress output
                if (!line.contains("Transferred:") && !line.contains("Elapsed time:")) {
                    logger.fine(line);
                }
            }
        }

        return process.waitFor();
    }

    /**
     * Get rclone shareable link
     */
    private String getRcloneLink(String remoteFile) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("rclone", "link", remoteFile);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        process.waitFor();
        return output.toString().trim();
    }

    /**
     * Get enabled publish nodes from database
     */
    private List<PublishNode> getEnabledPublishNodes() throws SQLException {
        List<PublishNode> nodes = new ArrayList<>();

        PreparedStatement stmt = dbConnection.prepareStatement(
            "SELECT node_id, node_name, node_type, rclone_remote, base_path " +
            "FROM publish_nodes WHERE enabled = TRUE ORDER BY priority"
        );

        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            PublishNode node = new PublishNode();
            node.nodeId = rs.getString("node_id");
            node.nodeName = rs.getString("node_name");
            node.nodeType = rs.getString("node_type");
            node.rcloneRemote = rs.getString("rclone_remote");
            node.basePath = rs.getString("base_path");
            nodes.add(node);
        }

        return nodes;
    }

    /**
     * Generate unique publish_id
     */
    private String generatePublishId(String artifactName, String version) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        return String.format("PUB_%s_%s_%s",
            artifactName.toUpperCase(),
            version.toUpperCase().replace(".", ""),
            timestamp);
    }

    /**
     * Create artifact record in database
     */
    private void createArtifactRecord(String artifactId, String artifactName, String version) throws SQLException {
        PreparedStatement stmt = dbConnection.prepareStatement(
            "INSERT INTO artifacts (artifact_id, artifact_name, version, published) " +
            "VALUES (?, ?, ?, TRUE) " +
            "ON DUPLICATE KEY UPDATE published = TRUE"
        );
        stmt.setString(1, artifactId);
        stmt.setString(2, artifactName);
        stmt.setString(3, version);
        stmt.executeUpdate();
    }

    /**
     * Create publication record in database
     */
    private void createPublicationRecord(String publishId, String artifactId, String artifactName,
                                        String version, String checksum, String fileSize) throws SQLException {
        PreparedStatement stmt = dbConnection.prepareStatement(
            "INSERT INTO artifact_publications " +
            "(publish_id, artifact_id, artifact_name, version, checksum, file_size) " +
            "VALUES (?, ?, ?, ?, ?, ?)"
        );
        stmt.setString(1, publishId);
        stmt.setString(2, artifactId);
        stmt.setString(3, artifactName);
        stmt.setString(4, version);
        stmt.setString(5, "sha256:" + checksum);
        stmt.setString(6, fileSize);
        stmt.executeUpdate();
    }

    /**
     * Record publish location in database
     */
    private void recordPublishLocation(String publishId, String nodeId, String publishUrl,
                                      String status, boolean available, String errorMessage) throws SQLException {
        PreparedStatement stmt = dbConnection.prepareStatement(
            "INSERT INTO artifact_publish_locations " +
            "(publish_id, node_id, publish_url, publish_status, available, error_message) " +
            "VALUES (?, ?, ?, ?, ?, ?)"
        );
        stmt.setString(1, publishId);
        stmt.setString(2, nodeId);
        stmt.setString(3, publishUrl);
        stmt.setString(4, status);
        stmt.setBoolean(5, available);
        stmt.setString(6, errorMessage);
        stmt.executeUpdate();
    }

    /**
     * Calculate SHA256 checksum
     */
    private String calculateChecksum(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }

        byte[] hashBytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Format file size to human-readable format
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return String.format("%.1fKB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1fMB", bytes / (1024.0 * 1024));
        return String.format("%.1fGB", bytes / (1024.0 * 1024 * 1024));
    }

    // Supporting classes
    static class ArtifactMetadata {
        String containerName;
        String versionDir;
        String artifactName;
        String version;
        String relativePath;
    }

    static class PublishNode {
        String nodeId;
        String nodeName;
        String nodeType;
        String rcloneRemote;
        String basePath;
    }

    static class PublishLocation {
        String nodeId;
        String nodeName;
        String publishUrl;
        String status;
        boolean available;
        String errorMessage;
    }

    public static class PublishResult {
        public String publishId;
        public String artifactName;
        public String version;
        public int successCount;
        public int failCount;
        public List<PublishLocation> locations;

        public boolean isSuccess() {
            return successCount > 0;
        }
    }

    // Main method for command-line usage
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java ArtifactPublishManager <artifact-file-path>");
            System.exit(1);
        }

        try {
            ArtifactPublishManager manager = new ArtifactPublishManager();
            PublishResult result = manager.publish(args[0]);

            System.out.println("\n═══════════════════════════════════════════════════════");
            System.out.println("      Publishing Summary");
            System.out.println("═══════════════════════════════════════════════════════\n");
            System.out.println("Publish ID: " + result.publishId);
            System.out.println("Artifact: " + result.artifactName + " v" + result.version);
            System.out.println("Successful uploads: " + result.successCount);
            System.out.println("Failed uploads: " + result.failCount);
            System.out.println("\nPublish locations:");
            for (PublishLocation loc : result.locations) {
                System.out.println("  " + (loc.available ? "✓" : "✗") + " " + loc.nodeName);
                if (loc.available) {
                    System.out.println("    URL: " + loc.publishUrl);
                } else {
                    System.out.println("    Error: " + loc.errorMessage);
                }
            }
            System.out.println("\n═══════════════════════════════════════════════════════");

            System.exit(result.isSuccess() ? 0 : 1);

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
