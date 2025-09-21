# Orchestrix & Telcobright Admin-UI Framework Documentation

## Overview
This documentation covers both the **Telcobright Admin-UI Framework** and the **Orchestrix** application built on top of it.

## Documentation Structure

### 📁 [Framework](./framework/)
Core framework concepts and overview
- [Overview](./framework/overview.md) - Framework introduction and ecosystem
- [Design Philosophy](./framework/design-philosophy.md) - Core principles and patterns
- [Technology Stack](./framework/technology-stack.md) - Technologies and libraries used

### 📁 [Architecture](./architecture/)
System architecture and design patterns
- [System Architecture](./architecture/system-architecture.md) - Three-tier architecture overview
- [Event-Driven Architecture](./architecture/event-driven-architecture.md) - Event bus and messaging
- [Data Flow](./architecture/data-flow.md) - Query and mutation flows
- [State Management](./architecture/state-management.md) - MobX stores and patterns

### 📁 [Features](./features/)
Framework features and capabilities
- [Stellar Query System](./features/stellar-query-system.md) - Unified query language
- [Debug Store System](./features/debug-store-system.md) - Development debugging
- [Console Redirect](./features/console-redirect.md) - Production console capture
- [WebSocket Events](./features/websocket-events.md) - Real-time communication

### 📁 [Configuration](./configuration/)
Configuration and environment management
- [Profile Management](./configuration/profile-management.md) - Dev/Staging/Prod profiles
- [Environment Configuration](./configuration/environment-configuration.md) - Config structure
- [Runtime Overrides](./configuration/runtime-overrides.md) - Dynamic configuration

### 📁 [Development](./development/)
Development guides and best practices
- [Quick Start](./development/quick-start.md) - Getting started guide
- [Development Workflow](./development/development-workflow.md) - Local development setup
- [Adding New Features](./development/adding-features.md) - Extension guide
- [Debugging Guide](./development/debugging-guide.md) - Troubleshooting tips
- [Best Practices](./development/best-practices.md) - Coding standards

### 📁 [API](./api/)
API documentation and references
- [WebSocket API](./api/websocket-api.md) - WebSocket event reference
- [Stellar API](./api/stellar-api.md) - Query language reference
- [REST API](./api/rest-api.md) - Backend endpoints

### 📁 [Applications](./applications/)
Applications built on the framework
- [Orchestrix Overview](./applications/orchestrix-overview.md) - Cloud infrastructure management
- [Future Applications](./applications/future-applications.md) - Planned applications

## Quick Links

- 🚀 **New to the project?** Start with [Quick Start Guide](./development/quick-start.md)
- 🏗️ **Understanding the framework?** Read [Framework Overview](./framework/overview.md)
- 🔧 **Need to debug?** Check [Debugging Guide](./development/debugging-guide.md)
- 📝 **Adding features?** See [Adding New Features](./development/adding-features.md)

## Telcobright Ecosystem

The Admin-UI Framework is part of the larger Telcobright ecosystem:
- **Admin-UI Framework** - Administrative interfaces (this framework)
- **Analytics Framework** - Data analytics and reporting
- **Integration Framework** - System integration and APIs
- **Mobile Framework** - Mobile application development

## Contributing

When adding documentation:
1. Place documents in the appropriate category folder
2. Follow single responsibility - one topic per file
3. Use clear, descriptive filenames
4. Update the category README if needed
5. Keep documents focused and concise