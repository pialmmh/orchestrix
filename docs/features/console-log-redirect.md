# Console Log Redirect

## Overview

The `ws.log()` helper method provides a simple way to log messages that:
1. Always calls `console.log()` for browser console output
2. Sends the log to the WebSocket debug backend when connected

## Usage

### Global Method
```javascript
// Available globally after app initialization
ws.log('User clicked button', { buttonId: 'submit' });
ws.log('API response:', response.data);
ws.log('Error occurred:', error.message);
```

### Module Import
```javascript
// Can also import for use in modules
import { wsLog } from '../utils/wsLogger';

wsLog('Component mounted');
```

## How It Works

1. **Initialization**: WSLogger connects to debug WebSocket on app start
2. **Dual Output**: Every `ws.log()` call outputs to both:
   - Browser console (via `console.log`)
   - WebSocket backend (if connected)
3. **Automatic Serialization**: Objects are automatically stringified
4. **Session Tracking**: Each session gets a unique ID for tracking

## Configuration

The feature respects the debug store configuration:
```typescript
// app.config.ts
storeDebug: {
  enabled: true  // Must be true for WebSocket logging
}
```

## WebSocket Message Format

```json
{
  "type": "console",
  "level": "log",
  "timestamp": 1234567890,
  "isoTimestamp": "2025-09-20T10:00:00Z",
  "message": "Formatted message string",
  "args": ["original", "arguments"],
  "source": "frontend",
  "sessionId": "session-123-abc"
}
```

## Backend Storage

Logs are written to JSONL files:
```
debug-store/store-debug/console-logs-YYYY-MM-DD.jsonl
```

## View Logs

```bash
# View real-time logs
tail -f debug-store/store-debug/console-logs-*.jsonl | jq -r '.message'

# Search for specific messages
grep "button" debug-store/store-debug/console-logs-*.jsonl | jq
```

## Benefits

- **Simple API**: Just use `ws.log()` instead of `console.log()`
- **No Batching**: Immediate transmission, no delays
- **No Overhead**: Falls back to console.log if WebSocket unavailable
- **Debug Only**: Automatically disabled in production
- **Session Tracking**: Track logs by user session

## Example

```javascript
// In your React component
function MyComponent() {
  const handleClick = () => {
    ws.log('Button clicked at', new Date().toISOString());
    // Your logic here
  };

  useEffect(() => {
    ws.log('MyComponent mounted');
    return () => ws.log('MyComponent unmounted');
  }, []);

  return <button onClick={handleClick}>Click Me</button>;
}
```

## Notes

- No periodic sending of browser events
- No automatic console interception
- Only sends when you explicitly call `ws.log()`
- WebSocket connection is managed automatically
- Fails silently if WebSocket is unavailable