package com.telcobright.orchestrix.automation.cluster.kafkadocker;

import com.telcobright.orchestrix.automation.core.device.impl.RemoteSshDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Docker Deployment Automation
 *
 * Atomic automation for managing Docker containers and Docker Compose:
 * - Create directories
 * - Upload docker-compose files
 * - Start/stop/restart containers
 * - Check container status
 * - View logs
 *
 * Reusable across all tenants and deployment types.
 *
 * Usage:
 * <pre>
 * DockerDeploymentAutomation docker = new DockerDeploymentAutomation(sshDevice);
 * docker.createWorkingDirectory("~/kafka-node-1");
 * docker.uploadDockerCompose("~/kafka-node-1", dockerComposeYaml);
 * docker.startContainers("~/kafka-node-1");
 * </pre>
 *
 * @author TelcoBright Orchestrix Team
 * @since 2025-11-16
 */
public class DockerDeploymentAutomation {

    private static final Logger log = LoggerFactory.getLogger(DockerDeploymentAutomation.class);
    private final RemoteSshDevice ssh;

    public DockerDeploymentAutomation(RemoteSshDevice ssh) {
        this.ssh = ssh;
    }

    /**
     * Create working directory for Docker Compose
     *
     * @param workDir Working directory path
     */
    public void createWorkingDirectory(String workDir) throws Exception {
        log.info("Creating working directory: {}", workDir);
        ssh.executeCommand("mkdir -p " + workDir);
    }

    /**
     * Create data directory with sudo and set ownership for Kafka (uid 1000)
     *
     * @param dataDir Data directory path
     */
    public void createDataDirectory(String dataDir) throws Exception {
        log.info("Creating data directory: {}", dataDir);
        ssh.executeCommand("sudo mkdir -p " + dataDir);
        // Kafka container runs as uid 1000, so set proper ownership
        ssh.executeCommand("sudo chown -R 1000:1000 " + dataDir);
        log.info("Data directory created with uid 1000 ownership: {}", dataDir);
    }

    /**
     * Upload docker-compose.yml file
     *
     * @param workDir Working directory
     * @param dockerComposeYaml Docker Compose YAML content
     */
    public void uploadDockerCompose(String workDir, String dockerComposeYaml) throws Exception {
        log.info("Uploading docker-compose.yml to {}", workDir);

        // Escape single quotes in YAML content
        String escapedYaml = dockerComposeYaml.replace("'", "'\\''");

        // Upload using heredoc
        String command = String.format("cat > %s/docker-compose.yml << 'EOF'\n%s\nEOF",
                workDir, dockerComposeYaml);

        ssh.executeCommand(command);
        log.info("docker-compose.yml uploaded successfully");
    }

    /**
     * Verify docker-compose.yml exists
     *
     * @param workDir Working directory
     * @return true if file exists
     */
    public boolean verifyDockerComposeExists(String workDir) throws Exception {
        String output = ssh.executeCommand("test -f " + workDir + "/docker-compose.yml && echo 'EXISTS' || echo 'NOT_FOUND'");
        return output.trim().equals("EXISTS");
    }

    /**
     * Display docker-compose.yml content
     *
     * @param workDir Working directory
     * @return Docker Compose file content
     */
    public String showDockerCompose(String workDir) throws Exception {
        return ssh.executeCommand("cat " + workDir + "/docker-compose.yml");
    }

    /**
     * Start containers using docker compose
     *
     * @param workDir Working directory containing docker-compose.yml
     */
    public void startContainers(String workDir) throws Exception {
        log.info("Starting containers in {}", workDir);
        String command = String.format("cd %s && sudo docker compose up -d", workDir);
        String output = ssh.executeCommand(command);
        log.info("Containers started: {}", output.trim());
    }

    /**
     * Stop containers using docker compose
     *
     * @param workDir Working directory containing docker-compose.yml
     */
    public void stopContainers(String workDir) throws Exception {
        log.info("Stopping containers in {}", workDir);
        String command = String.format("cd %s && sudo docker compose down", workDir);
        String output = ssh.executeCommand(command);
        log.info("Containers stopped: {}", output.trim());
    }

    /**
     * Restart containers using docker compose
     *
     * @param workDir Working directory containing docker-compose.yml
     */
    public void restartContainers(String workDir) throws Exception {
        log.info("Restarting containers in {}", workDir);
        String command = String.format("cd %s && sudo docker compose restart", workDir);
        String output = ssh.executeCommand(command);
        log.info("Containers restarted: {}", output.trim());
    }

    /**
     * Check container status
     *
     * @param containerName Container name (optional, null for all)
     * @return Container status output
     */
    public String getContainerStatus(String containerName) throws Exception {
        String command;
        if (containerName != null) {
            command = "docker ps | grep " + containerName;
        } else {
            command = "docker ps";
        }

        return ssh.executeCommand(command);
    }

    /**
     * Check if container is running
     *
     * @param containerName Container name
     * @return true if container is running
     */
    public boolean isContainerRunning(String containerName) throws Exception {
        String status = getContainerStatus(containerName);
        return status.contains(containerName) && status.contains("Up");
    }

    /**
     * Get container logs
     *
     * @param containerName Container name
     * @param lines Number of lines to tail (0 for all)
     * @return Container logs
     */
    public String getContainerLogs(String containerName, int lines) throws Exception {
        String command;
        if (lines > 0) {
            command = String.format("sudo docker logs --tail %d %s", lines, containerName);
        } else {
            command = "sudo docker logs " + containerName;
        }

        return ssh.executeCommand(command);
    }

    /**
     * Execute command inside container
     *
     * @param containerName Container name
     * @param command Command to execute
     * @return Command output
     */
    public String execInContainer(String containerName, String command) throws Exception {
        String dockerExecCmd = String.format("sudo docker exec %s %s", containerName, command);
        return ssh.executeCommand(dockerExecCmd);
    }

    /**
     * Wait for container to be healthy
     *
     * @param containerName Container name
     * @param maxWaitSeconds Maximum wait time in seconds
     * @return true if container became healthy within timeout
     */
    public boolean waitForContainer(String containerName, int maxWaitSeconds) throws Exception {
        log.info("Waiting for container {} (max {} seconds)", containerName, maxWaitSeconds);

        for (int i = 0; i < maxWaitSeconds; i++) {
            if (isContainerRunning(containerName)) {
                log.info("Container {} is running", containerName);
                return true;
            }

            if (i % 5 == 0 && i > 0) {
                log.debug("Still waiting for {} ({}/{}s)", containerName, i, maxWaitSeconds);
            }

            Thread.sleep(1000);
        }

        log.warn("Container {} did not become healthy within {} seconds", containerName, maxWaitSeconds);
        return false;
    }

    /**
     * Remove container
     *
     * @param containerName Container name
     * @param force Force removal even if running
     */
    public void removeContainer(String containerName, boolean force) throws Exception {
        log.info("Removing container: {}", containerName);
        String command = force ?
                "sudo docker rm -f " + containerName :
                "sudo docker rm " + containerName;

        ssh.executeCommand(command);
        log.info("Container {} removed", containerName);
    }

    /**
     * Verify Docker is installed
     *
     * @return Docker version string
     */
    public String verifyDocker() throws Exception {
        return ssh.executeCommand("docker --version");
    }

    /**
     * Verify Docker Compose is installed
     *
     * @return Docker Compose version string
     */
    public String verifyDockerCompose() throws Exception {
        return ssh.executeCommand("docker compose version || docker-compose --version");
    }
}
