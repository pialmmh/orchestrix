package com.telcobright.orchestrix.network.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents an attachment of a container/VM to a bridge
 */
@Entity
@Table(name = "bridge_attachments")
public class BridgeAttachment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "bridge_id", nullable = false)
    private Bridge bridge;
    
    @Column(name = "container_id")
    private String containerId; // LXC container name or Docker container ID
    
    @Column(name = "container_type")
    @Enumerated(EnumType.STRING)
    private ContainerType containerType;
    
    @Column(name = "veth_host_side")
    private String vethHostSide; // Host side of veth pair
    
    @Column(name = "veth_container_side")
    private String vethContainerSide; // Container side of veth pair (usually eth0)
    
    @Column(name = "attached_at")
    private LocalDateTime attachedAt;
    
    @PrePersist
    protected void onCreate() {
        attachedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Bridge getBridge() {
        return bridge;
    }
    
    public void setBridge(Bridge bridge) {
        this.bridge = bridge;
    }
    
    public String getContainerId() {
        return containerId;
    }
    
    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }
    
    public ContainerType getContainerType() {
        return containerType;
    }
    
    public void setContainerType(ContainerType containerType) {
        this.containerType = containerType;
    }
    
    public String getVethHostSide() {
        return vethHostSide;
    }
    
    public void setVethHostSide(String vethHostSide) {
        this.vethHostSide = vethHostSide;
    }
    
    public String getVethContainerSide() {
        return vethContainerSide;
    }
    
    public void setVethContainerSide(String vethContainerSide) {
        this.vethContainerSide = vethContainerSide;
    }
    
    public LocalDateTime getAttachedAt() {
        return attachedAt;
    }
    
    public void setAttachedAt(LocalDateTime attachedAt) {
        this.attachedAt = attachedAt;
    }
    
    public enum ContainerType {
        LXC,
        DOCKER,
        KVM,
        KUBERNETES_POD
    }
}