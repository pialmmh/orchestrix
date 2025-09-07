// ===============================================
// ORCHESTRIX FRONTEND CONFIGURATION SERVICE
// ===============================================
// Centralized configuration management for all environments
// Automatically loads configuration based on environment variables

interface AppConfig {
  // API Configuration
  apiUrl: string;
  wsUrl: string;
  apiTimeout: number;

  // Application Information
  name: string;
  version: string;
  environment: string;
  buildNumber: string;

  // Feature Flags
  debug: boolean;
  mockData: boolean;
  analytics: boolean;
  errorReporting: boolean;
  reduxDevTools: boolean;

  // Security Settings
  sessionTimeout: number;
  idleTimeout: number;
  maxLoginAttempts: number;

  // UI Configuration
  defaultTheme: 'light' | 'dark';
  themeSwitcher: boolean;
  defaultLanguage: string;
  paginationSize: number;
  logLevel: 'debug' | 'info' | 'warn' | 'error';

  // External URLs
  docsUrl: string;
  supportUrl: string;

  // External Services
  analyticsId?: string;
  errorReportingDsn?: string;
  cdnUrl?: string;
}

class ConfigurationService {
  private config: AppConfig;

  constructor() {
    this.config = this.loadConfiguration();
    this.validateConfiguration();
  }

  private loadConfiguration(): AppConfig {
    return {
      // API Configuration
      apiUrl: process.env.REACT_APP_API_URL || 'http://localhost:8090/api',
      wsUrl: process.env.REACT_APP_WS_URL || 'ws://localhost:8090/ws',
      apiTimeout: parseInt(process.env.REACT_APP_API_TIMEOUT || '30000'),

      // Application Information
      name: process.env.REACT_APP_NAME || 'Orchestrix',
      version: process.env.REACT_APP_VERSION || '1.0.0',
      environment: process.env.REACT_APP_ENVIRONMENT || 'development',
      buildNumber: process.env.REACT_APP_BUILD_NUMBER || 'local-build',

      // Feature Flags
      debug: process.env.REACT_APP_ENABLE_DEBUG === 'true',
      mockData: process.env.REACT_APP_ENABLE_MOCK_DATA === 'true',
      analytics: process.env.REACT_APP_ENABLE_ANALYTICS === 'true',
      errorReporting: process.env.REACT_APP_ENABLE_ERROR_REPORTING === 'true',
      reduxDevTools: process.env.REACT_APP_ENABLE_REDUX_DEVTOOLS === 'true',

      // Security Settings
      sessionTimeout: parseInt(process.env.REACT_APP_SESSION_TIMEOUT || '1800000'), // 30 minutes default
      idleTimeout: parseInt(process.env.REACT_APP_IDLE_TIMEOUT || '900000'), // 15 minutes default
      maxLoginAttempts: parseInt(process.env.REACT_APP_MAX_LOGIN_ATTEMPTS || '5'),

      // UI Configuration
      defaultTheme: (process.env.REACT_APP_DEFAULT_THEME as 'light' | 'dark') || 'light',
      themeSwitcher: process.env.REACT_APP_ENABLE_THEME_SWITCHER !== 'false',
      defaultLanguage: process.env.REACT_APP_DEFAULT_LANGUAGE || 'en',
      paginationSize: parseInt(process.env.REACT_APP_PAGINATION_SIZE || '20'),
      logLevel: (process.env.REACT_APP_LOG_LEVEL as any) || 'info',

      // External URLs
      docsUrl: process.env.REACT_APP_DOCS_URL || 'https://docs.orchestrix.com',
      supportUrl: process.env.REACT_APP_SUPPORT_URL || 'https://support.orchestrix.com',

      // External Services
      analyticsId: process.env.REACT_APP_ANALYTICS_ID,
      errorReportingDsn: process.env.REACT_APP_ERROR_REPORTING_DSN,
      cdnUrl: process.env.REACT_APP_CDN_URL,
    };
  }

  private validateConfiguration(): void {
    if (!this.config.apiUrl) {
      console.error('Configuration Error: API URL is required');
    }

    if (this.config.apiTimeout < 1000) {
      console.warn('Configuration Warning: API timeout is very low, consider increasing');
    }

    if (this.config.environment === 'production' && this.config.debug) {
      console.warn('Configuration Warning: Debug mode enabled in production');
    }

    if (this.config.environment === 'production' && this.config.reduxDevTools) {
      console.warn('Configuration Warning: Redux DevTools enabled in production');
    }
  }

  public getConfig(): AppConfig {
    return { ...this.config };
  }

  public get<K extends keyof AppConfig>(key: K): AppConfig[K] {
    return this.config[key];
  }

  public isDevelopment(): boolean {
    return this.config.environment === 'development';
  }

  public isStaging(): boolean {
    return this.config.environment === 'staging';
  }

  public isProduction(): boolean {
    return this.config.environment === 'production';
  }

  public getApiEndpoint(path: string): string {
    const baseUrl = this.config.apiUrl.replace(/\/$/, ''); // Remove trailing slash
    const cleanPath = path.startsWith('/') ? path : `/${path}`;
    return `${baseUrl}${cleanPath}`;
  }

  public log(level: 'debug' | 'info' | 'warn' | 'error', message: string, ...args: any[]): void {
    const levels = ['debug', 'info', 'warn', 'error'];
    const currentLevelIndex = levels.indexOf(this.config.logLevel);
    const messageLevelIndex = levels.indexOf(level);

    if (messageLevelIndex >= currentLevelIndex) {
      console[level](`[${this.config.name}] ${message}`, ...args);
    }
  }

  public printConfiguration(): void {
    if (this.isDevelopment()) {
      console.group('🔧 Orchestrix Configuration');
      console.log('Environment:', this.config.environment);
      console.log('Version:', this.config.version);
      console.log('Build:', this.config.buildNumber);
      console.log('API URL:', this.config.apiUrl);
      console.log('WebSocket URL:', this.config.wsUrl);
      console.log('Features:', {
        debug: this.config.debug,
        analytics: this.config.analytics,
        errorReporting: this.config.errorReporting,
        mockData: this.config.mockData
      });
      console.groupEnd();
    }
  }
}

// Export singleton instance
export const config = new ConfigurationService();

// Export default instance and types
export default config;
export type { AppConfig };