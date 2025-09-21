# Debug Store Usage Guide

## Common Commands

### View Live Events
```bash
# Watch all events
tail -f debug-store/store-debug/store-events-*.jsonl | jq

# Watch specific entity
tail -f debug-store/store-debug/store-events-*.jsonl | jq 'select(.entity=="partner")'
```

### Search Logs

```bash
# Find errors
grep "ERROR" debug-store/store-debug/store-events-*.jsonl | jq

# Find slow queries (>1 second)
grep "duration" debug-store/store-debug/*.jsonl | jq 'select(.metadata.duration > 1000)'

# Find specific operation
grep "MUTATION_DELETE" debug-store/store-debug/*.jsonl | jq
```

### Console Output

```bash
# View console logs
tail debug-store/store-debug/console-logs-*.jsonl | jq -r '.message'

# Filter by log level
grep '"level":"error"' debug-store/store-debug/console-logs-*.jsonl | jq
```

### State Inspection

```bash
# Current state
cat debug-store/store-debug/consolidated-view.json | jq

# Entity state
cat debug-store/store-debug/partner/current.json | jq

# State history
tail debug-store/store-debug/partner/history.jsonl | jq
```

## Troubleshooting Patterns

### WebSocket Connection Issues
```bash
grep -E "CONNECTION_|WebSocket" debug-store/store-debug/*.jsonl | jq
```

### Failed Mutations
```bash
grep "MUTATION_" debug-store/store-debug/*.jsonl | jq 'select(.status=="error")'
```

### State Sync Problems
```bash
grep "STATE_SYNC" debug-store/store-debug/*.jsonl | jq
```

## Cleanup

```bash
# Remove old logs (older than 24 hours)
find debug-store/store-debug -name "*.jsonl" -mtime +1 -delete

# Clear all debug logs
rm -f debug-store/store-debug/*.jsonl
rm -rf debug-store/store-debug/*/
```

## Related

- [Debug Store Configuration](./debug-store-configuration.md)
- [Debug Store Log Format](./debug-store-log-format.md)