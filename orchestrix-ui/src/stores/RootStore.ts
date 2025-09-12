import { OrganizationInfraStore } from './infrastructure/OrganizationInfraStore';

export class RootStore {
  organizationInfraStore: OrganizationInfraStore;

  constructor() {
    this.organizationInfraStore = new OrganizationInfraStore();
  }

  // Initialize all stores
  async initialize() {
    // Load initial data for all stores
    await Promise.all([
      this.organizationInfraStore.loadInfrastructureTree(),
      this.organizationInfraStore.loadPartners(),
    ]);
  }

  // Clear all caches
  clearAllCaches() {
    this.organizationInfraStore.clearCache();
  }
}

// Create singleton instance
const rootStore = new RootStore();

export default rootStore;