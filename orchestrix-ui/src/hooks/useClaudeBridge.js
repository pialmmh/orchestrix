import { useEffect } from 'react';

export const useClaudeBridge = (enabled = true) => {
  useEffect(() => {
    if (!enabled) return;

    const BRIDGE_PORT = 3456;
    let ws = null;
    let reconnectTimeout = null;

    const connect = () => {
      try {
        ws = new WebSocket(`ws://localhost:${BRIDGE_PORT}`);

        ws.onopen = () => {
          console.log('🌉 Claude Bridge connected');
          // Send initial app state
          ws.send(JSON.stringify({
            type: 'init',
            data: {
              url: window.location.href,
              userAgent: navigator.userAgent,
              timestamp: new Date().toISOString()
            }
          }));
        };

        ws.onmessage = (event) => {
          try {
            const message = JSON.parse(event.data);
            console.log('📨 Bridge message:', message);
            
            switch (message.type) {
              case 'eval':
                try {
                  // eslint-disable-next-line no-eval
                  const result = eval(message.code);
                  ws.send(JSON.stringify({
                    type: 'eval-result',
                    id: message.id,
                    result: result
                  }));
                } catch (error) {
                  ws.send(JSON.stringify({
                    type: 'eval-error',
                    id: message.id,
                    error: error.toString()
                  }));
                }
                break;
                
              case 'get-state':
                // Get React components state
                const components = document.querySelectorAll('[data-component]');
                const state = Array.from(components).map(comp => ({
                  name: comp.dataset.component,
                  props: comp.dataset.props ? JSON.parse(comp.dataset.props) : {}
                }));
                ws.send(JSON.stringify({
                  type: 'state',
                  id: message.id,
                  state: state
                }));
                break;
                
              default:
                console.log('Unknown message type:', message.type);
            }
          } catch (error) {
            console.error('Bridge message error:', error);
          }
        };

        ws.onclose = () => {
          console.log('🌉 Claude Bridge disconnected');
          // Attempt to reconnect after 3 seconds
          reconnectTimeout = setTimeout(connect, 3000);
        };

        ws.onerror = (error) => {
          console.error('🌉 Claude Bridge error:', error);
        };

        // Make websocket available globally for debugging
        window.__claudeBridge = ws;

      } catch (error) {
        console.error('Failed to connect to Claude Bridge:', error);
        // Retry connection after 3 seconds
        reconnectTimeout = setTimeout(connect, 3000);
      }
    };

    connect();

    // Cleanup on unmount
    return () => {
      if (reconnectTimeout) {
        clearTimeout(reconnectTimeout);
      }
      if (ws) {
        ws.close();
      }
    };
  }, [enabled]);
};