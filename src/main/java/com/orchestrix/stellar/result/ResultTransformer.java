package com.orchestrix.stellar.result;

import com.orchestrix.stellar.model.QueryNode;
import com.orchestrix.stellar.schema.SchemaMeta;
import com.orchestrix.stellar.schema.TableMeta;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Transforms flat SQL results with prefixed column names (e.g., p__id, c__name)
 * into hierarchical nested objects with clean field names.
 */
public class ResultTransformer {
    private final SchemaMeta schemaMeta;
    
    public ResultTransformer(SchemaMeta schemaMeta) {
        this.schemaMeta = schemaMeta;
    }
    
    /**
     * Transform flat rows to hierarchical structure based on query node structure
     */
    public List<Map<String, Object>> transform(List<FlatRow> flatRows, QueryNode queryNode) {
        if (flatRows == null || flatRows.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Group rows by root entity
        Map<Object, List<FlatRow>> groupedRows = groupByRootEntity(flatRows, queryNode);
        
        // Transform each group into a hierarchical object
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map.Entry<Object, List<FlatRow>> entry : groupedRows.entrySet()) {
            Map<String, Object> rootObject = buildHierarchicalObject(entry.getValue(), queryNode);
            if (rootObject != null && !rootObject.isEmpty()) {
                results.add(rootObject);
            }
        }
        
        return results;
    }
    
    /**
     * Group flat rows by root entity ID
     */
    private Map<Object, List<FlatRow>> groupByRootEntity(List<FlatRow> flatRows, QueryNode queryNode) {
        TableMeta tableMeta = schemaMeta.nodes().get(queryNode.kind);
        String prefix = tableMeta.alias() + "__";
        String idColumn = prefix + "id";
        
        Map<Object, List<FlatRow>> grouped = new LinkedHashMap<>();
        for (FlatRow row : flatRows) {
            Object rootId = row.get(idColumn);
            if (rootId != null) {
                grouped.computeIfAbsent(rootId, k -> new ArrayList<>()).add(row);
            }
        }
        
        return grouped;
    }
    
    /**
     * Build a hierarchical object from a group of flat rows
     */
    private Map<String, Object> buildHierarchicalObject(List<FlatRow> rows, QueryNode queryNode) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        
        FlatRow firstRow = rows.get(0);
        TableMeta tableMeta = schemaMeta.nodes().get(queryNode.kind);
        String prefix = tableMeta.alias() + "__";
        
        // Extract fields for this entity
        Map<String, Object> result = new LinkedHashMap<>();
        for (String column : tableMeta.columns()) {
            String prefixedColumn = prefix + column;
            Object value = firstRow.get(prefixedColumn);
            if (value != null) {
                String cleanFieldName = snakeToCamelCase(column);
                result.put(cleanFieldName, transformValue(value));
            }
        }
        
        // Process includes (nested entities)
        if (queryNode.include != null && !queryNode.include.isEmpty()) {
            for (QueryNode childNode : queryNode.include) {
                // Skip lazy-loaded children
                if (childNode.lazy) {
                    continue;
                }
                
                List<Map<String, Object>> childObjects = buildChildObjects(rows, childNode);
                if (!childObjects.isEmpty()) {
                    String fieldName = pluralizeName(childNode.kind.name().toLowerCase());
                    result.put(fieldName, childObjects);
                }
            }
        }
        
