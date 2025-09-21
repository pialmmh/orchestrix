// Store Debug Server with XState and Full MobX Store in Context
const { createMachine, interpret, assign } = require('xstate');
const WebSocket = require('ws');
const express = require('express');
const cors = require('cors');
const fs = require('fs').promises;
const path = require('path');
const cron = require('node-cron');
const rfs = require('rotating-file-stream');

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

// Connected WebSocket clients
const clients = new Set();

// Initial MobX-like store structure
const initialStore = {
  entities: {
    compute: [],
    datacenter: [],
    partner: [],
    cloud: [],
    networkDevice: [],
    container: [],
    storage: []
  },
  queries: {},
  mutations: {},
  cache: {},
  metadata: {
    lastUpdate: Date.now(),
    version: '1.0.0'
  }
};

// Load store from disk if exists
async function loadInitialStore() {
  try {
    const storePath = path.join(STORE_DIR, 'debug-store.json');
    const data = await fs.readFile(storePath, 'utf8');
    console.log('Store loaded from disk');
    return JSON.parse(data);
  } catch (error) {
    console.log('No existing store found, starting fresh');
    return initialStore;
  }
}

// Save store to disk
async function saveStoreToDisk(store) {
  try {
    const storePath = path.join(STORE_DIR, 'debug-store.json');
    await fs.writeFile(storePath, JSON.stringify(store, null, 2));
    
    // Also save timestamped backup
    const backupPath = path.join(STORE_DIR, `store-${Date.now()}.json`);
    await fs.writeFile(backupPath, JSON.stringify(store, null, 2));
  } catch (error) {
    console.error('Failed to save store:', error);
  }
}

