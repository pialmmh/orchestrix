# Debug Store Overview

## What is Debug Store?

Debug Store is a development-time debugging system that logs all application events, state changes, and console output to persistent JSONL files for comprehensive debugging and analysis.

## Core Capabilities

- **Event Logging**: Captures all WebSocket events and state mutations
- **Console Capture**: Records frontend console.log output
- **State History**: Maintains complete state change timeline
- **Performance Metrics**: Tracks query execution times
- **Error Context**: Preserves full error details with stack traces

## When to Use

Debug Store is designed for:
- Development and debugging
- Reproducing user-reported issues
- Performance analysis
- State inconsistency troubleshooting
- Integration testing

## Architecture

```
Frontend (React) <-> WebSocket <-> Debug Store Server <-> Backend API
                          |
                          v
                    JSONL Log Files
```

## Related Documentation

- [Debug Store Configuration](./debug-store-configuration.md)
- [Debug Store Log Format](./debug-store-log-format.md)
- [Debug Store Usage Guide](./debug-store-usage.md)