import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  IconButton,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Tooltip,
  Stack,
  Tabs,
  Tab,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction,
  Divider,
  Alert,
} from '@mui/material';
import {
  Add,
  Edit,
  Delete,
  Computer,
  Memory,
  Storage,
  Key,
  VpnKey,
  Lock,
  Terminal,
  Api,
  Apps,
  CheckCircle,
  Error,
  Warning,
  ContentCopy,
  Visibility,
  VisibilityOff,
} from '@mui/icons-material';
import axios from 'axios';

interface ComputeResource {
  id: number;
  name: string;
  hostname: string;
  ipAddress?: string;
  status: string;
  environment: string;
  nodeType?: string;
  purpose?: string;
  cpuCores?: number;
  memoryGb?: number;
  storageGb?: number;
  osVersionId?: number;
  osVersionDisplay?: string;
  datacenterId?: number;
  datacenterName?: string;
  partnerId?: number;
  partnerName?: string;
  tags?: string[];
  notes?: string;
}

interface OSVersion {
  id: number;
  osType: string;
  distribution: string;
  version: string;
  displayName: string;
}

interface AccessCredential {
  id: number;
  accessType: string;
  accessName?: string;
  host?: string;
  port?: number;
  protocol?: string;
  authType: string;
  username?: string;
  isActive: boolean;
  isPrimary: boolean;
  lastTestedAt?: string;
  lastTestStatus?: string;
  lastTestMessage?: string;
  hasSshKey?: boolean;
  hasAuthParams?: boolean;
}

