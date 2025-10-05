package com.telcobright.orchestrix.automation.binary.entity;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for building standalone binaries
 *
 * Base configuration that can be extended for specific binary types (Go, Rust, C++, etc.)
 */
public class BinaryBuildConfig {

    // Binary metadata
    private String binaryName;
    private String version;
    private String description;

    // Build paths
    private String sourceDirectory;      // Where source code is located
    private String buildDirectory;       // Where to build (temp)
    private String outputDirectory;      // Where to place final binary
    private String outputBinaryName;     // Final binary filename

    // Build settings
    private String targetOS = "linux";
    private String targetArch = "amd64";
    private Map<String, String> buildEnv = new HashMap<>();
    private Map<String, String> buildFlags = new HashMap<>();

    // Testing
    private boolean runTests = true;
    private int testPort = 7001;
    private int testTimeoutSeconds = 30;

    // Dependencies
    private Map<String, String> dependencies = new HashMap<>();

    public BinaryBuildConfig() {
    }

    public BinaryBuildConfig(String binaryName, String version) {
        this.binaryName = binaryName;
        this.version = version;
        this.outputBinaryName = binaryName;
    }

    // Getters and Setters
    public String getBinaryName() {
        return binaryName;
    }

    public void setBinaryName(String binaryName) {
        this.binaryName = binaryName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSourceDirectory() {
        return sourceDirectory;
    }

    public void setSourceDirectory(String sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
    }

    public String getBuildDirectory() {
        return buildDirectory;
    }

    public void setBuildDirectory(String buildDirectory) {
        this.buildDirectory = buildDirectory;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public String getOutputBinaryName() {
        return outputBinaryName;
    }

    public void setOutputBinaryName(String outputBinaryName) {
        this.outputBinaryName = outputBinaryName;
    }

    public String getTargetOS() {
        return targetOS;
    }

    public void setTargetOS(String targetOS) {
        this.targetOS = targetOS;
    }

    public String getTargetArch() {
        return targetArch;
    }

    public void setTargetArch(String targetArch) {
        this.targetArch = targetArch;
    }

    public Map<String, String> getBuildEnv() {
        return buildEnv;
    }

    public void setBuildEnv(Map<String, String> buildEnv) {
        this.buildEnv = buildEnv;
    }

    public void addBuildEnv(String key, String value) {
        this.buildEnv.put(key, value);
    }

    public Map<String, String> getBuildFlags() {
        return buildFlags;
    }

    public void setBuildFlags(Map<String, String> buildFlags) {
        this.buildFlags = buildFlags;
    }

    public void addBuildFlag(String key, String value) {
        this.buildFlags.put(key, value);
    }

    public boolean isRunTests() {
        return runTests;
    }

    public void setRunTests(boolean runTests) {
        this.runTests = runTests;
    }

    public int getTestPort() {
        return testPort;
    }

    public void setTestPort(int testPort) {
        this.testPort = testPort;
    }

    public int getTestTimeoutSeconds() {
        return testTimeoutSeconds;
    }

    public void setTestTimeoutSeconds(int testTimeoutSeconds) {
        this.testTimeoutSeconds = testTimeoutSeconds;
    }

    public Map<String, String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Map<String, String> dependencies) {
        this.dependencies = dependencies;
    }

    public void addDependency(String name, String version) {
        this.dependencies.put(name, version);
    }

    /**
     * Get full path to output binary
     */
    public String getOutputBinaryPath() {
        return outputDirectory + "/" + outputBinaryName;
    }

    @Override
    public String toString() {
        return "BinaryBuildConfig{" +
                "binaryName='" + binaryName + '\'' +
                ", version='" + version + '\'' +
                ", targetOS='" + targetOS + '\'' +
                ", targetArch='" + targetArch + '\'' +
                ", outputPath='" + getOutputBinaryPath() + '\'' +
                '}';
    }
}
