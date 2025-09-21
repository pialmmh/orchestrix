import { TelcobrightFramework } from 'stellar-crud-base';
import { appConfig } from './app.config';

/**
 * Initialize Telcobright Admin-UI Framework (stellar-crud-base)
 */
export function initializeFramework() {
  const framework = TelcobrightFramework.init({
    // Store location based on debug mode
    storeLocation: appConfig.storeDebug.enabled ? 'backend' : 'browser',

    // Stellar configuration
    stellar: {
      endpoint: appConfig.api.baseUrl + '/stellar',
      timeout: appConfig.storeDebug.requestTimeoutMs,
      retryAttempts: 3,
      retryDelay: 1000
    },

    // WebSocket configuration for backend stores
    websocket: {
      url: appConfig.websocket.storeDebugUrl || 'ws://localhost:3013/store-events',
      reconnect: true,
      reconnectInterval: 3000,
      messageQueueSize: 100,
      heartbeatInterval: 30000
    },

    // EventBus configuration
    eventBus: {
      maxListeners: 100,
      enableLogging: appConfig.storeDebug.enabled,
      logLevel: 'debug'
    },

    // Debug configuration
    debug: {
      enabled: appConfig.storeDebug.enabled,
      logToFile: true,
      logPath: './debug-logs',
      includeStackTrace: true
    },

    // Logging configuration
    logging: {
      wsLog: {
        enabled: appConfig.consoleRedirect?.enabled || false,
        sendToBackend: appConfig.consoleRedirect?.sendToBackend || false,
        includeTimestamp: true,
        includeSessionId: true
      }
    }
  });

  return framework;
}

// Export the framework instance
export const framework = initializeFramework();