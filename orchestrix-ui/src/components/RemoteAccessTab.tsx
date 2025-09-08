import React, { useState, useEffect } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Typography,
  IconButton,
  Chip,
  Alert,
  Tooltip,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Menu,
  MenuItem,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  CircularProgress
} from '@mui/material';
import {
  Add,
  Edit,
  Delete,
  MoreVert,
  VpnKey,
  Computer,
  CheckCircle,
  Error,
  Warning,
  Schedule,
  Security,
  Sync,
  Science,
  Star,
  StarBorder,
  ContentCopy,
  Refresh
} from '@mui/icons-material';
import RemoteAccessDialog from './RemoteAccessDialog';

interface RemoteAccessTabProps {
  deviceType: string;
  deviceId: number;
  deviceName: string;
}

const RemoteAccessTab: React.FC<RemoteAccessTabProps> = ({
  deviceType,
  deviceId,
  deviceName
}) => {
  const [remoteAccesses, setRemoteAccesses] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [selectedAccess, setSelectedAccess] = useState<any>(null);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [menuAccessId, setMenuAccessId] = useState<number | null>(null);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [testResults, setTestResults] = useState<{ [key: number]: any }>({});

  useEffect(() => {
    fetchRemoteAccesses();
  }, [deviceType, deviceId]);

  const fetchRemoteAccesses = async () => {
    setLoading(true);
    try {
      const response = await fetch(`/api/remote-access/device/${deviceType}/${deviceId}`);
      if (response.ok) {
        const data = await response.json();
        setRemoteAccesses(data);
      }
    } catch (error) {
      console.error('Error fetching remote accesses:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleAdd = () => {
    setSelectedAccess(null);
    setDialogOpen(true);
  };

  const handleEdit = (access: any) => {
    setSelectedAccess(access);
    setDialogOpen(true);
    setAnchorEl(null);
  };

  const handleDelete = async () => {
    if (menuAccessId) {
      try {
        const response = await fetch(`/api/remote-access/${menuAccessId}`, {
          method: 'DELETE'
        });
        if (response.ok) {
          fetchRemoteAccesses();
        }
      } catch (error) {
        console.error('Error deleting remote access:', error);
      }
    }
    setDeleteDialogOpen(false);
    setMenuAccessId(null);
  };

  const handleSave = async (data: any) => {
    try {
      const url = selectedAccess 
        ? `/api/remote-access/${selectedAccess.id}`
        : '/api/remote-access';
      
      const response = await fetch(url, {
        method: selectedAccess ? 'PUT' : 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
      });
      
      if (response.ok) {
        fetchRemoteAccesses();
      }
    } catch (error) {
      console.error('Error saving remote access:', error);
    }
  };

  const handleTest = async (accessId: number) => {
    setTestResults(prev => ({ ...prev, [accessId]: 'testing' }));
    try {
      const response = await fetch(`/api/remote-access/${accessId}/test`, {
        method: 'POST'
      });
      const result = await response.json();
      setTestResults(prev => ({ ...prev, [accessId]: result.status }));
      
      // Refresh data to get updated test status
      setTimeout(fetchRemoteAccesses, 2000);
    } catch (error) {
      setTestResults(prev => ({ ...prev, [accessId]: 'error' }));
    }
  };

  const handleSync = async (accessId: number) => {
    try {
      await fetch(`/api/remote-access/${accessId}/sync`, {
        method: 'POST'
      });
      fetchRemoteAccesses();
    } catch (error) {
      console.error('Error syncing with Bitwarden:', error);
    }
  };

  const handleSetPrimary = async (accessId: number) => {
    const access = remoteAccesses.find(a => a.id === accessId);
    if (access) {
      access.isPrimary = true;
      handleSave(access);
    }
  };

  const getStatusIcon = (access: any) => {
    if (testResults[access.id] === 'testing') {
      return <CircularProgress size={16} />;
    }
    
    const status = testResults[access.id] || access.lastTestStatus;
    
    switch (status) {
      case 'SUCCESS':
      case 'success':
        return <CheckCircle color="success" fontSize="small" />;
      case 'FAILED':
      case 'failed':
        return <Error color="error" fontSize="small" />;
      case 'UNTESTED':
        return <Warning color="warning" fontSize="small" />;
      default:
        return <Schedule color="disabled" fontSize="small" />;
    }
  };

  const getAuthMethodLabel = (method: string) => {
    const labels: any = {
      PASSWORD: 'Password',
      SSH_KEY: 'SSH Key',
      SSH_KEY_WITH_PASSPHRASE: 'SSH Key + Pass',
      CERTIFICATE: 'Certificate',
      API_KEY: 'API Key',
      BEARER_TOKEN: 'Bearer Token',
      OAUTH2: 'OAuth 2.0',
      KERBEROS: 'Kerberos',
      BASIC_AUTH: 'Basic Auth'
    };
    return labels[method] || method;
  };

  const getAccessTypeIcon = (type: string) => {
    switch (type) {
      case 'SSH':
      case 'TELNET':
      case 'SERIAL':
        return '🖥️';
      case 'RDP':
      case 'VNC':
        return '🖱️';
      case 'HTTPS':
      case 'HTTP':
      case 'REST_API':
        return '🌐';
      case 'KUBERNETES_API':
      case 'DOCKER_API':
        return '🐳';
      default:
        return '🔌';
    }
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" p={3}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h6">Remote Access Configurations</Typography>
        <Button
          variant="contained"
          startIcon={<Add />}
          onClick={handleAdd}
        >
          Add Remote Access
        </Button>
      </Box>

      {remoteAccesses.length === 0 ? (
        <Alert severity="info">
          No remote access configurations found. Click "Add Remote Access" to create one.
        </Alert>
      ) : (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Name</TableCell>
                <TableCell>Type</TableCell>
                <TableCell>Host</TableCell>
                <TableCell>Auth</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Bitwarden</TableCell>
                <TableCell>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {remoteAccesses.map((access) => (
                <TableRow key={access.id}>
                  <TableCell>
                    <Box display="flex" alignItems="center" gap={1}>
                      {access.isPrimary && (
                        <Tooltip title="Primary Access">
                          <Star color="primary" fontSize="small" />
                        </Tooltip>
                      )}
                      <Typography variant="body2">
                        {access.accessName || 'Unnamed'}
                      </Typography>
                      {access.isEmergencyAccess && (
                        <Chip label="Emergency" size="small" color="error" />
                      )}
                    </Box>
                  </TableCell>
                  
                  <TableCell>
                    <Box display="flex" alignItems="center" gap={1}>
                      <span>{getAccessTypeIcon(access.accessType)}</span>
                      <Typography variant="body2">{access.accessType}</Typography>
                    </Box>
                  </TableCell>
                  
                  <TableCell>
                    <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                      {access.host}:{access.port}
                    </Typography>
                  </TableCell>
                  
                  <TableCell>
                    <Chip
                      label={getAuthMethodLabel(access.authMethod)}
                      size="small"
                      variant="outlined"
                    />
                  </TableCell>
                  
                  <TableCell>
                    <Box display="flex" alignItems="center" gap={1}>
                      {getStatusIcon(access)}
                      <Typography variant="caption">
                        {access.lastTestStatus || 'Untested'}
                      </Typography>
                    </Box>
                  </TableCell>
                  
                  <TableCell>
                    {access.bitwardenItemId ? (
                      <Tooltip title={`Last sync: ${access.bitwardenLastSync || 'Never'}`}>
                        <Chip
                          icon={<VpnKey />}
                          label="Synced"
                          size="small"
                          color="success"
                          variant="outlined"
                        />
                      </Tooltip>
                    ) : (
                      <Chip
                        label="Not synced"
                        size="small"
                        variant="outlined"
                        color="default"
                      />
                    )}
                  </TableCell>
                  
                  <TableCell>
                    <Box display="flex" gap={0.5}>
                      <Tooltip title="Test Connection">
                        <IconButton
                          size="small"
                          onClick={() => handleTest(access.id)}
                          disabled={testResults[access.id] === 'testing'}
                        >
                          <Science fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      
                      {access.bitwardenSyncEnabled && (
                        <Tooltip title="Sync with Bitwarden">
                          <IconButton
                            size="small"
                            onClick={() => handleSync(access.id)}
                          >
                            <Sync fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      )}
                      
                      {!access.isPrimary && (
                        <Tooltip title="Set as Primary">
                          <IconButton
                            size="small"
                            onClick={() => handleSetPrimary(access.id)}
                          >
                            <StarBorder fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      )}
                      
                      <IconButton
                        size="small"
                        onClick={(e) => {
                          setAnchorEl(e.currentTarget);
                          setMenuAccessId(access.id);
                        }}
                      >
                        <MoreVert fontSize="small" />
                      </IconButton>
                    </Box>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={() => setAnchorEl(null)}
      >
        <MenuItem onClick={() => {
          const access = remoteAccesses.find(a => a.id === menuAccessId);
          if (access) handleEdit(access);
        }}>
          <Edit fontSize="small" sx={{ mr: 1 }} /> Edit
        </MenuItem>
        <MenuItem onClick={() => {
          const access = remoteAccesses.find(a => a.id === menuAccessId);
          if (access && access.connectionUrl) {
            navigator.clipboard.writeText(access.connectionUrl);
          }
          setAnchorEl(null);
        }}>
          <ContentCopy fontSize="small" sx={{ mr: 1 }} /> Copy Connection
        </MenuItem>
        <MenuItem onClick={() => {
          setDeleteDialogOpen(true);
          setAnchorEl(null);
        }} sx={{ color: 'error.main' }}>
          <Delete fontSize="small" sx={{ mr: 1 }} /> Delete
        </MenuItem>
      </Menu>

      <RemoteAccessDialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        deviceType={deviceType}
        deviceId={deviceId}
        deviceName={deviceName}
        existingAccess={selectedAccess}
        onSave={handleSave}
      />

      <Dialog open={deleteDialogOpen} onClose={() => setDeleteDialogOpen(false)}>
        <DialogTitle>Confirm Delete</DialogTitle>
        <DialogContent>
          Are you sure you want to delete this remote access configuration?
          {remoteAccesses.find(a => a.id === menuAccessId)?.bitwardenItemId && (
            <Alert severity="warning" sx={{ mt: 2 }}>
              This will also delete the associated credentials from Bitwarden.
            </Alert>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleDelete} color="error" variant="contained">
            Delete
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default RemoteAccessTab;