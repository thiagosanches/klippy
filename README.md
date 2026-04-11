# Klippy

Secure clipboard sharing across devices on your local network with end-to-end PGP encryption.

<img width="345" height="767" alt="image" src="https://github.com/user-attachments/assets/532ff771-5bd1-49c0-b608-72628b28e168" />


## Features

- **End-to-end encryption**: RSA-4096 PGP encryption - server never sees plaintext
- **Cross-platform**: Linux bash client and Android app
- **Simple**: Push and pull clipboard with a single command/tap
- **Self-hosted**: Run your own server with Docker
- **Zero dependencies**: Server uses only Node.js built-ins

## Components

### 1. Server (Node.js)

Pure Node.js HTTP server with zero npm dependencies. Stores encrypted clipboard data in memory and on disk.

**Features:**
- Node.js 22 LTS
- Uses only `node:http` and `node:fs`
- 512 KB body size limit
- Docker support with docker-compose
- Built-in tests using `node:test`

**Endpoints:**
- `GET /health` - Health check
- `GET /clipboard` - Retrieve encrypted clipboard
- `POST /clipboard` - Store encrypted clipboard

### 2. Linux Client (Bash)

Bash script for encrypting/decrypting clipboard using GPG.

**Features:**
- Supports Wayland (`wl-clipboard`) and X11 (`xclip`)
- Uses GPG for PGP encryption/decryption
- ASCII-armored output for JSON compatibility

### 3. Android App (Kotlin)

Material Design 3 Android app with PGP encryption.

**Features:**
- Kotlin with Material Design 3
- Bouncy Castle (`bcpg-jdk18on:1.78`) for PGP
- Encrypted SharedPreferences (`security-crypto:1.0.0`)
- HttpURLConnection (no OkHttp/Retrofit)
- Key generation in-app

## Setup

### 0. Configure Environment (Optional)

Create a `.env` file from the example:

```bash
cp .env.example .env
```

Edit `.env` with your settings:

```bash
# Server Configuration
SERVER_URL=http://192.168.1.100:3000
SERVER_PORT=3000

# GPG Key Email
GPG_KEY_EMAIL=klippy@aiouti.net
```

This allows you to configure server URL and GPG key email once, instead of passing them as arguments each time.

### 1. Generate PGP Keys

```bash
./keygen-and-setup.sh

# Export keys
gpg --armor --export klippy@aiouti.net > klippy-public.asc
gpg --armor --export-secret-keys klippy@aiouti.net > klippy-private.asc
```

### 2. Start Server

**Option A: Docker**
```bash
cd server
docker-compose up -d
```

**Option B: Direct**
```bash
cd server
npm start
```

Server runs on port 3000 by default.

### 3. Configure Linux Client

```bash
# Push clipboard to server
./client/klippy.sh push http://192.168.1.100:3000

# Pull clipboard from server
./client/klippy.sh pull http://192.168.1.100:3000
```

### 4. Configure Android App

1. Open Klippy app
2. Tap settings icon
3. Enter server URL (e.g., `http://192.168.1.100:3000`)
4. Either generate keys in-app or paste your exported keys
5. Tap Save

## Usage

### Linux

```bash
# Copy something to clipboard, then push
echo "Hello World" | wl-copy
./client/klippy.sh push http://server:3000

# Pull and paste
./client/klippy.sh pull http://server:3000
wl-paste
```

### Android

1. Copy text on Android
2. Open Klippy
3. Tap "Push" button
4. On another device, tap "Pull" button
5. Clipboard is now synced

## Security

- **RSA-4096**: Industry-standard key length
- **ASCII-armored**: PGP messages are ASCII-armored for JSON transport
- **No passphrase**: Keys have no passphrase (store securely!)
- **Server-side**: Server only stores encrypted data, cannot decrypt
- **Transport**: Use HTTPS in production (configure reverse proxy)
- **Android storage**: Keys stored in EncryptedSharedPreferences

## Testing

### Server Tests

```bash
cd server
npm test
```

## Architecture

```
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│   Device A  │         │   Server    │         │   Device B  │
│             │         │             │         │             │
│ Plaintext   │         │             │         │             │
│     ↓       │         │             │         │             │
│ Encrypt     │         │             │         │             │
│  (PGP)      │         │             │         │             │
│     ↓       │         │             │         │             │
│  POST ──────┼────────→│   Store     │         │             │
│             │         │ (encrypted) │         │             │
│             │         │     ↓       │         │             │
│             │         │             │←────────┼─── GET      │
│             │         │             │         │     ↓       │
│             │         │             │         │  Decrypt    │
│             │         │             │         │   (PGP)     │
│             │         │             │         │     ↓       │
│             │         │             │         │ Plaintext   │
└─────────────┘         └─────────────┘         └─────────────┘
```

## Project Structure

```
klippy/
├── server/
│   ├── src/
│   │   ├── server.js       # HTTP server
│   │   └── store.js        # Data persistence
│   ├── test/
│   │   └── server.test.js  # Unit tests
│   ├── Dockerfile
│   ├── docker-compose.yml
│   └── package.json
├── client/
│   └── klippy.sh           # Bash client
├── android/
│   └── app/
│       └── src/main/
│           ├── java/net/aiouti/klippy/
│           │   ├── MainActivity.kt
│           │   ├── SettingsActivity.kt
│           │   ├── CryptoHelper.kt
│           │   ├── ApiClient.kt
│           │   └── KeyRepository.kt
│           ├── res/
│           └── AndroidManifest.xml
└── keygen-and-setup.sh     # Key generation
```

## Requirements

### Server
- Node.js 22+
- Docker (optional)

### Linux Client
- Bash
- GPG
- `wl-clipboard` (Wayland) or `xclip` (X11)
- `curl`
- `jq`

### Android
- Android 8.0+ (API 26)
- Internet permission

## License

MIT

## Author

Klippy - Secure clipboard sharing
