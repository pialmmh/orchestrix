# Debug Store Configuration

## Enabling Debug Store

### Application Configuration

```typescript
// src/config/app.config.ts
storeDebug: {
  enabled: true,              // Enable debug store
  logRetentionHours: 24,     // Keep logs for 24 hours
  requestTimeoutMs: 30000    // WebSocket timeout
}
```

### Environment Variable

```bash
# Enable via environment
REACT_APP_STORE_DEBUG=true npm start
```

### Profile-Based Activation

Debug Store is automatically enabled in development profile:
```typescript
// Development profile (auto-enabled)
storeDebug: { enabled: true }

// Production profile (disabled by default)
storeDebug: { enabled: false }
```

## Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `enabled` | boolean | false | Enable/disable debug store |
| `logRetentionHours` | number | 24 | Hours to retain log files |
| `requestTimeoutMs` | number | 30000 | WebSocket request timeout |

## File Storage Location

```
orchestrix-ui/debug-store/store-debug/
├── console-logs-*.jsonl      # Console output logs
├── store-events-*.jsonl      # Event logs
└── [entity]/                 # Entity-specific logs
    ├── current.json
    └── history.jsonl
```

## Related

- [Profile Management](../configuration/profile-management.md)
- [Debug Store Overview](./debug-store-overview.md)