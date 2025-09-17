package com.orchestrix.stellar.service;

import com.orchestrix.stellar.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for handling generic entity modifications with lazy hierarchy building and caching
 */
@Service
@Slf4j
public class EntityModificationService {
    
    // Global cache for entity hierarchies
    private final ConcurrentHashMap<String, EntityHierarchy> entityMap = new ConcurrentHashMap<>();
    
    @Autowired
    private DataSource dataSource;
    
    /**
     * Process entity modification request with nested entities
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> processModification(EntityModificationRequest request) throws SQLException {
        // Build hierarchy key from request
        String hierarchyKey = buildHierarchyKey(request);
        log.info("Processing modification for hierarchy: {}", hierarchyKey);
        
        // Get or create entity hierarchy
        EntityHierarchy hierarchy = entityMap.computeIfAbsent(hierarchyKey, key -> {
            log.info("Building new hierarchy for: {}", key);
            return buildEntityHierarchy(request);
        });
        
        // Mark as used for statistics
        hierarchy.markUsed();
        log.info("Hierarchy cache hit. Usage count: {}", hierarchy.getUsageCount());
        
        // Validate hierarchy
        if (!hierarchy.isValid()) {
            throw new IllegalArgumentException("Invalid entity hierarchy: " + hierarchyKey);
        }
        
        // Execute modifications in a single transaction
        return executeInTransaction(request, hierarchy);
    }
    
    /**
     * Build hierarchy key from request structure
     */
    private String buildHierarchyKey(EntityModificationRequest request) {
        List<String> entities = new ArrayList<>();
        collectEntityNames(request, entities);
        return String.join("-", entities);
    }
    
    /**
     * Recursively collect entity names from request
     */
    private void collectEntityNames(EntityModificationRequest request, List<String> entities) {
        entities.add(request.getEntityName().toLowerCase());
        if (request.getInclude() != null) {
            for (EntityModificationRequest child : request.getInclude()) {
                collectEntityNames(child, entities);
            }
        }
    }
    
    /**
     * Build entity hierarchy metadata using reflection and schema validation
     */
    private EntityHierarchy buildEntityHierarchy(EntityModificationRequest request) {
        String key = buildHierarchyKey(request);
        EntityHierarchy hierarchy = new EntityHierarchy(key);
        
        List<EntityHierarchy.EntityLevel> levels = new ArrayList<>();
        Map<String, EntityHierarchy.ForeignKeyMapping> fkMappings = new HashMap<>();
        
        try {
            // Build levels and validate relationships
            buildLevels(request, null, levels, fkMappings);
            
            hierarchy.setLevels(levels);
            hierarchy.setForeignKeyMappings(fkMappings);
            hierarchy.setValid(true);
            
            log.info("Successfully built hierarchy: {} with {} levels", key, levels.size());
            
        } catch (Exception e) {
            log.warn("Failed to build hierarchy: {} - {}", key, e.getMessage());
            hierarchy.setValid(false);
        }
        
        return hierarchy;
    }
    
    /**
     * Recursively build entity levels and validate relationships
     */
    private void buildLevels(EntityModificationRequest request, String parentEntity,
                            List<EntityHierarchy.EntityLevel> levels,
                            Map<String, EntityHierarchy.ForeignKeyMapping> fkMappings) {
        
        String entityName = request.getEntityName().toLowerCase();
        String tableName = getTableName(entityName);
        String primaryKey = getPrimaryKey(entityName);
        
        // Create entity level
        EntityHierarchy.EntityLevel level = new EntityHierarchy.EntityLevel(
            entityName, tableName, primaryKey, null, null
        );
        
        // Get columns from database metadata
        level.setColumns(getTableColumns(tableName));
        levels.add(level);
        
        // If there's a parent, validate and create FK mapping
        if (parentEntity != null) {
            validateRelationship(parentEntity, entityName);
            String fkColumn = getForeignKeyColumn(parentEntity, entityName);
            String parentPk = getPrimaryKey(parentEntity);
            
            EntityHierarchy.ForeignKeyMapping fkMapping = new EntityHierarchy.ForeignKeyMapping(
                parentEntity, entityName, parentPk, fkColumn
            );
            fkMappings.put(parentEntity + "-" + entityName, fkMapping);
        }
        
        // Process nested entities
        if (request.getInclude() != null) {
            for (EntityModificationRequest child : request.getInclude()) {
                buildLevels(child, entityName, levels, fkMappings);
            }
        }
    }
    
    /**
     * Validate that a relationship exists between parent and child entities
     */
    private void validateRelationship(String parentEntity, String childEntity) {
        // Check against Northwind schema relationships
        Map<String, List<String>> validRelationships = Map.of(
            "category", List.of("product"),
            "product", List.of("orderdetail"),
            "customer", List.of("salesorder"),
            "salesorder", List.of("orderdetail"),
            "employee", List.of("salesorder"),
            "shipper", List.of("salesorder"),
            "supplier", List.of("product"),
            "orderdetail", List.of() // No children
        );
        
        List<String> validChildren = validRelationships.get(parentEntity);
        if (validChildren == null || !validChildren.contains(childEntity)) {
            throw new IllegalArgumentException(
                "No valid relationship from " + parentEntity + " to " + childEntity
            );
        }
    }
    
