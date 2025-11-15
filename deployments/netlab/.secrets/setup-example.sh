#!/bin/bash
# Example script to set up secrets for netlab tenant
# Usage: ./setup-example.sh

set -e

SECRETS_DIR="$(cd "$(dirname "$0")" && pwd)"
echo "Setting up secrets in: $SECRETS_DIR"
echo ""

# Check if secrets already exist
if [ -f "$SECRETS_DIR/ssh-password.txt" ] || [ -f "$SECRETS_DIR/ssh-key" ]; then
    echo "⚠️  Secrets already exist!"
    read -p "Overwrite existing secrets? (yes/no): " confirm
    if [ "$confirm" != "yes" ]; then
        echo "Aborted."
        exit 0
    fi
fi

echo "Choose authentication method:"
echo "1) SSH Password (for testing)"
echo "2) SSH Key (recommended for production)"
read -p "Enter choice (1 or 2): " choice

case $choice in
    1)
        echo ""
        echo "=== SSH Password Setup ==="
        read -sp "Enter SSH password: " password
        echo ""

        # Save password to file
        echo "$password" > "$SECRETS_DIR/ssh-password.txt"
        chmod 600 "$SECRETS_DIR/ssh-password.txt"

        echo "✅ Password saved to: ssh-password.txt"
        echo ""
        echo "Update common.conf with:"
        echo "  SSH_PASSWORD_FILE=\"deployments/netlab/.secrets/ssh-password.txt\""
        ;;

    2)
        echo ""
        echo "=== SSH Key Setup ==="

        # Generate key
        ssh-keygen -t ed25519 \
            -f "$SECRETS_DIR/ssh-key" \
            -N "" \
            -C "netlab-automation@$(hostname)"

        chmod 600 "$SECRETS_DIR/ssh-key"
        chmod 644 "$SECRETS_DIR/ssh-key.pub"

        echo ""
        echo "✅ SSH key pair generated:"
        echo "   Private: ssh-key"
        echo "   Public:  ssh-key.pub"
        echo ""

        # Show public key
        echo "Public key:"
        cat "$SECRETS_DIR/ssh-key.pub"
        echo ""

        echo "Next steps:"
        echo "1. Copy public key to all nodes:"
        echo "   ssh-copy-id -i $SECRETS_DIR/ssh-key.pub telcobright@10.20.0.30"
        echo "   ssh-copy-id -i $SECRETS_DIR/ssh-key.pub telcobright@10.20.0.31"
        echo "   ssh-copy-id -i $SECRETS_DIR/ssh-key.pub telcobright@10.20.0.32"
        echo ""
        echo "2. Update common.conf with:"
        echo "   SSH_KEY_FILE=\"deployments/netlab/.secrets/ssh-key\""
        ;;

    *)
        echo "Invalid choice. Aborted."
        exit 1
        ;;
esac

echo ""
echo "🔒 Secrets directory configured successfully!"
