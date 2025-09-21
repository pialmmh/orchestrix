# Telcobright Admin-UI Framework Overview

## Introduction

The Telcobright Admin-UI Framework is an enterprise-grade framework designed specifically for building sophisticated administrative interfaces, management dashboards, and Line of Business (LOB) applications.

## Purpose

This framework provides a standardized approach for building:
- Administrative dashboards
- ERP/CRM systems
- Cloud and network management tools
- Infrastructure monitoring applications
- Configuration management interfaces

## Position in Telcobright Ecosystem

The Admin-UI Framework is one of several specialized frameworks in the Telcobright ecosystem:

```
Telcobright Ecosystem
├── Admin-UI Framework (this framework)
│   └── For administrative interfaces
├── Analytics Framework
│   └── For data analysis and reporting
├── Integration Framework
│   └── For system integration and APIs
└── Mobile Framework
    └── For mobile applications
```

## Key Characteristics

1. **Event-Driven**: All state changes propagate through events
2. **Real-Time**: WebSocket-based communication for instant updates
3. **Debuggable**: Comprehensive debugging with store-debug mode
4. **Configurable**: Multi-profile configuration system
5. **Extensible**: Plugin architecture for new features

## Framework vs Application

### Framework Layer (Reusable)
- Event bus system
- WebSocket communication
- State management patterns
- Debug infrastructure
- Configuration system
- UI theme and components

### Application Layer (Custom)
- Business logic
- Domain-specific stores
- Custom UI components
- Application routes
- Business rules

## Applications Built on This Framework

### Current
- **Orchestrix**: Cloud infrastructure management and automation

### Planned
- **CRM System**: Customer relationship management
- **ERP Module**: Enterprise resource planning
- **Network Automation**: Network configuration and monitoring
- **Service Desk**: IT service management

## Next Steps

- Learn about [Design Philosophy](./design-philosophy.md)
- Review the [Technology Stack](./technology-stack.md)
- Start with [Quick Start Guide](../development/quick-start.md)