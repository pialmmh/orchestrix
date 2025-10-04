package com.telcobright.orchestrix.automation.devices.server.linux.containerdeploy.lxc.impl;

import com.telcobright.orchestrix.automation.devices.server.linux.containerdeploy.base.*;
import com.telcobright.orchestrix.automation.devices.server.linux.containerdeploy.lxc.preparer.LxcPrerequisitePreparer;
import com.telcobright.orchestrix.automation.devices.server.linux.base.LinuxDistribution;
import com.telcobright.orchestrix.automation.core.device.SshDevice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LXC/LXD container deployment implementation
 */
public class LxcContainerDeployment extends AbstractContainerDeployment {

    private final LxcPrerequisitePreparer prerequisitePreparer;
    private final LxcPrerequisitePreparer.PrerequisiteConfig prerequisiteConfig;

    public LxcContainerDeployment(ContainerConfig config, boolean useSudo) {
        super(config, ContainerTechnology.LXC, useSudo);
        this.prerequisitePreparer = null;
        this.prerequisiteConfig = null;
    }

    public LxcContainerDeployment(ContainerConfig config,
                                  LxcPrerequisitePreparer.PrerequisiteConfig prerequisiteConfig,
                                  boolean useSudo) {
        super(config, ContainerTechnology.LXC, useSudo);
        this.prerequisitePreparer = new LxcPrerequisitePreparer(LinuxDistribution.UNKNOWN, useSudo);
        this.prerequisiteConfig = prerequisiteConfig;
    }

    @Override
    public boolean verifyPrerequisites(SshDevice device) throws Exception {
        // Check if LXC/LXD is installed
        String result = executeCommand(device, "which lxc");
        if (result == null || result.isEmpty()) {
            // Try LXD
            result = executeCommand(device, "which lxd");
            if (result == null || result.isEmpty()) {
                logger.warning("Neither lxc nor lxd command found");
                return false;
            }
        }

        // Check if LXD is initialized
        result = executeCommand(device, "lxc profile show default 2>/dev/null");
        if (result == null || result.contains("error")) {
            logger.warning("LXD not initialized");
            return false;
        }

        return true;
    }

    @Override
    public boolean deployContainer(SshDevice device, ContainerConfig config) throws Exception {
        // If prerequisite config is provided, prepare environment first
        if (prerequisitePreparer != null && prerequisiteConfig != null) {
            logger.info("Preparing prerequisites for LXC deployment");
            LxcPrerequisitePreparer.PrerequisiteReport report =
                prerequisitePreparer.checkAndPrepare(device, prerequisiteConfig);

            if (!report.isSuccess()) {
                logger.severe("Prerequisites preparation failed:\n" + report);
                return false;
            }
            logger.info("Prerequisites prepared successfully");
        }

        // Continue with standard deployment
        return super.deployContainer(device, config);
    }

    @Override
    public boolean containerExists(SshDevice device, String containerName) throws Exception {
        String result = executeCommand(device, "lxc list --format csv");
        return result != null && result.contains(containerName);
    }

    @Override
    public boolean startContainer(SshDevice device, String containerName) throws Exception {
        String result = executeCommand(device, "lxc start " + containerName);
        return result != null && !result.contains("error");
    }

    @Override
    public boolean stopContainer(SshDevice device, String containerName) throws Exception {
        String result = executeCommand(device, "lxc stop " + containerName);
        return result != null && !result.contains("error");
    }

    @Override
    public boolean removeContainer(SshDevice device, String containerName) throws Exception {
        // Stop container first if running
        ContainerStatus status = getContainerStatus(device, containerName);
        if (status != null && status.isRunning()) {
            stopContainer(device, containerName);
            // Wait for container to stop
            Thread.sleep(2000);
        }

        String result = executeCommand(device, "lxc delete " + containerName);
        return result != null && !result.contains("error");
    }

    @Override
    public ContainerStatus getContainerStatus(SshDevice device, String containerName)
            throws Exception {

        String result = executeCommand(device,
            String.format("lxc list %s --format csv", containerName));

        if (result == null || result.isEmpty()) {
            return null;
        }

        ContainerStatus status = new ContainerStatus();
        status.setContainerName(containerName);

        // Parse CSV: name,state,ipv4,ipv6,type,snapshots
        String[] parts = result.split(",");
        if (parts.length >= 2) {
            status.setState(ContainerStatus.State.fromString(parts[1]));
        }
        if (parts.length >= 3 && !parts[2].isEmpty()) {
            // Extract IP from format like "10.0.3.100 (eth0)"
            String ipField = parts[2];
            Pattern ipPattern = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)");
            Matcher matcher = ipPattern.matcher(ipField);
            if (matcher.find()) {
                status.setIpAddress(matcher.group(1));
            }
        }

        // Get additional info
        result = executeCommand(device,
            String.format("lxc info %s 2>/dev/null", containerName));
        if (result != null) {
            // Parse PID
            Pattern pidPattern = Pattern.compile("Pid:\\s*(\\d+)");
            Matcher pidMatcher = pidPattern.matcher(result);
            if (pidMatcher.find()) {
                status.setPid(Long.parseLong(pidMatcher.group(1)));
            }

            // Parse memory usage
            Pattern memPattern = Pattern.compile("Memory \\(current\\):\\s*([\\d.]+)([KMG]?)B");
            Matcher memMatcher = memPattern.matcher(result);
            if (memMatcher.find()) {
                long memUsage = parseSize(memMatcher.group(1), memMatcher.group(2));
                status.setMemoryUsage(memUsage);
            }
        }

