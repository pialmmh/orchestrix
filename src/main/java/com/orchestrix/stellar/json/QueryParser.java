package com.orchestrix.stellar.json;

import com.orchestrix.stellar.model.*;
import java.util.*;

/**
 * Parser for converting JSON query format from the frontend to QueryNode.
 * Handles the JavaScript library's query format.
 */
public class QueryParser {
    
    /**
     * Parse a Map representation of a query (from JSON) into a QueryNode
     */
    @SuppressWarnings("unchecked")
    public static QueryNode parse(Map<String, Object> json) {
        if (json == null || !json.containsKey("kind")) {
            throw new IllegalArgumentException("Query must have a 'kind' field");
        }
        
        // Parse kind
        String kindStr = (String) json.get("kind");
        Kind kind = parseKind(kindStr);
        
        QueryNode.Builder builder = QueryNode.of(kind);
        
        // Parse criteria
        if (json.containsKey("criteria")) {
            Map<String, Object> criteriaMap = (Map<String, Object>) json.get("criteria");
            if (criteriaMap != null && !criteriaMap.isEmpty()) {
                Criteria criteria = parseCriteria(criteriaMap);
                builder.criteria(criteria);
            }
        }
        
        // Parse page
        if (json.containsKey("page")) {
            Map<String, Object> pageMap = (Map<String, Object>) json.get("page");
            if (pageMap != null) {
                Page page = parsePage(pageMap);
                builder.page(page);
            }
        }
        
        // Parse lazy flag
        if (json.containsKey("lazy")) {
            Boolean lazy = (Boolean) json.get("lazy");
            if (lazy != null) {
                builder.lazy(lazy);
            }
        }
        
        // Parse lazyLoadKey
        if (json.containsKey("lazyLoadKey")) {
            String lazyLoadKey = (String) json.get("lazyLoadKey");
            if (lazyLoadKey != null) {
                builder.lazyLoadKey(lazyLoadKey);
            }
        }
        
        // Parse include (nested queries)
        if (json.containsKey("include")) {
            List<Map<String, Object>> includeList = (List<Map<String, Object>>) json.get("include");
            if (includeList != null) {
                for (Map<String, Object> includeMap : includeList) {
                    QueryNode nested = parse(includeMap);
                    builder.include(nested);
                }
            }
        }
        
        return builder.build();
    }
    
    private static Kind parseKind(String kind) {
        if (kind == null) {
            throw new IllegalArgumentException("Kind cannot be null");
        }
        
        return switch (kind.toLowerCase()) {
            // Original entities
            case "person" -> Kind.PERSON;
            case "order" -> Kind.ORDER;
            case "invoice" -> Kind.INVOICE;
            // Northwind entities
            case "category" -> Kind.CATEGORY;
            case "product" -> Kind.PRODUCT;
            case "customer" -> Kind.CUSTOMER;
            case "salesorder" -> Kind.SALESORDER;
            case "orderdetail" -> Kind.ORDERDETAIL;
            case "employee" -> Kind.EMPLOYEE;
            case "shipper" -> Kind.SHIPPER;
            case "supplier" -> Kind.SUPPLIER;
            default -> throw new IllegalArgumentException("Unknown kind: " + kind);
        };
    }
    
    @SuppressWarnings("unchecked")
    private static Criteria parseCriteria(Map<String, Object> criteriaMap) {
        if (criteriaMap == null || criteriaMap.isEmpty()) {
            return null;
        }
        
        Map<String, List<Object>> fields = new LinkedHashMap<>();
        
        for (Map.Entry<String, Object> entry : criteriaMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            List<Object> values;
            if (value instanceof List) {
                values = (List<Object>) value;
            } else if (value instanceof Map) {
                // Handle operators like {$gte: 18}
                Map<String, Object> operators = (Map<String, Object>) value;
                values = new ArrayList<>();
                for (Map.Entry<String, Object> op : operators.entrySet()) {
                    // For now, just add the value, ignoring the operator
                    // In a full implementation, you'd handle different operators
                    values.add(op.getValue());
                }
            } else {
                values = List.of(value);
            }
            
            fields.put(key, values);
        }
        
        // Build criteria using the first entry, then add the rest
        if (fields.isEmpty()) {
            return null;
        }
        
        Iterator<Map.Entry<String, List<Object>>> iter = fields.entrySet().iterator();
        Map.Entry<String, List<Object>> first = iter.next();
        Criteria criteria = Criteria.of(first.getKey(), first.getValue().toArray());
        
        while (iter.hasNext()) {
            Map.Entry<String, List<Object>> entry = iter.next();
            criteria = criteria.and(entry.getKey(), entry.getValue().toArray());
        }
        
        return criteria;
    }
    
    private static Page parsePage(Map<String, Object> pageMap) {
        if (pageMap == null) {
            return null;
        }
        
        int limit = ((Number) pageMap.getOrDefault("limit", 20)).intValue();
        int offset = ((Number) pageMap.getOrDefault("offset", 0)).intValue();
        
        return Page.of(limit, offset);
    }
}