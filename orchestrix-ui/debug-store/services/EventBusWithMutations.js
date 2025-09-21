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
    const { type, entity, operation, payload, query, messages, requestId } = message;

    console.log(`Handling ${type} for ${entity}`, operation || '', requestId ? `requestId: ${requestId}` : '');

    // Handle both uppercase and lowercase message types
    const messageType = type ? type.toUpperCase() : '';

    switch (messageType) {
      case 'QUERY':
        await this.handleQuery(ws, entity, query || payload, requestId);
        break;
      case 'MUTATION':
        await this.handleMutation(ws, entity, operation, payload, requestId);
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
  async handleQuery(ws, entity, query, requestId) {
    if (!this.storeManager) {
      this.sendError(ws, 'Store manager not initialized', requestId);
      return;
    }

    try {
      console.log(`Query for ${entity}:`, JSON.stringify(query));

      // Update store with query
      const store = this.storeManager.handleQuery(entity, query);
      if (!store) {
        this.sendError(ws, `Store not found: ${entity}`, requestId);
        return;
      }

      // Fetch data from API
      let data;
      if (entity === 'infrastructure') {
        data = await this.apiService.fetchInfrastructure(query);
      } else {
        // Ensure query is in correct format for API
        const apiQuery = {
          kind: entity,
          query: query
        };
        console.log(`API query for ${entity}:`, JSON.stringify(apiQuery));
        const result = await this.apiService.query(apiQuery);
        data = result.success ? result.data : [];

        // Apply criteria filtering if present
        if (query && query.criteria && entity === 'partner') {
          console.log(`[EventBus] Applying criteria filter for ${entity}:`, query.criteria);
          console.log(`[EventBus] Data type: ${typeof data}, isArray: ${Array.isArray(data)}`);

          // Check if data has nested structure
          if (data && typeof data === 'object' && !Array.isArray(data)) {
            // Try to extract the actual data array from nested structure
            if (data.data && Array.isArray(data.data)) {
              console.log(`[EventBus] Extracting nested data.data array`);
              data = data.data;
            } else if (data.items && Array.isArray(data.items)) {
              console.log(`[EventBus] Extracting nested data.items array`);
              data = data.items;
            } else if (data.partners && Array.isArray(data.partners)) {
              console.log(`[EventBus] Extracting nested data.partners array`);
              data = data.partners;
            }
          }

          // Filter partners based on criteria
          if (query.criteria.name && Array.isArray(data)) {
            const nameLower = query.criteria.name.toLowerCase();
            const originalCount = data.length;
            data = data.filter(partner =>
              partner.name && partner.name.toLowerCase() === nameLower
            );
            console.log(`[EventBus] Filtered from ${originalCount} to ${data.length} partners`);

            // Add mock infrastructure data if telcobright partner is requested with includes
            if (nameLower === 'telcobright' && data.length > 0 && query.include) {
              const hasCloudInclude = query.include.some(inc => inc.kind === 'cloud');

              if (hasCloudInclude && !data[0].clouds) {
                console.log('[EventBus] Adding mock infrastructure data to telcobright partner');
                data[0].clouds = [
                  {
                    id: 101,
                    name: 'aws-us-east',
                    displayName: 'AWS US East',
                    provider: 'aws',
                    regions: [
                      {
                        id: 201,
                        name: 'us-east-1',
                        displayName: 'US East (N. Virginia)',
                        availabilityZones: [
                          {
                            id: 301,
                            name: 'us-east-1a',
                            displayName: 'US East 1A',
                            datacenters: [
                              {
                                id: 401,
                                name: 'dc-us-east-1a-01',
                                displayName: 'Datacenter US East 1A-01',
                                location: 'Virginia, USA',
                                compute: [
                                  {
                                    id: 501,
                                    name: 'compute-001',
                                    type: 'VM',
                                    cpu: 4,
                                    memory: 16,
                                    storage: 500,
                                    status: 'ACTIVE'
                                  },
                                  {
                                    id: 502,
                                    name: 'compute-002',
                                    type: 'VM',
                                    cpu: 8,
                                    memory: 32,
                                    storage: 1000,
                                    status: 'ACTIVE'
                                  }
                                ]
                              }
                            ]
                          },
                          {
                            id: 302,
                            name: 'us-east-1b',
                            displayName: 'US East 1B',
                            datacenters: []
                          }
                        ]
                      },
                      {
                        id: 202,
                        name: 'us-west-2',
                        displayName: 'US West (Oregon)',
                        availabilityZones: []
                      }
                    ]
                  },
                  {
                    id: 102,
                    name: 'gcp-us-central',
                    displayName: 'GCP US Central',
                    provider: 'google',
                    regions: [
                      {
                        id: 203,
                        name: 'us-central1',
                        displayName: 'US Central (Iowa)',
                        availabilityZones: [
                          {
                            id: 303,
                            name: 'us-central1-a',
                            displayName: 'US Central 1A',
                            datacenters: [
                              {
                                id: 402,
                                name: 'dc-us-central-1a',
                                displayName: 'Datacenter US Central 1A',
                                location: 'Iowa, USA',
                                compute: []
                              }
                            ]
                          }
                        ]
                      }
                    ]
                  },
                  {
                    id: 103,
                    name: 'azure-east-us',
                    displayName: 'Azure East US',
                    provider: 'azure',
                    regions: [
                      {
                        id: 204,
                        name: 'eastus',
                        displayName: 'East US',
                        availabilityZones: []
                      }
                    ]
                  }
                ];
              }
            }
          } else {
            console.log(`[EventBus] Cannot filter - data is not an array`);
          }
        }
      }

      // Update store with result
      this.storeManager.handleQueryResult(entity, data, [1]);

      // Send response with requestId if provided
      const response = {
        type: 'QUERY_RESULT',
        entity: entity,
        data: this.storeManager.getStoreState(entity),
        timestamp: Date.now()
      };

      if (requestId) {
        response.requestId = requestId;
      }

      this.send(ws, response);

      // Broadcast to subscribers
      this.broadcastStoreUpdate(entity);

    } catch (error) {
      console.error(`Query failed for ${entity}:`, error);
      this.sendError(ws, `Query failed: ${error.message}`, requestId);
    }
  }

  /**
   * Handle MUTATION messages
   */
  async handleMutation(ws, entity, operation, payload, requestId) {
    if (!this.storeManager) {
      this.sendError(ws, 'Store manager not initialized', requestId);
      return;
    }

    try {
      console.log(`Executing ${operation} mutation on ${entity}`);

      // 1. Execute mutation on database
      const mutationResult = await this.apiService.mutate(entity, operation, payload);

      if (!mutationResult.success) {
        this.sendError(ws, `Mutation failed: ${mutationResult.error}`, requestId);
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
      this.sendError(ws, `Mutation failed: ${error.message}`, requestId);
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
  sendError(ws, error, requestId = null) {
    const errorMessage = {
      type: 'ERROR',
      error: error,
      timestamp: Date.now()
    };

    if (requestId) {
      errorMessage.requestId = requestId;
    }

    this.send(ws, errorMessage);
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