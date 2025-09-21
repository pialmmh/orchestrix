# Stellar Mutations

## Create Operation

```javascript
{
  kind: 'compute',
  operation: 'CREATE',
  payload: {
    name: 'Web-Server-02',
    hostname: 'web02.example.com',
    datacenterId: 38,
    status: 'ACTIVE',
    specs: {
      cpu: 4,
      memory: 8192,
      storage: 100
    }
  }
}
```

## Update Operation

```javascript
{
  kind: 'compute',
  operation: 'UPDATE',
  payload: {
    id: 2,
    status: 'INACTIVE',
    specs: {
      memory: 16384
    }
  }
}
```

## Delete Operation

```javascript
{
  kind: 'compute',
  operation: 'DELETE',
  payload: {
    id: 2
  }
}
```

## Batch Operations

```javascript
{
  kind: 'partner',
  operation: 'BATCH_UPDATE',
  payload: {
    criteria: { status: 'PENDING' },
    updates: { status: 'ACTIVE' }
  }
}
```

## Response Format

### Success Response
```json
{
  "success": true,
  "data": {
    "id": 123,
    "kind": "compute",
    "...entity fields..."
  },
  "operation": "CREATE",
  "timestamp": "2025-09-20T10:00:00Z"
}
```

### Error Response
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Missing required field: name",
    "field": "name"
  },
  "operation": "CREATE",
  "timestamp": "2025-09-20T10:00:00Z"
}
```

## Mutation Triggers

After successful mutations:
1. WebSocket notification sent to all clients
2. Related stores automatically refreshed
3. Debug logs updated (if enabled)
4. State history recorded

## Related

- [Stellar Query Overview](./stellar-query-overview.md)
- [Stellar Query Syntax](./stellar-query-syntax.md)