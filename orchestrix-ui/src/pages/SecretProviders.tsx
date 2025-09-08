import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Paper,
  Card,
  CardContent,
  Button,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Alert,
  IconButton,
  Chip,
  Switch,
  FormControlLabel,
  Divider,
  Tabs,
  Tab,
  InputAdornment,
  Tooltip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions
} from '@mui/material';
import Grid from '@mui/material/Grid';
import {
  VpnKey,
  Security,
  CheckCircle,
  Error,
  Settings,
  Add,
  Edit,
  Delete,
  Visibility,
  VisibilityOff,
  Refresh,
  CloudUpload,
  CloudDownload,
  Link,
  LinkOff
} from '@mui/icons-material';
import axios from 'axios';

interface SecretProviderConfig {
  id?: number;
  providerType: string;
  name: string;
  enabled: boolean;
  apiUrl?: string;
  identityUrl?: string;
  clientId?: string;
  clientSecret?: string;
  organizationId?: string;
  namespace?: string;
  region?: string;
  vaultPath?: string;
  isDefault?: boolean;
  lastSync?: string;
  status?: 'connected' | 'disconnected' | 'error';
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

const SecretProviders: React.FC = () => {
  const [activeTab, setActiveTab] = useState(0);
  const [providers, setProviders] = useState<SecretProviderConfig[]>([]);
  const [selectedProvider, setSelectedProvider] = useState<SecretProviderConfig | null>(null);
  const [showSecrets, setShowSecrets] = useState<{ [key: string]: boolean }>({});
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [testResults, setTestResults] = useState<{ [key: string]: boolean }>({});
  const [loading, setLoading] = useState(false);

  const providerTypes = [
    { value: 'BITWARDEN', label: 'Bitwarden/Vaultwarden', icon: <VpnKey /> },
    { value: 'HASHICORP_VAULT', label: 'HashiCorp Vault', icon: <Security /> },
    { value: 'AWS_SECRETS_MANAGER', label: 'AWS Secrets Manager', icon: <CloudUpload /> },
    { value: 'AZURE_KEY_VAULT', label: 'Azure Key Vault', icon: <CloudDownload /> },
    { value: 'GCP_SECRET_MANAGER', label: 'Google Cloud Secret Manager', icon: <CloudUpload /> },
    { value: 'LOCAL_ENCRYPTED', label: 'Local Encrypted Storage', icon: <Security /> }
  ];

  useEffect(() => {
    fetchProviders();
  }, []);

  const fetchProviders = async () => {
    setLoading(true);
    try {
      // For now, use mock data since the backend endpoint doesn't exist yet
      const mockProviders: SecretProviderConfig[] = [
        {
          id: 1,
          providerType: 'BITWARDEN',
          name: 'Primary Bitwarden',
          enabled: true,
          apiUrl: 'http://localhost:8080',
          identityUrl: 'http://localhost:8080/identity',
          clientId: 'orchestrix-client',
          organizationId: 'org-123',
          isDefault: true,
          lastSync: new Date().toISOString(),
          status: 'connected'
        },
        {
          id: 2,
          providerType: 'LOCAL_ENCRYPTED',
          name: 'Local Storage',
          enabled: true,
          isDefault: false,
          status: 'connected'
        }
      ];
      setProviders(mockProviders);
    } catch (error) {
      console.error('Failed to fetch providers:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleTestConnection = async (provider: SecretProviderConfig) => {
    try {
      const response = await axios.post('/api/secret-providers/test', {
        providerType: provider.providerType,
        config: provider
      });
      setTestResults({ ...testResults, [provider.id!]: response.data.success });
    } catch (error) {
      setTestResults({ ...testResults, [provider.id!]: false });
    }
  };

  const handleSaveProvider = async () => {
    if (!selectedProvider) return;
    
    try {
      if (selectedProvider.id) {
        await axios.put(`/api/secret-providers/${selectedProvider.id}`, selectedProvider);
      } else {
        await axios.post('/api/secret-providers', selectedProvider);
      }
      setEditDialogOpen(false);
      fetchProviders();
    } catch (error) {
      console.error('Failed to save provider:', error);
    }
  };

  const handleDeleteProvider = async (id: number) => {
    if (!window.confirm('Are you sure you want to delete this provider?')) return;
    
    try {
      await axios.delete(`/api/secret-providers/${id}`);
      fetchProviders();
    } catch (error) {
      console.error('Failed to delete provider:', error);
    }
  };

  const toggleShowSecret = (key: string) => {
    setShowSecrets({ ...showSecrets, [key]: !showSecrets[key] });
  };

  const getProviderIcon = (type: string) => {
    const provider = providerTypes.find(p => p.value === type);
    return provider?.icon || <Security />;
  };

  const getStatusColor = (status?: string) => {
    switch (status) {
      case 'connected': return 'success';
      case 'disconnected': return 'warning';
      case 'error': return 'error';
      default: return 'default';
    }
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">
          Secret Provider Configuration
        </Typography>
        <Button
          variant="contained"
          startIcon={<Add />}
          onClick={() => {
            setSelectedProvider({
              providerType: 'BITWARDEN',
              name: '',
              enabled: true
            });
            setEditDialogOpen(true);
          }}
        >
          Add Provider
        </Button>
      </Box>

      <Alert severity="info" sx={{ mb: 3 }}>
        Configure your secret providers to securely store and manage credentials for devices.
        When you add credentials to a device and select a provider, the credentials are automatically
        synchronized with that provider.
      </Alert>

      <Paper sx={{ mb: 3 }}>
        <Tabs value={activeTab} onChange={(e, v) => setActiveTab(v)}>
          <Tab label="Active Providers" />
          <Tab label="Configuration Guide" />
          <Tab label="Sync Status" />
        </Tabs>

        <TabPanel value={activeTab} index={0}>
          <Grid container spacing={3}>
            {providers.map((provider) => (
              <Grid item xs={12} md={6} key={provider.id}>
                <Card sx={{ height: '100%' }}>
                  <CardContent>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        {getProviderIcon(provider.providerType)}
                        <Typography variant="h6">
                          {provider.name}
                        </Typography>
                        {provider.isDefault && (
                          <Chip label="Default" size="small" color="primary" />
                        )}
                      </Box>
                      <Box>
                        <IconButton
                          size="small"
                          onClick={() => {
                            setSelectedProvider(provider);
                            setEditDialogOpen(true);
                          }}
                        >
                          <Edit />
                        </IconButton>
                        <IconButton
                          size="small"
                          onClick={() => handleDeleteProvider(provider.id!)}
                        >
                          <Delete />
                        </IconButton>
                      </Box>
                    </Box>

                    <Box sx={{ mb: 2 }}>
                      <Chip
                        label={provider.status || 'unknown'}
                        color={getStatusColor(provider.status) as any}
                        size="small"
                        icon={provider.status === 'connected' ? <CheckCircle /> : <Error />}
                      />
                    </Box>

                    <Typography variant="body2" color="text.secondary" gutterBottom>
                      Type: {providerTypes.find(p => p.value === provider.providerType)?.label}
                    </Typography>

                    {provider.apiUrl && (
                      <Typography variant="body2" color="text.secondary" gutterBottom>
                        API URL: {provider.apiUrl}
                      </Typography>
                    )}

                    {provider.lastSync && (
                      <Typography variant="body2" color="text.secondary" gutterBottom>
                        Last Sync: {new Date(provider.lastSync).toLocaleString()}
                      </Typography>
                    )}

                    <Box sx={{ mt: 2, display: 'flex', gap: 1 }}>
                      <Button
                        size="small"
                        variant="outlined"
                        onClick={() => handleTestConnection(provider)}
                      >
                        Test Connection
                      </Button>
                      <Button
                        size="small"
                        variant="outlined"
                        startIcon={<Refresh />}
                      >
                        Sync Now
                      </Button>
                    </Box>

                    {testResults[provider.id!] !== undefined && (
                      <Alert
                        severity={testResults[provider.id!] ? 'success' : 'error'}
                        sx={{ mt: 2 }}
                      >
                        {testResults[provider.id!] ? 'Connection successful' : 'Connection failed'}
                      </Alert>
                    )}
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        </TabPanel>

        <TabPanel value={activeTab} index={1}>
          <Typography variant="h6" gutterBottom>
            Configuration Guide
          </Typography>
          
          <Box sx={{ mb: 3 }}>
            <Typography variant="subtitle1" gutterBottom>
              Bitwarden/Vaultwarden Setup
            </Typography>
            <Typography variant="body2" color="text.secondary" paragraph>
              1. Install Vaultwarden using Docker: docker run -d --name vaultwarden -p 8080:80 vaultwarden/server
            </Typography>
            <Typography variant="body2" color="text.secondary" paragraph>
              2. Create an organization in Bitwarden/Vaultwarden
            </Typography>
            <Typography variant="body2" color="text.secondary" paragraph>
              3. Generate API credentials from Admin Panel → Settings → API Key
            </Typography>
            <Typography variant="body2" color="text.secondary" paragraph>
              4. Add the API URL and credentials in the provider configuration
            </Typography>
          </Box>

          <Divider sx={{ my: 2 }} />

          <Box sx={{ mb: 3 }}>
            <Typography variant="subtitle1" gutterBottom>
              HashiCorp Vault Setup
            </Typography>
            <Typography variant="body2" color="text.secondary" paragraph>
              1. Install Vault: vault server -dev
            </Typography>
            <Typography variant="body2" color="text.secondary" paragraph>
              2. Enable KV secrets engine: vault secrets enable -path=orchestrix kv-v2
            </Typography>
            <Typography variant="body2" color="text.secondary" paragraph>
              3. Create an access token with appropriate policies
            </Typography>
            <Typography variant="body2" color="text.secondary" paragraph>
              4. Configure the Vault URL and token in the provider settings
            </Typography>
          </Box>
        </TabPanel>

        <TabPanel value={activeTab} index={2}>
          <Typography variant="h6" gutterBottom>
            Synchronization Status
          </Typography>
          
          <Alert severity="success" sx={{ mb: 2 }}>
            All credentials are automatically synchronized when created or updated.
          </Alert>

          <Box>
            <Typography variant="body2" color="text.secondary">
              • Credentials are pushed to the selected provider when saved
            </Typography>
            <Typography variant="body2" color="text.secondary">
              • Updates are synchronized in real-time
            </Typography>
            <Typography variant="body2" color="text.secondary">
              • Deletions remove credentials from both Orchestrix and the provider
            </Typography>
            <Typography variant="body2" color="text.secondary">
              • Failed syncs are logged and can be retried manually
            </Typography>
          </Box>
        </TabPanel>
      </Paper>

      {/* Edit Provider Dialog */}
      <Dialog open={editDialogOpen} onClose={() => setEditDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>
          {selectedProvider?.id ? 'Edit' : 'Add'} Secret Provider
        </DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
            <FormControl fullWidth>
              <InputLabel>Provider Type</InputLabel>
              <Select
                value={selectedProvider?.providerType || ''}
                onChange={(e) => setSelectedProvider({
                  ...selectedProvider!,
                  providerType: e.target.value
                })}
                label="Provider Type"
              >
                {providerTypes.map((type) => (
                  <MenuItem key={type.value} value={type.value}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      {type.icon}
                      {type.label}
                    </Box>
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <TextField
              label="Name"
              value={selectedProvider?.name || ''}
              onChange={(e) => setSelectedProvider({
                ...selectedProvider!,
                name: e.target.value
              })}
              fullWidth
              required
            />

            {selectedProvider?.providerType === 'BITWARDEN' && (
              <>
                <TextField
                  label="API URL"
                  value={selectedProvider?.apiUrl || ''}
                  onChange={(e) => setSelectedProvider({
                    ...selectedProvider!,
                    apiUrl: e.target.value
                  })}
                  fullWidth
                  placeholder="http://localhost:8080"
                />
                <TextField
                  label="Identity URL"
                  value={selectedProvider?.identityUrl || ''}
                  onChange={(e) => setSelectedProvider({
                    ...selectedProvider!,
                    identityUrl: e.target.value
                  })}
                  fullWidth
                  placeholder="http://localhost:8080/identity"
                />
                <TextField
                  label="Client ID"
                  value={selectedProvider?.clientId || ''}
                  onChange={(e) => setSelectedProvider({
                    ...selectedProvider!,
                    clientId: e.target.value
                  })}
                  fullWidth
                />
                <TextField
                  label="Client Secret"
                  type={showSecrets['clientSecret'] ? 'text' : 'password'}
                  value={selectedProvider?.clientSecret || ''}
                  onChange={(e) => setSelectedProvider({
                    ...selectedProvider!,
                    clientSecret: e.target.value
                  })}
                  fullWidth
                  InputProps={{
                    endAdornment: (
                      <InputAdornment position="end">
                        <IconButton
                          onClick={() => toggleShowSecret('clientSecret')}
                          edge="end"
                        >
                          {showSecrets['clientSecret'] ? <VisibilityOff /> : <Visibility />}
                        </IconButton>
                      </InputAdornment>
                    )
                  }}
                />
                <TextField
                  label="Organization ID"
                  value={selectedProvider?.organizationId || ''}
                  onChange={(e) => setSelectedProvider({
                    ...selectedProvider!,
                    organizationId: e.target.value
                  })}
                  fullWidth
                />
              </>
            )}

            {selectedProvider?.providerType === 'HASHICORP_VAULT' && (
              <>
                <TextField
                  label="Vault URL"
                  value={selectedProvider?.apiUrl || ''}
                  onChange={(e) => setSelectedProvider({
                    ...selectedProvider!,
                    apiUrl: e.target.value
                  })}
                  fullWidth
                  placeholder="http://localhost:8200"
                />
                <TextField
                  label="Token"
                  type={showSecrets['token'] ? 'text' : 'password'}
                  value={selectedProvider?.clientSecret || ''}
                  onChange={(e) => setSelectedProvider({
                    ...selectedProvider!,
                    clientSecret: e.target.value
                  })}
                  fullWidth
                  InputProps={{
                    endAdornment: (
                      <InputAdornment position="end">
                        <IconButton
                          onClick={() => toggleShowSecret('token')}
                          edge="end"
                        >
                          {showSecrets['token'] ? <VisibilityOff /> : <Visibility />}
                        </IconButton>
                      </InputAdornment>
                    )
                  }}
                />
                <TextField
                  label="Vault Path"
                  value={selectedProvider?.vaultPath || ''}
                  onChange={(e) => setSelectedProvider({
                    ...selectedProvider!,
                    vaultPath: e.target.value
                  })}
                  fullWidth
                  placeholder="orchestrix/data"
                />
              </>
            )}

            {selectedProvider?.providerType === 'AWS_SECRETS_MANAGER' && (
              <>
                <TextField
                  label="AWS Region"
                  value={selectedProvider?.region || ''}
                  onChange={(e) => setSelectedProvider({
                    ...selectedProvider!,
                    region: e.target.value
                  })}
                  fullWidth
                  placeholder="us-east-1"
                />
                <TextField
                  label="Access Key ID"
                  value={selectedProvider?.clientId || ''}
                  onChange={(e) => setSelectedProvider({
                    ...selectedProvider!,
                    clientId: e.target.value
                  })}
                  fullWidth
                />
                <TextField
                  label="Secret Access Key"
                  type={showSecrets['awsSecret'] ? 'text' : 'password'}
                  value={selectedProvider?.clientSecret || ''}
                  onChange={(e) => setSelectedProvider({
                    ...selectedProvider!,
                    clientSecret: e.target.value
                  })}
                  fullWidth
                  InputProps={{
                    endAdornment: (
                      <InputAdornment position="end">
                        <IconButton
                          onClick={() => toggleShowSecret('awsSecret')}
                          edge="end"
                        >
                          {showSecrets['awsSecret'] ? <VisibilityOff /> : <Visibility />}
                        </IconButton>
                      </InputAdornment>
                    )
                  }}
                />
              </>
            )}

            <FormControlLabel
              control={
                <Switch
                  checked={selectedProvider?.enabled || false}
                  onChange={(e) => setSelectedProvider({
                    ...selectedProvider!,
                    enabled: e.target.checked
                  })}
                />
              }
              label="Enabled"
            />

            <FormControlLabel
              control={
                <Switch
                  checked={selectedProvider?.isDefault || false}
                  onChange={(e) => setSelectedProvider({
                    ...selectedProvider!,
                    isDefault: e.target.checked
                  })}
                />
              }
              label="Set as Default Provider"
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleSaveProvider} variant="contained">
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default SecretProviders;