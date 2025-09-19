// Store Debug Mode Configuration
export interface StoreDebugConfig {
  store_debug: boolean;
  websocket_url: string;
  log_retention_hours: number;
  request_timeout_ms: number; // Timeout for EventBus requests
}

// Default configuration
export const defaultStoreDebugConfig: StoreDebugConfig = {
  store_debug: true, // Debug mode enabled for testing
  websocket_url: 'ws://localhost:8081',
  log_retention_hours: 24, // Keep logs for 1 day
  request_timeout_ms: 30000, // 30 seconds default timeout
};

// Get configuration from environment or localStorage
export function getStoreDebugConfig(): StoreDebugConfig {
  // Check localStorage first for runtime changes
  const savedConfig = localStorage.getItem('storeDebugConfig');
  if (savedConfig) {
    try {
      return { ...defaultStoreDebugConfig, ...JSON.parse(savedConfig) };
    } catch (e) {
      console.warn('Invalid store debug config in localStorage', e);
    }
  }

  // Check environment variables
  const envConfig: Partial<StoreDebugConfig> = {};
  if (process.env.REACT_APP_STORE_DEBUG !== undefined) {
    envConfig.store_debug = process.env.REACT_APP_STORE_DEBUG === 'true';
  }
  if (process.env.REACT_APP_STORE_DEBUG_WS_URL) {
    envConfig.websocket_url = process.env.REACT_APP_STORE_DEBUG_WS_URL;
  }

  return { ...defaultStoreDebugConfig, ...envConfig };
}

// Save configuration to localStorage
export function saveStoreDebugConfig(config: Partial<StoreDebugConfig>): void {
  const currentConfig = getStoreDebugConfig();
  const newConfig = { ...currentConfig, ...config };
  localStorage.setItem('storeDebugConfig', JSON.stringify(newConfig));
  
  // Reload page to apply new configuration
  if (config.store_debug !== currentConfig.store_debug) {
    window.location.reload();
  }
}