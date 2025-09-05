const express = require('express');
const cors = require('cors');
const fs = require('fs').promises;
const path = require('path');

const app = express();
app.use(cors());
app.use(express.json({ limit: '10mb' }));

// Store for app states
let currentState = {};
let stateHistory = [];
const MAX_HISTORY = 50;

// Endpoint for React app to send state
app.post('/api/state', (req, res) => {
  currentState = {
    ...req.body,
    timestamp: new Date().toISOString(),
    id: Date.now()
  };
  
  stateHistory.unshift(currentState);
  if (stateHistory.length > MAX_HISTORY) {
    stateHistory = stateHistory.slice(0, MAX_HISTORY);
  }
  
  const components = Object.keys(req.body.components || {});
  const errors = (req.body.errors || []).length;
  const time = new Date().toLocaleTimeString();
  
  console.log(`📥 [${time}] State received - Components: ${components.length}, Errors: ${errors}`);
  
  res.json({ success: true, id: currentState.id });
});

// Endpoints for Claude Code
app.get('/api/state', (req, res) => res.json(currentState));
app.get('/api/history', (req, res) => res.json(stateHistory));
app.get('/api/components', (req, res) => res.json(currentState.components || {}));
app.get('/api/console', (req, res) => res.json(currentState.console || []));
app.get('/api/storage', (req, res) => res.json(currentState.storage || {}));
app.get('/api/network', (req, res) => res.json(currentState.network || []));
app.get('/api/errors', (req, res) => res.json(currentState.errors || []));

// Save state to file
app.post('/api/save', async (req, res) => {
  const filename = `state-${Date.now()}.json`;
  await fs.writeFile(filename, JSON.stringify(currentState, null, 2));
  console.log(`💾 State saved to ${filename}`);
  res.json({ filename });
});

// Health check
app.get('/health', (req, res) => {
  res.json({ 
    status: 'running',
    hasState: Object.keys(currentState).length > 0,
    historyCount: stateHistory.length,
    uptime: process.uptime()
  });
});

const PORT = process.env.PORT || 9999;
app.listen(PORT, () => {
  console.log(`
🌉 Claude Bridge Server Running
================================
Server URL: http://localhost:${PORT}
Health Check: http://localhost:${PORT}/health

Endpoints for Claude Code:
--------------------------
GET  http://localhost:${PORT}/api/state      - Full current state
GET  http://localhost:${PORT}/api/components - React components
GET  http://localhost:${PORT}/api/console    - Console logs
GET  http://localhost:${PORT}/api/errors     - Errors
GET  http://localhost:${PORT}/api/network    - Network requests
GET  http://localhost:${PORT}/api/storage    - Browser storage
GET  http://localhost:${PORT}/api/history    - State history
POST http://localhost:${PORT}/api/save       - Save state to file

Waiting for React app to connect...
  `);
});
