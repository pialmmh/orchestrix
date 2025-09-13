package com.telcobright.orchestrix.network.entity.lxc;

import javax.persistence.*;

/**
 * LXC Container Mount Point Configuration
 */
@Entity
@Table(name = "lxc_mounts")
public class LxcMount {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "container_id", nullable = false)
    private LxcContainer container;
    
    @Column(name = "mount_name", nullable = false)
    private String mountName; // e.g., "workspace", "config", "data"
    
    @Column(name = "host_path", nullable = false)
    private String hostPath; // Path on host machine
    
    @Column(name = "container_path", nullable = false)
    private String containerPath; // Path inside container
    
    @Column(name = "mount_type")
    @Enumerated(EnumType.STRING)
    private MountType mountType = MountType.DISK;
    
    @Column(name = "readonly")
    private Boolean readonly = false;
    
    @Column(name = "create_if_missing")
    private Boolean createIfMissing = true;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public LxcContainer getContainer() {
        return container;
    }
    
    public void setContainer(LxcContainer container) {
        this.container = container;
    }
    
    public String getMountName() {
        return mountName;
    }
    
    public void setMountName(String mountName) {
        this.mountName = mountName;
    }
    
    public String getHostPath() {
        return hostPath;
    }
    
    public void setHostPath(String hostPath) {
        this.hostPath = hostPath;
    }
    
    public String getContainerPath() {
        return containerPath;
    }
    
    public void setContainerPath(String containerPath) {
        this.containerPath = containerPath;
    }
    
    public MountType getMountType() {
        return mountType;
    }
    
    public void setMountType(MountType mountType) {
        this.mountType = mountType;
    }
    
    public Boolean getReadonly() {
        return readonly;
    }
    
    public void setReadonly(Boolean readonly) {
        this.readonly = readonly;
    }
    
    public Boolean getCreateIfMissing() {
        return createIfMissing;
    }
    
    public void setCreateIfMissing(Boolean createIfMissing) {
        this.createIfMissing = createIfMissing;
    }
    
    public enum MountType {
        DISK,
        FILE,
        DEVICE
    }
}