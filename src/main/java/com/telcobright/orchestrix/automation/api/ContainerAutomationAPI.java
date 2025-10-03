package com.telcobright.orchestrix.automation.api;

import com.telcobright.orchestrix.automation.api.model.AutomationOperationResult;

/**
 * API for container automation operations.
 *
 * <p>This interface provides methods for building and managing containers
 * across different technologies (LXC, Docker, Kubernetes, Podman).
 *
 * <p>Container building follows a two-phase pattern:
 * <ul>
 *   <li>Base containers: Infrastructure with tools, runtimes, and libraries</li>
 *   <li>App containers: Derived from base, includes application code</li>
 * </ul>
 */
public interface ContainerAutomationAPI {

    /**
     * Build a base container with infrastructure components.
     *
     * <p>Base containers include:
     * <ul>
     *   <li>Operating system (Debian, Ubuntu, etc.)</li>
     *   <li>Runtime environments (JVM, Python, Node.js, etc.)</li>
     *   <li>Build tools (Maven, Gradle, npm, etc.)</li>
     *   <li>Monitoring and logging infrastructure (Promtail, metrics exporters)</li>
     *   <li>System packages and utilities</li>
     * </ul>
     *
     * <p>Base containers are reusable - multiple app containers can extend one base.
     *
     * @param configFile Path to configuration file with build parameters
     * @return Result of the build operation including container name and status
     * @throws Exception if build fails or configuration is invalid
     *
     * @example
     * <pre>
     * ContainerAutomationAPI api = new ContainerAutomationAPIImpl();
     * AutomationOperationResult result = api.buildBaseContainer("/path/to/build.conf");
     * if (result.isSuccess()) {
     *     System.out.println("Base container created: " + result.getMessage());
     * }
     * </pre>
     */
    AutomationOperationResult buildBaseContainer(String configFile) throws Exception;

    /**
     * Build an application container from a base container.
     *
     * <p>App containers extend a base container and add:
     * <ul>
     *   <li>Application JAR/WAR/binary</li>
     *   <li>Application-specific configuration</li>
     *   <li>Environment variables</li>
     *   <li>Port mappings</li>
     *   <li>Volume mounts</li>
     *   <li>Service configuration (systemd)</li>
     * </ul>
     *
     * <p>App containers are lightweight since they inherit infrastructure from base.
     *
     * @param configFile Path to configuration file with app parameters
     * @return Result of the build operation including container name and status
     * @throws Exception if build fails, base container not found, or app JAR missing
     *
     * @example
     * <pre>
     * ContainerAutomationAPI api = new ContainerAutomationAPIImpl();
     * AutomationOperationResult result = api.buildAppContainer("/path/to/app-build.conf");
     * if (result.isSuccess()) {
     *     System.out.println("App container created: " + result.getMessage());
     * }
     * </pre>
     */
    AutomationOperationResult buildAppContainer(String configFile) throws Exception;

    /**
     * Start a container.
     *
     * @param containerName Name of the container to start
     * @return Result of the start operation
     * @throws Exception if container not found or start fails
     */
    AutomationOperationResult startContainer(String containerName) throws Exception;

    /**
     * Stop a container.
     *
     * @param containerName Name of the container to stop
     * @return Result of the stop operation
     * @throws Exception if container not found or stop fails
     */
    AutomationOperationResult stopContainer(String containerName) throws Exception;

    /**
     * Delete a container.
     *
     * @param containerName Name of the container to delete
     * @param force Whether to force deletion even if container is running
     * @return Result of the delete operation
     * @throws Exception if container not found or deletion fails
     */
    AutomationOperationResult deleteContainer(String containerName, boolean force) throws Exception;

    /**
     * Get container status and information.
     *
     * @param containerName Name of the container to query
     * @return Result containing container status, IP address, resource usage, etc.
     * @throws Exception if container not found or query fails
     */
    AutomationOperationResult getContainerInfo(String containerName) throws Exception;

    /**
     * Execute a command inside a container.
     *
     * @param containerName Name of the container
     * @param command Command to execute
     * @return Result containing command output
     * @throws Exception if container not found or command execution fails
     */
    AutomationOperationResult executeCommand(String containerName, String command) throws Exception;

    /**
     * Export a container as an image for reuse.
     *
     * @param containerName Name of the container to export
     * @param imageName Name for the exported image
     * @param exportPath Path where to save the exported image
     * @return Result of the export operation
     * @throws Exception if export fails
     */
    AutomationOperationResult exportContainer(String containerName, String imageName, String exportPath) throws Exception;
}
