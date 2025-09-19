# loadInfrastructureTree Usage Guide

## Function Signature
```typescript
async loadInfrastructureTree(partner: string = 'self'): Promise<void>
```

## Parameters

- **partner** (string, optional): Specifies which partner's infrastructure to load
  - Default: `'self'`

## Usage Examples

### 1. Load Organization's Own Infrastructure (Default)
```typescript
// These are equivalent:
await store.loadInfrastructureTree();
await store.loadInfrastructureTree('self');
```
This loads infrastructure where the partner has the role 'self' (organization's own infrastructure).

### 2. Load Specific Partner's Infrastructure by Name
```typescript
// Load infrastructure for a specific partner
await store.loadInfrastructureTree('partner-acme-corp');
await store.loadInfrastructureTree('cloudprovider-aws');
```
This loads infrastructure for a specific partner by their name.

### 3. Load All Partners' Infrastructure
```typescript
// Load infrastructure for all partners
await store.loadInfrastructureTree('all');
```
This loads infrastructure for all partners without any filtering.

### 4. Load by Partner ID
```typescript
// If you have a partner ID instead of name
const partnerId = 123;
await store.loadInfrastructureTree(`id:${partnerId}`);
```

## Enhanced Version with More Options

If you need more flexibility, here's an enhanced version you can implement:

```typescript
interface LoadInfrastructureOptions {
  partner?: string | number;  // Partner name, ID, 'self', or 'all'
  includeCompute?: boolean;   // Include compute resources
  includeNetwork?: boolean;   // Include network devices
  depth?: number;             // How deep to load the tree
  filter?: {
    status?: string[];        // Filter by status
    type?: string[];         // Filter by type
    datacenter?: number[];   // Filter by datacenter IDs
  };
}

async loadInfrastructureTreeAdvanced(options: LoadInfrastructureOptions = {}) {
  const {
    partner = 'self',
    includeCompute = true,
    includeNetwork = true,
    depth = 4,
    filter = {}
  } = options;

  // Build criteria based on partner parameter
  const criteria: any = {};
  
  if (partner === 'self') {
    criteria.roles = JSON.stringify(['self']);
  } else if (partner === 'all') {
    // No criteria filter for all partners
  } else if (typeof partner === 'number') {
    criteria.id = partner;
  } else if (typeof partner === 'string' && partner.startsWith('id:')) {
    criteria.id = parseInt(partner.substring(3));
  } else {
    criteria.name = partner;
  }

  // Add additional filters
  if (filter.status?.length) {
    criteria.status = filter.status;
  }
  if (filter.type?.length) {
    criteria.type = filter.type;
  }

  // Build include hierarchy dynamically
  const datacenterIncludes = [];
  if (includeCompute) {
    datacenterIncludes.push({ kind: 'compute' });
  }
  if (includeNetwork) {
    datacenterIncludes.push({ kind: 'networkdevice' });
  }

  const query: QueryNode = {
    kind: 'partner',
    ...(Object.keys(criteria).length > 0 && { criteria }),
    include: depth >= 1 ? [
      {
        kind: 'cloud',
        include: depth >= 2 ? [
          {
            kind: 'datacenter',
            ...(filter.datacenter && {
              criteria: { id: filter.datacenter }
            }),
            include: depth >= 3 ? datacenterIncludes : []
          }
        ] : []
      }
    ] : []
  };

  const data = await this.executeQuery(query);
  
  if (data) {
    runInAction(() => {
      this.treeData = transformStellarToTree(data);
      // Auto-expand based on depth
      this.autoExpandToDepth(depth);
    });
  }
}
```

## Use Cases

### Partner Management View
```typescript
// In a partner management component
const PartnerInfraView = ({ partnerId }) => {
  useEffect(() => {
    // Load specific partner's infrastructure
    store.loadInfrastructureTree(partnerId);
  }, [partnerId]);
  
  return <InfrastructureTree data={store.treeData} />;
};
```

### Organization Dashboard
```typescript
// Load own infrastructure on dashboard
useEffect(() => {
  store.loadInfrastructureTree('self');
}, []);
```

### Multi-Tenant View
```typescript
// Toggle between different views
const handleViewChange = (view: string) => {
  switch(view) {
    case 'own':
      store.loadInfrastructureTree('self');
      break;
    case 'partners':
      store.loadInfrastructureTree('all');
      break;
    case 'specific':
      store.loadInfrastructureTree(selectedPartnerName);
      break;
  }
};
```

## Database Query Generated

The function generates different SQL queries based on the partner parameter:

### For 'self':
```sql
SELECT * FROM partner 
WHERE roles LIKE '%"self"%'
-- Plus nested joins for cloud, datacenter, compute, networkdevice
```

### For specific partner name:
```sql
SELECT * FROM partner 
WHERE name = 'partner-name'
-- Plus nested joins
```

### For 'all':
```sql
SELECT * FROM partner
-- No WHERE clause, loads all partners
-- Plus nested joins
```

## Performance Considerations

1. **Use specific partners when possible** - Loading 'all' can be slow with many partners
2. **Consider pagination** - For large datasets, implement pagination at the partner level
3. **Use lazy loading** - For deep hierarchies, consider lazy-loading child nodes
4. **Cache results** - The store can cache results for frequently accessed partners

## Error Handling

The function handles errors internally, but you can wrap calls for specific handling:

```typescript
try {
  await store.loadInfrastructureTree(partnerName);
} catch (error) {
  console.error(`Failed to load infrastructure for ${partnerName}:`, error);
  // Show user-friendly error message
  notificationStore.error(`Unable to load ${partnerName} infrastructure`);
}
```