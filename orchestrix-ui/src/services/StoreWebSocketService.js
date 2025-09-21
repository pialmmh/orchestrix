/**
 * WebSocket service for communicating with the store server
 * Handles both queries and mutations through EventBus pattern
 */

import { appConfig } from '../config/app.config';

class StoreWebSocketService {
  constructor() {
    this.ws = null;
    this.isConnected = false;
    this.reconnectTimeout = null;
    this.messageQueue = [];
    this.responseHandlers = new Map();
    this.subscriptions = new Map();
    this.wsUrl = appConfig.websocket.storeDebugUrl;
    this.reconnectInterval = appConfig.websocket.reconnectInterval;
    this.maxQueueSize = appConfig.websocket.messageQueueSize;
    this.maxReconnectAttempts = 5;
    this.reconnectAttempts = 0;
    this.debugMode = appConfig.storeDebug.enabled;
  }

  connect() {
    if (this.ws) {
      return;
    }

    try {
      console.log(`[StoreWebSocket] Connecting to ${this.wsUrl}...`);
      this.ws = new WebSocket(this.wsUrl);

      this.ws.onopen = () => {
        console.log('[StoreWebSocket] Connected to store server');
        this.isConnected = true;
        this.reconnectAttempts = 0; // Reset counter on successful connection

        // Process queued messages
        while (this.messageQueue.length > 0) {
          const message = this.messageQueue.shift();
          this.send(message);
        }
      };

      this.ws.onmessage = (event) => {
        try {
          const message = JSON.parse(event.data);
          this.handleMessage(message);
        } catch (error) {
          console.error('[StoreWebSocket] Failed to parse message:', error);
        }
      };

      this.ws.onerror = (error) => {
        console.error('[StoreWebSocket] WebSocket error:', error);
      };

      this.ws.onclose = () => {
        console.log('[StoreWebSocket] Disconnected from store server');
        this.isConnected = false;
        this.ws = null;

        // Attempt reconnection using configured interval
        this.scheduleReconnect();
      };
    } catch (error) {
      console.error('[StoreWebSocket] Failed to connect:', error);
      this.scheduleReconnect();
    }
  }

  scheduleReconnect() {
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
    }

    // In debug mode, throw error after max attempts
    if (this.debugMode && this.reconnectAttempts >= this.maxReconnectAttempts) {
      const error = new Error(`Failed to connect to WebSocket debug server at ${this.wsUrl} after ${this.maxReconnectAttempts} attempts. Debug mode requires WebSocket connection.`);
      console.error('[StoreWebSocket] Fatal:', error.message);
      // Show error to user
      alert(error.message);
      throw error;
    }

