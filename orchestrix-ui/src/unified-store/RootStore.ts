import { makeAutoObservable } from 'mobx';
import { OrganizationInfraStore } from './infrastructure/OrganizationInfraStore';

export class RootStore {
  organizationInfraStore: OrganizationInfraStore;

  constructor() {
    this.organizationInfraStore = new OrganizationInfraStore();
    makeAutoObservable(this);
  }

  // Reset all stores
  reset() {
    console.log('Resetting all stores...');
    this.organizationInfraStore.reset();
  }
}

// Singleton instance
const rootStore = new RootStore();
export default rootStore;