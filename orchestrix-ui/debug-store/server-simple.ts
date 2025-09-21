#!/usr/bin/env tsx

import WebSocket from 'ws';
import http from 'http';
import { getStoreManager } from './unified-store/core/StoreManager';
import { getEventBus } from './unified-store/events/EventBus';
import { getEventHistoryLogger } from './unified-store/core/EventHistoryLogger';
import chalk from 'chalk';

const PORT = process.env.WS_PORT || 8080;

// Create HTTP server
const server = http.createServer((req, res) => {
  if (req.url === '/health') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'healthy', timestamp: Date.now() }));
  } else if (req.url === '/stores') {
    // Endpoint to view current store state
    const storeManager = getStoreManager();
    const stores: Record<string, any> = {};
    storeManager.getAllStores().forEach((store, name) => {
      stores[name] = store;
    });
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(stores, null, 2));
  } else {
    res.writeHead(404);
    res.end('Not found');
  }
});

// Create WebSocket server
const wss = new WebSocket.Server({ server });

console.log(chalk.cyan('🚀 Simple Store Server Starting...'));
console.log(chalk.gray('━'.repeat(50)));

// Initialize store manager and logger
const storeManager = getStoreManager();
const eventBus = getEventBus();
const eventLogger = getEventHistoryLogger();

// Track connected clients
const clients = new Set<WebSocket>();
let messageCount = 0;

// Handle WebSocket connections
wss.on('connection', (ws) => {
  const clientId = Date.now().toString();
  clients.add(ws);

  console.log(chalk.green(`✓ Client connected [${clientId}]`));
  console.log(chalk.gray(`  Total clients: ${clients.size}`));

  // Send connection confirmation
  ws.send(JSON.stringify({
    type: 'connection',
    status: 'connected',
    clientId,
    timestamp: Date.now()
  }));

  // Handle incoming messages
  ws.on('message', async (message) => {
    messageCount++;

    try {
      const event = JSON.parse(message.toString());

      console.log(chalk.blue(`📨 [${messageCount}] Received ${event.type} event`));
      console.log(chalk.gray(`  Operation: ${event.operation}`));
      console.log(chalk.gray(`  Entity: ${event.entity || 'N/A'}`));

      // Log all incoming events
      eventLogger.logEvent(event);

      // Handle different event types
      if (event.type === 'query' && event.operation === 'REQUEST') {
        await handleQueryRequest(ws, event);
      } else if (event.type === 'mutation' && event.operation === 'REQUEST') {
        await handleMutationRequest(ws, event);
      } else if (event.type === 'store' && event.operation === 'GET_STATE') {
        await handleGetState(ws, event);
      } else if (event.type === 'store-method' && event.operation === 'REQUEST') {
        await handleStoreMethod(ws, event);
      } else if (event.type === 'get-state' && event.operation === 'REQUEST') {
        await handleGetStoreState(ws, event);
      } else {
        // Publish to event bus for other handlers
        eventBus.publish(event);

        // Echo to all other clients
        broadcast(ws, event);
      }

    } catch (error: any) {
      console.error(chalk.red('❌ Error processing message:'), error);

      ws.send(JSON.stringify({
        type: 'error',
        error: error.message,
        timestamp: Date.now()
      }));
    }
  });

  // Handle client disconnect
  ws.on('close', () => {
    clients.delete(ws);
    console.log(chalk.yellow(`✗ Client disconnected [${clientId}]`));
    console.log(chalk.gray(`  Remaining clients: ${clients.size}`));
  });

  ws.on('error', (error) => {
    console.error(chalk.red(`❌ WebSocket error for client [${clientId}]:`), error);
  });
});

// Handle query requests
async function handleQueryRequest(ws: WebSocket, event: any) {
  const { id, payload } = event;

  console.log(chalk.cyan(`🔍 Processing query: ${JSON.stringify(payload)}`));

  try {
    // Get infrastructure store
    const infraStore = storeManager.getStore<any>('infrastructure');

    // Execute query through store
    const result = await infraStore.executeQuery(payload);

    // Send response
    const response = {
      id,
      type: 'query',
      operation: 'RESPONSE',
      entity: event.entity,
      payload: result,
      success: true,
      timestamp: Date.now()
    };

    ws.send(JSON.stringify(response));

    console.log(chalk.green(`✅ Query successful`));
    console.log(chalk.gray(`  Result count: ${Array.isArray(result) ? result.length : 1}`));

  } catch (error: any) {
    console.error(chalk.red('❌ Query failed:'), error);

    const errorResponse = {
      id,
      type: 'query',
      operation: 'RESPONSE',
      entity: event.entity,
      payload: null,
      success: false,
      error: error.message,
      timestamp: Date.now()
    };

    ws.send(JSON.stringify(errorResponse));
  }
}

// Handle mutation requests
async function handleMutationRequest(ws: WebSocket, event: any) {
  const { id, payload } = event;

  console.log(chalk.magenta(`✏️ Processing mutation: ${payload.operation} on ${payload.entityName}`));

  try {
    // Get infrastructure store
    const infraStore = storeManager.getStore<any>('infrastructure');

    // Execute mutation through store
    const result = await infraStore.executeMutation(payload);

    // Send response
    const response = {
      id,
      type: 'mutation',
      operation: 'RESPONSE',
      entity: event.entity,
      payload: result,
      success: true,
      timestamp: Date.now()
    };

    ws.send(JSON.stringify(response));

    console.log(chalk.green(`✅ Mutation successful`));

  } catch (error: any) {
    console.error(chalk.red('❌ Mutation failed:'), error);

    const errorResponse = {
      id,
      type: 'mutation',
      operation: 'RESPONSE',
      entity: event.entity,
      payload: null,
      success: false,
      error: error.message,
      timestamp: Date.now()
    };

    ws.send(JSON.stringify(errorResponse));
  }
}

