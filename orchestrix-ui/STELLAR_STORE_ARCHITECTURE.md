# Stellar Store Architecture & Debug Mode Guide

## Overview

This document provides comprehensive instructions for working with the Stellar Query System, Event-Driven Store Architecture, and Debug Mode in Orchestrix. All store operations flow through an event bus that can operate in two modes: local (production) or WebSocket (debug).

## Architecture

### Two Operational Modes

#### 1. Production Mode (Local EventBus)
```
UI → QueryService → LocalEventBus → LocalStore → StellarClient → Backend API
                                          ↓
                                    Success/Error → EventBus → UI
```

#### 2. Debug Mode (WebSocket EventBus)
```
UI → QueryService → WebSocketEventBus → Store Server → StellarClient → Backend API
                                              ↓
                                    Success/Error → WebSocket → All Clients
```

## Quick Start

### 1. Enable/Disable Debug Mode

Edit `src/config/storeDebugConfig.ts`:
```typescript
export const defaultStoreDebugConfig: StoreDebugConfig = {
  store_debug: false,  // Set to true for debug mode
  websocket_url: 'ws://localhost:3013/store-debug',
  log_retention_hours: 24,
  request_timeout_ms: 30000,
};
```

### 2. Start Store Server (Debug Mode Only)

```bash
# Install dependencies
cd store-server
npm install

# Start server
npm start

# Or run from UI directory
cd orchestrix-ui
node store-server/server.js
```

### 3. Run Application

```bash
# Development
npm start

# Build with automatic store sync
npm run build  # Automatically syncs stores to backend
```

## Stellar Query System

### Basic Query Structure

```typescript
import QueryService from '../services/QueryService';

// Simple query
const query: QueryNode = {
  kind: 'partner',  // Entity type
  criteria: { name: 'telcobright' },  // Filter conditions
  page: { limit: 10, offset: 0 }  // Pagination
};

// Execute query
const response = await QueryService.executeQuery(query);
```

### Nested Queries with Relationships

```typescript
const nestedQuery: QueryNode = {
  kind: 'partner',
  criteria: { name: 'telcobright' },
  include: [
    {
      kind: 'cloud',
      include: [
        {
          kind: 'datacenter',
          include: [
            {
              kind: 'compute',
              page: { limit: 100, offset: 0 }
            }
          ]
        }
      ]
    }
  ]
};
```

### Mutations

```typescript
import MutationService from '../services/MutationService';

// Create
const createRequest: EntityModificationRequest = {
  entityName: 'compute',
  operation: 'INSERT',
  data: {
    name: 'web-server-01',
    ip_address: '192.168.1.100',
    datacenterId: 1
  }
};

// Update
const updateRequest: EntityModificationRequest = {
  entityName: 'compute',
  operation: 'UPDATE',
  id: 123,
  data: { status: 'active' }
};

// Delete
const deleteRequest: EntityModificationRequest = {
  entityName: 'compute',
  operation: 'DELETE',
  id: 123
};

// Execute mutation
const response = await MutationService.executeMutation(createRequest);
```

## Creating a New Store

### 1. Create Store Class

Create `src/unified-store/[domain]/[Name]Store.ts`:

```typescript
import { makeObservable, observable, action } from 'mobx';
import { UnifiedStore, ApiResponse } from '../base/UnifiedStore';
import QueryService from '../services/QueryService';

export class OrderStore extends UnifiedStore {
  orders: Order[] = [];
  selectedOrder: Order | null = null;

  constructor() {
    super();
    makeObservable(this, {
      orders: observable,
      selectedOrder: observable,
      loadOrders: action,
      selectOrder: action,
    });
  }

  async loadOrders(customerId?: number) {
    return this.executeOperation(
      'loadOrders',
      async () => {
        const query = {
          kind: 'order',
          criteria: customerId ? { customer_id: customerId } : {},
          page: { limit: 50, offset: 0 }
        };

        const response = await QueryService.executeQuery<Order>(query);

        if (response.success) {
          this.orders = response.data || [];
        }

        return response;
      },
      {
        retryCount: 2,  // Retry twice on failure
        retryDelay: 1000,  // Wait 1s between retries
        errorHandler: (error) => {
          console.error('Failed to load orders:', error);
          // Custom error handling
        }
      }
    );
  }

  async createOrder(orderData: Partial<Order>) {
    return this.executeOperation(
      'createOrder',
      async () => {
        const request = {
          entityName: 'order',
          operation: 'INSERT' as const,
          data: orderData
        };

        const response = await MutationService.executeMutation(request);

        if (response.success) {
          await this.loadOrders();  // Reload after creation
        }

        return response;
      }
    );
  }

  selectOrder(order: Order | null) {
    this.selectedOrder = order;
  }
}
```

