// Store Debug Server with XState and Log Rotation
const WebSocket = require('ws');
const express = require('express');
const cors = require('cors');
const fs = require('fs').promises;
const path = require('path');
const cron = require('node-cron');
const rfs = require('rotating-file-stream');
const { createMachine, interpret, assign } = require('xstate');

const app = express();
app.use(cors());
app.use(express.json());

const PORT = process.env.PORT || 3011;
const WS_PORT = process.env.WS_PORT || 3011;
const STORE_DIR = path.join(__dirname, 'stores');
const LOG_DIR = path.join(__dirname, 'logs');
const EVENT_LOG_DIR = path.join(__dirname, 'event-logs');

// Ensure directories exist
async function ensureDirectories() {
  await fs.mkdir(STORE_DIR, { recursive: true });
  await fs.mkdir(LOG_DIR, { recursive: true });
  await fs.mkdir(EVENT_LOG_DIR, { recursive: true });
}

// Create rotating log stream (rotates daily, keeps 1 day)
const eventLogStream = rfs.createStream('events.log', {
  interval: '1d', // rotate daily
  maxFiles: 1,    // keep only 1 day
  path: EVENT_LOG_DIR,
  compress: 'gzip' // compress old files
});

// Create XState machine for store management
const storeMachine = createMachine({
  id: 'storeDebugMachine',
  initial: 'readyForQuery',
  context: {
    // Full MobX+Stellar store kept in context
    store: {
      entities: {},
      metadata: {
        lastUpdate: Date.now(),
        eventCount: 0
      }
    },
    // Event history with store snapshots
    eventHistory: [],
    // Current operation
    currentOperation: null,
    // Error state
    lastError: null,
    // Metrics
    metrics: {
      queryCount: 0,
      mutationCount: 0,
      errorCount: 0,
      successRate: 100,
      avgResponseTime: 0
    }
  },
  states: {
    readyForQuery: {
      on: {
        QUERY: {
          target: 'loading',
          actions: 'captureEvent'
        },
        MUTATION: {
          target: 'loading',
          actions: 'captureEvent'
        },
        RESET: {
          target: 'readyForQuery',
          actions: 'resetStore'
        }
      }
    },
    loading: {
      invoke: {
        src: 'processOperation',
        onDone: {
          target: 'success',
          actions: 'handleSuccess'
        },
        onError: {
          target: 'failed',
          actions: 'handleError'
        }
      }
    },
    success: {
      after: {
        100: 'readyForQuery'
      },
      entry: 'notifySuccess'
    },
    failed: {
      after: {
        1000: 'readyForQuery'
      },
      entry: 'notifyError',
      on: {
        RETRY: {
          target: 'loading',
          actions: 'captureEvent'
        }
      }
    }
  }
}, {
  actions: {
    captureEvent: assign((context, event) => {
      const snapshot = JSON.parse(JSON.stringify(context.store));
      const historyEntry = {
        timestamp: Date.now(),
        event: event,
        stateBefore: snapshot,
        stateAfter: null // Will be filled after operation
      };
      
      // Keep only last 100 events
      const newHistory = [...context.eventHistory, historyEntry].slice(-100);
      
      return {
        ...context,
        currentOperation: event,
        eventHistory: newHistory,
        lastError: null
      };
    }),
    
    handleSuccess: assign((context, event) => {
      const { data } = event;
      const updatedStore = data.store || context.store;
      const lastHistoryEntry = context.eventHistory[context.eventHistory.length - 1];
      
      if (lastHistoryEntry) {
        lastHistoryEntry.stateAfter = JSON.parse(JSON.stringify(updatedStore));
      }
      
      // Update metrics
      const metrics = { ...context.metrics };
      if (context.currentOperation?.type === 'QUERY') {
        metrics.queryCount++;
      } else if (context.currentOperation?.type === 'MUTATION') {
        metrics.mutationCount++;
      }
      
      const totalOps = metrics.queryCount + metrics.mutationCount;
      metrics.successRate = totalOps > 0 
        ? ((totalOps - metrics.errorCount) / totalOps * 100).toFixed(2)
        : 100;
      
      return {
        ...context,
        store: updatedStore,
        currentOperation: null,
        metrics
      };
    }),
    
    handleError: assign((context, event) => {
      const { data } = event;
      const lastHistoryEntry = context.eventHistory[context.eventHistory.length - 1];
      
      if (lastHistoryEntry) {
        lastHistoryEntry.error = data?.message || 'Unknown error';
      }
      
      // Update metrics
      const metrics = { ...context.metrics };
      metrics.errorCount++;
      
      const totalOps = metrics.queryCount + metrics.mutationCount;
      metrics.successRate = totalOps > 0 
        ? ((totalOps - metrics.errorCount) / totalOps * 100).toFixed(2)
        : 100;
      
      return {
        ...context,
        lastError: data?.message || 'Unknown error',
        currentOperation: null,
        metrics
      };
    }),
    
    resetStore: assign({
      store: {
        entities: {},
        metadata: {
          lastUpdate: Date.now(),
          eventCount: 0
        }
      },
      eventHistory: [],
      currentOperation: null,
      lastError: null,
      metrics: {
        queryCount: 0,
        mutationCount: 0,
        errorCount: 0,
        successRate: 100,
        avgResponseTime: 0
      }
    }),
    
    notifySuccess: (context) => {
      console.log('Operation successful:', context.currentOperation?.type);
    },
    
    notifyError: (context) => {
      console.error('Operation failed:', context.lastError);
    }
  },
  
  services: {
    processOperation: async (context, event) => {
      const operation = context.currentOperation;
      const startTime = Date.now();
      
      try {
        // Simulate processing delay
        await new Promise(resolve => setTimeout(resolve, 100 + Math.random() * 100));
        
        let updatedStore = { ...context.store };
        
        if (operation.type === 'QUERY') {
          // Handle query operation
          const { entity, payload } = operation;
          let data = updatedStore.entities[entity] || [];
          
          // Apply filters if present
          if (payload?.filters) {
            data = applyFilters(data, payload.filters);
          }
          
          // Apply pagination if present
          if (payload?.page) {
            const { offset = 0, limit = 10 } = payload.page;
            data = data.slice(offset, offset + limit);
          }
          
          return { store: updatedStore, result: data };
          
        } else if (operation.type === 'MUTATION') {
          // Handle mutation operation
          const { entity, operation: op, payload } = operation;
          
          // Ensure entity array exists
          if (!updatedStore.entities[entity]) {
            updatedStore.entities[entity] = [];
          }
          
          let result;
          
          switch (op) {
            case 'create':
              result = createEntity(updatedStore, entity, payload);
              break;
            case 'update':
              result = updateEntity(updatedStore, entity, payload);
              break;
            case 'delete':
              result = deleteEntity(updatedStore, entity, payload.id);
              break;
            default:
              throw new Error(`Unknown operation: ${op}`);
          }
          
          updatedStore.metadata.lastUpdate = Date.now();
          updatedStore.metadata.eventCount++;
          
          return { store: updatedStore, result };
        }
        
        throw new Error(`Unknown operation type: ${operation.type}`);
        
      } catch (error) {
        throw error;
      } finally {
        const responseTime = Date.now() - startTime;
        // Update average response time
        const totalOps = context.metrics.queryCount + context.metrics.mutationCount + 1;
        context.metrics.avgResponseTime = 
          ((context.metrics.avgResponseTime * (totalOps - 1) + responseTime) / totalOps).toFixed(2);
      }
    }
  }
});