    this.reconnectAttempts++;
    this.reconnectTimeout = setTimeout(() => {
      console.log(`[StoreWebSocket] Attempting to reconnect... (attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
      this.connect();
    }, this.reconnectInterval);
  }

  handleMessage(message) {
    const { type, entity } = message;

    switch (type) {
      case 'CONNECTION':
        console.log('[StoreWebSocket] Connection confirmed');
        break;

      case 'QUERY_RESULT':
        // Handle query response
        if (message.requestId) {
          const handler = this.responseHandlers.get(message.requestId);
          if (handler) {
            // Extract the actual data from the store state structure
            // The backend sends { entity, query, data, meta } where data contains the actual results
            console.log(`[StoreWebSocket] QUERY_RESULT received for ${message.entity} with requestId ${message.requestId}`);
            console.log('[StoreWebSocket] Full message.data structure:', JSON.stringify(message.data, null, 2).substring(0, 500));

            // The backend sends the store state which has: { entity, query, data: { data: [...], count, success }, meta }
            // We need to extract the nested data.data array
            let responseData = message.data;
            if (responseData && responseData.data && responseData.data.data) {
              // Extract the actual data array from the nested structure
              responseData = responseData.data.data;
              console.log(`[StoreWebSocket] Extracted nested data array with ${responseData.length} items`);
            } else if (responseData && responseData.data) {
              responseData = responseData.data;
              console.log('[StoreWebSocket] Using data field from response');
            }

            handler.resolve(responseData);
            this.responseHandlers.delete(message.requestId);
          }
        }
        break;

      case 'MUTATION_SUCCESS':
        // Handle mutation success
        console.log(`[StoreWebSocket] Mutation ${message.operation} succeeded for ${entity}`);
        if (message.requestId) {
          const handler = this.responseHandlers.get(message.requestId);
          if (handler) {
            handler.resolve(message.data);
            this.responseHandlers.delete(message.requestId);
          }
        }
        break;

      case 'STORE_UPDATE':
      case 'SUBSCRIPTION_DATA':
        // Handle store updates for subscribers
        const subscribers = this.subscriptions.get(entity) || [];
        subscribers.forEach(callback => {
          try {
            callback(message.data);
          } catch (error) {
            console.error(`[StoreWebSocket] Subscriber error for ${entity}:`, error);
          }
        });
        break;

      case 'ERROR':
        console.error(`[StoreWebSocket] Error: ${message.error}`);
        if (message.requestId) {
          const handler = this.responseHandlers.get(message.requestId);
          if (handler) {
            handler.reject(new Error(message.error));
            this.responseHandlers.delete(message.requestId);
          }
        }
        break;

      default:
        console.log(`[StoreWebSocket] Unknown message type: ${type}`, message);
    }
  }

  send(message) {
    if (this.isConnected && this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
    } else {
      // Queue message for later (respect max queue size)
      if (this.messageQueue.length < this.maxQueueSize) {
        this.messageQueue.push(message);
      } else {
        console.warn('[StoreWebSocket] Message queue full, dropping message');
      }
      // Try to connect if not connected
      if (!this.ws) {
        this.connect();
      }
    }
  }

  /**
   * Execute a query and return the result
   */
  async query(entity, queryPayload) {
    return new Promise((resolve, reject) => {
      const requestId = this.generateRequestId();

      // Store the response handler
      this.responseHandlers.set(requestId, { resolve, reject });

      // Send the query
      this.send({
        type: 'QUERY',
        entity,
        query: queryPayload,
        payload: queryPayload, // Some backend versions expect payload
        requestId
      });

      // Set timeout
      setTimeout(() => {
        if (this.responseHandlers.has(requestId)) {
          this.responseHandlers.delete(requestId);
          reject(new Error('Query timeout'));
        }
      }, 30000); // 30 second timeout
    });
  }

  /**
   * Execute a query with a specific requestId
   */
  async queryWithId(entity, queryPayload, requestId) {
    return new Promise((resolve, reject) => {
      // Store the response handler with the specific requestId
      this.responseHandlers.set(requestId, { resolve, reject });

      // Send the query with the specific requestId
      this.send({
        type: 'QUERY',
        entity,
        query: queryPayload,
        payload: queryPayload, // Some backend versions expect payload
        requestId
      });

      // Set timeout
      setTimeout(() => {
        if (this.responseHandlers.has(requestId)) {
          this.responseHandlers.delete(requestId);
          reject(new Error('Query timeout'));
        }
      }, 30000); // 30 second timeout
    });
  }

  /**
   * Execute a mutation (CREATE, UPDATE, DELETE)
   */
  async mutate(entity, operation, data) {
    return new Promise((resolve, reject) => {
      const requestId = this.generateRequestId();

      // Store the response handler
      this.responseHandlers.set(requestId, { resolve, reject });

      // Send the mutation
      this.send({
        type: 'MUTATION',
        entity,
        operation,
        payload: data,
        requestId
      });

      // Set timeout
      setTimeout(() => {
        if (this.responseHandlers.has(requestId)) {
          this.responseHandlers.delete(requestId);
          reject(new Error('Mutation timeout'));
        }
      }, 30000); // 30 second timeout
    });
  }

  /**
   * Subscribe to store updates for a specific entity
   */
  subscribe(entity, callback) {
    if (!this.subscriptions.has(entity)) {
      this.subscriptions.set(entity, []);
    }

    const subscribers = this.subscriptions.get(entity);
    subscribers.push(callback);

    // Send subscription message
    this.send({
      type: 'SUBSCRIBE',
      entity
    });

    // Return unsubscribe function
    return () => {
      const index = subscribers.indexOf(callback);
      if (index > -1) {
        subscribers.splice(index, 1);
      }

      // If no more subscribers for this entity, unsubscribe
      if (subscribers.length === 0) {
        this.send({
          type: 'UNSUBSCRIBE',
          entity
        });
        this.subscriptions.delete(entity);
      }
    };
  }

  /**
   * Send console logs to backend for storage
   */
  sendConsoleLogs(messages) {
    // Send console logs batch
    this.send({
      type: 'CONSOLE_LOGS',
      entity: 'console',
      messages: messages,
      timestamp: Date.now(),
      source: 'frontend'
    });
  }

  /**
   * Generate unique request ID
   */
  generateRequestId() {
    return `req_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  /**
   * Disconnect from the server
   */
  disconnect() {
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }

    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }

    this.isConnected = false;
  }
}

// Create singleton instance
const storeWebSocketService = new StoreWebSocketService();

// Auto-connect on initialization
storeWebSocketService.connect();

export default storeWebSocketService;