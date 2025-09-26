package com.telcobright.orchestrix.automation.devices.server.linux.containerdeploy.base;

/**
 * Enumeration of supported container technologies
 */
public enum ContainerTechnology {
    LXC("lxc", "Linux Containers", "lxc", "tar.gz"),
    LXD("lxd", "LXD - Next generation system container manager", "lxd", "tar.gz"),
    DOCKER("docker", "Docker Container Platform", "docker", "tar"),
    PODMAN("podman", "Podman - Daemonless container engine", "podman", "tar"),
    KUBERNETES("kubernetes", "Kubernetes Container Orchestration", "kubectl", "yaml"),
    CONTAINERD("containerd", "Industry-standard container runtime", "ctr", "tar"),
    CRI_O("cri-o", "Lightweight container runtime for Kubernetes", "crictl", "tar"),
    SYSTEMD_NSPAWN("systemd-nspawn", "Systemd's minimal container tool", "systemd-nspawn", "tar"),
    SINGULARITY("singularity", "Container platform for HPC", "singularity", "sif"),
    KATA("kata", "Kata Containers - Secure container runtime", "kata-runtime", "tar");

    private final String id;
    private final String description;
    private final String command;
    private final String imageFormat;

    ContainerTechnology(String id, String description, String command, String imageFormat) {
        this.id = id;
        this.description = description;
        this.command = command;
        this.imageFormat = imageFormat;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getCommand() {
        return command;
    }

    public String getImageFormat() {
        return imageFormat;
    }

    public boolean isOrchestrationPlatform() {
        return this == KUBERNETES;
    }

    public boolean requiresDaemon() {
        return this == DOCKER || this == CONTAINERD || this == CRI_O;
    }

    public boolean isRootless() {
        return this == PODMAN || this == SINGULARITY;
    }

    public static ContainerTechnology fromCommand(String command) {
        for (ContainerTechnology tech : values()) {
            if (tech.command.equals(command)) {
                return tech;
            }
        }
        return null;
    }
}