import React, { useState, useEffect } from 'react';
import { Box, Paper, Typography, Button, TextField, Stack } from '@mui/material';

declare global {
  interface Window {
    ws: {
      log: (...args: any[]) => void;
    };
  }
}

const TestWSLogger: React.FC = () => {
  const [message, setMessage] = useState('');
  const [counter, setCounter] = useState(0);

  useEffect(() => {
    ws.log('TestWSLogger component mounted');

    return () => {
      ws.log('TestWSLogger component unmounted');
    };
  }, []);

  const handleSimpleLog = () => {
    ws.log('Simple log message at', new Date().toISOString());
  };

  const handleObjectLog = () => {
    const data = {
      timestamp: Date.now(),
      user: 'testuser',
      action: 'test_action',
      metadata: {
        counter: counter,
        browser: navigator.userAgent
      }
    };
    ws.log('Object log:', data);
    setCounter(counter + 1);
  };

  const handleCustomLog = () => {
    if (message) {
      ws.log('Custom message:', message);
      setMessage('');
    }
  };

  const handleErrorLog = () => {
    try {
      throw new Error('Test error for logging');
    } catch (error) {
      ws.log('Error caught:', error.message, error.stack);
    }
  };

  const handleMultipleArgs = () => {
    ws.log('Multiple', 'arguments', 'test', { number: 42 }, ['array', 'items']);
  };

  return (
    <Box sx={{ p: 3 }}>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h4" gutterBottom>
          WebSocket Logger Test
        </Typography>

        <Typography variant="body1" paragraph>
          Test the ws.log() functionality. Check browser console and backend logs.
        </Typography>

        <Stack spacing={2}>
          <Button
            variant="contained"
            onClick={handleSimpleLog}
          >
            Send Simple Log
          </Button>

          <Button
            variant="contained"
            color="secondary"
            onClick={handleObjectLog}
          >
            Send Object Log (Counter: {counter})
          </Button>

          <Button
            variant="contained"
            color="warning"
            onClick={handleErrorLog}
          >
            Send Error Log
          </Button>

          <Button
            variant="contained"
            color="info"
            onClick={handleMultipleArgs}
          >
            Send Multiple Arguments
          </Button>

          <Box sx={{ display: 'flex', gap: 2 }}>
            <TextField
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              placeholder="Enter custom message"
              fullWidth
            />
            <Button
              variant="contained"
              onClick={handleCustomLog}
              disabled={!message}
            >
              Send Custom
            </Button>
          </Box>
        </Stack>

        <Box sx={{ mt: 3, p: 2, bgcolor: 'grey.100', borderRadius: 1 }}>
          <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
            Check logs at: debug-store/store-debug/console-logs-*.jsonl
          </Typography>
        </Box>
      </Paper>
    </Box>
  );
};

export default TestWSLogger;