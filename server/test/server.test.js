const { test } = require('node:test');
const assert = require('node:assert');
const http = require('node:http');
const server = require('../src/server');

const BASE_URL = 'http://localhost:3000';

function request(method, path, data = null) {
  return new Promise((resolve, reject) => {
    const options = {
      method,
      headers: data ? { 'Content-Type': 'application/json' } : {}
    };

    const req = http.request(`${BASE_URL}${path}`, options, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, body: JSON.parse(body) });
        } catch {
          resolve({ status: res.statusCode, body });
        }
      });
    });

    req.on('error', reject);
    if (data) req.write(JSON.stringify(data));
    req.end();
  });
}

test('GET /health returns ok', async () => {
  const res = await request('GET', '/health');
  assert.strictEqual(res.status, 200);
  assert.strictEqual(res.body.status, 'ok');
});

test('POST /clipboard stores encrypted data', async () => {
  const testData = { encrypted: 'test-encrypted-data' };
  const res = await request('POST', '/clipboard', testData);
  assert.strictEqual(res.status, 200);
  assert.strictEqual(res.body.success, true);
});

test('GET /clipboard retrieves encrypted data', async () => {
  const res = await request('GET', '/clipboard');
  assert.strictEqual(res.status, 200);
  assert.strictEqual(res.body.encrypted, 'test-encrypted-data');
});

test('POST /clipboard rejects missing encrypted field', async () => {
  const res = await request('POST', '/clipboard', { wrong: 'field' });
  assert.strictEqual(res.status, 400);
});

test('GET /unknown returns 404', async () => {
  const res = await request('GET', '/unknown');
  assert.strictEqual(res.status, 404);
});

// Close server after tests
test.after(() => {
  server.close();
});
