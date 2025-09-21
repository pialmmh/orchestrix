# Stellar Migration: V1 to V2

## Key Changes

### Required `kind` Field
**V1**: Entity type was optional or inferred
```javascript
// V1 - No explicit entity type
{ criteria: { status: 'ACTIVE' } }
```

**V2**: Entity type is mandatory
```javascript
// V2 - Explicit kind required
{ kind: 'partner', criteria: { status: 'ACTIVE' } }
```

### Endpoint Changes
- **V1**: `/api/stellar/query-v1`
- **V2**: `/api/stellar/query`

### Controller Updates
- Removed: `StellarController.java`
- Active: `StellarV2Controller.java`

## Migration Steps

1. **Update Queries**: Add `kind` field to all queries
2. **Update Endpoints**: Change API URLs to V2 endpoints
3. **Update Stores**: Use new query format in store methods
4. **Test Thoroughly**: Verify all CRUD operations work

## Common Migration Issues

### Missing `kind` Field
**Error**: `Missing 'kind' field in query`
**Fix**: Add appropriate entity type to query

### Wrong Endpoint
**Error**: `404 Not Found`
**Fix**: Update to `/api/stellar/query` endpoint

### Controller Conflict
**Error**: `Ambiguous mapping`
**Fix**: Remove old StellarController.java

## Backwards Compatibility

V2 is NOT backwards compatible with V1. All clients must be updated to use the new format.

## Related

- [Stellar Query Overview](./stellar-query-overview.md)
- [Stellar Query Syntax](./stellar-query-syntax.md)