package com.telcobright.orchestrix.automation.model;

import com.telcobright.orchestrix.model.NetworkConfig;
import java.util.HashMap;
import java.util.Map;

/**
 * Automation-specific configuration
 * Used by automation tasks but still fairly reusable
 */
public class AutomationConfig {
    private String targetHost;
    private String executionMode; // local, ssh, docker-exec, lxc-exec
    private NetworkConfig networkConfig;
    private SshConfig sshConfig;
    private boolean dryRun = false;
    private boolean verbose = false;
    private int maxRetries = 3;
    private long retryDelayMs = 5000;
    private long commandTimeoutMs = 120000; // 2 minutes default
    private Map<String, String> variables = new HashMap<>();

    // Execution context
    private String workingDirectory;
    private String logDirectory = "/var/log/automation";
    private boolean streamOutput = false;

    // Constructors
    public AutomationConfig() {}

    public AutomationConfig(String executionMode) {
        this.executionMode = executionMode;
    }

    // Helper methods
    public boolean isLocalExecution() {
        return "local".equals(executionMode);
    }

    public boolean isSshExecution() {
        return "ssh".equals(executionMode);
    }

    public boolean isContainerExecution() {
        return "docker-exec".equals(executionMode) || "lxc-exec".equals(executionMode);
    }

    public void addVariable(String key, String value) {
        variables.put(key, value);
    }

    public String resolveVariable(String template) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    // Getters and Setters
    public String getTargetHost() { return targetHost; }
    public void setTargetHost(String targetHost) { this.targetHost = targetHost; }

    public String getExecutionMode() { return executionMode; }
    public void setExecutionMode(String executionMode) { this.executionMode = executionMode; }

    public NetworkConfig getNetworkConfig() { return networkConfig; }
    public void setNetworkConfig(NetworkConfig networkConfig) { this.networkConfig = networkConfig; }

    public SshConfig getSshConfig() { return sshConfig; }
    public void setSshConfig(SshConfig sshConfig) { this.sshConfig = sshConfig; }

    public boolean isDryRun() { return dryRun; }
    public void setDryRun(boolean dryRun) { this.dryRun = dryRun; }

    public boolean isVerbose() { return verbose; }
    public void setVerbose(boolean verbose) { this.verbose = verbose; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public long getRetryDelayMs() { return retryDelayMs; }
    public void setRetryDelayMs(long retryDelayMs) { this.retryDelayMs = retryDelayMs; }

    public long getCommandTimeoutMs() { return commandTimeoutMs; }
    public void setCommandTimeoutMs(long commandTimeoutMs) { this.commandTimeoutMs = commandTimeoutMs; }

    public Map<String, String> getVariables() { return variables; }
    public void setVariables(Map<String, String> variables) { this.variables = variables; }

    public String getWorkingDirectory() { return workingDirectory; }
    public void setWorkingDirectory(String workingDirectory) { this.workingDirectory = workingDirectory; }

    public String getLogDirectory() { return logDirectory; }
    public void setLogDirectory(String logDirectory) { this.logDirectory = logDirectory; }

    public boolean isStreamOutput() { return streamOutput; }
    public void setStreamOutput(boolean streamOutput) { this.streamOutput = streamOutput; }

    /**
     * SSH-specific configuration
     */
    public static class SshConfig {
        private String host;
        private int port = 22;
        private String username;
        private String password;
        private String privateKeyPath;
        private boolean strictHostKeyChecking = false;
        private int connectionTimeoutMs = 30000;

        // Constructors
        public SshConfig() {}

        public SshConfig(String host, String username, String privateKeyPath) {
            this.host = host;
            this.username = username;
            this.privateKeyPath = privateKeyPath;
        }

        // Getters and Setters
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }

        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getPrivateKeyPath() { return privateKeyPath; }
        public void setPrivateKeyPath(String privateKeyPath) { this.privateKeyPath = privateKeyPath; }

        public boolean isStrictHostKeyChecking() { return strictHostKeyChecking; }
        public void setStrictHostKeyChecking(boolean strictHostKeyChecking) {
            this.strictHostKeyChecking = strictHostKeyChecking;
        }

        public int getConnectionTimeoutMs() { return connectionTimeoutMs; }
        public void setConnectionTimeoutMs(int connectionTimeoutMs) {
            this.connectionTimeoutMs = connectionTimeoutMs;
        }
    }
}