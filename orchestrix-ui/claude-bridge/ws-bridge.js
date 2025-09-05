const WebSocket = require('ws');
const express = require('express');
const cors = require('cors');

const WS_PORT = 3456;
const HTTP_PORT = 3457;

// Create WebSocket server
const wss = new WebSocket.Server({ port: WS_PORT });

// Create HTTP server for REST API
const app = express();
app.use(cors());
app.use(express.json({ limit: '10mb' }));

let connectedClients = [];
let currentAppState = {};

console.log(`🌉 Claude Bridge WebSocket Server`);
console.log(`================================`);
console.log(`WebSocket: ws://localhost:${WS_PORT}`);
console.log(`HTTP API: http://localhost:${HTTP_PORT}`);
console.log(`Waiting for connections...`);

wss.on('connection', (ws) => {
  console.log('✅ New WebSocket client connected');
  connectedClients.push(ws);

  ws.on('message', (data) => {
    try {
      const message = JSON.parse(data);
      console.log('📨 Received message:', message.type);
      
      if (message.type === 'init') {
        currentAppState = message.data;
        console.log('🎯 App initialized:', message.data);
      }
    } catch (error) {
      console.error('❌ Error parsing message:', error);
    }
  });

  ws.on('close', () => {
    console.log('👋 Client disconnected');
    connectedClients = connectedClients.filter(client => client !== ws);
  });

  ws.on('error', (error) => {
    console.error('❌ WebSocket error:', error);
  });
});

// HTTP endpoints for Claude Code to interact with the app
app.post('/eval', (req, res) => {
  const { code } = req.body;
  const id = Date.now();
  
  if (connectedClients.length === 0) {
    return res.status(503).json({ error: 'No connected clients' });
  }
  
  const client = connectedClients[0];
  
  // Send eval request to browser
  client.send(JSON.stringify({
    type: 'eval',
    id: id,
    code: code
  }));
  
  // Wait for response (with timeout)
  let responded = false;
  const timeout = setTimeout(() => {
    if (!responded) {
      res.status(504).json({ error: 'Timeout waiting for eval response' });
    }
  }, 5000);
  
  // Listen for response
  const messageHandler = (data) => {
    try {
      const message = JSON.parse(data);
      if ((message.type === 'eval-result' || message.type === 'eval-error') && message.id === id) {
        responded = true;
        clearTimeout(timeout);
        client.removeListener('message', messageHandler);
        
        if (message.type === 'eval-result') {
          res.json({ result: message.result });
        } else {
          res.status(400).json({ error: message.error });
        }
      }
    } catch (error) {
      // Ignore parse errors
    }
  };
  
  client.on('message', messageHandler);
});

app.get('/state', (req, res) => {
  if (connectedClients.length === 0) {
    return res.status(503).json({ error: 'No connected clients' });
  }
  
  const client = connectedClients[0];
  const id = Date.now();
  
  client.send(JSON.stringify({
    type: 'get-state',
    id: id
  }));
  
  let responded = false;
  const timeout = setTimeout(() => {
    if (!responded) {
      res.status(504).json({ error: 'Timeout waiting for state' });
    }
  }, 5000);
  
  const messageHandler = (data) => {
    try {
      const message = JSON.parse(data);
      if (message.type === 'state' && message.id === id) {
        responded = true;
        clearTimeout(timeout);
        client.removeListener('message', messageHandler);
        res.json(message.state);
      }
    } catch (error) {
      // Ignore parse errors
    }
  };
  
  client.on('message', messageHandler);
});

app.get('/health', (req, res) => {
  res.json({
    status: 'running',
    clients: connectedClients.length,
    appState: currentAppState
  });
});

app.listen(HTTP_PORT, () => {
  console.log(`✅ HTTP API listening on port ${HTTP_PORT}`);
});