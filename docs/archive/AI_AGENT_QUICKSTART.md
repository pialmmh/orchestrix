# AI Agent Quick Start Guide - Telcobright Admin-UI Framework

## Overview
You're working with the Telcobright Admin-UI Framework - an enterprise application framework specifically designed for building administrative interfaces, ERP/CRM systems, and cloud automation tools with sophisticated management UIs. This is one of several frameworks in the Telcobright ecosystem. Orchestrix (cloud infrastructure management) is just one app built on this Admin-UI framework.

## Key Concepts to Understand

### 1. Debug Mode vs Production Mode
- **Debug Mode** (`store_debug: true`): Backend manages all state, everything is logged
- **Production Mode** (`store_debug: false`): Frontend manages state locally
- Check mode in: `orchestrix-ui/src/config/app.config.ts`

### 2. Three-Layer Architecture
```
Frontend (React) ←→ Store Server (WebSocket) ←→ Backend (Spring Boot)
```

### 3. Key Files You'll Work With

#### Configuration
- `orchestrix-ui/src/config/app.profile.ts` - Profile selector (dev/staging/prod)
- `orchestrix-ui/src/config/app.config.ts` - All app configuration

#### Services (Framework Core)
- `orchestrix-ui/src/services/StoreWebSocketService.js` - WebSocket communication
- `orchestrix-ui/src/services/ConsoleRedirectService.ts` - Console capture
- `orchestrix-ui/store-server/services/EventBusWithMutations.js` - Event handling

#### Stores (State Management)
- `orchestrix-ui/src/stores/` - MobX stores for state
- `orchestrix-ui/src/unified-store/RootStore.ts` - Root store manager

## Common Tasks

### 1. Running the Application
```bash
# Terminal 1: Backend
mvn spring-boot:run

# Terminal 2: Store Server
cd orchestrix-ui/store-server
npm start

# Terminal 3: Frontend
cd orchestrix-ui
npm start
```

### 2. Adding a New Feature
1. Create/modify store in `orchestrix-ui/src/stores/`
2. Add API endpoint in Spring Boot if needed
3. Connect via WebSocket events in `EventBusWithMutations.js`
4. Update UI components

### 3. Debugging Issues
1. Check console in browser DevTools
2. Look at `orchestrix-ui/store-server/store-debug/` for:
   - `console-logs-*.jsonl` - Frontend console output
   - `store-events-*.jsonl` - All WebSocket events
3. Check Spring Boot console for backend errors

### 4. Making a Query (Stellar System)
```javascript
// Frontend query example
const query = {
  kind: 'partner',  // REQUIRED in V2
  criteria: { status: 'ACTIVE' },
  page: { limit: 10, offset: 0 }
};
```

### 5. WebSocket Events
- `QUERY` - Fetch data
- `MUTATION` - Create/Update/Delete
- `SUBSCRIBE` - Real-time updates
- `CONSOLE_LOGS` - Console output (debug)

## Important Design Patterns

### 1. Event-Driven Updates
All state changes go through events:
```
User Action → Store → WebSocket → Backend → Database → Broadcast to Subscribers
```

### 2. Proxy Pattern in Debug Mode
When `store_debug: true`:
- Frontend stores are proxies
- Real state lives in backend
- All actions are logged

### 3. Configuration Profiles
- Development: Full debugging
- Staging: Moderate debugging
- Production: Minimal logging

## Common Pitfalls to Avoid

1. **Don't hardcode URLs** - Use config files
2. **Don't skip the 'kind' field** in Stellar queries
3. **Don't create files unnecessarily** - Edit existing ones
4. **Don't send console logs too frequently** - It overwhelms WebSocket

## File Structure
```
orchestrix/
├── docs/                              # Documentation
│   └── TELCOBRIGHT_FRAMEWORK_ARCHITECTURE.md
├── backend/                           # Spring Boot
│   └── src/main/java/com/orchestrix/
├── orchestrix-ui/                     # React Frontend
│   ├── src/
│   │   ├── config/                   # Configuration
│   │   ├── services/                 # Framework services
│   │   ├── stores/                   # State management
│   │   ├── pages/                    # Application pages
│   │   └── components/               # UI components
│   └── store-server/                 # WebSocket server
│       ├── services/
│       │   └── EventBusWithMutations.js
│       └── store-debug/              # Debug logs
```

## Quick Debugging Commands

```bash
# Check if services are running
curl http://localhost:8090/api/health  # Backend
curl http://localhost:3010            # Frontend

# View recent console logs
tail orchestrix-ui/store-server/store-debug/console-logs-*.jsonl | jq

# Check store events
ls -la orchestrix-ui/store-server/store-debug/store-events-*.jsonl

# Test backend query
curl -X POST http://localhost:8090/api/stellar/query \
  -H "Content-Type: application/json" \
  -d '{"kind":"partner","criteria":{}}'
```

## Framework vs Application

### Framework (Reusable)
- WebSocket event bus
- Stellar query system
- Debug store mechanism
- Console redirect
- Configuration profiles
- Base stores and services

### Application (Business Logic)
- Orchestrix: Cloud infrastructure management
- Future: CRM, ERP, etc.
- Specific stores (PartnerStore, InfrastructureStore)
- Business UI components

## Need More Context?
Read the full architecture document: `docs/TELCOBRIGHT_ADMIN_UI_FRAMEWORK_ARCHITECTURE.md`

## Remember
- This is the Admin-UI framework, part of the Telcobright ecosystem
- Designed specifically for administrative and management interfaces
- Debug mode is powerful - use it
- Everything is event-driven
- Console redirect should only capture intentional logs
- The framework is designed for multiple admin/management applications

## Other Telcobright Frameworks
- **Admin-UI Framework** (this one): Admin panels and management UIs
- **Analytics Framework**: Data analytics and reporting
- **Integration Framework**: System integration and APIs
- **Mobile Framework**: Mobile application development