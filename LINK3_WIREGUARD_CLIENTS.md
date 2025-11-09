# Link3 WireGuard VPN - Client Configuration Summary

## Server Details

- **Server IP:** 123.200.0.51
- **WireGuard Port:** 51820
- **Interface:** wg-overlay
- **Server Public Key:** cW4avyR1mgVmal/DmY0eKWNRrGVzXWYm84sukhMLFTs=

## Network Configuration

- **Overlay Network:** 10.9.9.0/24
- **Container Networks:** 10.10.0.0/16 (all Link3 and BDCOM container subnets)

## Client Profiles

All client configurations are available in: `/home/mustafa/telcobright-projects/orchestrix-frr-router/wireguard-clients/`

| Profile | IP Address | Config File | Status |
|---------|-----------|-------------|--------|
| client1 | 10.9.9.250/32 | client1.conf | ✅ Active |
| client2 | 10.9.9.251/32 | client2.conf | ✅ Active |
| client3 | 10.9.9.252/32 | client3.conf | ✅ Active |
| client4 | 10.9.9.253/32 | client4.conf | ✅ Active |
| client5 | 10.9.9.254/32 | client5.conf | ✅ Active |

## Client Configuration Details

Each client configuration includes:

```ini
[Interface]
Address = <CLIENT_IP>/32
PrivateKey = <CLIENT_PRIVATE_KEY>
DNS = 8.8.8.8

[Peer]
PublicKey = cW4avyR1mgVmal/DmY0eKWNRrGVzXWYm84sukhMLFTs=
Endpoint = 123.200.0.51:51820
AllowedIPs = 10.9.9.0/24, 10.10.0.0/16
PersistentKeepalive = 25
```

### Allowed IPs Explained

- **10.9.9.0/24** - WireGuard overlay network (access to other VPN clients and nodes)
- **10.10.0.0/16** - All container subnets across Link3 and BDCOM clusters

**Important:** Default gateway is NOT pushed, so the client's internet connection remains unchanged.

## How to Use a Client Profile

### Option 1: WireGuard GUI Client (Windows/Mac/Linux)

1. Download WireGuard from: https://www.wireguard.com/install/
2. Open WireGuard application
3. Click "Add Tunnel" → "Add from file"
4. Select one of the client configuration files (e.g., `client1.conf`)
5. Click "Activate"

### Option 2: WireGuard Mobile App (iOS/Android)

1. Download WireGuard app from App Store or Google Play
2. Transfer the `.conf` file to your mobile device
3. Open WireGuard app
4. Tap "+" → "Create from file or archive"
5. Select the configuration file
6. Tap the toggle to connect

### Option 3: Command Line (Linux)

```bash
# Copy config to WireGuard directory
sudo cp client1.conf /etc/wireguard/link3.conf

# Start the tunnel
sudo wg-quick up link3

# Check status
sudo wg show link3

# Stop the tunnel
sudo wg-quick down link3

# Enable auto-start on boot
sudo systemctl enable wg-quick@link3
```

## Testing Connection

After connecting, verify access to container networks:

```bash
# Test Link3 container (Debezium)
ping 10.10.197.183

# Test BDCOM Kafka brokers
ping 10.10.199.20   # BDCOM Server 1
ping 10.10.198.20   # BDCOM Server 2
ping 10.10.197.20   # BDCOM Server 3

# Test Link3 nodes
ping 10.9.9.1       # Node 1
ping 10.9.9.2       # Node 2
ping 10.9.9.3       # Node 3 (VPN server)
```

## Access to Services

Once connected via VPN, you can access:

### Link3 Services

- **Debezium:** 10.10.197.183
- **Link3 Containers:** 10.10.194.0/24 - 10.10.197.0/24

### BDCOM Services

- **Kafka Cluster:**
  - Broker 1: 10.10.199.20:9092
  - Broker 2: 10.10.198.20:9092
  - Broker 3: 10.10.197.20:9092

- **BDCOM Containers:** 10.10.196.0/24 - 10.10.199.0/24

## Troubleshooting

### Cannot connect to VPN

1. Check firewall allows UDP port 51820
2. Verify server is accessible: `nc -zvu 123.200.0.51 51820`
3. Check client configuration has correct endpoint and public key

### VPN connects but cannot access containers

1. Verify routes are pushed: `ip route | grep 10.10`
2. Test ping to VPN gateway: `ping 10.9.9.3`
3. Check BGP is running on Link3 nodes
4. Verify container subnets are announced via BGP

### How to check VPN connection status

**Windows/Mac/Linux GUI:**
- Open WireGuard app
- Check "Latest Handshake" - should be recent (< 2 minutes)
- Check "Transfer" - should show data transfer

**Linux CLI:**
```bash
sudo wg show link3
```

Look for:
- `latest handshake:` should be recent
- `endpoint:` should show server IP and port
- `transfer:` should show data sent/received

## Server Management

### View all connected clients

```bash
ssh -p 8210 tbsms@123.200.0.51
echo 'TB@l38800' | sudo -S wg show wg-overlay
```

### Add new client

1. Generate new client configuration using `add-link3-clients.sh`
2. Add peer to `/etc/wireguard/wg-overlay.conf`
3. Reload WireGuard: `sudo wg-quick down wg-overlay && sudo wg-quick up wg-overlay`

### Remove a client

1. Edit `/etc/wireguard/wg-overlay.conf`
2. Remove the [Peer] section for that client
3. Reload WireGuard: `sudo wg-quick down wg-overlay && sudo wg-quick up wg-overlay`

## Security Notes

- **Keep `.conf` files secure** - they contain private keys
- **Do not share client profiles** - each developer should have their own
- **Revoke access** by removing peer from server configuration
- **Monitor connections** regularly using `wg show`

## Files Location

All client configuration files are stored at:
```
/home/mustafa/telcobright-projects/orchestrix-frr-router/wireguard-clients/
├── client1.conf
├── client2.conf
├── client3.conf
├── client4.conf
└── client5.conf
```

## Scripts

- **add-link3-clients.sh** - Generate new client profiles
- **extract-peers.py** - Extract public keys from client configs
- **update-server-config.sh** - Update server with new clients

---

**Created:** 2025-11-06
**Last Updated:** 2025-11-06
**Status:** Active - 5 clients configured
