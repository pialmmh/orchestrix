/**
 * Stellar Framework - Error Handling Module
 *
 * Comprehensive error handling system for React applications
 * Captures and logs all errors through WebSocket to debug store
 */

export { default as StellarErrorBoundary } from './StellarErrorBoundary';
export {
  initializeGlobalErrorHandlers,
  logError,
  logWarning,
  logInfo
} from './StellarErrorHandler';

// Re-export for backward compatibility
export { default as GlobalErrorBoundary } from './StellarErrorBoundary';
export { initializeGlobalErrorHandlers as initializeErrorHandlers } from './StellarErrorHandler';