const express = require('express');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const app = express();
app.use(express.json());

const DATA_DIR = '/var/lib/unique-id-generator';
const DATA_FILE = path.join(DATA_DIR, 'state.json');
const PORT = process.env.PORT || 7001;

// Sharding configuration
const SHARD_ID = parseInt(process.env.SHARD_ID || '1');
const TOTAL_SHARDS = parseInt(process.env.TOTAL_SHARDS || '1');

console.log(`Starting Unique ID Generator - Shard ${SHARD_ID} of ${TOTAL_SHARDS}`);

const DataType = {
    INT: 'int',
    LONG: 'long',
    UUID8: 'uuid8',     // 8 chars
    UUID12: 'uuid12',   // 12 chars
    UUID16: 'uuid16',   // 16 chars
    UUID22: 'uuid22'    // 22 chars
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
    // Use shard ID as part of the random seed to minimize collisions
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    let result = '';

    // Add shard-specific entropy
    const shardEntropy = Buffer.from(`shard-${SHARD_ID}-${Date.now()}`);
    const randomBytes = Buffer.concat([
        shardEntropy,
        crypto.randomBytes(length * 2)
    ]);

    // Generate UUID with shard-influenced randomization
    for (let i = 0; i < length; i++) {
        // Mix shard ID into character selection
        const index = (randomBytes[i] + SHARD_ID * i) % chars.length;
        result += chars[index];
    }

    return result;
}

/**
 * Get next sequential value with sharding (interleaved approach)
 * Each shard generates: shardId + (n * totalShards)
 * Shard 1 of 3: 1, 4, 7, 10...
 * Shard 2 of 3: 2, 5, 8, 11...
 * Shard 3 of 3: 3, 6, 9, 12...
 */
function getNextShardedValue(currentIteration, dataType) {
    const baseValue = SHARD_ID + (currentIteration * TOTAL_SHARDS);

    if (dataType === DataType.LONG) {
        return BigInt(baseValue);
    }
    return baseValue;
}

/**
 * Get the iteration number from a sharded value
 */
function getIterationFromValue(value, dataType) {
    const numValue = dataType === DataType.LONG ? Number(value) : value;
    return Math.floor((numValue - SHARD_ID) / TOTAL_SHARDS);
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
                // Ensure currentIteration exists (for migration)
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

// Shard info endpoint
app.get('/shard-info', (req, res) => {
    res.json({
        shardId: SHARD_ID,
        totalShards: TOTAL_SHARDS,
        status: 'active'
    });
});

app.get('/health', (req, res) => {
    res.json({
        status: 'healthy',
        uptime: process.uptime(),
        shard: SHARD_ID,
        totalShards: TOTAL_SHARDS
    });
});

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
        record = {
            entityName,
            dataType,
            currentIteration: 0,  // Track iterations, not actual values
            shardId: SHARD_ID
        };
        state.push(record);
    } else {
        if (record.dataType !== dataType) {
            return res.status(400).json({
                error: 'Type mismatch',
                message: `Entity '${entityName}' is registered as '${record.dataType}', cannot use '${dataType}'`,
                registeredType: record.dataType
            });
        }
    }

    let nextValue;

    // Handle UUID types
    if (dataType.startsWith('uuid')) {
        const length = parseInt(dataType.substring(4));
        nextValue = generateUUID(length);
    }
    // Handle numeric types with sharding
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

    // UUID types return array of values
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
        record = {
            entityName,
            dataType,
            currentIteration: 0,
            shardId: SHARD_ID
        };
        state.push(record);
    } else {
        if (record.dataType !== dataType) {
            return res.status(400).json({
                error: 'Type mismatch',
                message: `Entity '${entityName}' is registered as '${record.dataType}', cannot use '${dataType}'`,
                registeredType: record.dataType
            });
        }
    }

    // Calculate sharded range
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

    const startValue = values[0];
    const endValue = values[values.length - 1];

    record.currentIteration += size;
    saveState(state);

    res.json({
        entityName,
        dataType: record.dataType,
        batchSize: values.length,
        startValue,
        endValue,
        values,  // Include actual values for clarity with sharding
        shard: SHARD_ID
    });
});

app.get('/api/status/:entityName', (req, res) => {
    const { entityName } = req.params;
    const record = state.find(r => r.entityName === entityName);

    if (!record) {
        return res.status(404).json({ error: 'Entity not found' });
    }

    // Calculate current value from iteration
    let currentValue = null;
    if (!record.dataType.startsWith('uuid')) {
        if (record.currentIteration > 0) {
            currentValue = getNextShardedValue(record.currentIteration - 1, record.dataType);
            if (record.dataType === DataType.LONG) {
                currentValue = currentValue.toString();
            }
        }
    }

    res.json({
        entityName: record.entityName,
        dataType: record.dataType,
        currentValue,
        currentIteration: record.currentIteration,
        shard: SHARD_ID,
        nextValue: record.dataType.startsWith('uuid') ? null :
                   getNextShardedValue(record.currentIteration, record.dataType).toString()
    });
});

