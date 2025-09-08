import React, { useState, useEffect } from 'react';
import {
  Box, Typography, Paper, Tab, Tabs, Card, CardContent,
  Chip, List, ListItem, ListItemText, ListItemIcon, IconButton,
  Button, Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Select, MenuItem, FormControl, InputLabel,
  Alert, Snackbar, Divider, Tooltip, Switch, FormControlLabel
} from '@mui/material';
import Grid from '@mui/material/Grid';
import {
  Cloud, Apps, DeveloperMode, Analytics, Security, NetworkCheck,
  Psychology, Widgets, Edit, Delete, Add, Info, CheckCircle,
  Save, RestartAlt
} from '@mui/icons-material';
import axios from 'axios';

interface ResourceGroup {
  id: number;
  name: string;
  displayName: string;
  description: string;
  category: string;
  serviceTypes: string[];
  isActive: boolean;
  sortOrder: number;
  icon: string;
  color: string;
}

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;
  return (
    <div hidden={value !== index} {...other}>
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  );
}

const Settings: React.FC = () => {
  const [tabValue, setTabValue] = useState(0);
  const [resourceGroups, setResourceGroups] = useState<ResourceGroup[]>([]);
  const [loading, setLoading] = useState(true);
  const [openDialog, setOpenDialog] = useState(false);
  const [editingGroup, setEditingGroup] = useState<ResourceGroup | null>(null);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' as 'success' | 'error' });

  const iconMap: { [key: string]: React.ElementType } = {
    cloud: Cloud,
    apps: Apps,
    developer_mode: DeveloperMode,
    analytics: Analytics,
    security: Security,
    network_check: NetworkCheck,
    psychology: Psychology,
    widgets: Widgets
  };

  useEffect(() => {
    fetchResourceGroups();
  }, []);

  const fetchResourceGroups = async () => {
    try {
      const response = await axios.get('http://localhost:8090/api/resource-groups');
      setResourceGroups(response.data);
    } catch (error) {
      console.error('Failed to fetch resource groups:', error);
      setSnackbar({ open: true, message: 'Failed to load resource groups', severity: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
  };

  const handleEditGroup = (group: ResourceGroup) => {
    setEditingGroup(group);
    setOpenDialog(true);
  };

  const handleDeleteGroup = async (groupId: number) => {
    if (!window.confirm('Are you sure you want to delete this resource group?')) return;
    
    try {
      await axios.delete(`http://localhost:8090/api/resource-groups/${groupId}`);
      await fetchResourceGroups();
      setSnackbar({ open: true, message: 'Resource group deleted successfully', severity: 'success' });
    } catch (error: any) {
      setSnackbar({ 
        open: true, 
        message: error.response?.data?.error || 'Failed to delete resource group', 
        severity: 'error' 
      });
    }
  };

  const handleSaveGroup = async () => {
    if (!editingGroup) return;
    
    try {
      await axios.put(`http://localhost:8090/api/resource-groups/${editingGroup.id}`, editingGroup);
      await fetchResourceGroups();
      setOpenDialog(false);
      setEditingGroup(null);
      setSnackbar({ open: true, message: 'Resource group updated successfully', severity: 'success' });
    } catch (error) {
      setSnackbar({ open: true, message: 'Failed to update resource group', severity: 'error' });
    }
  };

  const getCategoryColor = (category: string) => {
    switch (category) {
      case 'IaaS': return '#2196F3';
      case 'PaaS': return '#9C27B0';
      case 'SaaS': return '#4CAF50';
      default: return '#757575';
    }
  };

  return (
    <Box sx={{ width: '100%', p: 3 }}>
      <Typography variant="h4" gutterBottom>
        System Settings
      </Typography>

      <Paper sx={{ width: '100%', mb: 2 }}>
        <Tabs value={tabValue} onChange={handleTabChange} aria-label="settings tabs">
          <Tab label="Resource Groups" />
          <Tab label="General" />
          <Tab label="Network" />
          <Tab label="Security" />
          <Tab label="Notifications" />
          <Tab label="Integrations" />
        </Tabs>
      </Paper>

      <TabPanel value={tabValue} index={0}>
        <Box sx={{ mb: 3 }}>
          <Typography variant="h5" gutterBottom>
            Resource Group Templates
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            Predefined service groups that cloud providers offer. Select these when adding resources to datacenters.
          </Typography>
        </Box>

        <Grid container spacing={3}>
          {resourceGroups.map((group) => {
            const IconComponent = iconMap[group.icon] || Cloud;
            return (
              <Grid key={group.id} item xs={12} md={6} lg={4}>
                <Card sx={{ 
                  height: '100%',
                  borderLeft: `4px solid ${group.color}`,
                  '&:hover': { boxShadow: 3 }
                }}>
                  <CardContent>
                    <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                      <IconComponent sx={{ color: group.color, mr: 2, fontSize: 32 }} />
                      <Box sx={{ flexGrow: 1 }}>
                        <Typography variant="h6">{group.displayName}</Typography>
                        <Chip 
                          label={group.category} 
                          size="small" 
                          sx={{ 
                            bgcolor: getCategoryColor(group.category),
                            color: 'white',
                            mt: 0.5
                          }} 
                        />
                      </Box>
                      <Box>
                        <Tooltip title="Edit">
                          <IconButton size="small" onClick={() => handleEditGroup(group)}>
                            <Edit fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Delete">
                          <IconButton size="small" onClick={() => handleDeleteGroup(group.id)}>
                            <Delete fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      </Box>
                    </Box>
                    
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                      {group.description}
                    </Typography>
                    
                    <Divider sx={{ my: 1.5 }} />
                    
                    <Typography variant="subtitle2" gutterBottom>
                      Services Included:
                    </Typography>
                    <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                      {group.serviceTypes.map((service, idx) => (
                        <Chip
                          key={idx}
                          label={service}
                          size="small"
                          variant="outlined"
                          sx={{ fontSize: '0.75rem' }}
                        />
                      ))}
                    </Box>
                    
                    {group.isActive && (
                      <Box sx={{ mt: 2, display: 'flex', alignItems: 'center' }}>
                        <CheckCircle sx={{ color: 'success.main', fontSize: 16, mr: 0.5 }} />
                        <Typography variant="caption" color="success.main">
                          Active
                        </Typography>
                      </Box>
                    )}
                  </CardContent>
                </Card>
              </Grid>
            );
          })}
        </Grid>

        <Box sx={{ mt: 3, p: 2, bgcolor: 'background.paper', borderRadius: 1 }}>
          <Alert severity="info">
            <Typography variant="subtitle2" gutterBottom>
              How Resource Groups Work:
            </Typography>
            <Typography variant="body2">
              1. Resource groups are templates for cloud service bundles<br />
              2. When you add a resource group to a datacenter, it creates the service hierarchy<br />
              3. For example, selecting "Cloud Computing" adds Compute, Storage, and Networking services<br />
              4. You can then add specific resources (VMs, servers) under each service type
            </Typography>
          </Alert>
        </Box>
      </TabPanel>

      <TabPanel value={tabValue} index={1}>
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

      <TabPanel value={tabValue} index={2}>
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

      <TabPanel value={tabValue} index={3}>
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

      <TabPanel value={tabValue} index={4}>
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

      <TabPanel value={tabValue} index={5}>
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

      {/* Edit Dialog */}
      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="md" fullWidth>
        <DialogTitle>Edit Resource Group</DialogTitle>
        <DialogContent>
          {editingGroup && (
            <Box sx={{ pt: 2 }}>
              <TextField
                fullWidth
                label="Display Name"
                value={editingGroup.displayName}
                onChange={(e) => setEditingGroup({ ...editingGroup, displayName: e.target.value })}
                sx={{ mb: 2 }}
              />
              <TextField
                fullWidth
                multiline
                rows={3}
                label="Description"
                value={editingGroup.description}
                onChange={(e) => setEditingGroup({ ...editingGroup, description: e.target.value })}
                sx={{ mb: 2 }}
              />
              <FormControl fullWidth sx={{ mb: 2 }}>
                <InputLabel>Category</InputLabel>
                <Select
                  value={editingGroup.category}
                  onChange={(e) => setEditingGroup({ ...editingGroup, category: e.target.value })}
                  label="Category"
                >
                  <MenuItem value="IaaS">Infrastructure as a Service (IaaS)</MenuItem>
                  <MenuItem value="PaaS">Platform as a Service (PaaS)</MenuItem>
                  <MenuItem value="SaaS">Software as a Service (SaaS)</MenuItem>
                </Select>
              </FormControl>
              <FormControl fullWidth sx={{ mb: 2 }}>
                <InputLabel>Status</InputLabel>
                <Select
                  value={editingGroup.isActive ? 'active' : 'inactive'}
                  onChange={(e) => setEditingGroup({ ...editingGroup, isActive: e.target.value === 'active' })}
                  label="Status"
                >
                  <MenuItem value="active">Active</MenuItem>
                  <MenuItem value="inactive">Inactive</MenuItem>
                </Select>
              </FormControl>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button onClick={handleSaveGroup} variant="contained">Save</Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={snackbar.open}
        autoHideDuration={6000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
      >
        <Alert severity={snackbar.severity} onClose={() => setSnackbar({ ...snackbar, open: false })}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default Settings;