const Compute: React.FC = () => {
  const [resources, setResources] = useState<ComputeResource[]>([]);
  const [selectedResource, setSelectedResource] = useState<ComputeResource | null>(null);
  const [credentials, setCredentials] = useState<AccessCredential[]>([]);
  const [osVersions, setOSVersions] = useState<OSVersion[]>([]);
  const [loading, setLoading] = useState(true);
  const [openDialog, setOpenDialog] = useState(false);
  const [openCredentialDialog, setOpenCredentialDialog] = useState(false);
  const [tabValue, setTabValue] = useState(0);
  const [showPassword, setShowPassword] = useState(false);
  
  const [formData, setFormData] = useState({
    name: '',
    hostname: '',
    ipAddress: '',
    status: 'ACTIVE',
    environment: 'PRODUCTION',
    nodeType: 'dedicated_server',
    purpose: '',
    cpuCores: 0,
    memoryGb: 0,
    storageGb: 0,
    osVersionId: '',
    tags: '',
    notes: '',
  });
  
  const [credentialFormData, setCredentialFormData] = useState({
    accessType: 'SSH',
    accessName: '',
    host: '',
    port: 22,
    protocol: 'SSH',
    authType: 'SSH_KEY',
    username: '',
    passwordEncrypted: '',
    sshPrivateKey: '',
    sshPublicKey: '',
    apiKey: '',
    isActive: true,
    isPrimary: false,
  });

  useEffect(() => {
    fetchResources();
    fetchOSVersions();
  }, []);

  const fetchOSVersions = async () => {
    try {
      const response = await axios.get('/api/os-versions');
      setOSVersions(response.data || []);
    } catch (error) {
      console.error('Error fetching OS versions:', error);
    }
  };

  const fetchResources = async () => {
    try {
      const response = await axios.get('/api/api/computes');
      setResources(response.data || []);
    } catch (error) {
      console.error('Error fetching compute resources:', error);
    } finally {
      setLoading(false);
    }
  };

  const fetchCredentials = async (resourceId: number) => {
    try {
      const response = await axios.get(`/api/compute/resources/${resourceId}/credentials`);
      setCredentials(response.data.credentials || []);
    } catch (error) {
      console.error('Error fetching credentials:', error);
    }
  };

  const handleCreateResource = () => {
    setFormData({
      name: '',
      hostname: '',
      ipAddress: '',
      status: 'ACTIVE',
      environment: 'PRODUCTION',
      nodeType: 'dedicated_server',
      purpose: '',
      cpuCores: 0,
      memoryGb: 0,
      storageGb: 0,
      osVersionId: '',
      tags: '',
      notes: '',
    });
    setSelectedResource(null);
    setOpenDialog(true);
  };

  const handleEditResource = (resource: ComputeResource) => {
    setFormData({
      name: resource.name,
      hostname: resource.hostname,
      ipAddress: resource.ipAddress || '',
      status: resource.status,
      environment: resource.environment,
      nodeType: resource.nodeType || 'dedicated_server',
      purpose: resource.purpose || '',
      cpuCores: resource.cpuCores || 0,
      memoryGb: resource.memoryGb || 0,
      storageGb: resource.storageGb || 0,
      osVersionId: resource.osVersionId?.toString() || '',
      tags: resource.tags?.join(', ') || '',
      notes: resource.notes || '',
    });
    setSelectedResource(resource);
    setOpenDialog(true);
  };

  const handleViewCredentials = (resource: ComputeResource) => {
    setSelectedResource(resource);
    fetchCredentials(resource.id);
    setTabValue(1);
  };

  const handleDeleteResource = async (id: number) => {
    try {
      await axios.delete(`/api/api/computes/${id}`);
      fetchResources();
    } catch (error) {
      console.error('Error deleting resource:', error);
    }
  };

  const handleSubmit = async () => {
    try {
      const payload: any = {
        name: formData.name,
        hostname: formData.hostname,
        ipAddress: formData.ipAddress || null,
        status: formData.status,
        cpuCores: parseInt(formData.cpuCores.toString()) || 0,
        memoryGb: parseInt(formData.memoryGb.toString()) || 0,
        diskGb: parseInt(formData.storageGb.toString()) || 0,
        osVersion: formData.osVersionId ? { id: parseInt(formData.osVersionId) } : null,
        tags: formData.tags || null,
        notes: formData.notes || null,
        environment: formData.environment || null,
        nodeType: formData.nodeType || null,
        purpose: formData.purpose || null,
      };
      
      console.log('Sending payload:', JSON.stringify(payload, null, 2));

      if (selectedResource) {
        await axios.put(`/api/api/computes/${selectedResource.id}`, payload);
      } else {
        const response = await axios.post('/api/api/computes', payload);
        console.log('Response:', response.data);
      }
      
      setOpenDialog(false);
      fetchResources();
    } catch (error: any) {
      console.error('Error saving resource:', error);
      console.error('Error response:', error.response?.data);
      alert(`Failed to save compute: ${error.response?.data?.error || error.response?.data?.errors?.join(', ') || error.response?.data?.message || error.message}`);
    }
  };

  const handleAddCredential = () => {
    setCredentialFormData({
      accessType: 'SSH',
      accessName: '',
      host: selectedResource?.hostname || '',
      port: 22,
      protocol: 'SSH',
      authType: 'SSH_KEY',
      username: '',
      passwordEncrypted: '',
      sshPrivateKey: '',
      sshPublicKey: '',
      apiKey: '',
      isActive: true,
      isPrimary: false,
    });
    setOpenCredentialDialog(true);
  };

  const handleSubmitCredential = async () => {
    if (!selectedResource) return;
    
    try {
      await axios.post(`/api/compute/resources/${selectedResource.id}/credentials`, credentialFormData);
      setOpenCredentialDialog(false);
      fetchCredentials(selectedResource.id);
    } catch (error) {
      console.error('Error saving credential:', error);
    }
  };

  const handleTestCredential = async (credentialId: number) => {
    try {
      const response = await axios.post(`/api/compute/credentials/${credentialId}/test`);
      if (selectedResource) {
        fetchCredentials(selectedResource.id);
      }
    } catch (error) {
      console.error('Error testing credential:', error);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE': return 'success';
      case 'SUSPENDED': return 'warning';
      case 'DISCONTINUED': return 'error';
      case 'MAINTENANCE': return 'info';
      default: return 'default';
    }
  };

  const getEnvironmentColor = (env: string) => {
    switch (env) {
      case 'PRODUCTION': return 'error';
      case 'STAGING': return 'warning';
      case 'DEVELOPMENT': return 'info';
      case 'TEST': return 'default';
      default: return 'default';
    }
  };

  const getAccessTypeIcon = (type: string) => {
    switch (type) {
      case 'SSH': return <Terminal />;
      case 'RDP': return <Computer />;
      case 'REST_API': return <Api />;
      case 'K8S_API': return <Apps />;
      default: return <VpnKey />;
    }
  };

  const getTestStatusIcon = (status?: string) => {
    switch (status) {
      case 'SUCCESS': return <CheckCircle color="success" />;
      case 'FAILED': return <Error color="error" />;
      default: return <Warning color="warning" />;
    }
  };

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Compute Resources
      </Typography>
      <Typography variant="body2" color="text.secondary" gutterBottom>
        Manage compute resources and their automation access credentials
      </Typography>

      <Card sx={{ mt: 3 }}>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6">Resources</Typography>
            <Button
              variant="contained"
              startIcon={<Add />}
              onClick={handleCreateResource}
            >
              Add Resource
            </Button>
          </Box>

          <Tabs value={tabValue} onChange={(e, v) => setTabValue(v)} sx={{ mb: 2 }}>
            <Tab label="Resources List" />
            <Tab label="Access Credentials" disabled={!selectedResource} />
          </Tabs>

          {tabValue === 0 && (
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Name</TableCell>
                    <TableCell>Hostname / IP</TableCell>
                    <TableCell>Environment</TableCell>
                    <TableCell>Node Type</TableCell>
                    <TableCell>OS</TableCell>
                    <TableCell>Resources</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {resources.map((resource) => (
                    <TableRow key={resource.id}>
                      <TableCell>
                        <Typography variant="subtitle2">{resource.name}</Typography>
                        {resource.purpose && (
                          <Typography variant="caption" color="text.secondary">
                            {resource.purpose}
                          </Typography>
                        )}
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">{resource.hostname}</Typography>
                        {resource.ipAddress && (
                          <Typography variant="caption" color="text.secondary">
                            {resource.ipAddress}
                          </Typography>
                        )}
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={resource.environment}
                          color={getEnvironmentColor(resource.environment) as any}
                          size="small"
                        />
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={resource.nodeType === 'dedicated_server' ? 'Dedicated Server' : 'Virtual Machine'}
                          color={resource.nodeType === 'dedicated_server' ? 'primary' : 'secondary'}
                          size="small"
                          variant="outlined"
                        />
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">
                          {resource.osVersionDisplay || 'Not specified'}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Stack direction="row" spacing={1}>
                          {resource.cpuCores && (
                            <Chip
                              icon={<Memory />}
                              label={`${resource.cpuCores} CPU`}
                              size="small"
                              variant="outlined"
                            />
                          )}
                          {resource.memoryGb && (
                            <Chip
                              label={`${resource.memoryGb}GB RAM`}
                              size="small"
                              variant="outlined"
                            />
                          )}
                        </Stack>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={resource.status}
                          color={getStatusColor(resource.status) as any}
                          size="small"
                        />
                      </TableCell>
                      <TableCell>
                        <Tooltip title="Manage Access">
                          <IconButton
                            size="small"
                            onClick={() => handleViewCredentials(resource)}
                          >
                            <Key />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Edit">
                          <IconButton
                            size="small"
                            onClick={() => handleEditResource(resource)}
                          >
                            <Edit />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Delete">
                          <IconButton
                            size="small"
                            color="error"
                            onClick={() => handleDeleteResource(resource.id)}
                          >
                            <Delete />
                          </IconButton>
                        </Tooltip>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}

          {tabValue === 1 && selectedResource && (
            <Box>
              <Alert severity="info" sx={{ mb: 2 }}>
                Managing access credentials for: <strong>{selectedResource.name}</strong> ({selectedResource.hostname})
              </Alert>
              
              <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 2 }}>
                <Button
                  variant="contained"
                  startIcon={<Add />}
                  onClick={handleAddCredential}
                >
                  Add Credential
                </Button>
              </Box>

              <List>
                {credentials.map((credential, index) => (
                  <React.Fragment key={credential.id}>
                    {index > 0 && <Divider />}
                    <ListItem>
                      <Box sx={{ display: 'flex', alignItems: 'center', mr: 2 }}>
                        {getAccessTypeIcon(credential.accessType)}
                      </Box>
                      <ListItemText
                        primary={
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <Typography variant="subtitle2">
                              {credential.accessName || credential.accessType}
                            </Typography>
                            {credential.isPrimary && (
                              <Chip label="Primary" size="small" color="primary" />
                            )}
                            {!credential.isActive && (
                              <Chip label="Inactive" size="small" />
                            )}
                          </Box>
                        }
                        secondary={
                          <Box>
                            <Typography variant="caption" component="div">
                              {credential.authType} • {credential.username}@{credential.host}:{credential.port}
                            </Typography>
                            {credential.lastTestedAt && (
                              <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mt: 0.5 }}>
                                {getTestStatusIcon(credential.lastTestStatus)}
                                <Typography variant="caption" color="text.secondary">
                                  Last tested: {new Date(credential.lastTestedAt).toLocaleString()}
                                </Typography>
                              </Box>
                            )}
                          </Box>
                        }
                      />
                      <ListItemSecondaryAction>
                        <Tooltip title="Test Connection">
                          <IconButton
                            edge="end"
                            onClick={() => handleTestCredential(credential.id)}
                          >
                            <CheckCircle />
                          </IconButton>
                        </Tooltip>
                      </ListItemSecondaryAction>
                    </ListItem>
                  </React.Fragment>
                ))}
              </List>
            </Box>
          )}
        </CardContent>
      </Card>

      {/* Create/Edit Resource Dialog */}
      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="md" fullWidth>
        <DialogTitle>
          {selectedResource ? 'Edit Compute Resource' : 'Add Compute Resource'}
        </DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            <Box sx={{ display: 'flex', gap: 2 }}>
              <TextField
                fullWidth
                label="Resource Name"
                name="name"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                required
              />
              <TextField
                fullWidth
                label="Hostname"
                name="hostname"
                value={formData.hostname}
                onChange={(e) => setFormData({ ...formData, hostname: e.target.value })}
                required
              />
            </Box>
            <Box sx={{ display: 'flex', gap: 2 }}>
              <TextField
                fullWidth
                label="IP Address"
                name="ipAddress"
                value={formData.ipAddress}
                onChange={(e) => setFormData({ ...formData, ipAddress: e.target.value })}
              />
              <FormControl fullWidth>
                <InputLabel>Environment</InputLabel>
                <Select
                  value={formData.environment}
                  onChange={(e) => setFormData({ ...formData, environment: e.target.value })}
                  label="Environment"
                >
                  <MenuItem value="PRODUCTION">Production</MenuItem>
                  <MenuItem value="STAGING">Staging</MenuItem>
                  <MenuItem value="DEVELOPMENT">Development</MenuItem>
                  <MenuItem value="TEST">Test</MenuItem>
                </Select>
              </FormControl>
            </Box>
            <Box sx={{ display: 'flex', gap: 2 }}>
              <FormControl fullWidth>
                <InputLabel>Status</InputLabel>
                <Select
                  value={formData.status}
                  onChange={(e) => setFormData({ ...formData, status: e.target.value })}
                  label="Status"
                >
                  <MenuItem value="ACTIVE">Active</MenuItem>
                  <MenuItem value="SUSPENDED">Suspended</MenuItem>
                  <MenuItem value="DISCONTINUED">Discontinued</MenuItem>
                  <MenuItem value="MAINTENANCE">Maintenance</MenuItem>
                </Select>
              </FormControl>
              <TextField
                fullWidth
                label="Purpose"
                name="purpose"
                value={formData.purpose}
                onChange={(e) => setFormData({ ...formData, purpose: e.target.value })}
                placeholder="e.g., Web Server, Database, CI/CD"
              />
            </Box>
            <Box sx={{ display: 'flex', gap: 2 }}>
              <TextField
                fullWidth
                label="CPU Cores"
                name="cpuCores"
                type="number"
                value={formData.cpuCores}
                onChange={(e) => setFormData({ ...formData, cpuCores: parseInt(e.target.value) || 0 })}
              />
              <TextField
                fullWidth
                label="Memory (GB)"
                name="memoryGb"
                type="number"
                value={formData.memoryGb}
                onChange={(e) => setFormData({ ...formData, memoryGb: parseInt(e.target.value) || 0 })}
              />
              <TextField
                fullWidth
                label="Storage (GB)"
                name="storageGb"
                type="number"
                value={formData.storageGb}
                onChange={(e) => setFormData({ ...formData, storageGb: parseInt(e.target.value) || 0 })}
              />
            </Box>
            <Box sx={{ display: 'flex', gap: 2 }}>
              <FormControl fullWidth>
                <InputLabel>Node Type</InputLabel>
                <Select
                  value={formData.nodeType}
                  onChange={(e) => setFormData({ ...formData, nodeType: e.target.value })}
                  label="Node Type"
                >
                  <MenuItem value="dedicated_server">Dedicated Server</MenuItem>
                  <MenuItem value="vm">Virtual Machine</MenuItem>
                </Select>
              </FormControl>
              <FormControl fullWidth>
                <InputLabel>Operating System</InputLabel>
                <Select
                  value={formData.osVersionId}
                  onChange={(e) => setFormData({ ...formData, osVersionId: e.target.value })}
                  label="Operating System"
                >
                  <MenuItem value="">Select OS...</MenuItem>
                  {osVersions.map((osVersion) => (
                    <MenuItem key={osVersion.id} value={osVersion.id}>
                      {osVersion.displayName}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Box>
            <TextField
              fullWidth
              label="Tags (comma separated)"
              name="tags"
              value={formData.tags}
              onChange={(e) => setFormData({ ...formData, tags: e.target.value })}
              placeholder="e.g., web, nginx, production"
            />
            <TextField
              fullWidth
              multiline
              rows={3}
              label="Notes"
              name="notes"
              value={formData.notes}
              onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button onClick={handleSubmit} variant="contained">
            {selectedResource ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Add Credential Dialog */}
      <Dialog open={openCredentialDialog} onClose={() => setOpenCredentialDialog(false)} maxWidth="md" fullWidth>
        <DialogTitle>Add Access Credential</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            <Box sx={{ display: 'flex', gap: 2 }}>
              <FormControl fullWidth>
                <InputLabel>Access Type</InputLabel>
                <Select
                  value={credentialFormData.accessType}
                  onChange={(e) => setCredentialFormData({ ...credentialFormData, accessType: e.target.value })}
                  label="Access Type"
                >
                  <MenuItem value="SSH">SSH</MenuItem>
                  <MenuItem value="RDP">RDP (Windows)</MenuItem>
                  <MenuItem value="WINRM">WinRM</MenuItem>
                  <MenuItem value="REST_API">REST API</MenuItem>
                  <MenuItem value="ANSIBLE">Ansible</MenuItem>
                  <MenuItem value="K8S_API">Kubernetes API</MenuItem>
                  <MenuItem value="DOCKER_API">Docker API</MenuItem>
                  <MenuItem value="FTP">FTP</MenuItem>
                  <MenuItem value="SFTP">SFTP</MenuItem>
                </Select>
              </FormControl>
              <TextField
                fullWidth
                label="Access Name"
                value={credentialFormData.accessName}
                onChange={(e) => setCredentialFormData({ ...credentialFormData, accessName: e.target.value })}
                placeholder="e.g., Primary SSH, Ansible Access"
              />
            </Box>
            <Box sx={{ display: 'flex', gap: 2 }}>
              <TextField
                fullWidth
                label="Host"
                value={credentialFormData.host}
                onChange={(e) => setCredentialFormData({ ...credentialFormData, host: e.target.value })}
                required
              />
              <TextField
                fullWidth
                label="Port"
                type="number"
                value={credentialFormData.port}
                onChange={(e) => setCredentialFormData({ ...credentialFormData, port: parseInt(e.target.value) || 22 })}
                required
              />
            </Box>
            <Box sx={{ display: 'flex', gap: 2 }}>
              <FormControl fullWidth>
                <InputLabel>Auth Type</InputLabel>
                <Select
                  value={credentialFormData.authType}
                  onChange={(e) => setCredentialFormData({ ...credentialFormData, authType: e.target.value })}
                  label="Auth Type"
                >
                  <MenuItem value="PASSWORD">Password</MenuItem>
                  <MenuItem value="SSH_KEY">SSH Key</MenuItem>
                  <MenuItem value="CERTIFICATE">Certificate</MenuItem>
                  <MenuItem value="TOKEN">Token</MenuItem>
                  <MenuItem value="API_KEY">API Key</MenuItem>
                  <MenuItem value="OAUTH2">OAuth2</MenuItem>
                </Select>
              </FormControl>
              <TextField
                fullWidth
                label="Username"
                value={credentialFormData.username}
                onChange={(e) => setCredentialFormData({ ...credentialFormData, username: e.target.value })}
              />
            </Box>
            
            {credentialFormData.authType === 'PASSWORD' && (
              <TextField
                fullWidth
                label="Password"
                type={showPassword ? 'text' : 'password'}
                value={credentialFormData.passwordEncrypted}
                onChange={(e) => setCredentialFormData({ ...credentialFormData, passwordEncrypted: e.target.value })}
                InputProps={{
                  endAdornment: (
                    <IconButton onClick={() => setShowPassword(!showPassword)} edge="end">
                      {showPassword ? <VisibilityOff /> : <Visibility />}
                    </IconButton>
                  ),
                }}
              />
            )}
            
            {credentialFormData.authType === 'SSH_KEY' && (
              <>
                <TextField
                  fullWidth
                  multiline
                  rows={4}
                  label="SSH Private Key"
                  value={credentialFormData.sshPrivateKey}
                  onChange={(e) => setCredentialFormData({ ...credentialFormData, sshPrivateKey: e.target.value })}
                  placeholder="-----BEGIN OPENSSH PRIVATE KEY-----"
                />
                <TextField
                  fullWidth
                  multiline
                  rows={2}
                  label="SSH Public Key (optional)"
                  value={credentialFormData.sshPublicKey}
                  onChange={(e) => setCredentialFormData({ ...credentialFormData, sshPublicKey: e.target.value })}
                />
              </>
            )}
            
            {credentialFormData.authType === 'API_KEY' && (
              <TextField
                fullWidth
                label="API Key"
                value={credentialFormData.apiKey}
                onChange={(e) => setCredentialFormData({ ...credentialFormData, apiKey: e.target.value })}
              />
            )}
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenCredentialDialog(false)}>Cancel</Button>
          <Button onClick={handleSubmitCredential} variant="contained">
            Add Credential
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Compute;