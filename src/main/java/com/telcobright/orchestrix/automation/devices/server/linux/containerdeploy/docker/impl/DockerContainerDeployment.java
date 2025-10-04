package com.telcobright.orchestrix.automation.devices.server.linux.containerdeploy.docker.impl;

import com.telcobright.orchestrix.automation.devices.server.linux.containerdeploy.base.*;
import com.telcobright.orchestrix.automation.core.device.SshDevice;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Docker container deployment implementation
 */
public class DockerContainerDeployment extends AbstractContainerDeployment {

    public DockerContainerDeployment(ContainerConfig config, boolean useSudo) {
        super(config, ContainerTechnology.DOCKER, useSudo);
    }

    @Override
    public boolean verifyPrerequisites(SshDevice device) throws Exception {
        String result = executeCommand(device, "docker --version");
        return result != null && result.contains("Docker");
    }

    @Override
    public boolean containerExists(SshDevice device, String containerName) throws Exception {
        String result = executeCommand(device, "docker ps -a --format '{{.Names}}'");
        return result != null && result.contains(containerName);
    }

    @Override
    public boolean startContainer(SshDevice device, String containerName) throws Exception {
        String result = executeCommand(device, "docker start " + containerName);
        return result != null && !result.contains("Error");
    }

    @Override
    public boolean stopContainer(SshDevice device, String containerName) throws Exception {
        String result = executeCommand(device, "docker stop " + containerName);
        return result != null && !result.contains("Error");
    }

    @Override
    public boolean removeContainer(SshDevice device, String containerName) throws Exception {
        stopContainer(device, containerName);
        String result = executeCommand(device, "docker rm " + containerName);
        return result != null && !result.contains("Error");
    }

    @Override
    public ContainerStatus getContainerStatus(SshDevice device, String containerName) throws Exception {
        String result = executeCommand(device,
            String.format("docker inspect %s --format '{{.State.Status}}'", containerName));

        ContainerStatus status = new ContainerStatus();
        status.setContainerName(containerName);
        if (result != null) {
            status.setState(ContainerStatus.State.fromString(result.trim()));
        }
        return status;
    }

    @Override
    public List<ContainerInfo> listContainers(SshDevice device) throws Exception {
        List<ContainerInfo> containers = new ArrayList<>();
        String result = executeCommand(device, "docker ps -a --format '{{.Names}}:{{.Image}}:{{.Status}}'");

        if (result != null) {
            for (String line : result.split("\n")) {
                String[] parts = line.split(":");
                if (parts.length >= 3) {
                    ContainerInfo info = new ContainerInfo();
                    info.setName(parts[0]);
                    info.setImage(parts[1]);
                    info.setState(parts[2]);
                    info.setTechnology(ContainerTechnology.DOCKER);
                    containers.add(info);
                }
            }
        }
        return containers;
    }

    @Override
    public boolean exportContainer(SshDevice device, String containerName, String imagePath) throws Exception {
        String result = executeCommand(device,
            String.format("docker export %s > %s", containerName, imagePath));
        return result != null && !result.contains("Error");
    }

    @Override
    public boolean importContainer(SshDevice device, String imagePath, String containerName) throws Exception {
        String imageName = containerName + ":imported";
        String result = executeCommand(device,
            String.format("docker import %s %s", imagePath, imageName));

        if (result != null && !result.contains("Error")) {
            // Create and run container from imported image
            return createDockerContainer(device, imageName, containerName);
        }
        return false;
    }

    @Override
    public String getContainerLogs(SshDevice device, String containerName, int lines) throws Exception {
        String cmd = "docker logs " + containerName;
        if (lines > 0) {
            cmd += " --tail " + lines;
        }
        return executeCommand(device, cmd);
    }

    @Override
    public String executeInContainer(SshDevice device, String containerName, String command) throws Exception {
        return executeCommand(device,
            String.format("docker exec %s %s", containerName, command));
    }

    @Override
    protected boolean configureContainer(SshDevice device, ContainerConfig config) throws Exception {
        // Docker configuration is done during container creation
        return createDockerContainer(device, config.getImagePath(), config.getContainerName());
    }

    private boolean createDockerContainer(SshDevice device, String image, String containerName) throws Exception {
        StringBuilder cmd = new StringBuilder("docker run -d");
        cmd.append(" --name ").append(containerName);

        // Add configuration from config object
        if (config.getMemoryLimit() != null) {
            cmd.append(" -m ").append(config.getMemoryLimit());
        }

        // Add port mappings
        for (Map.Entry<Integer, Integer> port : config.getPortMappings().entrySet()) {
            cmd.append(" -p ").append(port.getKey()).append(":").append(port.getValue());
        }

        // Add volumes
        for (ContainerConfig.BindMount mount : config.getBindMounts()) {
            cmd.append(" -v ").append(mount.getHostPath()).append(":").append(mount.getContainerPath());
            if (mount.isReadOnly()) {
                cmd.append(":ro");
            }
        }

        // Add environment variables
        for (Map.Entry<String, String> env : config.getEnvironment().entrySet()) {
            cmd.append(" -e ").append(env.getKey()).append("=").append(env.getValue());
        }

        cmd.append(" ").append(image);

        String result = executeCommand(device, cmd.toString());
        return result != null && !result.contains("Error");
    }
}