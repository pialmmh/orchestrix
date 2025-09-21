# Debug Store Log Format

## Event Log Structure

Each event is logged as a single JSON line (JSONL format):

```json
{
  "timestamp": 1758332020409,
  "isoTimestamp": "2025-09-20T01:33:40.409Z",
  "type": "query",
  "operation": "QUERY_START",
  "entity": "partner",
  "data": {},
  "metadata": {
    "endpoint": "/stellar/query",
    "method": "POST",
    "duration": 125
  }
}
```

## Log Types

### Query Events
- `QUERY_START` - Query initiated
- `QUERY_SUCCESS` - Query completed successfully
- `QUERY_ERROR` - Query failed

### Mutation Events
- `MUTATION_CREATE` - Create operation
- `MUTATION_UPDATE` - Update operation
- `MUTATION_DELETE` - Delete operation

### System Events
- `CONNECTION_OPEN` - WebSocket connected
- `CONNECTION_CLOSE` - WebSocket disconnected
- `STATE_SYNC` - State synchronized

## Console Log Format

```json
{
  "timestamp": 1758332020409,
  "isoTimestamp": "2025-09-20T01:33:40.409Z",
  "level": "log|warn|error|info",
  "message": "Console message text",
  "args": [],
  "source": "frontend",
  "sessionId": "abc123"
}
```

## State History Format

```json
{
  "timestamp": 1758332020409,
  "previousState": {},
  "newState": {},
  "changes": {
    "added": [],
    "modified": [],
    "removed": []
  },
  "trigger": "QUERY_SUCCESS"
}
```

## Related

- [Debug Store Usage Guide](./debug-store-usage.md)
- [Debug Store Overview](./debug-store-overview.md)