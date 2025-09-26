package com.telcobright.orchestrix.automation.devices.server.linux.containerdeploy.base;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * Container information for listing containers
 */
public class ContainerInfo {
    private String name;
    private String id;
    private String image;
    private String state;
    private LocalDateTime created;
    private String ipAddress;
    private Map<String, String> labels = new HashMap<>();
    private ContainerTechnology technology;

    public ContainerInfo() {}

    public ContainerInfo(String name, String id, String image, String state) {
        this.name = name;
        this.id = id;
        this.image = image;
        this.state = state;
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public LocalDateTime getCreated() { return created; }
    public void setCreated(LocalDateTime created) { this.created = created; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public Map<String, String> getLabels() { return labels; }
    public void setLabels(Map<String, String> labels) { this.labels = labels; }

    public ContainerTechnology getTechnology() { return technology; }
    public void setTechnology(ContainerTechnology technology) { this.technology = technology; }

    @Override
    public String toString() {
        return String.format("%s (%s) - %s [%s]", name, id, image, state);
    }
}