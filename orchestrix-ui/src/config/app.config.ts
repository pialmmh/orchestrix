/**
 * Centralized application configuration
 * All environment-specific settings should be defined here
 */

import { activeProfile } from './app.profile';

export interface AppConfig {
  // API Configuration
  api: {
    baseUrl: string;
    timeout: number;
  };

  // WebSocket Configuration
  websocket: {
    storeDebugUrl: string;
    reconnectInterval: number;
    messageQueueSize: number;
  };

  // Store Debug Configuration
  storeDebug: {
    enabled: boolean;
    logRetentionHours: number;
    requestTimeoutMs: number;
  };

  // Console Redirect Configuration
  consoleRedirect: {
    enabled: boolean;
    captureLog: boolean;
    captureWarn: boolean;
    captureError: boolean;
    captureInfo: boolean;
    captureDebug: boolean;
    sendToBackend: boolean;
    preserveOriginal: boolean;
    batchInterval: number; // milliseconds to batch console messages
    maxBatchSize: number; // max messages to batch
  };

  // Feature Flags
  features: {
    enableDebugMode: boolean;
    enableWebSocketLogs: boolean;
    enablePerformanceMonitoring: boolean;
  };
}

// Development configuration
const devConfig: AppConfig = {
  api: {
    baseUrl: process.env.REACT_APP_API_URL || 'http://localhost:8090/api',
    timeout: 30000,
  },
  websocket: {
    storeDebugUrl: process.env.REACT_APP_WS_URL || 'ws://localhost:3013/store-debug',
    reconnectInterval: 3000,
    messageQueueSize: 100,
  },
  storeDebug: {
    enabled: process.env.REACT_APP_STORE_DEBUG === 'true' || true,
    logRetentionHours: 24,
    requestTimeoutMs: 30000,
  },
  consoleRedirect: {
    enabled: true, // Always enabled in debug mode
    captureLog: true,
    captureWarn: true,
    captureError: true,
    captureInfo: true,
    captureDebug: false, // Disable debug to reduce noise
    sendToBackend: true,
    preserveOriginal: true,
    batchInterval: 5000, // Increased to 5 seconds to reduce frequency
    maxBatchSize: 20, // Reduced batch size to avoid payload issues
  },
  features: {
    enableDebugMode: true,
    enableWebSocketLogs: true,
    enablePerformanceMonitoring: false,
  },
};

// Staging configuration
const stagingConfig: AppConfig = {
  api: {
    baseUrl: process.env.REACT_APP_API_URL || 'http://staging.api/api',
    timeout: 30000,
  },
  websocket: {
    storeDebugUrl: process.env.REACT_APP_WS_URL || 'ws://staging.api:3013/store-debug',
    reconnectInterval: 4000,
    messageQueueSize: 75,
  },
  storeDebug: {
    enabled: process.env.REACT_APP_STORE_DEBUG === 'true' || false,
    logRetentionHours: 12,
    requestTimeoutMs: 30000,
  },
  consoleRedirect: {
    enabled: process.env.REACT_APP_CONSOLE_REDIRECT === 'true' || true, // Can be toggled
    captureLog: true,
    captureWarn: true,
    captureError: true,
    captureInfo: true,
    captureDebug: false,
    sendToBackend: true,
    preserveOriginal: true,
    batchInterval: 2000,
    maxBatchSize: 30,
  },
  features: {
    enableDebugMode: false,
    enableWebSocketLogs: true,
    enablePerformanceMonitoring: true,
  },
};

// Production configuration
const prodConfig: AppConfig = {
  api: {
    baseUrl: process.env.REACT_APP_API_URL || '/api',
    timeout: 30000,
  },
  websocket: {
    storeDebugUrl: process.env.REACT_APP_WS_URL || 'wss://api.production.com/store-debug',
    reconnectInterval: 5000,
    messageQueueSize: 50,
  },
  storeDebug: {
    enabled: false,
    logRetentionHours: 1,
    requestTimeoutMs: 30000,
  },
  consoleRedirect: {
    enabled: process.env.REACT_APP_CONSOLE_REDIRECT === 'true' || false, // Disabled by default in production
    captureLog: false,
    captureWarn: true,
    captureError: true,
    captureInfo: false,
    captureDebug: false,
    sendToBackend: true,
    preserveOriginal: true,
    batchInterval: 5000,
    maxBatchSize: 20,
  },
  features: {
    enableDebugMode: false,
    enableWebSocketLogs: false,
    enablePerformanceMonitoring: true,
  },
};

