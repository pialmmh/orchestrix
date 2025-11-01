package com.telcobright.orchestrix.automation.routing.frr;

/**
 * Result of FRR router deployment to a single node
 */
public class FrrDeploymentResult {
    private final String host;
    private boolean success;
    private String message;
    private String containerName;

    public FrrDeploymentResult(String host) {
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    @Override
    public String toString() {
        return "FrrDeploymentResult{" +
                "host='" + host + '\'' +
                ", success=" + success +
                ", message='" + message + '\'' +
                ", containerName='" + containerName + '\'' +
                '}';
    }
}
