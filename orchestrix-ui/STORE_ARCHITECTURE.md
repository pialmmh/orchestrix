# Orchestrix Unified Store Architecture

## Overview

The Orchestrix store architecture implements a **unified, event-driven state management system** that works identically on both frontend and backend. This design enables:

- **UI-agnostic testing** - Test stores without any UI components
- **Debug mode with backend persistence** - Store runs on Node.js backend with WebSocket communication
- **Production mode with local state** - Store runs in browser with local event bus
- **Text file visibility** - Backend stores save snapshots to JSON files for debugging
- **Automatic synchronization** - TypeScript files sync between frontend and backend

## Architecture Principles

1. **Single Source of Truth** - One unified store codebase in TypeScript
2. **Reactive Queries** - Observable queries that auto-fetch data when changed
3. **Event-Driven** - All operations go through event bus
4. **Minimal API** - Simple, clean interface for store operations
5. **TypeScript Everywhere** - Same code runs on frontend and backend

## Directory Structure

```
orchestrix-ui/
├── src/unified-store/           # Frontend store (source)
│   ├── base/                    # Base classes
│   ├── core/                    # Core utilities
│   ├── events/                  # Event system
│   ├── infrastructure/          # Domain stores
│   └── services/                # API services
│
├── store-server/                # Backend server
│   ├── unified-store/           # Auto-synced from frontend
│   ├── store-snapshots/         # JSON snapshots
│   ├── server-simple.ts         # Simplified WebSocket server
│   ├── test-runner.ts           # UI-less testing
│   └── dev-server-simple.js     # Auto-sync dev server
│
└── scripts/
    └── sync-store.js            # Sync automation
```

## Store Components

### 1. StoreManager (`core/StoreManager.ts`)

Central manager for all stores with:
- Store registration and retrieval
- Automatic snapshot persistence
- Reactive snapshot saving
- Testing utilities

```typescript
const storeManager = getStoreManager();
const infraStore = storeManager.getStore<OrganizationInfraStore>('infrastructure');
```

### 2. StellarStore (`base/StellarStore.ts`)

Base class for stores that interact with Stellar API:
- Query execution
- Mutation handling
- Error management
- Loading states

### 3. EventBus (`events/EventBus.ts`)

Abstraction for event communication:
- **Debug mode**: WebSocket to backend
- **Production mode**: Local mitt event bus
- Request-response pattern
- Automatic mode switching

### 4. Store Snapshots

Backend automatically saves store state to JSON files:
- `store-snapshots/infrastructure-latest.json` - Current state
- `store-snapshots/history/` - Timestamped backups
- `store-snapshots/all-stores-latest.json` - Complete snapshot

## Running the System

### Development Mode (with Backend Store)

```bash
# Terminal 1: Start frontend
cd orchestrix-ui
npm start

# Terminal 2: Start backend store server
cd store-server
npm run dev:simple  # Auto-sync and auto-restart

# Or without auto-sync
npm run start:simple
```

### Testing Without UI

```bash
cd store-server
npm test  # Runs store tests without any UI
```

### View Store Snapshots

```bash
# List snapshots
cd store-server
npm run snapshots

# View current store state via HTTP
curl http://localhost:8080/stores
```

## Event Flow

### Debug Mode (WebSocket)

```
UI Component
    ↓
Store Method Call
    ↓
EventBus (WebSocket mode)
    ↓
WebSocket → Backend Server
    ↓
Backend Store Instance
    ↓
Stellar API Call
    ↓
Store Update + Snapshot
    ↓
WebSocket → Frontend
    ↓
UI Update
```

### Production Mode (Local)

```
UI Component
    ↓
Store Method Call
    ↓
EventBus (Local mode)
    ↓
LocalStore Handler
    ↓
Stellar API Call
    ↓
Store Update
    ↓
UI Update
```

## Configuration

### Debug Mode Toggle

```typescript
// src/config/storeDebugConfig.ts
export const config = {
  store_debug: true,  // Enable backend mode
  eventbus: 'ws',     // Auto-set based on debug flag
  wsUrl: 'ws://localhost:8080'
};
```

### Environment Variables

```bash
# .env
REACT_APP_STELLAR_BACKEND_URL=http://localhost:9090/api
REACT_APP_STORE_DEBUG=true
WS_PORT=8080
```

## Store Implementation Example

```typescript
export class OrganizationInfraStore extends StellarStore {
  @observable treeData: TreeNode[] = [];
  @observable selectedNode: TreeNode | null = null;

  @action
  async loadInfrastructureTree(partner: string = 'self') {
    const query: QueryNode = {
      kind: 'partner',
      criteria: { name: partner },
      page: { limit: 10, offset: 0 },
      include: [/* nested includes */]
    };

    const data = await this.executeQuery(query);

    runInAction(() => {
      this.treeData = transformStellarToTree(data);
    });
  }
}
```

## Testing Strategy

### 1. Unit Tests (Without UI)

```typescript
// test-runner.ts
await runner.runTest('Load Infrastructure Tree', async () => {
  const infraStore = storeManager.getStore('infrastructure');
  await infraStore.loadInfrastructureTree('self');

  if (!infraStore.treeData || infraStore.treeData.length === 0) {
    throw new Error('Tree data is empty');
  }
});
```

### 2. Integration Tests (With Backend)

```bash
# Start backend
npm run start:simple

# Run integration tests
npm run test:integration
```

### 3. E2E Tests (With UI)

```bash
# Full stack
npm run dev  # Frontend
npm run dev:simple  # Backend

# Run Playwright
npm run test:e2e
```

## Auto-Sync Mechanism

The `sync-store.js` script:
1. Copies TypeScript files as-is (no transformation)
2. Maintains directory structure
3. Generates index.ts for exports
4. Runs on post-build
5. Watches for changes in dev mode

```json
// package.json
"scripts": {
  "build": "react-scripts build",
  "postbuild": "node scripts/sync-store.js"
}
```

## Advantages

1. **Testability** - Stores work without UI components
2. **Debuggability** - JSON snapshots show exact state
3. **Consistency** - Same code on frontend and backend
4. **Simplicity** - No XState complexity, just MobX
5. **Reactivity** - Automatic updates on query changes
6. **Persistence** - Backend stores survive restarts

## Migration from XState

The simplified architecture removes XState in favor of:
- Direct MobX reactions for state tracking
- JSON file persistence for history
- EventBus for communication
- StoreManager for orchestration

## Future Enhancements

1. **Store Replay** - Load and replay snapshots
2. **Time Travel** - Navigate through historical states
3. **Store Sync** - Real-time multi-client sync
4. **Query Caching** - Smart cache invalidation
5. **Optimistic Updates** - Immediate UI feedback

## Troubleshooting

### Store not syncing
```bash
cd store-server
npm run sync
```

### WebSocket connection failed
- Check debug mode is enabled
- Verify backend is running on port 8080
- Check for CORS or firewall issues

### Snapshots not created
- Ensure store-snapshots/ directory exists
- Check write permissions
- Verify backend is running with Node.js

### TypeScript errors
- Run `npm run sync` to update backend
- Ensure tsx is installed
- Check tsconfig.json settings