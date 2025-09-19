const WebSocket = require('ws');
const axios = require('axios');
const http = require('http');
const { v4: uuidv4 } = require('uuid');
const { StoreMachineService, BaseStore } = require('./storeStateMachine');
const HistoryLogger = require('./historyLogger');

// Configuration
const PORT = process.env.STORE_SERVER_PORT || 3013;
const STELLAR_API_URL = process.env.STELLAR_API_URL || 'http://localhost:8090/api';
const ENABLE_XSTATE = process.env.ENABLE_XSTATE !== 'false'; // Default to true

// Create HTTP server
const server = http.createServer();

// Create WebSocket server
const wss = new WebSocket.Server({ server, path: '/store-debug' });

// Connected clients with session tracking
const sessions = new Map(); // sessionId -> { ws, machineService, stores }

// Initialize StoreMachineService if XState is enabled
const machineService = ENABLE_XSTATE ? new StoreMachineService({
  logDir: './logs',
  maxFileSize: 10 * 1024 * 1024,
  maxFiles: 30,
}) : null;

// Direct history logger for non-XState mode
const historyLogger = !ENABLE_XSTATE ? new HistoryLogger({
  logDir: './logs',
  maxFileSize: 10 * 1024 * 1024,
  maxFiles: 30,
}) : null;

