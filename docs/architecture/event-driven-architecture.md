# Event-Driven Architecture

## Overview

The Telcobright Admin-UI Framework uses an event-driven architecture where all state changes and communications flow through a centralized event bus via WebSocket connections.

## Core Concepts

### Event Bus
Central message broker that handles all communication between frontend and backend.

### Event Types
- **QUERY** - Data fetching
- **MUTATION** - Data modification (CREATE, UPDATE, DELETE)
- **SUBSCRIBE** - Real-time updates
- **UNSUBSCRIBE** - Stop updates
- **CONSOLE_LOGS** - Debug console capture
- **STORE_UPDATE** - State change broadcast

## Event Flow

```
User Action
    ↓
Frontend Store
    ↓
WebSocket Event
    ↓
Event Bus (Store Server)
    ↓
Backend API
    ↓
Database
    ↓
Response Event
    ↓
Update Subscribers
```

## Event Structure

### Standard Event Format
```typescript
interface StoreEvent {
  type: string;           // Event type
  entity: string;         // Target entity
  operation?: string;     // CRUD operation
  payload?: any;          // Event data
  requestId?: string;     // Tracking ID
  timestamp: number;      // Event time
  metadata?: {           // Additional info
    source: string;
    sessionId: string;
  };
}
```

## Event Examples

### Query Event
```json
{
  "type": "QUERY",
  "entity": "partner",
  "payload": {
    "kind": "partner",
    "criteria": { "status": "ACTIVE" }
  },
  "requestId": "req_1234",
  "timestamp": 1758332020409
}
```

### Mutation Event
```json
{
  "type": "MUTATION",
  "entity": "compute",
  "operation": "UPDATE",
  "payload": {
    "id": 2,
    "status": "INACTIVE"
  },
  "requestId": "req_5678",
  "timestamp": 1758332020410
}
```

### Subscribe Event
```json
{
  "type": "SUBSCRIBE",
  "entity": "infrastructure",
  "timestamp": 1758332020411
}
```

## Benefits

### Decoupling
- Components don't directly communicate
- Easy to add new event handlers
- Flexible system extension

### Debugging
- All events are logged
- Complete audit trail
- Easy replay of events

### Real-time Updates
- Instant state propagation
- Multiple subscriber support
- Automatic UI updates

### Scalability
- Horizontal scaling possible
- Event queue support
- Load distribution

## Implementation

### Frontend (Publishing Events)
```typescript
class PartnerStore {
  async fetchPartners() {
    const event = {
      type: 'QUERY',
      entity: 'partner',
      payload: { kind: 'partner' }
    };

    return this.eventBus.send(event);
  }
}
```

### Backend (Handling Events)
```javascript
async handleMessage(ws, message) {
  switch (message.type) {
    case 'QUERY':
      await this.handleQuery(ws, message);
      break;
    case 'MUTATION':
      await this.handleMutation(ws, message);
      break;
  }
}
```

### Subscribing to Updates
```typescript
const unsubscribe = storeWebSocketService.subscribe('partner', (data) => {
  console.log('Partner data updated:', data);
});
```

## Error Handling

### Event Errors
```json
{
  "type": "ERROR",
  "error": "Query failed",
  "requestId": "req_1234",
  "timestamp": 1758332020412
}
```

### Retry Logic
- Automatic reconnection on disconnect
- Event queue for offline mode
- Exponential backoff

## Best Practices

1. **Always include requestId** - For tracking
2. **Use specific event types** - Clear intent
3. **Keep payloads small** - Performance
4. **Handle errors gracefully** - User experience
5. **Clean up subscriptions** - Memory leaks
6. **Log important events** - Debugging