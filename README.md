# Orchestrix - Infrastructure Orchestration Platform

A comprehensive infrastructure management platform with real-time monitoring, event-driven architecture, and advanced error handling through the integrated Stellar framework.

## Table of Contents
- [Overview](#overview)
- [Architecture](#architecture)
- [Quick Start](#quick-start)
- [Stellar Framework](#stellar-framework)
- [Features](#features)
- [API Reference](#api-reference)
- [Development](#development)
- [Production Deployment](#production-deployment)

## Overview

Orchestrix is a modern infrastructure orchestration platform that provides:
- **Infrastructure Management**: Manage clouds, datacenters, compute resources, and network devices
- **Real-time Monitoring**: WebSocket-based live updates and monitoring
- **Event-driven Architecture**: Stellar framework for reactive data synchronization
- **Comprehensive Error Handling**: Automatic error capture and logging
- **LXC Container Orchestration**: Development and testing environment management

## Architecture

### System Components

```
orchestrix/
├── src/main/java/           # Spring Boot backend
├── orchestrix-ui/           # React frontend
│   ├── src/
│   │   ├── lib/stellar/     # Stellar framework
│   │   ├── stores/          # MobX stores
│   │   ├── services/        # API services
│   │   └── pages/           # UI components
│   └── debug-store/         # WebSocket debug server
└── images/lxc/              # LXC container configs
```

### Technology Stack
- **Backend**: Spring Boot 2.7, MySQL 8.0, Hibernate
- **Frontend**: React 18, TypeScript, MobX, Material-UI
- **Real-time**: WebSocket, EventBus pattern
- **Containerization**: LXC/LXD
- **Build**: Maven, npm/webpack

## Quick Start

### Prerequisites
- Java 21+
- Node.js 18+
- MySQL 8.0
- Maven 3.6+

### Installation

```bash
# Clone repository
git clone https://github.com/pialmmh/orchestrix.git
cd orchestrix

# Setup database
mysql -h 127.0.0.1 -u root -p123456 < database/schema.sql

# Start backend
mvn spring-boot:run

# Start frontend
cd orchestrix-ui
npm install
npm start

# Start debug store (development)
cd orchestrix-ui/debug-store
npm start
```

Access the application at http://localhost:3010

## Stellar Framework

Stellar is the integrated event-driven framework that powers Orchestrix's real-time features.

### Core Concepts

#### 1. Global Error Handling
Automatically captures and logs all application errors through WebSocket.

```typescript
// Initialize in index.tsx
import { initializeGlobalErrorHandlers, StellarErrorBoundary } from '@/lib/stellar/error-handling';

initializeGlobalErrorHandlers();

root.render(
  <React.StrictMode>
    <StellarErrorBoundary>
      <App />
    </StellarErrorBoundary>
  </React.StrictMode>
);
```

**What Gets Captured:**
- React component errors
- Unhandled JavaScript errors
- Promise rejections
- Console.error calls

**Manual Logging:**
```typescript
import { logError, logWarning, logInfo } from '@/lib/stellar/error-handling';

logError('ComponentName', new Error('Something failed'));
logWarning('ServiceName', 'Rate limit approaching');
logInfo('System', 'Operation completed');
```

#### 2. Query System
Declarative data fetching with nested relationships.

```typescript
// Simple query
const query: QueryNode = {
  kind: 'partner',
  criteria: { type: 'cloud-provider' },
  page: { limit: 10, offset: 0 }
};

// Complex nested query
const infrastructureQuery: QueryNode = {
  kind: 'partner',
  criteria: { name: 'telcobright' },
  include: [
    {
      kind: 'cloud',
      include: [
        {
          kind: 'datacenter',
          include: [
            { kind: 'compute' },
            { kind: 'network-device' }
          ]
        }
      ]
    }
  ]
};

const result = await store.executeQuery(query);
```

#### 3. Mutation System
Type-safe data modifications with automatic synchronization.

```typescript
// Create
await store.executeMutation({
  operation: 'CREATE',
  entity: 'compute',
  data: {
    name: 'vm-prod-001',
    cpu: 8,
    memory: 32,
    storage: 500,
    datacenterId: 401
  }
});

// Update
await store.executeMutation({
  operation: 'UPDATE',
  entity: 'compute',
  criteria: { id: 501 },
  data: { cpu: 16, memory: 64 }
});

// Delete
await store.executeMutation({
  operation: 'DELETE',
  entity: 'compute',
  criteria: { id: 501 }
});
```

#### 4. WebSocket Communication
Real-time bidirectional communication with automatic reconnection.

**Features:**
- Auto-reconnection on disconnect
- Message queuing during disconnection
- Request/response correlation
- Real-time store updates

**Message Types:**
- `QUERY` - Data queries
- `MUTATION` - Data modifications
- `SUBSCRIBE` - Real-time subscriptions
- `CONSOLE_LOGS` - Debug logging
- `STORE_UPDATE` - Real-time updates

#### 5. Store Management
MobX-based reactive stores with WebSocket integration.

```typescript
import { StellarStore } from '@/stores/base/StellarStore';

export class InfrastructureStore extends StellarStore {
  @observable data = [];

  async loadInfrastructure() {
    const query: QueryNode = {
      kind: 'infrastructure',
      include: ['cloud', 'datacenter', 'compute']
    };

    const result = await this.executeQuery(query);
    runInAction(() => {
      this.data = result;
    });
  }
}
```

## Features

### Infrastructure Management
- **Multi-cloud Support**: AWS, Azure, Google Cloud, Private clouds
- **Resource Hierarchy**: Partner → Cloud → Region → AZ → Datacenter → Compute/Network
- **Environment Management**: Development, Staging, Production environments
- **Network Automation**: MikroTik integration for network configuration

### UI Features
- **Infrastructure Tree View**: Hierarchical visualization with expand/collapse
- **WhatsApp-style Avatars**: Visual indicators for resource types
- **Organization/Partners Filter**: Toggle between organizational and partner views
- **Real-time Updates**: Live data synchronization via WebSocket
- **Error Recovery**: User-friendly error pages with recovery options

### Developer Features
- **Debug Store**: Development-time store for debugging
- **Console Log Capture**: All console logs sent to WebSocket
- **Query/Mutation History**: Track all data operations
- **Hot Module Replacement**: Fast development iteration

## API Reference

### Query API

```typescript
interface QueryNode {
  kind: string;              // Entity type
  criteria?: object;         // Filter criteria
  page?: {                   // Pagination
    limit: number;
    offset: number;
  };
  include?: QueryNode[];     // Nested includes
  orderBy?: {               // Sorting
    field: string;
    direction: 'asc' | 'desc';
  };
}
```

### Mutation API

```typescript
interface MutationRequest {
  operation: 'CREATE' | 'UPDATE' | 'DELETE';
  entity: string;           // Entity type
  criteria?: object;        // For UPDATE/DELETE
  data?: object;           // For CREATE/UPDATE
  options?: {
    returnNew?: boolean;   // Return updated entity
    validate?: boolean;    // Run validation
  };
}
```

### Error Handling API

```typescript
// Initialize error handlers
initializeGlobalErrorHandlers(): void

// Error boundary component
<StellarErrorBoundary>
  {children}
</StellarErrorBoundary>

// Manual logging
logError(source: string, error: Error | string, metadata?: any): void
logWarning(source: string, message: string, metadata?: any): void
logInfo(source: string, message: string, metadata?: any): void
```

### Store API

```typescript
class StellarStore {
  executeQuery(query: QueryNode): Promise<any>
  executeMutation(mutation: MutationRequest): Promise<any>
  subscribe(entity: string, callback: Function): void
  unsubscribe(entity: string): void
}
```

## Development

### Environment Setup

```bash
# Install dependencies
cd orchestrix-ui
npm install

# Start all services
npm run dev:all  # Starts frontend, backend, and debug store

# Or start individually
npm start         # Frontend only
npm run debug     # Debug store only
mvn spring-boot:run  # Backend only
```

### Configuration

```javascript
// orchestrix-ui/src/config/app.config.js
export const appConfig = {
  api: {
    baseUrl: 'http://localhost:8090/api'
  },
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

### Testing

```bash
# Unit tests
npm test

# Integration tests
mvn test

# E2E tests
npm run test:e2e
```

### Debug Store

The debug store captures all WebSocket traffic and console logs for debugging.

```bash
# Start debug store
cd orchestrix-ui/debug-store
npm start

# View logs
tail -f store-debug/console-logs-$(date +%Y-%m-%d).jsonl | jq '.'

# View store events
tail -f store-debug/store-events-*.jsonl | jq '.'
```

### Common Commands

```bash
# Build production
npm run build
mvn clean package

# Run linter
npm run lint

# Format code
npm run format

# Generate types
npm run generate-types
```

## Production Deployment

### Build for Production

```bash
# Build frontend
cd orchestrix-ui
npm run build

# Build backend
mvn clean package -Pprod

# Docker build
docker build -t orchestrix:latest .
```

### Configuration

```yaml
# application-prod.yml
server:
  port: 8080

spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}

websocket:
  url: wss://api.orchestrix.com/ws

logging:
  level:
    root: WARN
    com.telcobright: INFO
```

### Security Considerations

1. **WebSocket Security**
   - Use WSS (WebSocket Secure)
   - Implement authentication tokens
   - Add rate limiting
   - Validate all messages

2. **Error Handling**
   - Hide error details from users
   - Log errors to monitoring service
   - Configure alerts for critical errors

3. **Database**
   - Use connection pooling
   - Implement query optimization
   - Regular backups
   - Monitor slow queries

### Monitoring

```javascript
// Configure monitoring
export const monitoring = {
  sentry: {
    dsn: process.env.SENTRY_DSN,
    environment: 'production'
  },
  datadog: {
    apiKey: process.env.DATADOG_API_KEY,
    service: 'orchestrix'
  }
};
```

## Troubleshooting

### WebSocket Connection Issues
```javascript
// Check connection
console.log('WebSocket connected:', storeWebSocketService.isConnected);

// Manual reconnect
storeWebSocketService.connect();

// Check message queue
console.log('Queued messages:', storeWebSocketService.messageQueue.length);
```

### Store Debugging
```javascript
// Enable verbose logging
localStorage.setItem('stellar_debug', 'true');

// Check store state
console.log('Store:', store);
console.log('Error:', store.error);
console.log('Loading:', store.isLoading);
```

### Common Issues

**Issue**: "Store not found" error
```bash
# Solution: Add missing store to debug server
# Edit: debug-store/server-with-mutations.js
storeManager.createStore('storeName', StoreClass);
```

**Issue**: WebSocket disconnects frequently
```javascript
// Increase reconnect interval
appConfig.websocket.reconnectInterval = 5000;
```

**Issue**: Query timeout
```javascript
// Increase timeout in store
this.queryTimeout = 30000; // 30 seconds
```

## Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

### Code Style
- Use TypeScript for new code
- Follow ESLint rules
- Write unit tests for new features
- Update documentation

## License

This project is proprietary software. All rights reserved.

## Support

For issues and questions:
- GitHub Issues: https://github.com/pialmmh/orchestrix/issues
- Documentation: This README
- Debug logs: Check `debug-store/store-debug/` directory

## Changelog

### v2.0.0 (Latest)
- Added Stellar framework with event-driven architecture
- Implemented global error handling
- Added WebSocket-based real-time synchronization
- Improved Infrastructure tree UI with WhatsApp-style avatars
- Added Organization/Partners filter

### v1.0.0
- Initial release with basic infrastructure management
- Spring Boot backend with MySQL
- React frontend with Material-UI
- Basic CRUD operations