// Stellar client
const stellarClient = axios.create({
  baseURL: STELLAR_API_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Broadcast event to all clients in a session or globally
function broadcast(event, sessionId = null, excludeClient = null) {
  const message = JSON.stringify(event);

  if (sessionId) {
    // Broadcast to specific session
    const session = sessions.get(sessionId);
    if (session && session.ws.readyState === WebSocket.OPEN) {
      session.ws.send(message);
    }
  } else {
    // Broadcast to all sessions
    sessions.forEach((session, sid) => {
      if (session.ws !== excludeClient && session.ws.readyState === WebSocket.OPEN) {
        session.ws.send(message);
      }
    });
  }
}

// Handle query request with XState
async function handleQueryWithXState(event, sessionId) {
  const session = sessions.get(sessionId);
  if (!session) return;

  const storeName = event.entity || 'default';
  
  // Get or create machine for this store
  let machine = session.stores.get(storeName);
  if (!machine) {
    machine = machineService.createMachine(sessionId, storeName);
    session.stores.set(storeName, machine);
  }

  // Send query event to state machine
  try {
    machine.send({
      type: 'QUERY',
      query: event.payload,
      eventId: event.id,
    });

    // Wait for state machine to process (this is simplified, in real app would use callbacks)
    await new Promise(resolve => setTimeout(resolve, 100));

    // Get current state snapshot
    const snapshot = machine.state.context.store.getSnapshot();

    // Create response event
    const responseEvent = {
      id: event.id,
      timestamp: Date.now(),
      type: 'query',
      operation: 'RESPONSE',
      entity: storeName,
      payload: snapshot.data,
      metadata: {
        duration: Date.now() - event.timestamp,
        serverProcessed: true,
        xstateProcessed: true,
        machineState: machine.state.value,
      },
      success: !snapshot.error,
      error: snapshot.error,
    };

    broadcast(responseEvent, sessionId);
  } catch (error) {
    console.error(`[XSTATE] Query error for ${event.id}:`, error);
    
    const errorEvent = {
      id: event.id,
      timestamp: Date.now(),
      type: 'query',
      operation: 'RESPONSE',
      entity: storeName,
      payload: null,
      metadata: {
        duration: Date.now() - event.timestamp,
        xstateError: true,
      },
      success: false,
      error: error.message,
    };

    broadcast(errorEvent, sessionId);
  }
}

// Handle query request without XState (fallback)
async function handleQueryDirect(event, sessionId) {
  console.log(`[QUERY] Processing request ${event.id} for entity: ${event.entity}`);

  const query = event.payload;

  // Log event if history logger is available
  if (historyLogger) {
    await historyLogger.logEvent({
      ...event,
      sessionId,
    });
  }

  try {
    // Execute query against Stellar backend
    const response = await stellarClient.post('/stellar/query', query);

    // Create success response event
    const responseEvent = {
      id: event.id,
      timestamp: Date.now(),
      type: 'query',
      operation: 'RESPONSE',
      entity: query.kind || 'unknown',
      payload: response.data,
      metadata: {
        duration: Date.now() - event.timestamp,
        serverProcessed: true
      },
      success: true
    };

    console.log(`[QUERY] Success for ${event.id}`);

    // Log response if history logger is available
    if (historyLogger) {
      await historyLogger.logEvent({
        ...responseEvent,
        sessionId,
      });
    }

    broadcast(responseEvent, sessionId);

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

    // Log error if history logger is available
    if (historyLogger) {
      await historyLogger.logEvent({
        ...errorEvent,
        sessionId,
      });
    }

    broadcast(errorEvent, sessionId);
  }
}

// Handle mutation request
async function handleMutationRequest(event, sessionId) {
  console.log(`[MUTATION] Processing request ${event.id} for entity: ${event.entity}`);

  const mutation = event.payload;

  // Log event
  if (historyLogger) {
    await historyLogger.logEvent({
      ...event,
      sessionId,
    });
  }

  try {
    // Execute mutation against Stellar backend
    const response = await stellarClient.post('/stellar/modify', mutation);

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

    // Log response
    if (historyLogger) {
      await historyLogger.logEvent({
        ...responseEvent,
        sessionId,
      });
    }

    broadcast(responseEvent, sessionId);

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

    // Log error
    if (historyLogger) {
      await historyLogger.logEvent({
        ...errorEvent,
        sessionId,
      });
    }

    broadcast(errorEvent, sessionId);
  }
}

// WebSocket connection handler
wss.on('connection', (ws, req) => {
  const sessionId = uuidv4();
  console.log(`[CONNECTION] New client connected - Session: ${sessionId}`);

  // Create session
  const session = {
    ws,
    stores: new Map(), // Store name -> XState machine
    connectedAt: Date.now(),
  };
  sessions.set(sessionId, session);

  // Send welcome message with session info
  ws.send(JSON.stringify({
    type: 'system',
    operation: 'CONNECTED',
    timestamp: Date.now(),
    sessionId,
    metadata: {
      serverVersion: '2.0.0',
      stellarUrl: STELLAR_API_URL,
      xstateEnabled: ENABLE_XSTATE,
      features: {
        xstate: ENABLE_XSTATE,
        history: true,
        replay: ENABLE_XSTATE,
      }
    }
  }));

  // Handle incoming messages
  ws.on('message', async (message) => {
    try {
      const event = JSON.parse(message);

      // Add session ID to event if not present
      if (!event.sessionId) {
        event.sessionId = sessionId;
      }

      // Process different event types
      if (event.type === 'query' && event.operation === 'REQUEST') {
        if (ENABLE_XSTATE) {
          await handleQueryWithXState(event, sessionId);
        } else {
          await handleQueryDirect(event, sessionId);
        }
      } else if (event.type === 'mutation' && event.operation === 'REQUEST') {
        await handleMutationRequest(event, sessionId);
      } else if (event.type === 'system') {
        if (event.operation === 'PING') {
          // Respond to ping
          ws.send(JSON.stringify({
            type: 'system',
            operation: 'PONG',
            timestamp: Date.now(),
            sessionId
          }));
        } else if (event.operation === 'GET_HISTORY' && ENABLE_XSTATE) {
          // Get history for this session
          const history = await machineService.getHistory(sessionId, event.limit || 100);
          ws.send(JSON.stringify({
            type: 'system',
            operation: 'HISTORY',
            timestamp: Date.now(),
            sessionId,
            payload: history
          }));
        } else if (event.operation === 'REPLAY_EVENTS' && ENABLE_XSTATE) {
          // Replay events
          const snapshot = await machineService.replayEvents(
            sessionId,
            event.fromTimestamp,
            event.toTimestamp
          );
          ws.send(JSON.stringify({
            type: 'system',
            operation: 'REPLAY_RESULT',
            timestamp: Date.now(),
            sessionId,
            payload: snapshot
          }));
        } else if (event.operation === 'GET_STATS') {
          // Get statistics
          const stats = ENABLE_XSTATE 
            ? await machineService.getStats()
            : historyLogger ? historyLogger.getStats() : null;
          
          ws.send(JSON.stringify({
            type: 'system',
            operation: 'STATS',
            timestamp: Date.now(),
            sessionId,
            payload: stats
          }));
        }
      } else {
        // Just log other events
        console.log(`[EVENT] ${event.type}:${event.operation} - ${event.id}`);
      }

    } catch (error) {
      console.error('[ERROR] Failed to process message:', error);

      ws.send(JSON.stringify({
        type: 'system',
        operation: 'ERROR',
        timestamp: Date.now(),
        sessionId,
        error: error.message
      }));
    }
  });

  // Handle client disconnect
  ws.on('close', () => {
    console.log(`[DISCONNECT] Client disconnected - Session: ${sessionId}`);
    
    // Clean up session
    const session = sessions.get(sessionId);
    if (session) {
      // Stop all state machines for this session
      if (ENABLE_XSTATE) {
        session.stores.forEach(machine => machine.stop());
      }
      sessions.delete(sessionId);
    }
  });

  // Handle errors
  ws.on('error', (error) => {
    console.error(`[ERROR] WebSocket error for session ${sessionId}:`, error);
    sessions.delete(sessionId);
  });
});

// Admin HTTP endpoints
server.on('request', async (req, res) => {
  const url = new URL(req.url, `http://${req.headers.host}`);
  
  if (url.pathname === '/admin/stats' && req.method === 'GET') {
    // Return server statistics
    const stats = {
      sessions: sessions.size,
      xstateEnabled: ENABLE_XSTATE,
      uptime: process.uptime(),
      memory: process.memoryUsage(),
    };
    
    if (ENABLE_XSTATE) {
      stats.machineStats = await machineService.getStats();
    } else if (historyLogger) {
      stats.historyStats = historyLogger.getStats();
    }
    
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(stats, null, 2));
  } else if (url.pathname === '/admin/history' && req.method === 'GET') {
    // Return recent history
    const limit = parseInt(url.searchParams.get('limit') || '100');
    const sessionId = url.searchParams.get('sessionId');
    
    let history = [];
    if (ENABLE_XSTATE && sessionId) {
      history = await machineService.getHistory(sessionId, limit);
    } else if (historyLogger) {
      history = await historyLogger.getRecentEvents(limit);
    }
    
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(history, null, 2));
  } else {
    res.writeHead(404);
    res.end('Not Found');
  }
});

