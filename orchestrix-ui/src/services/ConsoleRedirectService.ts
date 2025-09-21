/**
 * Console Redirect Service
 * Captures console output and sends it to the backend via WebSocket
 */

import { appConfig } from '../config/app.config';
import storeWebSocketService from './StoreWebSocketService';

export interface ConsoleMessage {
  timestamp: number;
  isoTimestamp: string;
  level: 'log' | 'warn' | 'error' | 'info' | 'debug';
  message: string;
  args: any[];
  stackTrace?: string;
  source: 'frontend';
}

class ConsoleRedirectService {
  private originalConsole = {
    log: console.log,
    warn: console.warn,
    error: console.error,
    info: console.info,
    debug: console.debug,
  };

  private messageBuffer: ConsoleMessage[] = [];
  private batchTimer: NodeJS.Timeout | null = null;
  private isInitialized = false;
  private config = appConfig.consoleRedirect;

  /**
   * Initialize console redirection
   */
  initialize() {
    if (this.isInitialized || !this.config.enabled) {
      return;
    }

    console.log('[ConsoleRedirect] Initializing console redirection...');

    // Intercept console methods
    if (this.config.captureLog) {
      console.log = this.createInterceptor('log', this.originalConsole.log);
    }

    if (this.config.captureWarn) {
      console.warn = this.createInterceptor('warn', this.originalConsole.warn);
    }

    if (this.config.captureError) {
      console.error = this.createInterceptor('error', this.originalConsole.error);
    }

    if (this.config.captureInfo) {
      console.info = this.createInterceptor('info', this.originalConsole.info);
    }

    if (this.config.captureDebug) {
      console.debug = this.createInterceptor('debug', this.originalConsole.debug);
    }

    // Set up periodic batch sending
    if (this.config.sendToBackend && this.config.batchInterval > 0) {
      this.startBatchTimer();
    }

    // Send any buffered messages on page unload
    window.addEventListener('beforeunload', () => {
      this.flushMessages();
    });

    this.isInitialized = true;
    this.originalConsole.log('[ConsoleRedirect] Console redirection initialized');
  }

  /**
   * Create an interceptor for a console method
   */
  private createInterceptor(level: ConsoleMessage['level'], originalMethod: Function) {
    return (...args: any[]) => {
      // Preserve original console output if configured
      if (this.config.preserveOriginal) {
        originalMethod.apply(console, args);
      }

      // Filter out certain system/library logs to reduce noise
      const message = this.formatMessage(args);

      // Skip certain noisy logs
      if (this.shouldSkipLog(message, level)) {
        return;
      }

      // Capture the message
      const consoleMessage: ConsoleMessage = {
        timestamp: Date.now(),
        isoTimestamp: new Date().toISOString(),
        level,
        message,
        args: this.serializeArgs(args),
        source: 'frontend',
      };

      // Capture stack trace for errors
      if (level === 'error') {
        const error = args.find(arg => arg instanceof Error);
        if (error) {
          consoleMessage.stackTrace = error.stack;
        } else {
          // Generate stack trace for non-Error objects
          consoleMessage.stackTrace = new Error().stack;
        }
      }

      // Add to buffer
      this.addToBuffer(consoleMessage);
    };
  }

  /**
   * Determine if a log should be skipped based on its content
   */
  private shouldSkipLog(message: string, level: string): boolean {
    // Skip React DevTools and other framework logs
    const skipPatterns = [
      /Download the React DevTools/i,
      /React Hook useEffect has/i,
      /Each child in a list should have/i,
      /validateDOMNesting/i,
      /findDOMNode is deprecated/i,
      /\[HMR\]/i, // Hot Module Replacement logs
      /\[WDS\]/i, // Webpack Dev Server logs
      /Claude Bridge/i, // Skip Claude Bridge logs that are too frequent
      /CONSOLE_LOGS_ACK/i, // Skip acknowledgment logs
    ];

    // Only apply filters to log and debug levels, keep warnings and errors
    if (level === 'log' || level === 'debug') {
      return skipPatterns.some(pattern => pattern.test(message));
    }

    return false;
  }

  /**
   * Format console arguments into a string message
   */
  private formatMessage(args: any[]): string {
    return args.map(arg => {
      if (typeof arg === 'object') {
        try {
          return JSON.stringify(arg, null, 2);
        } catch (e) {
          return String(arg);
        }
      }
      return String(arg);
    }).join(' ');
  }

  /**
   * Serialize arguments for transmission (handle circular references)
   */
  private serializeArgs(args: any[]): any[] {
    const seen = new WeakSet();
    return args.map(arg => {
      try {
        return JSON.parse(JSON.stringify(arg, (key, value) => {
          if (typeof value === 'object' && value !== null) {
            if (seen.has(value)) {
              return '[Circular]';
            }
            seen.add(value);
          }
          return value;
        }));
      } catch (e) {
        return String(arg);
      }
    });
  }

  /**
   * Add message to buffer and trigger batch sending if needed
   */
  private addToBuffer(message: ConsoleMessage) {
    this.messageBuffer.push(message);

    // Check if buffer is full
    if (this.messageBuffer.length >= this.config.maxBatchSize) {
      this.flushMessages();
    }
  }

  /**
   * Start the batch timer
   */
  private startBatchTimer() {
    if (this.batchTimer) {
      clearInterval(this.batchTimer);
    }

    this.batchTimer = setInterval(() => {
      if (this.messageBuffer.length > 0) {
        this.flushMessages();
      }
    }, this.config.batchInterval);
  }

  /**
   * Send all buffered messages to backend
   */
  private flushMessages() {
    if (!this.config.sendToBackend || this.messageBuffer.length === 0) {
      return;
    }

    const messages = [...this.messageBuffer];
    this.messageBuffer = [];

    // Send to backend via WebSocket
    try {
      storeWebSocketService.sendConsoleLogs(messages);
    } catch (error) {
      // Use original console to avoid infinite loop
      this.originalConsole.error('[ConsoleRedirect] Failed to send logs:', error);
    }
  }

  /**
   * Restore original console methods
   */
  restore() {
    if (!this.isInitialized) {
      return;
    }

    console.log = this.originalConsole.log;
    console.warn = this.originalConsole.warn;
    console.error = this.originalConsole.error;
    console.info = this.originalConsole.info;
    console.debug = this.originalConsole.debug;

    if (this.batchTimer) {
      clearInterval(this.batchTimer);
      this.batchTimer = null;
    }

    // Flush any remaining messages
    this.flushMessages();

    this.isInitialized = false;
    this.originalConsole.log('[ConsoleRedirect] Console redirection disabled');
  }

  /**
   * Update configuration at runtime
   */
  updateConfig(config: Partial<typeof appConfig.consoleRedirect>) {
    // First restore if initialized
    if (this.isInitialized) {
      this.restore();
    }

    // Update config
    this.config = { ...this.config, ...config };

    // Reinitialize if enabled
    if (this.config.enabled) {
      this.initialize();
    }
  }

  /**
   * Get current configuration
   */
  getConfig() {
    return { ...this.config };
  }

  /**
   * Check if service is active
   */
  isActive() {
    return this.isInitialized && this.config.enabled;
  }
}

// Create singleton instance
const consoleRedirectService = new ConsoleRedirectService();

// Auto-initialize based on configuration
if (appConfig.consoleRedirect.enabled) {
  // Wait for DOM to be ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
      consoleRedirectService.initialize();
    });
  } else {
    consoleRedirectService.initialize();
  }
}

export default consoleRedirectService;