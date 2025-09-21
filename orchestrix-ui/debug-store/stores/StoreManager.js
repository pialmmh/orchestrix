const fs = require('fs');
const path = require('path');

class StoreManager {
  constructor(debugMode = false) {
    this.debugMode = debugMode;
    this.stores = new Map();
    this.debugPath = path.join(__dirname, '..', 'store-debug');

    if (this.debugMode) {
      this.ensureDebugDirectory();
    }
  }

  ensureDebugDirectory() {
    if (!fs.existsSync(this.debugPath)) {
      fs.mkdirSync(this.debugPath, { recursive: true });
    }
  }

  createStore(name, StoreClass) {
    const store = new StoreClass();
    this.stores.set(name, store);

    if (this.debugMode) {
      const storePath = path.join(this.debugPath, name);
      if (!fs.existsSync(storePath)) {
        fs.mkdirSync(storePath, { recursive: true });
      }
      this.saveCurrentState(name);
    }

    return store;
  }

  getStore(name) {
    return this.stores.get(name);
  }

  handleQuery(storeName, query) {
    const store = this.stores.get(storeName);
    if (!store) return null;

    const before = this.debugMode ? JSON.parse(JSON.stringify(store.toJSON())) : null;

    // Log query event
    if (this.debugMode) {
      this.logEvent(storeName, {
        type: 'QUERY',
        query: query,
        timestamp: Date.now(),
        stateBefore: before
      });
    }

    store.setQuery(query);

    return store;
  }

  handleQueryResult(storeName, data, pageNumbers) {
    const store = this.stores.get(storeName);
    if (!store) return null;

    const before = this.debugMode ? JSON.parse(JSON.stringify(store.toJSON())) : null;

    store.setData(data, pageNumbers);

    if (this.debugMode) {
      this.logEvent(storeName, {
        type: 'QUERY_RESULT',
        timestamp: Date.now(),
        stateBefore: before,
        stateAfter: store.toJSON()
      });
      this.saveCurrentState(storeName);
    }

    return store;
  }

  handleMutation(storeName, mutation) {
    const store = this.stores.get(storeName);
    if (!store) return null;

    const before = this.debugMode ? JSON.parse(JSON.stringify(store.toJSON())) : null;

    // Log mutation event
    if (this.debugMode) {
      this.logEvent(storeName, {
        type: 'MUTATION',
        mutation: mutation,
        timestamp: Date.now(),
        stateBefore: before
      });
    }

    // Apply mutation based on type
    switch (mutation.operation) {
      case 'CREATE':
        store.data.push(mutation.data);
        break;
      case 'UPDATE':
        const updateIndex = store.data.findIndex(item => item.id === mutation.data.id);
        if (updateIndex !== -1) {
          store.data[updateIndex] = mutation.data;
        }
        break;
      case 'DELETE':
        store.data = store.data.filter(item => item.id !== mutation.id);
        break;
    }

    if (this.debugMode) {
      this.logEvent(storeName, {
        type: 'MUTATION_RESULT',
        timestamp: Date.now(),
        stateBefore: before,
        stateAfter: store.toJSON()
      });
      this.saveCurrentState(storeName);
    }

    return store;
  }

  saveCurrentState(storeName) {
    const store = this.stores.get(storeName);
    if (!store) return;

    const filePath = path.join(this.debugPath, storeName, 'current.json');
    fs.writeFileSync(filePath, JSON.stringify(store.toJSON(), null, 2));
  }

  logEvent(storeName, event) {
    const filePath = path.join(this.debugPath, storeName, 'history.jsonl');
    fs.appendFileSync(filePath, JSON.stringify(event) + '\n');
  }

  clearHistory(storeName) {
    if (!this.debugMode) return;

    const historyPath = path.join(this.debugPath, storeName, 'history.jsonl');
    if (fs.existsSync(historyPath)) {
      fs.writeFileSync(historyPath, '');
    }
  }

  getStoreState(storeName) {
    const store = this.stores.get(storeName);
    return store ? store.toJSON() : null;
  }

  getAllStores() {
    const result = {};
    this.stores.forEach((store, name) => {
      result[name] = store.toJSON();
    });
    return result;
  }
}

module.exports = StoreManager;