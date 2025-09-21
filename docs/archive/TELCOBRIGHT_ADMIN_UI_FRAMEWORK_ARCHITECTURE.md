# Telcobright Admin-UI Framework Architecture

## Executive Summary

The **Telcobright Admin-UI Framework** is a comprehensive enterprise application framework designed for building administrative interfaces, Line of Business (LOB) applications, ERP/CRM systems, and cloud/network automation tools with infrastructure management capabilities. It is one of several frameworks in the Telcobright ecosystem, specifically focused on creating sophisticated admin panels and management UIs with event-driven architecture, real-time debugging, and a sophisticated data query system.

## Core Design Philosophy

### 1. Separation of Concerns
- **Framework Layer**: Reusable components, services, and infrastructure
- **Application Layer**: Business logic specific to each application (Orchestrix, CRM, etc.)
- **Configuration Layer**: Environment-specific settings with profile management

### 2. Event-Driven Architecture
- All state changes propagate through events
- WebSocket-based real-time communication
- Centralized event bus for component decoupling
- Debug-mode event capture for development

### 3. Developer Experience First
- Comprehensive debugging capabilities with store-debug mode
- Hot-reloadable configuration
- Profile-based environment management
- Console redirect for production debugging

## Architecture Components

### 1. Frontend Framework (React/TypeScript)

#### Core Services
```
/framework/
├── services/
│   ├── EventBusService.ts        # WebSocket event communication
│   ├── StoreWebSocketService.ts  # Store synchronization via WS
│   ├── ConsoleRedirectService.ts # Console capture for debugging
│   └── StellarQueryService.ts    # Database query abstraction
├── stores/
│   ├── BaseStore.ts              # Base class for all stores
│   ├── ProxyStore.ts             # Debug mode proxy store
│   └── StoreManager.ts           # Store lifecycle management
├── config/
│   ├── app.profile.ts            # Profile selector (dev/staging/prod)
│   └── app.config.ts             # Centralized configuration
└── components/
    ├── layouts/                  # Reusable layout components
    ├── theme/                    # Material-UI theme configuration
    └── common/                   # Shared UI components
```

#### Application Structure
```
/apps/
├── orchestrix/                   # Cloud infrastructure management
│   ├── pages/
│   ├── stores/
│   └── components/
├── crm/                          # Customer relationship management
│   ├── pages/
│   ├── stores/
│   └── components/
└── [other-apps]/
```

### 2. Backend Services

#### Store Server (Node.js/TypeScript)
```
/store-server/
├── EventBusWithMutations.js     # WebSocket server with CRUD
├── stores/
│   ├── StoreManager.js          # Store orchestration
│   └── StoreStructure.js        # Store definitions
├── services/
│   ├── ApiService.js            # Backend API integration
│   └── DebugLogger.js           # Event logging service
└── store-debug/                 # Debug logs directory
    ├── console-logs-*.jsonl     # Frontend console logs
    └── store-events-*.jsonl     # All store events
```

#### Spring Boot Backend
```
/backend/
├── stellar/                     # Stellar V2 Query Framework
│   ├── StellarV2Controller     # Query/Mutation endpoints
│   ├── QueryParserV2           # Query parsing with 'kind' field
│   └── MutationHandler         # CRUD operations
└── api/
    ├── controllers/
    └── services/
```

## Data Flow Architecture

### 1. Query Flow (Read Operations)
```
UI Component
  → Store (MobX)
    → StoreWebSocketService
      → EventBus (WebSocket)
        → Store Server
          → Spring Boot API
            → Stellar Query Engine
              → Database
```

### 2. Mutation Flow (Write Operations)
```
UI Action
  → Store Method
    → WebSocket MUTATION event
      → EventBus
        → API Service
          → Spring Boot
            → Database
              → Auto-refresh queries
                → Update all subscribers
```

### 3. Debug Mode Flow
```
When store_debug = true:
UI Component
  → ProxyStore
    → WebSocket
      → Backend Store (source of truth)
        → All state managed on backend
          → Event logging to JSONL files
```

## Key Framework Features

### 1. Stellar Query System

**Purpose**: Unified query language for all data operations

**Structure**:
```typescript
interface StellarQuery {
  kind: string;           // Entity type (required in V2)
  criteria?: {            // Filter conditions
    [field: string]: any;
  };
  page?: {               // Pagination
    limit: number;
    offset: number;
  };
  include?: string[];    // Related entities to include
  sort?: {              // Sorting
    field: string;
    order: 'ASC' | 'DESC';
  };
}
```

**Example**:
```typescript
const query: StellarQuery = {
  kind: 'partner',
  criteria: { status: 'ACTIVE' },
  include: ['environments', 'clouds'],
  page: { limit: 10, offset: 0 }
};
```

### 2. Event Bus System

**WebSocket Events**:
- `QUERY`: Request data
- `MUTATION`: Create/Update/Delete
- `SUBSCRIBE`: Real-time updates
- `UNSUBSCRIBE`: Stop updates
- `CONSOLE_LOGS`: Debug console capture
- `STORE_UPDATE`: Broadcast state changes

**Event Structure**:
```typescript
interface StoreEvent {
  type: string;
  entity: string;
  operation?: 'CREATE' | 'UPDATE' | 'DELETE';
  payload?: any;
  requestId?: string;
  timestamp: number;
}
```

### 3. Configuration Profiles

**Three Environments**:
1. **Development**: Full debugging, console capture, verbose logging
2. **Staging**: Moderate debugging, selective console capture
3. **Production**: Minimal logging, optional console capture

