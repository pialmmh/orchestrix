package com.telcobright.orchestrix.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic service configuration - reusable across orchestrix
 */
public class ServiceConfig {
    private String serviceName;
    private String serviceType; // systemd, docker, supervisord
    private String execCommand;
    private String workingDirectory;
    private String user = "nobody";
    private String group = "nogroup";
    private Integer port;
    private boolean autoStart = true;
    private boolean restart = true;
    private String restartPolicy = "always"; // always, on-failure, unless-stopped
    private Map<String, String> environment = new HashMap<>();
    private Long memoryLimit; // in bytes
    private Double cpuLimit; // 1.0 = 100%

    // Constructors
    public ServiceConfig() {}

    public ServiceConfig(String serviceName, String execCommand, Integer port) {
        this.serviceName = serviceName;
        this.execCommand = execCommand;
        this.port = port;
        this.serviceType = "systemd";
    }

    // Generate systemd service file content
    public String generateSystemdService() {
        StringBuilder service = new StringBuilder();
        service.append("[Unit]\n");
        service.append("Description=").append(serviceName).append("\n");
        service.append("After=network.target\n\n");

        service.append("[Service]\n");
        service.append("Type=simple\n");
        service.append("User=").append(user).append("\n");
        service.append("Group=").append(group).append("\n");

        if (workingDirectory != null) {
            service.append("WorkingDirectory=").append(workingDirectory).append("\n");
        }

        service.append("ExecStart=").append(execCommand).append("\n");

        if (restart) {
            service.append("Restart=").append(restartPolicy).append("\n");
            service.append("RestartSec=10\n");
        }

        // Environment variables
        for (Map.Entry<String, String> env : environment.entrySet()) {
            service.append("Environment=\"").append(env.getKey())
                   .append("=").append(env.getValue()).append("\"\n");
        }

        // Resource limits
        if (memoryLimit != null) {
            service.append("MemoryMax=").append(memoryLimit).append("\n");
        }
        if (cpuLimit != null) {
            service.append("CPUQuota=").append((int)(cpuLimit * 100)).append("%\n");
        }

        service.append("\n[Install]\n");
        service.append("WantedBy=multi-user.target\n");

        return service.toString();
    }

    // Add environment variable
    public void addEnvironment(String key, String value) {
        environment.put(key, value);
    }

    // Getters and Setters
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }

    public String getExecCommand() { return execCommand; }
    public void setExecCommand(String execCommand) { this.execCommand = execCommand; }

    public String getWorkingDirectory() { return workingDirectory; }
    public void setWorkingDirectory(String workingDirectory) { this.workingDirectory = workingDirectory; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }

    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }

    public boolean isAutoStart() { return autoStart; }
    public void setAutoStart(boolean autoStart) { this.autoStart = autoStart; }

    public boolean isRestart() { return restart; }
    public void setRestart(boolean restart) { this.restart = restart; }

    public String getRestartPolicy() { return restartPolicy; }
    public void setRestartPolicy(String restartPolicy) { this.restartPolicy = restartPolicy; }

    public Map<String, String> getEnvironment() { return environment; }
    public void setEnvironment(Map<String, String> environment) { this.environment = environment; }

    public Long getMemoryLimit() { return memoryLimit; }
    public void setMemoryLimit(Long memoryLimit) { this.memoryLimit = memoryLimit; }

    public Double getCpuLimit() { return cpuLimit; }
    public void setCpuLimit(Double cpuLimit) { this.cpuLimit = cpuLimit; }
}