// Helper functions for entity operations
function createEntity(store, entity, data) {
  const newItem = {
    id: Date.now(),
    ...data,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  };
  
  store.entities[entity].push(newItem);
  return newItem;
}

function updateEntity(store, entity, data) {
  const items = store.entities[entity];
  const index = items.findIndex(item => item.id === data.id);
  
  if (index === -1) {
    throw new Error(`Entity not found: ${data.id}`);
  }
  
  items[index] = {
    ...items[index],
    ...data,
    updatedAt: new Date().toISOString()
  };
  
  return items[index];
}

function deleteEntity(store, entity, id) {
  const items = store.entities[entity];
  const index = items.findIndex(item => item.id === id);
  
  if (index === -1) {
    throw new Error(`Entity not found: ${id}`);
  }
  
  const deleted = items.splice(index, 1)[0];
  return deleted;
}

function applyFilters(data, filters) {
  return data.filter(item => {
    for (const [key, value] of Object.entries(filters)) {
      if (item[key] !== value) {
        return false;
      }
    }
    return true;
  });
}

// Create and start the state machine service
const storeService = interpret(storeMachine).start();

// Subscribe to state changes
storeService.subscribe((state) => {
  if (state.changed) {
    console.log(`State transition: ${state.value}`);
    // Broadcast state change to all clients
    broadcast({
      type: 'state-change',
      state: state.value,
      context: {
        metrics: state.context.metrics,
        lastError: state.context.lastError,
        eventCount: state.context.eventHistory.length
      },
      timestamp: Date.now()
    });
  }
});

