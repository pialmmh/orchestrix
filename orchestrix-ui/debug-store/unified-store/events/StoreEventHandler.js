// Auto-generated from TypeScript source
// Original: events/StoreEventHandler.ts
// Generated: 2025-09-19T10:56:20.506Z

// StoreEventHandler - Base implementation for handling store events
const { StoreEvent, StoreEventResponse, createEventResponse } = require('./StoreEvent');
const { getEventBus, IEventBus } = require('./EventBus');
const { getStoreDebugConfig } = require('../../config/storeDebugConfig');

// Frontend Event Handler Interface


// Store Event Handler Implementation
export class StoreEventHandler implements FrontEndEventHandler {
  private eventBus: IEventBus;
  private mode: 'normal' | 'debug';
  private pendingEvents, StoreEvent>;
  private responseTimeouts, NodeJS.Timeout>;
  private config = getStoreDebugConfig();

  constructor() {
    this.eventBus = getEventBus();
    this.mode = this.config.store_debug ? 'debug' : 'normal';
    this.pendingEvents = new Map();
    this.responseTimeouts = new Map();
  }

  async handle(event): Promise {
    // Store pending event
    this.pendingEvents.set(event.id, event);

    // Publish event to bus
    this.eventBus.publish(event);

    // Set timeout for response
    const timeout = setTimeout(() => {
      // If no response after 10 seconds, publish error
      if (this.pendingEvents.has(event.id)) {
        const errorResponse = createEventResponse(
          event,
          'error',
          undefined,
          'Request timeout - no response received'
        );
        this.eventBus.publish(errorResponse);
        this.cleanup(event.id);
      }
    }, 10000); // 10 second timeout

    this.responseTimeouts.set(event.id, timeout);

    if (this.config.store_debug) {
      console.log(`[StoreEventHandler] Handling ${event.type} event:`, event);
    }
  }

  subscribe(eventId, handler: (response) => void): void {
    // Subscribe to specific event ID or wildcard
    this.eventBus.subscribe(eventId, (event) => {
      // Only call handler for response events
      if ('status' in event) {
        handler(event as StoreEventResponse);
        // Cleanup after receiving response
        if (eventId !== '*') {
          this.cleanup(eventId);
        }
      }
    });
  }

  unsubscribe(eventId, handler?: (response) => void): void {
    this.eventBus.unsubscribe(eventId, handler as any);
  }

  setMode(mode: 'normal' | 'debug'): void {
    this.mode = mode;
    // Mode change might require recreating the event bus
    // This would be handled by resetting the event bus
  }

  private cleanup(eventId): void {
    // Remove from pending events
    this.pendingEvents.delete(eventId);
    
    // Clear timeout
    const timeout = this.responseTimeouts.get(eventId);
    if (timeout) {
      clearTimeout(timeout);
      this.responseTimeouts.delete(eventId);
    }
  }

  // Helper method to subscribe to response for a specific event
  async handleWithResponse(event): Promise {
    return new Promise((resolve, reject) => {
      // Subscribe to response
      const handler = (response) => {
        if (response.id === event.id) {
          this.unsubscribe(event.id, handler);
          if (response.status === 'error') {
            reject(new Error(response.error || 'Unknown error'));
          } else {
            resolve(response);
          }
        }
      };

      this.subscribe(event.id, handler);
      
      // Handle the event
      this.handle(event).catch(reject);
    });
  }
}

// Singleton instance
let handlerInstance: StoreEventHandler | null = null;

export function getStoreEventHandler(): StoreEventHandler {
  if (!handlerInstance) {
    handlerInstance = new StoreEventHandler();
  }
  return handlerInstance;
}

// Reset handler (useful for testing or config changes)
export function resetStoreEventHandler(): void {
  handlerInstance = null;
}