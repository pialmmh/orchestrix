package com.telcobright.orchestrix.automation.core.storage.base;

/**
 * Represents a storage location with unique ID and path
 */
public class StorageLocation {
    private String id;
    private String path;
    private StorageTechnology technology;
    private StorageType storageType;
    private String description;

    public enum StorageType {
        SSD("ssd", "Solid State Drive"),
        SATA("sata", "SATA Hard Drive"),
        NVME("nvme", "NVMe Drive"),
        NETWORK("network", "Network Storage"),
        HYBRID("hybrid", "Hybrid Storage");

        private final String code;
        private final String description;

        StorageType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static StorageType fromCode(String code) {
            for (StorageType type : values()) {
                if (type.code.equalsIgnoreCase(code)) {
                    return type;
                }
            }
            return SATA; // Default
        }
    }

    public StorageLocation() {}

    public StorageLocation(String id, String path, StorageTechnology technology, StorageType storageType) {
        this.id = id;
        this.path = path;
        this.technology = technology;
        this.storageType = storageType;
    }

    // Builder pattern for convenient construction
    public static class Builder {
        private String id;
        private String path;
        private StorageTechnology technology = StorageTechnology.BTRFS;
        private StorageType storageType = StorageType.SATA;
        private String description;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder technology(StorageTechnology technology) {
            this.technology = technology;
            return this;
        }

        public Builder storageType(StorageType storageType) {
            this.storageType = storageType;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public StorageLocation build() {
            StorageLocation location = new StorageLocation();
            location.id = this.id;
            location.path = this.path;
            location.technology = this.technology;
            location.storageType = this.storageType;
            location.description = this.description;
            return location;
        }
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public StorageTechnology getTechnology() {
        return technology;
    }

    public void setTechnology(StorageTechnology technology) {
        this.technology = technology;
    }

    public StorageType getStorageType() {
        return storageType;
    }

    public void setStorageType(StorageType storageType) {
        this.storageType = storageType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return String.format("StorageLocation[id=%s, path=%s, tech=%s, type=%s]",
            id, path, technology.getCode(), storageType.getCode());
    }
}