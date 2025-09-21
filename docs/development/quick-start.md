# Quick Start Guide

## Prerequisites

- Node.js 16+
- Java 11+
- Maven 3.6+
- MySQL or PostgreSQL

## Installation

### 1. Clone Repository
```bash
git clone https://github.com/telcobright/orchestrix.git
cd orchestrix
```

### 2. Backend Setup
```bash
# Install dependencies
mvn clean install

# Configure database in application.properties
# Run backend
mvn spring-boot:run
```

### 3. Debug Store Setup
```bash
cd orchestrix-ui/debug-store
npm install
npm start
```

### 4. Frontend Setup
```bash
cd orchestrix-ui
npm install
npm start
```

## Verify Installation

### Check Services
- Backend: http://localhost:8090/api/health
- Frontend: http://localhost:3010
- WebSocket: ws://localhost:3013/store-debug

### Test Query
```bash
curl -X POST http://localhost:8090/api/stellar/query \
  -H "Content-Type: application/json" \
  -d '{"kind":"partner","criteria":{}}'
```

## Configuration

### Enable Debug Mode
```typescript
// orchestrix-ui/src/config/app.config.ts
storeDebug: {
  enabled: true  // Set to true
}
```

### Change Profile
```typescript
// orchestrix-ui/src/config/app.profile.ts
export const ACTIVE_PROFILE: ConfigProfile = 'development';
```

## First Steps

1. **Access the Application**
   - Open http://localhost:3010
   - Navigate to Infrastructure page

2. **Check Debug Logs**
   ```bash
   tail -f orchestrix-ui/debug-store/store-debug/store-events-*.jsonl
   ```

3. **Monitor Console**
   ```bash
   tail -f orchestrix-ui/debug-store/store-debug/console-logs-*.jsonl
   ```

## Common Commands

### Start All Services
```bash
# Terminal 1
mvn spring-boot:run

# Terminal 2
cd orchestrix-ui/debug-store && npm start

# Terminal 3
cd orchestrix-ui && npm start
```

### Clean Start
```bash
# Clear debug logs
rm orchestrix-ui/store-server/store-debug/*.jsonl

# Clear node modules
rm -rf orchestrix-ui/node_modules
rm -rf orchestrix-ui/store-server/node_modules

# Reinstall
npm install
```

## Troubleshooting

### Backend Not Starting
- Check Java version: `java -version`
- Check database connection
- Check port 8090 availability

### Frontend Not Loading
- Check Node version: `node -v`
- Clear browser cache
- Check port 3010 availability

### WebSocket Connection Failed
- Check store-server is running
- Verify port 3013 is open
- Check browser console for errors

### No Data Loading
- Verify backend is running
- Check network tab for API calls
- Review debug logs for errors

## Next Steps

- Read [Development Workflow](./development-workflow.md)
- Learn about [Adding Features](./adding-features.md)
- Review [Best Practices](./best-practices.md)