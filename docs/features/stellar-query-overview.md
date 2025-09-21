# Stellar Query Overview

## What is Stellar?

Stellar is a unified query system that provides a consistent JSON-based interface for all data operations across the Orchestrix application, abstracting database complexities into simple, declarative queries.

## Core Concepts

- **Unified Interface**: Single query format for all entities
- **Entity-Based**: All queries specify an entity type (`kind`)
- **Operation Types**: Supports queries (READ) and mutations (CREATE, UPDATE, DELETE)
- **Backend Agnostic**: Works with any database through adapters

## Query Structure

```typescript
interface StellarQuery {
  kind: string;           // Entity type (REQUIRED)
  criteria?: object;      // Filter conditions
  page?: {               // Pagination
    limit: number;
    offset: number;
  };
  include?: string[];    // Related entities
  sort?: {              // Sorting
    field: string;
    order: 'ASC' | 'DESC';
  };
}
```

## Supported Entities

- `partner` - Partner organizations
- `environment` - Deployment environments
- `compute` - Compute resources
- `infrastructure` - Infrastructure components
- `country` - Country definitions
- `cloud` - Cloud providers
- `region` - Geographic regions

## Related Documentation

- [Stellar Query Syntax](./stellar-query-syntax.md)
- [Stellar Mutations](./stellar-mutations.md)
- [Stellar Migration Guide](./stellar-migration-v1-to-v2.md)