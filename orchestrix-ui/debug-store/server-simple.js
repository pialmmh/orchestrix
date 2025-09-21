#!/usr/bin/env node

const WebSocket = require('ws');
const http = require('http');
const { getStoreManager } = require('./unified-store/core/StoreManager');
const { getEventBus } = require('./unified-store/events/EventBus');
const chalk = require('chalk');

const PORT = process.env.WS_PORT || 8080;

// Create HTTP server
const server = http.createServer((req, res) => {
  if (req.url === '/health') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'healthy', timestamp: Date.now() }));
  } else if (req.url === '/stores') {
    // Endpoint to view current store state
    const storeManager = getStoreManager();
    const stores = {};
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

// Initialize store manager
const storeManager = getStoreManager();
const eventBus = getEventBus();

// Track connected clients
const clients = new Set();
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

      // Handle different event types
      if (event.type === 'query' && event.operation === 'REQUEST') {
        await handleQueryRequest(ws, event);
      } else if (event.type === 'mutation' && event.operation === 'REQUEST') {
        await handleMutationRequest(ws, event);
      } else if (event.type === 'store' && event.operation === 'GET_STATE') {
        await handleGetState(ws, event);
      } else {
        // Publish to event bus for other handlers
        eventBus.publish(event);

        // Echo to all other clients
        broadcast(ws, event);
      }

    } catch (error) {
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
async function handleQueryRequest(ws, event) {
  const { id, payload } = event;

  console.log(chalk.cyan(`🔍 Processing query: ${JSON.stringify(payload)}`));

  try {
    // Get infrastructure store
    const infraStore = storeManager.getStore('infrastructure');

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

  } catch (error) {
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
async function handleMutationRequest(ws, event) {
  const { id, payload } = event;

  console.log(chalk.magenta(`✏️ Processing mutation: ${payload.operation} on ${payload.entityName}`));

  try {
    // Get infrastructure store
    const infraStore = storeManager.getStore('infrastructure');

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

  } catch (error) {
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
async function handleGetState(ws, event) {
  const { id, payload } = event;
  const { storeName } = payload || {};

  console.log(chalk.cyan(`📊 Getting state for store: ${storeName || 'all'}`));

  try {
    let storeData;

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

  } catch (error) {
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

// Broadcast to all clients except sender
function broadcast(sender, message) {
  const messageStr = JSON.stringify(message);
  clients.forEach((client) => {
    if (client !== sender && client.readyState === WebSocket.OPEN) {
      client.send(messageStr);
    }
  });
}

// Start server
server.listen(PORT, () => {
  console.log(chalk.green(`✓ Server listening on port ${PORT}`));
  console.log(chalk.gray(`  WebSocket: ws://localhost:${PORT}`));
  console.log(chalk.gray(`  HTTP: http://localhost:${PORT}/stores`));
  console.log(chalk.gray('━'.repeat(50)));
  console.log(chalk.cyan('📁 Store snapshots will be saved to: ./store-snapshots/'));
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
      // Dispose store manager
      storeManager.dispose();
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