// Create the XState machine with full store in context
const storeMachine = createMachine({
  id: 'storeDebug',
  initial: 'readyForQuery',
  predictableActionArguments: true,
  context: {
    // THE ENTIRE MOBX STORE STATE
    store: initialStore,
    
    // Current event being processed
    currentEvent: null,
    
    // Event history with store snapshots
    eventHistory: [],
    
    // Error log
    errorLog: [],
    
    // Metrics
    metrics: {
      totalQueries: 0,
      totalMutations: 0,
      successfulQueries: 0,
      successfulMutations: 0,
      failedOperations: 0,
      avgQueryTime: 0,
      avgMutationTime: 0
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
        }
      }
    },
    
    loading: {
      entry: 'logLoadingStart',
      invoke: {
        id: 'processStoreOperation',
        src: 'processStoreOperation',
        onDone: {
          target: 'success',
          actions: ['updateStore', 'logSuccess']
        },
        onError: {
          target: 'failed',
          actions: ['logError', 'captureError']
        }
      }
    },
    
    success: {
      entry: ['broadcastSuccess', 'persistStore'],
      always: {
        target: 'readyForQuery'
      }
    },
    
    failed: {
      entry: 'broadcastError',
      on: {
        RETRY: 'loading',
        RESET: 'readyForQuery'
      },
      after: {
        3000: 'readyForQuery' // Auto-recovery after 3 seconds
      }
    }
  }
}, {
  actions: {
    captureEvent: assign({
      currentEvent: (context, event) => ({
        id: event.id,
        type: event.type,
        entity: event.entity,
        operation: event.operation,
        payload: event.payload,
        timestamp: Date.now(),
        storeBeforeOperation: JSON.parse(JSON.stringify(context.store))
      }),
      eventHistory: (context, event) => {
        const newEvent = {
          id: event.id,
          type: event.type,
          entity: event.entity,
          operation: event.operation,
          timestamp: Date.now(),
          state: 'started',
          storeSnapshot: JSON.parse(JSON.stringify(context.store))
        };
        
        // Keep only last 100 events to prevent memory issues
        const history = [...context.eventHistory, newEvent];
        if (history.length > 100) {
          history.shift();
        }
        return history;
      }
    }),
    
    logLoadingStart: (context) => {
      console.log(`[Loading] Processing ${context.currentEvent.type} for ${context.currentEvent.entity}`);
      logEvent({
        ...context.currentEvent,
        state: 'loading'
      });
    },
    
    updateStore: assign({
      store: (context, event) => event.data.newStore,
      eventHistory: (context, event) => {
        const history = [...context.eventHistory];
        const lastEvent = history[history.length - 1];
        if (lastEvent) {
          lastEvent.endTime = Date.now();
          lastEvent.duration = lastEvent.endTime - lastEvent.timestamp;
          lastEvent.storeAfter = event.data.newStore;
          lastEvent.result = 'success';
          lastEvent.response = event.data.result;
        }
        return history;
      },
      metrics: (context, event) => {
        const isQuery = context.currentEvent.type === 'query';
        const duration = Date.now() - context.currentEvent.timestamp;
        
        if (isQuery) {
          const totalQueries = context.metrics.totalQueries + 1;
          const successfulQueries = context.metrics.successfulQueries + 1;
          const avgQueryTime = 
            (context.metrics.avgQueryTime * context.metrics.totalQueries + duration) / totalQueries;
          
          return {
            ...context.metrics,
            totalQueries,
            successfulQueries,
            avgQueryTime
          };
        } else {
          const totalMutations = context.metrics.totalMutations + 1;
          const successfulMutations = context.metrics.successfulMutations + 1;
          const avgMutationTime = 
            (context.metrics.avgMutationTime * context.metrics.totalMutations + duration) / totalMutations;
          
          return {
            ...context.metrics,
            totalMutations,
            successfulMutations,
            avgMutationTime
          };
        }
      }
    }),
    
    logSuccess: (context) => {
      console.log(`[Success] Completed ${context.currentEvent.type} for ${context.currentEvent.entity}`);
      logEvent({
        ...context.currentEvent,
        state: 'success',
        duration: Date.now() - context.currentEvent.timestamp
      });
    },
    
    logError: (context, event) => {
      console.error(`[Error] Failed ${context.currentEvent.type} for ${context.currentEvent.entity}:`, event.data);
      logEvent({
        ...context.currentEvent,
        state: 'error',
        error: event.data
      });
    },
    
    captureError: assign({
      errorLog: (context, event) => {
        const error = {
          event: context.currentEvent,
          error: event.data,
          timestamp: Date.now(),
          store: context.store
        };
        
        const log = [...context.errorLog, error];
        if (log.length > 50) {
          log.shift();
        }
        return log;
      },
      metrics: (context) => ({
        ...context.metrics,
        failedOperations: context.metrics.failedOperations + 1
      })
    }),
    
    broadcastSuccess: (context) => {
      const response = {
        id: context.currentEvent.id,
        type: `${context.currentEvent.type}-success`,
        entity: context.currentEvent.entity,
        data: context.eventHistory[context.eventHistory.length - 1]?.response,
        timestamp: Date.now()
      };
      broadcast(response);
    },
    
    broadcastError: (context) => {
      const response = {
        id: context.currentEvent.id,
        type: `${context.currentEvent.type}-error`,
        entity: context.currentEvent.entity,
        error: context.errorLog[context.errorLog.length - 1]?.error,
        timestamp: Date.now()
      };
      broadcast(response);
    },
    
    persistStore: async (context) => {
      await saveStoreToDisk(context.store);
    }
  },
  
  services: {
    processStoreOperation: async (context, event) => {
      const { currentEvent } = context;
      
      // Simulate API delay
      await new Promise(resolve => setTimeout(resolve, 100));
      
      if (currentEvent.type === 'query') {
        return processQuery(context.store, currentEvent);
      } else if (currentEvent.type === 'mutation') {
        return processMutation(context.store, currentEvent);
      }
      
      throw new Error(`Unknown event type: ${currentEvent.type}`);
    }
  }
});

