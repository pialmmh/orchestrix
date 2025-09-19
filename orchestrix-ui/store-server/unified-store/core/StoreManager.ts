import { OrganizationInfraStore } from '../infrastructure/OrganizationInfraStore';
import { getEventBus } from '../events/EventBus';
import { getStoreDebugConfig } from '../../config/storeDebugConfig';
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
  private snapshotDir: string;
  private snapshotInterval?: NodeJS.Timer;
  private disposers: Array<() => void> = [];

  constructor() {
    // Initialize stores
    this.initializeStores();

    // Setup snapshot directory for backend
    if (typeof process !== 'undefined' && process.versions?.node) {
      this.snapshotDir = path.join(process.cwd(), 'store-snapshots');
      this.ensureSnapshotDir();
      this.startSnapshotting();
    }

    // Setup reactive persistence
    this.setupReactivePersistence();
  }

  private initializeStores() {
    // Register all stores
    const infraStore = new OrganizationInfraStore();
    this.stores.set('infrastructure', infraStore);

    // Add more stores as needed
    // this.stores.set('order', new OrderStore());
    // this.stores.set('user', new UserStore());
  }

  private ensureSnapshotDir() {
    if (!fs.existsSync(this.snapshotDir)) {
      fs.mkdirSync(this.snapshotDir, { recursive: true });
    }
  }

  private setupReactivePersistence() {
    // React to any store changes and save snapshots
    this.stores.forEach((store, name) => {
      const disposer = reaction(
        () => toJS(store),
        (storeData) => {
          if (this.debugMode && typeof process !== 'undefined') {
            this.saveSnapshot(name, storeData);
          }
        },
        { delay: 1000 } // Debounce for 1 second
      );
      this.disposers.push(disposer);
    });
  }

  private saveSnapshot(storeName: string, storeData: any) {
    if (!this.snapshotDir) return;

    const timestamp = Date.now();
    const fileName = `${storeName}-latest.json`;
    const filePath = path.join(this.snapshotDir, fileName);

    const snapshot = {
      timestamp,
      storeName,
      data: storeData,
      metadata: {
        debugMode: this.debugMode,
        nodeVersion: process.version,
        lastUpdate: new Date(timestamp).toISOString()
      }
    };

    try {
      // Write pretty-printed JSON for readability
      fs.writeFileSync(filePath, JSON.stringify(snapshot, null, 2));

      // Also write timestamped backup
      const backupName = `${storeName}-${timestamp}.json`;
      const backupPath = path.join(this.snapshotDir, 'history', backupName);

      if (!fs.existsSync(path.dirname(backupPath))) {
        fs.mkdirSync(path.dirname(backupPath), { recursive: true });
      }

      fs.writeFileSync(backupPath, JSON.stringify(snapshot, null, 2));

      console.log(`💾 Store snapshot saved: ${fileName}`);
    } catch (error) {
      console.error('Failed to save snapshot:', error);
    }
  }

  private startSnapshotting() {
    // Save complete snapshot every 30 seconds
    this.snapshotInterval = setInterval(() => {
      this.saveCompleteSnapshot();
    }, 30000);
  }

  private saveCompleteSnapshot() {
    if (!this.snapshotDir) return;

    const allStores: Record<string, any> = {};
    this.stores.forEach((store, name) => {
      allStores[name] = toJS(store);
    });

    const snapshot: StoreSnapshot = {
      timestamp: Date.now(),
      stores: allStores,
      metadata: {
        eventCount: this.eventBus.getEventCount?.() || 0,
        lastOperation: this.eventBus.getLastOperation?.() || undefined
      }
    };

    const fileName = 'all-stores-latest.json';
    const filePath = path.join(this.snapshotDir, fileName);

    try {
      fs.writeFileSync(filePath, JSON.stringify(snapshot, null, 2));
      console.log(`💾 Complete store snapshot saved`);
    } catch (error) {
      console.error('Failed to save complete snapshot:', error);
    }
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

  // Load snapshot from file (useful for testing/debugging)
  loadSnapshot(storeName: string): any {
    if (!this.snapshotDir) return null;

    const fileName = `${storeName}-latest.json`;
    const filePath = path.join(this.snapshotDir, fileName);

    try {
      const content = fs.readFileSync(filePath, 'utf-8');
      const snapshot = JSON.parse(content);
      console.log(`📂 Loaded snapshot for ${storeName}`);
      return snapshot;
    } catch (error) {
      console.error(`Failed to load snapshot for ${storeName}:`, error);
      return null;
    }
  }

  // Clean up
  dispose() {
    if (this.snapshotInterval) {
      clearInterval(this.snapshotInterval);
    }
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