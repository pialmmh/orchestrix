// Auto-generated from TypeScript source
// Original: services/QueryService.ts
// Generated: 2025-09-19T10:56:20.507Z

const StellarClient = require('../../services/StellarClient');
const { QueryNode, QueryResponse } = require('../../models/stellar/QueryNode');
const { getEventBus } = require('../../store/events/EventBus');
const { StoreEvent, StoreEventResponse } = require('../events/StoreEvent');
const { v4 as uuidv4 } = require('uuid');
const { getStoreDebugConfig } = require('../../config/storeDebugConfig');
// Initialize LocalStore to handle events
import '../events/LocalStore';

class QueryService {
  private eventBus = getEventBus();
  private debugMode = getStoreDebugConfig().store_debug;
  private useEventBus = true; // Toggle to switch between EventBus and direct HTTP

  async executeQuery<T = any>(query): Promise<QueryResponse> {
    const eventId = uuidv4();
    const timestamp = Date.now();

    // Publish query start event if in debug mode
    if (this.debugMode) {
      const startEvent: StoreEvent = {
        id,
        type: 'query',
        operation: 'QUERY_START',
        entity: query.kind || 'unknown',
        payload,
        metadata: {
          endpoint: '/stellar/query',
          method: 'POST',
        },
      };
      this.eventBus.publish(startEvent);
    }

    try {
      // Use EventBus for queries
      let response: QueryResponse;

      if (this.useEventBus) {
        // Send query through EventBus and wait for response
        const data = await this.eventBus.request(eventId, query);
        response = { success, data } as QueryResponse;
      } else {
        // Fallback to direct HTTP call
        response = await StellarClient.post<QueryResponse>('stellar/query', query);
      }
      
      // Publish query success event if in debug mode
      if (this.debugMode) {
        const successEvent: StoreEventResponse = {
          id,
          timestamp: Date.now(),
          type: 'query',
          operation: 'QUERY_SUCCESS',
          entity: query.kind || 'unknown',
          payload,
          metadata: {
            endpoint: '/stellar/query',
            method: 'POST',
            duration: Date.now() - timestamp,
          },
          success,
        };
        this.eventBus.publish(successEvent);
      }
      
      return response;
    } catch (error) {
      console.error('Query execution error:', error);
      
      // Publish query error event if in debug mode
      if (this.debugMode) {
        const errorEvent: StoreEventResponse = {
          id,
          timestamp: Date.now(),
          type: 'query',
          operation: 'QUERY_ERROR',
          entity: query.kind || 'unknown',
          payload: error.response?.data || error,
          metadata: {
            endpoint: '/stellar/query',
            method: 'POST',
            duration: Date.now() - timestamp,
          },
          success,
          error: error.response?.data?.error || error.message || 'Query failed',
        };
        this.eventBus.publish(errorEvent);
      }
      
      return {
        success,
        error: error.response?.data?.error || error.message || 'Query failed',
      };
    }
  }

  async executeQueryByKind<T = any>(kind, query): Promise<QueryResponse> {
    const eventId = uuidv4();
    const timestamp = Date.now();

    // Publish query start event if in debug mode
    if (this.debugMode) {
      const startEvent: StoreEvent = {
        id,
        type: 'query',
        operation: 'QUERY_START',
        entity,
        payload,
        metadata: {
          endpoint: `/stellar/${kind}`,
          method: 'POST',
        },
      };
      this.eventBus.publish(startEvent);
    }

    try {
      const response = await StellarClient.post<QueryResponse>(`/stellar/${kind}`, query);
      
      // Publish query success event if in debug mode
      if (this.debugMode) {
        const successEvent: StoreEventResponse = {
          id,
          timestamp: Date.now(),
          type: 'query',
          operation: 'QUERY_SUCCESS',
          entity,
          payload,
          metadata: {
            endpoint: `/stellar/${kind}`,
            method: 'POST',
            duration: Date.now() - timestamp,
          },
          success,
        };
        this.eventBus.publish(successEvent);
      }
      
      return response;
    } catch (error) {
      console.error('Query execution error:', error);
      
      // Publish query error event if in debug mode
      if (this.debugMode) {
        const errorEvent: StoreEventResponse = {
          id,
          timestamp: Date.now(),
          type: 'query',
          operation: 'QUERY_ERROR',
          entity,
          payload: error.response?.data || error,
          metadata: {
            endpoint: `/stellar/${kind}`,
            method: 'POST',
            duration: Date.now() - timestamp,
          },
          success,
          error: error.response?.data?.error || error.message || 'Query failed',
        };
        this.eventBus.publish(errorEvent);
      }
      
      return {
        success,
        error: error.response?.data?.error || error.message || 'Query failed',
      };
    }
  }

  // Cache management
  async getCacheStats(): Promise {
    try {
      const response = await StellarClient.get('/stellar/cache/stats');
      return response;
    } catch (error) {
      console.error('Failed to get cache stats:', error);
      return null;
    }
  }

  async clearCache(): Promise {
    try {
      await StellarClient.delete('/stellar/cache/clear');
      return true;
    } catch (error) {
      console.error('Failed to clear cache:', error);
      return false;
    }
  }
}

module.exports = new QueryService();