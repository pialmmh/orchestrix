import * as fs from 'fs';
import * as path from 'path';
import { toJS } from 'mobx';

interface HistoryEntry {
  timestamp: number;
  event: string;
  stateBefore: any;
  stateAfter: any;
}

export class SimpleStoreLogger {
  private baseDir: string;

  constructor(baseDir: string = './store-debug') {
    this.baseDir = baseDir;
    this.ensureBaseDirectory();
  }

  private ensureBaseDirectory(): void {
    if (!fs.existsSync(this.baseDir)) {
      fs.mkdirSync(this.baseDir, { recursive: true });
    }
  }

  private ensureStoreDirectory(storeName: string): string {
    const storeDir = path.join(this.baseDir, storeName);
    if (!fs.existsSync(storeDir)) {
      fs.mkdirSync(storeDir, { recursive: true });
    }
    return storeDir;
  }

  // Write current state to a simple JSON file
  writeCurrentState(storeName: string, state: any): void {
    const storeDir = this.ensureStoreDirectory(storeName);
    const currentFile = path.join(storeDir, 'current.json');

    const currentData = {
      timestamp: Date.now(),
      state: toJS(state)
    };

    fs.writeFileSync(currentFile, JSON.stringify(currentData, null, 2));
  }

  // Append to history file (event, before, after)
  logHistory(storeName: string, event: string, stateBefore: any, stateAfter: any): void {
    const storeDir = this.ensureStoreDirectory(storeName);
    const historyFile = path.join(storeDir, 'history.jsonl');

    const entry: HistoryEntry = {
      timestamp: Date.now(),
      event,
      stateBefore: toJS(stateBefore),
      stateAfter: toJS(stateAfter)
    };

    const line = JSON.stringify(entry) + '\n';
    fs.appendFileSync(historyFile, line);

    // Also update current state
    this.writeCurrentState(storeName, stateAfter);
  }

  // Initialize a store (write initial state)
  initializeStore(storeName: string, initialState: any): void {
    const storeDir = this.ensureStoreDirectory(storeName);

    // Write current state
    this.writeCurrentState(storeName, initialState);

    // Write first history entry
    const historyFile = path.join(storeDir, 'history.jsonl');
    const entry: HistoryEntry = {
      timestamp: Date.now(),
      event: 'initialized',
      stateBefore: null,
      stateAfter: toJS(initialState)
    };

    fs.writeFileSync(historyFile, JSON.stringify(entry) + '\n');
  }

  // Get current state
  getCurrentState(storeName: string): any {
    const storeDir = this.ensureStoreDirectory(storeName);
    const currentFile = path.join(storeDir, 'current.json');

    if (fs.existsSync(currentFile)) {
      const content = fs.readFileSync(currentFile, 'utf-8');
      return JSON.parse(content);
    }
    return null;
  }

  // Get history
  getHistory(storeName: string, limit?: number): HistoryEntry[] {
    const storeDir = this.ensureStoreDirectory(storeName);
    const historyFile = path.join(storeDir, 'history.jsonl');

    if (!fs.existsSync(historyFile)) {
      return [];
    }

    const content = fs.readFileSync(historyFile, 'utf-8');
    const lines = content.trim().split('\n').filter(line => line);

    const entries = lines.map(line => {
      try {
        return JSON.parse(line);
      } catch (e) {
        return null;
      }
    }).filter(entry => entry !== null);

    return limit ? entries.slice(-limit) : entries;
  }
}

// Singleton instance
let instance: SimpleStoreLogger | null = null;

export function getSimpleStoreLogger(): SimpleStoreLogger {
  if (!instance) {
    instance = new SimpleStoreLogger();
  }
  return instance;
}