// Connected WebSocket clients
const clients = new Set();

// Store management functions
async function loadStore() {
  try {
    const storePath = path.join(STORE_DIR, 'debug-store.json');
    const data = await fs.readFile(storePath, 'utf8');
    const loadedStore = JSON.parse(data);
    
    // Load store into XState context
    storeService.send({
      type: 'RESET',
      store: loadedStore
    });
    
    console.log('Store loaded from disk');
  } catch (error) {
    console.log('No existing store found, starting fresh');
  }
}

async function saveStore() {
  try {
    const currentState = storeService.getSnapshot();
    const store = currentState.context.store;
    
    const storePath = path.join(STORE_DIR, 'debug-store.json');
    await fs.writeFile(storePath, JSON.stringify(store, null, 2));
    
    // Also save timestamped backup
    const backupPath = path.join(STORE_DIR, `store-${Date.now()}.json`);
    await fs.writeFile(backupPath, JSON.stringify(store, null, 2));
  } catch (error) {
    console.error('Failed to save store:', error);
  }
}

// Log event to rotating file
function logEvent(event) {
  const logEntry = {
    timestamp: new Date().toISOString(),
    ...event
  };
  eventLogStream.write(JSON.stringify(logEntry) + '\n');
}

// Clean old store backups (keep only last 24 hours)
async function cleanOldBackups() {
  try {
    const files = await fs.readdir(STORE_DIR);
    const now = Date.now();
    const oneDayAgo = now - (24 * 60 * 60 * 1000);
    
    for (const file of files) {
      if (file.startsWith('store-') && file.endsWith('.json')) {
        const timestamp = parseInt(file.replace('store-', '').replace('.json', ''));
        if (timestamp < oneDayAgo) {
          await fs.unlink(path.join(STORE_DIR, file));
          console.log(`Deleted old backup: ${file}`);
        }
      }
    }
  } catch (error) {
    console.error('Error cleaning old backups:', error);
  }
}

// Handle store events via XState
async function handleStoreEvent(event, ws) {
  logEvent(event);
  
  return new Promise((resolve, reject) => {
    // Create XState event from store event
    const xstateEvent = {
      type: event.type.toUpperCase(),
      entity: event.entity,
      operation: event.operation,
      payload: event.payload,
      id: event.id
    };
    
    // Subscribe to state changes for this specific event
    const subscription = storeService.subscribe((state) => {
      if (state.value === 'success' && state.context.currentOperation?.id === event.id) {
        const response = {
          id: event.id,
          type: `${event.type}-success`,
          entity: event.entity,
          data: state.context.store.entities[event.entity] || [],
          timestamp: Date.now()
        };
        
        ws.send(JSON.stringify(response));
        broadcast(response);
        
        subscription.unsubscribe();
        resolve();
        
        // Save store after successful mutation
        if (event.type === 'mutation') {
          saveStore();
        }
      } else if (state.value === 'failed' && state.context.currentOperation?.id === event.id) {
        const errorResponse = {
          id: event.id,
          type: `${event.type}-error`,
          entity: event.entity,
          error: state.context.lastError,
          timestamp: Date.now()
        };
        
        ws.send(JSON.stringify(errorResponse));
        broadcast(errorResponse);
        
        subscription.unsubscribe();
        reject(new Error(state.context.lastError));
      }
    });
    
    // Send event to state machine
    storeService.send(xstateEvent);
  });
}

function broadcast(message) {
  const messageStr = JSON.stringify(message);
  clients.forEach(client => {
    if (client.readyState === WebSocket.OPEN) {
      client.send(messageStr);
    }
  });
}

// WebSocket server
const wss = new WebSocket.Server({ port: WS_PORT });

wss.on('connection', (ws) => {
  console.log('New WebSocket connection');
  clients.add(ws);
  
  // Send current store state from XState context
  const currentState = storeService.getSnapshot();
  ws.send(JSON.stringify({
    type: 'store-snapshot',
    data: currentState.context.store,
    state: currentState.value,
    metrics: currentState.context.metrics,
    timestamp: Date.now()
  }));
  
  ws.on('message', async (message) => {
    try {
      const event = JSON.parse(message);
      await handleStoreEvent(event, ws);
    } catch (error) {
      console.error('Error processing message:', error);
    }
  });
  
  ws.on('close', () => {
    clients.delete(ws);
    console.log('WebSocket connection closed');
  });
  
  ws.on('error', (error) => {
    console.error('WebSocket error:', error);
  });
});