        return status;
    }

    @Override
    public List<ContainerInfo> listContainers(SshDevice device) throws Exception {
        List<ContainerInfo> containers = new ArrayList<>();

        String result = executeCommand(device, "lxc list --format csv");
        if (result == null || result.isEmpty()) {
            return containers;
        }

        String[] lines = result.split("\n");
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;

            String[] parts = line.split(",");
            if (parts.length >= 2) {
                ContainerInfo info = new ContainerInfo();
                info.setName(parts[0]);
                info.setState(parts[1]);
                info.setTechnology(ContainerTechnology.LXC);

                if (parts.length >= 3 && !parts[2].isEmpty()) {
                    Pattern ipPattern = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)");
                    Matcher matcher = ipPattern.matcher(parts[2]);
                    if (matcher.find()) {
                        info.setIpAddress(matcher.group(1));
                    }
                }

                containers.add(info);
            }
        }

        return containers;
    }

    @Override
    public boolean exportContainer(SshDevice device, String containerName, String imagePath)
            throws Exception {

        // Stop container if running
        ContainerStatus status = getContainerStatus(device, containerName);
        if (status != null && status.isRunning()) {
            logger.info("Stopping container for export");
            stopContainer(device, containerName);
            Thread.sleep(2000);
        }

        // Export container
        String result = executeCommand(device,
            String.format("lxc export %s %s", containerName, imagePath));

        // Restart if was running
        if (status != null && status.isRunning()) {
            startContainer(device, containerName);
        }

        return result != null && !result.contains("error");
    }

    @Override
    public boolean importContainer(SshDevice device, String imagePath, String containerName)
            throws Exception {

        // Import as image first
        String imageName = "imported-" + System.currentTimeMillis();
        String importCmd = String.format("lxc image import %s --alias %s", imagePath, imageName);
        String result = executeCommand(device, importCmd);

        if (result == null || result.contains("error")) {
            logger.severe("Failed to import image");
            return false;
        }

        // Launch container from imported image
        String launchCmd = String.format("lxc launch %s %s", imageName, containerName);
        result = executeCommand(device, launchCmd);

        // Clean up imported image
        executeCommand(device, "lxc image delete " + imageName);

        return result != null && !result.contains("error");
    }

    @Override
    public String getContainerLogs(SshDevice device, String containerName, int lines)
            throws Exception {

        String cmd = String.format("lxc console %s --show-log", containerName);
        if (lines > 0) {
            cmd += " | tail -n " + lines;
        }
        return executeCommand(device, cmd);
    }

    @Override
    public String executeInContainer(SshDevice device, String containerName, String command)
            throws Exception {

        String cmd = String.format("lxc exec %s -- %s", containerName, command);
        return executeCommand(device, cmd);
    }

    @Override
    protected boolean configureContainer(SshDevice device, ContainerConfig config)
            throws Exception {

        String containerName = config.getContainerName();

        // Configure network
        if (config.getIpAddress() != null) {
            String netCmd = String.format(
                "lxc config device override %s eth0 ipv4.address=%s",
                containerName, config.getIpAddress()
            );
            executeCommand(device, netCmd);
        }

        // Configure resource limits
        if (config.getMemoryLimit() != null) {
            String memCmd = String.format(
                "lxc config set %s limits.memory %d",
                containerName, config.getMemoryLimit()
            );
            executeCommand(device, memCmd);
        }

        if (config.getCpuShares() != null) {
            String cpuCmd = String.format(
                "lxc config set %s limits.cpu %d",
                containerName, config.getCpuShares()
            );
            executeCommand(device, cpuCmd);
        }

        // Configure bind mounts
        for (ContainerConfig.BindMount mount : config.getBindMounts()) {
            String mountName = "mount" + mount.getContainerPath().replace("/", "-");
            String mountCmd = String.format(
                "lxc config device add %s %s disk source=%s path=%s",
                containerName, mountName, mount.getHostPath(), mount.getContainerPath()
            );
            executeCommand(device, mountCmd);
        }

        // Set environment variables
        for (Map.Entry<String, String> env : config.getEnvironment().entrySet()) {
            String envCmd = String.format(
                "lxc config set %s environment.%s=%s",
                containerName, env.getKey(), env.getValue()
            );
            executeCommand(device, envCmd);
        }

        // Configure autostart
        if (config.isAutoStart()) {
            executeCommand(device,
                String.format("lxc config set %s boot.autostart true", containerName));
        }

        return true;
    }

    private long parseSize(String value, String unit) {
        double val = Double.parseDouble(value);
        switch (unit) {
            case "G":
                return (long) (val * 1024 * 1024 * 1024);
            case "M":
                return (long) (val * 1024 * 1024);
            case "K":
                return (long) (val * 1024);
            default:
                return (long) val;
        }
    }
}