const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const Store = require('./store');

// Load .env file if it exists
function loadEnv() {
  const envPath = path.join(__dirname, '../../.env');
  if (fs.existsSync(envPath)) {
    const envContent = fs.readFileSync(envPath, 'utf8');
    envContent.split('\n').forEach(line => {
      line = line.trim();
      if (line && !line.startsWith('#')) {
        const [key, ...valueParts] = line.split('=');
        const value = valueParts.join('=').trim();
        if (key && value && !process.env[key]) {
          process.env[key] = value;
        }
      }
    });
  }
}

loadEnv();

const PORT = process.env.SERVER_PORT || process.env.PORT || 3000;
const MAX_BODY_SIZE = 512 * 1024; // 512 KB

const store = new Store();

// Helper function to get client IP
function getClientIP(req) {
  return req.headers['x-forwarded-for']?.split(',')[0].trim() || 
         req.socket.remoteAddress || 
         'unknown';
}

// Helper function to log requests
function logRequest(req, statusCode) {
  const timestamp = new Date().toISOString();
  const ip = getClientIP(req);
  console.log(`[${timestamp}] ${ip} ${req.method} ${req.url} -> ${statusCode}`);
}

const server = http.createServer((req, res) => {
  // CORS headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    logRequest(req, 204);
    return;
  }

  // Health check
  if (req.url === '/health' && req.method === 'GET') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'ok' }));
    logRequest(req, 200);
    return;
  }

  // GET /clipboard - retrieve encrypted clipboard data
  if (req.url === '/clipboard' && req.method === 'GET') {
    const encrypted = store.get();
    if (encrypted) {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ encrypted }));
      logRequest(req, 200);
    } else {
      res.writeHead(404, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'No clipboard data' }));
      logRequest(req, 404);
    }
    return;
  }

  // POST /clipboard - store encrypted clipboard data
  if (req.url === '/clipboard' && req.method === 'POST') {
    let body = '';
    let size = 0;
    let responded = false;

    req.on('data', chunk => {
      if (responded) return;
      size += chunk.length;
      if (size > MAX_BODY_SIZE) {
        responded = true;
        res.writeHead(413, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Payload too large' }));
        logRequest(req, 413);
        req.destroy();
        return;
      }
      body += chunk.toString();
    });

    req.on('end', () => {
      if (responded) return;
      responded = true;
      try {
        const data = JSON.parse(body);
        if (!data.encrypted || typeof data.encrypted !== 'string') {
          res.writeHead(400, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: 'Invalid request: encrypted field required' }));
          logRequest(req, 400);
          return;
        }

        store.set(data.encrypted);
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: true }));
        logRequest(req, 200);
      } catch (err) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Invalid JSON' }));
        logRequest(req, 400);
      }
    });

    return;
  }

  // 404 for all other routes
  res.writeHead(404, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({ error: 'Not found' }));
  logRequest(req, 404);
});

server.listen(PORT, () => {
  console.log(`Klippy server listening on port ${PORT}`);
});

module.exports = server;