// Process query operation
async function processQuery(store, event) {
  const { entity, operation, payload } = event;
  
  let result = store.entities[entity] || [];
  
  // Apply filters if present
  if (payload?.filters) {
    result = result.filter(item => {
      for (const [key, value] of Object.entries(payload.filters)) {
        if (item[key] !== value) {
          return false;
        }
      }
      return true;
    });
  }
  
  // Apply pagination if present
  if (payload?.page) {
    const { offset = 0, limit = 10 } = payload.page;
    result = result.slice(offset, offset + limit);
  }
  
  // Update store's query cache
  const newStore = {
    ...store,
    queries: {
      ...store.queries,
      [event.id]: {
        result,
        timestamp: Date.now()
      }
    },
    metadata: {
      ...store.metadata,
      lastUpdate: Date.now()
    }
  };
  
  return { newStore, result };
}

// Process mutation operation
async function processMutation(store, event) {
  const { entity, operation, payload } = event;
  
  // Deep clone for immutability
  const newStore = JSON.parse(JSON.stringify(store));
  
  // Ensure entity array exists
  if (!newStore.entities[entity]) {
    newStore.entities[entity] = [];
  }
  
  let result;
  
  switch (operation) {
    case 'create':
      result = {
        ...payload,
        id: Date.now(),
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      };
      newStore.entities[entity].push(result);
      break;
      
    case 'update':
      const updateIndex = newStore.entities[entity].findIndex(e => e.id === payload.id);
      if (updateIndex !== -1) {
        result = {
          ...newStore.entities[entity][updateIndex],
          ...payload,
          updatedAt: new Date().toISOString()
        };
        newStore.entities[entity][updateIndex] = result;
      } else {
        throw new Error(`Entity not found: ${payload.id}`);
      }
      break;
      
    case 'delete':
      const deleteIndex = newStore.entities[entity].findIndex(e => e.id === payload.id);
      if (deleteIndex !== -1) {
        result = newStore.entities[entity].splice(deleteIndex, 1)[0];
      } else {
        throw new Error(`Entity not found: ${payload.id}`);
      }
      break;
      
    default:
      throw new Error(`Unknown operation: ${operation}`);
  }
  
  // Update metadata
  newStore.metadata.lastUpdate = Date.now();
  
  // Track mutation
  newStore.mutations[event.id] = {
    operation,
    entity,
    result,
    timestamp: Date.now()
  };
  
  return { newStore, result };
}

// Log event to rotating file
function logEvent(event) {
  const logEntry = {
    timestamp: new Date().toISOString(),
    ...event
  };
  eventLogStream.write(JSON.stringify(logEntry) + '\n');
}

// Broadcast to all connected clients
function broadcast(message) {
  const messageStr = JSON.stringify(message);
  clients.forEach(client => {
    if (client.readyState === WebSocket.OPEN) {
      client.send(messageStr);
    }
  });
}

// Initialize XState service
let service;

async function initializeService() {
  const initialStoreData = await loadInitialStore();
  
  service = interpret(storeMachine.withContext({
    ...storeMachine.context,
    store: initialStoreData
  }));
  
  // Add state change listener
  service.onTransition((state) => {
    console.log(`State: ${state.value}, Events processed: ${state.context.eventHistory.length}`);
  });
  
  service.start();
  
  return service;
}

// WebSocket server
const wss = new WebSocket.Server({ port: WS_PORT });

