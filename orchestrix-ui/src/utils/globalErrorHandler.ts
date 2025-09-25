import storeWebSocketService from '../services/StoreWebSocketService';

/**
 * Global error handler for unhandled errors and promise rejections
 * Logs all errors through WebSocket to the debug store
 */

interface ErrorLogData {
  level: string;
  message: string;
  timestamp: string;
  metadata: {
    source: string;
    stack?: string;
    filename?: string;
    lineno?: number;
    colno?: number;
    userAgent: string;
    url: string;
    type: string;
    [key: string]: any;
  };
}

/**
 * Send error log through WebSocket
 */
function logErrorToWebSocket(errorLog: ErrorLogData) {
  if (storeWebSocketService && storeWebSocketService.isConnected) {
    storeWebSocketService.sendConsoleLogs([errorLog]);
  }

  // Also log to console for debugging
  console.error('[GlobalErrorHandler]', errorLog);
}

/**
 * Initialize global error handlers
 */
export function initializeGlobalErrorHandlers() {
  // Handle unhandled errors
  window.addEventListener('error', (event: ErrorEvent) => {
    const errorLog: ErrorLogData = {
      level: 'ERROR',
      message: `[Window Error] ${event.message}`,
      timestamp: new Date().toISOString(),
      metadata: {
        source: 'window.onerror',
        stack: event.error?.stack,
        filename: event.filename,
        lineno: event.lineno,
        colno: event.colno,
        userAgent: navigator.userAgent,
        url: window.location.href,
        type: 'unhandled-error',
        error: event.error ? {
          name: event.error.name,
          message: event.error.message,
          stack: event.error.stack
        } : null
      }
    };

    logErrorToWebSocket(errorLog);

    // Prevent default browser error handling in production
    if (process.env.NODE_ENV === 'production') {
      event.preventDefault();
    }
  });

  // Handle unhandled promise rejections
  window.addEventListener('unhandledrejection', (event: PromiseRejectionEvent) => {
    const errorLog: ErrorLogData = {
      level: 'ERROR',
      message: `[Unhandled Promise Rejection] ${event.reason}`,
      timestamp: new Date().toISOString(),
      metadata: {
        source: 'unhandledrejection',
        stack: event.reason?.stack,
        userAgent: navigator.userAgent,
        url: window.location.href,
        type: 'unhandled-promise-rejection',
        reason: event.reason,
        promise: String(event.promise)
      }
    };

    logErrorToWebSocket(errorLog);

    // Prevent default browser handling in production
    if (process.env.NODE_ENV === 'production') {
      event.preventDefault();
    }
  });

  // Override console.error to also send to WebSocket
  const originalConsoleError = console.error;
  console.error = function(...args: any[]) {
    // Call original console.error
    originalConsoleError.apply(console, args);

    // Send to WebSocket
    const errorLog: ErrorLogData = {
      level: 'ERROR',
      message: `[Console Error] ${args.map(arg =>
        typeof arg === 'object' ? JSON.stringify(arg) : String(arg)
      ).join(' ')}`,
      timestamp: new Date().toISOString(),
      metadata: {
        source: 'console.error',
        userAgent: navigator.userAgent,
        url: window.location.href,
        type: 'console-error',
        arguments: args.map(arg => {
          if (arg instanceof Error) {
            return {
              name: arg.name,
              message: arg.message,
              stack: arg.stack
            };
          }
          return arg;
        })
      }
    };

    logErrorToWebSocket(errorLog);
  };

  // Log initialization
  console.log('[GlobalErrorHandler] Initialized global error handlers');

  // Send initialization log through WebSocket
  logErrorToWebSocket({
    level: 'INFO',
    message: '[GlobalErrorHandler] Initialized global error handlers',
    timestamp: new Date().toISOString(),
    metadata: {
      source: 'initialization',
      userAgent: navigator.userAgent,
      url: window.location.href,
      type: 'handler-init'
    }
  });
}

/**
 * Manual error logging function
 */
export function logError(source: string, error: Error | string, additionalData?: any) {
  const errorLog: ErrorLogData = {
    level: 'ERROR',
    message: `[${source}] ${error instanceof Error ? error.message : error}`,
    timestamp: new Date().toISOString(),
    metadata: {
      source,
      stack: error instanceof Error ? error.stack : undefined,
      userAgent: navigator.userAgent,
      url: window.location.href,
      type: 'manual-log',
      additionalData
    }
  };

  logErrorToWebSocket(errorLog);
}

/**
 * Log warnings through WebSocket
 */
export function logWarning(source: string, message: string, additionalData?: any) {
  const warningLog: ErrorLogData = {
    level: 'WARN',
    message: `[${source}] ${message}`,
    timestamp: new Date().toISOString(),
    metadata: {
      source,
      userAgent: navigator.userAgent,
      url: window.location.href,
      type: 'warning',
      additionalData
    }
  };

  logErrorToWebSocket(warningLog);
}

/**
 * Log info through WebSocket
 */
export function logInfo(source: string, message: string, additionalData?: any) {
  const infoLog: ErrorLogData = {
    level: 'INFO',
    message: `[${source}] ${message}`,
    timestamp: new Date().toISOString(),
    metadata: {
      source,
      userAgent: navigator.userAgent,
      url: window.location.href,
      type: 'info',
      additionalData
    }
  };

  logErrorToWebSocket(infoLog);
}