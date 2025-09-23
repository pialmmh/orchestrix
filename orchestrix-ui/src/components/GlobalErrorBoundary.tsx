import React, { Component, ErrorInfo, ReactNode } from 'react';
import { Box, Typography, Paper, Button, Stack } from '@mui/material';
import { Error as ErrorIcon } from '@mui/icons-material';
import storeWebSocketService from '../services/StoreWebSocketService';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
  errorInfo: ErrorInfo | null;
  errorCount: number;
}

class GlobalErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      hasError: false,
      error: null,
      errorInfo: null,
      errorCount: 0
    };
  }

  static getDerivedStateFromError(error: Error): Partial<State> {
    return { hasError: true };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    // Log error through WebSocket
    this.logError('React Error Boundary', error, errorInfo);

    this.setState({
      error,
      errorInfo,
      errorCount: this.state.errorCount + 1
    });
  }

  private logError(source: string, error: Error, errorInfo?: ErrorInfo | null) {
    const errorData = {
      source,
      message: error.message,
      stack: error.stack,
      componentStack: errorInfo?.componentStack,
      timestamp: new Date().toISOString(),
      userAgent: navigator.userAgent,
      url: window.location.href,
      errorCount: this.state.errorCount + 1
    };

    // Log to WebSocket using console logs format
    if (storeWebSocketService && storeWebSocketService.isConnected) {
      const errorLog = {
        level: 'ERROR',
        message: `[${source}] ${error.message}`,
        timestamp: new Date().toISOString(),
        metadata: errorData
      };
      storeWebSocketService.sendConsoleLogs([errorLog]);
    }

    // Also log to console for debugging
    console.error(`[${source}]`, error, errorInfo);
  }

  handleReset = () => {
    this.setState({
      hasError: false,
      error: null,
      errorInfo: null
    });
  };

  handleReload = () => {
    window.location.reload();
  };

  render() {
    if (this.state.hasError && this.state.error) {
      return (
        <Box
          sx={{
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            minHeight: '100vh',
            backgroundColor: '#f5f5f5',
            padding: 3
          }}
        >
          <Paper
            elevation={3}
            sx={{
              padding: 4,
              maxWidth: 600,
              width: '100%'
            }}
          >
            <Stack spacing={3} alignItems="center">
              <ErrorIcon sx={{ fontSize: 64, color: 'error.main' }} />

              <Typography variant="h4" component="h1" gutterBottom>
                Oops! Something went wrong
              </Typography>

              <Typography variant="body1" color="text.secondary" align="center">
                An unexpected error has occurred. The error has been logged and will be reviewed.
              </Typography>

              {process.env.NODE_ENV === 'development' && (
                <Paper
                  sx={{
                    padding: 2,
                    backgroundColor: '#f5f5f5',
                    width: '100%',
                    maxHeight: 300,
                    overflow: 'auto'
                  }}
                  variant="outlined"
                >
                  <Typography variant="subtitle2" gutterBottom>
                    Error Details (Development Mode):
                  </Typography>
                  <Typography variant="body2" component="pre" sx={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>
                    {this.state.error.toString()}
                  </Typography>
                  {this.state.error.stack && (
                    <Typography variant="body2" component="pre" sx={{ fontFamily: 'monospace', fontSize: '0.75rem', mt: 2 }}>
                      {this.state.error.stack}
                    </Typography>
                  )}
                </Paper>
              )}

              <Stack direction="row" spacing={2}>
                <Button
                  variant="contained"
                  onClick={this.handleReset}
                  color="primary"
                >
                  Try Again
                </Button>
                <Button
                  variant="outlined"
                  onClick={this.handleReload}
                  color="secondary"
                >
                  Reload Page
                </Button>
              </Stack>

              <Typography variant="caption" color="text.secondary">
                Error Count: {this.state.errorCount}
              </Typography>
            </Stack>
          </Paper>
        </Box>
      );
    }

    return this.props.children;
  }
}

export default GlobalErrorBoundary;