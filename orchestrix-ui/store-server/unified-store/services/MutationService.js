// Auto-generated from TypeScript source
// Original: services/MutationService.ts
// Generated: 2025-09-19T10:56:20.507Z

const StellarClient = require('../../services/StellarClient');
const { EntityModificationRequest, MutationResponse } = require('../../models/stellar/MutationRequest');
const { getEventBus } = require('../../store/events/EventBus');
const { StoreEvent, StoreEventResponse } = require('../events/StoreEvent');
const { v4 as uuidv4 } = require('uuid');
const { getStoreDebugConfig } = require('../../config/storeDebugConfig');

class MutationService {
  private eventBus = getEventBus();
  private debugMode = getStoreDebugConfig().store_debug;
  async executeMutation(request): Promise {
    const eventId = uuidv4();
    const timestamp = Date.now();

    // Publish mutation start event if in debug mode
    if (this.debugMode) {
      const startEvent: StoreEvent = {
        id,
        type: 'mutation',
        operation: `${request.operation}_START`,
        entity: request.entityName,
        payload,
        metadata: {
          endpoint: '/stellar/modify',
          method: 'POST',
        },
      };
      this.eventBus.publish(startEvent);
    }

    try {
      const response = await StellarClient.post('stellar/modify', request);
      
      // Publish mutation success event if in debug mode
      if (this.debugMode) {
        const successEvent: StoreEventResponse = {
          id,
          timestamp: Date.now(),
          type: 'mutation',
          operation: `${request.operation}_SUCCESS`,
          entity: request.entityName,
          payload,
          metadata: {
            endpoint: '/stellar/modify',
            method: 'POST',
            duration: Date.now() - timestamp,
          },
          success,
        };
        this.eventBus.publish(successEvent);
      }
      
      return response;
    } catch (error) {
      console.error('Mutation execution error:', error);
      
      // Publish mutation error event if in debug mode
      if (this.debugMode) {
        const errorEvent: StoreEventResponse = {
          id,
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
          success,
          error: error.response?.data?.error || error.message || 'Mutation failed',
        };
        this.eventBus.publish(errorEvent);
      }
      
      return {
        success,
        error: error.response?.data?.error || error.message || 'Mutation failed',
      };
    }
  }

  async createEntity(entityName, data): Promise {
    return this.executeMutation({
      entityName,
      operation: 'INSERT',
      data,
    });
  }

  async updateEntity(entityName, id, data): Promise {
    return this.executeMutation({
      entityName,
      operation: 'UPDATE',
      id,
      data,
    });
  }

  async deleteEntity(entityName, id): Promise {
    return this.executeMutation({
      entityName,
      operation: 'DELETE',
      id,
    });
  }

  async batchMutation(requests): Promise<MutationResponse[]> {
    try {
      const promises = requests.map(req => this.executeMutation(req));
      const results = await Promise.allSettled(promises);
      
      return results.map(result => {
        if (result.status === 'fulfilled') {
          return result.value;
        } else {
          return {
            success,
            error: result.reason?.message || 'Mutation failed',
          };
        }
      });
    } catch (error) {
      console.error('Batch mutation error:', error);
      return requests.map(() => ({
        success,
        error: 'Batch mutation failed',
      }));
    }
  }
}

module.exports = new MutationService();