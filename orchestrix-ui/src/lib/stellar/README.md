# Stellar Framework

A comprehensive event-driven framework for building scalable React applications with real-time data synchronization, advanced error handling, and WebSocket-based communication.

## Overview

Stellar is a modern application framework that provides:
- **Event-driven architecture** with WebSocket communication
- **Real-time data synchronization** between frontend and backend
- **Comprehensive error handling** with automatic logging
- **Query and mutation system** for data operations
- **Store management** with MobX integration
- **Debug tooling** for development

## Core Modules

### 1. Store Management (`/stores`)
Base store classes and patterns for state management using MobX.

```typescript
import { StellarStore } from '@stellar/stores';

class MyStore extends StellarStore {
  // Your store implementation
}
```

### 2. Error Handling (`/error-handling`)
Comprehensive error capture and logging system.

```typescript
import { StellarErrorBoundary, initializeGlobalErrorHandlers } from '@stellar/error-handling';

// Initialize at app startup
initializeGlobalErrorHandlers();

// Wrap your app
<StellarErrorBoundary>
  <App />
</StellarErrorBoundary>
```

### 3. Query System (`/models`)
Data query and mutation models.

```typescript
import { QueryNode, MutationRequest } from '@stellar/models';

const query: QueryNode = {
  kind: 'partner',
  criteria: { name: 'example' },
  page: { limit: 10, offset: 0 }
};
```

### 4. WebSocket Service (`/services`)
Real-time communication with backend.

```typescript
import { StoreWebSocketService } from '@stellar/services';

const wsService = new StoreWebSocketService();
wsService.connect();
```

## Features

### Global Error Handling

The framework automatically captures and logs:
- React component errors
- Unhandled JavaScript errors
- Promise rejections
- Console errors

All errors are sent through WebSocket to the debug store for analysis.

#### Error Boundary
Provides a user-friendly error page when React components fail:

```typescript
<StellarErrorBoundary>
  <YourComponent />
</StellarErrorBoundary>
```

#### Manual Error Logging
Log errors programmatically:

```typescript
import { logError, logWarning, logInfo } from '@stellar/error-handling';

// Log an error
logError('MyComponent', new Error('Something went wrong'));

// Log a warning
logWarning('MyService', 'Deprecated API usage');

// Log info
logInfo('System', 'Application started');
```

### WebSocket Communication

Real-time bidirectional communication with automatic:
- Connection management
- Reconnection on failure
- Message queuing
- Request/response handling

### Query & Mutation System

Declarative data fetching and modification:

```typescript
// Query data
const partners = await store.executeQuery({
  kind: 'partner',
  criteria: { type: 'cloud-provider' },
  include: [{ kind: 'cloud' }]
});

// Mutate data
await store.executeMutation({
  operation: 'CREATE',
  entity: 'compute',
  data: { name: 'vm-001', cpu: 4, memory: 16 }
});
```

### Debug Store

Development-time store for:
- Console log capture
- Error tracking
- Query/mutation history
- Performance metrics

## Installation

The Stellar framework is included in the Orchestrix UI project.

```bash
# Import from the framework
import { StellarErrorBoundary } from '@/lib/stellar/error-handling';
import { StellarStore } from '@/lib/stellar/stores';
```

## Usage Example

```typescript
// index.tsx
import { initializeGlobalErrorHandlers } from '@/lib/stellar/error-handling';
import { StellarErrorBoundary } from '@/lib/stellar/error-handling';

// Initialize error handlers
initializeGlobalErrorHandlers();

// Wrap your app
root.render(
  <React.StrictMode>
    <StellarErrorBoundary>
      <App />
    </StellarErrorBoundary>
  </React.StrictMode>
);
```

## Architecture

```
stellar/
├── error-handling/        # Error capture and logging
│   ├── StellarErrorBoundary.tsx
│   ├── StellarErrorHandler.ts
│   └── index.ts
├── stores/               # Store base classes
│   └── StellarStore.ts
├── models/              # Data models
│   ├── QueryNode.ts
│   └── MutationRequest.ts
├── services/            # WebSocket and API services
│   └── StoreWebSocketService.js
└── utils/              # Utility functions
    └── treeBuilder.ts
```

## Configuration

Configure through `app.config.js`:

```javascript
export const appConfig = {
  websocket: {
    storeDebugUrl: 'ws://localhost:3013/store-debug',
    reconnectInterval: 3000,
    messageQueueSize: 100
  },
  storeDebug: {
    enabled: true,
    logLevel: 'debug'
  }
};
```

## Error Handling Details

### Error Capture
- **Component Errors**: Caught by React Error Boundary
- **Window Errors**: Captured via window.onerror
- **Promise Rejections**: Captured via unhandledrejection event
- **Console Errors**: Intercepted and logged

### Error Logging Format
```typescript
{
  level: 'ERROR' | 'WARN' | 'INFO',
  message: string,
  timestamp: string,
  metadata: {
    source: string,
    stack?: string,
    userAgent: string,
    url: string,
    type: string,
    additionalData?: any
  }
}
```

### Error Display
In development, errors show:
- Error message
- Stack trace
- Component stack (for React errors)
- Recovery options (Try Again, Reload)

In production, errors show:
- User-friendly message
- Recovery options
- Error is logged silently

## Best Practices

1. **Always initialize error handlers** at app startup
2. **Wrap root component** with StellarErrorBoundary
3. **Use typed queries** with TypeScript interfaces
4. **Handle WebSocket disconnections** gracefully
5. **Log meaningful errors** with context
6. **Test error scenarios** in development

## API Reference

### StellarErrorBoundary

React component for catching and displaying errors.

**Props:**
- `children`: ReactNode - Components to wrap

**Methods:**
- `componentDidCatch(error, errorInfo)` - Captures React errors
- `handleReset()` - Resets error state
- `handleReload()` - Reloads the page

### initializeGlobalErrorHandlers()

Initializes global error capture.

**Returns:** void

**Side Effects:**
- Adds window error listener
- Adds unhandled rejection listener
- Overrides console.error

### logError(source, error, additionalData?)

Logs an error with metadata.

**Parameters:**
- `source`: string - Error source identifier
- `error`: Error | string - The error to log
- `additionalData?`: any - Additional context

### logWarning(source, message, additionalData?)

Logs a warning.

**Parameters:**
- `source`: string - Warning source
- `message`: string - Warning message
- `additionalData?`: any - Additional context

### logInfo(source, message, additionalData?)

Logs information.

**Parameters:**
- `source`: string - Info source
- `message`: string - Info message
- `additionalData?`: any - Additional context

## Development

### Debug Store Server

Run the debug store server:

```bash
cd orchestrix-ui/debug-store
npm start
```

### View Logs

Logs are stored in:
- `debug-store/store-debug/console-logs-*.jsonl`
- `debug-store/store-debug/store-events-*.jsonl`

### Testing Error Handling

```typescript
// Test component error
throw new Error('Test component error');

// Test promise rejection
Promise.reject('Test promise rejection');

// Test manual logging
logError('Test', new Error('Manual error'));
```

## License

Part of the Orchestrix project.