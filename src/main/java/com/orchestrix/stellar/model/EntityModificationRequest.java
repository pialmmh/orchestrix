package com.orchestrix.stellar.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Request model for entity modifications
 */
@Data
public class EntityModificationRequest {
    private String entityName;
    private ModifyOperation operation;
    private Map<String, Object> data;
    private Map<String, Object> criteria; // For UPDATE/DELETE
    private List<EntityModificationRequest> include; // Nested modifications
}