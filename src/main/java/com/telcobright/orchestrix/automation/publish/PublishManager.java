package com.telcobright.orchestrix.automation.publish;

import com.telcobright.orchestrix.automation.core.device.CommandExecutor;
import com.telcobright.orchestrix.automation.publish.entity.Artifact;
import com.telcobright.orchestrix.automation.publish.entity.ArtifactPublish;
import com.telcobright.orchestrix.automation.publish.entity.PublishLocation;
import com.telcobright.orchestrix.automation.publish.util.MD5Util;
import com.telcobright.orchestrix.automation.publish.util.RcloneManager;

import java.io.File;
import java.sql.Timestamp;
import java.util.logging.Logger;

/**
 * Publish Manager - Orchestrates artifact publishing workflow
 * <p>
 * Workflow:
 * 1. Create artifact record
 * 2. Generate MD5 hash file
 * 3. Validate publish location
 * 4. Upload to remote
 * 5. Download to temp for verification
 * 6. Verify MD5 matches
 * 7. Delete temp copy
 * 8. Update publish record
 */
public class PublishManager {
    private static final Logger logger = Logger.getLogger(PublishManager.class.getName());
    private final CommandExecutor device;
    private final RcloneManager rcloneManager;

    public PublishManager(CommandExecutor device) {
        this.device = device;
        this.rcloneManager = new RcloneManager(device);
    }

    /**
     * Create artifact metadata record
     *
     * @param artifactType    Type of artifact (e.g., 'lxc-container')
     * @param name            Artifact name (e.g., 'go-id')
     * @param version         Version (e.g., 'v1')
     * @param fileName        File name
     * @param localPath       Local file path
     * @param buildTimestamp  Build timestamp
     * @return Artifact instance
     * @throws Exception if artifact creation fails
     */
    public Artifact createArtifact(String artifactType, String name, String version,
                                   String fileName, String localPath, long buildTimestamp) throws Exception {
        File artifactFile = new File(localPath);

        if (!artifactFile.exists()) {
            throw new Exception("Artifact file not found: " + localPath);
        }

        // Generate MD5 hash
        String md5Hash = MD5Util.generate(artifactFile);
        long fileSizeBytes = artifactFile.length();

        Artifact artifact = new Artifact(
                artifactType,
                name,
                version,
                fileName,
                fileSizeBytes,
                md5Hash,
                localPath,
                buildTimestamp
        );

        logger.info("✓ Artifact metadata created:");
        logger.info("  Type:      " + artifactType);
        logger.info("  Name:      " + name);
        logger.info("  Version:   " + version);
        logger.info("  File:      " + fileName);
        logger.info("  Size:      " + formatBytes(fileSizeBytes));
        logger.info("  MD5:       " + md5Hash);
        logger.info("  Timestamp: " + buildTimestamp);

        // TODO: Save to database (requires DAO implementation)
        // artifactDAO.save(artifact);

        return artifact;
    }

    /**
     * Generate MD5 hash file for artifact
     *
     * @param artifactPath Path to artifact file
     * @return Path to .md5 file
     * @throws Exception if hash generation fails
     */
    public String generateHashFile(String artifactPath) throws Exception {
        File artifactFile = new File(artifactPath);
        String hashFilePath = MD5Util.writeHashFile(artifactFile);

        logger.info("✓ MD5 hash file created: " + hashFilePath);
        return hashFilePath;
    }

    /**
     * Validate publish location configuration
     *
     * @param publishLocation Publish location to validate
     * @return true if valid
     * @throws Exception if validation fails
     */
    public boolean validatePublishLocation(PublishLocation publishLocation) throws Exception {
        logger.info("Validating publish location: " + publishLocation.getName());

        // Check rclone installation
        if (!rcloneManager.isRcloneInstalled()) {
            throw new Exception("Rclone is not installed");
        }

        // Validate remote exists
        if (!rcloneManager.validateRemote(publishLocation.getRcloneRemote())) {
            throw new Exception("Rclone remote not configured: " + publishLocation.getRcloneRemote());
        }

        logger.info("✓ Publish location validated: " + publishLocation.getName());
        return true;
    }

    /**
     * Publish artifact to remote location with MD5 verification
     *
     * @param artifact        Artifact to publish
     * @param publishLocation Publish location
     * @return ArtifactPublish record
     * @throws Exception if publish fails
     */
    public ArtifactPublish publishArtifact(Artifact artifact, PublishLocation publishLocation) throws Exception {
        logger.info("=== Starting Artifact Publish ===");
        logger.info("Artifact: " + artifact.getFileName());
        logger.info("Location: " + publishLocation.getName());

        // Validate publish location
        validatePublishLocation(publishLocation);

        File artifactFile = new File(artifact.getLocalPath());
        String remotePath = null;
        File tempDownloadFile = null;
        boolean verified = false;

        try {
            // Step 1: Upload to remote
            logger.info("\n[1/4] Uploading artifact...");
            remotePath = rcloneManager.upload(
                    artifactFile,
                    publishLocation.getRcloneRemote(),
                    publishLocation.getRcloneTargetDir()
            );

            // Step 2: Download to temp for verification
            logger.info("\n[2/4] Downloading for verification...");
            String tempDir = System.getProperty("java.io.tmpdir");
            tempDownloadFile = new File(tempDir, "verify-" + artifact.getFileName());

            rcloneManager.download(remotePath, tempDownloadFile);

            // Step 3: Verify MD5 hash
            logger.info("\n[3/4] Verifying MD5 hash...");
            verified = MD5Util.verify(tempDownloadFile, artifact.getMd5Hash());

            if (!verified) {
                throw new Exception("MD5 verification FAILED - upload corrupted!");
            }

            // Step 4: Cleanup temp file
            logger.info("\n[4/4] Cleaning up temp file...");
            if (tempDownloadFile.delete()) {
                logger.info("✓ Temp file deleted: " + tempDownloadFile.getAbsolutePath());
            }

        } catch (Exception e) {
            // Cleanup on failure
            if (tempDownloadFile != null && tempDownloadFile.exists()) {
                tempDownloadFile.delete();
            }
            throw e;
        }

        // Create publish record
        ArtifactPublish publishRecord = new ArtifactPublish(
                artifact.getId(),
                publishLocation.getId(),
                remotePath
        );
        publishRecord.setVerified(verified);
        publishRecord.setVerificationTimestamp(new Timestamp(System.currentTimeMillis()));

        logger.info("\n=== Publish Successful ===");
        logger.info("✓ Remote Path: " + remotePath);
        logger.info("✓ MD5 Verified: " + verified);

        // TODO: Save publish record to database
        // artifactPublishDAO.save(publishRecord);

        return publishRecord;
    }

    /**
     * Check if artifact already published to location
     *
     * @param artifactId        Artifact ID
     * @param publishLocationId Publish location ID
     * @return true if already published
     */
    public boolean isAlreadyPublished(int artifactId, int publishLocationId) {
        // TODO: Query database
        // return artifactPublishDAO.exists(artifactId, publishLocationId);
        return false;
    }

    /**
     * Format bytes to human-readable string
     *
     * @param bytes Byte count
     * @return Formatted string (e.g., "156.8 MB")
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * Get rclone manager instance
     *
     * @return RcloneManager
     */
    public RcloneManager getRcloneManager() {
        return rcloneManager;
    }
}
