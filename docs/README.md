# Orchestrix Documentation

## Overview
Orchestrix is an infrastructure orchestration application built on the Telcobright Admin-UI Framework, providing comprehensive infrastructure management with real-time updates and debugging capabilities.

## Quick Navigation

### 🚀 Getting Started
- [Quick Start Guide](./development/quick-start.md) - Get up and running quickly
- [Development Workflow](./development/development-workflow.md) - Development best practices

### 🏗️ Architecture
- [Event-Driven Architecture](./architecture/event-driven-architecture.md) - Real-time event system
- [Store Architecture](./architecture/store-architecture.md) - State management design
- [WebSocket Communication](./architecture/websocket-communication.md) - Real-time messaging

### ⚙️ Configuration
- [Profile Management](./configuration/profile-management.md) - Environment profiles
- [Application Configuration](./configuration/application-configuration.md) - Config settings

### 🌟 Core Features

#### Stellar Query System
- [Stellar Overview](./features/stellar-query-overview.md) - Unified query language
- [Query Syntax](./features/stellar-query-syntax.md) - Query format and operators
- [Mutations](./features/stellar-mutations.md) - Create, Update, Delete operations
- [Migration Guide](./features/stellar-migration-v1-to-v2.md) - V1 to V2 migration

#### Debug Store System
- [Debug Store Overview](./features/debug-store-overview.md) - Debugging capabilities
- [Configuration](./features/debug-store-configuration.md) - Setup and options
- [Log Format](./features/debug-store-log-format.md) - Understanding log structure
- [Usage Guide](./features/debug-store-usage.md) - Common commands and patterns

#### Console Redirect
- [Console Log Redirect](./features/console-log-redirect.md) - Frontend logging capture

### 🛠️ Framework
- [Framework Overview](./framework/overview.md) - Telcobright Admin-UI Framework
- [Design Philosophy](./framework/design-philosophy.md) - Core principles

### 📦 API Reference
- [REST Endpoints](./api/rest-endpoints.md) - Backend API documentation
- [WebSocket API](./api/websocket-api.md) - Real-time API

### 🗄️ Archive
[Previous documentation versions](./archive/) for historical reference

## Documentation Principles

This documentation follows the **Single Responsibility Principle**:
- **One topic per file** - Each document covers a specific topic
- **Clear categories** - Organized in logical folders
- **Cross-references** - Related topics are linked
- **Practical examples** - Working code samples included

## Technology Stack

- **Frontend**: React, TypeScript, MobX, Material-UI
- **Backend**: Spring Boot (Java), Stellar Query System
- **Real-time**: WebSocket, EventBus
- **Debugging**: Debug Store with JSONL logging
- **Database**: MySQL/PostgreSQL with JPA

## Key Concepts

1. **Stellar Queries** - All data operations use unified JSON queries
2. **Event-Driven Updates** - Real-time state synchronization via WebSocket
3. **Debug Store** - Comprehensive debugging with event logging
4. **Profile-Based Config** - Environment-specific configurations

## Development Checklist

Before starting development:
- [ ] Read [Quick Start Guide](./development/quick-start.md)
- [ ] Understand [Stellar Query System](./features/stellar-query-overview.md)
- [ ] Configure [Debug Store](./features/debug-store-configuration.md) for development
- [ ] Review [Profile Management](./configuration/profile-management.md)

## Contributing

See [Development Workflow](./development/development-workflow.md) for contribution guidelines and coding standards.

## Support

For issues or questions:
1. Check relevant documentation section
2. Review [Archive](./archive/) for historical context
3. Contact the development team

---

*Documentation follows Telcobright Software Development Guidelines - see `/home/mustafa/telcobright-projects/ai-doc-guideline/ai-doc-guideline-software-projects.txt`*