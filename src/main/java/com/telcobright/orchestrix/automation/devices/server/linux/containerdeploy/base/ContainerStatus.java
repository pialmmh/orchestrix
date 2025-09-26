package com.telcobright.orchestrix.automation.devices.server.linux.containerdeploy.base;

import java.time.LocalDateTime;

/**
 * Container status information
 */
public class ContainerStatus {

    public enum State {
        RUNNING("running"),
        STOPPED("stopped"),
        PAUSED("paused"),
        CREATED("created"),
        EXITED("exited"),
        DEAD("dead"),
        RESTARTING("restarting"),
        UNKNOWN("unknown");

        private final String value;

        State(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static State fromString(String value) {
            for (State state : values()) {
                if (state.value.equalsIgnoreCase(value)) {
                    return state;
                }
            }
            return UNKNOWN;
        }
    }

    private String containerName;
    private String containerId;
    private State state;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private int exitCode;
    private String healthStatus;
    private long pid;
    private String ipAddress;
    private long memoryUsage;
    private long memoryLimit;
    private double cpuUsage;
    private long diskUsage;
    private long networkRx;
    private long networkTx;

    // Getters and setters
    public String getContainerName() { return containerName; }
    public void setContainerName(String containerName) { this.containerName = containerName; }

    public String getContainerId() { return containerId; }
    public void setContainerId(String containerId) { this.containerId = containerId; }

    public State getState() { return state; }
    public void setState(State state) { this.state = state; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }

    public int getExitCode() { return exitCode; }
    public void setExitCode(int exitCode) { this.exitCode = exitCode; }

    public String getHealthStatus() { return healthStatus; }
    public void setHealthStatus(String healthStatus) { this.healthStatus = healthStatus; }

    public long getPid() { return pid; }
    public void setPid(long pid) { this.pid = pid; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public long getMemoryUsage() { return memoryUsage; }
    public void setMemoryUsage(long memoryUsage) { this.memoryUsage = memoryUsage; }

    public long getMemoryLimit() { return memoryLimit; }
    public void setMemoryLimit(long memoryLimit) { this.memoryLimit = memoryLimit; }

    public double getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; }

    public long getDiskUsage() { return diskUsage; }
    public void setDiskUsage(long diskUsage) { this.diskUsage = diskUsage; }

    public long getNetworkRx() { return networkRx; }
    public void setNetworkRx(long networkRx) { this.networkRx = networkRx; }

    public long getNetworkTx() { return networkTx; }
    public void setNetworkTx(long networkTx) { this.networkTx = networkTx; }

    public boolean isRunning() {
        return state == State.RUNNING;
    }

    public boolean isStopped() {
        return state == State.STOPPED || state == State.EXITED;
    }

    @Override
    public String toString() {
        return String.format("Container[name=%s, id=%s, state=%s, ip=%s]",
            containerName, containerId, state, ipAddress);
    }
}