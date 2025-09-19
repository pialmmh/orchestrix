import { makeAutoObservable } from 'mobx';
import { OrganizationInfraStore } from './infrastructure/OrganizationInfraStore';
import { ProxyInfrastructureStore } from './base/ProxyStore';
import { getStoreDebugConfig } from '../config/storeDebugConfig';

export class RootStore {
  organizationInfraStore: OrganizationInfraStore | ProxyInfrastructureStore;
  debugMode: boolean;

  constructor() {
    const config = getStoreDebugConfig();
    this.debugMode = config.store_debug;

    if (this.debugMode) {
      console.log('🔧 Debug Mode: Creating proxy stores (backend handles state)');
      // In debug mode, create proxy stores that communicate with backend
      this.organizationInfraStore = new ProxyInfrastructureStore();
    } else {
      console.log('📦 Production Mode: Creating local stores');
      // In production mode, create actual stores that run locally
      this.organizationInfraStore = new OrganizationInfraStore();
    }

    makeAutoObservable(this);
  }

  // Reset all stores
  reset() {
    console.log('Resetting all stores...');
    if (!this.debugMode && 'reset' in this.organizationInfraStore) {
      this.organizationInfraStore.reset();
    } else if (this.debugMode && this.organizationInfraStore instanceof ProxyInfrastructureStore) {
      // Send reset command to backend
      this.organizationInfraStore.callBackendMethod('reset', []);
    }
  }

  // Get store type for debugging
  getStoreType() {
    return this.debugMode ? 'proxy' : 'local';
  }
}

// Singleton instance
const rootStore = new RootStore();

// Log store configuration
console.log(`🏪 RootStore initialized in ${rootStore.getStoreType()} mode`);

export default rootStore;