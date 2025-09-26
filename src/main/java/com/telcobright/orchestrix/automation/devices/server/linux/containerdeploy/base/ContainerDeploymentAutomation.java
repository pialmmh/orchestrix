package com.telcobright.orchestrix.automation.devices.server.linux.containerdeploy.base;

import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxAutomation;
import com.telcobright.orchestrix.device.SshDevice;
import java.util.List;
import java.util.Map;

/**
 * Base interface for all container deployment automations
 * Supports LXC, Docker, Podman, Kubernetes, and other container technologies
 */
public interface ContainerDeploymentAutomation extends LinuxAutomation {

    /**
     * Deploy a container from an image
     */
    boolean deployContainer(SshDevice device, ContainerConfig config) throws Exception;

    /**
     * Check if container exists
     */
    boolean containerExists(SshDevice device, String containerName) throws Exception;

    /**
     * Start a deployed container
     */
    boolean startContainer(SshDevice device, String containerName) throws Exception;

    /**
     * Stop a running container
     */
    boolean stopContainer(SshDevice device, String containerName) throws Exception;

    /**
     * Remove a container
     */
    boolean removeContainer(SshDevice device, String containerName) throws Exception;

    /**
     * Get container status
     */
    ContainerStatus getContainerStatus(SshDevice device, String containerName) throws Exception;

    /**
     * List all containers
     */
    List<ContainerInfo> listContainers(SshDevice device) throws Exception;

    /**
     * Export container as image
     */
    boolean exportContainer(SshDevice device, String containerName, String imagePath) throws Exception;

    /**
     * Import container from image
     */
    boolean importContainer(SshDevice device, String imagePath, String containerName) throws Exception;

    /**
     * Get container logs
     */
    String getContainerLogs(SshDevice device, String containerName, int lines) throws Exception;

    /**
     * Execute command in container
     */
    String executeInContainer(SshDevice device, String containerName, String command) throws Exception;

    /**
     * Get container technology type
     */
    ContainerTechnology getContainerTechnology();

    /**
     * Verify deployment prerequisites
     */
    boolean verifyPrerequisites(SshDevice device) throws Exception;
}