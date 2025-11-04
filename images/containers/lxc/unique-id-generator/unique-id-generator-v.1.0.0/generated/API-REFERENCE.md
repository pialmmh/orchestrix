# Unique ID Generator v1.0.0 - API Reference

## Base URL
```
http://<container-ip>:8080
```

## API Endpoints

### 1. Generate Single ID
**GET** `/api/next-id/:entityName?dataType=<type>`

Generates a single unique ID for the specified entity. Auto-registers entity on first use.

#### Parameters
- **entityName** (path) - Name of the entity (e.g., "user", "order", "session")
- **dataType** (query) - Type of ID to generate

#### Available Data Types
- `snowflake` - 64-bit time-ordered ID (recommended)
- `int` - 32-bit sequential integer
- `long` - 64-bit sequential integer
- `uuid8` - 8-character random string
- `uuid12` - 12-character random string
- `uuid16` - 16-character random string
- `uuid22` - 22-character random string

#### Response
```json
{
  "entityName": "user",
  "dataType": "snowflake",
  "value": "229599377285447680",
  "shard": 1
}
```

#### Examples
```bash
# Snowflake ID
curl 'http://10.10.199.51:8080/api/next-id/user?dataType=snowflake'

# Sequential integer
curl 'http://10.10.199.51:8080/api/next-id/order?dataType=int'

# UUID-16
curl 'http://10.10.199.51:8080/api/next-id/session?dataType=uuid16'
```

#### Error Responses
```json
// Type mismatch (entity exists with different type)
{
  "error": "Type mismatch",
  "message": "Entity 'user' is registered as 'snowflake', cannot use 'int'",
  "registeredType": "snowflake"
}

// Invalid data type
{
  "error": "Valid dataType is required",
  "validTypes": ["int", "long", "snowflake", "uuid8", "uuid12", "uuid16", "uuid22"]
}

// Integer overflow
{
  "error": "Integer overflow",
  "message": "Entity 'order' has reached maximum int value for shard 1"
}
```

---

### 2. Generate Batch of IDs
**GET** `/api/next-batch/:entityName?dataType=<type>&batchSize=<n>`

Generates multiple IDs in a single request.

#### Parameters
- **entityName** (path) - Name of the entity
- **dataType** (query) - Type of ID to generate
- **batchSize** (query) - Number of IDs to generate (1-10000)

#### Response
```json
{
  "entityName": "event",
  "dataType": "snowflake",
  "batchSize": 5,
  "values": [
    "229599957278003200",
    "229599957278003201",
    "229599957278003202",
    "229599957278003203",
    "229599957278003204"
  ],
  "shard": 1
}
```

#### Examples
```bash
# Batch of 100 Snowflake IDs
curl 'http://10.10.199.51:8080/api/next-batch/event?dataType=snowflake&batchSize=100'

# Batch of 50 sequential integers
curl 'http://10.10.199.51:8080/api/next-batch/invoice?dataType=int&batchSize=50'
```

#### Error Responses
```json
// Invalid batch size
{
  "error": "Valid batchSize is required",
  "message": "batchSize must be between 1 and 10000"
}

// Overflow in batch
{
  "error": "Overflow",
  "message": "Batch size 1000 would exceed maximum value for shard 1"
}
```

---

### 3. Parse Snowflake ID
**GET** `/api/parse-snowflake/:id`

Decodes a Snowflake ID to show its components.

#### Parameters
- **id** (path) - The Snowflake ID to parse

#### Response
```json
{
  "id": "229599957278003200",
  "timestamp": 1758808095576,
  "date": "2025-09-25T13:48:15.576Z",
  "shardId": 2,
  "sequence": 0,
  "binary": "0000001100101111101100111111000111010110000000000001000000000000"
}
```

#### Example
```bash
curl 'http://10.10.199.51:8080/api/parse-snowflake/229599957278003200'
```

#### Error Response
```json
{
  "error": "Invalid Snowflake ID",
  "message": "Invalid ID format"
}
```

---

### 4. Get Available Types
**GET** `/api/types`

Returns all available ID types and their descriptions.

#### Response
```json
{
  "availableTypes": [
    "int", "long", "snowflake",
    "uuid8", "uuid12", "uuid16", "uuid22"
  ],
  "description": {
    "int": "Sequential 32-bit integer (shard 1: 1, 4, 7...)",
    "long": "Sequential 64-bit integer (shard 1: 1, 4, 7...)",
    "snowflake": "64-bit time-ordered unique ID (timestamp + shard + sequence)",
    "uuid8": "Random 8-character alphanumeric string",
    "uuid12": "Random 12-character alphanumeric string",
    "uuid16": "Random 16-character alphanumeric string",
    "uuid22": "Random 22-character alphanumeric string"
  },
  "shardInfo": {
    "shardId": 1,
    "totalShards": 1
  },
  "snowflakeInfo": {
    "shardId": 1,
    "totalShards": 1,
    "epoch": "2024-01-01T00:00:00.000Z",
    "maxIdsPerMs": 4096,
    "maxShards": 1024,
    "bitsAllocation": {
      "timestamp": 41,
      "shardId": 10,
      "sequence": 12,
      "total": 64
    }
  }
}
```

