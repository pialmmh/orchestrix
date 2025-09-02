import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Provider } from 'react-redux';
import { ThemeProvider, CssBaseline } from '@mui/material';
import { store } from './store/store';
import { theme } from './theme/theme';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Users from './pages/Users';
import UserProfile from './pages/UserProfile';
import Partners from './pages/Partners';
import Clients from './pages/Clients';
import Deployments from './pages/Deployments';
import Servers from './pages/Servers';
import Security from './pages/Security';
import Reports from './pages/Reports';
import Settings from './pages/Settings';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';
import Layout from './components/Layout';
import PrivateRoute from './components/PrivateRoute';
import Datacenters from './pages/resources/DatacentersNew';
import Compute from './pages/resources/Compute';
import Storage from './pages/resources/Storage';
import Countries from './pages/resources/Countries';
import './utils/debugAxios';

function App() {
  return (
    <Provider store={store}>
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
                  <Layout />
                </PrivateRoute>
              }
            >
              <Route index element={<Navigate to="/dashboard" replace />} />
              <Route path="dashboard" element={<Dashboard />} />
              <Route path="users" element={<Users />} />
              <Route path="users/:id" element={<UserProfile />} />
              <Route path="partners" element={<Partners />} />
              <Route path="clients" element={<Clients />} />
              <Route path="deployments" element={<Deployments />} />
              <Route path="servers" element={<Servers />} />
              <Route path="security" element={<Security />} />
              <Route path="reports" element={<Reports />} />
              <Route path="settings" element={<Settings />} />
              <Route path="profile" element={<UserProfile />} />
              <Route path="resources/countries" element={<Countries />} />
              <Route path="resources/datacenters" element={<Datacenters />} />
              <Route path="resources/compute" element={<Compute />} />
              <Route path="resources/storage" element={<Storage />} />
            </Route>
          </Routes>
        </Router>
      </ThemeProvider>
    </Provider>
  );
}

export default App;