// REST API endpoints
app.get('/health', (req, res) => {
  const state = storeService.getSnapshot();
  res.json({
    status: 'healthy',
    stateMachine: state.value,
    clients: clients.size,
    eventCount: state.context.store.metadata.eventCount,
    lastUpdate: state.context.store.metadata.lastUpdate,
    metrics: state.context.metrics
  });
});

app.get('/store', (req, res) => {
  const state = storeService.getSnapshot();
  res.json(state.context.store);
});

app.get('/store/:entity', (req, res) => {
  const { entity } = req.params;
  const state = storeService.getSnapshot();
  res.json({
    entity,
    data: state.context.store.entities[entity] || [],
    count: (state.context.store.entities[entity] || []).length
  });
});

app.post('/store/reset', async (req, res) => {
  storeService.send('RESET');
  await saveStore();
  
  broadcast({
    type: 'store-reset',
    timestamp: Date.now()
  });
  
  res.json({ success: true });
});

// XState debug endpoints
app.get('/debug/state', (req, res) => {
  const state = storeService.getSnapshot();
  res.json({
    state: state.value,
    context: {
      metrics: state.context.metrics,
      lastError: state.context.lastError,
      eventHistoryCount: state.context.eventHistory.length,
      currentOperation: state.context.currentOperation
    }
  });
});

app.get('/debug/history', (req, res) => {
  const state = storeService.getSnapshot();
  const limit = parseInt(req.query.limit) || 20;
  const history = state.context.eventHistory.slice(-limit);
  res.json(history);
});

app.get('/debug/metrics', (req, res) => {
  const state = storeService.getSnapshot();
  res.json(state.context.metrics);
});

app.get('/debug/snapshot/:index', (req, res) => {
  const state = storeService.getSnapshot();
  const index = parseInt(req.params.index);
  const historyEntry = state.context.eventHistory[index];
  
  if (!historyEntry) {
    return res.status(404).json({ error: 'History entry not found' });
  }
  
  res.json({
    event: historyEntry.event,
    timestamp: historyEntry.timestamp,
    stateBefore: historyEntry.stateBefore,
    stateAfter: historyEntry.stateAfter,
    error: historyEntry.error
  });
});

app.get('/logs/events', async (req, res) => {
  try {
    const logFile = path.join(EVENT_LOG_DIR, 'events.log');
    const logs = await fs.readFile(logFile, 'utf8');
    const lines = logs.split('\n').filter(line => line);
    const events = lines.map(line => JSON.parse(line));
    res.json(events);
  } catch (error) {
    res.json([]);
  }
});

// Schedule cleanup tasks
cron.schedule('0 0 * * *', async () => {
  console.log('Running daily cleanup...');
  await cleanOldBackups();
});

// Cleanup old logs every hour
cron.schedule('0 * * * *', async () => {
  console.log('Checking for old logs...');
  const files = await fs.readdir(EVENT_LOG_DIR);
  const now = Date.now();
  const oneDayAgo = now - (24 * 60 * 60 * 1000);
  
  for (const file of files) {
    if (file.endsWith('.gz')) {
      const filePath = path.join(EVENT_LOG_DIR, file);
      const stats = await fs.stat(filePath);
      if (stats.mtime.getTime() < oneDayAgo) {
        await fs.unlink(filePath);
        console.log(`Deleted old log: ${file}`);
      }
    }
  }
});

// Start server
async function start() {
  await ensureDirectories();
  await loadStore();
  
  app.listen(PORT, () => {
    console.log(`Store Debug Server running on http://localhost:${PORT}`);
    console.log(`WebSocket server running on ws://localhost:${WS_PORT}`);
    console.log(`Store directory: ${STORE_DIR}`);
    console.log(`Log directory: ${EVENT_LOG_DIR}`);
    console.log('Log rotation: Daily, keeping 1 day max');
  });
}

// Graceful shutdown
process.on('SIGINT', async () => {
  console.log('\nShutting down gracefully...');
  await saveStore();
  eventLogStream.end();
  process.exit(0);
});

start().catch(console.error);