#### Example
```bash
curl 'http://10.10.199.51:8080/api/types'
```

---

### 5. Health Check
**GET** `/health`

Returns service health status and statistics.

#### Response
```json
{
  "status": "healthy",
  "uptime": 3600.5,
  "shard": 1,
  "totalShards": 1,
  "snowflakeStats": {
    "generated": 15234,
    "collisions": 102,
    "waits": 0,
    "shardId": 1,
    "totalShards": 1,
    "maxIdsPerMs": 4096,
    "maxShards": 1024
  }
}
```

#### Example
```bash
curl 'http://10.10.199.51:8080/health'
```

---

## Data Type Details

### Snowflake IDs
64-bit unique identifiers with the following structure:
- **41 bits**: Timestamp (milliseconds since 2024-01-01)
- **10 bits**: Shard ID (supports 1024 shards)
- **12 bits**: Sequence number (4096 IDs per millisecond)
- **1 bit**: Reserved (sign bit)

**Features:**
- Time-ordered (sortable by creation time)
- Globally unique across shards
- No coordination required between shards
- 4096 IDs per millisecond per shard
- Can run for 69 years from epoch

**Example:** `229599377285447680`

### Sequential IDs (int/long)
Traditional incrementing counters with sharding support.

**Sharding Pattern (Interleaved):**
- Shard 1: 1, 4, 7, 10, 13...
- Shard 2: 2, 5, 8, 11, 14...
- Shard 3: 3, 6, 9, 12, 15...

**Limits:**
- `int`: 0 to 2,147,483,647
- `long`: 0 to 9,223,372,036,854,775,807

### UUID Types
Random alphanumeric strings using shard-aware generation.

**Formats:**
- `uuid8`: 8 characters (e.g., "aB3xY9kL")
- `uuid12`: 12 characters (e.g., "aB3xY9kLmN2p")
- `uuid16`: 16 characters (e.g., "aB3xY9kLmN2pQ4rS")
- `uuid22`: 22 characters (e.g., "aB3xY9kLmN2pQ4rSt6uVwX")

**Note:** These are legacy types. Use Snowflake IDs for better performance and features.

---

## CLI Tool Reference

The `uid-cli` tool is available inside the container for maintenance tasks.

### Commands

#### List All Entities
```bash
uid-cli list
```

#### Check Entity Status
```bash
uid-cli status <entity-name>
```

#### Initialize Entity
```bash
uid-cli init <entity-name> <type> [start-value]

# Examples
uid-cli init product snowflake
uid-cli init order int 1000
```

#### Reset Counter
```bash
uid-cli reset <entity-name> <value>

# Example
uid-cli reset order 5000
```

#### Delete Entity
```bash
uid-cli delete <entity-name>
```

#### Clear All Entities
```bash
uid-cli clear --force
```

#### System Information
```bash
uid-cli info
```

#### Help
```bash
uid-cli help
uid-cli help <command>
```

### Environment Variables
- `DATA_DIR` - Data directory (default: /var/lib/unique-id-generator)
- `SHARD_ID` - Shard identifier (default: 1)
- `TOTAL_SHARDS` - Total number of shards (default: 1)

---

## Error Codes

| Code | Description |
|------|-------------|
| 400 | Bad Request - Invalid parameters |
| 404 | Not Found - Endpoint doesn't exist |
| 500 | Internal Server Error - Overflow or system error |

---

## Rate Limits

No built-in rate limits. The service can handle:
- **Snowflake IDs**: Up to 4096 IDs per millisecond per shard
- **Sequential IDs**: Limited by CPU and I/O for state persistence
- **UUID types**: Limited by random generation speed

---

## Best Practices

1. **Use Snowflake IDs** for most use cases
2. **Use batch generation** for bulk operations
3. **Deploy multiple shards** for high throughput (>4096 IDs/ms)
4. **Monitor health endpoint** for performance metrics
5. **Use consistent entity names** across your application
6. **Don't mix ID types** for the same entity

---

## Migration Notes

When migrating from UUID-based IDs to Snowflake:
1. Create new entities with snowflake type
2. Keep old UUID entities for backward compatibility
3. Gradually migrate references in your application
4. Use CLI to manage entity lifecycle