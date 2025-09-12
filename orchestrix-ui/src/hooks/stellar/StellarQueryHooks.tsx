import { useState, useEffect } from 'react';
import { makeAutoObservable, runInAction } from 'mobx';
import axios from 'axios';

// Configuration
const SPRING_BOOT_URL = 'http://localhost:8091';

// Types
interface QueryNode {
  kind: string;
  criteria?: Record<string, any>;
  page?: { limit: number; offset?: number };
  include?: QueryNode[];
  lazy?: boolean;
}

interface QueryResult<T> {
  data: T[];
  loading: boolean;
  error: string | null;
  refresh: () => Promise<void>;
}

// Observable Query Class
class QueryObservable<T = any> {
  data: T[] = [];
  loading: boolean = false;
  error: string | null = null;
  
  constructor(
    private query: QueryNode,
    private baseUrl: string = SPRING_BOOT_URL
  ) {
    makeAutoObservable(this);
  }
  
  async execute() {
    runInAction(() => {
      this.loading = true;
      this.error = null;
    });
    
    try {
      const response = await axios.post(
        `${this.baseUrl}/api/query`,
        this.query,
        {
          headers: { 'Content-Type': 'application/json' }
        }
      );
      
      runInAction(() => {
        this.data = response.data.data || [];
        this.loading = false;
      });
    } catch (error: any) {
      runInAction(() => {
        this.error = error.response?.data?.error || error.message;
        this.loading = false;
      });
    }
  }
}

// Main Query Hook
export function useStellarQuery<T = any>(
  query: QueryNode,
  options?: {
    autoFetch?: boolean;
    baseUrl?: string;
  }
): QueryResult<T> {
  const { autoFetch = true, baseUrl = SPRING_BOOT_URL } = options || {};
  const [observable] = useState(() => new QueryObservable<T>(query, baseUrl));
  
  useEffect(() => {
    if (autoFetch) {
      observable.execute();
    }
  }, [autoFetch]);
  
  return {
    data: observable.data,
    loading: observable.loading,
    error: observable.error,
    refresh: () => observable.execute()
  };
}

// Mutation Hook
export function useStellarMutation(baseUrl: string = SPRING_BOOT_URL) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  const mutate = async (request: {
    entityName: string;
    operation: 'INSERT' | 'UPDATE' | 'DELETE';
    data?: Record<string, any>;
    criteria?: Record<string, any>;
    include?: any[];
  }) => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await axios.post(
        `${baseUrl}/api/modify`,
        request,
        {
          headers: { 'Content-Type': 'application/json' }
        }
      );
      setLoading(false);
      return response.data;
    } catch (error: any) {
      const errorMessage = error.response?.data?.error || error.message;
      setError(errorMessage);
      setLoading(false);
      throw new Error(errorMessage);
    }
  };
  
  return { mutate, loading, error };
}