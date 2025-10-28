# Tunnel Gateway - Standalone Mode

**Simple SSH tunneling without containers!**

A single bash script that creates SSH tunnels directly on your local Ubuntu machine. No LXC, no Docker, just pure SSH tunneling.

## Quick Start

```bash
# 1. Edit config file (uses same INI format)
nano local-tunnels.conf

# 2. Run the script
./tunnel-gateway-local.sh local-tunnels.conf

# 3. Keep it running while you need tunnels
# Press Ctrl+C to stop all tunnels
```

## Features

✅ **No containers** - Runs directly on your machine
✅ **Same INI config** - Uses familiar configuration format
✅ **Auto dependency check** - Verifies SSH tools are installed
✅ **Multiple tunnels** - Connect to many services at once
✅ **Auto cleanup** - Ctrl+C stops all tunnels cleanly
✅ **Port conflict detection** - Warns if ports are in use
✅ **Tunnel monitoring** - Detects and reports if tunnels die

## Configuration Format

Uses the same INI format as the container version:

```ini
[mysql-prod]
# SSH Connection
sshAddress = db.production.com
sshUsername = dbuser
sshPassword = your_password
# OR use: sshKeyFile = /path/to/key
sshPort = 22

# Tunnel Configuration
localPort = 3306
remoteHost = localhost
remotePort = 3306

# Database Credentials (for your app)
dbUsername = root
dbPassword = dbpass


[kafka-prod]
sshAddress = kafka.production.com
sshUsername = kafkauser
sshKeyFile = ~/.ssh/kafka_key
localPort = 9092
remoteHost = localhost
remotePort = 9092
```

## Usage in Your Code

All services accessible at `127.0.0.1:<localPort>`:

```java
// MySQL
String url = "jdbc:mysql://127.0.0.1:3306/mydb";

// Kafka
props.put("bootstrap.servers", "127.0.0.1:9092");

// PostgreSQL
String url = "jdbc:postgresql://127.0.0.1:5432/mydb";

// Redis
Jedis jedis = new Jedis("127.0.0.1", 6379);
```

## Dependencies

The script checks and tells you if anything is missing:

```bash
sudo apt update
sudo apt install -y openssh-client sshpass net-tools
```

## Container vs Standalone

| Feature | Container Mode | Standalone Mode |
|---------|---------------|-----------------|
| **Setup** | LXC required | Just the script |
| **Overhead** | ~11 MB container | None |
| **IP Address** | 10.10.199.150 | 127.0.0.1 |
| **Isolation** | Full container | Same as host |
| **Auto-start** | Yes (container boot) | No (manual) |
| **Use Case** | Shared tunnels | Personal use |

## When to Use Standalone

✅ Quick development tasks
✅ Personal laptop/workstation
✅ Don't want to manage containers
✅ Need tunnels only while coding
✅ Testing/debugging

## When to Use Container

✅ Always-on tunnels
✅ Shared development environment
✅ Team collaboration
✅ Production-like setup
✅ Service isolation

## Example Session

```bash
$ ./tunnel-gateway-local.sh my-services.conf

╔════════════════════════════════════════════════════════════════╗
║           Tunnel Gateway - Standalone Mode                    ║
║           SSH Tunneling Without Containers                    ║
╚════════════════════════════════════════════════════════════════╝

Checking dependencies...
✓ All dependencies installed

Loading configuration from: my-services.conf

Starting tunnel: mysql-prod
  SSH: dbuser@db.production.com:22
  Forward: 127.0.0.1:3306 -> localhost:3306
  ✓ Started (PID: 123456)

Starting tunnel: kafka-prod
  SSH: kafkauser@kafka.production.com:22
  Forward: 127.0.0.1:9092 -> localhost:9092
  ✓ Started (PID: 123457)

════════════════════════════════════════════════════════════════
✓ 2 tunnel(s) active
════════════════════════════════════════════════════════════════

Active SSH Tunnels:
-------------------
  → 127.0.0.1:3306
  → 127.0.0.1:9092

Local Listening Ports:
----------------------
  → 127.0.0.1:3306
  → 127.0.0.1:9092

════════════════════════════════════════════════════════════════
Tunnels are running!
════════════════════════════════════════════════════════════════

Your applications can now connect to:
  → 127.0.0.1:<local-port>

Press Ctrl+C to stop all tunnels

(script keeps running...)
```