### 2. Register Store in RootStore

Edit `src/stores/RootStore.ts`:

```typescript
import { OrderStore } from '../unified-store/orders/OrderStore';

export class RootStore {
  // ... existing stores
  orderStore: OrderStore;

  constructor() {
    // ... existing initialization
    this.orderStore = new OrderStore();
  }
}
```

### 3. Store Will Auto-Sync

After building, the store automatically syncs to `store-server/unified-store/` for debug mode.

## Store Features

### Error Handling

All stores inherit comprehensive error handling from `UnifiedStore`:

```typescript
// Automatic retry with exponential backoff
await store.loadData({
  retryCount: 3,
  retryDelay: 1000,
  timeout: 30000,
  errorHandler: (error) => {
    // Custom error handling
    notifyUser(error.message);
  }
});
```

### Operation History

Stores track all operations:

```typescript
const stats = store.getStats();
console.log(`Success rate: ${stats.successRate}%`);
console.log(`Average duration: ${stats.avgDuration}ms`);
```

### Event Publishing

All operations publish events automatically:

```typescript
// Events are published for:
// - Operation START
// - Operation SUCCESS (with data)
// - Operation ERROR (with error details)
```

## Debug Mode Features

### WebSocket Store Server with XState

When debug mode is enabled:

1. **All queries go through WebSocket** instead of direct HTTP
2. **Server maintains event log** for debugging with automatic rotation
3. **Multiple clients can connect** for monitoring
4. **Event history** is preserved with JSONL format and compression
5. **XState machines** manage store state with full history tracking
6. **Event replay** capability for time-travel debugging

### Monitoring Events

Connect multiple browser tabs to see all events:

```javascript
// Events are automatically logged in console when debug mode is on
[EventBus] Published: {
  id: "abc-123",
  type: "query",
  operation: "REQUEST",
  entity: "partner",
  payload: {...}
}
```

### Store Server Configuration

#### XState-Enhanced Server (Recommended)
Use `store-server/serverWithXState.js` for advanced features:

```javascript
const PORT = process.env.STORE_SERVER_PORT || 3013;
const STELLAR_API_URL = process.env.STELLAR_API_URL || 'http://localhost:8090/api';
const ENABLE_XSTATE = process.env.ENABLE_XSTATE !== 'false'; // Default to true

// Features:
// - XState machines for state management
// - MobX stores embedded in machine context
// - Event history with JSONL logging
// - Log rotation and compression
// - Event replay for time-travel debugging
// - Admin endpoints for monitoring
```

#### Basic Server
Use `store-server/server.js` for simple debugging:

```javascript
const PORT = process.env.STORE_SERVER_PORT || 3013;
const STELLAR_API_URL = process.env.STELLAR_API_URL || 'http://localhost:8090/api';
const MAX_LOG_SIZE = 10000;  // Event log size
```

## File Organization

```
orchestrix-ui/
├── src/
│   ├── unified-store/          # All store code (single source of truth)
│   │   ├── base/              # Base classes
│   │   ├── infrastructure/   # Infrastructure domain stores
│   │   ├── events/           # Event bus system
│   │   ├── services/         # Query/Mutation services
│   │   └── helpers/          # Utilities
│   └── config/
│       └── storeDebugConfig.ts  # Debug mode configuration
├── store-server/              # WebSocket server for debug mode
│   ├── server.js             # Basic WebSocket server
│   ├── serverWithXState.js  # XState-enhanced server
│   ├── storeStateMachine.js # XState machine definitions
│   ├── historyLogger.js     # Event history with rotation
│   ├── unified-store/        # Auto-synced from src/unified-store
│   ├── logs/                 # Event logs and archives
│   └── package.json
└── scripts/
    └── sync-store.js         # Post-build sync script
```

## Build Process

The build process automatically syncs stores:

```json
// package.json
{
  "scripts": {
    "build": "react-scripts build && npm run sync-store",
    "sync-store": "node scripts/sync-store.js",
    "sync-store:watch": "node scripts/sync-store.js --watch"
  }
}
```

## Testing

