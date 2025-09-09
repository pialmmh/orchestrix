import React, { useState, useRef } from 'react';
import {
  Box,
  Typography,
  Paper,
  TextField,
  Button,
  Alert,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Tabs,
  Tab,
  Card,
  CardContent,
  Chip,
  IconButton,
  Tooltip,
  Divider
} from '@mui/material';
import Grid from '@mui/material/Grid';
import {
  PlayArrow,
  Stop,
  Send,
  History,
  Delete,
  ContentCopy,
  Code,
  Terminal as TerminalIcon
} from '@mui/icons-material';
import WebSocketTerminal from '../components/WebSocketTerminal';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;
  return (
    <div hidden={value !== index} {...other}>
      {value === index && <Box sx={{ pt: 3 }}>{children}</Box>}
    </div>
  );
}

interface ConnectionHistory {
  url: string;
  timestamp: Date;
  status: 'success' | 'failed';
}

const WebSocketTest: React.FC = () => {
  const [activeTab, setActiveTab] = useState(0);
  const [wsUrl, setWsUrl] = useState('ws://localhost:8080/ws');
  const [isConnected, setIsConnected] = useState(false);
  const [connectionHistory, setConnectionHistory] = useState<ConnectionHistory[]>([]);
  const [messageToSend, setMessageToSend] = useState('');
  const [protocol, setProtocol] = useState('ws');
  const [host, setHost] = useState('localhost');
  const [port, setPort] = useState('8080');
  const [path, setPath] = useState('/ws');
  // const terminalRef = useRef<any>(null); // Ref not needed since WebSocketTerminal doesn't support refs

  // Sample WebSocket servers for testing
  const sampleServers = [
    { name: 'Echo Server', url: 'wss://echo.websocket.org' },
    { name: 'Local Spring Boot', url: 'ws://localhost:8080/ws' },
    { name: 'Socket.IO Test', url: 'ws://localhost:3000/socket.io/?EIO=4&transport=websocket' },
    { name: 'Binance Stream', url: 'wss://stream.binance.com:9443/ws/btcusdt@trade' },
    { name: 'Blockchain.info', url: 'wss://ws.blockchain.info/inv' }
  ];

  // Build URL from components
  const buildUrl = () => {
    const url = `${protocol}://${host}:${port}${path}`;
    setWsUrl(url);
    return url;
  };

  const handleConnect = () => {
    const url = buildUrl();
    
    // Add to history
    setConnectionHistory(prev => [{
      url,
      timestamp: new Date(),
      status: 'success'
    }, ...prev.slice(0, 9)]);

    // Terminal will handle the connection
    setIsConnected(true);
  };

  const handleDisconnect = () => {
    // The terminal component handles disconnection internally
    setIsConnected(false);
  };

  const handleSendMessage = () => {
    if (messageToSend) {
      // Message sending would be handled through the WebSocket connection
      // The WebSocketTerminal component manages its own WebSocket internally
      // For now, just clear the message field
      setMessageToSend('');
      // TODO: Implement message sending through WebSocket when connected
    }
  };

  const handleConnectionChange = (connected: boolean) => {
    setIsConnected(connected);
    
    if (!connected && connectionHistory.length > 0) {
      // Update last history item status if disconnected
      const updatedHistory = [...connectionHistory];
      if (updatedHistory[0].status === 'success') {
        updatedHistory[0].status = 'failed';
        setConnectionHistory(updatedHistory);
      }
    }
  };

  const copyUrl = () => {
    navigator.clipboard.writeText(wsUrl);
  };

  const clearHistory = () => {
    setConnectionHistory([]);
  };

  const loadSampleServer = (url: string) => {
    // Parse the URL
    const urlObj = new URL(url);
    setProtocol(urlObj.protocol.replace(':', ''));
    setHost(urlObj.hostname);
    setPort(urlObj.port || (urlObj.protocol === 'wss:' ? '443' : '80'));
    setPath(urlObj.pathname + urlObj.search);
    setWsUrl(url);
  };

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        WebSocket Terminal Test
      </Typography>
      
      <Alert severity="info" sx={{ mb: 3 }}>
        Test WebSocket connections with a real-time terminal view. Connect to any WebSocket server
        and see the messages in a terminal-like interface.
      </Alert>

      <Grid container spacing={3}>
        {/* Connection Configuration */}
        <Grid item xs={12} lg={4}>
          <Paper sx={{ p: 3, height: '100%' }}>
            <Typography variant="h6" gutterBottom>
              Connection Settings
            </Typography>
            
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <FormControl fullWidth size="small">
                <InputLabel>Protocol</InputLabel>
                <Select
                  value={protocol}
                  onChange={(e) => setProtocol(e.target.value)}
                  label="Protocol"
                >
                  <MenuItem value="ws">ws://</MenuItem>
                  <MenuItem value="wss">wss:// (Secure)</MenuItem>
                </Select>
              </FormControl>

              <TextField
                label="Host"
                value={host}
                onChange={(e) => setHost(e.target.value)}
                size="small"
                fullWidth
              />

              <TextField
                label="Port"
                value={port}
                onChange={(e) => setPort(e.target.value)}
                size="small"
                fullWidth
              />

              <TextField
                label="Path"
                value={path}
                onChange={(e) => setPath(e.target.value)}
                size="small"
                fullWidth
                placeholder="/ws"
              />

              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <TextField
                  label="Full URL"
                  value={wsUrl}
                  onChange={(e) => setWsUrl(e.target.value)}
                  size="small"
                  fullWidth
                  InputProps={{
                    readOnly: true
                  }}
                />
                <Tooltip title="Copy URL">
                  <IconButton onClick={copyUrl} size="small">
                    <ContentCopy />
                  </IconButton>
                </Tooltip>
              </Box>

              <Box sx={{ display: 'flex', gap: 1 }}>
                {!isConnected ? (
                  <Button
                    variant="contained"
                    color="success"
                    startIcon={<PlayArrow />}
                    onClick={handleConnect}
                    fullWidth
                  >
                    Connect
                  </Button>
                ) : (
                  <Button
                    variant="contained"
                    color="error"
                    startIcon={<Stop />}
                    onClick={handleDisconnect}
                    fullWidth
                  >
                    Disconnect
                  </Button>
                )}
              </Box>

              <Chip
                label={isConnected ? 'Connected' : 'Disconnected'}
                color={isConnected ? 'success' : 'default'}
                variant={isConnected ? 'filled' : 'outlined'}
              />
            </Box>

            <Divider sx={{ my: 2 }} />

            <Typography variant="subtitle2" gutterBottom>
              Sample Servers
            </Typography>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
              {sampleServers.map((server) => (
                <Button
                  key={server.url}
                  size="small"
                  variant="outlined"
                  onClick={() => loadSampleServer(server.url)}
                  sx={{ justifyContent: 'flex-start' }}
                >
                  {server.name}
                </Button>
              ))}
            </Box>
          </Paper>
        </Grid>

        {/* Terminal and Controls */}
        <Grid item xs={12} lg={8}>
          <Paper sx={{ mb: 3 }}>
            <Tabs value={activeTab} onChange={(e, v) => setActiveTab(v)}>
              <Tab label="Terminal" icon={<TerminalIcon />} />
              <Tab label="Send Message" icon={<Send />} />
              <Tab label="Connection History" icon={<History />} />
              <Tab label="Examples" icon={<Code />} />
            </Tabs>

            <TabPanel value={activeTab} index={0}>
              <WebSocketTerminal
                wsUrl={isConnected ? wsUrl : undefined}
                height="500px"
                theme="dark"
                onConnectionChange={handleConnectionChange}
                autoConnect={isConnected}
                showControls={true}
                title="WebSocket Terminal"
              />
            </TabPanel>

            <TabPanel value={activeTab} index={1}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    Send Message
                  </Typography>
                  <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
                    <TextField
                      label="Message"
                      value={messageToSend}
                      onChange={(e) => setMessageToSend(e.target.value)}
                      fullWidth
                      multiline
                      rows={4}
                      placeholder='{"type": "message", "content": "Hello WebSocket!"}'
                    />
                  </Box>
                  <Button
                    variant="contained"
                    startIcon={<Send />}
                    onClick={handleSendMessage}
                    disabled={!isConnected || !messageToSend}
                  >
                    Send Message
                  </Button>
                  
                  <Box sx={{ mt: 3 }}>
                    <Typography variant="subtitle2" gutterBottom>
                      Quick Messages
                    </Typography>
                    <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                      <Chip
                        label="Ping"
                        onClick={() => setMessageToSend('{"type": "ping"}')}
                        clickable
                      />
                      <Chip
                        label="Subscribe"
                        onClick={() => setMessageToSend('{"type": "subscribe", "channel": "test"}')}
                        clickable
                      />
                      <Chip
                        label="Unsubscribe"
                        onClick={() => setMessageToSend('{"type": "unsubscribe", "channel": "test"}')}
                        clickable
                      />
                      <Chip
                        label="Echo Test"
                        onClick={() => setMessageToSend('Echo test message')}
                        clickable
                      />
                    </Box>
                  </Box>
                </CardContent>
              </Card>
            </TabPanel>

            <TabPanel value={activeTab} index={2}>
              <Card>
                <CardContent>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                    <Typography variant="h6">
                      Connection History
                    </Typography>
                    <Button
                      size="small"
                      startIcon={<Delete />}
                      onClick={clearHistory}
                      disabled={connectionHistory.length === 0}
                    >
                      Clear
                    </Button>
                  </Box>
                  
                  {connectionHistory.length === 0 ? (
                    <Typography color="text.secondary">
                      No connection history yet
                    </Typography>
                  ) : (
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                      {connectionHistory.map((item, index) => (
                        <Box
                          key={index}
                          sx={{
                            p: 1.5,
                            border: '1px solid',
                            borderColor: 'divider',
                            borderRadius: 1,
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center'
                          }}
                        >
                          <Box>
                            <Typography variant="body2">
                              {item.url}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                              {item.timestamp.toLocaleString()}
                            </Typography>
                          </Box>
                          <Chip
                            label={item.status}
                            size="small"
                            color={item.status === 'success' ? 'success' : 'error'}
                          />
                        </Box>
                      ))}
                    </Box>
                  )}
                </CardContent>
              </Card>
            </TabPanel>

            <TabPanel value={activeTab} index={3}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    WebSocket Examples
                  </Typography>
                  
                  <Typography variant="subtitle1" gutterBottom sx={{ mt: 2 }}>
                    1. Echo Server (wss://echo.websocket.org)
                  </Typography>
                  <Typography variant="body2" color="text.secondary" paragraph>
                    Echoes back any message you send. Great for testing basic connectivity.
                  </Typography>
                  
                  <Typography variant="subtitle1" gutterBottom>
                    2. Binance Trade Stream
                  </Typography>
                  <Typography variant="body2" color="text.secondary" paragraph>
                    URL: wss://stream.binance.com:9443/ws/btcusdt@trade
                    <br />
                    Streams real-time Bitcoin/USDT trades.
                  </Typography>
                  
                  <Typography variant="subtitle1" gutterBottom>
                    3. Spring Boot WebSocket
                  </Typography>
                  <Typography variant="body2" color="text.secondary" paragraph>
                    For local Spring Boot apps with WebSocket support:
                    <br />
                    URL: ws://localhost:8080/ws
                  </Typography>
                  
                  <Typography variant="subtitle1" gutterBottom>
                    4. Custom STOMP Endpoint
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    For STOMP over WebSocket:
                    <br />
                    URL: ws://localhost:8080/ws-stomp
                    <br />
                    Then send CONNECT frame: {`CONNECT\naccept-version:1.2\n\n\0`}
                  </Typography>
                </CardContent>
              </Card>
            </TabPanel>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};

export default WebSocketTest;