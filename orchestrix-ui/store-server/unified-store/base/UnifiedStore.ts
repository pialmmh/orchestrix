import { makeObservable, observable, action, runInAction } from 'mobx';
import { getEventBus } from '../events/EventBus';
import { getStoreDebugConfig } from '../../config/storeDebugConfig';
import { v4 as uuidv4 } from 'uuid';

export interface ApiResponse<T = any> {
  success: boolean;
  data?: T;
  error?: string;
  metadata?: {
    timestamp: number;
    duration?: number;
    retryCount?: number;
  };
}

export interface StoreError {
  code: string;
  message: string;
  details?: any;
  timestamp: number;
  operation?: string;
}

/**
 * Unified Store Base Class
 * Handles both success and error responses consistently
 * Works identically in both frontend (local) and backend (debug) modes
 */
export abstract class UnifiedStore {
  // Observable state
  loading: boolean = false;
  error: StoreError | null = null;
  lastOperation: string | null = null;
  operationHistory: Array<{
    id: string;
    operation: string;
    timestamp: number;
    success: boolean;
    duration: number;
  }> = [];

  // Event bus for communication
  protected eventBus = getEventBus();
  protected debugMode = getStoreDebugConfig().store_debug;

  constructor() {
    makeObservable(this, {
      loading: observable,
      error: observable,
      lastOperation: observable,
      operationHistory: observable,
      setLoading: action,
      setError: action,
      clearError: action,
      addToHistory: action,
    });
  }

  // State management actions
  setLoading(loading: boolean) {
    this.loading = loading;
  }

  setError(error: StoreError | null) {
    this.error = error;
    if (error) {
      console.error(`[${this.constructor.name}] Error:`, error);
    }
  }

  clearError() {
    this.error = null;
  }

  addToHistory(entry: any) {
    this.operationHistory.push(entry);
    // Keep only last 100 entries
    if (this.operationHistory.length > 100) {
      this.operationHistory.shift();
    }
  }

  /**
   * Execute an API operation with comprehensive error handling
   */
  protected async executeOperation<T = any>(
    operation: string,
    executor: () => Promise<ApiResponse<T>>,
    options: {
      retryCount?: number;
      retryDelay?: number;
      timeout?: number;
      errorHandler?: (error: StoreError) => void;
    } = {}
  ): Promise<ApiResponse<T>> {
    const operationId = uuidv4();
    const startTime = Date.now();

    const {
      retryCount = 0,
      retryDelay = 1000,
      timeout = 30000,
      errorHandler
    } = options;

    this.lastOperation = operation;
    this.setLoading(true);
    this.clearError();

    // Publish operation start event
    this.publishEvent({
      id: operationId,
      type: 'operation',
      operation: 'START',
      entity: this.constructor.name,
      payload: { operation },
      timestamp: startTime,
    });

    let attempt = 0;
    let lastError: StoreError | null = null;

    while (attempt <= retryCount) {
      try {
        // Execute with timeout
        const response = await this.withTimeout(executor(), timeout);

        if (response.success) {
          // Handle success
          runInAction(() => {
            this.setLoading(false);
            this.addToHistory({
              id: operationId,
              operation,
              timestamp: startTime,
              success: true,
              duration: Date.now() - startTime,
            });
          });

          // Publish success event
          this.publishEvent({
            id: operationId,
            type: 'operation',
            operation: 'SUCCESS',
            entity: this.constructor.name,
            payload: response.data,
            timestamp: Date.now(),
            metadata: {
              duration: Date.now() - startTime,
              attempt,
            },
          });

          return response;
        } else {
          // Handle API error response
          lastError = {
            code: 'API_ERROR',
            message: response.error || 'Operation failed',
            details: response,
            timestamp: Date.now(),
            operation,
          };
        }
      } catch (error: any) {
        // Handle network/timeout errors
        lastError = {
          code: error.code || 'NETWORK_ERROR',
          message: error.message || 'Network request failed',
          details: error,
          timestamp: Date.now(),
          operation,
        };
      }

      // Retry logic
      if (attempt < retryCount) {
        attempt++;
        console.log(`[${this.constructor.name}] Retrying ${operation} (attempt ${attempt + 1}/${retryCount + 1})`);
        await new Promise(resolve => setTimeout(resolve, retryDelay * attempt));
      } else {
        break;
      }
    }

    // All attempts failed
    runInAction(() => {
      this.setError(lastError);
      this.setLoading(false);
      this.addToHistory({
        id: operationId,
        operation,
        timestamp: startTime,
        success: false,
        duration: Date.now() - startTime,
      });
    });

    // Custom error handler
    if (errorHandler && lastError) {
      errorHandler(lastError);
    }

    // Publish error event
    this.publishEvent({
      id: operationId,
      type: 'operation',
      operation: 'ERROR',
      entity: this.constructor.name,
      payload: lastError,
      timestamp: Date.now(),
      metadata: {
        duration: Date.now() - startTime,
        attempts: attempt + 1,
      },
    });

    return {
      success: false,
      error: lastError?.message || 'Operation failed',
      metadata: {
        timestamp: Date.now(),
        duration: Date.now() - startTime,
        retryCount: attempt,
      },
    };
  }

  /**
   * Add timeout to a promise
   */
  private withTimeout<T>(promise: Promise<T>, timeout: number): Promise<T> {
    return Promise.race([
      promise,
      new Promise<never>((_, reject) =>
        setTimeout(() => reject(new Error(`Operation timed out after ${timeout}ms`)), timeout)
      ),
    ]);
  }

  /**
   * Publish event to event bus
   */
  protected publishEvent(event: any) {
    if (this.debugMode) {
      console.log(`[${this.constructor.name}] Event:`, event);
    }
    this.eventBus.publish(event);
  }

  /**
   * Reset store to initial state
   */
  reset() {
    this.loading = false;
    this.error = null;
    this.lastOperation = null;
    this.operationHistory = [];
  }

  /**
   * Get operation statistics
   */
  getStats() {
    const total = this.operationHistory.length;
    const successful = this.operationHistory.filter(op => op.success).length;
    const failed = total - successful;
    const avgDuration = total > 0
      ? this.operationHistory.reduce((sum, op) => sum + op.duration, 0) / total
      : 0;

    return {
      total,
      successful,
      failed,
      successRate: total > 0 ? (successful / total) * 100 : 0,
      avgDuration: Math.round(avgDuration),
    };
  }
}