// Initialize a new entity with specified type and optional starting value
app.post('/api/init/:entityName', (req, res) => {
    const { entityName } = req.params;
    const { dataType, startValue } = req.body;

    if (!entityName) {
        return res.status(400).json({ error: 'Entity name is required' });
    }

    if (!dataType || !Object.values(DataType).includes(dataType)) {
        return res.status(400).json({
            error: 'Valid dataType is required',
            validTypes: Object.values(DataType)
        });
    }

    // Check if entity already exists
    const existingRecord = state.find(r => r.entityName === entityName);
    if (existingRecord) {
        return res.status(409).json({
            error: 'Entity already exists',
            message: `Entity '${entityName}' is already registered with type '${existingRecord.dataType}'`,
            currentStatus: {
                dataType: existingRecord.dataType,
                currentIteration: existingRecord.currentIteration,
                shard: SHARD_ID
            }
        });
    }

    // Initialize new entity
    let initialIteration = 0;

    // If startValue provided for numeric types, calculate the iteration
    if (startValue !== undefined && !dataType.startsWith('uuid')) {
        let numericValue;
        try {
            if (dataType === DataType.LONG) {
                numericValue = BigInt(startValue);
                if (numericValue < 0n || numericValue > MAX_LONG) {
                    return res.status(400).json({
                        error: 'Invalid start value',
                        message: `Value must be between 0 and ${MAX_LONG}`
                    });
                }
            } else { // INT
                numericValue = parseInt(startValue);
                if (isNaN(numericValue) || numericValue < 0 || numericValue > MAX_INT) {
                    return res.status(400).json({
                        error: 'Invalid start value',
                        message: `Value must be between 0 and ${MAX_INT}`
                    });
                }
            }
        } catch (error) {
            return res.status(400).json({
                error: 'Invalid start value format',
                message: error.message
            });
        }

        // Auto-adjust value to be compatible with this shard
        const numValue = dataType === DataType.LONG ? Number(numericValue) : numericValue;
        let adjustedValue = numericValue;

        if ((numValue % TOTAL_SHARDS) !== (SHARD_ID % TOTAL_SHARDS)) {
            // Find the next valid value for this shard that's >= startValue
            const adjustment = SHARD_ID - (numValue % TOTAL_SHARDS);
            adjustedValue = numValue + (adjustment < 0 ? adjustment + TOTAL_SHARDS : adjustment);

            if (dataType === DataType.LONG) {
                adjustedValue = BigInt(adjustedValue);
            }
        }

        // Calculate initial iteration from adjusted value
        initialIteration = getIterationFromValue(adjustedValue, dataType);

        // Store the adjusted value for response
        numericValue = adjustedValue;
    }

    // Create new entity record
    const newRecord = {
        entityName,
        dataType,
        currentIteration: initialIteration,
        shardId: SHARD_ID
    };

    state.push(newRecord);
    saveState(state);

    // Prepare response
    const response = {
        entityName,
        dataType,
        shard: SHARD_ID,
        initialized: true,
        message: `Entity '${entityName}' initialized successfully with type '${dataType}'`
    };

    if (!dataType.startsWith('uuid')) {
        if (startValue !== undefined) {
            const actualStart = initialIteration > 0 ?
                getNextShardedValue(initialIteration - 1, dataType) : SHARD_ID;

            response.requestedStartValue = startValue;
            response.actualStartValue = dataType === DataType.LONG ? actualStart.toString() : actualStart;

            if (startValue != actualStart) {
                response.adjusted = true;
                response.adjustmentReason = `Value adjusted to match shard ${SHARD_ID} pattern`;
            }
        } else {
            response.actualStartValue = SHARD_ID;
        }

        response.nextValue = getNextShardedValue(initialIteration, dataType);
        if (dataType === DataType.LONG) {
            response.nextValue = response.nextValue.toString();
        }
        response.pattern = `${SHARD_ID}, ${SHARD_ID + TOTAL_SHARDS}, ${SHARD_ID + 2*TOTAL_SHARDS}...`;
    }

    res.status(201).json(response);
});

