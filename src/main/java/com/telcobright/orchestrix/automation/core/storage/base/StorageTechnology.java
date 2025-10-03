package com.telcobright.orchestrix.automation.core.storage.base;

/**
 * Enum representing different storage technologies
 */
public enum StorageTechnology {
    BTRFS("btrfs", "B-tree File System", true, true, true),
    LVM("lvm", "Logical Volume Manager", true, true, false),
    ZFS("zfs", "Z File System", true, true, true),
    CEPH("ceph", "Ceph Distributed Storage", true, true, true),
    EXT4("ext4", "Fourth Extended Filesystem", false, false, false),
    XFS("xfs", "XFS Filesystem", false, false, false),
    NFS("nfs", "Network File System", false, false, false),
    GLUSTERFS("glusterfs", "Gluster File System", true, true, false);

    private final String code;
    private final String description;
    private final boolean supportsSnapshots;
    private final boolean supportsQuotas;
    private final boolean supportsCompression;

    StorageTechnology(String code, String description, boolean supportsSnapshots,
                      boolean supportsQuotas, boolean supportsCompression) {
        this.code = code;
        this.description = description;
        this.supportsSnapshots = supportsSnapshots;
        this.supportsQuotas = supportsQuotas;
        this.supportsCompression = supportsCompression;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public boolean supportsSnapshots() {
        return supportsSnapshots;
    }

    public boolean supportsQuotas() {
        return supportsQuotas;
    }

    public boolean supportsCompression() {
        return supportsCompression;
    }

    public static StorageTechnology fromCode(String code) {
        for (StorageTechnology tech : values()) {
            if (tech.code.equalsIgnoreCase(code)) {
                return tech;
            }
        }
        throw new IllegalArgumentException("Unknown storage technology: " + code);
    }
}