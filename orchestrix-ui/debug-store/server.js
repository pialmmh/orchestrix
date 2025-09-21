const WebSocket = require('ws');
const axios = require('axios');
const http = require('http');

// Configuration
const PORT = process.env.STORE_SERVER_PORT || 3013;
const STELLAR_API_URL = process.env.STELLAR_API_URL || 'http://localhost:8090/api';

// Create HTTP server
const server = http.createServer();

// Create WebSocket server
const wss = new WebSocket.Server({ server, path: '/store-debug' });

// Connected clients
const clients = new Set();

// Event log for debugging
const eventLog = [];
const MAX_LOG_SIZE = 10000;

// Stellar client
const stellarClient = axios.create({
  baseURL: STELLAR_API_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Log event
function logEvent(event) {
  eventLog.push({
    ...event,
    serverTimestamp: Date.now()
  });

  // Keep log size manageable
  if (eventLog.length > MAX_LOG_SIZE) {
    eventLog.splice(0, eventLog.length - MAX_LOG_SIZE);
  }
}

// Broadcast event to all clients
function broadcast(event, excludeClient = null) {
  const message = JSON.stringify(event);

  clients.forEach(client => {
    if (client !== excludeClient && client.readyState === WebSocket.OPEN) {
      client.send(message);
    }
  });
}

// Handle query request
async function handleQueryRequest(event, ws) {
  console.log(`[QUERY] Processing request ${event.id} for entity: ${event.entity}`);

  const query = event.payload;

  try {
    // Execute query against Stellar backend
    const response = await stellarClient.post('/stellar/query', query);

    let filteredData = response.data;

    // Apply criteria filtering if present
    if (query.criteria && query.kind === 'partner') {
      console.log(`[QUERY] Applying criteria filter:`, query.criteria);

      // Filter partners based on criteria
      if (query.criteria.name) {
        const nameLower = query.criteria.name.toLowerCase();
        filteredData = response.data.filter(partner =>
          partner.name && partner.name.toLowerCase() === nameLower
        );
        console.log(`[QUERY] Filtered from ${response.data.length} to ${filteredData.length} partners`);
      }
    }

    // Create success response event
    const responseEvent = {
      id: event.id,
      timestamp: Date.now(),
      type: 'query',
      operation: 'RESPONSE',
      entity: query.kind || 'unknown',
      payload: filteredData,
      metadata: {
        duration: Date.now() - event.timestamp,
        serverProcessed: true,
        originalCount: response.data.length,
        filteredCount: filteredData.length
      },
      success: true
    };

    console.log(`[QUERY] Success for ${event.id}, returning ${filteredData.length} items (filtered from ${response.data.length})`);

    // Log and broadcast response
    logEvent(responseEvent);
    broadcast(responseEvent);

  } catch (error) {
    console.error(`[QUERY] Error for ${event.id}:`, error.message);

    // Create error response event
    const errorEvent = {
      id: event.id,
      timestamp: Date.now(),
      type: 'query',
      operation: 'RESPONSE',
      entity: query.kind || 'unknown',
      payload: null,
      metadata: {
        duration: Date.now() - event.timestamp,
        serverProcessed: true
      },
      success: false,
      error: error.response?.data?.error || error.message || 'Query failed'
    };

    // Log and broadcast error
    logEvent(errorEvent);
    broadcast(errorEvent);
  }
}

// Handle mutation request
async function handleMutationRequest(event, ws) {
  console.log(`[MUTATION] Processing request ${event.id} for entity: ${event.entity}`);

  const mutation = event.payload;

  try {
    // Execute mutation against Stellar backend
    const response = await stellarClient.post('/stellar/mutate', mutation);

    // Create success response event
    const responseEvent = {
      id: event.id,
      timestamp: Date.now(),
      type: 'mutation',
      operation: 'RESPONSE',
      entity: mutation.entityName,
      payload: response.data,
      metadata: {
        duration: Date.now() - event.timestamp,
        serverProcessed: true
      },
      success: true
    };

    console.log(`[MUTATION] Success for ${event.id}`);

    // Log and broadcast response
    logEvent(responseEvent);
    broadcast(responseEvent);

  } catch (error) {
    console.error(`[MUTATION] Error for ${event.id}:`, error.message);

    // Create error response event
    const errorEvent = {
      id: event.id,
      timestamp: Date.now(),
      type: 'mutation',
      operation: 'RESPONSE',
      entity: mutation.entityName,
      payload: null,
      metadata: {
        duration: Date.now() - event.timestamp,
        serverProcessed: true
      },
      success: false,
      error: error.response?.data?.error || error.message || 'Mutation failed'
    };

    // Log and broadcast error
    logEvent(errorEvent);
    broadcast(errorEvent);
  }
}

// WebSocket connection handler
wss.on('connection', (ws, req) => {
  console.log(`[CONNECTION] New client connected from ${req.socket.remoteAddress}`);

  clients.add(ws);

  // Send welcome message with server info
  ws.send(JSON.stringify({
    type: 'system',
    operation: 'CONNECTED',
    timestamp: Date.now(),
    metadata: {
      serverVersion: '1.0.0',
      stellarUrl: STELLAR_API_URL,
      eventLogSize: eventLog.length
    }
  }));

  // Handle incoming messages
  ws.on('message', async (message) => {
    try {
      const event = JSON.parse(message);

      // Log all events
      logEvent(event);

      // Broadcast event to other clients for monitoring
      broadcast(event, ws);

      // Process different event types
      if (event.type === 'query' && event.operation === 'REQUEST') {
        await handleQueryRequest(event, ws);
      } else if (event.type === 'mutation' && event.operation === 'REQUEST') {
        await handleMutationRequest(event, ws);
      } else if (event.type === 'system' && event.operation === 'PING') {
        // Respond to ping
        ws.send(JSON.stringify({
          type: 'system',
          operation: 'PONG',
          timestamp: Date.now()
        }));
      } else {
        // Just log and broadcast other events
        console.log(`[EVENT] ${event.type}:${event.operation} - ${event.id}`);
      }

    } catch (error) {
      console.error('[ERROR] Failed to process message:', error);

      ws.send(JSON.stringify({
        type: 'system',
        operation: 'ERROR',
        timestamp: Date.now(),
        error: error.message
      }));
    }
  });

  // Handle client disconnect
  ws.on('close', () => {
    console.log('[DISCONNECT] Client disconnected');
    clients.delete(ws);
  });

  // Handle errors
  ws.on('error', (error) => {
    console.error('[ERROR] WebSocket error:', error);
    clients.delete(ws);
  });
});

// Start server
server.listen(PORT, () => {
  console.log(`
╔════════════════════════════════════════════════════════╗
║     Orchestrix Store Debug Server                     ║
║     WebSocket server for event-driven architecture    ║
╠════════════════════════════════════════════════════════╣
║  Port: ${PORT}                                        ║
║  WebSocket Path: ws://localhost:${PORT}/store-debug   ║
║  Stellar API: ${STELLAR_API_URL}                      ║
║  Status: Running                                      ║
╚════════════════════════════════════════════════════════╝
  `);

  console.log('[INFO] Waiting for WebSocket connections...');
});

// Graceful shutdown
process.on('SIGINT', () => {
  console.log('\n[SHUTDOWN] Closing server...');

  // Close all client connections
  clients.forEach(client => {
    client.send(JSON.stringify({
      type: 'system',
      operation: 'SERVER_SHUTDOWN',
      timestamp: Date.now()
    }));
    client.close();
  });

  // Close WebSocket server
  wss.close(() => {
    console.log('[SHUTDOWN] WebSocket server closed');

    // Close HTTP server
    server.close(() => {
      console.log('[SHUTDOWN] HTTP server closed');
      process.exit(0);
    });
  });
});