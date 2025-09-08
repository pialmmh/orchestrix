import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Box,
  Alert,
  FormControlLabel,
  Checkbox,
  Switch,
  Tabs,
  Tab,
  Chip,
  Typography,
  Divider
} from '@mui/material';
import {
  Security,
  VpnKey,
  CheckCircle
} from '@mui/icons-material';

interface RemoteAccessDialogProps {
  open: boolean;
  onClose: () => void;
  deviceType: string;
  deviceId: number;
  deviceName: string;
  existingAccess?: any;
  onSave: (data: any) => void;
}

const RemoteAccessDialog: React.FC<RemoteAccessDialogProps> = ({
  open,
  onClose,
  deviceType,
  deviceId,
  deviceName,
  existingAccess,
  onSave
}) => {
  const [activeTab, setActiveTab] = useState(0);
  
  const [formData, setFormData] = useState({
    accessName: '',
    accessType: 'SSH',
    accessProtocol: 'SSH',
    host: '',
    port: 22,
    authMethod: 'PASSWORD',
    username: '',
    bitwardenSyncEnabled: true,
    bitwardenItemId: '',
    sudoEnabled: false,
    sudoMethod: 'sudo',
    requiresMfa: false,
    isActive: true,
    isPrimary: false,
    notes: ''
  });

  useEffect(() => {
    if (existingAccess) {
      setFormData(existingAccess);
    }
  }, [existingAccess]);

  const handleChange = (field: string, value: any) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
    
    // Auto-adjust port based on protocol
    if (field === 'accessProtocol') {
      const defaultPorts: any = {
        SSH: 22,
        RDP: 3389,
        TELNET: 23,
        HTTPS: 443,
        HTTP: 80
      };
      if (defaultPorts[value]) {
        setFormData(prev => ({
          ...prev,
          port: defaultPorts[value]
        }));
      }
    }
  };

  const handleSubmit = () => {
    const accessData = {
      ...formData,
      deviceType: deviceType.toUpperCase(),
      deviceId,
      deviceName
    };
    onSave(accessData);
    onClose();
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>
        <Typography variant="h6">
          {existingAccess ? 'Edit' : 'Add'} Remote Access - {deviceName}
        </Typography>
      </DialogTitle>
      
      <DialogContent>
        <Tabs value={activeTab} onChange={(e, v) => setActiveTab(v)} sx={{ mb: 2 }}>
          <Tab label="Connection" />
          <Tab label="Authentication" />
          <Tab label="Bitwarden" />
          <Tab label="Advanced" />
        </Tabs>

        {/* Connection Tab */}
        {activeTab === 0 && (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, px: 3, maxWidth: 600, mx: 'auto' }}>
            <TextField
              label="Access Name"
              value={formData.accessName}
              onChange={(e) => handleChange('accessName', e.target.value)}
              fullWidth
              size="small"
              placeholder="e.g., Primary SSH, Management Console"
            />
            
            <Box sx={{ display: 'flex', gap: 2 }}>
              <FormControl fullWidth size="small">
                <InputLabel>Access Type</InputLabel>
                <Select
                  value={formData.accessType}
                  onChange={(e) => handleChange('accessType', e.target.value)}
                  label="Access Type"
                >
                  <MenuItem value="SSH">SSH</MenuItem>
                  <MenuItem value="RDP">RDP</MenuItem>
                  <MenuItem value="TELNET">Telnet</MenuItem>
                  <MenuItem value="HTTPS">HTTPS</MenuItem>
                  <MenuItem value="REST_API">REST API</MenuItem>
                  <MenuItem value="SNMP">SNMP</MenuItem>
                </Select>
              </FormControl>
              
              <FormControl fullWidth size="small">
                <InputLabel>Protocol</InputLabel>
                <Select
                  value={formData.accessProtocol}
                  onChange={(e) => handleChange('accessProtocol', e.target.value)}
                  label="Protocol"
                >
                  <MenuItem value="SSH">SSH</MenuItem>
                  <MenuItem value="SSH2">SSH2</MenuItem>
                  <MenuItem value="RDP">RDP</MenuItem>
                  <MenuItem value="TELNET">Telnet</MenuItem>
                  <MenuItem value="HTTPS">HTTPS</MenuItem>
                  <MenuItem value="HTTP">HTTP</MenuItem>
                </Select>
              </FormControl>
            </Box>
            
            <Box sx={{ display: 'flex', gap: 2 }}>
              <TextField
                label="Host/IP Address"
                value={formData.host}
                onChange={(e) => handleChange('host', e.target.value)}
                fullWidth
                size="small"
                placeholder="192.168.1.100 or server.example.com"
                sx={{ flex: 2 }}
              />
              
              <TextField
                label="Port"
                type="number"
                value={formData.port}
                onChange={(e) => handleChange('port', parseInt(e.target.value))}
                fullWidth
                size="small"
                sx={{ flex: 1 }}
              />
            </Box>
            
            <FormControlLabel
              control={
                <Switch
                  checked={formData.isPrimary}
                  onChange={(e) => handleChange('isPrimary', e.target.checked)}
                />
              }
              label="Set as Primary Access Method"
            />
            
            <FormControlLabel
              control={
                <Switch
                  checked={formData.isActive}
                  onChange={(e) => handleChange('isActive', e.target.checked)}
                />
              }
              label="Active"
            />
          </Box>
        )}

        {/* Authentication Tab */}
        {activeTab === 1 && (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, px: 3, maxWidth: 600, mx: 'auto' }}>
            <FormControl fullWidth size="small">
              <InputLabel>Authentication Method</InputLabel>
              <Select
                value={formData.authMethod}
                onChange={(e) => handleChange('authMethod', e.target.value)}
                label="Authentication Method"
              >
                <MenuItem value="PASSWORD">Password</MenuItem>
                <MenuItem value="SSH_KEY">SSH Key</MenuItem>
                <MenuItem value="SSH_KEY_WITH_PASSPHRASE">SSH Key with Passphrase</MenuItem>
                <MenuItem value="CERTIFICATE">Certificate</MenuItem>
                <MenuItem value="API_KEY">API Key</MenuItem>
                <MenuItem value="BEARER_TOKEN">Bearer Token</MenuItem>
              </Select>
            </FormControl>
            
            <TextField
              label="Username"
              value={formData.username}
              onChange={(e) => handleChange('username', e.target.value)}
              fullWidth
              size="small"
            />
            
            <Divider />
            
            <FormControlLabel
              control={
                <Checkbox
                  checked={formData.sudoEnabled}
                  onChange={(e) => handleChange('sudoEnabled', e.target.checked)}
                />
              }
              label="Enable Sudo/Elevated Privileges"
            />
            
            {formData.sudoEnabled && (
              <FormControl fullWidth size="small">
                <InputLabel>Sudo Method</InputLabel>
                <Select
                  value={formData.sudoMethod}
                  onChange={(e) => handleChange('sudoMethod', e.target.value)}
                  label="Sudo Method"
                >
                  <MenuItem value="sudo">sudo</MenuItem>
                  <MenuItem value="su">su</MenuItem>
                  <MenuItem value="enable">enable (Cisco)</MenuItem>
                  <MenuItem value="runas">runas (Windows)</MenuItem>
                </Select>
              </FormControl>
            )}
            
            <FormControlLabel
              control={
                <Checkbox
                  checked={formData.requiresMfa}
                  onChange={(e) => handleChange('requiresMfa', e.target.checked)}
                />
              }
              label="Requires Multi-Factor Authentication"
            />
          </Box>
        )}

        {/* Bitwarden Tab */}
        {activeTab === 2 && (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, px: 3, maxWidth: 600, mx: 'auto' }}>
            <Alert severity="info">
              Credentials are securely stored in your self-hosted Bitwarden/Vaultwarden instance
            </Alert>
            
            <FormControlLabel
              control={
                <Switch
                  checked={formData.bitwardenSyncEnabled}
                  onChange={(e) => handleChange('bitwardenSyncEnabled', e.target.checked)}
                />
              }
              label="Enable Bitwarden Sync"
            />
            
            {formData.bitwardenSyncEnabled && existingAccess && (
              <TextField
                label="Bitwarden Item ID"
                value={formData.bitwardenItemId}
                fullWidth
                size="small"
                disabled
                helperText="Auto-generated on first save"
              />
            )}
            
            {!existingAccess && formData.bitwardenSyncEnabled && (
              <Alert severity="info">
                Bitwarden item will be created automatically when you save this configuration.
              </Alert>
            )}
          </Box>
        )}

        {/* Advanced Tab */}
        {activeTab === 3 && (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, px: 3, maxWidth: 600, mx: 'auto' }}>
            <TextField
              label="Notes"
              value={formData.notes}
              onChange={(e) => handleChange('notes', e.target.value)}
              fullWidth
              multiline
              rows={4}
              size="small"
              placeholder="Additional notes or instructions"
            />
          </Box>
        )}
      </DialogContent>
      
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button onClick={handleSubmit} variant="contained" startIcon={<Security />}>
          Save Remote Access
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default RemoteAccessDialog;