        return result;
    }
    
    /**
     * Build child objects from flat rows
     */
    private List<Map<String, Object>> buildChildObjects(List<FlatRow> rows, QueryNode childNode) {
        TableMeta childTableMeta = schemaMeta.nodes().get(childNode.kind);
        String childPrefix = childTableMeta.alias() + "__";
        String childIdColumn = childPrefix + "id";
        
        // Use LinkedHashMap to preserve order and eliminate duplicates
        Map<Object, Map<String, Object>> childMap = new LinkedHashMap<>();
        
        for (FlatRow row : rows) {
            Object childId = row.get(childIdColumn);
            if (childId != null && !childMap.containsKey(childId)) {
                Map<String, Object> childObject = new LinkedHashMap<>();
                
                // Extract fields for child entity
                for (String column : childTableMeta.columns()) {
                    String prefixedColumn = childPrefix + column;
                    Object value = row.get(prefixedColumn);
                    if (value != null) {
                        String cleanFieldName = snakeToCamelCase(column);
                        childObject.put(cleanFieldName, transformValue(value));
                    }
                }
                
                // Recursively process nested includes for this child
                if (childNode.include != null && !childNode.include.isEmpty()) {
                    // Filter rows that belong to this child entity
                    List<FlatRow> childRows = rows.stream()
                        .filter(r -> childId.equals(r.get(childIdColumn)))
                        .collect(Collectors.toList());
                    
                    for (QueryNode grandchildNode : childNode.include) {
                        if (!grandchildNode.lazy) {
                            List<Map<String, Object>> grandchildObjects = buildChildObjects(childRows, grandchildNode);
                            if (!grandchildObjects.isEmpty()) {
                                String fieldName = pluralizeName(grandchildNode.kind.name().toLowerCase());
                                childObject.put(fieldName, grandchildObjects);
                            }
                        }
                    }
                }
                
                childMap.put(childId, childObject);
            }
        }
        
        return new ArrayList<>(childMap.values());
    }
    
    /**
     * Convert snake_case to camelCase
     */
    private String snakeToCamelCase(String snakeCase) {
        if (snakeCase == null || snakeCase.isEmpty()) {
            return snakeCase;
        }
        
        // Handle special cases
        if (snakeCase.equals("id")) {
            return "id";
        }
        
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;
        
        for (int i = 0; i < snakeCase.length(); i++) {
            char c = snakeCase.charAt(i);
            if (c == '_') {
                capitalizeNext = true;
            } else {
                if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }
        }
        
        return result.toString();
    }
    
    /**
     * Simple pluralization for collection field names
     */
    private String pluralizeName(String name) {
        String camelName = snakeToCamelCase(name);
        
        // Handle special cases for Orchestrix entities
        Map<String, String> specialPlurals = Map.of(
            "partner", "partners",
            "cloud", "clouds",
            "datacenter", "datacenters",
            "compute", "computes",
            "networkdevice", "networkDevices",
            "networkDevice", "networkDevices",
            "category", "categories",
            "country", "countries",
            "city", "cities",
            "availabilityzone", "availabilityZones"
        );
        
        String plural = specialPlurals.get(camelName);
        if (plural != null) {
            return plural;
        }
        
        // Default pluralization
        if (camelName.endsWith("y") && !camelName.endsWith("ay") && !camelName.endsWith("ey") && !camelName.endsWith("oy")) {
            return camelName.substring(0, camelName.length() - 1) + "ies";
        } else if (camelName.endsWith("s") || camelName.endsWith("x") || camelName.endsWith("ch") || camelName.endsWith("sh")) {
            return camelName + "es";
        } else {
            return camelName + "s";
        }
    }
    
    /**
     * Transform special values (e.g., JSON arrays)
     */
    private Object transformValue(Object value) {
        if (value == null) {
            return null;
        }
        
        // Handle JSON arrays stored as strings
        if (value instanceof String) {
            String strValue = (String) value;
            if (strValue.startsWith("[") && strValue.endsWith("]")) {
                try {
                    // Simple JSON array parsing for common cases
                    return parseJsonArray(strValue);
                } catch (Exception e) {
                    // If parsing fails, return as-is
                    return value;
                }
            }
        }
        
        return value;
    }
    
    /**
     * Simple JSON array parser for string arrays
     */
    private List<String> parseJsonArray(String jsonArray) {
        String content = jsonArray.substring(1, jsonArray.length() - 1).trim();
        if (content.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<String> result = new ArrayList<>();
        String[] parts = content.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
                result.add(trimmed.substring(1, trimmed.length() - 1));
            } else {
                result.add(trimmed);
            }
        }
        
        return result;
    }
}