# Clarity LXC Jenkins Manual Build Setup Guide

## Overview
This guide will help you set up manual, versioned building and uploading of your Clarity LXC container to Google Drive using Jenkins. Builds are triggered manually with version numbers, not automatically scheduled.

## Files Created
- `Jenkinsfile.clarity` - Pipeline definition with versioning for building and uploading the container
- `trigger-manual-build.sh` - Script to trigger builds with version parameters
- `setup-jenkins-agent.sh` - Script to configure your PC as a Jenkins agent
- `install-jenkins-job.sh` - Script to create the Jenkins job via API
- `test-setup.sh` - Verification script to check everything is configured correctly
- `SETUP_GUIDE.md` - This guide

## Step-by-Step Setup

### Step 1: Test Your Current Setup
First, verify that your local environment is ready:

```bash
cd /home/mustafa/telcobright-projects/orchestrix/jenkins
chmod +x test-setup.sh
./test-setup.sh
```

This will check:
- Required tools (Java, LXC, rclone, etc.)
- Build scripts and configuration
- rclone Google Drive access
- Permissions

### Step 2: Setup Jenkins Agent on Your PC

1. Make the setup script executable:
```bash
chmod +x setup-jenkins-agent.sh
```

2. Edit the script and update these variables at the top:
```bash
JENKINS_URL="http://your-jenkins-public-ip:8080"
AGENT_NAME="orchestrix-agent"
AGENT_SECRET="will-get-from-jenkins"
```

3. Run the setup script:
```bash
./setup-jenkins-agent.sh
```

### Step 3: Configure Agent in Jenkins

1. Log into your Jenkins server
2. Go to **Manage Jenkins** → **Manage Nodes and Clouds** → **New Node**
3. Configure:
   - Node name: `orchestrix-agent`
   - Type: Permanent Agent
   - Remote root directory: `/home/mustafa/jenkins-agent`
   - Labels: `orchestrix-agent linux lxc-builder`
   - Usage: Use this node as much as possible
   - Launch method: **Launch agent by connecting it to the controller**
   - Availability: Keep this agent online as much as possible

4. Click **Save** and copy the secret token shown

5. Update the agent configuration on your PC:
```bash
cd ~/jenkins-agent
nano agent.conf  # Update AGENT_SECRET with the token from Jenkins
```

### Step 4: Start the Jenkins Agent

Option A - Run manually (for testing):
```bash
cd ~/jenkins-agent
./start-agent.sh
```

Option B - Run as a service (recommended):
```bash
sudo systemctl enable jenkins-agent
sudo systemctl start jenkins-agent
sudo systemctl status jenkins-agent
```

### Step 5: Install the Jenkins Job

1. Get your Jenkins API token:
   - Log into Jenkins
   - Click your username (top right)
   - Click **Configure**
   - Under **API Token**, click **Add new Token**
   - Copy the generated token

2. Edit the install script:
```bash
cd /home/mustafa/telcobright-projects/orchestrix/jenkins
nano install-jenkins-job.sh
```

Update these variables:
```bash
JENKINS_URL="http://your-jenkins-public-ip:8080"
JENKINS_USER="your-username"
JENKINS_TOKEN="your-api-token"
```

3. Run the installation:
```bash
chmod +x install-jenkins-job.sh
./install-jenkins-job.sh
```

### Step 6: Verify Everything Works

1. Check the agent is connected in Jenkins:
   - Go to **Manage Jenkins** → **Manage Nodes**
   - Your `orchestrix-agent` should show as online

2. Check the job was created:
   - You should see `clarity-lxc-manual-build` in the Jenkins dashboard

3. Run a test build:
   - Click on `clarity-lxc-manual-build`
   - Click **Build with Parameters**
   - Set your version (e.g., 1.0.0)
   - Choose build type (RELEASE, SNAPSHOT, or DEVELOPMENT)
   - Click **Build**
   - Watch the console output

## Manual Build Process

### Triggering Builds

Builds are triggered manually with version control:

#### Option 1: Via Jenkins UI
1. Go to `clarity-lxc-manual-build` job
2. Click **Build with Parameters**
3. Enter parameters:
   - **VERSION**: Your version number (e.g., 1.2.0)
   - **BUILD_TYPE**: RELEASE, SNAPSHOT, or DEVELOPMENT
   - **CLEAN_BUILD**: Delete existing container first
   - **UPLOAD_TO_GDRIVE**: Upload to Google Drive
   - **RELEASE_NOTES**: Optional notes for this version

#### Option 2: Via Command Line
```bash
cd /home/mustafa/telcobright-projects/orchestrix/jenkins
./trigger-manual-build.sh -v 1.2.0 -t RELEASE -c -r "Production release"
```

### Version Management

- **RELEASE**: Production-ready versions
- **SNAPSHOT**: Testing/preview versions
- **DEVELOPMENT**: Development builds

Backups are organized in Google Drive by version:
```
orchestrix/lxc-images/clarity/
├── 1.0.0/
│   ├── clarity-v1.0.0-RELEASE-*.tar.gz
│   └── metadata.json
├── 1.1.0/
│   └── clarity-v1.1.0-SNAPSHOT-*.tar.gz
└── latest/
    └── (latest RELEASE version)
```

## Monitoring

- **Jenkins Dashboard**: View build history and logs
- **Email Notifications**: Configure in Jenkins job settings
- **Google Drive**: Check `pialmmhtb:orchestrix/lxc-images/clarity/` for backups

## Troubleshooting

### Agent Won't Connect
- Check firewall allows connection to Jenkins server
- Verify the secret token is correct
- Check Java is installed: `java -version`

### Build Fails
- Check sudo permissions: `sudo lxc list`
- Verify rclone is configured: `rclone listremotes`
- Check the build script works manually: `sudo /home/mustafa/telcobright-projects/orchestrix/images/containers/lxc/clarity/buildClarity.sh`

### Upload Fails
- Test rclone manually: `rclone ls pialmmhtb:orchestrix/`
- Check rclone config: `cat ~/.config/rclone/rclone.conf`
- Verify Google Drive permissions

## Manual Commands

### Trigger versioned build via script:
```bash
# Release build
./trigger-manual-build.sh -v 2.0.0 -t RELEASE -c -u -r "Major release with new features"

# Development build (no upload)
./trigger-manual-build.sh -v 1.0.1 -t DEVELOPMENT -n

# Snapshot build with wait
./trigger-manual-build.sh -v 1.5.0-beta -t SNAPSHOT -w
```

### Test the container build locally:
```bash
cd /home/mustafa/telcobright-projects/orchestrix/images/containers/lxc/clarity
sudo ./buildClarity.sh --overwrite
```

Test Google Drive upload:
```bash
LATEST=$(ls -t clarity-*.tar.gz | head -1)
rclone copy $LATEST pialmmhtb:orchestrix/lxc-images/clarity/
```

Check container status:
```bash
sudo lxc list clarity
```

## Support

If you encounter issues:
1. Check the test script: `./test-setup.sh`
2. Review Jenkins console output for specific errors
3. Check agent logs: `sudo journalctl -u jenkins-agent -f`
4. Verify container logs: `sudo lxc exec clarity -- journalctl -f`