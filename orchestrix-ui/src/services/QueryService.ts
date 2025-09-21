import StellarClient from './StellarClient';
import { QueryNode, QueryResponse } from '../models/stellar/QueryNode';
import { getEventBus } from '../store/events/EventBus';
import { StoreEvent, StoreEventResponse } from '../store/events/StoreEvent';
import { v4 as uuidv4 } from 'uuid';
import { getStoreDebugConfig } from '../config/storeDebugConfig';
// Initialize LocalStore to handle events
import '../store/events/LocalStore';
// Initialize EventBusWebSocketHandler for debug mode
import '../store/events/EventBusWebSocketHandler';

class QueryService {
  private eventBus = getEventBus();
  private debugMode = getStoreDebugConfig().store_debug;
  private useEventBus = true; // Toggle to switch between EventBus and direct HTTP

  async executeQuery<T = any>(query: QueryNode): Promise<QueryResponse<T>> {
    const eventId = uuidv4();
    const timestamp = Date.now();

    // Publish query start event if in debug mode
    if (this.debugMode) {
      const startEvent: StoreEvent = {
        id: eventId,
        timestamp,
        type: 'query',
        operation: 'QUERY_START',
        entity: query.kind || 'unknown',
        payload: query,
        metadata: {
          endpoint: '/stellar/query',
          method: 'POST',
        },
      };
      this.eventBus.publish(startEvent);
    }

    try {
      // Use EventBus for queries
      let response: QueryResponse<T>;

      if (this.useEventBus) {
        // Send query through EventBus and wait for response
        const data = await this.eventBus.request<T>(eventId, query);
        response = { success: true, data } as QueryResponse<T>;
      } else {
        // Fallback to direct HTTP call
        response = await StellarClient.post<QueryResponse<T>>('stellar/query', query);
      }
      
      // Publish query success event if in debug mode
      if (this.debugMode) {
        const successEvent: StoreEventResponse = {
          id: eventId,
          timestamp: Date.now(),
          type: 'query',
          operation: 'QUERY_SUCCESS',
          entity: query.kind || 'unknown',
          payload: response,
          metadata: {
            endpoint: '/stellar/query',
            method: 'POST',
            duration: Date.now() - timestamp,
          },
          success: true,
        };
        this.eventBus.publish(successEvent);
      }
      
      return response;
    } catch (error: any) {
      console.error('Query execution error:', error);
      
      // Publish query error event if in debug mode
      if (this.debugMode) {
        const errorEvent: StoreEventResponse = {
          id: eventId,
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
          success: false,
          error: error.response?.data?.error || error.message || 'Query failed',
        };
        this.eventBus.publish(errorEvent);
      }
      
      return {
        success: false,
        error: error.response?.data?.error || error.message || 'Query failed',
      };
    }
  }

  async executeQueryByKind<T = any>(kind: string, query: QueryNode): Promise<QueryResponse<T>> {
    const eventId = uuidv4();
    const timestamp = Date.now();

    // Publish query start event if in debug mode
    if (this.debugMode) {
      const startEvent: StoreEvent = {
        id: eventId,
        timestamp,
        type: 'query',
        operation: 'QUERY_START',
        entity: kind,
        payload: query,
        metadata: {
          endpoint: `/stellar/${kind}`,
          method: 'POST',
        },
      };
      this.eventBus.publish(startEvent);
    }

    try {
      const response = await StellarClient.post<QueryResponse<T>>(`/stellar/${kind}`, query);
      
      // Publish query success event if in debug mode
      if (this.debugMode) {
        const successEvent: StoreEventResponse = {
          id: eventId,
          timestamp: Date.now(),
          type: 'query',
          operation: 'QUERY_SUCCESS',
          entity: kind,
          payload: response,
          metadata: {
            endpoint: `/stellar/${kind}`,
            method: 'POST',
            duration: Date.now() - timestamp,
          },
          success: true,
        };
        this.eventBus.publish(successEvent);
      }
      
      return response;
    } catch (error: any) {
      console.error('Query execution error:', error);
      
      // Publish query error event if in debug mode
      if (this.debugMode) {
        const errorEvent: StoreEventResponse = {
          id: eventId,
          timestamp: Date.now(),
          type: 'query',
          operation: 'QUERY_ERROR',
          entity: kind,
          payload: error.response?.data || error,
          metadata: {
            endpoint: `/stellar/${kind}`,
            method: 'POST',
            duration: Date.now() - timestamp,
          },
          success: false,
          error: error.response?.data?.error || error.message || 'Query failed',
        };
        this.eventBus.publish(errorEvent);
      }
      
      return {
        success: false,
        error: error.response?.data?.error || error.message || 'Query failed',
      };
    }
  }

  // Cache management
  async getCacheStats(): Promise<any> {
    try {
      const response = await StellarClient.get('/stellar/cache/stats');
      return response;
    } catch (error) {
      console.error('Failed to get cache stats:', error);
      return null;
    }
  }

  async clearCache(): Promise<boolean> {
    try {
      await StellarClient.delete('/stellar/cache/clear');
      return true;
    } catch (error) {
      console.error('Failed to clear cache:', error);
      return false;
    }
  }
}

export default new QueryService();