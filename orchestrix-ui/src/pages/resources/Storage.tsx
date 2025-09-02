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
  LinearProgress,
  Avatar,
  Stack,
  Switch,
  FormControlLabel,
} from '@mui/material';
import {
  Add,
  Edit,
  Delete,
  CloudUpload,
  Storage as StorageIcon,
  Folder,
  CloudQueue,
  Security,
  Backup,
  Warning,
  CheckCircle,
  Link,
  Key,
  Dns,
} from '@mui/icons-material';
import axios from 'axios';

interface StorageResource {
  id: string;
  name: string;
  category: string;
  type: string;
  capacity: string;
  used: string;
  status: string;
  authType: string;
  endpoint: string;
  encryption: string;
  backupEnabled: boolean;
  lastBackup: string;
}

const Storage: React.FC = () => {
  const [storages, setStorages] = useState<StorageResource[]>([]);
  const [loading, setLoading] = useState(true);
  const [openDialog, setOpenDialog] = useState(false);
  const [selectedStorage, setSelectedStorage] = useState<StorageResource | null>(null);
  const [formData, setFormData] = useState({
    name: '',
    category: 'CLOUD',
    type: 'AWS S3',
    capacity: '100',
    authType: 'IAM Role',
    endpoint: '',
    encryption: 'AES-256',
    backupEnabled: true,
  });

  useEffect(() => {
    fetchStorageResources();
  }, []);

  const fetchStorageResources = async () => {
    try {
      const response = await axios.get('/api/api/resources/storage');
      setStorages(response.data.storages || []);
    } catch (error) {
      console.error('Error fetching storage resources:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateStorage = () => {
    setFormData({
      name: '',
      category: 'CLOUD',
      type: 'AWS S3',
      capacity: '100',
      authType: 'IAM Role',
      endpoint: '',
      encryption: 'AES-256',
      backupEnabled: true,
    });
    setSelectedStorage(null);
    setOpenDialog(true);
  };

  const handleEditStorage = (storage: StorageResource) => {
    setFormData({
      name: storage.name,
      category: storage.category,
      type: storage.type,
      capacity: storage.capacity.replace(/[^0-9]/g, ''),
      authType: storage.authType,
      endpoint: storage.endpoint,
      encryption: storage.encryption,
      backupEnabled: storage.backupEnabled,
    });
    setSelectedStorage(storage);
    setOpenDialog(true);
  };

  const handleDeleteStorage = (id: string) => {
    setStorages(storages.filter(s => s.id !== id));
  };

  const handleSubmit = () => {
    setOpenDialog(false);
    fetchStorageResources();
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE': return 'success';
      case 'DEPRECATED': return 'warning';
      case 'INACTIVE': return 'error';
      default: return 'default';
    }
  };

  const getCategoryIcon = (category: string) => {
    switch (category) {
      case 'CLOUD': return <CloudQueue />;
      case 'NETWORK': return <Dns />;
      case 'ATTACHED': return <StorageIcon />;
      default: return <Folder />;
    }
  };

  const getCategoryColor = (category: string) => {
    switch (category) {
      case 'CLOUD': return '#1976D2';
      case 'NETWORK': return '#9C27B0';
      case 'ATTACHED': return '#F57C00';
      default: return '#666';
    }
  };

  const getUsagePercentage = (used: string, capacity: string) => {
    const usedNum = parseFloat(used) || 0;
    const capacityNum = parseFloat(capacity) || 1;
    return Math.round((usedNum / capacityNum) * 100);
  };

  const getUsageColor = (percentage: number) => {
    if (percentage < 60) return 'success';
    if (percentage < 80) return 'warning';
    return 'error';
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
        <Typography>Loading storage resources...</Typography>
      </Box>
    );
  }

  const totalCapacity = storages.reduce((sum, s) => {
    const cap = parseFloat(s.capacity) || 0;
    return sum + cap;
  }, 0);

  const totalUsed = storages.reduce((sum, s) => {
    const used = parseFloat(s.used) || 0;
    return sum + used;
  }, 0);

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Storage Resources</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={handleCreateStorage}>
          Add Storage
        </Button>
      </Box>

      {/* Summary Cards */}
      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 3, mb: 3 }}>
        <Box sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(50% - 12px)', md: '0 0 calc(25% - 18px)' } }}>
          <Card>
            <CardContent>
              <Stack direction="row" spacing={1} alignItems="center">
                <Avatar sx={{ bgcolor: 'primary.main' }}>
                  <StorageIcon />
                </Avatar>
                <Box>
                  <Typography variant="h6">{storages.length}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    Total Storage Systems
                  </Typography>
                </Box>
              </Stack>
            </CardContent>
          </Card>
        </Box>
        <Box sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(50% - 12px)', md: '0 0 calc(25% - 18px)' } }}>
          <Card>
            <CardContent>
              <Stack direction="row" spacing={1} alignItems="center">
                <Avatar sx={{ bgcolor: 'info.main' }}>
                  <CloudUpload />
                </Avatar>
                <Box>
                  <Typography variant="h6">
                    {storages.filter(s => s.category === 'CLOUD').length}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Cloud Storage
                  </Typography>
                </Box>
              </Stack>
            </CardContent>
          </Card>
        </Box>
        <Box sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(50% - 12px)', md: '0 0 calc(25% - 18px)' } }}>
          <Card>
            <CardContent>
              <Stack direction="row" spacing={1} alignItems="center">
                <Avatar sx={{ bgcolor: 'warning.main' }}>
                  <Folder />
                </Avatar>
                <Box>
                  <Typography variant="h6">
                    {totalCapacity > 1000 ? `${(totalCapacity / 1000).toFixed(1)} PB` : `${totalCapacity} TB`}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Total Capacity
                  </Typography>
                </Box>
              </Stack>
            </CardContent>
          </Card>
        </Box>
        <Box sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(50% - 12px)', md: '0 0 calc(25% - 18px)' } }}>
          <Card>
            <CardContent>
              <Stack direction="row" spacing={1} alignItems="center">
                <Avatar sx={{ bgcolor: 'success.main' }}>
                  <Backup />
                </Avatar>
                <Box>
                  <Typography variant="h6">
                    {storages.filter(s => s.backupEnabled).length}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Backup Enabled
                  </Typography>
                </Box>
              </Stack>
            </CardContent>
          </Card>
        </Box>
      </Box>

      {/* Storage Table */}
      <Card>
        <CardContent>
          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Name</TableCell>
                  <TableCell>Category</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Capacity / Used</TableCell>
                  <TableCell>Authentication</TableCell>
                  <TableCell>Endpoint</TableCell>
                  <TableCell>Encryption</TableCell>
                  <TableCell>Backup</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {storages.map((storage) => {
                  const usagePercentage = getUsagePercentage(storage.used, storage.capacity);
                  return (
                    <TableRow key={storage.id} hover>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <Avatar sx={{ width: 32, height: 32, bgcolor: getCategoryColor(storage.category) }}>
                            {getCategoryIcon(storage.category)}
                          </Avatar>
                          <Typography variant="body1" fontWeight="medium">
                            {storage.name}
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Chip 
                          label={storage.category} 
                          size="small" 
                          variant="outlined"
                          sx={{ borderColor: getCategoryColor(storage.category), color: getCategoryColor(storage.category) }}
                        />
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">{storage.type}</Typography>
                      </TableCell>
                      <TableCell>
                        <Box>
                          <Typography variant="body2">
                            {storage.used} / {storage.capacity}
                          </Typography>
                          <LinearProgress
                            variant="determinate"
                            value={usagePercentage}
                            color={getUsageColor(usagePercentage) as any}
                            sx={{ mt: 0.5, height: 4, borderRadius: 2 }}
                          />
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                          <Key fontSize="small" sx={{ color: 'text.secondary' }} />
                          <Typography variant="body2">{storage.authType}</Typography>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Tooltip title={storage.endpoint}>
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                            <Link fontSize="small" sx={{ color: 'text.secondary' }} />
                            <Typography variant="body2" fontFamily="monospace" sx={{ maxWidth: 150, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                              {storage.endpoint}
                            </Typography>
                          </Box>
                        </Tooltip>
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                          <Security fontSize="small" sx={{ color: 'success.main' }} />
                          <Typography variant="body2">{storage.encryption}</Typography>
                        </Box>
                      </TableCell>
                      <TableCell>
                        {storage.backupEnabled ? (
                          <Tooltip title={`Last: ${storage.lastBackup}`}>
                            <CheckCircle color="success" fontSize="small" />
                          </Tooltip>
                        ) : (
                          <Warning color="warning" fontSize="small" />
                        )}
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={storage.status}
                          color={getStatusColor(storage.status) as any}
                          size="small"
                        />
                      </TableCell>
                      <TableCell align="right">
                        <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 0.5 }}>
                          <Tooltip title="Edit">
                            <IconButton
                              size="small"
                              onClick={() => handleEditStorage(storage)}
                            >
                              <Edit fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="Delete">
                            <IconButton
                              size="small"
                              color="error"
                              onClick={() => handleDeleteStorage(storage.id)}
                            >
                              <Delete fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        </Box>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>

      {/* Create/Edit Dialog */}
      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="md" fullWidth>
        <DialogTitle>
          {selectedStorage ? 'Edit Storage Resource' : 'Add New Storage Resource'}
        </DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2, mt: 1 }}>
            <Box sx={{ flex: '0 0 100%' }}>
              <TextField
                fullWidth
                label="Storage Name"
                name="name"
                value={formData.name}
                onChange={handleInputChange}
                required
              />
            </Box>
            <Box sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(50% - 8px)' } }}>
              <FormControl fullWidth>
                <InputLabel>Category</InputLabel>
                <Select
                  name="category"
                  value={formData.category}
                  onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                  label="Category"
                >
                  <MenuItem value="CLOUD">Cloud Storage</MenuItem>
                  <MenuItem value="NETWORK">Network Storage</MenuItem>
                  <MenuItem value="ATTACHED">Attached Storage</MenuItem>
                </Select>
              </FormControl>
            </Box>
            <Box sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(50% - 8px)' } }}>
              <FormControl fullWidth>
                <InputLabel>Type</InputLabel>
                <Select
                  name="type"
                  value={formData.type}
                  onChange={(e) => setFormData({ ...formData, type: e.target.value })}
                  label="Type"
                >
                  {formData.category === 'CLOUD' && [
                    <MenuItem key="aws" value="AWS S3">AWS S3</MenuItem>,
                    <MenuItem key="azure" value="Azure Storage">Azure Storage</MenuItem>,
                    <MenuItem key="gcp" value="Google Drive">Google Drive</MenuItem>,
                    <MenuItem key="github" value="GitHub">GitHub</MenuItem>,
                  ]}
                  {formData.category === 'NETWORK' && [
                    <MenuItem key="ftp" value="FTP">FTP</MenuItem>,
                    <MenuItem key="sftp" value="SFTP">SFTP</MenuItem>,
                    <MenuItem key="smb" value="SMB">SMB/CIFS</MenuItem>,
                    <MenuItem key="nfs" value="NFS">NFS</MenuItem>,
                  ]}
                  {formData.category === 'ATTACHED' && [
                    <MenuItem key="nas" value="Network Attached">NAS</MenuItem>,
                    <MenuItem key="san" value="Storage Area Network">SAN</MenuItem>,
                    <MenuItem key="das" value="Direct Attached">DAS</MenuItem>,
                  ]}
                </Select>
              </FormControl>
            </Box>
            <Box sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(50% - 8px)' } }}>
              <TextField
                fullWidth
                label="Capacity (TB)"
                name="capacity"
                type="number"
                value={formData.capacity}
                onChange={handleInputChange}
                required
              />
            </Box>
            <Box sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(50% - 8px)' } }}>
              <FormControl fullWidth>
                <InputLabel>Authentication Type</InputLabel>
                <Select
                  name="authType"
                  value={formData.authType}
                  onChange={(e) => setFormData({ ...formData, authType: e.target.value })}
                  label="Authentication Type"
                >
                  <MenuItem value="IAM Role">IAM Role</MenuItem>
                  <MenuItem value="OAuth2">OAuth2</MenuItem>
                  <MenuItem value="Token">Token</MenuItem>
                  <MenuItem value="SSH Key">SSH Key</MenuItem>
                  <MenuItem value="User/Pass">Username/Password</MenuItem>
                  <MenuItem value="SAS Token">SAS Token</MenuItem>
                  <MenuItem value="SMB">SMB</MenuItem>
                  <MenuItem value="iSCSI">iSCSI</MenuItem>
                </Select>
              </FormControl>
            </Box>
            <Box sx={{ flex: '0 0 100%' }}>
              <TextField
                fullWidth
                label="Endpoint/URL"
                name="endpoint"
                value={formData.endpoint}
                onChange={handleInputChange}
                placeholder="e.g., s3.amazonaws.com, ftp.company.com"
                required
              />
            </Box>
            <Box sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(50% - 8px)' } }}>
              <FormControl fullWidth>
                <InputLabel>Encryption</InputLabel>
                <Select
                  name="encryption"
                  value={formData.encryption}
                  onChange={(e) => setFormData({ ...formData, encryption: e.target.value })}
                  label="Encryption"
                >
                  <MenuItem value="AES-256">AES-256</MenuItem>
                  <MenuItem value="AES-128">AES-128</MenuItem>
                  <MenuItem value="RSA">RSA</MenuItem>
                  <MenuItem value="None">None</MenuItem>
                </Select>
              </FormControl>
            </Box>
            <Box sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(50% - 8px)' } }}>
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.backupEnabled}
                    onChange={(e) => setFormData({ ...formData, backupEnabled: e.target.checked })}
                  />
                }
                label="Enable Backup"
              />
            </Box>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button onClick={handleSubmit} variant="contained">
            {selectedStorage ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Storage;