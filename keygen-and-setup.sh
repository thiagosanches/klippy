#!/bin/bash
# Generate RSA-4096 PGP key for Klippy
# Email: klippy@aiouti.net
# No passphrase

set -e

echo "Generating RSA-4096 PGP key for klippy@aiouti.net..."

gpg --batch --gen-key <<EOF
Key-Type: RSA
Key-Length: 4096
Name-Real: Klippy
Name-Email: klippy@aiouti.net
Expire-Date: 0
%no-protection
%commit
EOF

echo ""
echo "Key generated successfully!"
echo ""
echo "Export public key:"
echo "  gpg --armor --export klippy@aiouti.net > klippy-public.asc"
echo ""
echo "Export private key:"
echo "  gpg --armor --export-secret-keys klippy@aiouti.net > klippy-private.asc"
echo ""
echo "List keys:"
echo "  gpg --list-keys klippy@aiouti.net"
