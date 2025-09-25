package com.telcobright.orchestrix.images.lxc.uniqueidgenerator.entities;

/**
 * Runtime shard configuration for Unique ID Generator
 * This is applied at container launch, not during build
 */
public class ShardConfig {

    private int shardId = 1;
    private int totalShards = 1;
    private String distributionStrategy = "interleaved"; // interleaved, range, bit-encoded

    public ShardConfig() {}

    public ShardConfig(int shardId, int totalShards) {
        this.shardId = shardId;
        this.totalShards = totalShards;
    }

    public boolean isValid() {
        return shardId > 0 && shardId <= totalShards && totalShards > 0;
    }

    public boolean isSharded() {
        return totalShards > 1;
    }

    /**
     * Calculate the next value for this shard using interleaved strategy
     * Formula: shardId + (iteration * totalShards)
     */
    public long getNextInterleavedValue(long iteration) {
        return shardId + (iteration * totalShards);
    }

    /**
     * Calculate range start for range-based strategy
     */
    public long getRangeStart(long totalRange) {
        long rangePerShard = totalRange / totalShards;
        return (shardId - 1) * rangePerShard + 1;
    }

    /**
     * Calculate range end for range-based strategy
     */
    public long getRangeEnd(long totalRange) {
        long rangePerShard = totalRange / totalShards;
        if (shardId == totalShards) {
            return totalRange; // Last shard gets any remainder
        }
        return shardId * rangePerShard;
    }

    // Getters and Setters
    public int getShardId() { return shardId; }
    public void setShardId(int shardId) { this.shardId = shardId; }

    public int getTotalShards() { return totalShards; }
    public void setTotalShards(int totalShards) { this.totalShards = totalShards; }

    public String getDistributionStrategy() { return distributionStrategy; }
    public void setDistributionStrategy(String distributionStrategy) {
        this.distributionStrategy = distributionStrategy;
    }

    @Override
    public String toString() {
        return String.format("ShardConfig[shard=%d/%d, strategy=%s]",
            shardId, totalShards, distributionStrategy);
    }
}