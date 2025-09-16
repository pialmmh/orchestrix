# Orchestrix Data Model Evolution

## Project Vision

Orchestrix is evolving to become a comprehensive **cloud inventory management and automation platform** that can eventually serve as an alternative to Infrastructure as Code (IaC) tools like Terraform. The platform aims to support organizations ranging from small businesses with a single server to large cloud providers managing global infrastructure.

## Primary Goals

1. **Inventory Management** (Current Focus)
   - Track all cloud and on-premise resources
   - Support multi-cloud environments (AWS, Azure, GCP, on-prem)
   - Handle varying scales (1 server to thousands)

2. **Simple Automation** (Next Phase)
   - Docker container deployments
   - Basic orchestration tasks
   - Resource provisioning

3. **Full IaC Platform** (Future Vision)
   - Complete Terraform alternative
   - State management
   - Dependency resolution
   - Multi-provider support

## Current Data Model

### Hierarchical Structure
```
Partner (Organization)
└── Cloud
    └── Region
        └── Availability Zone
            └── Datacenter
                └── Compute
                    └── Container
```

### Environment Association (Current Implementation)
- Datacenters have a single `environment_id` field
- Environments: Production, Staging, Development, Testing
- UI displays environment in datacenter names: "Dhaka-DC1 (PRODUCTION)"
- Environment filter allows viewing resources by environment

## Data Model Evolution Plan

### Phase 1: Flexible Environment Association (Immediate Need)

#### Problem Statement
- A single compute resource may host multiple environments
- Small organizations might run prod/dev on same server
- Large organizations need strict environment separation
- Kubernetes clusters use namespaces for environment isolation

#### Proposed Solution: Multi-Level Environment Association

```sql
-- Environments can be associated at ANY level
CREATE TABLE environment_associations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    environment_id INT,
    resource_type VARCHAR(50),  -- 'datacenter', 'compute', 'container', 'k8s_namespace'
    resource_id BIGINT,
    is_primary BOOLEAN DEFAULT false,
    allocated_percentage INT,    -- For shared resources
    metadata JSON,               -- Flexible additional info
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (environment_id) REFERENCES environments(id),
    UNIQUE KEY (environment_id, resource_type, resource_id)
);
```

### Phase 2: Workload Tracking

#### Purpose
Track what's actually running on compute resources and how environments are isolated.

```sql
-- Track actual workloads (containers, VMs, processes)
CREATE TABLE compute_workloads (
    id BIGINT PRIMARY KEY,
    compute_id BIGINT,
    environment_id INT,
    workload_type VARCHAR(50), -- 'container', 'vm', 'k8s_pod', 'process'
    workload_name VARCHAR(255),
    workload_id VARCHAR(255),   -- Container ID, VM UUID, etc.

    -- Resource allocation
    cpu_cores DECIMAL(5,2),
    memory_mb INT,
    disk_gb INT,

    -- Networking
    ip_address VARCHAR(45),
    ports JSON,

    -- Isolation details
    isolation_method VARCHAR(50), -- 'docker', 'k8s_namespace', 'vm', 'user'
    isolation_details JSON,

    status VARCHAR(50),
    created_at TIMESTAMP
);
```

### Phase 3: Resource Tagging

```sql
-- Universal tagging for any resource
CREATE TABLE resource_tags (
    resource_type VARCHAR(50),
    resource_id BIGINT,
    tag_key VARCHAR(100),
    tag_value VARCHAR(255),
    PRIMARY KEY (resource_type, resource_id, tag_key)
);
```

### Phase 4: Automation Support

```sql
-- Track automation capabilities
CREATE TABLE compute_capabilities (
    compute_id BIGINT,
    capability_type VARCHAR(50), -- 'docker', 'kubernetes', 'ansible'
    capability_version VARCHAR(50),
    configuration JSON,
    PRIMARY KEY (compute_id, capability_type)
);

-- Automation targets
CREATE TABLE automation_targets (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255),
    target_type VARCHAR(50),
    target_id BIGINT,
    connection_info JSON,
    capabilities JSON,
    status VARCHAR(50)
);
```

## Real-World Isolation Mechanisms

### 1. Container-Based (Docker)
- Different networks per environment
- Port mapping separation
- Resource limits (CPU/memory quotas)
- Volume isolation

### 2. Kubernetes Namespaces
- Logical separation within cluster
- Resource quotas per namespace
- Network policies
- RBAC per environment

### 3. Virtual Machines
- Complete OS isolation
- Dedicated resources
- Network isolation

### 4. Process-Level
- Different users
- Different ports
- systemd services

### 5. Directory-Based
- Web server vhosts
- Separate directories
- Different domains/subdomains

## Migration Strategy

### Step 1: Add Association Table (No Breaking Changes)
```sql
-- Add new table
CREATE TABLE environment_associations (...);

-- Migrate existing data
INSERT INTO environment_associations (environment_id, resource_type, resource_id, is_primary)
SELECT environment_id, 'datacenter', id, true
FROM datacenters
WHERE environment_id IS NOT NULL;
```

### Step 2: Update API to Check Both
- Check `datacenters.environment_id` (legacy)
- Check `environment_associations` (new)
- Gradually migrate to new model

### Step 3: Update UI
- Show environment badges on resources
- Support filtering by environment at any level
- Display resource sharing percentages

## UI/UX Evolution

### Current View
```
Tree: Partner → Cloud → Region → AZ → Datacenter (ENV) → Compute
Filter: By Environment Type
```

### Future View
```
Infrastructure View (Physical):
Partner → Cloud → Region → AZ → Datacenter → Compute [env badges]

Environment View (Logical):
Production
├── Dedicated: DC-1 (100%)
├── Shared: Server-1 (60%)
└── Containers: 15 containers

Automation View:
Targets → Capabilities → Deployments
```

## Key Design Principles

1. **Backward Compatibility**: Don't break existing code
2. **Flexibility**: Support any organization size
3. **Reality-Based**: Model how infrastructure actually works
4. **Extensible**: Easy to add new resource types
5. **Automation-Ready**: Support future automation features

## Implementation Priority

1. ✅ Basic hierarchy and environment display (DONE)
2. 🚧 Flexible environment associations (NEXT)
3. ⏳ Workload tracking
4. ⏳ Resource tagging
5. ⏳ Simple automation (Docker deployments)
6. ⏳ Advanced automation (IaC features)

## Database Schema Compatibility

The proposed changes are **additive** - they don't modify existing tables, only add new ones. This ensures:
- No data loss
- No breaking changes
- Gradual migration possible
- Rollback capability

## For AI Agents

When working on this project:

1. **Current State**:
   - Hierarchical inventory working
   - Environments shown on datacenters
   - Basic filtering implemented

2. **Next Steps**:
   - Implement `environment_associations` table
   - Add workload tracking
   - Build simple Docker deployment automation

3. **Keep in Mind**:
   - Support scales from 1 server to cloud-provider scale
   - Environments can be shared on single compute
   - Model must reflect real-world practices
   - Don't over-engineer, but stay extensible

4. **Testing Scenarios**:
   - Small: 1 server, all environments
   - Medium: Multiple DCs, mixed environments
   - Large: Regions, strict separation
   - Cloud-native: Kubernetes namespaces

## References

- Stellar V2 Query System: Unlimited nesting support
- Current Implementation: `/orchestrix-ui/src/stores/infrastructure/`
- Database: MySQL/MariaDB
- Frontend: React + MobX + Material-UI
- Backend: Spring Boot + Stellar Query Engine