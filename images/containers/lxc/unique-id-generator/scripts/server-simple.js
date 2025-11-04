const express = require('express');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const SnowflakeGenerator = require('./snowflake');

const app = express();
app.use(express.json());

const DATA_DIR = process.env.DATA_DIR || '/var/lib/unique-id-generator';
const DATA_FILE = path.join(DATA_DIR, 'state.json');
const PORT = process.env.PORT || 7001;

// Sharding configuration
const SHARD_ID = parseInt(process.env.SHARD_ID || '1');
const TOTAL_SHARDS = parseInt(process.env.TOTAL_SHARDS || '1');

console.log(`Starting Unique ID Generator - Shard ${SHARD_ID} of ${TOTAL_SHARDS}`);

// Initialize Snowflake generator for this shard
const snowflakeGen = new SnowflakeGenerator(SHARD_ID, TOTAL_SHARDS);

const DataType = {
    INT: 'int',
    LONG: 'long',
    SNOWFLAKE: 'snowflake',  // 64-bit Snowflake ID
    UUID8: 'uuid8',     // 8 chars (legacy)
    UUID12: 'uuid12',   // 12 chars (legacy)
    UUID16: 'uuid16',   // 16 chars (legacy)
    UUID22: 'uuid22'    // 22 chars (legacy)
};

const MAX_INT = 2147483647;
const MAX_LONG = 9223372036854775807n;

if (!fs.existsSync(DATA_DIR)) {
    fs.mkdirSync(DATA_DIR, { recursive: true });
}

/**
 * Generate UUID with shard-aware randomization
 */
function generateUUID(length) {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    let result = '';

    const shardEntropy = Buffer.from(`shard-${SHARD_ID}-${Date.now()}`);
    const randomBytes = Buffer.concat([
        shardEntropy,
        crypto.randomBytes(length * 2)
    ]);

    for (let i = 0; i < length; i++) {
        const index = (randomBytes[i] + SHARD_ID * i) % chars.length;
        result += chars[index];
    }

    return result;
}

/**
 * Get next sequential value with sharding (interleaved approach)
 */
function getNextShardedValue(currentIteration, dataType) {
    const baseValue = SHARD_ID + (currentIteration * TOTAL_SHARDS);

    if (dataType === DataType.LONG) {
        return BigInt(baseValue);
    }
    return baseValue;
}

function loadState() {
    try {
        if (fs.existsSync(DATA_FILE)) {
            const data = fs.readFileSync(DATA_FILE, 'utf8');
            const state = JSON.parse(data);

            for (const entity of state) {
                if (entity.dataType === DataType.LONG && typeof entity.currentIteration === 'string') {
                    entity.currentIteration = BigInt(entity.currentIteration);
                }
                if (entity.currentIteration === undefined) {
                    entity.currentIteration = 0;
                }
            }
            return state;
        }
    } catch (error) {
        console.error('Error loading state:', error);
    }
    return [];
}

function saveState(state) {
    const stateToSave = state.map(entity => ({
        ...entity,
        currentIteration: entity.dataType === DataType.LONG
            ? entity.currentIteration.toString()
            : entity.currentIteration
    }));

    fs.writeFileSync(DATA_FILE, JSON.stringify(stateToSave, null, 2));
}

let state = loadState();

// ============ SIMPLIFIED API - ID GENERATION ONLY ============

// Health check
app.get('/health', (req, res) => {
    res.json({
        status: 'healthy',
        uptime: process.uptime(),
        shard: SHARD_ID,
        totalShards: TOTAL_SHARDS,
        snowflakeStats: snowflakeGen.getStats()
    });
});

// Generate single ID - Auto-register entity if needed
app.get('/api/next-id/:entityName', (req, res) => {
    const { entityName } = req.params;
    const { dataType } = req.query;

    if (!entityName) {
        return res.status(400).json({ error: 'Entity name is required' });
    }

    if (!dataType || !Object.values(DataType).includes(dataType)) {
        return res.status(400).json({
            error: 'Valid dataType is required',
            validTypes: Object.values(DataType)
        });
    }

    let record = state.find(r => r.entityName === entityName);

    if (!record) {
        // Auto-register entity
        record = {
            entityName,
            dataType,
            currentIteration: 0,
            shardId: SHARD_ID
        };
        state.push(record);
        console.log(`Auto-registered entity '${entityName}' with type '${dataType}'`);
    } else {
        // Check type consistency
        if (record.dataType !== dataType) {
            return res.status(400).json({
                error: 'Type mismatch',
                message: `Entity '${entityName}' is registered as '${record.dataType}', cannot use '${dataType}'`,
                registeredType: record.dataType
            });
        }
    }

    let nextValue;

    // Generate ID based on type
    if (dataType === DataType.SNOWFLAKE) {
        nextValue = snowflakeGen.generate();
    }
    else if (dataType.startsWith('uuid')) {
        const length = parseInt(dataType.substring(4));
        nextValue = generateUUID(length);
    }
    else {
        const nextShardedValue = getNextShardedValue(record.currentIteration, dataType);

        // Check for overflow
        if (dataType === DataType.INT && nextShardedValue > MAX_INT) {
            return res.status(500).json({
                error: 'Integer overflow',
                message: `Entity '${entityName}' has reached maximum int value for shard ${SHARD_ID}`
            });
        } else if (dataType === DataType.LONG && nextShardedValue > MAX_LONG) {
            return res.status(500).json({
                error: 'Long overflow',
                message: `Entity '${entityName}' has reached maximum long value for shard ${SHARD_ID}`
            });
        }

        nextValue = nextShardedValue;
        record.currentIteration++;
    }

    saveState(state);

    res.json({
        entityName,
        dataType: record.dataType,
        value: record.dataType === DataType.LONG ? nextValue.toString() : nextValue,
        shard: SHARD_ID
    });
});

