import StellarClient from '../../services/StellarClient';
import { EntityModificationRequest, MutationResponse } from '../../models/stellar/MutationRequest';
import { getEventBus } from '../../store/events/EventBus';
import { StoreEvent, StoreEventResponse } from '../events/StoreEvent';
import { v4 as uuidv4 } from 'uuid';
import { getStoreDebugConfig } from '../../config/storeDebugConfig';

class MutationService {
  private eventBus = getEventBus();
  private debugMode = getStoreDebugConfig().store_debug;
  async executeMutation(request: EntityModificationRequest): Promise<MutationResponse> {
    const eventId = uuidv4();
    const timestamp = Date.now();

    // Publish mutation start event if in debug mode
    if (this.debugMode) {
      const startEvent: StoreEvent = {
        id: eventId,
        timestamp,
        type: 'mutation',
        operation: `${request.operation}_START`,
        entity: request.entityName,
        payload: request,
        metadata: {
          endpoint: '/stellar/modify',
          method: 'POST',
        },
      };
      this.eventBus.publish(startEvent);
    }

    try {
      const response = await StellarClient.post<MutationResponse>('stellar/modify', request);
      
      // Publish mutation success event if in debug mode
      if (this.debugMode) {
        const successEvent: StoreEventResponse = {
          id: eventId,
          timestamp: Date.now(),
          type: 'mutation',
          operation: `${request.operation}_SUCCESS`,
          entity: request.entityName,
          payload: response,
          metadata: {
            endpoint: '/stellar/modify',
            method: 'POST',
            duration: Date.now() - timestamp,
          },
          success: true,
        };
        this.eventBus.publish(successEvent);
      }
      
      return response;
    } catch (error: any) {
      console.error('Mutation execution error:', error);
      
      // Publish mutation error event if in debug mode
      if (this.debugMode) {
        const errorEvent: StoreEventResponse = {
          id: eventId,
          timestamp: Date.now(),
          type: 'mutation',
          operation: `${request.operation}_ERROR`,
          entity: request.entityName,
          payload: error.response?.data || error,
          metadata: {
            endpoint: '/stellar/modify',
            method: 'POST',
            duration: Date.now() - timestamp,
          },
          success: false,
          error: error.response?.data?.error || error.message || 'Mutation failed',
        };
        this.eventBus.publish(errorEvent);
      }
      
      return {
        success: false,
        error: error.response?.data?.error || error.message || 'Mutation failed',
      };
    }
  }

  async createEntity(entityName: string, data: Record<string, any>): Promise<MutationResponse> {
    return this.executeMutation({
      entityName,
      operation: 'INSERT',
      data,
    });
  }

  async updateEntity(entityName: string, id: number | string, data: Record<string, any>): Promise<MutationResponse> {
    return this.executeMutation({
      entityName,
      operation: 'UPDATE',
      id,
      data,
    });
  }

  async deleteEntity(entityName: string, id: number | string): Promise<MutationResponse> {
    return this.executeMutation({
      entityName,
      operation: 'DELETE',
      id,
    });
  }

  async batchMutation(requests: EntityModificationRequest[]): Promise<MutationResponse[]> {
    try {
      const promises = requests.map(req => this.executeMutation(req));
      const results = await Promise.allSettled(promises);
      
      return results.map(result => {
        if (result.status === 'fulfilled') {
          return result.value;
        } else {
          return {
            success: false,
            error: result.reason?.message || 'Mutation failed',
          };
        }
      });
    } catch (error: any) {
      console.error('Batch mutation error:', error);
      return requests.map(() => ({
        success: false,
        error: 'Batch mutation failed',
      }));
    }
  }
}

export default new MutationService();