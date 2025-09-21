# Orchestrix Store Debug Server

WebSocket server for event-driven architecture in debug mode. All store operations are processed server-side.

## Architecture

When debug mode is enabled:
1. Frontend sends all events through WebSocket to this server
2. Server processes queries/mutations against Stellar backend
3. Server broadcasts responses back to all connected clients
4. Multiple clients can connect for monitoring/debugging

## Installation

```bash
cd store-server
npm install
```

## Running

```bash
# Default (port 3013)
npm start

# Custom port
STORE_SERVER_PORT=4000 npm start

# Custom Stellar API
STELLAR_API_URL=http://localhost:8090/api npm start

# Development mode with auto-restart
npm run dev
```

## WebSocket Events

### Request Events
- `type: 'query', operation: 'REQUEST'` - Query request
- `type: 'mutation', operation: 'REQUEST'` - Mutation request

### Response Events
- `type: 'query', operation: 'RESPONSE'` - Query response
- `type: 'mutation', operation: 'RESPONSE'` - Mutation response

### System Events
- `type: 'system', operation: 'CONNECTED'` - Client connected
- `type: 'system', operation: 'PING/PONG'` - Heartbeat
- `type: 'system', operation: 'ERROR'` - Error occurred

## Frontend Configuration

Enable debug mode in frontend:
```javascript
// In localStorage or environment
{
  "store_debug": true,
  "eventbus": "websocket",
  "websocket_url": "ws://localhost:3013/store-debug"
}
```

## Features

- Centralized store processing
- Event logging for debugging
- Multi-client support for monitoring
- Automatic error handling
- Graceful shutdown