# Deployment Organization Structure

## Directory Organization
```
orchestrix/
│
├── images/containers/lxc/                         [Container Artifacts]
│   │
│   ├── consul/
│   │   └── consul-v.1/
│   │       └── generated/
│   │           ├── artifact/
│   │           │   └── consul-v1.tar.gz     [Container Image]
│   │           │
│   │           ├── deployments/             [Deployment Scenarios]
│   │           │   ├── local-single.yml         ← Dev single node
│   │           │   ├── local-3node-cluster.yml  ← Local cluster
│   │           │   └── production-cluster.yml   ← Production
│   │           │
│   │           └── deploy.sh               [Interactive Runner]
│   │
│   ├── go-id/
│   │   └── go-id-v.1/
│   │       └── generated/
│   │           ├── artifact/
│   │           │   └── go-id-v1.tar.gz
│   │           │
│   │           ├── deployments/
│   │           │   ├── local-dev.yml
│   │           │   └── load-balanced.yml
│   │           │
│   │           └── deploy.sh
│   │
│   └── [other-services]/
│
├── automation/                         [Java Automation]
│   └── api/deployment/
│       └── SimpleDeploymentManager.java
│
└── deployment/                         [Central Deployment]
    └── deploy.sh                       [Main deployment script]
```

## User Workflow Diagram
```
┌─────────────────────────────────────────────────────────────────────┐
│                        USER NAVIGATION FLOW                         │
└─────────────────────────────────────────────────────────────────────┘

User starts here:
    │
    ▼
cd images/containers/lxc/consul/consul-v.1/generated/
    │
    ▼
./deploy.sh
    │
    ▼
┌────────────────────────────────────┐
│    INTERACTIVE MENU APPEARS        │
│                                    │
│  1) Local Single Node              │
│  2) Local 3-Node Cluster           │
│  3) Production Cluster             │
│  4) Custom YAML                    │
│  5) List Containers                │
│  6) Stop All                       │
│                                    │
│  Select option: _                  │
└────────────────────────────────────┘
    │
    ├── Option 1 → Deploys using local-single.yml
    ├── Option 2 → Deploys using local-3node-cluster.yml
    ├── Option 3 → Deploys using production-cluster.yml
    └── Option 4 → Lists all YAMLs for selection

Each option runs:
    │
    ▼
/orchestrix/deployment/deploy.sh [selected-yaml]
    │
    ▼
SimpleDeploymentManager.java
    │
    ▼
Container Deployed!
```

## Advantages of This Structure

### 1. Self-Contained Artifacts
```
Each artifact folder contains:
├── artifact/          → The container image
├── deployments/       → All deployment scenarios
└── deploy.sh          → Interactive runner

Everything needed is in ONE place!
```

### 2. Easy Navigation for Users
```bash
# User knows exactly where to go:
cd images/containers/lxc/consul/consul-v.1/generated/

# One command to see all options:
./deploy.sh

# No need to remember YAML names or paths!
```

### 3. Scenario-Based YAMLs
```
local-single.yml       → Quick development
local-3node-cluster.yml → Test clustering
production-cluster.yml  → Real deployment
custom-app.yml         → Special cases
```

### 4. Progressive Complexity
```
Beginner: Just run ./deploy.sh and select option 1
Advanced: Create custom YAML in deployments/
Expert: Directly call deployment/deploy.sh with any YAML
```

## Example: Adding New Service

When scaffolding a new service (e.g., Redis):

```bash
# 1. Scaffold creates structure
images/containers/lxc/redis/redis-v.1/generated/
    ├── artifact/
    │   └── redis-v1.tar.gz
    │
    ├── deployments/
    │   ├── local-single.yml
    │   ├── local-cluster.yml
    │   └── production.yml
    │
    └── deploy.sh

# 2. User deploys
cd images/containers/lxc/redis/redis-v.1/generated/
./deploy.sh
→ Select: 1) Local Single Instance

# Done! Service running
```

## Template for deploy.sh
```bash
#!/bin/bash
# Each service gets this interactive runner

echo "Select deployment:"
echo "1) Local Single"
echo "2) Local Cluster"
echo "3) Production"

read option

case $option in
  1) deploy.sh deployments/local-single.yml ;;
  2) deploy.sh deployments/local-cluster.yml ;;
  3) deploy.sh deployments/production.yml ;;
esac
```

## Benefits for Future UI

When UI is ready, it can:
1. **Scan** all `images/containers/lxc/*/generated/deployments/*.yml`
2. **Present** them in a web interface
3. **Execute** the same deploy.sh with selected YAML
4. **Track** results in database

The structure remains unchanged - UI just becomes another way to trigger the same YAMLs!

## Quick Start for New Users

```bash
# 1. Find your service
ls images/containers/lxc/

# 2. Go to its generated folder
cd images/containers/lxc/consul/consul-v.1/generated/

# 3. Run interactive deployer
./deploy.sh

# 4. Select option
→ 2 (for local cluster)

# Done!
```

## Summary

- **YAMLs are truth**: Each deployment has a YAML
- **Organized by artifact**: Everything in generated/ folder
- **Interactive runners**: User-friendly deploy.sh
- **Multiple scenarios**: Dev, staging, production configs
- **UI-ready**: Same structure works for future web UI