import React from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Alert,
  AlertTitle,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Chip,
  Button,
} from '@mui/material';
import {
  Warning,
  CheckCircle,
  Error,
  Shield,
  VpnKey,
  Lock,
  Security as SecurityIcon,
} from '@mui/icons-material';

const Security: React.FC = () => {
  const securityAlerts = [
    { id: 1, severity: 'high', message: '3 failed login attempts from IP 192.168.1.100', time: '5 minutes ago' },
    { id: 2, severity: 'medium', message: 'SSL certificate expires in 30 days', time: '1 hour ago' },
    { id: 3, severity: 'low', message: 'New device login: iPhone 12 Pro', time: '2 hours ago' },
  ];

  const securityChecks = [
    { name: 'Two-Factor Authentication', status: 'enabled', icon: <VpnKey /> },
    { name: 'SSL/TLS Encryption', status: 'enabled', icon: <Lock /> },
    { name: 'Firewall Protection', status: 'enabled', icon: <Shield /> },
    { name: 'Intrusion Detection', status: 'warning', icon: <SecurityIcon /> },
  ];

  const getSeverityColor = (severity: string) => {
    switch (severity) {
      case 'high': return 'error';
      case 'medium': return 'warning';
      case 'low': return 'info';
      default: return 'default';
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'enabled': return <CheckCircle color="success" />;
      case 'warning': return <Warning color="warning" />;
      case 'disabled': return <Error color="error" />;
      default: return null;
    }
  };

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Security Dashboard
      </Typography>

      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 3 }}>
        <Box sx={{ flex: { xs: '0 0 100%', lg: '0 0 calc(60% - 12px)' } }}>
          <Card sx={{ mb: 3 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Security Alerts
              </Typography>
              {securityAlerts.map((alert) => (
                <Alert
                  key={alert.id}
                  severity={getSeverityColor(alert.severity) as any}
                  sx={{ mb: 2 }}
                >
                  <AlertTitle>{alert.message}</AlertTitle>
                  {alert.time}
                </Alert>
              ))}
              <Button fullWidth variant="outlined">
                View All Alerts
              </Button>
            </CardContent>
          </Card>

          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Recent Activity
              </Typography>
              <List>
                <ListItem>
                  <ListItemText
                    primary="Admin user password changed"
                    secondary="Yesterday at 3:45 PM"
                  />
                </ListItem>
                <ListItem>
                  <ListItemText
                    primary="New API key generated"
                    secondary="2 days ago"
                  />
                </ListItem>
                <ListItem>
                  <ListItemText
                    primary="Security audit completed"
                    secondary="5 days ago"
                  />
                </ListItem>
              </List>
            </CardContent>
          </Card>
        </Box>

        <Box sx={{ flex: { xs: '0 0 100%', lg: '0 0 calc(40% - 12px)' } }}>
          <Card sx={{ mb: 3 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Security Status
              </Typography>
              <List>
                {securityChecks.map((check) => (
                  <ListItem key={check.name}>
                    <ListItemIcon>{check.icon}</ListItemIcon>
                    <ListItemText primary={check.name} />
                    {getStatusIcon(check.status)}
                  </ListItem>
                ))}
              </List>
            </CardContent>
          </Card>

          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Security Score
              </Typography>
              <Box sx={{ textAlign: 'center', py: 3 }}>
                <Typography variant="h2" color="success.main">
                  85/100
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Good Security Posture
                </Typography>
              </Box>
              <Button fullWidth variant="contained">
                Run Security Scan
              </Button>
            </CardContent>
          </Card>
        </Box>
      </Box>
    </Box>
  );
};

export default Security;