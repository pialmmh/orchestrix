package com.telcobright.orchestrix.automation.publish.util;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 * MD5 hash utility for file verification
 */
public class MD5Util {
    private static final Logger logger = Logger.getLogger(MD5Util.class.getName());
    private static final int BUFFER_SIZE = 8192;

    /**
     * Generate MD5 hash for a file
     *
     * @param file File to hash
     * @return MD5 hash as hex string
     * @throws Exception if hashing fails
     */
    public static String generate(File file) throws Exception {
        if (!file.exists() || !file.isFile()) {
            throw new FileNotFoundException("File not found: " + file.getAbsolutePath());
        }

        MessageDigest md = MessageDigest.getInstance("MD5");

        try (InputStream is = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(is)) {

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = bis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
        }

        byte[] digest = md.digest();
        return bytesToHex(digest);
    }

    /**
     * Generate MD5 hash for a file path
     *
     * @param filePath Path to file
     * @return MD5 hash as hex string
     * @throws Exception if hashing fails
     */
    public static String generate(String filePath) throws Exception {
        return generate(new File(filePath));
    }

    /**
     * Verify file matches expected MD5 hash
     *
     * @param file         File to verify
     * @param expectedHash Expected MD5 hash
     * @return true if hash matches
     * @throws Exception if verification fails
     */
    public static boolean verify(File file, String expectedHash) throws Exception {
        String actualHash = generate(file);
        boolean matches = actualHash.equalsIgnoreCase(expectedHash);

        if (matches) {
            logger.info("✓ MD5 verification successful: " + file.getName());
        } else {
            logger.severe("✗ MD5 verification FAILED: " + file.getName());
            logger.severe("  Expected: " + expectedHash);
            logger.severe("  Actual:   " + actualHash);
        }

        return matches;
    }

    /**
     * Verify file path matches expected MD5 hash
     *
     * @param filePath     Path to file
     * @param expectedHash Expected MD5 hash
     * @return true if hash matches
     * @throws Exception if verification fails
     */
    public static boolean verify(String filePath, String expectedHash) throws Exception {
        return verify(new File(filePath), expectedHash);
    }

    /**
     * Write MD5 hash to .md5 file
     * Creates file with same name as input + .md5 extension
     *
     * @param file File to generate hash for
     * @return Path to .md5 file
     * @throws Exception if writing fails
     */
    public static String writeHashFile(File file) throws Exception {
        String hash = generate(file);
        String hashFilePath = file.getAbsolutePath() + ".md5";

        // Write hash with filename (standard md5sum format)
        String content = hash + "  " + file.getName() + "\n";
        Files.write(Paths.get(hashFilePath), content.getBytes());

        logger.info("MD5 hash file created: " + hashFilePath);
        return hashFilePath;
    }

    /**
     * Write MD5 hash to .md5 file for file path
     *
     * @param filePath Path to file
     * @return Path to .md5 file
     * @throws Exception if writing fails
     */
    public static String writeHashFile(String filePath) throws Exception {
        return writeHashFile(new File(filePath));
    }

    /**
     * Read MD5 hash from .md5 file
     *
     * @param hashFilePath Path to .md5 file
     * @return MD5 hash string
     * @throws Exception if reading fails
     */
    public static String readHashFile(String hashFilePath) throws Exception {
        String content = new String(Files.readAllBytes(Paths.get(hashFilePath)));
        // Parse standard md5sum format: "hash  filename"
        String[] parts = content.trim().split("\\s+");
        if (parts.length == 0) {
            throw new Exception("Invalid MD5 file format");
        }
        return parts[0];
    }

    /**
     * Convert byte array to hex string
     *
     * @param bytes Byte array
     * @return Hex string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Get hash file path for given file
     *
     * @param file Original file
     * @return Path to .md5 file
     */
    public static String getHashFilePath(File file) {
        return file.getAbsolutePath() + ".md5";
    }

    /**
     * Check if MD5 hash file exists
     *
     * @param file Original file
     * @return true if .md5 file exists
     */
    public static boolean hashFileExists(File file) {
        return new File(getHashFilePath(file)).exists();
    }
}
