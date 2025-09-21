// Auto-generated from TypeScript source
// Original: RootStore.ts
// Generated: 2025-09-19T10:56:20.502Z

const { makeAutoObservable } = require('mobx');
const { OrganizationInfraStore } = require('./infrastructure/OrganizationInfraStore');

export class RootStore {
  organizationInfraStore: OrganizationInfraStore;

  constructor() {
    this.organizationInfraStore = new OrganizationInfraStore();
    makeAutoObservable(this);
  }

  // Reset all stores
  reset() {
    this.organizationInfraStore.reset();
  }
}

// Singleton instance
const rootStore = new RootStore();
module.exports = rootStore;