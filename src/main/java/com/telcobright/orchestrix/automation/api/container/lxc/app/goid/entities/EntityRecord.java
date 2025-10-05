package com.telcobright.orchestrix.automation.api.container.lxc.app.goid.entities;

/**
 * Entity state record for ID generation
 *
 * <p>Persisted to JSON state file in container
 */
public class EntityRecord {
    private String entityName;
    private String dataType;
    private long currentIteration;
    private int shardId;

    public EntityRecord() {
    }

    public EntityRecord(String entityName, String dataType, long currentIteration, int shardId) {
        this.entityName = entityName;
        this.dataType = dataType;
        this.currentIteration = currentIteration;
        this.shardId = shardId;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public long getCurrentIteration() {
        return currentIteration;
    }

    public void setCurrentIteration(long currentIteration) {
        this.currentIteration = currentIteration;
    }

    public int getShardId() {
        return shardId;
    }

    public void setShardId(int shardId) {
        this.shardId = shardId;
    }
}
