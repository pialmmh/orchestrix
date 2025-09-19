// Auto-generated from TypeScript source
// Original: events/LocalStore.ts
// Generated: 2025-09-19T03:59:30.867Z

const { getEventBus } = require('./EventBus');
const { StoreEvent, StoreEventResponse } = require('./StoreEvent');
const StellarClient = require('../../services/StellarClient');
const { QueryNode, QueryResponse } = require('../../models/stellar/QueryNode');
const { EntityModificationRequest, MutationResponse } = require('../../models/stellar/MutationRequest');
const { getStoreDebugConfig } = require('../../config/storeDebugConfig');

/**
 * LocalStore handles query execution locally in the browser.
 * It listens for query events from the EventBus and processes them using Stellar.
 * Only active when NOT in debug mode with WebSocket.
 */
export class LocalStore {
  private eventBus = getEventBus();
  private isListening = false;

  constructor() {
    const config = getStoreDebugConfig();
    // Only start listening if we're NOT in debug mode
    // In debug mode, the WebSocket server handles all query processing
    if (!config.store_debug) {
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
    this.eventBus.subscribe('*', async (event) => {
      // Only process REQUEST events
      if (event.type === 'query' && event.operation === 'REQUEST') {
        await this.handleQueryRequest(event);
      } else if (event.type === 'mutation' && event.operation === 'REQUEST') {
        await this.handleMutationRequest(event);
      }
    });
  }

  /**
   * Handle a query request from the event bus
   */
  private async handleQueryRequest(event) {
    const query = event.payload as QueryNode;

    try {
      // Execute the query using Stellar
      const response = await StellarClient.post('stellar/query', query);

      // Publish success response - extract the actual data from response
      const responseData = response.data || response;
      this.eventBus.publish({
        id: event.id,
        timestamp: Date.now(),
        type: 'query',
        operation: 'RESPONSE',
        entity: query.kind || 'unknown',
        payload, // Pass the full response data
        metadata: {
          duration: Date.now() - event.timestamp
        },
        success: true
      });
    } catch (error) {
      // Publish error response
      this.eventBus.publish({
        id: event.id,
        timestamp: Date.now(),
        type: 'query',
        operation: 'RESPONSE',
        entity: query.kind || 'unknown',
        payload,
        metadata: {
          duration: Date.now() - event.timestamp
        },
        success,
        error: error.response?.data?.error || error.message || 'Query failed'
      });
    }
  }

  /**
   * Handle a mutation request from the event bus
   */
  private async handleMutationRequest(event) {
    const mutation = event.payload as EntityModificationRequest;

    try {
      // Execute the mutation using Stellar
      const response = await StellarClient.post('stellar/mutate', mutation);

      // Publish success response
      this.eventBus.publish({
        id: event.id,
        timestamp: Date.now(),
        type: 'mutation',
        operation: 'RESPONSE',
        entity: mutation.entityName,
        payload: response.data,
        metadata: {
          duration: Date.now() - event.timestamp
        },
        success: true
      });
    } catch (error) {
      // Publish error response
      this.eventBus.publish({
        id: event.id,
        timestamp: Date.now(),
        type: 'mutation',
        operation: 'RESPONSE',
        entity: mutation.entityName,
        payload,
        metadata: {
          duration: Date.now() - event.timestamp
        },
        success,
        error: error.response?.data?.error || error.message || 'Mutation failed'
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
let localStoreInstance: LocalStore | null = null;

export function getLocalStore(): LocalStore {
  if (!localStoreInstance) {
    localStoreInstance = new LocalStore();
  }
  return localStoreInstance;
}

// Auto-initialize when module is imported
getLocalStore();