wss.on('connection', (ws) => {
  console.log('New WebSocket connection');
  clients.add(ws);
  
  // Send current state and store
  ws.send(JSON.stringify({
    type: 'state-snapshot',
    state: service.state.value,
    store: service.state.context.store,
    metrics: service.state.context.metrics,
    timestamp: Date.now()
  }));
  
  ws.on('message', async (message) => {
    try {
      const event = JSON.parse(message);
      
      // Send event to state machine
      if (event.type === 'query') {
        service.send('QUERY', event);
      } else if (event.type === 'mutation') {
        service.send('MUTATION', event);
      }
    } catch (error) {
      console.error('Error processing message:', error);
      ws.send(JSON.stringify({
        type: 'error',
        error: error.message
      }));
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

// Get current state and store
app.get('/debug/state', (req, res) => {
  res.json({
    state: service.state.value,
    store: service.state.context.store,
    metrics: service.state.context.metrics,
    eventCount: service.state.context.eventHistory.length,
    canAcceptQuery: service.state.matches('readyForQuery')
  });
});

// Get complete context (including history)
app.get('/debug/context', (req, res) => {
  res.json(service.state.context);
});

// Get event history
app.get('/debug/history', (req, res) => {
  res.json(service.state.context.eventHistory);
});

// Get specific event from history
app.get('/debug/history/:index', (req, res) => {
  const index = parseInt(req.params.index);
  const event = service.state.context.eventHistory[index];
  
  if (!event) {
    return res.status(404).json({ error: 'Event not found' });
  }
  
  // Compute diff if store snapshots exist
  let diff = null;
  if (event.storeSnapshot && event.storeAfter) {
    diff = computeStoreDiff(event.storeSnapshot, event.storeAfter);
  }
  
  res.json({
    event,
    diff
  });
});

// Get error log
app.get('/debug/errors', (req, res) => {
  res.json(service.state.context.errorLog);
});

// Get metrics
app.get('/debug/metrics', (req, res) => {
  res.json(service.state.context.metrics);
});

// Replay events up to a certain point
app.post('/debug/replay', async (req, res) => {
  const { upToIndex } = req.body;
  const events = service.state.context.eventHistory.slice(0, upToIndex + 1);
  
  // Start with initial store
  let replayedStore = initialStore;
  
  for (const event of events) {
    if (event.storeAfter) {
      replayedStore = event.storeAfter;
    }
  }
  
  res.json({
    replayedStore,
    eventsReplayed: events.length
  });
});

// Reset state machine
app.post('/debug/reset', async (req, res) => {
  service.stop();
  await initializeService();
  
  broadcast({
    type: 'state-reset',
    timestamp: Date.now()
  });
  
  res.json({ success: true });
});

// Export state for XState visualizer
app.get('/debug/export', (req, res) => {
  res.json({
    machine: storeMachine.config,
    state: service.state.value,
    context: service.state.context
  });
});

// Health check
app.get('/health', (req, res) => {
  res.json({
    status: 'healthy',
    state: service.state.value,
    clients: clients.size,
    eventCount: service.state.context.eventHistory.length,
    metrics: service.state.context.metrics
  });
});

// Compute diff between two store states
function computeStoreDiff(before, after) {
  const diff = {
    added: {},
    modified: {},
    deleted: {}
  };
  
  // Check for additions and modifications
  for (const entity in after.entities) {
    if (!before.entities[entity]) {
      diff.added[entity] = after.entities[entity];
    } else {
      const beforeItems = before.entities[entity];
      const afterItems = after.entities[entity];
      
      if (JSON.stringify(beforeItems) !== JSON.stringify(afterItems)) {
        diff.modified[entity] = {
          before: beforeItems,
          after: afterItems
        };
      }
    }
  }
  
  // Check for deletions
  for (const entity in before.entities) {
    if (!after.entities[entity]) {
      diff.deleted[entity] = before.entities[entity];
    }
  }
  
  return diff;
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

// Schedule cleanup tasks
cron.schedule('0 0 * * *', async () => {
  console.log('Running daily cleanup...');
  await cleanOldBackups();
});

// Start server
async function start() {
  await ensureDirectories();
  await initializeService();
  
  app.listen(PORT, () => {
    console.log(`\n🚀 Store Debug Server with XState running`);
    console.log(`📊 REST API: http://localhost:${PORT}`);
    console.log(`🔌 WebSocket: ws://localhost:${WS_PORT}`);
    console.log(`📁 Store directory: ${STORE_DIR}`);
    console.log(`📝 Event logs: ${EVENT_LOG_DIR}`);
    console.log(`🔄 State: ${service.state.value}`);
    console.log(`⚡ Log rotation: Daily, keeping 1 day max\n`);
  });
}

// Graceful shutdown
process.on('SIGINT', async () => {
  console.log('\nShutting down gracefully...');
  await saveStoreToDisk(service.state.context.store);
  eventLogStream.end();
  service.stop();
  process.exit(0);
});

start().catch(console.error);