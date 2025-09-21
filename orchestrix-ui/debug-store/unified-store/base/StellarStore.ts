import { makeObservable, observable, action, runInAction } from 'mobx';
import QueryService from '../services/QueryService';
import MutationService from '../services/MutationService';
import { QueryNode, QueryResponse } from '../../models/stellar/QueryNode';
import { EntityModificationRequest, MutationResponse } from '../../models/stellar/MutationRequest';

export abstract class StellarStore {
  loading: boolean = false;
  error: string | null = null;
  lastQuery: QueryNode | null = null;
  queryCache: Map<string, { data: any; timestamp: number }> = new Map();
  cacheTimeout: number = 5 * 60 * 1000; // 5 minutes default

  protected queryService = QueryService;
  protected mutationService = MutationService;

  constructor() {
    makeObservable(this, {
      loading: observable,
      error: observable,
      lastQuery: observable,
      setLoading: action,
      setError: action,
      clearError: action,
      executeQuery: action,
      executeMutation: action,
    });
  }

  setLoading(loading: boolean) {
    this.loading = loading;
  }

  setError(error: string | null) {
    this.error = error;
  }

  clearError() {
    this.error = null;
  }

  protected getCacheKey(query: QueryNode): string {
    return JSON.stringify(query);
  }

  protected getFromCache(query: QueryNode): any | null {
    const key = this.getCacheKey(query);
    const cached = this.queryCache.get(key);
    
    if (cached) {
      const now = Date.now();
      if (now - cached.timestamp < this.cacheTimeout) {
        return cached.data;
      } else {
        this.queryCache.delete(key);
      }
    }
    
    return null;
  }

  protected setCache(query: QueryNode, data: any) {
    const key = this.getCacheKey(query);
    this.queryCache.set(key, {
      data,
      timestamp: Date.now(),
    });
  }

  clearCache() {
    this.queryCache.clear();
  }

  async executeQuery<T = any>(query: QueryNode, useCache: boolean = true): Promise<T[] | null> {
    try {
      // Check cache first
      if (useCache) {
        const cached = this.getFromCache(query);
        if (cached) {
          console.log('Returning cached data for query:', query);
          return cached;
        }
      }

      this.setLoading(true);
      this.clearError();
      this.lastQuery = query;

      const response = await this.queryService.executeQuery<T>(query);
      console.log('🔍 StellarStore executeQuery response:', response);

      if (response.success && response.data) {
        runInAction(() => {
          this.setLoading(false);
          if (useCache) {
            this.setCache(query, response.data);
          }
        });
        return response.data;
      } else {
        runInAction(() => {
          this.setError(response.error || 'Query failed');
          this.setLoading(false);
        });
        return null;
      }
    } catch (error: any) {
      runInAction(() => {
        this.setError(error.message || 'Query execution failed');
        this.setLoading(false);
      });
      return null;
    }
  }

  async executeMutation(request: EntityModificationRequest): Promise<MutationResponse> {
    try {
      this.setLoading(true);
      this.clearError();

      const response = await this.mutationService.executeMutation(request);

      runInAction(() => {
        this.setLoading(false);
        if (!response.success) {
          this.setError(response.error || 'Mutation failed');
        } else {
          // Clear cache after successful mutation
          this.clearCache();
        }
      });

      return response;
    } catch (error: any) {
      runInAction(() => {
        this.setError(error.message || 'Mutation execution failed');
        this.setLoading(false);
      });
      return {
        success: false,
        error: error.message || 'Mutation execution failed',
      };
    }
  }

  async refetch(): Promise<void> {
    if (this.lastQuery) {
      await this.executeQuery(this.lastQuery, false);
    }
  }
}