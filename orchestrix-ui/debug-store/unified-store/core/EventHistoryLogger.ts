import * as fs from 'fs';
import * as path from 'path';
import { StoreEvent, StoreEventResponse } from '../events/StoreEvent';

interface LogEntry {
  timestamp: number;
  isoTimestamp: string;
  type: 'event' | 'mutation' | 'query' | 'store-update';
  operation?: string;
  entity?: string;
  success?: boolean;
  duration?: number;
  data: any;
  metadata?: any;
}

export class EventHistoryLogger {
  private logDir: string;
  private currentLogFile: string;
  private logStream: fs.WriteStream | null = null;
  private rotationInterval: NodeJS.Timeout | null = null;
  private maxFileSize: number = 10 * 1024 * 1024; // 10MB
  private maxFiles: number = 10;
  private currentFileSize: number = 0;

  constructor(logDir: string = './store-logs') {
    this.logDir = logDir;
    this.currentLogFile = this.getLogFileName();
    this.ensureLogDirectory();
    this.initializeLogStream();
    this.setupRotation();
  }

  private ensureLogDirectory(): void {
    if (!fs.existsSync(this.logDir)) {
      fs.mkdirSync(this.logDir, { recursive: true });
      console.log(`📁 Created log directory: ${this.logDir}`);
    }
  }

  private getLogFileName(): string {
    const now = new Date();
    const timestamp = now.toISOString().replace(/[:.]/g, '-').slice(0, -5);
    return path.join(this.logDir, `store-events-${timestamp}.jsonl`);
  }

  private initializeLogStream(): void {
    if (this.logStream) {
      this.logStream.end();
    }

    this.currentLogFile = this.getLogFileName();
    this.logStream = fs.createWriteStream(this.currentLogFile, { flags: 'a' });
    this.currentFileSize = 0;

    // Check if file already exists and get its size
    if (fs.existsSync(this.currentLogFile)) {
      const stats = fs.statSync(this.currentLogFile);
      this.currentFileSize = stats.size;
    }

    console.log(`📝 Logging events to: ${this.currentLogFile}`);
  }

  private setupRotation(): void {
    // Check for rotation every minute
    this.rotationInterval = setInterval(() => {
      this.checkRotation();
    }, 60000);
  }

  private checkRotation(): void {
    // Rotate if file size exceeds limit
    if (this.currentFileSize >= this.maxFileSize) {
      this.rotateLog();
    }

    // Also rotate daily at midnight
    const now = new Date();
    if (now.getHours() === 0 && now.getMinutes() === 0) {
      this.rotateLog();
    }
  }

  private rotateLog(): void {
    console.log('🔄 Rotating log file...');

    if (this.logStream) {
      this.logStream.end();
    }

    // Start a new log file
    this.initializeLogStream();

    // Clean up old files
    this.cleanupOldLogs();
  }


  private cleanupOldLogs(): void {
    const files = fs.readdirSync(this.logDir);
    const logFiles = files
      .filter(f => f.startsWith('store-events-') && f.endsWith('.jsonl'))
      .map(f => ({
        name: f,
        path: path.join(this.logDir, f),
        mtime: fs.statSync(path.join(this.logDir, f)).mtime.getTime()
      }))
      .sort((a, b) => b.mtime - a.mtime);

    // Keep only the most recent files
    if (logFiles.length > this.maxFiles) {
      const filesToDelete = logFiles.slice(this.maxFiles);
      filesToDelete.forEach(file => {
        fs.unlinkSync(file.path);
        console.log(`🗑️ Deleted old log: ${file.name}`);
      });
    }
  }

  logEvent(event: StoreEvent | StoreEventResponse): void {
    const logEntry: LogEntry = {
      timestamp: event.timestamp,
      isoTimestamp: new Date(event.timestamp).toISOString(),
      type: event.type as any,
      operation: event.operation,
      entity: event.entity,
      success: (event as StoreEventResponse).success,
      duration: event.metadata?.duration,
      data: event.payload,
      metadata: event.metadata
    };

    this.writeLog(logEntry);
  }

  logMutation(mutation: any): void {
    const logEntry: LogEntry = {
      timestamp: Date.now(),
      isoTimestamp: new Date().toISOString(),
      type: 'mutation',
      operation: mutation.operation || 'MUTATION',
      entity: mutation.entity,
      success: mutation.success,
      data: mutation,
      metadata: mutation.metadata
    };

    this.writeLog(logEntry);
  }

  logStoreUpdate(storeName: string, data: any): void {
    const logEntry: LogEntry = {
      timestamp: Date.now(),
      isoTimestamp: new Date().toISOString(),
      type: 'store-update',
      entity: storeName,
      data: data,
      metadata: {
        dataSize: JSON.stringify(data).length,
        keys: Object.keys(data || {})
      }
    };

    this.writeLog(logEntry);

    // Also write current store state to a separate always-current file
    this.writeCurrentStoreState(storeName, data);
  }

  private writeCurrentStoreState(storeName: string, data: any): void {
    const stateFile = path.join(this.logDir, `current-state-${storeName}.json`);
    const stateData = {
      timestamp: Date.now(),
      isoTimestamp: new Date().toISOString(),
      storeName,
      currentState: data,
      metadata: {
        lastUpdated: new Date().toISOString(),
        dataSize: JSON.stringify(data).length,
        keys: Object.keys(data || {})
      }
    };

    try {
      fs.writeFileSync(stateFile, JSON.stringify(stateData, null, 2));
    } catch (error) {
      console.error(`Failed to write current state for ${storeName}:`, error);
    }
  }

