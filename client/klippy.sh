#!/bin/bash
# Klippy - Encrypted clipboard sharing client
# Usage: klippy.sh push|pull [server-url]

set -e

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Load .env file if it exists
if [ -f "$PROJECT_ROOT/.env" ]; then
  export $(grep -v '^#' "$PROJECT_ROOT/.env" | xargs)
fi

# Configuration (from .env or defaults)
GPG_KEY="${GPG_KEY_EMAIL:-klippy@aiouti.net}"
SERVER_URL="${2:-${SERVER_URL:-http://localhost:3000}}"

# Detect clipboard tool
if command -v wl-copy &> /dev/null; then
  CLIP_COPY="wl-copy"
  CLIP_PASTE="wl-paste"
elif command -v xclip &> /dev/null; then
  CLIP_COPY="xclip -selection clipboard -in"
  CLIP_PASTE="xclip -selection clipboard -out"
else
  echo "Error: Neither wl-clipboard (Wayland) nor xclip (X11) found"
  exit 1
fi

push_clipboard() {
  echo "Reading clipboard..."
  PLAIN_TEXT=$($CLIP_PASTE)
  
  if [ -z "$PLAIN_TEXT" ]; then
    echo "Error: Clipboard is empty"
    exit 1
  fi
  
  echo "Encrypting..."
  ENCRYPTED=$(echo "$PLAIN_TEXT" | gpg --armor --encrypt --recipient "$GPG_KEY" 2>/dev/null)
  
  echo "Sending to server..."
  RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$SERVER_URL/clipboard" \
    -H "Content-Type: application/json" \
    -d "{\"encrypted\":$(echo "$ENCRYPTED" | jq -Rs .)}")
  
  HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
  BODY=$(echo "$RESPONSE" | head -n-1)
  
  if [ "$HTTP_CODE" = "200" ]; then
    echo "✓ Clipboard pushed successfully"
  else
    echo "✗ Failed to push clipboard (HTTP $HTTP_CODE)"
    echo "$BODY"
    exit 1
  fi
}

pull_clipboard() {
  echo "Fetching from server..."
  RESPONSE=$(curl -s -w "\n%{http_code}" "$SERVER_URL/clipboard")
  
  HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
  BODY=$(echo "$RESPONSE" | head -n-1)
  
  if [ "$HTTP_CODE" != "200" ]; then
    echo "✗ Failed to fetch clipboard (HTTP $HTTP_CODE)"
    echo "$BODY"
    exit 1
  fi
  
  ENCRYPTED=$(echo "$BODY" | jq -r '.encrypted')
  
  if [ -z "$ENCRYPTED" ] || [ "$ENCRYPTED" = "null" ]; then
    echo "Error: No data on server"
    exit 1
  fi
  
  echo "Decrypting..."
  PLAIN_TEXT=$(echo "$ENCRYPTED" | gpg --decrypt 2>/dev/null)
  
  echo "Writing to clipboard..."
  echo "$PLAIN_TEXT" | $CLIP_COPY
  
  echo "✓ Clipboard pulled successfully"
}

# Main
case "$1" in
  push)
    push_clipboard
    ;;
  pull)
    pull_clipboard
    ;;
  *)
    echo "Usage: $0 {push|pull} [server-url]"
    echo "  push - Encrypt and send clipboard to server"
    echo "  pull - Fetch and decrypt clipboard from server"
    echo ""
    echo "Default server: http://localhost:3000"
    exit 1
    ;;
esac
