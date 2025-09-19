const fs = require('fs');
const path = require('path');
const { format } = require('date-fns');
const zlib = require('zlib');
const { pipeline } = require('stream/promises');

class HistoryLogger {
  constructor(config = {}) {
    this.logDir = config.logDir || path.join(__dirname, 'logs');
    this.archiveDir = path.join(this.logDir, 'archive');
    this.maxFileSize = config.maxFileSize || 10 * 1024 * 1024; // 10MB default
    this.maxFiles = config.maxFiles || 30; // Keep 30 files
    this.currentFile = null;
    this.writeStream = null;
    this.eventCount = 0;

    this.ensureDirectories();
    this.openLogFile();
  }

  ensureDirectories() {
    // Create log directory
    if (!fs.existsSync(this.logDir)) {
      fs.mkdirSync(this.logDir, { recursive: true });
    }

    // Create archive directory
    if (!fs.existsSync(this.archiveDir)) {
      fs.mkdirSync(this.archiveDir, { recursive: true });
    }
  }

  openLogFile() {
    const timestamp = format(new Date(), 'yyyy-MM-dd-HH');
    const filename = `store-history-${timestamp}.jsonl`;
    this.currentFile = path.join(this.logDir, filename);

    // Close existing stream if any
    if (this.writeStream) {
      this.writeStream.end();
    }

    // Open new write stream
    this.writeStream = fs.createWriteStream(this.currentFile, { flags: 'a' });
    console.log(`[HistoryLogger] Opened log file: ${filename}`);
  }

  async logEvent(event) {
    const logEntry = {
      timestamp: Date.now(),
      sequence: ++this.eventCount,
      event: event.type || event.operation,
      sessionId: event.sessionId,
      data: event,
      snapshot: event.stateSnapshot || null,
    };

    // Write to file (JSONL format - one JSON object per line)
    const line = JSON.stringify(logEntry) + '\n';
    this.writeStream.write(line);

    // Check if rotation is needed
    if (await this.shouldRotate()) {
      await this.rotate();
    }

    return logEntry;
  }

  async shouldRotate() {
    try {
      const stats = fs.statSync(this.currentFile);
      return stats.size > this.maxFileSize;
    } catch {
      return false;
    }
  }

  async rotate() {
    console.log('[HistoryLogger] Rotating log file...');

    // Close current write stream
    if (this.writeStream) {
      this.writeStream.end();
    }

    // Generate archive filename
    const timestamp = format(new Date(), 'yyyy-MM-dd-HH-mm-ss');
    const archiveName = `store-history-${timestamp}.jsonl.gz`;
    const archivePath = path.join(this.archiveDir, archiveName);

    // Compress and archive
    try {
      const source = fs.createReadStream(this.currentFile);
      const destination = fs.createWriteStream(archivePath);
      const gzip = zlib.createGzip({ level: 9 });

      await pipeline(source, gzip, destination);
      console.log(`[HistoryLogger] Archived to: ${archiveName}`);

      // Delete original file
      fs.unlinkSync(this.currentFile);
    } catch (error) {
      console.error('[HistoryLogger] Archive failed:', error);
    }

    // Open new log file
    this.openLogFile();

    // Clean up old archives
    await this.cleanupOldFiles();
  }

  async cleanupOldFiles() {
    try {
      const files = fs.readdirSync(this.archiveDir)
        .filter(f => f.startsWith('store-history-') && f.endsWith('.jsonl.gz'))
        .map(f => ({
          name: f,
          path: path.join(this.archiveDir, f),
          time: fs.statSync(path.join(this.archiveDir, f)).mtime.getTime()
        }))
        .sort((a, b) => b.time - a.time); // Sort by newest first

      // Keep only maxFiles
      if (files.length > this.maxFiles) {
        const toDelete = files.slice(this.maxFiles);
        for (const file of toDelete) {
          fs.unlinkSync(file.path);
          console.log(`[HistoryLogger] Deleted old archive: ${file.name}`);
        }
      }
    } catch (error) {
      console.error('[HistoryLogger] Cleanup failed:', error);
    }
  }