// Start server
server.listen(PORT, () => {
  console.log(`
╔════════════════════════════════════════════════════════╗
║     Orchestrix Store Debug Server v2.0                ║
║     WebSocket + XState Event-Driven Architecture      ║
╠════════════════════════════════════════════════════════╣
║  Port: ${PORT}                                        ║
║  WebSocket: ws://localhost:${PORT}/store-debug        ║
║  Stellar API: ${STELLAR_API_URL}                      ║
║  XState: ${ENABLE_XSTATE ? 'Enabled' : 'Disabled'}                                  ║
║  Admin Stats: http://localhost:${PORT}/admin/stats    ║
║  Admin History: http://localhost:${PORT}/admin/history║
║  Status: Running                                      ║
╚════════════════════════════════════════════════════════╝
  `);

  console.log('[INFO] Waiting for WebSocket connections...');
  console.log('[INFO] XState features:', ENABLE_XSTATE ? 'State machines, history tracking, event replay' : 'Direct query processing');
});

// Graceful shutdown
process.on('SIGINT', () => {
  console.log('\n[SHUTDOWN] Closing server...');

  // Close all sessions
  sessions.forEach((session, sessionId) => {
    session.ws.send(JSON.stringify({
      type: 'system',
      operation: 'SERVER_SHUTDOWN',
      timestamp: Date.now(),
      sessionId
    }));
    
    // Stop state machines
    if (ENABLE_XSTATE) {
      session.stores.forEach(machine => machine.stop());
    }
    
    session.ws.close();
  });

  // Clean up services
  if (machineService) {
    machineService.cleanup();
  }
  if (historyLogger) {
    historyLogger.close();
  }

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

module.exports = {
  server,
  wss,
  sessions,
  machineService,
};
