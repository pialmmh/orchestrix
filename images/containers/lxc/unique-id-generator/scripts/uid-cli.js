#!/usr/bin/env node

/**
 * Unique ID Generator CLI Tool
 * Maintenance and management commands
 */

const fs = require('fs');
const path = require('path');

// Configuration
const DATA_DIR = process.env.DATA_DIR || '/var/lib/unique-id-generator';
const DATA_FILE = path.join(DATA_DIR, 'state.json');
const SHARD_ID = parseInt(process.env.SHARD_ID || '1');
const TOTAL_SHARDS = parseInt(process.env.TOTAL_SHARDS || '1');

const DataType = {
    INT: 'int',
    LONG: 'long',
    SNOWFLAKE: 'snowflake',
    UUID8: 'uuid8',
    UUID12: 'uuid12',
    UUID16: 'uuid16',
    UUID22: 'uuid22'
};

// Command line arguments
const args = process.argv.slice(2);
const command = args[0];

// Helper functions
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
        console.error('Error loading state:', error.message);
        process.exit(1);
    }
    return [];
}

function saveState(state) {
    try {
        if (!fs.existsSync(DATA_DIR)) {
            fs.mkdirSync(DATA_DIR, { recursive: true });
        }

        const stateToSave = state.map(entity => ({
            ...entity,
            currentIteration: entity.dataType === DataType.LONG
                ? entity.currentIteration.toString()
                : entity.currentIteration
        }));

        fs.writeFileSync(DATA_FILE, JSON.stringify(stateToSave, null, 2));
    } catch (error) {
        console.error('Error saving state:', error.message);
        process.exit(1);
    }
}

function getNextShardedValue(currentIteration, dataType) {
    const baseValue = SHARD_ID + (currentIteration * TOTAL_SHARDS);

    if (dataType === DataType.LONG) {
        return BigInt(baseValue);
    }
    return baseValue;
}

function getIterationFromValue(value, dataType) {
    const numValue = dataType === DataType.LONG ? Number(value) : value;
    return Math.floor((numValue - SHARD_ID) / TOTAL_SHARDS);
}

