#!/usr/bin/env python3
import subprocess
import re

peers_config = ""

for i in range(1, 6):
    filename = f"wireguard-clients/client{i}.conf"

    try:
        with open(filename, 'r') as f:
            content = f.read()

        # Extract private key and IP
        priv_match = re.search(r'PrivateKey = (.+)', content)
        ip_match = re.search(r'Address = (.+)', content)

        if priv_match and ip_match:
            priv_key = priv_match.group(1).strip()
            ip_addr = ip_match.group(1).strip()

            # Generate public key from private key
            result = subprocess.run(['wg', 'pubkey'],
                                  input=priv_key.encode(),
                                  capture_output=True)
            pub_key = result.stdout.decode().strip()

            print(f"client{i}: {ip_addr} -> {pub_key}")

            peers_config += f"""
# Client: client{i}
[Peer]
PublicKey = {pub_key}
AllowedIPs = {ip_addr}
"""
    except Exception as e:
        print(f"Error processing client{i}: {e}")

# Write peers configuration to file
with open('peers_to_add.conf', 'w') as f:
    f.write(peers_config)

print("\nPeer configuration written to: peers_to_add.conf")
