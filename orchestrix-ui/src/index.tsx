import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App';
import reportWebVitals from './reportWebVitals';
import consoleRedirectService from './services/ConsoleRedirectService';
import GlobalErrorBoundary from './components/GlobalErrorBoundary';
import { initializeGlobalErrorHandlers } from './utils/globalErrorHandler';
import './services/TestConsoleRedirect'; // Test console redirect

// Initialize global error handlers first
// This captures all unhandled errors and promise rejections
initializeGlobalErrorHandlers();

// Initialize console redirect service if configured
// This will automatically start capturing console logs based on the active profile
consoleRedirectService.initialize();

const root = ReactDOM.createRoot(
  document.getElementById('root') as HTMLElement
);
root.render(
  <React.StrictMode>
    <GlobalErrorBoundary>
      <App />
    </GlobalErrorBoundary>
  </React.StrictMode>
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
