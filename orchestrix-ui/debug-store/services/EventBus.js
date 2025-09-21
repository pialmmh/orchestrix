const WebSocket = require('ws');

class EventBus {
  constructor(port = 8081) {
    this.port = port;
    this.wss = null;
    this.clients = new Set();
    this.handlers = new Map();
    this.storeManager = null;
  }

  setStoreManager(storeManager) {
    this.storeManager = storeManager;
  }

  start() {
    this.wss = new WebSocket.Server({
      port: this.port,
      maxPayload: 100 * 1024 * 1024 // 100MB max payload size
    });

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

      // Send initial connection confirmation
      this.send(ws, {
        type: 'CONNECTION',
        status: 'connected',
        timestamp: Date.now()
      });
    });

    console.log(`EventBus WebSocket server started on port ${this.port}`);
  }

  handleMessage(ws, message) {
    const { type, entity, operation, payload } = message;

    switch (type) {
      case 'QUERY':
        this.handleQuery(ws, entity, payload);
        break;
      case 'MUTATION':
        this.handleMutation(ws, entity, operation, payload);
        break;
      case 'SUBSCRIBE':
        this.handleSubscribe(ws, entity);
        break;
      case 'UNSUBSCRIBE':
        this.handleUnsubscribe(ws, entity);
        break;
      default:
        this.sendError(ws, `Unknown message type: ${type}`);
    }
  }

  async handleQuery(ws, entity, query) {
    if (!this.storeManager) {
      this.sendError(ws, 'Store manager not initialized');
      return;
    }

    // Update store with query
    const store = this.storeManager.handleQuery(entity, query);
    if (!store) {
      this.sendError(ws, `Store not found: ${entity}`);
      return;
    }

    // Simulate fetching data (in real app, this would query database)
    // For now, return empty data
    const data = await this.fetchData(entity, query);

    // Update store with result
    this.storeManager.handleQueryResult(entity, data, [1]);

    // Send response
    this.send(ws, {
      type: 'QUERY_RESULT',
      entity: entity,
      data: this.storeManager.getStoreState(entity),
      timestamp: Date.now()
    });

    // Broadcast to all subscribers of this entity
    this.broadcast({
      type: 'STORE_UPDATE',
      entity: entity,
      data: this.storeManager.getStoreState(entity),
      timestamp: Date.now()
    }, entity);
  }

  async handleMutation(ws, entity, operation, mutation) {
    if (!this.storeManager) {
      this.sendError(ws, 'Store manager not initialized');
      return;
    }

    // Apply mutation
    const store = this.storeManager.handleMutation(entity, {
      operation,
      ...mutation
    });

    if (!store) {
      this.sendError(ws, `Store not found: ${entity}`);
      return;
    }

    // Send response
    this.send(ws, {
      type: 'MUTATION_RESULT',
      entity: entity,
      operation: operation,
      success: true,
      data: this.storeManager.getStoreState(entity),
      timestamp: Date.now()
    });

    // Broadcast to all subscribers
    this.broadcast({
      type: 'STORE_UPDATE',
      entity: entity,
      data: this.storeManager.getStoreState(entity),
      timestamp: Date.now()
    }, entity);
  }

  handleSubscribe(ws, entity) {
    if (!ws.subscriptions) {
      ws.subscriptions = new Set();
    }
    ws.subscriptions.add(entity);

    // Send current state
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
  }

  handleUnsubscribe(ws, entity) {
    if (ws.subscriptions) {
      ws.subscriptions.delete(entity);
    }
  }

  send(ws, message) {
    if (ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify(message));
    }
  }

  sendError(ws, error) {
    this.send(ws, {
      type: 'ERROR',
      error: error,
      timestamp: Date.now()
    });
  }

  broadcast(message, entity = null) {
    this.clients.forEach(client => {
      if (entity && client.subscriptions && !client.subscriptions.has(entity)) {
        return; // Skip if not subscribed to this entity
      }
      this.send(client, message);
    });
  }

  async fetchData(entity, query) {
    // Mock data for testing
    // In real app, this would query the database
    switch (entity) {
      case 'country':
        return [
          { id: 1, name: 'Bangladesh', code: 'BD', capital: 'Dhaka' },
          { id: 2, name: 'India', code: 'IN', capital: 'New Delhi' }
        ];
      case 'infrastructure':
        return {
          partners: [{
            id: 24,
            name: 'telcobright',
            displayName: 'Telcobright',
            environments: [],
            clouds: []
          }]
        };
      default:
        return [];
    }
  }

  stop() {
    if (this.wss) {
      this.wss.close();
      console.log('EventBus stopped');
    }
  }
}

module.exports = EventBus;