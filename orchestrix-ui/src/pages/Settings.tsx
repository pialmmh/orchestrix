import React, { useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  TextField,
  Button,
  Switch,
  FormControlLabel,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Divider,
  Tab,
  Tabs,
} from '@mui/material';
import { Save, RestartAlt } from '@mui/icons-material';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;
  return (
    <div hidden={value !== index} {...other}>
      {value === index && <Box sx={{ py: 3 }}>{children}</Box>}
    </div>
  );
}

const Settings: React.FC = () => {
  const [tabValue, setTabValue] = useState(0);

  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
  };

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        System Settings
      </Typography>

      <Card>
        <CardContent>
          <Tabs value={tabValue} onChange={handleTabChange}>
            <Tab label="General" />
            <Tab label="Network" />
            <Tab label="Security" />
            <Tab label="Notifications" />
            <Tab label="Integrations" />
          </Tabs>

          <TabPanel value={tabValue} index={0}>
            <Typography variant="h6" gutterBottom>General Settings</Typography>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, maxWidth: 600 }}>
              <TextField fullWidth label="System Name" defaultValue="Orchestrix" />
              <TextField fullWidth label="Organization" defaultValue="TelcoBright" />
              <FormControl fullWidth>
                <InputLabel>Timezone</InputLabel>
                <Select defaultValue="UTC" label="Timezone">
                  <MenuItem value="UTC">UTC</MenuItem>
                  <MenuItem value="EST">Eastern Time</MenuItem>
                  <MenuItem value="PST">Pacific Time</MenuItem>
                </Select>
              </FormControl>
              <FormControlLabel
                control={<Switch defaultChecked />}
                label="Enable automatic updates"
              />
              <FormControlLabel
                control={<Switch defaultChecked />}
                label="Send usage statistics"
              />
            </Box>
          </TabPanel>

          <TabPanel value={tabValue} index={1}>
            <Typography variant="h6" gutterBottom>Network Settings</Typography>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, maxWidth: 600 }}>
              <TextField fullWidth label="API Base URL" defaultValue="http://localhost:8090" />
              <TextField fullWidth label="WebSocket URL" defaultValue="ws://localhost:8090/ws" />
              <TextField fullWidth label="Proxy Server" placeholder="http://proxy.example.com:8080" />
              <TextField fullWidth label="DNS Servers" defaultValue="8.8.8.8, 8.8.4.4" />
              <FormControlLabel
                control={<Switch />}
                label="Use SSL/TLS"
              />
            </Box>
          </TabPanel>

          <TabPanel value={tabValue} index={2}>
            <Typography variant="h6" gutterBottom>Security Settings</Typography>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, maxWidth: 600 }}>
              <FormControlLabel
                control={<Switch defaultChecked />}
                label="Enforce two-factor authentication"
              />
              <FormControlLabel
                control={<Switch defaultChecked />}
                label="Enable password complexity requirements"
              />
              <TextField
                fullWidth
                type="number"
                label="Session timeout (minutes)"
                defaultValue="30"
              />
              <TextField
                fullWidth
                type="number"
                label="Max login attempts"
                defaultValue="5"
              />
              <FormControlLabel
                control={<Switch defaultChecked />}
                label="Enable audit logging"
              />
            </Box>
          </TabPanel>

          <TabPanel value={tabValue} index={3}>
            <Typography variant="h6" gutterBottom>Notification Settings</Typography>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, maxWidth: 600 }}>
              <TextField fullWidth label="SMTP Server" defaultValue="smtp.gmail.com" />
              <TextField fullWidth label="SMTP Port" defaultValue="587" />
              <TextField fullWidth label="From Email" defaultValue="noreply@orchestrix.com" />
              <FormControlLabel
                control={<Switch defaultChecked />}
                label="Enable email notifications"
              />
              <FormControlLabel
                control={<Switch />}
                label="Enable SMS notifications"
              />
              <FormControlLabel
                control={<Switch defaultChecked />}
                label="Enable in-app notifications"
              />
            </Box>
          </TabPanel>

          <TabPanel value={tabValue} index={4}>
            <Typography variant="h6" gutterBottom>Integration Settings</Typography>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, maxWidth: 600 }}>
              <TextField fullWidth label="Slack Webhook URL" placeholder="https://hooks.slack.com/..." />
              <TextField fullWidth label="Teams Webhook URL" placeholder="https://outlook.office.com/..." />
              <TextField fullWidth label="Jira URL" placeholder="https://company.atlassian.net" />
              <TextField fullWidth label="GitHub Token" type="password" placeholder="ghp_..." />
              <FormControlLabel
                control={<Switch />}
                label="Enable Slack integration"
              />
              <FormControlLabel
                control={<Switch />}
                label="Enable Teams integration"
              />
            </Box>
          </TabPanel>

          <Divider sx={{ my: 3 }} />
          
          <Box sx={{ display: 'flex', gap: 2 }}>
            <Button variant="contained" startIcon={<Save />}>
              Save Settings
            </Button>
            <Button variant="outlined" startIcon={<RestartAlt />}>
              Reset to Defaults
            </Button>
          </Box>
        </CardContent>
      </Card>
    </Box>
  );
};

export default Settings;