  private writeLog(entry: LogEntry): void {
    if (!this.logStream) {
      console.error('Log stream not initialized');
      return;
    }

    const line = JSON.stringify(entry) + '\n';
    const byteLength = Buffer.byteLength(line);

    this.logStream.write(line);
    this.currentFileSize += byteLength;

    // Check if rotation is needed after write
    if (this.currentFileSize >= this.maxFileSize) {
      this.rotateLog();
    }
  }

  // Get recent events for debugging
  async getRecentEvents(limit: number = 100): Promise<LogEntry[]> {
    const currentFile = this.currentLogFile;
    if (!fs.existsSync(currentFile)) {
      return [];
    }

    const content = fs.readFileSync(currentFile, 'utf-8');
    const lines = content.trim().split('\n').filter(line => line);
    const recentLines = lines.slice(-limit);

    return recentLines.map(line => {
      try {
        return JSON.parse(line);
      } catch (e) {
        console.error('Failed to parse log line:', e);
        return null;
      }
    }).filter(entry => entry !== null) as LogEntry[];
  }

  // Get events within a time range
  async getEventsByTimeRange(startTime: number, endTime: number): Promise<LogEntry[]> {
    const events: LogEntry[] = [];
    const files = fs.readdirSync(this.logDir)
      .filter(f => f.startsWith('store-events-') && f.endsWith('.jsonl'))
      .sort();

    for (const file of files) {
      const filePath = path.join(this.logDir, file);
      const content = fs.readFileSync(filePath, 'utf-8');
      const lines = content.trim().split('\n').filter(line => line);

      for (const line of lines) {
        try {
          const entry = JSON.parse(line) as LogEntry;
          if (entry.timestamp >= startTime && entry.timestamp <= endTime) {
            events.push(entry);
          }
        } catch (e) {
          console.error('Failed to parse log line:', e);
        }
      }
    }

    return events.sort((a, b) => a.timestamp - b.timestamp);
  }

  // Write consolidated view with store state and recent history
  async writeConsolidatedView(): Promise<void> {
    try {
      // Get recent events
      const recentEvents = await this.getRecentEvents(100);

      // Get current store states
      const storeStates: Record<string, any> = {};
      const stateFiles = fs.readdirSync(this.logDir)
        .filter(f => f.startsWith('current-state-') && f.endsWith('.json'));

      for (const file of stateFiles) {
        const content = fs.readFileSync(path.join(this.logDir, file), 'utf-8');
        const state = JSON.parse(content);
        const storeName = file.replace('current-state-', '').replace('.json', '');
        storeStates[storeName] = state;
      }

      // Create consolidated view
      const consolidatedView = {
        timestamp: Date.now(),
        isoTimestamp: new Date().toISOString(),
        currentStoreStates: storeStates,
        recentEventHistory: recentEvents.filter(e => e.type === 'event' || e.type === 'mutation' || e.type === 'query'),
        statistics: {
          totalEvents: recentEvents.length,
          eventsByType: recentEvents.reduce((acc, e) => {
            acc[e.type] = (acc[e.type] || 0) + 1;
            return acc;
          }, {} as Record<string, number>),
          lastMutation: recentEvents.filter(e => e.type === 'mutation').slice(-1)[0],
          lastQuery: recentEvents.filter(e => e.type === 'query').slice(-1)[0]
        }
      };

      // Write consolidated view
      const viewFile = path.join(this.logDir, 'consolidated-view.json');
      fs.writeFileSync(viewFile, JSON.stringify(consolidatedView, null, 2));

      console.log('📊 Consolidated view updated');
    } catch (error) {
      console.error('Failed to write consolidated view:', error);
    }
  }

  // Create a summary report
  async generateSummaryReport(): Promise<any> {
    const recent = await this.getRecentEvents(1000);

    const summary = {
      totalEvents: recent.length,
      timeRange: {
        start: recent[0]?.isoTimestamp,
        end: recent[recent.length - 1]?.isoTimestamp
      },
      byType: {} as Record<string, number>,
      byEntity: {} as Record<string, number>,
      byOperation: {} as Record<string, number>,
      successRate: 0,
      averageDuration: 0
    };

    let successCount = 0;
    let totalDuration = 0;
    let durationCount = 0;

    for (const event of recent) {
      // Count by type
      summary.byType[event.type] = (summary.byType[event.type] || 0) + 1;

      // Count by entity
      if (event.entity) {
        summary.byEntity[event.entity] = (summary.byEntity[event.entity] || 0) + 1;
      }

      // Count by operation
      if (event.operation) {
        summary.byOperation[event.operation] = (summary.byOperation[event.operation] || 0) + 1;
      }

      // Calculate success rate
      if (event.success !== undefined) {
        if (event.success) successCount++;
      }

      // Calculate average duration
      if (event.duration) {
        totalDuration += event.duration;
        durationCount++;
      }
    }

    summary.successRate = recent.length > 0 ? (successCount / recent.length) * 100 : 0;
    summary.averageDuration = durationCount > 0 ? totalDuration / durationCount : 0;

    return summary;
  }

  destroy(): void {
    if (this.rotationInterval) {
      clearInterval(this.rotationInterval);
    }
    if (this.logStream) {
      this.logStream.end();
    }
  }
}

// Singleton instance
let loggerInstance: EventHistoryLogger | null = null;

export function getEventHistoryLogger(): EventHistoryLogger {
  if (!loggerInstance) {
    loggerInstance = new EventHistoryLogger();
  }
  return loggerInstance;
}