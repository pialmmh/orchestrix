# Tunnel Gateway - Distribution Package for Developers

## 📦 What to Download

Developers need **2 files** to get started:

### 1. Container Image (Required)
```
tunnel-gateway-v1-YYYYMMDD-HHMMSS.tar.gz
```
**Location:** `tunnel-gateway-v.1/generated/artifact/`  
**Size:** ~11 MB  
**Description:** The LXC container image

### 2. Installation Script (Required)
```
install-tunnel-gateway.sh
```
**Location:** `tunnel-gateway/`  
**Size:** ~10 KB  
**Description:** Automated setup wizard

### 3. Documentation (Optional but Recommended)
```
QUICKSTART.md   - 1-page quick reference
INSTALL.md      - Complete installation guide
README.md       - Technical documentation
```

---

## 🚀 Step-by-Step Instructions for Developers

### Step 1: Download Files

**Option A: Download from release/artifact server**
```bash
# Create a directory
mkdir tunnel-gateway-setup
cd tunnel-gateway-setup

# Download the image file
wget https://your-server.com/tunnel-gateway-v1-20251028-035621.tar.gz

# Download the installer
wget https://your-server.com/install-tunnel-gateway.sh
chmod +x install-tunnel-gateway.sh
```

**Option B: Copy from shared location**
```bash
# Create a directory
mkdir tunnel-gateway-setup
cd tunnel-gateway-setup

# Copy files from shared drive/folder
cp /path/to/shared/tunnel-gateway-v*.tar.gz .
cp /path/to/shared/install-tunnel-gateway.sh .
chmod +x install-tunnel-gateway.sh
```

### Step 2: Run Installer
```bash
./install-tunnel-gateway.sh
```

### Step 3: Follow Interactive Prompts

The script will:
1. ✅ Check/install LXD
2. ✅ Setup networking
3. ✅ Import container image
4. ✅ Ask you to configure tunnels
5. ✅ Launch container automatically

### Step 4: Start Using!

Connect your app to the container IP:
```java
// MySQL example
jdbc:mysql://10.10.199.150:3306/mydb

// Kafka example
bootstrap.servers=10.10.199.150:9092
```

---

## 📋 Example: Complete Setup (2 minutes)

```bash
# 1. Create directory and download
mkdir ~/tunnel-gateway-setup
cd ~/tunnel-gateway-setup
cp /shared/tunnel-gateway-v1-20251028-035621.tar.gz .
cp /shared/install-tunnel-gateway.sh .
chmod +x install-tunnel-gateway.sh

# 2. Run installer
./install-tunnel-gateway.sh

# 3. Answer prompts (example):
# Container name: my-dev-gateway
# Container IP: 10.10.199.150
# Service name: mysql-prod
# Local port: 3306
# SSH host: db.production.com
# SSH user: dbuser
# Auth: password
# Password: [enter your password]
# Remote host: localhost
# Remote port: 3306
# [type 'done' to finish]

# 4. Done! Container is running
```

---

## 🎯 What Each File Does

### tunnel-gateway-v*.tar.gz
- **What:** Pre-built Alpine Linux container (~11 MB)
- **Contains:** SSH tunnel software, management scripts
- **Used by:** Installation script (imported automatically)

### install-tunnel-gateway.sh
- **What:** Interactive setup wizard
- **Does:** Everything! Installs LXD, imports image, configures tunnels, launches container
- **Interactive:** Yes - asks questions, provides color-coded feedback

---

## 📁 Distribution Package Structure

For your team, create a shared folder with:

```
tunnel-gateway-distribution/
├── tunnel-gateway-v1-20251028-035621.tar.gz  (11 MB - Required)
├── install-tunnel-gateway.sh                 (10 KB - Required)
├── QUICKSTART.md                             (Optional)
├── INSTALL.md                                (Optional)
└── README.md                                 (Optional)
```

**Minimum for developers:** Just the first 2 files!

---

## 🔄 Update Process

When a new version is released:

1. Download new tar.gz file
2. Same install-tunnel-gateway.sh works for all versions
3. Run installer - it will detect and use new image

---

## ⚡ Quick Commands

```bash
# After installation, manage your gateway:
sudo lxc list                    # See container status
sudo lxc stop tunnel-gateway-dev # Stop container
sudo lxc start tunnel-gateway-dev # Start container
```

---

## 💡 Distribution Tips for Admins

### For Internal Sharing
```bash
# Copy files to shared network drive
cp tunnel-gateway-v*.tar.gz /mnt/shared/tools/
cp install-tunnel-gateway.sh /mnt/shared/tools/
cp *.md /mnt/shared/tools/

# Set permissions
chmod 755 /mnt/shared/tools/install-tunnel-gateway.sh
chmod 644 /mnt/shared/tools/tunnel-gateway-v*.tar.gz
```

### For HTTP Distribution
```bash
# Upload to internal web server
scp tunnel-gateway-v*.tar.gz webserver:/var/www/html/tools/
scp install-tunnel-gateway.sh webserver:/var/www/html/tools/
scp *.md webserver:/var/www/html/tools/

# Developers download via:
wget http://internal-server/tools/tunnel-gateway-v1-20251028-035621.tar.gz
wget http://internal-server/tools/install-tunnel-gateway.sh
```

### For Email Distribution
```
Subject: Tunnel Gateway - SSH Tunneling Tool for Development

Hi Team,

Access remote services (MySQL, Kafka, etc.) from your dev machine using SSH tunnels.

Download these 2 files:
1. tunnel-gateway-v1-20251028-035621.tar.gz (11 MB)
2. install-tunnel-gateway.sh (10 KB)

Run: ./install-tunnel-gateway.sh

Full docs: See attached QUICKSTART.md

Attachments:
- tunnel-gateway-v1-20251028-035621.tar.gz
- install-tunnel-gateway.sh
- QUICKSTART.md
```

---

## 🎓 Training Slide

**"Tunnel Gateway - One Slide"**

**Problem:** Can't access production MySQL/Kafka from dev laptop (firewall, no VPN, no IP whitelist)

**Solution:** Tunnel Gateway!

**Setup:**
1. Download 2 files (~11 MB total)
2. Run `./install-tunnel-gateway.sh`
3. Answer a few questions
4. Done! (2 minutes)

**Usage:**
```
jdbc:mysql://10.10.199.150:3306/mydb
```

**Benefits:** Works from anywhere, secure, no admin needed

---

## ❓ FAQ

**Q: Do I need root/admin access?**  
A: You need `sudo` for LXD operations, but the script handles everything.

**Q: Will this work on my laptop?**  
A: Yes! Any Linux machine (Ubuntu, Debian, etc.)

**Q: What if LXD is not installed?**  
A: The script will install it for you (via snap).

**Q: Can I configure multiple services?**  
A: Yes! The script has a loop - configure MySQL, Kafka, PostgreSQL, etc. all at once.

**Q: What if something goes wrong?**  
A: Check INSTALL.md troubleshooting section, or ask your admin.

---

## 📧 Support

For issues:
- Read INSTALL.md troubleshooting section
- Check: `sudo lxc list` (is container running?)
- Check: `sudo lxc exec container-name -- /usr/local/bin/list-tunnels.sh`
- Contact: [your-support-email]
