# Design Philosophy

## Core Principles

### 1. Separation of Concerns

The framework enforces clear boundaries between:
- **Framework Code**: Reusable infrastructure
- **Application Code**: Business logic
- **Configuration**: Environment settings

### 2. Event-Driven Architecture

All state changes and communication flow through events:
- Provides audit trail
- Enables debugging
- Supports real-time updates
- Decouples components

### 3. Developer Experience First

The framework prioritizes developer productivity:
- Comprehensive debugging tools
- Hot-reload support
- Clear error messages
- Extensive logging

### 4. Configuration Over Code

Behavior can be modified through configuration:
- Multiple environment profiles
- Runtime overrides
- Feature flags
- No hardcoded values

### 5. Observability by Design

Every action is observable:
- All events are logged
- Console output captured
- Performance metrics tracked
- State changes recorded

## Design Patterns

### Proxy Pattern
In debug mode, frontend stores are proxies to backend stores, ensuring single source of truth.

### Observer Pattern
Components subscribe to store changes for reactive updates.

### Command Pattern
All mutations are commands sent through the event bus.

### Factory Pattern
Stores are created through factory methods for consistency.

## Architectural Decisions

### Why WebSocket?
- Real-time bidirectional communication
- Lower overhead than polling
- Persistent connection
- Event streaming support

### Why MobX?
- Simple reactive state management
- Less boilerplate than Redux
- Better TypeScript support
- Automatic dependency tracking

### Why Event Bus?
- Centralized communication
- Loose coupling
- Easy debugging
- Plugin architecture

### Why JSONL Logs?
- Line-delimited for streaming
- Human readable
- Easy to parse
- Append-only performance

## Trade-offs

### Chosen
- **Flexibility** over simplicity
- **Debuggability** over performance
- **Consistency** over customization
- **Explicit** over implicit

### Accepted Limitations
- Initial learning curve
- Additional abstraction layers
- Larger bundle size
- WebSocket dependency