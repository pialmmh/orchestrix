# X11 Forwarding Setup for BDCOM Server

**Server configured:** 2025-11-10
**Server IP:** 10.255.246.173
**SSH Port:** 15605
**Username:** bdcom

---

## Server Configuration Complete ✅

The following has been configured on the BDCOM server:

1. ✅ **X11 utilities installed:**
   - xauth (X11 authentication)
   - xterm (terminal emulator)
   - x11-apps (test applications like xeyes, xclock)

2. ✅ **SSH X11 forwarding enabled:**
   - X11Forwarding yes
   - X11UseLocalhost yes
   - SSH service restarted

3. ✅ **Wireshark installed:**
   - Wireshark GUI application
   - User `bdcom` added to wireshark group
   - dumpcap capabilities set for packet capture

---

## How to Connect from Your Local PC

### On Linux/Mac

1. **Connect with X11 forwarding enabled:**
   ```bash
   ssh -X -p 15605 bdcom@10.255.246.173
   # Or use -Y for trusted X11 forwarding
   ssh -Y -p 15605 bdcom@10.255.246.173
   ```

2. **Verify X11 is working:**
   ```bash
   echo $DISPLAY
   # Should show something like: localhost:10.0
   ```

3. **Test with simple app:**
   ```bash
   xeyes &
   # Should open a GUI window with eyes that follow your cursor
   ```

4. **Run Wireshark:**
   ```bash
   wireshark &
   # Opens Wireshark GUI on your local display
   ```

### On Windows

#### Option 1: Using MobaXterm (Recommended)
1. Download and install [MobaXterm](https://mobaxterm.mobatek.net/)
2. MobaXterm has built-in X server
3. Create new SSH session:
   - Remote host: 10.255.246.173
   - Port: 15605
   - Username: bdcom
   - Advanced SSH settings → X11-Forwarding: ✅ enabled
4. Connect and run: `wireshark &`

#### Option 2: Using VcXsrv + PuTTY
1. **Install VcXsrv:**
   - Download from https://sourceforge.net/projects/vcxsrv/
   - Run XLaunch with default settings
   - Keep it running in background

2. **Configure PuTTY:**
   - Session:
     - Host: 10.255.246.173
     - Port: 15605
   - Connection → SSH → X11:
     - ✅ Enable X11 forwarding
     - X display location: localhost:0.0
   - Save session and connect

3. **Run Wireshark:**
   ```bash
   wireshark &
   ```

#### Option 3: Using WSL2 (Windows Subsystem for Linux)
1. **Install WSL2 with GUI support** (Windows 11 has this built-in)
2. **From WSL2 terminal:**
   ```bash
   ssh -X -p 15605 bdcom@10.255.246.173
   wireshark &
   ```

---

## Running Wireshark

### As Regular User (Recommended)
```bash
# Connect with X11 forwarding
ssh -X -p 15605 bdcom@10.255.246.173

# Run wireshark (you may need to log out and back in for group membership to take effect)
wireshark &
```

### With Sudo (If needed)
```bash
# If you need root access for certain interfaces
sudo -E wireshark &
# The -E flag preserves the DISPLAY environment variable
```

### Capture on Specific Interface
```bash
# List interfaces first
ip link show

# Start wireshark and select interface from GUI
wireshark &

# Or specify interface directly
wireshark -i wg-overlay &   # For WireGuard overlay
wireshark -i eth0 &          # For management interface
```

---

## Troubleshooting

### X11 forwarding failed on channel 0

**On server, check SSH config:**
```bash
sudo grep X11 /etc/ssh/sshd_config
# Should show:
# X11Forwarding yes
# X11UseLocalhost yes
```

**On client, ensure X server is running:**
- Linux: `echo $DISPLAY` should show something
- Mac: Ensure XQuartz is running
- Windows: Ensure VcXsrv/MobaXterm X server is running

### DISPLAY variable is not set

**Reconnect with -X or -Y flag:**
```bash
ssh -Y -p 15605 bdcom@10.255.246.173
```

**Check if xauth is working:**
```bash
xauth list
# Should show entries
```

### Wireshark permission errors

**User not in wireshark group yet:**
```bash
# Log out and log back in, or start new login shell
su - $USER
# Or reconnect SSH session
```

**Check dumpcap capabilities:**
```bash
getcap /usr/bin/dumpcap
# Should show: cap_net_admin,cap_net_raw=eip
```

### GUI is very slow

**Use compression:**
```bash
ssh -XC -p 15605 bdcom@10.255.246.173
# -C enables compression
```

**Use trusted forwarding (faster but less secure):**
```bash
ssh -Y -p 15605 bdcom@10.255.246.173
```

---

## Testing X11 Applications

### Simple Test Apps
```bash
# Eyes that follow cursor
xeyes &

# Clock
xclock &

# Terminal
xterm &
```

### Check What's Using X11
```bash
# See X11 forwarding details
echo $DISPLAY

# List X11 authentication
xauth list

# Check running X11 apps
ps aux | grep -E 'wireshark|xterm|xeyes'
```

---

## Capturing BGP/WireGuard Traffic

### Capture WireGuard Overlay Traffic
```bash
ssh -X -p 15605 bdcom@10.255.246.173
wireshark -i wg-overlay -f "not port 22" &
```

### Capture BGP Traffic on Overlay
```bash
wireshark -i wg-overlay -f "tcp port 179" &
# Filter for BGP protocol (port 179)
```

### Capture All Traffic on Management Interface
```bash
wireshark -i eth0 &
```

### Save Capture to File (For later analysis locally)
```bash
# Capture to file on server
sudo tcpdump -i wg-overlay -w /tmp/capture.pcap

# Download to local PC
scp -P 15605 bdcom@10.255.246.173:/tmp/capture.pcap ./

# Open locally in Wireshark
wireshark capture.pcap
```

---

## Quick Reference

### Connect with X11
```bash
ssh -X -p 15605 bdcom@10.255.246.173
```

### Run Wireshark GUI
```bash
wireshark &
```

### Run Wireshark on WireGuard Interface
```bash
wireshark -i wg-overlay &
```

### Run with Sudo (preserve DISPLAY)
```bash
sudo -E wireshark &
```

### Test X11 is working
```bash
xeyes &
```

---

## Server Details

- **SSH Port:** 15605
- **X11 Forwarding:** Enabled
- **Wireshark:** Installed at `/usr/bin/wireshark`
- **User wireshark group:** Yes (bdcom user added)
- **Dumpcap capabilities:** Set for non-root capture

---

## Notes

- The `-X` flag enables X11 forwarding (more secure, validates requests)
- The `-Y` flag enables trusted X11 forwarding (faster, less security checks)
- The `-C` flag enables compression (helps with slow connections)
- You can combine flags: `ssh -YC -p 15605 bdcom@10.255.246.173`

- **First time after installation:** You may need to log out and back in for wireshark group membership to take effect
- **Performance:** X11 forwarding over network can be slow for complex GUIs. Consider using VNC or other remote desktop if Wireshark is too slow.
- **Security:** Only use `-Y` (trusted forwarding) on networks you trust

---

**Configuration completed:** 2025-11-10
**Tested:** Server-side configuration verified
**Ready to use:** Connect from your local PC with X server running
