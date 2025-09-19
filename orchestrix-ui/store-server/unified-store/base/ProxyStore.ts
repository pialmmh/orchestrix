import { makeObservable, observable, action, runInAction } from 'mobx';
import { getEventBus } from '../../store/events/EventBus';
import { v4 as uuidv4 } from 'uuid';

/**
 * ProxyStore for debug mode - doesn't store data locally,
 * just forwards all operations to the backend via WebSocket
 */
export class ProxyStore {
  loading: boolean = false;
  error: string | null = null;
  data: any = null;

  private eventBus = getEventBus();
  private storeName: string;

  constructor(storeName: string) {
    this.storeName = storeName;

    makeObservable(this, {
      loading: observable,
      error: observable,
      data: observable,
      setLoading: action,
      setError: action,
      setData: action,
    });

    // Subscribe to state updates from backend
    this.subscribeToBackendUpdates();
  }

  setLoading(loading: boolean) {
    this.loading = loading;
  }

  setError(error: string | null) {
    this.error = error;
  }

  setData(data: any) {
    this.data = data;
  }

  private subscribeToBackendUpdates() {
    // Listen for state updates from backend
    this.eventBus.subscribe('*', (event: any) => {
      if (event.type === 'state-update' && event.storeName === this.storeName) {
        runInAction(() => {
          this.data = event.payload;
        });
      }
    });
  }

  /**
   * Generic method to call any store method on the backend
   */
  async callBackendMethod(methodName: string, args: any[]): Promise<any> {
    const eventId = uuidv4();

    this.setLoading(true);
    this.setError(null);

    try {
      const response = await this.eventBus.request(eventId, {
        type: 'store-method',
        storeName: this.storeName,
        methodName,
        args,
      });

      this.setLoading(false);
      return response;
    } catch (error: any) {
      this.setError(error.message);
      this.setLoading(false);
      throw error;
    }
  }

  /**
   * Request current state from backend
   */
  async syncState(): Promise<void> {
    const eventId = uuidv4();

    try {
      const state = await this.eventBus.request(eventId, {
        type: 'get-state',
        storeName: this.storeName,
      });

      runInAction(() => {
        this.data = state;
      });
    } catch (error: any) {
      console.error('Failed to sync state:', error);
      this.setError(error.message);
    }
  }
}

/**
 * Create a proxy for infrastructure store
 */
export class ProxyInfrastructureStore extends ProxyStore {
  treeData: any[] = [];
  selectedNode: any = null;
  expandedNodeIds: string[] = [];

  constructor() {
    super('infrastructure');

    makeObservable(this, {
      treeData: observable,
      selectedNode: observable,
      expandedNodeIds: observable,
    });
  }

  async loadInfrastructureTree(partner: string = 'self') {
    const result = await this.callBackendMethod('loadInfrastructureTree', [partner]);

    runInAction(() => {
      this.treeData = result.treeData || [];
    });

    return result;
  }

  async setSelectedNode(node: any) {
    await this.callBackendMethod('setSelectedNode', [node]);

    runInAction(() => {
      this.selectedNode = node;
    });
  }

  async toggleNodeExpanded(nodeId: string) {
    await this.callBackendMethod('toggleNodeExpanded', [nodeId]);

    runInAction(() => {
      const index = this.expandedNodeIds.indexOf(nodeId);
      if (index >= 0) {
        this.expandedNodeIds.splice(index, 1);
      } else {
        this.expandedNodeIds.push(nodeId);
      }
    });
  }

  // Add other proxy methods as needed
  async createCompute(datacenterId: number, computeData: any) {
    return this.callBackendMethod('createCompute', [datacenterId, computeData]);
  }

  async updateCompute(computeId: number, updates: any) {
    return this.callBackendMethod('updateCompute', [computeId, updates]);
  }

  async deleteCompute(computeId: number) {
    return this.callBackendMethod('deleteCompute', [computeId]);
  }
}