// Generate batch of IDs
app.get('/api/next-batch/:entityName', (req, res) => {
    const { entityName } = req.params;
    const { dataType, batchSize } = req.query;

    if (!entityName) {
        return res.status(400).json({ error: 'Entity name is required' });
    }

    if (!dataType || !Object.values(DataType).includes(dataType)) {
        return res.status(400).json({
            error: 'Valid dataType is required',
            validTypes: Object.values(DataType)
        });
    }

    const size = parseInt(batchSize);
    if (!batchSize || isNaN(size) || size < 1 || size > 10000) {
        return res.status(400).json({
            error: 'Valid batchSize is required',
            message: 'batchSize must be between 1 and 10000'
        });
    }

    // Snowflake IDs
    if (dataType === DataType.SNOWFLAKE) {
        const values = snowflakeGen.generateBatch(size);
        return res.json({
            entityName,
            dataType,
            batchSize: size,
            values,
            shard: SHARD_ID
        });
    }

    // UUID types
    if (dataType.startsWith('uuid')) {
        const length = parseInt(dataType.substring(4));
        const values = [];
        for (let i = 0; i < size; i++) {
            values.push(generateUUID(length));
        }

        return res.json({
            entityName,
            dataType,
            batchSize: size,
            values,
            shard: SHARD_ID
        });
    }

    // Numeric types with sharding
    let record = state.find(r => r.entityName === entityName);

    if (!record) {
        // Auto-register entity
        record = {
            entityName,
            dataType,
            currentIteration: 0,
            shardId: SHARD_ID
        };
        state.push(record);
        console.log(`Auto-registered entity '${entityName}' with type '${dataType}'`);
    } else {
        if (record.dataType !== dataType) {
            return res.status(400).json({
                error: 'Type mismatch',
                message: `Entity '${entityName}' is registered as '${record.dataType}', cannot use '${dataType}'`,
                registeredType: record.dataType
            });
        }
    }

    // Generate batch of numeric IDs
    const values = [];
    let overflow = false;

    for (let i = 0; i < size; i++) {
        const shardedValue = getNextShardedValue(record.currentIteration + i, dataType);

        if (dataType === DataType.INT && shardedValue > MAX_INT) {
            overflow = true;
            break;
        } else if (dataType === DataType.LONG && shardedValue > MAX_LONG) {
            overflow = true;
            break;
        }

        values.push(dataType === DataType.LONG ? shardedValue.toString() : shardedValue);
    }

    if (overflow) {
        return res.status(500).json({
            error: 'Overflow',
            message: `Batch size ${size} would exceed maximum value for shard ${SHARD_ID}`
        });
    }

    record.currentIteration += size;
    saveState(state);

    res.json({
        entityName,
        dataType: record.dataType,
        batchSize: values.length,
        values,
        shard: SHARD_ID
    });
});

// Parse Snowflake ID
app.get('/api/parse-snowflake/:id', (req, res) => {
    const { id } = req.params;
    try {
        const parsed = snowflakeGen.parse(id);
        res.json(parsed);
    } catch (error) {
        res.status(400).json({
            error: 'Invalid Snowflake ID',
            message: error.message
        });
    }
});

// Get available types
app.get('/api/types', (req, res) => {
    res.json({
        availableTypes: Object.values(DataType),
        description: {
            int: `Sequential 32-bit integer (shard ${SHARD_ID}: ${SHARD_ID}, ${SHARD_ID + TOTAL_SHARDS}, ${SHARD_ID + 2*TOTAL_SHARDS}...)`,
            long: `Sequential 64-bit integer (shard ${SHARD_ID}: ${SHARD_ID}, ${SHARD_ID + TOTAL_SHARDS}, ${SHARD_ID + 2*TOTAL_SHARDS}...)`,
            snowflake: '64-bit time-ordered unique ID (timestamp + shard + sequence)',
            uuid8: 'Random 8-character alphanumeric string',
            uuid12: 'Random 12-character alphanumeric string',
            uuid16: 'Random 16-character alphanumeric string',
            uuid22: 'Random 22-character alphanumeric string'
        },
        shardInfo: {
            shardId: SHARD_ID,
            totalShards: TOTAL_SHARDS
        },
        snowflakeInfo: snowflakeGen.getInfo()
    });
});

// 404 handler
app.use((req, res) => {
    res.status(404).json({ error: 'Endpoint not found' });
});

// Error handler
app.use((error, req, res, next) => {
    console.error('Server error:', error);
    res.status(500).json({ error: 'Internal server error' });
});

const server = app.listen(PORT, '0.0.0.0', () => {
    console.log(`Unique ID Generator (Shard ${SHARD_ID}/${TOTAL_SHARDS}) listening on port ${PORT}`);
    console.log(`Data directory: ${DATA_DIR}`);
    console.log('Simplified API - ID generation only');
    console.log('Use uid-cli for maintenance tasks');
});

process.on('SIGTERM', () => {
    console.log('SIGTERM received, shutting down gracefully...');
    server.close(() => {
        console.log('Server closed');
        process.exit(0);
    });
});

process.on('SIGINT', () => {
    console.log('SIGINT received, shutting down gracefully...');
    server.close(() => {
        console.log('Server closed');
        process.exit(0);
    });
});