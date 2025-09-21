/**
 * WebSocket Logger Utility
 * Provides a global ws.log() method that:
 * 1. Logs to console.log
 * 2. Sends to WebSocket debug backend if connected
 */

import { appConfig } from '../config/app.config';

class WSLogger {
  constructor() {
    this.ws = null;
    this.connected = false;
    this.init();
  }

  init() {
    // Only initialize WebSocket if debug mode is enabled
    if (!appConfig.storeDebug?.enabled) {
      return;
    }

    const wsUrl = appConfig.websocket?.storeDebugUrl || 'ws://localhost:3013/store-debug';

    try {
      this.ws = new WebSocket(wsUrl);

      this.ws.onopen = () => {
        this.connected = true;
        console.log('[WSLogger] Connected to debug WebSocket');
      };

      this.ws.onclose = () => {
        this.connected = false;
        console.log('[WSLogger] Disconnected from debug WebSocket');
      };

      this.ws.onerror = (error) => {
        console.error('[WSLogger] WebSocket error:', error);
        this.connected = false;
      };
    } catch (error) {
      console.error('[WSLogger] Failed to initialize WebSocket:', error);
    }
  }

  /**
   * Log to console and send to WebSocket if connected
   * @param {...any} args - Values to log
   */
  log(...args) {
    // Always log to console
    console.log(...args);

    // Send to WebSocket if connected and debug mode is enabled
    if (this.connected && this.ws && this.ws.readyState === WebSocket.OPEN) {
      try {
        const logMessage = {
          type: 'console',
          level: 'log',
          timestamp: Date.now(),
          isoTimestamp: new Date().toISOString(),
          message: args.map(arg => {
            if (typeof arg === 'object') {
              try {
                return JSON.stringify(arg);
              } catch {
                return String(arg);
              }
            }
            return String(arg);
          }).join(' '),
          args: args,
          source: 'frontend',
          sessionId: this.getSessionId()
        };

        this.ws.send(JSON.stringify(logMessage));
      } catch (error) {
        // Silently fail - don't want to disrupt normal logging
        console.error('[WSLogger] Failed to send log to WebSocket:', error);
      }
    }
  }

  getSessionId() {
    // Get or create session ID
    if (!this.sessionId) {
      this.sessionId = sessionStorage.getItem('ws-session-id');
      if (!this.sessionId) {
        this.sessionId = `session-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
        sessionStorage.setItem('ws-session-id', this.sessionId);
      }
    }
    return this.sessionId;
  }
}

// Create singleton instance
const wsLogger = new WSLogger();

// Create global ws object with log method
window.ws = {
  log: (...args) => wsLogger.log(...args)
};

// Export for use in modules if needed
export default wsLogger;
export const wsLog = (...args) => wsLogger.log(...args);