// Reset counter for numeric entities
app.put('/api/reset/:entityName', (req, res) => {
    const { entityName } = req.params;
    const { value } = req.body;

    if (!entityName) {
        return res.status(400).json({ error: 'Entity name is required' });
    }

    if (value === undefined || value === null) {
        return res.status(400).json({ error: 'Value is required in request body' });
    }

    const record = state.find(r => r.entityName === entityName);

    if (!record) {
        return res.status(404).json({
            error: 'Entity not found',
            message: `Entity '${entityName}' is not registered. Use /api/next-id/${entityName} first to register it.`
        });
    }

    // Only allow reset for numeric types
    if (record.dataType.startsWith('uuid')) {
        return res.status(400).json({
            error: 'Invalid operation',
            message: `Cannot reset counter for UUID type '${record.dataType}'. Reset is only available for numeric types (int, long).`
        });
    }

    // Parse the value based on type
    let numericValue;
    try {
        if (record.dataType === DataType.LONG) {
            numericValue = BigInt(value);
            if (numericValue < 0n || numericValue > MAX_LONG) {
                return res.status(400).json({
                    error: 'Invalid value',
                    message: `Value must be between 0 and ${MAX_LONG}`
                });
            }
        } else { // INT
            numericValue = parseInt(value);
            if (isNaN(numericValue) || numericValue < 0 || numericValue > MAX_INT) {
                return res.status(400).json({
                    error: 'Invalid value',
                    message: `Value must be between 0 and ${MAX_INT}`
                });
            }
        }
    } catch (error) {
        return res.status(400).json({
            error: 'Invalid value format',
            message: error.message
        });
    }

    // Auto-adjust value to be compatible with this shard
    const numValue = record.dataType === DataType.LONG ? Number(numericValue) : numericValue;
    let adjustedValue = numericValue;
    let wasAdjusted = false;

    if ((numValue % TOTAL_SHARDS) !== (SHARD_ID % TOTAL_SHARDS)) {
        // Find the next valid value for this shard that's >= requested value
        const adjustment = SHARD_ID - (numValue % TOTAL_SHARDS);
        adjustedValue = numValue + (adjustment < 0 ? adjustment + TOTAL_SHARDS : adjustment);
        wasAdjusted = true;

        if (record.dataType === DataType.LONG) {
            adjustedValue = BigInt(adjustedValue);
        }
    }

    // Calculate the iteration number from the adjusted value
    const newIteration = getIterationFromValue(adjustedValue, record.dataType);
    const previousIteration = record.currentIteration;
    const previousValue = previousIteration > 0 ?
        getNextShardedValue(previousIteration - 1, record.dataType) : null;

    // Update the iteration
    record.currentIteration = newIteration;
    saveState(state);

    const response = {
        entityName: record.entityName,
        dataType: record.dataType,
        shard: SHARD_ID,
        reset: {
            previousValue: previousValue ?
                (record.dataType === DataType.LONG ? previousValue.toString() : previousValue) : null,
            requestedValue: record.dataType === DataType.LONG ? numericValue.toString() : numericValue,
            actualValue: record.dataType === DataType.LONG ? adjustedValue.toString() : adjustedValue,
            previousIteration,
            newIteration
        },
        nextValue: getNextShardedValue(record.currentIteration, record.dataType).toString()
    };

    if (wasAdjusted) {
        response.adjusted = true;
        response.adjustmentReason = `Value adjusted from ${value} to ${adjustedValue} to match shard ${SHARD_ID} pattern`;
    }

    response.message = `Counter reset successfully. Next value will be ${getNextShardedValue(record.currentIteration, record.dataType)}`;

    res.json(response);
});

app.get('/api/list', (req, res) => {
    const entities = state.map(record => {
        let currentValue = null;
        if (!record.dataType.startsWith('uuid') && record.currentIteration > 0) {
            currentValue = getNextShardedValue(record.currentIteration - 1, record.dataType);
            if (record.dataType === DataType.LONG) {
                currentValue = currentValue.toString();
            }
        }

        return {
            entityName: record.entityName,
            dataType: record.dataType,
            currentValue,
            currentIteration: record.currentIteration,
            shard: SHARD_ID
        };
    });

    res.json({
        entities,
        shardInfo: {
            shardId: SHARD_ID,
            totalShards: TOTAL_SHARDS
        }
    });
});

app.get('/api/types', (req, res) => {
    res.json({
        availableTypes: Object.values(DataType),
        description: {
            int: `Sequential 32-bit integer (shard ${SHARD_ID}: ${SHARD_ID}, ${SHARD_ID + TOTAL_SHARDS}, ${SHARD_ID + 2*TOTAL_SHARDS}...)`,
            long: `Sequential 64-bit integer (shard ${SHARD_ID}: ${SHARD_ID}, ${SHARD_ID + TOTAL_SHARDS}, ${SHARD_ID + 2*TOTAL_SHARDS}...)`,
            uuid8: 'Random 8-character alphanumeric string (shard-aware)',
            uuid12: 'Random 12-character alphanumeric string (shard-aware)',
            uuid16: 'Random 16-character alphanumeric string (shard-aware)',
            uuid22: 'Random 22-character alphanumeric string (shard-aware)'
        },
        shardInfo: {
            shardId: SHARD_ID,
            totalShards: TOTAL_SHARDS
        }
    });
});

app.use((req, res) => {
    res.status(404).json({ error: 'Endpoint not found' });
});

app.use((error, req, res, next) => {
    console.error('Server error:', error);
    res.status(500).json({ error: 'Internal server error' });
});

const server = app.listen(PORT, '0.0.0.0', () => {
    console.log(`Unique ID Generator (Shard ${SHARD_ID}/${TOTAL_SHARDS}) listening on port ${PORT}`);
    console.log(`Data directory: ${DATA_DIR}`);
    console.log(`State file: ${DATA_FILE}`);
    console.log(`Interleaved sharding: IDs will be ${SHARD_ID}, ${SHARD_ID + TOTAL_SHARDS}, ${SHARD_ID + 2*TOTAL_SHARDS}...`);
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