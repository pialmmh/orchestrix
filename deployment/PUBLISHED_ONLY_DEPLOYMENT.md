# Published-Only Deployment Architecture

## Core Principle: NO DEPLOYMENT WITHOUT PUBLISHING

```
┌─────────────────────────────────────────────────────────────────────┐
│                    MANDATORY WORKFLOW                               │
│                                                                      │
│   SCAFFOLD  →  PUBLISH  →  DEPLOY                                  │
│                    ↑                                                │
│                    └── REQUIRED (No bypass allowed)                 │
└─────────────────────────────────────────────────────────────────────┘
```

## Workflow Enforcement

### Step 1: SCAFFOLD
```bash
cd images/containers/lxc/consul/build
./build.sh
→ Output: consul-v1.tar.gz (LOCAL ONLY - CANNOT DEPLOY)
```

### Step 2: PUBLISH (MANDATORY)
```bash
cd ../consul-v.1/generated
./publish.sh
→ Upload to: Google Drive/S3/HTTP Server
→ Get publish URL
→ Register in database with publish_id
→ Status: PUBLISHED ✓
```

### Step 3: DEPLOY (ONLY FROM PUBLISHED)
```bash
./deploy.sh
→ Check: Is artifact published?
→ NO: ❌ REJECT - "Artifact not published"
→ YES: ✓ Proceed with deployment
→ Pull from: Published URL only
```

## Database Schema (Enforced)

```sql
-- Artifacts table
CREATE TABLE artifacts (
    artifact_id VARCHAR(50) PRIMARY KEY,
    artifact_name VARCHAR(100) NOT NULL,
    version VARCHAR(50) NOT NULL,
    build_date TIMESTAMP,
    published BOOLEAN DEFAULT FALSE,
    publish_id VARCHAR(100) UNIQUE,  -- NULL until published
    CONSTRAINT no_deploy_without_publish
        CHECK (published = TRUE OR publish_id IS NOT NULL)
);

-- Publications table (Required for deployment)
CREATE TABLE artifact_publications (
    publish_id VARCHAR(100) PRIMARY KEY,
    artifact_id VARCHAR(50) NOT NULL,
    publish_url TEXT NOT NULL,
    publish_type VARCHAR(50),  -- googledrive, s3, http
    checksum VARCHAR(256) NOT NULL,
    publish_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    available BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (artifact_id) REFERENCES artifacts(artifact_id)
);

-- Deployments table (Only accepts published artifacts)
CREATE TABLE deployments (
    deployment_id VARCHAR(100) PRIMARY KEY,
    publish_id VARCHAR(100) NOT NULL,  -- Must reference published artifact
    deployment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    target_host VARCHAR(255),
    status VARCHAR(50),
    FOREIGN KEY (publish_id) REFERENCES artifact_publications(publish_id)
);
```

## YAML Structure (Published Only)

```yaml
# consul-deploy.yml
deployment:
  name: "consul-cluster"
  type: "multi-instance"

# ONLY published artifacts allowed
artifact:
  source: "published"  # ONLY valid value
  publish_id: "PUB_CONSUL_V1_20241008"  # Required

  # No local paths allowed!
  # path: "../artifact/consul.tar.gz"  ❌ INVALID

  # Published location (fetched from DB using publish_id)
  # Auto-populated from database

terminal:
  type: "ssh"
  connection:
    host: "10.0.1.100"
    user: "deploy"
```

## Deployment Validation Flow

```
User runs: ./deploy.sh config.yml
                │
                ▼
    ┌─────────────────────────┐
    │  Read YAML Config        │
    └───────────┬──────────────┘
                ▼
    ┌─────────────────────────┐
    │  Extract publish_id      │
    └───────────┬──────────────┘
                ▼
    ┌─────────────────────────┐
    │  Query Database:         │
    │  SELECT * FROM           │
    │  artifact_publications   │
    │  WHERE publish_id = ?    │
    └───────────┬──────────────┘
                ▼
         ┌──────────────┐
         │   Found?     │
         └──┬───────┬───┘
            NO      YES
            │        │
            ▼        ▼
        ┌───────┐  ┌────────────┐
        │ ABORT │  │ Get URL    │
        │       │  │ Deploy     │
        └───────┘  └────────────┘
```

## Enforcement in Code

### DeploymentManager.java
```java
public class DeploymentManager {

    public DeploymentResult deploy(String configFile) {
        Config config = loadYaml(configFile);

        // ENFORCE: Only published artifacts
        if (!"published".equals(config.artifact.source)) {
            throw new IllegalStateException(
                "ERROR: Only published artifacts can be deployed. " +
                "Please publish your artifact first using publish.sh"
            );
        }

        // ENFORCE: Must have publish_id
        if (config.artifact.publish_id == null) {
            throw new IllegalStateException(
                "ERROR: No publish_id found. " +
                "Artifact must be published before deployment."
            );
        }

        // Fetch from database
        ArtifactPublication pub = db.query(
            "SELECT * FROM artifact_publications WHERE publish_id = ?",
            config.artifact.publish_id
        );

        if (pub == null) {
            throw new IllegalStateException(
                "ERROR: Published artifact not found: " +
                config.artifact.publish_id
            );
        }

        // Deploy ONLY from published URL
        return deployFromUrl(pub.publish_url);
    }
}
```

## Benefits of Published-Only Deployment

1. **Audit Trail** - Every deployment traced to published artifact
2. **Integrity** - Checksums verified from publication
3. **Consistency** - Same artifact deployed everywhere
4. **Security** - No arbitrary file uploads
5. **Rollback** - Can redeploy any published version
6. **Compliance** - Clear approval/publish workflow

## Quick Reference

### Publishing Artifact
```bash
# After building
cd images/containers/lxc/consul/consul-v.1/generated
./publish.sh

# Output:
Published to Google Drive
Publish ID: PUB_CONSUL_V1_20241008
URL: https://drive.google.com/uc?id=XXX
```

### Deployment Config
```yaml
artifact:
  source: "published"
  publish_id: "PUB_CONSUL_V1_20241008"
  # System will fetch URL from database
```

### Deployment Command
```bash
./deploy.sh consul-production.yml

# System checks:
# ✓ Is published?
# ✓ Has valid publish_id?
# ✓ URL accessible?
# → Deploy from published URL
```

## Error Messages

```
ERROR: Cannot deploy unpublished artifact
Solution: Run publish.sh first

ERROR: No publish_id in configuration
Solution: Add publish_id from publish output

ERROR: Published artifact not found in database
Solution: Verify publish_id is correct

ERROR: Published URL not accessible
Solution: Check Google Drive/S3 permissions
```