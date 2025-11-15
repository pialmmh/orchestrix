# Netlab Secrets Directory

⚠️ **WARNING: This directory contains sensitive credentials - NEVER commit to git!**

## Purpose

Store SSH passwords, private keys, and other secrets used by deployment automations.

## Organization

```
.secrets/
├── ssh-password.txt          # SSH password for all nodes
├── ssh-key                   # SSH private key (if using key auth)
├── ssh-key.pub              # SSH public key
├── node1-ssh-password.txt   # Node-specific password (optional)
├── node2-ssh-password.txt   # Node-specific password (optional)
├── node3-ssh-password.txt   # Node-specific password (optional)
└── README.md                # This file (safe to commit)
```

## Setup Instructions

### 1. Create SSH Password File

```bash
# For shared password across all nodes
echo "your_ssh_password" > .secrets/ssh-password.txt
chmod 600 .secrets/ssh-password.txt

# Update common.conf
SSH_PASSWORD_FILE="deployments/netlab/.secrets/ssh-password.txt"
```

### 2. Or Use SSH Key (Recommended)

```bash
# Generate SSH key pair
ssh-keygen -t ed25519 -f .secrets/ssh-key -N "" -C "netlab-automation"

# Copy public key to all nodes
ssh-copy-id -i .secrets/ssh-key.pub telcobright@10.20.0.30
ssh-copy-id -i .secrets/ssh-key.pub telcobright@10.20.0.31
ssh-copy-id -i .secrets/ssh-key.pub telcobright@10.20.0.32

# Set permissions
chmod 600 .secrets/ssh-key
chmod 644 .secrets/ssh-key.pub

# Update common.conf
SSH_KEY_FILE="deployments/netlab/.secrets/ssh-key"
```

### 3. Node-Specific Passwords (Optional)

If different nodes have different passwords:

```bash
echo "node1_password" > .secrets/node1-ssh-password.txt
echo "node2_password" > .secrets/node2-ssh-password.txt
echo "node3_password" > .secrets/node3-ssh-password.txt
chmod 600 .secrets/node*-ssh-password.txt
```

## Security Best Practices

1. **Never commit secrets:**
   - The `.secrets/` directory is gitignored
   - Only README.md is tracked in git

2. **Restrict permissions:**
   ```bash
   chmod 700 .secrets/              # Directory: owner only
   chmod 600 .secrets/*.txt         # Files: owner read/write only
   chmod 600 .secrets/ssh-key       # Private key: owner read/write only
   ```

3. **Backup securely:**
   ```bash
   # Backup to encrypted location
   tar -czf ~/backups/netlab-secrets-$(date +%Y%m%d).tar.gz .secrets/
   gpg -c ~/backups/netlab-secrets-*.tar.gz
   ```

4. **Rotate credentials regularly:**
   - Change passwords every 90 days
   - Regenerate SSH keys annually
   - Update all nodes after rotation

## Usage in Automation

### Bash Scripts

```bash
# Source common config
source deployments/netlab/common.conf

# Load password from file
if [ -f "$SSH_PASSWORD_FILE" ]; then
  SSH_PASSWORD=$(cat "$SSH_PASSWORD_FILE")
fi

# Or use SSH key
if [ -f "$SSH_KEY_FILE" ]; then
  ssh -i "$SSH_KEY_FILE" "$SSH_USER@$NODE1_IP"
fi
```

### Java Automation

```java
String passwordFile = commonConfig.get("SSH_PASSWORD_FILE");
if (passwordFile != null) {
    String password = Files.readString(Path.of(passwordFile)).trim();
}

String keyFile = commonConfig.get("SSH_KEY_FILE");
if (keyFile != null) {
    SSHConnection ssh = new SSHConnection(host, user, keyFile);
}
```

## What's Gitignored

From `deployments/.gitignore`:
```gitignore
# Secrets directory (except README.md)
.secrets/*.txt
.secrets/*.key
.secrets/*-password*
.secrets/ssh-key
.secrets/id_*
!.secrets/README.md
```

## Emergency: Leaked Secrets

If secrets are accidentally committed:

1. **Immediately rotate all credentials**
2. **Remove from git history:**
   ```bash
   git filter-branch --force --index-filter \
     'git rm --cached --ignore-unmatch deployments/netlab/.secrets/ssh-password.txt' \
     --prune-empty --tag-name-filter cat -- --all
   ```
3. **Force push (coordinate with team first!)**
4. **Update all deployment configs with new credentials**

## Questions?

See main documentation: `deployments/README.md`
