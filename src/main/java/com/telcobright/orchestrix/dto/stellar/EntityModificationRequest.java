package com.telcobright.orchestrix.dto.stellar;

import java.util.List;
import java.util.Map;

public class EntityModificationRequest {
    private String entityName;
    private String operation; // INSERT, UPDATE, DELETE
    private Map<String, Object> data;
    private Object id;
    private List<EntityModificationRequest> nested;

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }

    public List<EntityModificationRequest> getNested() {
        return nested;
    }

    public void setNested(List<EntityModificationRequest> nested) {
        this.nested = nested;
    }
}