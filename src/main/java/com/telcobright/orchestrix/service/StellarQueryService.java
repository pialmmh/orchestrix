package com.telcobright.orchestrix.service;

import com.telcobright.orchestrix.dto.stellar.QueryNode;
import com.telcobright.orchestrix.dto.stellar.EntityModificationRequest;
import com.telcobright.orchestrix.dto.stellar.MutationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StellarQueryService {

    @Autowired
    private EntityManager entityManager;

    public List<Map<String, Object>> executeQuery(QueryNode queryNode) {
        try {
            String entityName = normalizeEntityName(queryNode.getKind());
            Class<?> entityClass = Class.forName("com.telcobright.orchestrix.entity." + entityName);
            
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<?> query = cb.createQuery(entityClass);
            Root<?> root = query.from(entityClass);
            
            // Apply criteria if present
            if (queryNode.getCriteria() != null && !queryNode.getCriteria().isEmpty()) {
                List<Predicate> predicates = buildPredicates(cb, root, queryNode.getCriteria());
                query.where(predicates.toArray(new Predicate[0]));
            }
            
            // Execute query
            Query jpaQuery = entityManager.createQuery(query);
            
            // Apply pagination if present
            if (queryNode.getPage() != null) {
                jpaQuery.setFirstResult(queryNode.getPage().getOffset());
                jpaQuery.setMaxResults(queryNode.getPage().getLimit());
            }
            
            List<?> results = jpaQuery.getResultList();
            
            // Convert to Maps with proper field names and handle includes
            List<Map<String, Object>> resultMaps = new ArrayList<>();
            for (Object entity : results) {
                Map<String, Object> entityMap = entityToCleanMap(entity, queryNode.getSelect());
                
                // Handle includes (nested queries)
                if (queryNode.getInclude() != null) {
                    for (QueryNode includeNode : queryNode.getInclude()) {
                        String relationName = includeNode.getKind();
                        String propertyName = pluralizeName(toCamelCase(relationName));
                        List<Map<String, Object>> nestedResults = executeNestedQuery(entity, includeNode, relationName);
                        entityMap.put(propertyName, nestedResults);
                    }
                }
                
                resultMaps.add(entityMap);
            }
            
            return resultMaps;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to execute Stellar query: " + e.getMessage(), e);
        }
    }
    
    private List<Map<String, Object>> executeNestedQuery(Object parentEntity, QueryNode includeNode, String relationName) {
        try {
            // Try to find the relation field
            String fieldName = findRelationFieldName(parentEntity.getClass(), relationName);
            if (fieldName == null) {
                return new ArrayList<>();
            }
            
            Field relationField = parentEntity.getClass().getDeclaredField(fieldName);
            relationField.setAccessible(true);
            Object relationValue = relationField.get(parentEntity);
            
            if (relationValue == null) {
                return new ArrayList<>();
            }
            
            List<Map<String, Object>> results = new ArrayList<>();
            
            if (relationValue instanceof Collection) {
                // Handle one-to-many relations
                for (Object childEntity : (Collection<?>) relationValue) {
                    Map<String, Object> childMap = entityToCleanMap(childEntity, includeNode.getSelect());
                    
                    // Recursively handle nested includes
                    if (includeNode.getInclude() != null) {
                        for (QueryNode nestedInclude : includeNode.getInclude()) {
                            String nestedRelationName = nestedInclude.getKind();
                            String propertyName = pluralizeName(toCamelCase(nestedRelationName));
                            List<Map<String, Object>> nestedResults = executeNestedQuery(childEntity, nestedInclude, nestedRelationName);
                            childMap.put(propertyName, nestedResults);
                        }
                    }
                    
                    results.add(childMap);
                }
            } else {
                // Handle many-to-one relations
                Map<String, Object> childMap = entityToCleanMap(relationValue, includeNode.getSelect());
                
                // Recursively handle nested includes
                if (includeNode.getInclude() != null) {
                    for (QueryNode nestedInclude : includeNode.getInclude()) {
                        String nestedRelationName = nestedInclude.getKind();
                        String propertyName = pluralizeName(toCamelCase(nestedRelationName));
                        List<Map<String, Object>> nestedResults = executeNestedQuery(relationValue, nestedInclude, nestedRelationName);
                        childMap.put(propertyName, nestedResults);
                    }
                }
                
                results.add(childMap);
            }
            
            return results;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    private String findRelationFieldName(Class<?> entityClass, String relationName) {
        // Normalize the relation name
        String normalized = normalizeEntityName(relationName);
        
        // Try different variations
        String[] variations = {
            toCamelCase(relationName) + "s",  // e.g., "clouds"
            toCamelCase(relationName),        // e.g., "cloud"
            normalized.toLowerCase() + "s",    // e.g., "datacenters"
            normalized.toLowerCase()           // e.g., "datacenter"
        };
        
        for (String variation : variations) {
            try {
                entityClass.getDeclaredField(variation);
                return variation;
            } catch (NoSuchFieldException e) {
                // Try next variation
            }
        }
        
        return null;
    }
    
    private String normalizeEntityName(String kind) {
        // Convert to proper entity class name
        String entityName = kind.substring(0, 1).toUpperCase() + kind.substring(1).toLowerCase();
        
        // Handle special cases
        if (kind.equalsIgnoreCase("networkdevice") || kind.equalsIgnoreCase("network-device")) {
            entityName = "NetworkDevice";
        } else if (kind.equalsIgnoreCase("availabilityzone") || kind.equalsIgnoreCase("availability-zone")) {
            entityName = "AvailabilityZone";
        } else if (kind.equalsIgnoreCase("datacenter")) {
            entityName = "Datacenter";
        } else if (kind.equalsIgnoreCase("partner")) {
            entityName = "Partner";
        } else if (kind.equalsIgnoreCase("cloud")) {
            entityName = "Cloud";
        } else if (kind.equalsIgnoreCase("compute")) {
            entityName = "Compute";
        }
        
        return entityName;
    }
    
    private String toCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        // Handle hyphenated names
        String[] parts = input.split("-");
        if (parts.length > 1) {
            StringBuilder result = new StringBuilder(parts[0].toLowerCase());
            for (int i = 1; i < parts.length; i++) {
                result.append(parts[i].substring(0, 1).toUpperCase());
                result.append(parts[i].substring(1).toLowerCase());
            }
            return result.toString();
        }
        
        // Simple lowercase for single words
        return input.toLowerCase();
    }
    
    private String pluralizeName(String name) {
        // Simple pluralization rules
        if (name.endsWith("y") && !name.endsWith("ay") && !name.endsWith("ey") && !name.endsWith("oy")) {
            return name.substring(0, name.length() - 1) + "ies";
        } else if (name.endsWith("s") || name.endsWith("x") || name.endsWith("z") || 
                   name.endsWith("ch") || name.endsWith("sh")) {
            return name + "es";
        } else {
            return name + "s";
        }
    }
    
    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<?> root, Map<String, Object> criteria) {
        List<Predicate> predicates = new ArrayList<>();
        
        for (Map.Entry<String, Object> entry : criteria.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Map) {
                // Handle complex criteria (e.g., $eq, $ne, $gt, $lt, etc.)
                Map<String, Object> operators = (Map<String, Object>) value;
                for (Map.Entry<String, Object> opEntry : operators.entrySet()) {
                    String operator = opEntry.getKey();
                    Object operandValue = opEntry.getValue();
                    
                    switch (operator) {
                        case "$eq":
                            predicates.add(cb.equal(root.get(key), operandValue));
                            break;
                        case "$ne":
                            predicates.add(cb.notEqual(root.get(key), operandValue));
                            break;
                        case "$gt":
                            predicates.add(cb.greaterThan(root.get(key), (Comparable) operandValue));
                            break;
                        case "$lt":
                            predicates.add(cb.lessThan(root.get(key), (Comparable) operandValue));
                            break;
                        case "$gte":
                            predicates.add(cb.greaterThanOrEqualTo(root.get(key), (Comparable) operandValue));
                            break;
                        case "$lte":
                            predicates.add(cb.lessThanOrEqualTo(root.get(key), (Comparable) operandValue));
                            break;
                        case "$like":
                            predicates.add(cb.like(root.get(key), (String) operandValue));
                            break;
                        case "$in":
                            predicates.add(root.get(key).in((Collection<?>) operandValue));
                            break;
                    }
                }
            } else {
                // Simple equality check
                predicates.add(cb.equal(root.get(key), value));
            }
        }
        
        return predicates;
    }
    
    private Map<String, Object> entityToCleanMap(Object entity, List<String> selectFields) {
        Map<String, Object> map = new HashMap<>();
        
        try {
            Class<?> clazz = entity.getClass();
            
            // If no select fields specified, include all simple fields
            if (selectFields == null || selectFields.isEmpty()) {
                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);
                    String fieldName = field.getName();
                    
                    // Skip collections and complex types for basic mapping
                    if (!Collection.class.isAssignableFrom(field.getType()) && 
                        !field.getType().getPackage().getName().startsWith("com.telcobright.orchestrix.entity")) {
                        
                        Object value = field.get(entity);
                        
                        // Convert field name from snake_case to camelCase
                        String camelCaseName = snakeToCamelCase(fieldName);
                        
                        // Handle special value conversions
                        if (value instanceof String && value.toString().startsWith("[") && value.toString().endsWith("]")) {
                            // Convert JSON array strings to actual arrays
                            try {
                                value = value.toString().replaceAll("[\\[\\]\"]", "").split(",");
                            } catch (Exception e) {
                                // Keep as string if conversion fails
                            }
                        }
                        
                        map.put(camelCaseName, value);
                    }
                }
            } else {
                // Include only selected fields
                for (String fieldName : selectFields) {
                    try {
                        Field field = clazz.getDeclaredField(fieldName);
                        field.setAccessible(true);
                        Object value = field.get(entity);
                        String camelCaseName = snakeToCamelCase(fieldName);
                        map.put(camelCaseName, value);
                    } catch (NoSuchFieldException e) {
                        // Field doesn't exist, skip it
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return map;
    }
    
    private String snakeToCamelCase(String input) {
        if (!input.contains("_")) {
            return input;
        }
        
        String[] parts = input.split("_");
        StringBuilder result = new StringBuilder(parts[0]);
        
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                result.append(parts[i].substring(0, 1).toUpperCase());
                result.append(parts[i].substring(1));
            }
        }
        
        return result.toString();
    }
    
    @Transactional
    public MutationResponse executeMutation(EntityModificationRequest request) {
        MutationResponse response = new MutationResponse();
        
        try {
            String entityName = normalizeEntityName(request.getEntityName());
            Class<?> entityClass = Class.forName("com.telcobright.orchestrix.entity." + entityName);
            
            switch (request.getOperation().toUpperCase()) {
                case "INSERT":
                    Object newEntity = createEntityFromData(entityClass, request.getData());
                    entityManager.persist(newEntity);
                    entityManager.flush();
                    response.setSuccess(true);
                    response.setMessage("Entity created successfully");
                    response.setData(entityToCleanMap(newEntity, null));
                    response.setEntityName(entityName);
                    break;
                    
                case "UPDATE":
                    Object existingEntity = entityManager.find(entityClass, request.getId());
                    if (existingEntity != null) {
                        updateEntityFromData(existingEntity, request.getData());
                        entityManager.merge(existingEntity);
                        entityManager.flush();
                        response.setSuccess(true);
                        response.setMessage("Entity updated successfully");
                        response.setData(entityToCleanMap(existingEntity, null));
                        response.setEntityName(entityName);
                        response.setId(request.getId());
                    } else {
                        response.setSuccess(false);
                        response.setMessage("Entity not found");
                    }
                    break;
                    
                case "DELETE":
                    Object entityToDelete = entityManager.find(entityClass, request.getId());
                    if (entityToDelete != null) {
                        entityManager.remove(entityToDelete);
                        entityManager.flush();
                        response.setSuccess(true);
                        response.setMessage("Entity deleted successfully");
                        response.setEntityName(entityName);
                        response.setId(request.getId());
                    } else {
                        response.setSuccess(false);
                        response.setMessage("Entity not found");
                    }
                    break;
                    
                default:
                    response.setSuccess(false);
                    response.setMessage("Invalid operation: " + request.getOperation());
            }
            
            // Handle nested operations if present
            if (request.getNested() != null && response.isSuccess()) {
                for (EntityModificationRequest nestedRequest : request.getNested()) {
                    MutationResponse nestedResponse = executeMutation(nestedRequest);
                    if (!nestedResponse.isSuccess()) {
                        response.setSuccess(false);
                        response.setMessage("Nested operation failed: " + nestedResponse.getMessage());
                        break;
                    }
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.setSuccess(false);
            response.setMessage("Mutation failed: " + e.getMessage());
        }
        
        return response;
    }
    
    private Object createEntityFromData(Class<?> entityClass, Map<String, Object> data) throws Exception {
        Object entity = entityClass.getDeclaredConstructor().newInstance();
        updateEntityFromData(entity, data);
        return entity;
    }
    
    private void updateEntityFromData(Object entity, Map<String, Object> data) throws Exception {
        Class<?> clazz = entity.getClass();
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();
            
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                
                // Handle type conversions if necessary
                if (value != null) {
                    Class<?> fieldType = field.getType();
                    
                    // Convert numbers if necessary
                    if (fieldType == Long.class || fieldType == long.class) {
                        if (value instanceof Number) {
                            value = ((Number) value).longValue();
                        }
                    } else if (fieldType == Integer.class || fieldType == int.class) {
                        if (value instanceof Number) {
                            value = ((Number) value).intValue();
                        }
                    } else if (fieldType == Double.class || fieldType == double.class) {
                        if (value instanceof Number) {
                            value = ((Number) value).doubleValue();
                        }
                    } else if (fieldType == Float.class || fieldType == float.class) {
                        if (value instanceof Number) {
                            value = ((Number) value).floatValue();
                        }
                    } else if (fieldType == Boolean.class || fieldType == boolean.class) {
                        if (!(value instanceof Boolean)) {
                            value = Boolean.valueOf(value.toString());
                        }
                    }
                    
                    field.set(entity, value);
                }
            } catch (NoSuchFieldException e) {
                // Field doesn't exist, skip it
                System.out.println("Field not found: " + fieldName);
            }
        }
    }
}