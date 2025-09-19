// Auto-generated from TypeScript source
// Original: events/EventBus.ts
// Generated: 2025-09-19T03:59:30.867Z

// EventBus Abstraction Layer
import mitt, { Emitter } from 'mitt';
const { StoreEvent, StoreEventResponse } = require('./StoreEvent');
const { getStoreDebugConfig } = require('../../config/storeDebugConfig');

// Event types for the bus
;

// EventBus interface


// Local EventBus using mitt
export class LocalEventBus implements IEventBus {
  private emitter: Emitter;

  constructor() {
    this.emitter = mitt();
  }

  publish(event): void {
    // Publish to specific event ID channel
    this.emitter.emit(event.id, event);
    // Also publish to wildcard channel for global listeners
    this.emitter.emit('*', event);
    
    if (getStoreDebugConfig().store_debug) {
      console.log('[LocalEventBus] Published:', event);
    }
  }

  subscribe(eventId: string | '*', handler: (event) => void): void {
    this.emitter.on(eventId, handler);
  }

  unsubscribe(eventId: string | '*', handler?: (event) => void): void {
    if (handler) {
      this.emitter.off(eventId, handler);
    } else {
      // Remove all handlers for this event
      const emitterWithAll = this.emitter as any;
      if (emitterWithAll.all) {
        emitterWithAll.all.delete(eventId);
      }
    }
  }

  clear(): void {
    this.emitter.all?.clear();
  }

  async request<T = any>(eventId, payload): Promise {
    const config = getStoreDebugConfig();

    return new Promise((resolve, reject) => {
      let responded = false;

      // Subscribe to response with the same eventId
      const responseHandler = (event) => {
        // Only handle RESPONSE events, not REQUEST events
        if (event.id === eventId && event.operation === 'RESPONSE' && !responded) {
          responded = true;
          this.unsubscribe(eventId, responseHandler);

          const response = event as StoreEventResponse;
          if (response.success) {
            resolve(response.payload as T);
          } else {
            reject(new Error(response.error || 'Request failed'));
          }
        }
      };

      this.subscribe(eventId, responseHandler);

      // Publish the request
      this.publish({
        id,
        timestamp: Date.now(),
        type: 'query',
        operation: 'REQUEST',
        entity: payload.kind || 'unknown',
        payload,
        metadata: {}
      });

      // Timeout after configured duration
      setTimeout(() => {
        if (!responded) {
          responded = true;
          this.unsubscribe(eventId, responseHandler);
          reject(new Error('Request timeout'));
        }
      }, config.request_timeout_ms);
    });
  }
}

// WebSocket EventBus for debug mode
export class WebSocketEventBus implements IEventBus {
  private ws: WebSocket | null = null;
  private localBus: LocalEventBus;
  private reconnectTimeout: NodeJS.Timeout | null = null;
  private messageQueue: (StoreEvent | StoreEventResponse)[] = [];
  private isConnected: boolean = false;
  private config = getStoreDebugConfig();

  constructor() {
    this.localBus = new LocalEventBus();
    this.connect();
  }

  private connect(): void {
    try {
      this.ws = new WebSocket(this.config.websocket_url);

      this.ws.onopen = () => {
        console.log('[WebSocketEventBus] Connected to store debug server');
        this.isConnected = true;
        // Send queued messages
        while (this.messageQueue.length > 0) {
          const event = this.messageQueue.shift();
          if (event) {
            this.sendToWebSocket(event);
          }
        }
      };

      this.ws.onmessage = (message) => {
        try {
          const event = JSON.parse(message.data) as StoreEvent | StoreEventResponse;
          // Publish received events to local bus
          this.localBus.publish(event);
        } catch (error) {
          console.error('[WebSocketEventBus] Failed to parse message:', error);
        }
      };

      this.ws.onerror = (error) => {
        console.error('[WebSocketEventBus] WebSocket error:', error);
      };

      this.ws.onclose = () => {
        console.log('[WebSocketEventBus] Disconnected from store debug server');
        this.isConnected = false;
        // Attempt to reconnect after 5 seconds
        this.reconnectTimeout = setTimeout(() => {
          console.log('[WebSocketEventBus] Attempting to reconnect...');
          this.connect();
        }, 5000);
      };
    } catch (error) {
      console.error('[WebSocketEventBus] Failed to connect:', error);
      // Fall back to local bus
      this.isConnected = false;
    }
  }

  private sendToWebSocket(event): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(event));
    } else {
      // Queue message for later
      this.messageQueue.push(event);
    }
  }

  publish(event): void {
    // Always publish to local bus
    this.localBus.publish(event);
    
    // Also send to WebSocket if connected
    if (this.isConnected) {
      this.sendToWebSocket(event);
    } else {
      // Queue for later
      this.messageQueue.push(event);
    }
  }

  subscribe(eventId: string | '*', handler: (event) => void): void {
    this.localBus.subscribe(eventId, handler);
  }

  unsubscribe(eventId: string | '*', handler?: (event) => void): void {
    this.localBus.unsubscribe(eventId, handler);
  }

  clear(): void {
    this.localBus.clear();
  }

  async request<T = any>(eventId, payload): Promise {
    const config = getStoreDebugConfig();

    return new Promise((resolve, reject) => {
      let responded = false;

      // Subscribe to response with the same eventId
      const responseHandler = (event) => {
        // Only handle RESPONSE events, not REQUEST events
        if (event.id === eventId && event.operation === 'RESPONSE' && !responded) {
          responded = true;
          this.unsubscribe(eventId, responseHandler);

          const response = event as StoreEventResponse;
          if (response.success) {
            resolve(response.payload as T);
          } else {
            reject(new Error(response.error || 'Request failed'));
          }
        }
      };

      this.subscribe(eventId, responseHandler);

      // Publish the request through WebSocket
      this.publish({
        id,
        timestamp: Date.now(),
        type: 'query',
        operation: 'REQUEST',
        entity: payload.kind || 'unknown',
        payload,
        metadata: {}
      });

      // Timeout after configured duration
      setTimeout(() => {
        if (!responded) {
          responded = true;
          this.unsubscribe(eventId, responseHandler);
          reject(new Error('Request timeout'));
        }
      }, config.request_timeout_ms);
    });
  }

  disconnect(): void {
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
    }
    if (this.ws) {
      this.ws.close();
    }
  }
}

// Factory to create the appropriate EventBus
export function createEventBus(): IEventBus {
  const config = getStoreDebugConfig();

  // EventBus type is determined by store_debug flag only
  // When debug is on, use WebSocket; when off, use Local
  if (config.store_debug) {
    console.log('[EventBus] Creating WebSocketEventBus for debug mode');
    return new WebSocketEventBus();
  } else {
    console.log('[EventBus] Creating LocalEventBus');
    return new LocalEventBus();
  }
}

// Singleton instance
let eventBusInstance: IEventBus | null = null;

export function getEventBus(): IEventBus {
  if (!eventBusInstance) {
    eventBusInstance = createEventBus();
  }
  return eventBusInstance;
}

// Reset the event bus (useful for testing or config changes)
export function resetEventBus(): void {
  if (eventBusInstance) {
    if (eventBusInstance instanceof WebSocketEventBus) {
      eventBusInstance.disconnect();
    }
    eventBusInstance.clear();
    eventBusInstance = null;
  }
}