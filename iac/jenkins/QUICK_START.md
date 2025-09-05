# Jenkins Docker Agent - Quick Start Guide

## 🚀 Fresh Installation (3 minutes)

### Step 1: Run Setup
```bash
cd /home/mustafa/telcobright-projects/orchestrix/jenkins
./jenkins-docker-agent.sh setup
```

### Step 2: Get Agent Secret
1. Open browser: http://172.82.66.90:8080/computer/orchestrix-agent
2. Look for the secret in the connection command
3. Copy the long string (looks like: 8cd449babef9455c53a013b3fb6beecaa42c1720cecf66c35ebe7421e34c0a54)

### Step 3: Paste Secret
When the script prompts "Enter the agent secret:", paste the secret and press Enter.

**That's it!** Your agent is now:
- ✅ Running
- ✅ Connected to Jenkins
- ✅ Will auto-start on reboot
- ✅ Has Docker and LXC access

## 🔧 Daily Commands

```bash
# Check if agent is running
./manage-agent.sh status

# View logs if there are issues
./manage-agent.sh logs

# Restart if needed
./manage-agent.sh restart
```

## 🔍 Verify Installation

1. Check container is running:
```bash
docker ps | grep jenkins-orchestrix-agent
```

2. Check Jenkins UI:
- Go to http://172.82.66.90:8080
- Click "Manage Jenkins" → "Nodes"
- "orchestrix-agent" should show as online

## ⚠️ Important

- **This is the ONLY supported method** for Jenkins agents
- The agent runs in Docker for reliability and isolation
- All old agent scripts have been removed
- Use `./manage-agent.sh` for all management tasks

## Need Help?

1. Check logs: `./manage-agent.sh logs`
2. Read full docs: `cat DOCKER_AGENT_README.md`
3. Restart agent: `./manage-agent.sh restart`