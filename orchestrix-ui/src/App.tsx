import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Provider } from 'react-redux';
import { ThemeProvider, CssBaseline } from '@mui/material';
import { store } from './store/store';
import { theme } from './theme/theme';
import { StoreProvider } from './stores/base/StoreProvider';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Users from './pages/Users';
import UserProfile from './pages/UserProfile';
import Partners from './pages/Partners';
import Clients from './pages/Clients';
import Deployments from './pages/Deployments';
import Servers from './pages/Servers';
import Security from './pages/Security';
import SecretProviders from './pages/SecretProviders';
import Reports from './pages/Reports';
import Settings from './pages/Settings';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';
import Layout from './components/Layout';
import LayoutNew from './components/LayoutNew';
import PrivateRoute from './components/PrivateRoute';
import Datacenters from './pages/resources/DatacentersNew';
import Compute from './pages/resources/Compute';
import Storage from './pages/resources/Storage';
import Countries from './pages/resources/Countries';
import InfrastructureCloudNative from './pages/InfrastructureCloudNative';
import InfrastructureStellar from './pages/InfrastructureStellar';
import RemoteAccess from './pages/RemoteAccess';
import WebSocketTest from './pages/WebSocketTest';
import './utils/debugAxios';
// @ts-ignore
import { useClaudeBridge } from './hooks/useClaudeBridge';

function App() {
  // Enable Claude Bridge for debugging in development
  useClaudeBridge(process.env.NODE_ENV === 'development');
  return (
    <Provider store={store}>
      <StoreProvider>
        <ThemeProvider theme={theme}>
          <CssBaseline />
          <Router>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/forgot-password" element={<ForgotPassword />} />
            <Route path="/reset-password" element={<ResetPassword />} />
            <Route
              path="/"
              element={
                <PrivateRoute>
                  <LayoutNew />
                </PrivateRoute>
              }
            >
              <Route index element={<Navigate to="/dashboard" replace />} />
              <Route path="dashboard" element={<Dashboard />} />
              <Route path="infrastructure" element={<InfrastructureCloudNative />} />
              <Route path="infrastructure-stellar" element={<InfrastructureStellar />} />
              <Route path="clouds" element={<Navigate to="/infrastructure" replace />} />
              <Route path="users" element={<Users />} />
              <Route path="users/:id" element={<UserProfile />} />
              <Route path="partners" element={<Partners />} />
              <Route path="clients" element={<Clients />} />
              <Route path="remote-access" element={<RemoteAccess />} />
              <Route path="deployments" element={<Deployments />} />
              <Route path="servers" element={<Servers />} />
              <Route path="security" element={<Security />} />
              <Route path="secret-providers" element={<SecretProviders />} />
              <Route path="reports" element={<Reports />} />
              <Route path="settings" element={<Settings />} />
              <Route path="profile" element={<UserProfile />} />
              <Route path="resources/countries" element={<Countries />} />
              <Route path="resources/datacenters" element={<Datacenters />} />
              <Route path="resources/compute" element={<Compute />} />
              <Route path="resources/storage" element={<Storage />} />
              <Route path="websocket-test" element={<WebSocketTest />} />
            </Route>
          </Routes>
          </Router>
        </ThemeProvider>
      </StoreProvider>
    </Provider>
  );
}

export default App;