## Troubleshooting

### Port Already in Use

```
⚠ Warning: Port 3306 is already in use
Skipping this tunnel
```

**Solution:** Change `localPort` in config or stop the conflicting service.

### SSH Connection Failed

```
✗ Failed to start (check SSH credentials/connectivity)
```

**Solutions:**
- Verify SSH credentials
- Test: `ssh user@host` manually
- Check firewall/network connectivity
- Verify sshpass is installed (for password auth)

### Tunnel Died

```
⚠ Warning: Tunnel (PID 123456) died
```

**Causes:**
- SSH connection lost
- Remote server restarted
- Network interruption

**Solution:** Stop (Ctrl+C) and restart the script.

## Advanced Usage

### Custom SSH Port

```ini
[service]
sshAddress = myserver.com
sshPort = 2222
```

### SSH Key Authentication

```ini
[service]
sshAddress = myserver.com
sshUsername = user
sshKeyFile = ~/.ssh/my_key
# Don't specify sshPassword when using key
```

### Remote Host != localhost

```ini
[mysql-from-jumpbox]
sshAddress = jumpbox.com
sshUsername = admin
sshPassword = pass
localPort = 3306
remoteHost = internal-db-server.local  # Different from SSH host
remotePort = 3306
```

### Multiple Services, One Config

```ini
[mysql-prod]
localPort = 3306
# ...

[mysql-staging]
localPort = 3307  # Different local port
# ...

[postgres-prod]
localPort = 5432
# ...

[kafka-prod]
localPort = 9092
# ...
```

## Security Notes

⚠️ **Development Use Only**

- Passwords stored in plaintext in config file
- Auto-accepts SSH host keys (`StrictHostKeyChecking=no`)
- No certificate validation

**For production:**
- Use SSH key authentication only
- Enable `StrictHostKeyChecking`
- Store credentials in encrypted vault
- Consider VPN instead of SSH tunnels

**Protect your config file:**
```bash
chmod 600 my-tunnels.conf
```

## Comparison with Other Tools

| Tool | Standalone Script | SSH Command | autossh | Container |
|------|------------------|-------------|---------|-----------|
| Multiple tunnels | ✅ One config | ❌ Multiple commands | ⚠️ Multiple configs | ✅ One config |
| Auto-reconnect | ⚠️ Detects death | ❌ No | ✅ Yes | ✅ Yes |
| Easy config | ✅ INI format | ❌ CLI args | ⚠️ Service files | ✅ INI format |
| Setup time | ⏱️ 10 seconds | ⏱️ 5 seconds | ⏱️ 5 minutes | ⏱️ 2 minutes |
| Isolation | ❌ No | ❌ No | ❌ No | ✅ Yes |

## Files

```
standalone/
├── tunnel-gateway-local.sh    # Main script
├── local-tunnels.conf         # Example config
└── README.md                  # This file
```

## Tips

💡 **Run in tmux/screen** for persistent tunnels:
```bash
tmux
./tunnel-gateway-local.sh my-tunnels.conf
# Detach: Ctrl+B then D
```

💡 **Use different config files** for different environments:
```bash
./tunnel-gateway-local.sh production.conf
./tunnel-gateway-local.sh staging.conf
```

💡 **Test before using in app:**
```bash
# MySQL
mysql -h 127.0.0.1 -P 3306 -u user -p

# Telnet test
telnet 127.0.0.1 3306
```

## FAQ

**Q: Can I run this alongside the container version?**
A: Yes! Just use different local ports (e.g., 13306 instead of 3306).

**Q: What happens if my laptop goes to sleep?**
A: Tunnels will die. Restart the script when you wake up.

**Q: Can I use this on macOS/Windows?**
A: macOS: Yes (install sshpass via brew). Windows: Use WSL2.

**Q: How do I run this as a background service?**
A: Use systemd or supervisor. But at that point, consider the container version.

**Q: Why 127.0.0.1 instead of 0.0.0.0?**
A: Security. Only local processes can connect. Change in script if needed.

## Support

For issues:
- Check container version README.md for config format details
- Verify SSH connectivity: `ssh user@host`
- Test dependencies: `which ssh sshpass`
- Enable verbose SSH: Change `LogLevel=ERROR` to `LogLevel=DEBUG` in script

---

**Simple. Direct. No containers needed.**