// Test configuration (kept for backward compatibility)
const testConfig: AppConfig = {
  api: {
    baseUrl: 'http://localhost:8090/api',
    timeout: 5000,
  },
  websocket: {
    storeDebugUrl: 'ws://localhost:3013/store-debug',
    reconnectInterval: 1000,
    messageQueueSize: 10,
  },
  storeDebug: {
    enabled: true,
    logRetentionHours: 1,
    requestTimeoutMs: 5000,
  },
  consoleRedirect: {
    enabled: true,
    captureLog: true,
    captureWarn: true,
    captureError: true,
    captureInfo: true,
    captureDebug: true,
    sendToBackend: false, // Don't send to backend in tests
    preserveOriginal: true,
    batchInterval: 100,
    maxBatchSize: 10,
  },
  features: {
    enableDebugMode: true,
    enableWebSocketLogs: true,
    enablePerformanceMonitoring: false,
  },
};

// Profile configurations mapping
const profileConfigs: Record<string, AppConfig> = {
  development: devConfig,
  staging: stagingConfig,
  production: prodConfig,
};

// Select configuration based on active profile
function getEnvironmentConfig(): AppConfig {
  // Use the active profile from app.profile.ts
  const config = profileConfigs[activeProfile] || devConfig;

  // Special handling for test environment
  if (process.env.NODE_ENV === 'test') {
    return testConfig;
  }

  return config;
}

// Export the configuration
export const appConfig = getEnvironmentConfig();

// Helper function to get nested config values safely
export function getConfigValue<T>(path: string, defaultValue?: T): T {
  const keys = path.split('.');
  let value: any = appConfig;

  for (const key of keys) {
    if (value && typeof value === 'object' && key in value) {
      value = value[key];
    } else {
      return defaultValue as T;
    }
  }

  return value as T;
}

// Allow runtime config overrides from localStorage (for debugging)
export function overrideConfig(overrides: Partial<AppConfig>): void {
  const currentOverrides = localStorage.getItem('appConfigOverrides');
  const existingOverrides = currentOverrides ? JSON.parse(currentOverrides) : {};
  const newOverrides = { ...existingOverrides, ...overrides };

  localStorage.setItem('appConfigOverrides', JSON.stringify(newOverrides));

  // Reload to apply changes
  window.location.reload();
}

// Apply any stored overrides
function applyStoredOverrides(config: AppConfig): AppConfig {
  const overrides = localStorage.getItem('appConfigOverrides');
  if (!overrides) return config;

  try {
    const overrideObj = JSON.parse(overrides);
    return deepMerge(config, overrideObj);
  } catch (e) {
    console.warn('Failed to apply config overrides:', e);
    return config;
  }
}

// Deep merge helper
function deepMerge(target: any, source: any): any {
  const output = { ...target };

  if (isObject(target) && isObject(source)) {
    Object.keys(source).forEach(key => {
      if (isObject(source[key])) {
        if (!(key in target)) {
          output[key] = source[key];
        } else {
          output[key] = deepMerge(target[key], source[key]);
        }
      } else {
        output[key] = source[key];
      }
    });
  }

  return output;
}

function isObject(item: any): boolean {
  return item && typeof item === 'object' && !Array.isArray(item);
}

// Apply overrides to the exported config
Object.assign(appConfig, applyStoredOverrides(appConfig));

// Log configuration in development
if (appConfig.features.enableDebugMode) {
  console.log(`🔧 App Configuration (Profile: ${activeProfile}):`, appConfig);
}