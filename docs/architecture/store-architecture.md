# Store Architecture

## Overview

Orchestrix uses MobX-based stores for state management with a unified architecture that works in both frontend and backend environments.

## Store Hierarchy

```
RootStore
├── PartnerStore
├── EnvironmentStore
├── ComputeStore
├── InfrastructureStore
└── CountryStore
```

## Base Store Pattern

All stores extend from `BaseStore`:

```typescript
class BaseStore {
  protected api: ApiService
  protected eventBus: EventBus

  async query(stellarQuery: StellarQuery)
  async mutate(mutation: StellarMutation)
  subscribe(event: string, handler: Function)
}
```

## Store Modes

### Production Mode
- Direct state management
- Optimistic updates
- Local caching

### Debug Mode
- Stores become proxies
- Backend manages state
- All changes logged
- WebSocket synchronization

## Event-Driven Updates

Stores automatically update via WebSocket events:
1. Mutation occurs
2. Backend broadcasts event
3. Stores receive notification
4. Affected stores refresh

## Related

- [Event-Driven Architecture](./event-driven-architecture.md)
- [WebSocket Communication](./websocket-communication.md)