const WebSocket = require('ws');
const ApiService = require('./ApiService');

class EventBusWithMutations {
  constructor(port = 8081) {
    this.port = port;
    this.wss = null;
    this.clients = new Set();
    this.storeManager = null;
    this.apiService = new ApiService();
  }

  setStoreManager(storeManager) {
    this.storeManager = storeManager;
  }

  start() {
    this.wss = new WebSocket.Server({ port: this.port });

    this.wss.on('connection', (ws) => {
      console.log('New client connected');
      this.clients.add(ws);

      ws.on('message', (message) => {
        try {
          const data = JSON.parse(message);
          this.handleMessage(ws, data);
        } catch (error) {
          console.error('Invalid message:', error);
          this.sendError(ws, 'Invalid message format');
        }
      });

      ws.on('close', () => {
        console.log('Client disconnected');
        this.clients.delete(ws);
      });

      ws.on('error', (error) => {
        console.error('WebSocket error:', error);
        this.clients.delete(ws);
      });

      // Send connection confirmation
      this.send(ws, {
        type: 'CONNECTION',
        status: 'connected',
        timestamp: Date.now()
      });
    });

    console.log(`EventBus WebSocket server started on port ${this.port}`);
  }

  async handleMessage(ws, message) {
    const { type, entity, operation, payload, query, messages } = message;

    console.log(`Handling ${type} for ${entity}`, operation || '');

    switch (type) {
      case 'QUERY':
        await this.handleQuery(ws, entity, query || payload);
        break;
      case 'MUTATION':
        await this.handleMutation(ws, entity, operation, payload);
        break;
      case 'SUBSCRIBE':
        this.handleSubscribe(ws, entity);
        break;
      case 'UNSUBSCRIBE':
        this.handleUnsubscribe(ws, entity);
        break;
      case 'CONSOLE_LOGS':
        await this.handleConsoleLogs(ws, messages, message);
        break;
      default:
        this.sendError(ws, `Unknown message type: ${type}`);
    }
  }

  /**
   * Handle QUERY messages
   */
  async handleQuery(ws, entity, query) {
    if (!this.storeManager) {
      this.sendError(ws, 'Store manager not initialized');
      return;
    }

    try {
      // Update store with query
      const store = this.storeManager.handleQuery(entity, query);
      if (!store) {
        this.sendError(ws, `Store not found: ${entity}`);
        return;
      }

      // Fetch data from API
      let data;
      if (entity === 'infrastructure') {
        data = await this.apiService.fetchInfrastructure(query);
      } else {
        const result = await this.apiService.query(query);
        data = result.success ? result.data : [];
      }

      // Update store with result
      this.storeManager.handleQueryResult(entity, data, [1]);

      // Send response
      this.send(ws, {
        type: 'QUERY_RESULT',
        entity: entity,
        data: this.storeManager.getStoreState(entity),
        timestamp: Date.now()
      });

      // Broadcast to subscribers
      this.broadcastStoreUpdate(entity);

    } catch (error) {
      console.error(`Query failed for ${entity}:`, error);
      this.sendError(ws, `Query failed: ${error.message}`);
    }
  }

  /**
   * Handle MUTATION messages
   */
  async handleMutation(ws, entity, operation, payload) {
    if (!this.storeManager) {
      this.sendError(ws, 'Store manager not initialized');
      return;
    }

    try {
      console.log(`Executing ${operation} mutation on ${entity}`);

      // 1. Execute mutation on database
      const mutationResult = await this.apiService.mutate(entity, operation, payload);

      if (!mutationResult.success) {
        this.sendError(ws, `Mutation failed: ${mutationResult.error}`);
        return;
      }

      console.log(`Mutation ${operation} succeeded, refreshing store...`);

      // 2. Get the store's current query
      const store = this.storeManager.getStore(entity);
      if (!store || !store.query) {
        // If no query, just send success
        this.send(ws, {
          type: 'MUTATION_SUCCESS',
          entity: entity,
          operation: operation,
          data: mutationResult.data,
          timestamp: Date.now()
        });
        return;
      }

      // 3. Re-execute the current query to get fresh data
      console.log('Re-executing query to refresh store...');
      let freshData;
      if (entity === 'infrastructure') {
        freshData = await this.apiService.fetchInfrastructure(store.query);
      } else {
        const result = await this.apiService.query(store.query);
        freshData = result.success ? result.data : store.data;
      }

      // 4. Update store with fresh data
      this.storeManager.handleQueryResult(entity, freshData, store.meta.pagesLoaded);

      // 5. Send success response with updated store
      this.send(ws, {
        type: 'MUTATION_SUCCESS',
        entity: entity,
        operation: operation,
        data: this.storeManager.getStoreState(entity),
        timestamp: Date.now()
      });

      // 6. Broadcast update to all subscribers
      console.log('Broadcasting store update to all subscribers...');
      this.broadcastStoreUpdate(entity);

    } catch (error) {
      console.error(`Mutation ${operation} failed:`, error);
      this.sendError(ws, `Mutation failed: ${error.message}`);
    }
  }

