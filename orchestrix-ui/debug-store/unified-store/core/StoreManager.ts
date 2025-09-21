import { OrganizationInfraStore } from '../infrastructure/OrganizationInfraStore';
import { getEventBus } from '../events/EventBus';
import { getStoreDebugConfig } from '../../config/storeDebugConfig';
import { getSimpleStoreLogger } from './SimpleStoreLogger';
import * as fs from 'fs';
import * as path from 'path';
import { reaction, toJS } from 'mobx';

interface StoreSnapshot {
  timestamp: number;
  stores: Record<string, any>;
  metadata: {
    eventCount: number;
    lastOperation?: string;
  };
}

export class StoreManager {
  private stores: Map<string, any> = new Map();
  private eventBus = getEventBus();
  private debugMode = getStoreDebugConfig().store_debug;
  private simpleLogger = getSimpleStoreLogger();
  private disposers: Array<() => void> = [];
  private storeStates: Map<string, any> = new Map();

  constructor() {
    // Initialize stores
    this.initializeStores();

    // Setup reactive persistence for debug mode
    if (this.debugMode) {
      this.setupReactivePersistence();
    }
  }

  private initializeStores() {
    // Register all stores
    const infraStore = new OrganizationInfraStore();
    this.stores.set('infrastructure', infraStore);

    // Initialize debug logging for each store
    if (this.debugMode) {
      this.stores.forEach((store, name) => {
        const initialState = toJS(store);
        this.storeStates.set(name, initialState);
        this.simpleLogger.initializeStore(name, initialState);
      });
    }

    // Add more stores as needed
    // this.stores.set('order', new OrderStore());
    // this.stores.set('user', new UserStore());
  }


  private setupReactivePersistence() {
    // React to any store changes and save snapshots
    this.stores.forEach((store, name) => {
      const disposer = reaction(
        () => toJS(store),
        (storeData) => {
          // Get previous state
          const previousState = this.storeStates.get(name);

          // Log the change
          this.simpleLogger.logHistory(
            name,
            'state_changed',
            previousState,
            storeData
          );

          // Update stored state
          this.storeStates.set(name, storeData);
        },
        { delay: 1000 } // Debounce for 1 second
      );
      this.disposers.push(disposer);
    });
  }




  getStore<T = any>(name: string): T {
    const store = this.stores.get(name);
    if (!store) {
      throw new Error(`Store '${name}' not found`);
    }
    return store as T;
  }

  getAllStores(): Map<string, any> {
    return this.stores;
  }

  // Test helper - can be used without UI
  async testStoreOperation(storeName: string, operation: () => Promise<any>) {
    const store = this.getStore(storeName);
    console.log(`🧪 Testing store: ${storeName}`);

    try {
      const result = await operation();
      console.log('✅ Test passed:', result);
      return { success: true, result };
    } catch (error) {
      console.error('❌ Test failed:', error);
      return { success: false, error };
    }
  }


  // Clean up
  dispose() {
    this.disposers.forEach(disposer => disposer());
    this.disposers = [];
  }
}

// Singleton instance
let managerInstance: StoreManager | null = null;

export function getStoreManager(): StoreManager {
  if (!managerInstance) {
    managerInstance = new StoreManager();
  }
  return managerInstance;
}

export function resetStoreManager(): void {
  if (managerInstance) {
    managerInstance.dispose();
    managerInstance = null;
  }
}