    /**
     * Execute modifications in a single transaction
     */
    private Map<String, Object> executeInTransaction(EntityModificationRequest request, 
                                                     EntityHierarchy hierarchy) throws SQLException {
        Connection conn = null;
        Map<String, Object> result = new HashMap<>();
        
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);
            
            // Execute modification recursively
            Map<String, Object> modResult = executeModification(conn, request, hierarchy, null, null);
            result.put("data", modResult);
            result.put("success", true);
            result.put("hierarchyKey", hierarchy.getKey());
            result.put("cacheStats", Map.of(
                "usageCount", hierarchy.getUsageCount(),
                "createdAt", hierarchy.getCreatedAt(),
                "lastUsedAt", hierarchy.getLastUsedAt()
            ));
            
            conn.commit();
            log.info("Transaction committed successfully for: {}", hierarchy.getKey());
            
        } catch (Exception e) {
            if (conn != null) {
                conn.rollback();
                log.warn("Transaction rolled back: {}", e.getMessage());
            }
            throw new SQLException("Transaction failed: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
        
        return result;
    }
    
    /**
     * Execute a single modification and its nested modifications
     */
    private Map<String, Object> executeModification(Connection conn, 
                                                   EntityModificationRequest request,
                                                   EntityHierarchy hierarchy,
                                                   String parentEntity,
                                                   Object parentId) throws SQLException {
        
        Map<String, Object> result = new HashMap<>();
        String entityName = request.getEntityName().toLowerCase();
        
        // Execute based on operation type
        Object entityId = null;
        switch (request.getOperation()) {
            case INSERT:
                entityId = executeInsert(conn, request, parentEntity, parentId);
                result.put("insertedId", entityId);
                result.put("operation", "INSERT");
                break;
                
            case UPDATE:
                int updatedRows = executeUpdate(conn, request, parentEntity, parentId);
                result.put("updatedRows", updatedRows);
                result.put("operation", "UPDATE");
                break;
                
            case DELETE:
                int deletedRows = executeDelete(conn, request, parentEntity, parentId);
                result.put("deletedRows", deletedRows);
                result.put("operation", "DELETE");
                break;
        }
        
        result.put("entity", entityName);
        
        // Process nested modifications
        if (request.getInclude() != null && !request.getInclude().isEmpty()) {
            List<Map<String, Object>> nestedResults = new ArrayList<>();
            for (EntityModificationRequest child : request.getInclude()) {
                Map<String, Object> childResult = executeModification(
                    conn, child, hierarchy, entityName, entityId
                );
                nestedResults.add(childResult);
            }
            result.put("nested", nestedResults);
        }
        
        return result;
    }
    
    /**
     * Execute INSERT operation
     */
    private Object executeInsert(Connection conn, EntityModificationRequest request,
                                String parentEntity, Object parentId) throws SQLException {
        
        String tableName = getTableName(request.getEntityName());
        Map<String, Object> data = new HashMap<>(request.getData());
        
        // Add foreign key if this is a child entity
        if (parentEntity != null && parentId != null) {
            String fkColumn = getForeignKeyColumn(parentEntity, request.getEntityName());
            data.put(fkColumn, parentId);
        }
        
        // Build INSERT SQL
        String columns = String.join(", ", data.keySet());
        String placeholders = data.keySet().stream()
            .map(k -> "?")
            .collect(Collectors.joining(", "));
        
        String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";
        log.debug("Executing INSERT: {}", sql);
        
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int index = 1;
            for (Object value : data.values()) {
                stmt.setObject(index++, value);
            }
            
            stmt.executeUpdate();
            
            // Get generated ID
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getObject(1);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Execute UPDATE operation
     */
    private int executeUpdate(Connection conn, EntityModificationRequest request,
                             String parentEntity, Object parentId) throws SQLException {
        
        String tableName = getTableName(request.getEntityName());
        Map<String, Object> data = request.getData();
        Map<String, Object> criteria = request.getCriteria();
        
        if (criteria == null || criteria.isEmpty()) {
            throw new IllegalArgumentException("UPDATE requires criteria");
        }
        
        // Build UPDATE SQL
        String setClause = data.keySet().stream()
            .map(k -> k + " = ?")
            .collect(Collectors.joining(", "));
        
        String whereClause = criteria.keySet().stream()
            .map(k -> k + " = ?")
            .collect(Collectors.joining(" AND "));
        
        // Add parent constraint if applicable
        if (parentEntity != null && parentId != null) {
            String fkColumn = getForeignKeyColumn(parentEntity, request.getEntityName());
            whereClause += " AND " + fkColumn + " = ?";
        }
        
        String sql = "UPDATE " + tableName + " SET " + setClause + " WHERE " + whereClause;
        log.debug("Executing UPDATE: {}", sql);
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int index = 1;
            
            // Set values
            for (Object value : data.values()) {
                stmt.setObject(index++, value);
            }
            
            // Set criteria
            for (Object value : criteria.values()) {
                stmt.setObject(index++, value);
            }
            
            // Set parent ID if applicable
            if (parentEntity != null && parentId != null) {
                stmt.setObject(index++, parentId);
            }
            
            return stmt.executeUpdate();
        }
    }
    
    /**
     * Execute DELETE operation
     */
    private int executeDelete(Connection conn, EntityModificationRequest request,
                             String parentEntity, Object parentId) throws SQLException {
        
        String tableName = getTableName(request.getEntityName());
        Map<String, Object> criteria = request.getCriteria();
        
        if (criteria == null || criteria.isEmpty()) {
            throw new IllegalArgumentException("DELETE requires criteria");
        }
        
        // Build DELETE SQL
        String whereClause = criteria.keySet().stream()
            .map(k -> k + " = ?")
            .collect(Collectors.joining(" AND "));
        
        // Add parent constraint if applicable
        if (parentEntity != null && parentId != null) {
            String fkColumn = getForeignKeyColumn(parentEntity, request.getEntityName());
            whereClause += " AND " + fkColumn + " = ?";
        }
        
        String sql = "DELETE FROM " + tableName + " WHERE " + whereClause;
        log.debug("Executing DELETE: {}", sql);
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int index = 1;
            
            // Set criteria
            for (Object value : criteria.values()) {
                stmt.setObject(index++, value);
            }
            
            // Set parent ID if applicable
            if (parentEntity != null && parentId != null) {
                stmt.setObject(index++, parentId);
            }
            
            return stmt.executeUpdate();
        }
    }
    
    /**
     * Get table name for entity
     */
    private String getTableName(String entityName) {
        // All Orchestrix tables now use singular names matching the entity names
        // Special cases for underscored table names
        Map<String, String> specialCases = Map.of(
            "availabilityzone", "availability_zone",
            "networkdevice", "network_device",
            "resourcepool", "resource_pool",
            "resourcegroup", "resource_group",
            "environmentassociation", "environment_association",
            "computeworkload", "compute_workload",
            "computecapability", "compute_capability",
            "datacenterresourcegroup", "datacenter_resource_group"
        );

        String tableName = specialCases.get(entityName.toLowerCase());
        if (tableName == null) {
            // Default: use entity name as-is (singular)
            tableName = entityName.toLowerCase();
        }
        return tableName;
    }
    
    /**
     * Get primary key column for entity
     */
    private String getPrimaryKey(String entityName) {
        // All Orchestrix entities use 'id' as primary key
        return "id";
    }
    
    /**
     * Get foreign key column linking parent to child
     */
    private String getForeignKeyColumn(String parentEntity, String childEntity) {
        String key = parentEntity.toLowerCase() + "-" + childEntity.toLowerCase();

        // Orchestrix foreign key mappings
        Map<String, String> foreignKeys = Map.of(
            "partner-cloud", "partner_id",
            "partner-environment", "partner_id",
            "cloud-region", "cloud_id",
            "cloud-datacenter", "cloud_id",
            "region-availabilityzone", "region_id",
            "availabilityzone-datacenter", "availability_zone_id",
            "datacenter-compute", "datacenter_id",
            "datacenter-networkdevice", "datacenter_id",
            "compute-container", "compute_id"
        );

        // Also check reverse relationships
        Map<String, String> additionalKeys = Map.of(
            "datacenter-resourcepool", "datacenter_id",
            "resourcepool-compute", "resource_pool_id",
            "environment-environmentassociation", "environment_id",
            "compute-computeworkload", "compute_id",
            "compute-computecapability", "compute_id"
        );

        String fk = foreignKeys.get(key);
        if (fk == null) {
            fk = additionalKeys.get(key);
        }
        if (fk == null) {
            // Default pattern: parent_entity + "_id"
            fk = parentEntity.toLowerCase() + "_id";
        }
        return fk;
    }
    
    /**
     * Get table columns from database metadata
     */
    private List<String> getTableColumns(String tableName) {
        List<String> columns = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getColumns(null, null, tableName, null)) {
                while (rs.next()) {
                    columns.add(rs.getString("COLUMN_NAME"));
                }
            }
        } catch (SQLException e) {
            log.warn("Failed to get columns for table: {}", tableName);
        }
        return columns;
    }
    
    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCachedHierarchies", entityMap.size());
        stats.put("hierarchies", entityMap.values().stream()
            .map(h -> Map.of(
                "key", h.getKey(),
                "usageCount", h.getUsageCount(),
                "lastUsedAt", h.getLastUsedAt(),
                "isValid", h.isValid()
            ))
            .collect(Collectors.toList()));
        return stats;
    }
    
    /**
     * Clear cache (for testing/maintenance)
     */
    public void clearCache() {
        entityMap.clear();
        log.info("Entity hierarchy cache cleared");
    }
}