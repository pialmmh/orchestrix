package com.orchestrix.stellar.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Represents a cached entity hierarchy with metadata
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityHierarchy {
    
    private String key;
    private List<EntityLevel> levels;
    private Map<String, ForeignKeyMapping> foreignKeyMappings;
    private boolean valid;
    private long usageCount;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
    
    public EntityHierarchy(String key) {
        this.key = key;
        this.usageCount = 0;
        this.createdAt = LocalDateTime.now();
        this.lastUsedAt = LocalDateTime.now();
        this.valid = false;
    }
    
    public void markUsed() {
        this.usageCount++;
        this.lastUsedAt = LocalDateTime.now();
    }
    
    /**
     * Represents a single level in the entity hierarchy
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntityLevel {
        private String entityName;
        private String tableName;
        private String primaryKey;
        private String parentForeignKey;
        private String parentEntity;
        private List<String> columns;
        
        public EntityLevel(String entityName, String tableName, String primaryKey, 
                          String parentForeignKey, String parentEntity) {
            this.entityName = entityName;
            this.tableName = tableName;
            this.primaryKey = primaryKey;
            this.parentForeignKey = parentForeignKey;
            this.parentEntity = parentEntity;
        }
    }
    
    /**
     * Represents a foreign key relationship between entities
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForeignKeyMapping {
        private String parentEntity;
        private String childEntity;
        private String parentPrimaryKey;
        private String childForeignKey;
    }
}
