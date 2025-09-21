# Stellar Query Syntax

## Basic Query

```javascript
{
  kind: 'partner',
  criteria: { status: 'ACTIVE' }
}
```

## Query with Pagination

```javascript
{
  kind: 'compute',
  criteria: { datacenterId: 38 },
  page: {
    limit: 10,
    offset: 0
  }
}
```

## Query with Relations

```javascript
{
  kind: 'partner',
  include: ['environments', 'clouds', 'regions'],
  criteria: { type: 'service-provider' }
}
```

## Advanced Criteria

### Comparison Operators
```javascript
{
  kind: 'infrastructure',
  criteria: {
    createdAt: { $gte: '2025-01-01' },
    status: { $ne: 'DELETED' },
    priority: { $lt: 5 }
  }
}
```

### Array Operators
```javascript
{
  kind: 'partner',
  criteria: {
    region: { $in: ['us-east', 'us-west'] },
    tags: { $contains: 'premium' }
  }
}
```

### Logical Operators
```javascript
{
  kind: 'compute',
  criteria: {
    $or: [
      { status: 'ACTIVE' },
      { status: 'PENDING' }
    ]
  }
}
```

## Sorting

```javascript
{
  kind: 'partner',
  sort: {
    field: 'createdAt',
    order: 'DESC'
  }
}
```

## Complex Example

```javascript
{
  kind: 'infrastructure',
  criteria: {
    status: 'ACTIVE',
    type: { $in: ['compute', 'network'] },
    createdAt: { $gte: '2025-01-01' }
  },
  include: ['partner', 'environment'],
  page: { limit: 20, offset: 0 },
  sort: { field: 'name', order: 'ASC' }
}
```

## Related

- [Stellar Query Overview](./stellar-query-overview.md)
- [Stellar Mutations](./stellar-mutations.md)