// Commands
const commands = {
    list: {
        description: 'List all registered entities',
        usage: 'uid-cli list',
        run: () => {
            const state = loadState();
            if (state.length === 0) {
                console.log('No entities registered');
                return;
            }

            console.log('Registered Entities:');
            console.log('===================');

            for (const entity of state) {
                let currentValue = 'N/A';

                if (!entity.dataType.startsWith('uuid') && entity.dataType !== DataType.SNOWFLAKE) {
                    if (entity.currentIteration > 0) {
                        currentValue = getNextShardedValue(entity.currentIteration - 1, entity.dataType);
                        if (entity.dataType === DataType.LONG) {
                            currentValue = currentValue.toString();
                        }
                    }
                }

                console.log(`\nEntity: ${entity.entityName}`);
                console.log(`  Type: ${entity.dataType}`);
                console.log(`  Shard: ${entity.shardId || SHARD_ID}`);
                console.log(`  Current Value: ${currentValue}`);
                console.log(`  Iteration: ${entity.currentIteration}`);

                if (!entity.dataType.startsWith('uuid') && entity.dataType !== DataType.SNOWFLAKE) {
                    const nextValue = getNextShardedValue(entity.currentIteration, entity.dataType);
                    console.log(`  Next Value: ${entity.dataType === DataType.LONG ? nextValue.toString() : nextValue}`);
                }
            }
        }
    },

    status: {
        description: 'Show status of a specific entity',
        usage: 'uid-cli status <entity-name>',
        run: (entityName) => {
            if (!entityName) {
                console.error('Entity name is required');
                process.exit(1);
            }

            const state = loadState();
            const entity = state.find(e => e.entityName === entityName);

            if (!entity) {
                console.log(`Entity '${entityName}' not found`);
                process.exit(1);
            }

            console.log(`\nEntity: ${entity.entityName}`);
            console.log(`Type: ${entity.dataType}`);
            console.log(`Shard: ${entity.shardId || SHARD_ID}`);
            console.log(`Iterations: ${entity.currentIteration}`);

            if (!entity.dataType.startsWith('uuid') && entity.dataType !== DataType.SNOWFLAKE) {
                let currentValue = 'None';
                if (entity.currentIteration > 0) {
                    currentValue = getNextShardedValue(entity.currentIteration - 1, entity.dataType);
                    if (entity.dataType === DataType.LONG) {
                        currentValue = currentValue.toString();
                    }
                }

                const nextValue = getNextShardedValue(entity.currentIteration, entity.dataType);
                console.log(`Current Value: ${currentValue}`);
                console.log(`Next Value: ${entity.dataType === DataType.LONG ? nextValue.toString() : nextValue}`);

                if (TOTAL_SHARDS > 1) {
                    console.log(`Pattern: ${SHARD_ID}, ${SHARD_ID + TOTAL_SHARDS}, ${SHARD_ID + 2*TOTAL_SHARDS}...`);
                }
            }
        }
    },

    init: {
        description: 'Initialize a new entity with specified type',
        usage: 'uid-cli init <entity-name> <type> [start-value]',
        run: (entityName, dataType, startValue) => {
            if (!entityName || !dataType) {
                console.error('Entity name and data type are required');
                console.log('Usage: uid-cli init <entity-name> <type> [start-value]');
                console.log('Types:', Object.values(DataType).join(', '));
                process.exit(1);
            }

            if (!Object.values(DataType).includes(dataType)) {
                console.error(`Invalid data type: ${dataType}`);
                console.log('Valid types:', Object.values(DataType).join(', '));
                process.exit(1);
            }

            const state = loadState();
            const existing = state.find(e => e.entityName === entityName);

            if (existing) {
                console.error(`Entity '${entityName}' already exists with type '${existing.dataType}'`);
                process.exit(1);
            }

            let initialIteration = 0;

            // Handle start value for numeric types
            if (startValue && !dataType.startsWith('uuid') && dataType !== DataType.SNOWFLAKE) {
                let numericValue;

                try {
                    if (dataType === DataType.LONG) {
                        numericValue = BigInt(startValue);
                    } else {
                        numericValue = parseInt(startValue);
                    }
                } catch (error) {
                    console.error('Invalid start value:', error.message);
                    process.exit(1);
                }

                // Auto-adjust to match shard pattern
                const numValue = dataType === DataType.LONG ? Number(numericValue) : numericValue;

                if ((numValue % TOTAL_SHARDS) !== (SHARD_ID % TOTAL_SHARDS)) {
                    const adjustment = SHARD_ID - (numValue % TOTAL_SHARDS);
                    const adjustedValue = numValue + (adjustment < 0 ? adjustment + TOTAL_SHARDS : adjustment);

                    console.log(`Adjusting start value from ${startValue} to ${adjustedValue} to match shard ${SHARD_ID}`);
                    numericValue = dataType === DataType.LONG ? BigInt(adjustedValue) : adjustedValue;
                }

                initialIteration = getIterationFromValue(numericValue, dataType) + 1;  // +1 because we want the next value to be the start value
            }

            // Create new entity
            const newEntity = {
                entityName,
                dataType,
                currentIteration: initialIteration,
                shardId: SHARD_ID
            };

            state.push(newEntity);
            saveState(state);

            console.log(`✓ Entity '${entityName}' initialized with type '${dataType}'`);

            if (!dataType.startsWith('uuid') && dataType !== DataType.SNOWFLAKE) {
                const nextValue = getNextShardedValue(initialIteration, dataType);
                console.log(`  First value will be: ${dataType === DataType.LONG ? nextValue.toString() : nextValue}`);

                if (TOTAL_SHARDS > 1) {
                    console.log(`  Pattern: ${SHARD_ID}, ${SHARD_ID + TOTAL_SHARDS}, ${SHARD_ID + 2*TOTAL_SHARDS}...`);
                }
            }
        }
    },

    reset: {
        description: 'Reset counter for a numeric entity',
        usage: 'uid-cli reset <entity-name> <value>',
        run: (entityName, value) => {
            if (!entityName || value === undefined) {
                console.error('Entity name and value are required');
                console.log('Usage: uid-cli reset <entity-name> <value>');
                process.exit(1);
            }

            const state = loadState();
            const entity = state.find(e => e.entityName === entityName);

            if (!entity) {
                console.error(`Entity '${entityName}' not found`);
                process.exit(1);
            }

            if (entity.dataType.startsWith('uuid') || entity.dataType === DataType.SNOWFLAKE) {
                console.error(`Cannot reset counter for type '${entity.dataType}'`);
                console.log('Reset is only available for numeric types (int, long)');
                process.exit(1);
            }

            let numericValue;

            try {
                if (entity.dataType === DataType.LONG) {
                    numericValue = BigInt(value);
                } else {
                    numericValue = parseInt(value);
                    if (isNaN(numericValue)) throw new Error('Invalid number');
                }
            } catch (error) {
                console.error('Invalid value:', error.message);
                process.exit(1);
            }

            // Auto-adjust to match shard pattern
            const numValue = entity.dataType === DataType.LONG ? Number(numericValue) : numericValue;

            if ((numValue % TOTAL_SHARDS) !== (SHARD_ID % TOTAL_SHARDS)) {
                const adjustment = SHARD_ID - (numValue % TOTAL_SHARDS);
                const adjustedValue = numValue + (adjustment < 0 ? adjustment + TOTAL_SHARDS : adjustment);

                console.log(`Adjusting value from ${value} to ${adjustedValue} to match shard ${SHARD_ID}`);
                numericValue = entity.dataType === DataType.LONG ? BigInt(adjustedValue) : adjustedValue;
            }

            const previousIteration = entity.currentIteration;
            const newIteration = getIterationFromValue(numericValue, entity.dataType);

            entity.currentIteration = newIteration;
            saveState(state);

            console.log(`✓ Entity '${entityName}' counter reset`);
            console.log(`  Previous iteration: ${previousIteration}`);
            console.log(`  New iteration: ${newIteration}`);

            const nextValue = getNextShardedValue(entity.currentIteration, entity.dataType);
            console.log(`  Next value will be: ${entity.dataType === DataType.LONG ? nextValue.toString() : nextValue}`);
        }
    },

    delete: {
        description: 'Delete an entity',
        usage: 'uid-cli delete <entity-name>',
        run: (entityName) => {
            if (!entityName) {
                console.error('Entity name is required');
                process.exit(1);
            }

            const state = loadState();
            const index = state.findIndex(e => e.entityName === entityName);

            if (index === -1) {
                console.error(`Entity '${entityName}' not found`);
                process.exit(1);
            }

            const entity = state[index];
            state.splice(index, 1);
            saveState(state);

            console.log(`✓ Entity '${entityName}' (type: ${entity.dataType}) deleted`);
        }
    },

    clear: {
        description: 'Clear all entities (WARNING: destructive)',
        usage: 'uid-cli clear [--force]',
        run: (force) => {
            if (force !== '--force') {
                console.log('This will delete ALL entities. Use --force to confirm.');
                process.exit(1);
            }

            const state = loadState();
            const count = state.length;

            saveState([]);
            console.log(`✓ Cleared ${count} entities`);
        }
    },

    info: {
        description: 'Show system information',
        usage: 'uid-cli info',
        run: () => {
            const state = loadState();

            console.log('Unique ID Generator - System Information');
            console.log('========================================');
            console.log(`Shard ID: ${SHARD_ID}`);
            console.log(`Total Shards: ${TOTAL_SHARDS}`);
            console.log(`Data Directory: ${DATA_DIR}`);
            console.log(`State File: ${DATA_FILE}`);
            console.log(`Entities: ${state.length}`);

            if (fs.existsSync(DATA_FILE)) {
                const stats = fs.statSync(DATA_FILE);
                console.log(`State File Size: ${stats.size} bytes`);
                console.log(`Last Modified: ${stats.mtime}`);
            }

            // Count by type
            const typeCounts = {};
            for (const entity of state) {
                typeCounts[entity.dataType] = (typeCounts[entity.dataType] || 0) + 1;
            }

            if (Object.keys(typeCounts).length > 0) {
                console.log('\nEntities by Type:');
                for (const [type, count] of Object.entries(typeCounts)) {
                    console.log(`  ${type}: ${count}`);
                }
            }
        }
    },

    help: {
        description: 'Show help information',
        usage: 'uid-cli help [command]',
        run: (cmd) => {
            if (cmd && commands[cmd]) {
                const command = commands[cmd];
                console.log(`\n${cmd}: ${command.description}`);
                console.log(`Usage: ${command.usage}\n`);
                return;
            }

            console.log('\nUnique ID Generator CLI');
            console.log('=======================\n');
            console.log('Usage: uid-cli <command> [options]\n');
            console.log('Commands:');

            for (const [name, cmd] of Object.entries(commands)) {
                console.log(`  ${name.padEnd(10)} ${cmd.description}`);
            }

            console.log('\nEnvironment Variables:');
            console.log('  DATA_DIR       Data directory (default: /var/lib/unique-id-generator)');
            console.log('  SHARD_ID       Shard ID (default: 1)');
            console.log('  TOTAL_SHARDS   Total shards (default: 1)');

            console.log('\nExamples:');
            console.log('  uid-cli list                      # List all entities');
            console.log('  uid-cli init user snowflake       # Initialize user entity with Snowflake IDs');
            console.log('  uid-cli init order int 1000       # Initialize order entity starting at 1000');
            console.log('  uid-cli reset order 5000          # Reset order counter to 5000');
            console.log('  uid-cli status user               # Show status of user entity');
            console.log('  uid-cli delete session            # Delete session entity');
            console.log('');
        }
    }
};

// Main execution
if (!command || command === 'help') {
    commands.help.run(args[1]);
} else if (commands[command]) {
    commands[command].run(...args.slice(1));
} else {
    console.error(`Unknown command: ${command}`);
    console.log('Run "uid-cli help" for available commands');
    process.exit(1);
}