// Handle get state requests
async function handleGetState(ws: WebSocket, event: any) {
  const { id, payload } = event;
  const { storeName } = payload || {};

  console.log(chalk.cyan(`📊 Getting state for store: ${storeName || 'all'}`));

  try {
    let storeData: Record<string, any>;

    if (storeName) {
      const store = storeManager.getStore(storeName);
      storeData = { [storeName]: store };
    } else {
      storeData = {};
      storeManager.getAllStores().forEach((store, name) => {
        storeData[name] = store;
      });
    }

    const response = {
      id,
      type: 'store',
      operation: 'STATE_RESPONSE',
      payload: storeData,
      success: true,
      timestamp: Date.now()
    };

    ws.send(JSON.stringify(response));

    console.log(chalk.green(`✅ State retrieved successfully`));

  } catch (error: any) {
    console.error(chalk.red('❌ Failed to get state:'), error);

    const errorResponse = {
      id,
      type: 'store',
      operation: 'STATE_RESPONSE',
      payload: null,
      success: false,
      error: error.message,
      timestamp: Date.now()
    };

    ws.send(JSON.stringify(errorResponse));
  }
}

// Handle store method calls
async function handleStoreMethod(ws: WebSocket, event: any) {
  const { id, payload } = event;
  const { storeName, methodName, args } = payload;

  console.log(chalk.magenta(`🔧 Calling store method: ${storeName}.${methodName}`));

  try {
    // Get the store from manager
    const store = storeManager.getStore(storeName);

    // Call the method dynamically
    let result;
    if (typeof (store as any)[methodName] === 'function') {
      result = await (store as any)[methodName](...args);
    } else {
      throw new Error(`Method ${methodName} not found on store ${storeName}`);
    }

    // Send response
    const response = {
      id,
      type: 'store-method',
      operation: 'RESPONSE',
      payload: result,
      success: true,
      timestamp: Date.now()
    };

    ws.send(JSON.stringify(response));

    console.log(chalk.green(`✅ Store method executed successfully`));

  } catch (error: any) {
    console.error(chalk.red('❌ Store method failed:'), error);

    const errorResponse = {
      id,
      type: 'store-method',
      operation: 'RESPONSE',
      payload: null,
      success: false,
      error: error.message,
      timestamp: Date.now()
    };

    ws.send(JSON.stringify(errorResponse));
  }
}

// Handle get store state requests
async function handleGetStoreState(ws: WebSocket, event: any) {
  const { id, payload } = event;
  const { storeName } = payload;

  console.log(chalk.cyan(`📊 Getting state for store: ${storeName}`));

  try {
    const store = storeManager.getStore(storeName);

    // Get observable properties from store
    const state = JSON.parse(JSON.stringify(store));

    const response = {
      id,
      type: 'get-state',
      operation: 'RESPONSE',
      payload: state,
      success: true,
      timestamp: Date.now()
    };

    ws.send(JSON.stringify(response));

    console.log(chalk.green(`✅ State retrieved successfully`));

  } catch (error: any) {
    console.error(chalk.red('❌ Failed to get state:'), error);

    const errorResponse = {
      id,
      type: 'get-state',
      operation: 'RESPONSE',
      payload: null,
      success: false,
      error: error.message,
      timestamp: Date.now()
    };

    ws.send(JSON.stringify(errorResponse));
  }
}

// Broadcast to all clients except sender
function broadcast(sender: WebSocket, message: any) {
  const messageStr = JSON.stringify(message);
  clients.forEach((client) => {
    if (client !== sender && client.readyState === WebSocket.OPEN) {
      client.send(messageStr);
    }
  });
}

// Subscribe to all EventBus events for logging
eventBus.subscribe('*', (event) => {
  eventLogger.logEvent(event);
});

// Periodically update consolidated view (every 10 seconds)
setInterval(async () => {
  await eventLogger.writeConsolidatedView();
}, 10000);

// Start server
server.listen(PORT, () => {
  console.log(chalk.green(`✓ Server listening on port ${PORT}`));
  console.log(chalk.gray(`  WebSocket: ws://localhost:${PORT}`));
  console.log(chalk.gray(`  HTTP: http://localhost:${PORT}/stores`));
  console.log(chalk.gray('━'.repeat(50)));
  console.log(chalk.cyan('📁 Store snapshots will be saved to: ./store-snapshots/'));
  console.log(chalk.cyan('📝 Event logs will be saved to: ./store-logs/'));
  console.log(chalk.gray('━'.repeat(50)));
});

// Graceful shutdown
process.on('SIGINT', () => {
  console.log(chalk.yellow('\n📛 Shutting down server...'));

  // Close all WebSocket connections
  clients.forEach((client) => {
    client.close();
  });

  // Close servers
  wss.close(() => {
    server.close(() => {
      // Dispose store manager and logger
      storeManager.dispose();
      eventLogger.destroy();
      console.log(chalk.green('✓ Server shut down gracefully'));
      process.exit(0);
    });
  });
});

// Error handling
process.on('unhandledRejection', (error) => {
  console.error(chalk.red('❌ Unhandled rejection:'), error);
});

process.on('uncaughtException', (error) => {
  console.error(chalk.red('❌ Uncaught exception:'), error);
  process.exit(1);
});