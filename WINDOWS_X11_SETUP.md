# Windows X11 Setup for BDCOM Server

Quick guide for Windows users to run Wireshark GUI from BDCOM server.

---

## Recommended: MobaXterm (Easiest)

### 1. Download and Install
- Download: https://mobaxterm.mobatek.net/download-home-edition.html
- Install the free Home Edition
- No additional configuration needed - X server is built-in!

### 2. Create Connection
1. Click **Session** → **SSH**
2. Enter details:
   ```
   Remote host: 10.255.246.173
   Port: 15605
   Username: bdcom
   ```
3. Click **Advanced SSH settings** tab
4. ✅ Check **X11-Forwarding**
5. Click **OK**
6. Double-click the saved session to connect
7. Enter password when prompted

### 3. Run Wireshark
```bash
# In MobaXterm terminal
wireshark &
```

**Done!** Wireshark GUI will appear in a new window.

---

## Alternative: VcXsrv + PuTTY

### 1. Install VcXsrv (X Server)

**Download:** https://sourceforge.net/projects/vcxsrv/

**Install and Run:**
1. Install VcXsrv
2. Launch **XLaunch** from Start Menu
3. Configuration wizard:
   - Display settings: **Multiple windows**, Display number: **0** → Next
   - Client startup: **Start no client** → Next
   - Extra settings: ✅ **Clipboard**, ✅ **Primary Selection** → Next
   - Finish
4. **Important:** Keep XLaunch running (check system tray)

### 2. Install and Configure PuTTY

**Download:** https://www.putty.org/

**Configure Session:**
1. Open PuTTY
2. **Session** category:
   ```
   Host Name: 10.255.246.173
   Port: 15605
   Connection type: SSH
   ```
3. **Connection → Data**:
   ```
   Auto-login username: bdcom
   ```
4. **Connection → SSH → X11**:
   - ✅ Enable X11 forwarding
   - X display location: `localhost:0.0`
5. **Connection → SSH → Auth → Credentials**:
   - (Optional) Browse to private key if using key auth
6. Go back to **Session**
7. Saved Sessions: `BDCOM-X11`
8. Click **Save**

**Connect:**
1. Select saved session `BDCOM-X11`
2. Click **Open**
3. Enter password when prompted
4. Run: `wireshark &`

---

## Alternative: WSL2 (Windows 11)

Windows 11 has built-in WSL2 with GUI support!

### 1. Install WSL2
```powershell
# In PowerShell (as Administrator)
wsl --install
```

### 2. Update WSL2 (if already installed)
```powershell
wsl --update
```

### 3. Connect from WSL2
```bash
# In WSL2 Ubuntu terminal
ssh -X -p 15605 bdcom@10.255.246.173

# Run Wireshark
wireshark &
```

**Benefit:** Native Linux experience on Windows!

---

## Quick Test

After connecting, test X11 is working:

```bash
# Check DISPLAY variable
echo $DISPLAY
# Should output: localhost:10.0 (or similar)

# Test with simple app
xeyes &
# Eyes should appear that follow your cursor

# Run Wireshark
wireshark &
```

---

## Troubleshooting

### "X11 forwarding request failed"

**VcXsrv users:**
- Ensure XLaunch is running (check system tray)
- Restart XLaunch if needed

**PuTTY users:**
- Verify X11 forwarding is enabled in PuTTY settings
- Check X display location is `localhost:0.0`

**MobaXterm users:**
- X11 forwarding should work automatically
- Try restarting MobaXterm

### "Can't open display"

**Windows Firewall:**
1. Allow VcXsrv through firewall
2. Or temporarily disable firewall to test

**X Server not running:**
- VcXsrv: Launch XLaunch
- MobaXterm: Built-in, should work automatically

### Performance is slow

**Enable compression:**
- PuTTY: Connection → SSH → ✅ Enable compression
- MobaXterm: Settings → SSH → ✅ Compression

**Use tcpdump instead:**
```bash
# Capture on server
sudo tcpdump -i wg-overlay -w /tmp/bgp.pcap

# Download to Windows
# In PowerShell:
scp -P 15605 bdcom@10.255.246.173:/tmp/bgp.pcap C:\Downloads\

# Open in local Wireshark (download from wireshark.org)
```

---

## Summary

| Method | Pros | Cons |
|--------|------|------|
| **MobaXterm** | ✅ Easiest, all-in-one | Larger download |
| **VcXsrv + PuTTY** | ✅ Lightweight | Two separate tools |
| **WSL2** | ✅ Native Linux | Windows 11 only |

**Recommendation:** Use **MobaXterm** for simplicity.

---

## Connection Details

```
Server: 10.255.246.173
Port: 15605
Username: bdcom
X11 Forwarding: Required
```

---

**Created:** 2025-11-10
**Server configured with:** xauth, xterm, x11-apps, wireshark