  /**
   * Handle SUBSCRIBE messages
   */
  handleSubscribe(ws, entity) {
    if (!ws.subscriptions) {
      ws.subscriptions = new Set();
    }
    ws.subscriptions.add(entity);

    // Send current state if available
    if (this.storeManager) {
      const state = this.storeManager.getStoreState(entity);
      if (state) {
        this.send(ws, {
          type: 'SUBSCRIPTION_DATA',
          entity: entity,
          data: state,
          timestamp: Date.now()
        });
      }
    }

    console.log(`Client subscribed to ${entity}`);
  }

  /**
   * Handle UNSUBSCRIBE messages
   */
  handleUnsubscribe(ws, entity) {
    if (ws.subscriptions) {
      ws.subscriptions.delete(entity);
      console.log(`Client unsubscribed from ${entity}`);
    }
  }

  /**
   * Handle CONSOLE_LOGS messages - store frontend console logs
   */
  async handleConsoleLogs(ws, messages, fullMessage) {
    try {
      const fs = require('fs').promises;
      const path = require('path');

      // Create debug directory if it doesn't exist
      const debugDir = path.join(__dirname, '..', 'store-debug');
      await fs.mkdir(debugDir, { recursive: true });

      // Append to console logs file (same as store-events file pattern)
      const date = new Date();
      const dateStr = date.toISOString().split('T')[0]; // YYYY-MM-DD
      const timeStr = date.toISOString().split('T')[1].replace(/:/g, '-').split('.')[0]; // HH-MM-SS
      const consoleLogFile = path.join(debugDir, `console-logs-${dateStr}.jsonl`);

      // Format each message and append to file
      const logEntries = messages.map(msg => {
        return JSON.stringify({
          ...msg,
          receivedAt: Date.now(),
          receivedAtIso: new Date().toISOString(),
          sessionId: ws.id || 'unknown'
        });
      }).join('\n');

      await fs.appendFile(consoleLogFile, logEntries + '\n');

      // Also log to the regular store-events file for unified view
      const eventLogFile = path.join(debugDir, `store-events-${dateStr}T${timeStr}.jsonl`);
      const eventEntry = JSON.stringify({
        timestamp: Date.now(),
        isoTimestamp: new Date().toISOString(),
        type: 'console',
        operation: 'CONSOLE_LOGS',
        entity: 'frontend',
        count: messages.length,
        messages: messages,
        metadata: {
          source: fullMessage.source || 'frontend',
          sessionId: ws.id || 'unknown'
        }
      });

      await fs.appendFile(eventLogFile, eventEntry + '\n');

      // Send acknowledgment
      this.send(ws, {
        type: 'CONSOLE_LOGS_ACK',
        received: messages.length,
        timestamp: Date.now()
      });

      console.log(`Stored ${messages.length} console log messages`);
    } catch (error) {
      console.error('Failed to store console logs:', error);
      this.sendError(ws, `Failed to store console logs: ${error.message}`);
    }
  }

  /**
   * Broadcast store update to all subscribers
   */
  broadcastStoreUpdate(entity) {
    const state = this.storeManager.getStoreState(entity);
    if (!state) return;

    const message = {
      type: 'STORE_UPDATE',
      entity: entity,
      data: state,
      timestamp: Date.now()
    };

    let subscriberCount = 0;
    this.clients.forEach(client => {
      if (client.subscriptions && client.subscriptions.has(entity)) {
        this.send(client, message);
        subscriberCount++;
      }
    });

    console.log(`Broadcasted ${entity} update to ${subscriberCount} subscribers`);
  }

  /**
   * Send message to a client
   */
  send(ws, message) {
    if (ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify(message));
    }
  }

  /**
   * Send error message
   */
  sendError(ws, error) {
    this.send(ws, {
      type: 'ERROR',
      error: error,
      timestamp: Date.now()
    });
  }

  /**
   * Broadcast to all clients
   */
  broadcast(message) {
    this.clients.forEach(client => {
      this.send(client, message);
    });
  }

  /**
   * Stop the server
   */
  stop() {
    if (this.wss) {
      this.wss.close();
      console.log('EventBus stopped');
    }
  }
}

module.exports = EventBusWithMutations;