import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  IconButton,
  Chip,
  TextField,
  InputAdornment,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Tabs,
  Tab,
  Tooltip,
  CircularProgress,
  Badge
} from '@mui/material';
import {
  Add,
  Edit,
  Delete,
  Search,
  FilterList,
  VpnKey,
  CheckCircle,
  Error,
  Warning,
  Refresh,
  Settings,
  Security,
  Science,
  Sync,
  Schedule,
  RotateRight
} from '@mui/icons-material';
import RemoteAccessDialog from '../components/RemoteAccessDialog';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;
  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`tabpanel-${index}`}
      aria-labelledby={`tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  );
}

const RemoteAccess: React.FC = () => {
  const [activeTab, setActiveTab] = useState(0);
  const [remoteAccesses, setRemoteAccesses] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState('ALL');
  const [filterStatus, setFilterStatus] = useState('ALL');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [selectedAccess, setSelectedAccess] = useState<any>(null);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [deleteId, setDeleteId] = useState<number | null>(null);
  const [providersStatus, setProvidersStatus] = useState<any>(null);
  const [statistics, setStatistics] = useState<any>(null);

  useEffect(() => {
    fetchRemoteAccesses();
    fetchStatistics();
    fetchProvidersStatus();
  }, []);

  const fetchRemoteAccesses = async () => {
    setLoading(true);
    try {
      const response = await fetch('/api/remote-access');
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

  const fetchStatistics = async () => {
    try {
      const response = await fetch('/api/remote-access/stats');
      if (response.ok) {
        const data = await response.json();
        setStatistics(data);
      }
    } catch (error) {
      console.error('Error fetching statistics:', error);
    }
  };

  const fetchProvidersStatus = async () => {
    try {
      const response = await fetch('/api/remote-access/providers/status');
      if (response.ok) {
        const data = await response.json();
        setProvidersStatus(data);
      }
    } catch (error) {
      console.error('Error fetching providers status:', error);
      setProvidersStatus({ error: true });
    }
  };
  
  const testProviderConnection = async (providerType: string) => {
    try {
      const response = await fetch(`/api/remote-access/provider/${providerType}/test`);
      if (response.ok) {
        const data = await response.json();
        // Refresh providers status after test
        fetchProvidersStatus();
        return data;
      }
    } catch (error) {
      console.error(`Error testing ${providerType} connection:`, error);
      return { status: 'error' };
    }
  };

  const handleAdd = () => {
    setSelectedAccess(null);
    setDialogOpen(true);
  };

  const handleEdit = (access: any) => {
    setSelectedAccess(access);
    setDialogOpen(true);
  };

  const handleDelete = async () => {
    if (deleteId) {
      try {
        const response = await fetch(`/api/remote-access/${deleteId}`, {
          method: 'DELETE'
        });
        if (response.ok) {
          fetchRemoteAccesses();
          fetchStatistics();
        }
      } catch (error) {
        console.error('Error deleting remote access:', error);
      }
    }
    setDeleteDialogOpen(false);
    setDeleteId(null);
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
        fetchStatistics();
      }
    } catch (error) {
      console.error('Error saving remote access:', error);
    }
  };

  const handleTest = async (id: number) => {
    try {
      const response = await fetch(`/api/remote-access/${id}/test`, {
        method: 'POST'
      });
      if (response.ok) {
        fetchRemoteAccesses();
      }
    } catch (error) {
      console.error('Error testing connection:', error);
    }
  };

  const handleSync = async (id: number) => {
    try {
      const response = await fetch(`/api/remote-access/${id}/sync`, {
        method: 'POST'
      });
      if (response.ok) {
        fetchRemoteAccesses();
      }
    } catch (error) {
      console.error('Error syncing with Bitwarden:', error);
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'SUCCESS':
        return <CheckCircle color="success" fontSize="small" />;
      case 'FAILED':
      case 'AUTH_FAILED':
      case 'CONNECTION_REFUSED':
        return <Error color="error" fontSize="small" />;
      case 'UNTESTED':
        return <Warning color="warning" fontSize="small" />;
      default:
        return <Schedule color="disabled" fontSize="small" />;
    }
  };

  const filteredAccesses = remoteAccesses.filter(access => {
    const matchesSearch = searchTerm === '' || 
      access.deviceName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      access.accessName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      access.host?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      access.username?.toLowerCase().includes(searchTerm.toLowerCase());
    
    const matchesType = filterType === 'ALL' || access.deviceType === filterType;
    const matchesStatus = filterStatus === 'ALL' || 
      (filterStatus === 'ACTIVE' && access.isActive) ||
      (filterStatus === 'INACTIVE' && !access.isActive);
    
    return matchesSearch && matchesType && matchesStatus;
  });

  const needsRotation = remoteAccesses.filter(a => a.needsRotation).length;
  const expired = remoteAccesses.filter(a => a.isExpired).length;

  return (
    <Box sx={{ p: 3 }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Remote Access Management</Typography>
        <Box display="flex" gap={2}>
          {providersStatus && providersStatus.defaultProvider && (
            <Chip
              icon={<Security />}
              label={`Default: ${providersStatus.defaultProvider}`}
              color="primary"
            />
          )}
          <Button
            variant="outlined"
            startIcon={<Settings />}
            onClick={() => setActiveTab(2)}
          >
            Settings
          </Button>
          <Button
            variant="contained"
            startIcon={<Add />}
            onClick={handleAdd}
          >
            Add Remote Access
          </Button>
        </Box>
      </Box>

      {/* Statistics Cards */}
      <Box display="flex" gap={2} mb={3}>
        <Card sx={{ flex: 1 }}>
          <CardContent>
            <Typography color="textSecondary" gutterBottom>
              Total Configurations
            </Typography>
            <Typography variant="h5">
              {statistics?.total || 0}
            </Typography>
          </CardContent>
        </Card>
        <Card sx={{ flex: 1 }}>
          <CardContent>
            <Typography color="textSecondary" gutterBottom>
              Active
            </Typography>
            <Typography variant="h5" color="success.main">
              {statistics?.active || 0}
            </Typography>
          </CardContent>
        </Card>
        <Card sx={{ flex: 1 }}>
          <CardContent>
            <Typography color="textSecondary" gutterBottom>
              Needs Rotation
            </Typography>
            <Typography variant="h5" color="warning.main">
              <Badge badgeContent={needsRotation} color="warning">
                <RotateRight />
              </Badge>
            </Typography>
          </CardContent>
        </Card>
        <Card sx={{ flex: 1 }}>
          <CardContent>
            <Typography color="textSecondary" gutterBottom>
              Expired
            </Typography>
            <Typography variant="h5" color="error.main">
              <Badge badgeContent={expired} color="error">
                <Warning />
              </Badge>
            </Typography>
          </CardContent>
        </Card>
      </Box>

      <Card>
        <Tabs value={activeTab} onChange={(e, v) => setActiveTab(v)}>
          <Tab label="All Credentials" />
          <Tab label="By Device Type" />
          <Tab label="Provider Settings" />
        </Tabs>

        <TabPanel value={activeTab} index={0}>
          {/* Filters */}
          <Box display="flex" gap={2} mb={2}>
            <TextField
              placeholder="Search..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              size="small"
              sx={{ flex: 1 }}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <Search />
                  </InputAdornment>
                )
              }}
            />
            <FormControl size="small" sx={{ minWidth: 150 }}>
              <InputLabel>Device Type</InputLabel>
              <Select
                value={filterType}
                onChange={(e) => setFilterType(e.target.value)}
                label="Device Type"
              >
                <MenuItem value="ALL">All Types</MenuItem>
                <MenuItem value="COMPUTE">Compute</MenuItem>
                <MenuItem value="NETWORK_DEVICE">Network Device</MenuItem>
                <MenuItem value="STORAGE">Storage</MenuItem>
                <MenuItem value="FIREWALL">Firewall</MenuItem>
                <MenuItem value="LOAD_BALANCER">Load Balancer</MenuItem>
              </Select>
            </FormControl>
            <FormControl size="small" sx={{ minWidth: 120 }}>
              <InputLabel>Status</InputLabel>
              <Select
                value={filterStatus}
                onChange={(e) => setFilterStatus(e.target.value)}
                label="Status"
              >
                <MenuItem value="ALL">All</MenuItem>
                <MenuItem value="ACTIVE">Active</MenuItem>
                <MenuItem value="INACTIVE">Inactive</MenuItem>
              </Select>
            </FormControl>
            <IconButton onClick={fetchRemoteAccesses}>
              <Refresh />
            </IconButton>
          </Box>

          {/* Table */}
          {loading ? (
            <Box display="flex" justifyContent="center" p={3}>
              <CircularProgress />
            </Box>
          ) : (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Device</TableCell>
                    <TableCell>Access Name</TableCell>
                    <TableCell>Type</TableCell>
                    <TableCell>Host</TableCell>
                    <TableCell>Username</TableCell>
                    <TableCell>Auth Method</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Provider</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {filteredAccesses.map((access) => (
                    <TableRow key={access.id}>
                      <TableCell>
                        <Box>
                          <Typography variant="body2">{access.deviceName}</Typography>
                          <Typography variant="caption" color="textSecondary">
                            {access.deviceType}
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Box display="flex" alignItems="center" gap={1}>
                          {access.isPrimary && (
                            <Chip label="Primary" size="small" color="primary" />
                          )}
                          {access.isEmergencyAccess && (
                            <Chip label="Emergency" size="small" color="error" />
                          )}
                          <Typography variant="body2">
                            {access.accessName || 'Unnamed'}
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell>{access.accessType}</TableCell>
                      <TableCell>
                        <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                          {access.host}:{access.port}
                        </Typography>
                      </TableCell>
                      <TableCell>{access.username}</TableCell>
                      <TableCell>
                        <Chip
                          label={access.authMethod?.replace(/_/g, ' ')}
                          size="small"
                          variant="outlined"
                        />
                      </TableCell>
                      <TableCell>
                        <Box display="flex" alignItems="center" gap={0.5}>
                          {getStatusIcon(access.lastTestStatus || 'UNTESTED')}
                          <Typography variant="caption">
                            {access.lastTestStatus || 'Untested'}
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Chip
                          icon={<VpnKey />}
                          label={access.secretProviderType || 'LOCAL_ENCRYPTED'}
                          size="small"
                          color={access.secretItemId ? 'success' : 'default'}
                          variant="outlined"
                        />
                      </TableCell>
                      <TableCell>
                        <Box display="flex" gap={0.5}>
                          <Tooltip title="Test Connection">
                            <IconButton
                              size="small"
                              onClick={() => handleTest(access.id)}
                            >
                              <Science fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          {access.secretProviderType && access.secretItemId && (
                            <Tooltip title={`Sync with ${access.secretProviderType}`}>
                              <IconButton
                                size="small"
                                onClick={() => handleSync(access.id)}
                              >
                                <Sync fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          )}
                          <Tooltip title="Edit">
                            <IconButton
                              size="small"
                              onClick={() => handleEdit(access)}
                            >
                              <Edit fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="Delete">
                            <IconButton
                              size="small"
                              onClick={() => {
                                setDeleteId(access.id);
                                setDeleteDialogOpen(true);
                              }}
                            >
                              <Delete fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        </Box>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </TabPanel>

        <TabPanel value={activeTab} index={1}>
          {/* By Device Type View */}
          <Box display="flex" flexWrap="wrap" gap={2}>
            {statistics?.byDeviceType && Object.entries(statistics.byDeviceType).map(([type, count]: any) => (
              <Card key={type} sx={{ flex: '1 1 300px' }}>
                <CardContent>
                  <Typography variant="h6">{type}</Typography>
                  <Typography variant="h4">{count}</Typography>
                  <Button
                    size="small"
                    onClick={() => {
                      setFilterType(type);
                      setActiveTab(0);
                    }}
                  >
                    View All
                  </Button>
                </CardContent>
              </Card>
            ))}
          </Box>
        </TabPanel>

        <TabPanel value={activeTab} index={2}>
          {/* Bitwarden Settings */}
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Secret Providers Configuration
              </Typography>
              {providersStatus && providersStatus.providers && (
                <Box sx={{ mb: 2 }}>
                  {Object.entries(providersStatus.providers).map(([provider, status]: [string, any]) => (
                    <Alert 
                      key={provider}
                      severity={status.connected ? 'success' : 'warning'}
                      sx={{ mb: 1 }}
                    >
                      <strong>{provider}:</strong> {status.connected ? 'Connected' : 'Not Connected'}
                      {status.errorMessage && ` - ${status.errorMessage}`}
                    </Alert>
                  ))}
                </Box>
              )}
              <Box display="flex" flexDirection="column" gap={2}>
                <TextField
                  label="Default Provider"
                  value={providersStatus?.defaultProvider || 'LOCAL_ENCRYPTED'}
                  disabled
                  fullWidth
                  helperText="Configure in application.properties"
                />
                <TextField
                  label="Organization ID"
                  value="From environment variable"
                  disabled
                  fullWidth
                  helperText="Set BITWARDEN_ORG_ID environment variable"
                />
                <TextField
                  label="Default Collection"
                  value="Infrastructure-Dev"
                  disabled
                  fullWidth
                  helperText="Configure in application.properties"
                />
                <Button
                  variant="outlined"
                  startIcon={<Refresh />}
                  onClick={fetchProvidersStatus}
                >
                  Refresh Status
                </Button>
              </Box>
            </CardContent>
          </Card>
        </TabPanel>
      </Card>

      <RemoteAccessDialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        deviceType=""
        deviceId={0}
        deviceName=""
        existingAccess={selectedAccess}
        onSave={handleSave}
      />

      <Dialog open={deleteDialogOpen} onClose={() => setDeleteDialogOpen(false)}>
        <DialogTitle>Confirm Delete</DialogTitle>
        <DialogContent>
          Are you sure you want to delete this remote access configuration?
          This will also delete the associated credentials from Bitwarden if synced.
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

export default RemoteAccess;