import StellarClient from './StellarClient';
import { QueryNode, QueryResponse } from '../models/stellar/QueryNode';

class QueryService {
  async executeQuery<T = any>(query: QueryNode): Promise<QueryResponse<T>> {
    try {
      const response = await StellarClient.post<QueryResponse<T>>('/stellar/query', query);
      return response;
    } catch (error: any) {
      console.error('Query execution error:', error);
      return {
        success: false,
        error: error.response?.data?.error || error.message || 'Query failed',
      };
    }
  }

  async executeQueryByKind<T = any>(kind: string, query: QueryNode): Promise<QueryResponse<T>> {
    try {
      const response = await StellarClient.post<QueryResponse<T>>(`/stellar/${kind}`, query);
      return response;
    } catch (error: any) {
      console.error('Query execution error:', error);
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