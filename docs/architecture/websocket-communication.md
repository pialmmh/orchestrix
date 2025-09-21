# WebSocket Communication

## Overview

WebSocket provides real-time bidirectional communication between frontend and backend for state synchronization and event notifications.

## Connection Details

- **URL**: `ws://localhost:3013/store-debug` (development)
- **Protocol**: WebSocket with JSON messages
- **Reconnection**: Automatic with exponential backoff

## Message Types

### Query Messages
```json
{
  "type": "query",
  "entity": "partner",
  "query": { "kind": "partner", "criteria": {} }
}
```

### Mutation Messages
```json
{
  "type": "mutation",
  "operation": "CREATE",
  "entity": "compute",
  "payload": { ... }
}
```

### Event Notifications
```json
{
  "type": "event",
  "event": "ENTITY_UPDATED",
  "entity": "partner",
  "data": { ... }
}
```

### Console Messages
```json
{
  "type": "console",
  "level": "log",
  "message": "...",
  "timestamp": 1234567890
}
```

## Connection Management

```typescript
class WebSocketService {
  connect(): void
  disconnect(): void
  send(message: object): void
  on(event: string, handler: Function): void
  reconnect(): void
}
```

## Event Flow

1. Client sends message via WebSocket
2. Server processes request
3. Server broadcasts updates to all clients
4. Clients update local state

## Error Handling

- Connection failures trigger automatic reconnection
- Messages queued during disconnection
- Timeout handling for requests
- Error events propagated to stores

## Related

- [Event-Driven Architecture](./event-driven-architecture.md)
- [Store Architecture](./store-architecture.md)