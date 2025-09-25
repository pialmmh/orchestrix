const express = require('express');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const app = express();
app.use(express.json());

const DATA_DIR = '/var/lib/sequence-service';
const DATA_FILE = path.join(DATA_DIR, 'state.json');
const PORT = process.env.PORT || 7001;

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

function generateUUID(length) {
    // Generate random bytes and convert to base64-like string (alphanumeric)
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    let result = '';
    const bytes = crypto.randomBytes(length);
    for (let i = 0; i < length; i++) {
        result += chars[bytes[i] % chars.length];
    }
    return result;
}

function loadState() {
    try {
        if (fs.existsSync(DATA_FILE)) {
            const data = fs.readFileSync(DATA_FILE, 'utf8');
            const state = JSON.parse(data);

            for (const entity of state) {
                if (entity.dataType === DataType.LONG && typeof entity.currentVal === 'string') {
                    entity.currentVal = BigInt(entity.currentVal);
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
        currentVal: entity.dataType === DataType.LONG
            ? entity.currentVal.toString()
            : entity.currentVal
    }));

    fs.writeFileSync(DATA_FILE, JSON.stringify(stateToSave, null, 2));
}

let state = loadState();

app.get('/health', (req, res) => {
    res.json({ status: 'healthy', uptime: process.uptime() });
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
            currentVal: dataType === DataType.LONG ? 1n :
                       dataType === DataType.INT ? 1 : null
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
    // Handle numeric types
    else {
        nextValue = record.currentVal;

        if (record.dataType === DataType.INT) {
            if (record.currentVal >= MAX_INT) {
                return res.status(500).json({
                    error: 'Integer overflow',
                    message: `Entity '${entityName}' has reached maximum int value`
                });
            }
            record.currentVal++;
        } else if (record.dataType === DataType.LONG) {
            if (record.currentVal >= MAX_LONG) {
                return res.status(500).json({
                    error: 'Long overflow',
                    message: `Entity '${entityName}' has reached maximum long value`
                });
            }
            record.currentVal++;
        }
    }

    saveState(state);

    res.json({
        entityName,
        dataType: record.dataType,
        value: record.dataType === DataType.LONG ? nextValue.toString() : nextValue
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
            values
        });
    }

    // Numeric types return range
    let record = state.find(r => r.entityName === entityName);

    if (!record) {
        record = {
            entityName,
            dataType,
            currentVal: dataType === DataType.LONG ? 1n : 1
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

    const startValue = record.currentVal;
    let endValue;

    if (record.dataType === DataType.INT) {
        endValue = record.currentVal + size - 1;
        if (endValue > MAX_INT) {
            return res.status(500).json({
                error: 'Integer overflow',
                message: `Batch size ${size} would exceed maximum int value`
            });
        }
        record.currentVal += size;
    } else {
        endValue = record.currentVal + BigInt(size) - 1n;
        if (endValue > MAX_LONG) {
            return res.status(500).json({
                error: 'Long overflow',
                message: `Batch size ${size} would exceed maximum long value`
            });
        }
        record.currentVal += BigInt(size);
    }

    saveState(state);

    res.json({
        entityName,
        dataType: record.dataType,
        batchSize: size,
        startValue: record.dataType === DataType.LONG ? startValue.toString() : startValue,
        endValue: record.dataType === DataType.LONG ? endValue.toString() : endValue
    });
});

app.get('/api/status/:entityName', (req, res) => {
    const { entityName } = req.params;
    const record = state.find(r => r.entityName === entityName);

    if (!record) {
        return res.status(404).json({ error: 'Entity not found' });
    }

    res.json({
        entityName: record.entityName,
        dataType: record.dataType,
        currentValue: record.dataType === DataType.LONG
            ? record.currentVal.toString()
            : record.currentVal
    });
});

app.get('/api/list', (req, res) => {
    const entities = state.map(record => ({
        entityName: record.entityName,
        dataType: record.dataType,
        currentValue: record.dataType === DataType.LONG
            ? record.currentVal.toString()
            : record.currentVal
    }));

    res.json({ entities });
});

app.get('/api/types', (req, res) => {
    res.json({
        availableTypes: Object.values(DataType),
        description: {
            int: 'Sequential 32-bit integer (1 to 2,147,483,647)',
            long: 'Sequential 64-bit integer (1 to 9,223,372,036,854,775,807)',
            uuid8: 'Random 8-character alphanumeric string',
            uuid12: 'Random 12-character alphanumeric string',
            uuid16: 'Random 16-character alphanumeric string',
            uuid22: 'Random 22-character alphanumeric string'
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
    console.log(`Sequence service listening on port ${PORT}`);
    console.log(`Data directory: ${DATA_DIR}`);
    console.log(`State file: ${DATA_FILE}`);
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