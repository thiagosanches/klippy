const fs = require('node:fs');
const path = require('node:path');

const DATA_DIR = path.join(__dirname, '../data');
const DATA_FILE = path.join(DATA_DIR, 'clipboard.json');

class Store {
  constructor() {
    this.data = { encrypted: null, timestamp: null };
    this.ensureDataDir();
    this.load();
  }

  ensureDataDir() {
    if (!fs.existsSync(DATA_DIR)) {
      fs.mkdirSync(DATA_DIR, { recursive: true });
    }
  }

  load() {
    try {
      if (fs.existsSync(DATA_FILE)) {
        const content = fs.readFileSync(DATA_FILE, 'utf8');
        this.data = JSON.parse(content);
      }
    } catch (err) {
      console.error('Error loading data:', err.message);
    }
  }

  save() {
    try {
      fs.writeFileSync(DATA_FILE, JSON.stringify(this.data, null, 2), 'utf8');
    } catch (err) {
      console.error('Error saving data:', err.message);
    }
  }

  get() {
    return this.data.encrypted;
  }

  set(encrypted) {
    this.data.encrypted = encrypted;
    this.data.timestamp = new Date().toISOString();
    this.save();
  }
}

module.exports = Store;
