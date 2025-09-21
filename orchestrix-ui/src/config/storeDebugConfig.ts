// Store Debug Mode Configuration
// This file now wraps the centralized app.config.ts for backward compatibility

import { appConfig } from './app.config';

export interface StoreDebugConfig {
  store_debug: boolean;
  websocket_url: string;
  log_retention_hours: number;
  request_timeout_ms: number; // Timeout for EventBus requests
}

// Get configuration from centralized app config
export function getStoreDebugConfig(): StoreDebugConfig {
  return {
    store_debug: appConfig.storeDebug.enabled,
    websocket_url: appConfig.websocket.storeDebugUrl,
    log_retention_hours: appConfig.storeDebug.logRetentionHours,
    request_timeout_ms: appConfig.storeDebug.requestTimeoutMs,
  };
}

// Save configuration to localStorage (delegates to app.config.ts)
export function saveStoreDebugConfig(config: Partial<StoreDebugConfig>): void {
  const overrides: any = {};

  if (config.store_debug !== undefined) {
    overrides.storeDebug = { ...overrides.storeDebug, enabled: config.store_debug };
  }
  if (config.websocket_url !== undefined) {
    overrides.websocket = { ...overrides.websocket, storeDebugUrl: config.websocket_url };
  }
  if (config.log_retention_hours !== undefined) {
    overrides.storeDebug = { ...overrides.storeDebug, logRetentionHours: config.log_retention_hours };
  }
  if (config.request_timeout_ms !== undefined) {
    overrides.storeDebug = { ...overrides.storeDebug, requestTimeoutMs: config.request_timeout_ms };
  }

  // Use the centralized override function
  const { overrideConfig } = require('./app.config');
  overrideConfig(overrides);
}