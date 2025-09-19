import * as fs from 'fs';
import * as path from 'path';
import { toJS } from 'mobx';

interface HistoryEntry {
  timestamp: number;
  isoTimestamp: string;
  eventType: 'initial' | 'mutation' | 'query' | 'update' | 'action';
  eventDescription: string;
  stateBefore: any;
  stateAfter?: any;
  metadata?: any;
}

export class StoreHistoryLogger {
  private historyDir: string;
  private streams: Map<string, fs.WriteStream> = new Map();

  constructor(historyDir: string = './store-logs/history') {
    this.historyDir = historyDir;
    this.ensureHistoryDirectory();
  }

  private ensureHistoryDirectory(): void {
    if (!fs.existsSync(this.historyDir)) {
      fs.mkdirSync(this.historyDir, { recursive: true });
      console.log(`📁 Created history directory: ${this.historyDir}`);
    }
  }

  private getStream(storeName: string): fs.WriteStream {
    if (!this.streams.has(storeName)) {
      const fileName = path.join(this.historyDir, `${storeName}-chronicle.jsonl`);
      const stream = fs.createWriteStream(fileName, { flags: 'a' });
      this.streams.set(storeName, stream);
      console.log(`📜 Created chronicle log for store: ${storeName}`);
    }
    return this.streams.get(storeName)!;
  }

  logInitialState(storeName: string, initialState: any): void {
    const entry: HistoryEntry = {
      timestamp: Date.now(),
      isoTimestamp: new Date().toISOString(),
      eventType: 'initial',
      eventDescription: 'Store initialized',
      stateBefore: toJS(initialState),
      metadata: {
        storeSize: JSON.stringify(initialState).length,
        keys: Object.keys(initialState || {})
      }
    };

    this.writeEntry(storeName, entry);
  }

  logBeforeEvent(storeName: string, eventType: string, eventDescription: string, currentState: any, metadata?: any): string {
    const entryId = `${storeName}-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;

    const entry: HistoryEntry = {
      timestamp: Date.now(),
      isoTimestamp: new Date().toISOString(),
      eventType: eventType as any,
      eventDescription,
      stateBefore: toJS(currentState),
      metadata: {
        ...metadata,
        entryId,
        storeSize: JSON.stringify(currentState).length
      }
    };

    this.writeEntry(storeName, entry);
    return entryId;
  }

  logAfterEvent(storeName: string, entryId: string, newState: any, metadata?: any): void {
    const entry: HistoryEntry = {
      timestamp: Date.now(),
      isoTimestamp: new Date().toISOString(),
      eventType: 'update',
      eventDescription: `State after event ${entryId}`,
      stateBefore: null, // This is a continuation entry
      stateAfter: toJS(newState),
      metadata: {
        ...metadata,
        entryId,
        isAfterEvent: true,
        storeSize: JSON.stringify(newState).length
      }
    };

    this.writeEntry(storeName, entry);
  }

  logEvent(storeName: string, eventType: string, eventDescription: string, beforeState: any, afterState: any, metadata?: any): void {
    // Log before state
    const entryBefore: HistoryEntry = {
      timestamp: Date.now(),
      isoTimestamp: new Date().toISOString(),
      eventType: eventType as any,
      eventDescription,
      stateBefore: toJS(beforeState),
      metadata: {
        ...metadata,
        phase: 'before',
        storeSize: JSON.stringify(beforeState).length
      }
    };

    this.writeEntry(storeName, entryBefore);

    // Log after state
    const entryAfter: HistoryEntry = {
      timestamp: Date.now(),
      isoTimestamp: new Date().toISOString(),
      eventType: 'update',
      eventDescription: `State after: ${eventDescription}`,
      stateBefore: null,
      stateAfter: toJS(afterState),
      metadata: {
        ...metadata,
        phase: 'after',
        storeSize: JSON.stringify(afterState).length,
        changes: this.detectChanges(beforeState, afterState)
      }
    };

    this.writeEntry(storeName, entryAfter);
  }

  private detectChanges(before: any, after: any): any {
    const changes: any = {
      added: [],
      modified: [],
      removed: []
    };

    // Check for added or modified keys
    for (const key in after) {
      if (!(key in before)) {
        changes.added.push(key);
      } else if (JSON.stringify(before[key]) !== JSON.stringify(after[key])) {
        changes.modified.push(key);
      }
    }

    // Check for removed keys
    for (const key in before) {
      if (!(key in after)) {
        changes.removed.push(key);
      }
    }

    return changes;
  }

  private writeEntry(storeName: string, entry: HistoryEntry): void {
    const stream = this.getStream(storeName);
    const line = JSON.stringify(entry) + '\n';
    stream.write(line);
  }

  // Get recent history for a store
  async getStoreHistory(storeName: string, limit: number = 100): Promise<HistoryEntry[]> {
    const fileName = path.join(this.historyDir, `${storeName}-chronicle.jsonl`);

    if (!fs.existsSync(fileName)) {
      return [];
    }

    const content = fs.readFileSync(fileName, 'utf-8');
    const lines = content.trim().split('\n').filter(line => line);
    const recentLines = lines.slice(-limit);

    return recentLines.map(line => {
      try {
        return JSON.parse(line);
      } catch (e) {
        console.error('Failed to parse history line:', e);
        return null;
      }
    }).filter(entry => entry !== null) as HistoryEntry[];
  }

  // Get a readable summary of recent changes
  async getStoreSummary(storeName: string): Promise<any> {
    const history = await this.getStoreHistory(storeName, 50);

    const summary = {
      storeName,
      totalEvents: history.length,
      firstEvent: history[0],
      lastEvent: history[history.length - 1],
      eventTypes: {} as Record<string, number>,
      recentChanges: history.slice(-10).map(h => ({
        time: h.isoTimestamp,
        event: h.eventDescription,
        type: h.eventType
      }))
    };

    for (const entry of history) {
      summary.eventTypes[entry.eventType] = (summary.eventTypes[entry.eventType] || 0) + 1;
    }

    return summary;
  }

  destroy(): void {
    for (const [storeName, stream] of this.streams) {
      stream.end();
      console.log(`📕 Closed chronicle log for store: ${storeName}`);
    }
    this.streams.clear();
  }
}

// Singleton instance
let historyLoggerInstance: StoreHistoryLogger | null = null;

export function getStoreHistoryLogger(): StoreHistoryLogger {
  if (!historyLoggerInstance) {
    historyLoggerInstance = new StoreHistoryLogger();
  }
  return historyLoggerInstance;
}

export function resetStoreHistoryLogger(): void {
  if (historyLoggerInstance) {
    historyLoggerInstance.destroy();
    historyLoggerInstance = null;
  }
}