### Test Production Mode
```bash
# Set store_debug: false in config
npm start
# Check console: "[EventBus] Creating LocalEventBus"
```

### Test Debug Mode
```bash
# Set store_debug: true in config
# Start store server
cd store-server && npm start

# In another terminal
npm start
# Check console: "[EventBus] Creating WebSocketEventBus for debug mode"
```

## Troubleshooting

### Issue: No data loading
1. Check if backend is running: `http://localhost:8090/api/health`
2. Verify EventBus mode matches expectations
3. Check browser console for errors

### Issue: WebSocket connection failed
1. Ensure store server is running: `cd store-server && npm start`
2. Check port 3013 is not blocked
3. Verify websocket_url in config

### Issue: Stores not syncing
1. Run sync manually: `npm run sync-store`
2. Check for syntax errors in TypeScript files
3. Verify scripts/sync-store.js has execute permissions

## Best Practices

1. **Always use UnifiedStore base class** for consistent error handling
2. **Keep stores in unified-store folder** for auto-sync
3. **Use executeOperation wrapper** for all API calls
4. **Handle both success and error** responses explicitly
5. **Test both modes** before deploying
6. **Monitor operation history** in production for performance

## Migration Guide

### From Old Store to Unified Store

```typescript
// Old approach
class OldStore {
  async loadData() {
    try {
      const response = await api.get('/data');
      this.data = response.data;
    } catch (error) {
      console.error(error);
    }
  }
}

// New approach
class NewStore extends UnifiedStore {
  async loadData() {
    return this.executeOperation(
      'loadData',
      async () => {
        const response = await QueryService.executeQuery({
          kind: 'data',
          page: { limit: 100, offset: 0 }
        });

        if (response.success) {
          this.data = response.data;
        }

        return response;
      },
      { retryCount: 2 }
    );
  }
}
```

## XState Integration

### Overview

The XState integration embeds MobX stores as context within state machines, providing:
- **State machine management** of all store operations
- **Complete history tracking** with event replay
- **Time-travel debugging** capabilities
- **Automatic log rotation** with compression

### Using XState Server

```bash
# Start with XState enabled (default)
cd store-server
node serverWithXState.js

# Or disable XState for basic mode
ENABLE_XSTATE=false node serverWithXState.js
```

### Admin Endpoints

```bash
# View server statistics
curl http://localhost:3013/admin/stats

# View recent history
curl http://localhost:3013/admin/history?limit=50

# View session-specific history
curl http://localhost:3013/admin/history?sessionId=abc-123&limit=100
```

### XState Machine States

Each store operates through these states:
- **idle**: Ready for operations
- **querying**: Processing a query request
- **mutating**: Processing a mutation request
- **restoring**: Restoring from history

### Event Replay Example

```javascript
// Client sends replay request via WebSocket
ws.send(JSON.stringify({
  type: 'system',
  operation: 'REPLAY_EVENTS',
  fromTimestamp: Date.now() - 3600000, // 1 hour ago
  toTimestamp: Date.now() - 1800000,   // 30 minutes ago
  sessionId: 'current-session-id'
}));

// Server replays events and returns final state
```

### History Logger Features

- **JSONL format**: One JSON object per line for efficient parsing
- **Automatic rotation**: Rotates when file exceeds 10MB
- **Compression**: Archives old logs with gzip
- **Retention policy**: Keeps 30 archive files
- **Search capability**: Query by time, event type, or session

## API Reference

### QueryNode Interface
```typescript
interface QueryNode {
  kind: string;           // Entity type
  criteria?: object;      // Filter conditions
  page?: {
    limit: number;
    offset: number;
  };
  include?: QueryNode[];  // Nested queries
  orderBy?: {
    field: string;
    direction: 'ASC' | 'DESC';
  };
}
```

### EntityModificationRequest Interface
```typescript
interface EntityModificationRequest {
  entityName: string;
  operation: 'INSERT' | 'UPDATE' | 'DELETE';
  id?: number;          // Required for UPDATE/DELETE
  data?: object;        // Required for INSERT/UPDATE
}
```

### ApiResponse Interface
```typescript
interface ApiResponse<T = any> {
  success: boolean;
  data?: T;
  error?: string;
  metadata?: {
    timestamp: number;
    duration?: number;
    retryCount?: number;
  };
}
```

---

**Note:** This architecture ensures that stores behave identically whether running locally or through the debug server, making development and debugging seamless.