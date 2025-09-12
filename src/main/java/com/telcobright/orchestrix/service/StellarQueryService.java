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

@Service
public class StellarQueryService {

    @Autowired
    private EntityManager entityManager;

    public List<Map<String, Object>> executeQuery(QueryNode queryNode) {
        try {
            String entityName = queryNode.getKind();
            // Capitalize first letter to match entity class name
            entityName = entityName.substring(0, 1).toUpperCase() + entityName.substring(1).toLowerCase();
            
            // Handle special cases
            if (entityName.equalsIgnoreCase("networkdevice")) {
                entityName = "NetworkDevice";
            } else if (entityName.equalsIgnoreCase("availabilityzone")) {
                entityName = "AvailabilityZone";
            }
            
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
            
            // Convert to Maps and handle includes
            List<Map<String, Object>> resultMaps = new ArrayList<>();
            for (Object entity : results) {
                Map<String, Object> entityMap = entityToMap(entity, queryNode.getSelect());
                
                // Handle includes (nested queries)
                if (queryNode.getInclude() != null) {
                    for (QueryNode includeNode : queryNode.getInclude()) {
                        String relationName = includeNode.getKind();
                        List<Map<String, Object>> nestedResults = executeNestedQuery(entity, includeNode, relationName);
                        entityMap.put(relationName, nestedResults);
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
            // Get the relation field from parent entity
            Field relationField = parentEntity.getClass().getDeclaredField(relationName);
            relationField.setAccessible(true);
            Object relationValue = relationField.get(parentEntity);
            
            if (relationValue == null) {
                return new ArrayList<>();
            }
            
            List<Map<String, Object>> results = new ArrayList<>();
            
            if (relationValue instanceof Collection) {
                // Handle one-to-many relations
                for (Object childEntity : (Collection<?>) relationValue) {
                    Map<String, Object> childMap = entityToMap(childEntity, includeNode.getSelect());
                    
                    // Recursively handle nested includes
                    if (includeNode.getInclude() != null) {
                        for (QueryNode nestedInclude : includeNode.getInclude()) {
                            String nestedRelationName = nestedInclude.getKind();
                            List<Map<String, Object>> nestedResults = executeNestedQuery(childEntity, nestedInclude, nestedRelationName);
                            childMap.put(nestedRelationName, nestedResults);
                        }
                    }
                    
                    results.add(childMap);
                }
            } else {
                // Handle many-to-one relations
                Map<String, Object> childMap = entityToMap(relationValue, includeNode.getSelect());
                
                // Recursively handle nested includes
                if (includeNode.getInclude() != null) {
                    for (QueryNode nestedInclude : includeNode.getInclude()) {
                        String nestedRelationName = nestedInclude.getKind();
                        List<Map<String, Object>> nestedResults = executeNestedQuery(relationValue, nestedInclude, nestedRelationName);
                        childMap.put(nestedRelationName, nestedResults);
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
    
    private Map<String, Object> entityToMap(Object entity, List<String> selectFields) {
        Map<String, Object> map = new HashMap<>();
        
        try {
            Class<?> clazz = entity.getClass();
            
            // If no select fields specified, include all fields
            if (selectFields == null || selectFields.isEmpty()) {
                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);
                    String fieldName = field.getName();
                    
                    // Skip collections and complex types for now
                    if (!Collection.class.isAssignableFrom(field.getType()) && 
                        !field.getType().getPackage().getName().startsWith("com.telcobright.orchestrix.entity")) {
                        Object value = field.get(entity);
                        map.put(fieldName, value);
                    }
                }
            } else {
                // Include only selected fields
                for (String fieldName : selectFields) {
                    Field field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object value = field.get(entity);
                    map.put(fieldName, value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return map;
    }
    
    @Transactional
    public MutationResponse executeMutation(EntityModificationRequest request) {
        MutationResponse response = new MutationResponse();
        
        try {
            String entityName = request.getEntityName();
            // Capitalize first letter to match entity class name
            entityName = entityName.substring(0, 1).toUpperCase() + entityName.substring(1).toLowerCase();
            
            // Handle special cases
            if (entityName.equalsIgnoreCase("networkdevice")) {
                entityName = "NetworkDevice";
            } else if (entityName.equalsIgnoreCase("availabilityzone")) {
                entityName = "AvailabilityZone";
            }
            
            Class<?> entityClass = Class.forName("com.telcobright.orchestrix.entity." + entityName);
            
            switch (request.getOperation().toUpperCase()) {
                case "INSERT":
                    Object newEntity = createEntityFromData(entityClass, request.getData());
                    entityManager.persist(newEntity);
                    entityManager.flush();
                    response.setSuccess(true);
                    response.setMessage("Entity created successfully");
                    response.setData(entityToMap(newEntity, null));
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
                        response.setData(entityToMap(existingEntity, null));
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