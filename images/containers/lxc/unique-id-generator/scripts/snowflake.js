/**
 * Snowflake ID Generator
 *
 * Generates 64-bit unique IDs with the following structure:
 * - 1 bit: unused (sign bit for compatibility)
 * - 41 bits: timestamp (milliseconds since custom epoch)
 * - 10 bits: shard ID (supports up to 1024 shards)
 * - 12 bits: sequence number (4096 IDs per millisecond per shard)
 *
 * Total: 64 bits
 */
class SnowflakeGenerator {
    constructor(shardId = 1, totalShards = 1) {
        // Validate shard configuration
        if (shardId < 1 || shardId > 1024) {
            throw new Error('Shard ID must be between 1 and 1024');
        }
        if (shardId > totalShards) {
            throw new Error('Shard ID cannot be greater than total shards');
        }

        // Configuration
        this.shardId = shardId - 1; // Convert to 0-based for bit operations
        this.totalShards = totalShards;

        // Bit lengths
        this.timestampBits = 41;
        this.shardIdBits = 10;
        this.sequenceBits = 12;

        // Max values
        this.maxShardId = (1 << this.shardIdBits) - 1; // 1023
        this.maxSequence = (1 << this.sequenceBits) - 1; // 4095

        // Bit shifts
        this.timestampShift = this.shardIdBits + this.sequenceBits; // 22
        this.shardIdShift = this.sequenceBits; // 12

        // Custom epoch (2024-01-01 00:00:00 UTC)
        this.epoch = 1704067200000;

        // State
        this.sequence = 0;
        this.lastTimestamp = -1;

        // Statistics
        this.stats = {
            generated: 0,
            collisions: 0,
            waits: 0
        };
    }

    /**
     * Generate a new Snowflake ID
     * @returns {string} 64-bit ID as a string
     */
    generate() {
        let timestamp = this._currentTime();

        // Handle clock moving backwards
        if (timestamp < this.lastTimestamp) {
            throw new Error(`Clock moved backwards. Refusing to generate ID for ${this.lastTimestamp - timestamp} milliseconds`);
        }

        if (timestamp === this.lastTimestamp) {
            // Same millisecond, increment sequence
            this.sequence = (this.sequence + 1) & this.maxSequence;

            if (this.sequence === 0) {
                // Sequence overflow, wait for next millisecond
                this.stats.waits++;
                timestamp = this._waitNextMillis(timestamp);
            }
            this.stats.collisions++;
        } else {
            // New millisecond, reset sequence
            this.sequence = 0;
        }

        this.lastTimestamp = timestamp;
        this.stats.generated++;

        // Generate ID using BigInt for 64-bit precision
        const timestampBits = BigInt(timestamp - this.epoch) << BigInt(this.timestampShift);
        const shardIdBits = BigInt(this.shardId) << BigInt(this.shardIdShift);
        const sequenceBits = BigInt(this.sequence);

        const id = timestampBits | shardIdBits | sequenceBits;

        return id.toString();
    }

    /**
     * Generate multiple IDs at once
     * @param {number} count Number of IDs to generate
     * @returns {string[]} Array of ID strings
     */
    generateBatch(count) {
        const ids = [];
        for (let i = 0; i < count; i++) {
            ids.push(this.generate());
        }
        return ids;
    }

    /**
     * Parse a Snowflake ID to extract its components
     * @param {string} id The ID to parse
     * @returns {object} Parsed components
     */
    parse(id) {
        const snowflakeId = BigInt(id);

        // Extract components
        const sequence = Number(snowflakeId & BigInt(this.maxSequence));
        const shardId = Number((snowflakeId >> BigInt(this.shardIdShift)) & BigInt(this.maxShardId));
        const timestamp = Number(snowflakeId >> BigInt(this.timestampShift)) + this.epoch;

        return {
            id: id,
            timestamp: timestamp,
            date: new Date(timestamp),
            shardId: shardId + 1, // Convert back to 1-based
            sequence: sequence,
            binary: snowflakeId.toString(2).padStart(64, '0')
        };
    }

    /**
     * Get current time in milliseconds
     * @returns {number} Current timestamp
     */
    _currentTime() {
        return Date.now();
    }

    /**
     * Wait until the next millisecond
     * @param {number} lastTimestamp Last timestamp
     * @returns {number} Next timestamp
     */
    _waitNextMillis(lastTimestamp) {
        let timestamp = this._currentTime();
        while (timestamp <= lastTimestamp) {
            timestamp = this._currentTime();
        }
        return timestamp;
    }

    /**
     * Get generator statistics
     * @returns {object} Statistics
     */
    getStats() {
        return {
            ...this.stats,
            shardId: this.shardId + 1,
            totalShards: this.totalShards,
            maxIdsPerMs: this.maxSequence + 1,
            maxShards: this.maxShardId + 1
        };
    }

    /**
     * Reset the generator state
     */
    reset() {
        this.sequence = 0;
        this.lastTimestamp = -1;
        this.stats = {
            generated: 0,
            collisions: 0,
            waits: 0
        };
    }

    /**
     * Get info about the generator configuration
     * @returns {object} Configuration info
     */
    getInfo() {
        return {
            shardId: this.shardId + 1,
            totalShards: this.totalShards,
            epoch: new Date(this.epoch).toISOString(),
            maxIdsPerMs: this.maxSequence + 1,
            maxShards: this.maxShardId + 1,
            bitsAllocation: {
                timestamp: this.timestampBits,
                shardId: this.shardIdBits,
                sequence: this.sequenceBits,
                total: 64
            }
        };
    }
}

// Export for Node.js
if (typeof module !== 'undefined' && module.exports) {
    module.exports = SnowflakeGenerator;
}