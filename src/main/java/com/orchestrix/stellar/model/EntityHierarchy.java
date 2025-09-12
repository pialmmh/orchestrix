package com.orchestrix.stellar.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * Cached entity hierarchy metadata for efficient processing
 */
@Data
public class EntityHierarchy {
    private String key; // e.g., "category-product" or "customer-salesorder-orderdetail"
    private List<EntityLevel> levels;
    private Map<String, ForeignKeyMapping> foreignKeyMappings;
    private boolean isValid;
    private long createdAt;
    private long lastUsedAt;
    private int usageCount;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EntityLevel {
        private String entityName;
        private String tableName;
        private String primaryKey;
        private List<String> columns;
        private Map<String, Class<?>> columnTypes;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ForeignKeyMapping {
        private String parentEntity;
        private String childEntity;
        private String parentColumn;
        private String childColumn;
    }
    
    // Constructor
    public EntityHierarchy(String key) {
        this.key = key;
        this.createdAt = System.currentTimeMillis();
        this.lastUsedAt = this.createdAt;
        this.usageCount = 0;
    }
    
    // Method to mark usage
    public void markUsed() {
        this.lastUsedAt = System.currentTimeMillis();
        this.usageCount++;
    }
}