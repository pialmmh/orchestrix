import { getEventBus } from './EventBus';
import { StoreEvent, StoreEventResponse } from './StoreEvent';
import storeWebSocketService from '../../services/StoreWebSocketService';
import { getStoreDebugConfig } from '../../config/storeDebugConfig';

/**
 * EventBusWebSocketHandler forwards EventBus queries to the WebSocket debug server
 * Only active when IN debug mode with WebSocket.
 */
export class EventBusWebSocketHandler {
  private eventBus = getEventBus();
  private isListening = false;

  constructor() {
    const config = getStoreDebugConfig();
    // Only start listening if we're IN debug mode
    if (config.store_debug) {
      console.log('[EventBusWebSocketHandler] Starting in debug mode - forwarding queries to WebSocket');
      this.startListening();
    }
  }

  /**
   * Start listening for query requests on the event bus
   */
  private startListening() {
    if (this.isListening) return;

    this.isListening = true;

    // Listen for all events
    this.eventBus.subscribe('*', async (event: StoreEvent | StoreEventResponse) => {
      // Check if event is valid
      if (!event || typeof event !== 'object') {
        console.warn('[EventBusWebSocketHandler] Invalid event received:', event);
        return;
      }

      // Only process REQUEST events
      if (event.type === 'query' && event.operation === 'REQUEST') {
        await this.handleQueryRequest(event);
      } else if (event.type === 'mutation' && event.operation === 'REQUEST') {
        await this.handleMutationRequest(event);
      }
    });
  }

  /**
   * Handle a query request from the event bus by forwarding to WebSocket
   */
  private async handleQueryRequest(event: StoreEvent) {
    const query = event.payload;
    const entity = query.kind || 'partner'; // Default to partner for infrastructure queries

    try {
      console.log(`[EventBusWebSocketHandler] Forwarding query for ${entity} to WebSocket with eventId: ${event.id}`);

      // Forward query to WebSocket service with eventId preserved as requestId
      const response = await storeWebSocketService.queryWithId(entity, query, event.id);

      console.log(`[EventBusWebSocketHandler] Got response from WebSocket for ${entity}:`,
        Array.isArray(response) ? `Array with ${response.length} items` : typeof response);

      // Publish success response
      this.eventBus.publish({
        id: event.id,
        timestamp: Date.now(),
        type: 'query',
        operation: 'RESPONSE',
        entity: entity,
        payload: response,
        metadata: {
          duration: Date.now() - event.timestamp
        },
        success: true
      });
    } catch (error: any) {
      console.error('[EventBusWebSocketHandler] Query failed:', error);

      // Publish error response
      this.eventBus.publish({
        id: event.id,
        timestamp: Date.now(),
        type: 'query',
        operation: 'RESPONSE',
        entity: entity,
        payload: null,
        metadata: {
          duration: Date.now() - event.timestamp
        },
        success: false,
        error: error.message || 'Query failed'
      });
    }
  }

  /**
   * Handle a mutation request from the event bus
   */
  private async handleMutationRequest(event: StoreEvent) {
    const mutation = event.payload;
    const entity = mutation.entityName || mutation.kind;

    try {
      console.log(`[EventBusWebSocketHandler] Forwarding mutation for ${entity} to WebSocket`);

      // Forward mutation to WebSocket service
      const response = await storeWebSocketService.mutate(
        entity,
        mutation.operation,
        mutation.data || mutation
      );

      // Publish success response
      this.eventBus.publish({
        id: event.id,
        timestamp: Date.now(),
        type: 'mutation',
        operation: 'RESPONSE',
        entity: entity,
        payload: response,
        metadata: {
          duration: Date.now() - event.timestamp
        },
        success: true
      });
    } catch (error: any) {
      console.error('[EventBusWebSocketHandler] Mutation failed:', error);

      // Publish error response
      this.eventBus.publish({
        id: event.id,
        timestamp: Date.now(),
        type: 'mutation',
        operation: 'RESPONSE',
        entity: entity,
        payload: null,
        metadata: {
          duration: Date.now() - event.timestamp
        },
        success: false,
        error: error.message || 'Mutation failed'
      });
    }
  }

  /**
   * Stop listening for events
   */
  stopListening() {
    this.eventBus.unsubscribe('*');
    this.isListening = false;
  }
}

// Create singleton instance
let handlerInstance: EventBusWebSocketHandler | null = null;

export function getEventBusWebSocketHandler(): EventBusWebSocketHandler {
  if (!handlerInstance) {
    handlerInstance = new EventBusWebSocketHandler();
  }
  return handlerInstance;
}

// Auto-initialize when module is imported
getEventBusWebSocketHandler();