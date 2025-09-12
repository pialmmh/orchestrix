import StellarClient from './StellarClient';
import { EntityModificationRequest, MutationResponse } from '../models/stellar/MutationRequest';

class MutationService {
  async executeMutation(request: EntityModificationRequest): Promise<MutationResponse> {
    try {
      const response = await StellarClient.post<MutationResponse>('/stellar/modify', request);
      return response;
    } catch (error: any) {
      console.error('Mutation execution error:', error);
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