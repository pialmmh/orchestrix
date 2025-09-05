package com.telcobright.orchestrix.dto;

public class CloudDTO {
    private String name;
    private String description;
    private Integer partnerId;
    private String deploymentRegion;
    private String status;

    // Constructors
    public CloudDTO() {}

    public CloudDTO(String name, String description, Integer partnerId, String deploymentRegion, String status) {
        this.name = name;
        this.description = description;
        this.partnerId = partnerId;
        this.deploymentRegion = deploymentRegion;
        this.status = status;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(Integer partnerId) {
        this.partnerId = partnerId;
    }

    public String getDeploymentRegion() {
        return deploymentRegion;
    }

    public void setDeploymentRegion(String deploymentRegion) {
        this.deploymentRegion = deploymentRegion;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}