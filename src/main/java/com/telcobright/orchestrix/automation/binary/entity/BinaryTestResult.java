package com.telcobright.orchestrix.automation.binary.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of binary testing
 */
public class BinaryTestResult {

    private boolean success;
    private String binaryPath;
    private long buildTimeMs;
    private long binarySizeBytes;

    private List<String> testsPassed = new ArrayList<>();
    private List<String> testsFailed = new ArrayList<>();
    private String errorMessage;

    public BinaryTestResult() {
    }

    public BinaryTestResult(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getBinaryPath() {
        return binaryPath;
    }

    public void setBinaryPath(String binaryPath) {
        this.binaryPath = binaryPath;
    }

    public long getBuildTimeMs() {
        return buildTimeMs;
    }

    public void setBuildTimeMs(long buildTimeMs) {
        this.buildTimeMs = buildTimeMs;
    }

    public long getBinarySizeBytes() {
        return binarySizeBytes;
    }

    public void setBinarySizeBytes(long binarySizeBytes) {
        this.binarySizeBytes = binarySizeBytes;
    }

    public List<String> getTestsPassed() {
        return testsPassed;
    }

    public void setTestsPassed(List<String> testsPassed) {
        this.testsPassed = testsPassed;
    }

    public void addTestPassed(String testName) {
        this.testsPassed.add(testName);
    }

    public List<String> getTestsFailed() {
        return testsFailed;
    }

    public void setTestsFailed(List<String> testsFailed) {
        this.testsFailed = testsFailed;
    }

    public void addTestFailed(String testName) {
        this.testsFailed.add(testName);
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Get binary size in human-readable format
     */
    public String getBinarySizeFormatted() {
        if (binarySizeBytes < 1024) {
            return binarySizeBytes + " B";
        } else if (binarySizeBytes < 1024 * 1024) {
            return String.format("%.2f KB", binarySizeBytes / 1024.0);
        } else {
            return String.format("%.2f MB", binarySizeBytes / (1024.0 * 1024.0));
        }
    }

    /**
     * Get build time in human-readable format
     */
    public String getBuildTimeFormatted() {
        if (buildTimeMs < 1000) {
            return buildTimeMs + " ms";
        } else {
            return String.format("%.2f s", buildTimeMs / 1000.0);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BinaryTestResult{\n");
        sb.append("  success=").append(success).append("\n");
        sb.append("  binaryPath='").append(binaryPath).append("'\n");
        sb.append("  buildTime=").append(getBuildTimeFormatted()).append("\n");
        sb.append("  binarySize=").append(getBinarySizeFormatted()).append("\n");
        sb.append("  testsPassed=").append(testsPassed.size()).append("\n");
        sb.append("  testsFailed=").append(testsFailed.size()).append("\n");
        if (errorMessage != null) {
            sb.append("  error='").append(errorMessage).append("'\n");
        }
        sb.append("}");
        return sb.toString();
    }
}