  async getRecentEvents(count = 100) {
    const events = [];

    try {
      // Read current file
      const content = fs.readFileSync(this.currentFile, 'utf-8');
      const lines = content.trim().split('\n');

      // Parse last N lines
      const startIdx = Math.max(0, lines.length - count);
      for (let i = startIdx; i < lines.length; i++) {
        if (lines[i]) {
          try {
            events.push(JSON.parse(lines[i]));
          } catch (e) {
            // Skip invalid lines
          }
        }
      }
    } catch (error) {
      console.error('[HistoryLogger] Failed to read recent events:', error);
    }

    return events;
  }

  async searchEvents(criteria) {
    const results = [];
    const {
      startTime,
      endTime,
      eventType,
      sessionId,
      limit = 1000
    } = criteria;

    // Search in current file
    await this.searchFile(this.currentFile, results, criteria, limit);

    // If we need more results, search archives
    if (results.length < limit) {
      const archives = fs.readdirSync(this.archiveDir)
        .filter(f => f.endsWith('.jsonl.gz'))
        .sort()
        .reverse();

      for (const archive of archives) {
        if (results.length >= limit) break;

        const archivePath = path.join(this.archiveDir, archive);
        await this.searchArchive(archivePath, results, criteria, limit);
      }
    }

    return results.slice(0, limit);
  }

  async searchFile(filepath, results, criteria, limit) {
    try {
      const content = fs.readFileSync(filepath, 'utf-8');
      const lines = content.trim().split('\n');

      for (const line of lines) {
        if (results.length >= limit) break;
        if (!line) continue;

        try {
          const entry = JSON.parse(line);

          if (this.matchesCriteria(entry, criteria)) {
            results.push(entry);
          }
        } catch (e) {
          // Skip invalid lines
        }
      }
    } catch (error) {
      console.error(`[HistoryLogger] Failed to search file ${filepath}:`, error);
    }
  }

  async searchArchive(archivePath, results, criteria, limit) {
    try {
      // Decompress archive to temp file
      const tempFile = archivePath + '.tmp';
      const source = fs.createReadStream(archivePath);
      const destination = fs.createWriteStream(tempFile);
      const gunzip = zlib.createGunzip();

      await pipeline(source, gunzip, destination);

      // Search temp file
      await this.searchFile(tempFile, results, criteria, limit);

      // Clean up temp file
      fs.unlinkSync(tempFile);
    } catch (error) {
      console.error(`[HistoryLogger] Failed to search archive ${archivePath}:`, error);
    }
  }

  matchesCriteria(entry, criteria) {
    const { startTime, endTime, eventType, sessionId } = criteria;

    if (startTime && entry.timestamp < startTime) return false;
    if (endTime && entry.timestamp > endTime) return false;
    if (eventType && entry.event !== eventType) return false;
    if (sessionId && entry.sessionId !== sessionId) return false;

    return true;
  }

  getStats() {
    const stats = {
      currentFile: path.basename(this.currentFile),
      currentSize: 0,
      totalEvents: this.eventCount,
      archives: []
    };

    try {
      // Current file size
      const fileStats = fs.statSync(this.currentFile);
      stats.currentSize = fileStats.size;

      // Archive info
      const archives = fs.readdirSync(this.archiveDir);
      stats.archives = archives.map(f => {
        const archiveStats = fs.statSync(path.join(this.archiveDir, f));
        return {
          name: f,
          size: archiveStats.size,
          modified: archiveStats.mtime
        };
      });
    } catch (error) {
      console.error('[HistoryLogger] Failed to get stats:', error);
    }

    return stats;
  }

  close() {
    if (this.writeStream) {
      this.writeStream.end();
      this.writeStream = null;
    }
  }
}

module.exports = HistoryLogger;