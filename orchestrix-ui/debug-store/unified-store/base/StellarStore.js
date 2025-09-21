// Auto-generated from TypeScript source
// Original: base/StellarStore.ts
// Generated: 2025-09-19T10:56:20.503Z

const { makeObservable, observable, action, runInAction } = require('mobx');
const QueryService = require('../../services/QueryService');
const MutationService = require('../../services/MutationService');
const { QueryNode, QueryResponse } = require('../../models/stellar/QueryNode');
const { EntityModificationRequest, MutationResponse } = require('../../models/stellar/MutationRequest');

export abstract class StellarStore {
  loading: boolean = false;
  error: string | null = null;
  lastQuery: QueryNode | null = null;
  queryCache, { data: any; timestamp: number }> = new Map();
  cacheTimeout: number = 5 * 60 * 1000; // 5 minutes default

  protected queryService = QueryService;
  protected mutationService = MutationService;

  constructor() {
    makeObservable(this, {
      loading,
      error,
      lastQuery,
      setLoading,
      setError,
      clearError,
      executeQuery,
      executeMutation,
    });
  }

  setLoading(loading) {
    this.loading = loading;
  }

  setError(error) {
    this.error = error;
  }

  clearError() {
    this.error = null;
  }

  protected getCacheKey(query): string {
    return JSON.stringify(query);
  }

  protected getFromCache(query): any | null {
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

  protected setCache(query, data) {
    const key = this.getCacheKey(query);
    this.queryCache.set(key, {
      data,
      timestamp: Date.now(),
    });
  }

  clearCache() {
    this.queryCache.clear();
  }

  async executeQuery<T = any>(query, useCache: boolean = true): Promise<T[] | null> {
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

      const response = await this.queryService.executeQuery(query);
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
    } catch (error) {
      runInAction(() => {
        this.setError(error.message || 'Query execution failed');
        this.setLoading(false);
      });
      return null;
    }
  }

  async executeMutation(request): Promise {
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
    } catch (error) {
      runInAction(() => {
        this.setError(error.message || 'Mutation execution failed');
        this.setLoading(false);
      });
      return {
        success,
        error: error.message || 'Mutation execution failed',
      };
    }
  }

  async refetch(): Promise {
    if (this.lastQuery) {
      await this.executeQuery(this.lastQuery, false);
    }
  }
}