**Profile Configuration**:
```typescript
// app.profile.ts
export const ACTIVE_PROFILE: ConfigProfile = 'development';

// Environment variable override
REACT_APP_PROFILE=production npm start
```

### 4. Debug Store System

**Features**:
- All events logged to JSONL files
- Console output capture
- Query/mutation history
- Performance metrics
- Error tracking

**File Structure**:
```
store-debug/
├── console-logs-2025-09-20.jsonl      # Console outputs
├── store-events-2025-09-20T*.jsonl    # All events
└── [entity]/
    ├── current.json                    # Current state
    └── history.jsonl                   # State history
```

### 5. Console Redirect System

**Purpose**: Capture frontend console.log() for debugging production issues

**Implementation**:
- Intercepts console methods
- Filters framework noise
- Batches messages for efficiency
- Sends to backend via WebSocket
- Stores in JSONL format

**Configuration**:
```typescript
consoleRedirect: {
  enabled: true,
  captureLog: true,
  captureWarn: true,
  captureError: true,
  captureInfo: false,
  captureDebug: false,
  batchInterval: 5000,  // 5 seconds
  maxBatchSize: 20
}
```

## Development Workflow

### 1. Local Development
```bash
# Backend
cd backend
mvn spring-boot:run

# Store Server
cd orchestrix-ui/store-server
npm start

# Frontend
cd orchestrix-ui
npm start
```

### 2. Debug Mode Development
1. Set `store_debug: true` in config
2. All state managed by backend
3. Check `store-debug/` for event logs
4. Use consolidated-view.json for state inspection

### 3. Adding New Application
1. Create app folder under `/apps/`
2. Extend BaseStore for state management
3. Use framework services (EventBus, Stellar)
4. Apply common theme and layouts
5. Register routes in main app

## Best Practices

### 1. State Management
- Use MobX stores for local state
- Extend BaseStore for consistency
- In debug mode, use ProxyStore
- Keep business logic in stores, not components

### 2. Data Fetching
- Always use Stellar queries
- Implement optimistic updates
- Cache responses appropriately
- Handle errors gracefully

### 3. Event Handling
- Name events clearly: `ENTITY_ACTION`
- Include requestId for tracking
- Log important events
- Clean up subscriptions

### 4. Configuration
- Never hardcode URLs or settings
- Use profile-based configuration
- Allow runtime overrides
- Document all config options

### 5. Debugging
- Use meaningful console.log() messages
- Leverage store-debug mode
- Check event logs for issues
- Monitor WebSocket traffic

## Framework Extension Points

### 1. Adding New Store
```typescript
export class CustomStore extends BaseStore {
  constructor() {
    super('customEntity');
  }

  async fetchData(criteria: any) {
    return this.query({ kind: 'custom', criteria });
  }
}
```

### 2. Adding New WebSocket Event
```typescript
// In EventBusWithMutations.js
case 'CUSTOM_EVENT':
  await this.handleCustomEvent(ws, message);
  break;
```

### 3. Custom Query Type
```typescript
// Extend StellarQuery
interface CustomQuery extends StellarQuery {
  customField?: string;
  aggregations?: any[];
}
```

## Technology Stack

### Frontend
- React 18.x with TypeScript
- MobX 6.x for state management
- Material-UI v5 for components
- WebSocket for real-time
- React Router for navigation

### Backend
- Spring Boot 2.7.x
- Stellar V2 Query Framework
- WebSocket (ws library)
- Node.js for store server
- MySQL/PostgreSQL database

### Development Tools
- TypeScript for type safety
- ESLint for code quality
- Jest for testing
- Webpack for bundling

## Security Considerations

1. **Authentication**: JWT tokens for API access
2. **Authorization**: Role-based access control
3. **WebSocket Security**: Token validation on connection
4. **Data Validation**: Input sanitization
5. **Console Redirect**: Filter sensitive data

## Performance Optimization

1. **Lazy Loading**: Code splitting for routes
2. **Caching**: Query result caching
3. **Batching**: Batch console logs and events
4. **Pagination**: Limit data per request
5. **WebSocket Compression**: Enable compression

## Monitoring and Observability

1. **Event Logs**: All events in JSONL format
2. **Console Capture**: Frontend logs to backend
3. **Performance Metrics**: Query timing
4. **Error Tracking**: Centralized error handling
5. **Health Checks**: Service availability

## Future Enhancements

1. **GraphQL Integration**: Alternative to Stellar
2. **Microservices**: Service mesh architecture
3. **Real-time Collaboration**: Multi-user editing
4. **AI Integration**: Intelligent suggestions
5. **Mobile Support**: React Native apps

## Relationship to Other Telcobright Frameworks

The Telcobright Admin-UI Framework is part of the larger Telcobright ecosystem, which includes:
- **Telcobright Admin-UI Framework** (this framework): Administrative interfaces and management panels
- **Telcobright Analytics Framework**: Data analytics and reporting
- **Telcobright Integration Framework**: System integration and API management
- **Telcobright Mobile Framework**: Mobile application development

Each framework can work independently or together as part of a comprehensive enterprise solution.

## Conclusion

The Telcobright Admin-UI Framework provides a solid foundation for building enterprise administrative applications with real-time capabilities, comprehensive debugging, and flexible configuration. By separating framework concerns from application logic, it enables rapid development of new business applications while maintaining consistency and quality across all admin interfaces.

For implementation details, refer to the specific service documentation and code examples in the framework directory.