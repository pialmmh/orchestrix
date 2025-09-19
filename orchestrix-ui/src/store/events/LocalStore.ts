import { getEventBus } from './EventBus';
import { StoreEvent, StoreEventResponse } from './StoreEvent';
import StellarClient from '../../services/StellarClient';
import { QueryNode, QueryResponse } from '../../models/stellar/QueryNode';
import { EntityModificationRequest, MutationResponse } from '../../models/stellar/MutationRequest';

/**
 * LocalStore handles query execution locally in the browser.
 * It listens for query events from the EventBus and processes them using Stellar.
 */
export class LocalStore {
  private eventBus = getEventBus();
  private isListening = false;

  constructor() {
    this.startListening();
  }

  /**
   * Start listening for query requests on the event bus
   */
  private startListening() {
    if (this.isListening) return;

    this.isListening = true;

    // Listen for all events
    this.eventBus.subscribe('*', async (event: StoreEvent | StoreEventResponse) => {
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
  private async handleQueryRequest(event: StoreEvent) {
    const query = event.payload as QueryNode;

    try {
      // Execute the query using Stellar
      const response = await StellarClient.post<QueryResponse>('stellar/query', query);

      // Publish success response
      this.eventBus.publish({
        id: event.id,
        timestamp: Date.now(),
        type: 'query',
        operation: 'RESPONSE',
        entity: query.kind || 'unknown',
        payload: response.data,
        metadata: {
          duration: Date.now() - event.timestamp
        },
        success: true
      });
    } catch (error: any) {
      // Publish error response
      this.eventBus.publish({
        id: event.id,
        timestamp: Date.now(),
        type: 'query',
        operation: 'RESPONSE',
        entity: query.kind || 'unknown',
        payload: null,
        metadata: {
          duration: Date.now() - event.timestamp
        },
        success: false,
        error: error.response?.data?.error || error.message || 'Query failed'
      });
    }
  }

  /**
   * Handle a mutation request from the event bus
   */
  private async handleMutationRequest(event: StoreEvent) {
    const mutation = event.payload as EntityModificationRequest;

    try {
      // Execute the mutation using Stellar
      const response = await StellarClient.post<MutationResponse>('stellar/mutate', mutation);

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
    } catch (error: any) {
      // Publish error response
      this.eventBus.publish({
        id: event.id,
        timestamp: Date.now(),
        type: 'mutation',
        operation: 'RESPONSE',
        entity: mutation.entityName,
        payload: null,
        metadata: {
          duration: Date.now() - event.timestamp
        },
        success: false,
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