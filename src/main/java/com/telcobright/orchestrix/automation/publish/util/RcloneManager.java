package com.telcobright.orchestrix.automation.publish.util;

import com.telcobright.orchestrix.automation.core.device.CommandExecutor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Rclone operations manager for artifact publishing
 */
public class RcloneManager {
    private static final Logger logger = Logger.getLogger(RcloneManager.class.getName());
    private final CommandExecutor device;

    public RcloneManager(CommandExecutor device) {
        this.device = device;
    }

    /**
     * Check if rclone is installed
     *
     * @return true if rclone is available
     */
    public boolean isRcloneInstalled() {
        try {
            String result = device.executeCommand("which rclone");
            return result != null && result.contains("/rclone");
        } catch (Exception e) {
            logger.severe("Failed to check rclone installation: " + e.getMessage());
            return false;
        }
    }

    /**
     * List all configured rclone remotes
     *
     * @return List of remote names
     * @throws Exception if listing fails
     */
    public List<String> listRemotes() throws Exception {
        List<String> remotes = new ArrayList<>();
        String result = device.executeCommand("rclone listremotes");

        if (result != null && !result.trim().isEmpty()) {
            for (String line : result.split("\n")) {
                String remote = line.trim();
                if (!remote.isEmpty()) {
                    // Remove trailing colon
                    if (remote.endsWith(":")) {
                        remote = remote.substring(0, remote.length() - 1);
                    }
                    remotes.add(remote);
                }
            }
        }

        return remotes;
    }

    /**
     * Validate that a specific remote exists
     *
     * @param remoteName Remote name to check
     * @return true if remote exists
     * @throws Exception if validation fails
     */
    public boolean validateRemote(String remoteName) throws Exception {
        List<String> remotes = listRemotes();
        boolean exists = remotes.contains(remoteName);

        if (exists) {
            logger.info("✓ Rclone remote validated: " + remoteName);
        } else {
            logger.severe("✗ Rclone remote NOT found: " + remoteName);
            logger.severe("  Available remotes: " + String.join(", ", remotes));
        }

        return exists;
    }

    /**
     * Upload file to rclone remote
     *
     * @param localFile       Local file to upload
     * @param remoteName      Rclone remote name
     * @param remoteDirectory Remote directory path
     * @return Remote file path
     * @throws Exception if upload fails
     */
    public String upload(File localFile, String remoteName, String remoteDirectory) throws Exception {
        if (!localFile.exists()) {
            throw new Exception("Local file not found: " + localFile.getAbsolutePath());
        }

        // Construct remote path
        String remotePath = remoteName + ":" + remoteDirectory + "/" + localFile.getName();

        logger.info("Uploading to rclone remote...");
        logger.info("  Local:  " + localFile.getAbsolutePath());
        logger.info("  Remote: " + remotePath);

        // Upload with progress
        String command = "rclone copy -P \"" + localFile.getAbsolutePath() + "\" \"" +
                remoteName + ":" + remoteDirectory + "\"";

        String result = device.executeCommand(command, true);  // Use PTY for progress

        logger.info("✓ Upload completed: " + remotePath);
        return remotePath;
    }

    /**
     * Download file from rclone remote
     *
     * @param remotePath Remote file path (remote:path/file)
     * @param localFile  Local destination file
     * @throws Exception if download fails
     */
    public void download(String remotePath, File localFile) throws Exception {
        logger.info("Downloading from rclone remote...");
        logger.info("  Remote: " + remotePath);
        logger.info("  Local:  " + localFile.getAbsolutePath());

        // Ensure parent directory exists
        localFile.getParentFile().mkdirs();

        // Download with progress
        String command = "rclone copy -P \"" + remotePath + "\" \"" +
                localFile.getParentFile().getAbsolutePath() + "\"";

        String result = device.executeCommand(command, true);  // Use PTY for progress

        if (!localFile.exists()) {
            throw new Exception("Download failed - file not found: " + localFile.getAbsolutePath());
        }

        logger.info("✓ Download completed: " + localFile.getAbsolutePath());
    }

    /**
     * Check if file exists on remote
     *
     * @param remotePath Remote file path (remote:path/file)
     * @return true if file exists
     */
    public boolean remoteFileExists(String remotePath) {
        try {
            String command = "rclone lsf \"" + remotePath + "\"";
            String result = device.executeCommand(command);
            return result != null && !result.trim().isEmpty();
        } catch (Exception e) {
            logger.warning("Failed to check remote file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get remote file size
     *
     * @param remotePath Remote file path (remote:path/file)
     * @return File size in bytes, or -1 if not found
     */
    public long getRemoteFileSize(String remotePath) {
        try {
            String command = "rclone size --json \"" + remotePath + "\"";
            String result = device.executeCommand(command);

            if (result != null && result.contains("\"bytes\"")) {
                // Parse JSON output: {"count":1,"bytes":12345}
                String bytesStr = result.substring(result.indexOf("\"bytes\":") + 8);
                bytesStr = bytesStr.substring(0, bytesStr.indexOf("}")).trim();
                return Long.parseLong(bytesStr);
            }
        } catch (Exception e) {
            logger.warning("Failed to get remote file size: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Delete file from remote
     *
     * @param remotePath Remote file path (remote:path/file)
     * @throws Exception if deletion fails
     */
    public void delete(String remotePath) throws Exception {
        logger.info("Deleting from remote: " + remotePath);
        String command = "rclone delete \"" + remotePath + "\"";
        device.executeCommand(command);
        logger.info("✓ Remote file deleted");
    }
}
