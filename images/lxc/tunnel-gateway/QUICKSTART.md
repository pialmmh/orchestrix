# Tunnel Gateway - Quick Start

## 🚀 One-Command Setup

```bash
./install-tunnel-gateway.sh
```

That's it! The script will guide you through everything.

## 📋 What You Need

1. Linux machine (Ubuntu/Debian)
2. The `tunnel-gateway-v*.tar.gz` file
3. 5 minutes

## 💡 Example: MySQL Production Access

**Problem:** Need to access production MySQL from your dev laptop, but:
- Firewall blocks external connections
- No VPN access
- Can't whitelist your home IP

**Solution:** Tunnel Gateway!

```bash
# During installation, configure:
Service name: mysql-prod
Local port: 3306
SSH host: prod-server.company.com
SSH user: your-username
Auth: password
Remote host: localhost
Remote port: 3306

# In your code:
jdbc:mysql://10.10.199.150:3306/mydb
```

**Result:** Your code connects to local container IP, traffic flows through SSH tunnel, MySQL sees connection from localhost. ✨

## 🎯 Common Services

| Service | Local Port | Remote Port |
|---------|------------|-------------|
| MySQL | 3306 | 3306 |
| PostgreSQL | 5432 | 5432 |
| Kafka | 9092 | 9092 |
| Redis | 6379 | 6379 |
| MongoDB | 27017 | 27017 |

## 🔧 Post-Install Commands

```bash
# List active tunnels
sudo lxc exec tunnel-gateway-dev -- /usr/local/bin/list-tunnels.sh

# Stop container
sudo lxc stop tunnel-gateway-dev

# Start container
sudo lxc start tunnel-gateway-dev

# Delete container
sudo lxc delete tunnel-gateway-dev --force
```

## 📖 Full Documentation

- **INSTALL.md** - Complete installation guide
- **README.md** - Technical documentation

## ⚡ Benefits

- ✅ Works from anywhere (home, coffee shop, etc.)
- ✅ No firewall changes needed
- ✅ No IP whitelisting required
- ✅ All traffic encrypted via SSH
- ✅ Multiple services in one container
- ✅ Tiny footprint (~11 MB)

---

**Questions?** Check INSTALL.md for detailed examples and troubleshooting.
