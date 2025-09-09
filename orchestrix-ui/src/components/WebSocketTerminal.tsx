import React, { useEffect, useRef, useState, useCallback } from 'react';
import { Terminal } from 'xterm';
import { FitAddon } from 'xterm-addon-fit';
import { SearchAddon } from 'xterm-addon-search';
import { WebLinksAddon } from 'xterm-addon-web-links';
import { Box, IconButton, TextField, Tooltip, Paper, Typography } from '@mui/material';
import {
  Clear,
  Search,
  ContentCopy,
  Download,
  Fullscreen,
  FullscreenExit,
  PlayArrow,
  Pause
} from '@mui/icons-material';
import 'xterm/css/xterm.css';

interface WebSocketTerminalProps {
  wsUrl?: string;
  height?: string | number;
  theme?: 'dark' | 'light' | 'custom';
  customTheme?: any;
  onConnectionChange?: (connected: boolean) => void;
  onMessage?: (message: string) => void;
  autoConnect?: boolean;
  showControls?: boolean;
  title?: string;
}

const WebSocketTerminal: React.FC<WebSocketTerminalProps> = ({
  wsUrl,
  height = '500px',
  theme = 'dark',
  customTheme,
  onConnectionChange,
  onMessage,
  autoConnect = false,
  showControls = true,
  title
}) => {
  const terminalRef = useRef<HTMLDivElement>(null);
  const terminal = useRef<Terminal | null>(null);
  const fitAddon = useRef<FitAddon | null>(null);
  const searchAddon = useRef<SearchAddon | null>(null);
  const ws = useRef<WebSocket | null>(null);
  
  const [isConnected, setIsConnected] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [isPaused, setIsPaused] = useState(false);
  const [searchVisible, setSearchVisible] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [messageBuffer, setMessageBuffer] = useState<string[]>([]);

  // Terminal themes
  const themes = {
    dark: {
      background: '#1e1e1e',
      foreground: '#d4d4d4',
      cursor: '#d4d4d4',
      cursorAccent: '#1e1e1e',
      selection: '#3a3d41',
      black: '#1e1e1e',
      red: '#f44747',
      green: '#608b4e',
      yellow: '#dcdcaa',
      blue: '#569cd6',
      magenta: '#c678dd',
      cyan: '#56b6c2',
      white: '#d4d4d4',
      brightBlack: '#666666',
      brightRed: '#f44747',
      brightGreen: '#608b4e',
      brightYellow: '#dcdcaa',
      brightBlue: '#569cd6',
      brightMagenta: '#c678dd',
      brightCyan: '#56b6c2',
      brightWhite: '#ffffff'
    },
    light: {
      background: '#ffffff',
      foreground: '#383838',
      cursor: '#383838',
      cursorAccent: '#ffffff',
      selection: '#b5d5ff',
      black: '#000000',
      red: '#c41a16',
      green: '#448c27',
      yellow: '#cb9000',
      blue: '#1e6fcc',
      magenta: '#a90d91',
      cyan: '#0aa1a7',
      white: '#a8a8a8',
      brightBlack: '#686868',
      brightRed: '#ff3e3e',
      brightGreen: '#83d082',
      brightYellow: '#ffdd33',
      brightBlue: '#5eb7f7',
      brightMagenta: '#f86adf',
      brightCyan: '#4ed9d9',
      brightWhite: '#ffffff'
    }
  };

  // Initialize terminal
  useEffect(() => {
    if (!terminalRef.current) return;

    const term = new Terminal({
      cursorBlink: true,
      fontSize: 14,
      fontFamily: '"Cascadia Code", "Courier New", monospace',
      lineHeight: 1.2,
      letterSpacing: 0,
      theme: customTheme || (theme === 'custom' ? themes.dark : themes[theme]),
      scrollback: 10000,
      convertEol: true
    });

    terminal.current = term;
    term.open(terminalRef.current);

    // Add addons
    fitAddon.current = new FitAddon();
    term.loadAddon(fitAddon.current);
    
    searchAddon.current = new SearchAddon();
    term.loadAddon(searchAddon.current);
    
    const webLinksAddon = new WebLinksAddon();
    term.loadAddon(webLinksAddon);

    // Initial fit
    fitAddon.current.fit();

    // Welcome message
    term.writeln('\x1b[1;34m=== WebSocket Terminal ===\x1b[0m');
    term.writeln('Ready to connect to WebSocket server...');
    term.writeln('');

    // Handle window resize
    const handleResize = () => {
      if (fitAddon.current) {
        fitAddon.current.fit();
      }
    };
    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
      term.dispose();
    };
  }, [theme, customTheme]);

  // Auto-connect if URL provided
  useEffect(() => {
    if (autoConnect && wsUrl) {
      connectWebSocket(wsUrl);
    }
  }, [wsUrl, autoConnect]);

  const connectWebSocket = useCallback((url: string) => {
    if (ws.current?.readyState === WebSocket.OPEN) {
      ws.current.close();
    }

    try {
      ws.current = new WebSocket(url);
      
      ws.current.onopen = () => {
        setIsConnected(true);
        onConnectionChange?.(true);
        terminal.current?.writeln(`\x1b[32m✓ Connected to ${url}\x1b[0m`);
        terminal.current?.writeln('');
      };

      ws.current.onmessage = (event) => {
        const message = event.data;
        
        if (!isPaused && terminal.current) {
          // Handle different message formats
          if (typeof message === 'string') {
            try {
              // Try to parse as JSON
              const jsonData = JSON.parse(message);
              if (jsonData.content) {
                terminal.current.writeln(jsonData.content);
              } else {
                terminal.current.writeln(JSON.stringify(jsonData, null, 2));
              }
            } catch {
              // Plain text message
              terminal.current.writeln(message);
            }
          } else {
            terminal.current.writeln(message.toString());
          }
          
          onMessage?.(message);
        } else {
          // Buffer messages when paused
          setMessageBuffer(prev => [...prev, message]);
        }
      };

      ws.current.onerror = (error) => {
        terminal.current?.writeln(`\x1b[31m✗ WebSocket error occurred\x1b[0m`);
        console.error('WebSocket error:', error);
      };

      ws.current.onclose = () => {
        setIsConnected(false);
        onConnectionChange?.(false);
        terminal.current?.writeln(`\x1b[33m⚠ Disconnected from WebSocket\x1b[0m`);
      };
    } catch (error) {
      terminal.current?.writeln(`\x1b[31m✗ Failed to connect: ${error}\x1b[0m`);
      console.error('Connection error:', error);
    }
  }, [isPaused, onConnectionChange, onMessage]);

  const disconnect = useCallback(() => {
    if (ws.current) {
      ws.current.close();
      ws.current = null;
    }
  }, []);

  const clearTerminal = useCallback(() => {
    terminal.current?.clear();
    terminal.current?.writeln('\x1b[1;34m=== Terminal Cleared ===\x1b[0m');
    terminal.current?.writeln('');
  }, []);

  const copyContent = useCallback(() => {
    if (terminal.current) {
      const selection = terminal.current.getSelection();
      if (selection) {
        navigator.clipboard.writeText(selection);
      } else {
        // Copy entire buffer
        const buffer = terminal.current.buffer.active;
        let content = '';
        for (let i = 0; i < buffer.length; i++) {
          const line = buffer.getLine(i);
          if (line) {
            content += line.translateToString() + '\n';
          }
        }
        navigator.clipboard.writeText(content);
      }
    }
  }, []);

  const downloadLog = useCallback(() => {
    if (terminal.current) {
      const buffer = terminal.current.buffer.active;
      let content = '';
      for (let i = 0; i < buffer.length; i++) {
        const line = buffer.getLine(i);
        if (line) {
          content += line.translateToString() + '\n';
        }
      }
      
      const blob = new Blob([content], { type: 'text/plain' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `terminal-log-${new Date().getTime()}.txt`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    }
  }, []);

  const togglePause = useCallback(() => {
    setIsPaused(prev => {
      const newPaused = !prev;
      if (!newPaused && messageBuffer.length > 0) {
        // Flush buffer when unpausing
        messageBuffer.forEach(msg => {
          terminal.current?.writeln(msg);
        });
        setMessageBuffer([]);
      }
      return newPaused;
    });
  }, [messageBuffer]);

  const handleSearch = useCallback(() => {
    if (searchTerm && searchAddon.current) {
      searchAddon.current.findNext(searchTerm);
    }
  }, [searchTerm]);

  const toggleFullscreen = useCallback(() => {
    setIsFullscreen(prev => !prev);
    setTimeout(() => {
      fitAddon.current?.fit();
    }, 100);
  }, []);

  // Send message to WebSocket (for bidirectional communication)
  const sendMessage = useCallback((message: string) => {
    if (ws.current?.readyState === WebSocket.OPEN) {
      ws.current.send(message);
      terminal.current?.writeln(`\x1b[36m→ ${message}\x1b[0m`);
    }
  }, []);

  // Note: To expose methods, wrap component with forwardRef when needed

  return (
    <Paper
      elevation={3}
      sx={{
        display: 'flex',
        flexDirection: 'column',
        height: isFullscreen ? '100vh' : height,
        width: '100%',
        position: isFullscreen ? 'fixed' : 'relative',
        top: isFullscreen ? 0 : 'auto',
        left: isFullscreen ? 0 : 'auto',
        zIndex: isFullscreen ? 9999 : 'auto',
        bgcolor: theme === 'custom' ? (customTheme?.background || themes.dark.background) : themes[theme].background
      }}
    >
      {showControls && (
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            p: 1,
            borderBottom: '1px solid rgba(255,255,255,0.1)',
            bgcolor: 'rgba(0,0,0,0.2)'
          }}
        >
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            {title && (
              <Typography variant="subtitle2" sx={{ color: 'text.secondary', mr: 2 }}>
                {title}
              </Typography>
            )}
            <Tooltip title={isPaused ? "Resume" : "Pause"}>
              <IconButton size="small" onClick={togglePause} color={isPaused ? "warning" : "default"}>
                {isPaused ? <PlayArrow /> : <Pause />}
              </IconButton>
            </Tooltip>
            <Tooltip title="Clear">
              <IconButton size="small" onClick={clearTerminal}>
                <Clear />
              </IconButton>
            </Tooltip>
            <Tooltip title="Copy">
              <IconButton size="small" onClick={copyContent}>
                <ContentCopy />
              </IconButton>
            </Tooltip>
            <Tooltip title="Download Log">
              <IconButton size="small" onClick={downloadLog}>
                <Download />
              </IconButton>
            </Tooltip>
            <Tooltip title="Search">
              <IconButton 
                size="small" 
                onClick={() => setSearchVisible(!searchVisible)}
                color={searchVisible ? "primary" : "default"}
              >
                <Search />
              </IconButton>
            </Tooltip>
            <Tooltip title={isFullscreen ? "Exit Fullscreen" : "Fullscreen"}>
              <IconButton size="small" onClick={toggleFullscreen}>
                {isFullscreen ? <FullscreenExit /> : <Fullscreen />}
              </IconButton>
            </Tooltip>
          </Box>
          
          {searchVisible && (
            <Box sx={{ display: 'flex', gap: 1 }}>
              <TextField
                size="small"
                placeholder="Search..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
                sx={{ width: 200 }}
              />
              <IconButton size="small" onClick={handleSearch}>
                <Search />
              </IconButton>
            </Box>
          )}
        </Box>
      )}
      
      <Box
        ref={terminalRef}
        sx={{
          flexGrow: 1,
          overflow: 'hidden',
          position: 'relative'
        }}
      />
      
      {isPaused && messageBuffer.length > 0 && (
        <Box
          sx={{
            position: 'absolute',
            bottom: 10,
            right: 10,
            bgcolor: 'warning.main',
            color: 'warning.contrastText',
            px: 2,
            py: 0.5,
            borderRadius: 1,
            fontSize: '0.875rem'
          }}
        >
          {messageBuffer.length} messages buffered
        </Box>
      )}
    </Paper>
  );
};

export default WebSocketTerminal;