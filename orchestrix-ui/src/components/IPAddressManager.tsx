import React, { useState, useEffect } from 'react';
import {
  Box,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  IconButton,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Chip,
  Stack,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  OutlinedInput,
  Checkbox,
  ListItemText,
  SelectChangeEvent,
  Typography,
  Alert,
  Tooltip,
  Grid,
  FormControlLabel,
  Switch
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Check as CheckIcon,
  Close as CloseIcon,
  NetworkCheck as NetworkCheckIcon,
  Star as StarIcon,
  StarBorder as StarBorderIcon
} from '@mui/icons-material';
import axios from 'axios';

interface IPAddress {
  id?: number;
  ipAddress: string;
  subnetMask: number;
  gateway?: string;
  isPrimary: boolean;
  isActive: boolean;
  types: string[];
  vlanId?: string;
  macAddress?: string;
  dnsServers?: string;
  networkSegment?: string;
  networkZone?: string;
  assignmentMethod: string;
  description?: string;
  notes?: string;
  tags?: string;
}

interface IPAddressManagerProps {
  deviceType: 'COMPUTE' | 'NETWORK_DEVICE' | 'CONTAINER';
  deviceId: number;
  deviceName: string;
  readOnly?: boolean;
}

const IPAddressManager: React.FC<IPAddressManagerProps> = ({
  deviceType,
  deviceId,
  deviceName,
  readOnly = false
}) => {
  const [ipAddresses, setIpAddresses] = useState<IPAddress[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [openDialog, setOpenDialog] = useState(false);
  const [editingIp, setEditingIp] = useState<IPAddress | null>(null);
  const [ipTypes, setIpTypes] = useState<any[]>([]);
  const [assignmentMethods, setAssignmentMethods] = useState<any[]>([]);
  
  // Form state
  const [formData, setFormData] = useState<IPAddress>({
    ipAddress: '',
    subnetMask: 24,
    gateway: '',
    isPrimary: false,
    isActive: true,
    types: [],
    vlanId: '',
    macAddress: '',
    dnsServers: '',
    networkSegment: '',
    networkZone: '',
    assignmentMethod: 'STATIC',
    description: '',
    notes: '',
    tags: ''
  });

  useEffect(() => {
    fetchIpAddresses();
    fetchIpTypes();
    fetchAssignmentMethods();
  }, [deviceType, deviceId]);

  const fetchIpAddresses = async () => {
    if (!deviceId) return;
    
    setLoading(true);
    setError(null);
    try {
      const response = await axios.get(
        `http://localhost:8090/api/api/ip-addresses/device/${deviceType}/${deviceId}`
      );
      setIpAddresses(response.data);
    } catch (err) {
      console.error('Error fetching IP addresses:', err);
      setError('Failed to load IP addresses');
    } finally {
      setLoading(false);
    }
  };

  const fetchIpTypes = async () => {
    try {
      const response = await axios.get('http://localhost:8090/api/api/ip-addresses/types');
      setIpTypes(response.data);
    } catch (err) {
      console.error('Error fetching IP types:', err);
    }
  };

  const fetchAssignmentMethods = async () => {
    try {
      const response = await axios.get('http://localhost:8090/api/api/ip-addresses/assignment-methods');
      setAssignmentMethods(response.data);
    } catch (err) {
      console.error('Error fetching assignment methods:', err);
    }
  };

  const handleOpenDialog = (ip?: IPAddress) => {
    if (ip) {
      setEditingIp(ip);
      setFormData(ip);
    } else {
      setEditingIp(null);
      setFormData({
        ipAddress: '',
        subnetMask: 24,
        gateway: '',
        isPrimary: false,
        isActive: true,
        types: [],
        vlanId: '',
        macAddress: '',
        dnsServers: '',
        networkSegment: '',
        networkZone: '',
        assignmentMethod: 'STATIC',
        description: '',
        notes: '',
        tags: ''
      });
    }
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setEditingIp(null);
    setError(null);
  };

  const handleFormChange = (field: string, value: any) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
  };

  const handleTypesChange = (event: SelectChangeEvent<string[]>) => {
    const value = event.target.value;
    setFormData(prev => ({
      ...prev,
      types: typeof value === 'string' ? value.split(',') : value
    }));
  };

  const handleSave = async () => {
    console.log('handleSave called, formData:', formData);
    setError(null);
    try {
      let savedIp: IPAddress;
      
      if (editingIp?.id) {
        // Update existing IP
        const response = await axios.put(
          `http://localhost:8090/api/api/ip-addresses/${editingIp.id}`,
          formData
        );
        savedIp = response.data;
      } else {
        // Create new IP
        const response = await axios.post(
          'http://localhost:8090/api/api/ip-addresses',
          formData
        );
        savedIp = response.data;
        
        // Assign to device
        await axios.post(
          `http://localhost:8090/api/api/ip-addresses/${savedIp.id}/assign/${deviceType}/${deviceId}`
        );
      }
      
      // Update types if changed
      if (formData.types.length > 0) {
        await axios.put(
          `http://localhost:8090/api/api/ip-addresses/${savedIp.id}/types`,
          formData.types
        );
      }
      
      await fetchIpAddresses();
      handleCloseDialog();
    } catch (err: any) {
      console.error('Error saving IP address:', err);
      setError(err.response?.data?.message || 'Failed to save IP address');
    }
  };

  const handleDelete = async (ipId: number) => {
    if (!window.confirm('Are you sure you want to delete this IP address?')) {
      return;
    }
    
    try {
      await axios.delete(`http://localhost:8090/api/api/ip-addresses/${ipId}`);
      await fetchIpAddresses();
    } catch (err) {
      console.error('Error deleting IP address:', err);
      setError('Failed to delete IP address');
    }
  };

  const handleSetPrimary = async (ipId: number) => {
    try {
      await axios.post(
        `http://localhost:8090/api/api/ip-addresses/${ipId}/set-primary/${deviceType}/${deviceId}`
      );
      await fetchIpAddresses();
    } catch (err) {
      console.error('Error setting primary IP:', err);
      setError('Failed to set primary IP address');
    }
  };

  const getTypeColor = (type: string): "default" | "primary" | "secondary" | "error" | "info" | "success" | "warning" => {
    const colorMap: Record<string, any> = {
      'PUBLIC': 'error',
      'PRIVATE': 'primary',
      'MANAGEMENT': 'warning',
      'VPN': 'secondary',
      'MONITORING': 'info',
      'BACKUP': 'success',
      'CLUSTER': 'default'
    };
    return colorMap[type] || 'default';
  };

  const getAssignmentMethodColor = (method: string): "default" | "primary" | "secondary" => {
    const colorMap: Record<string, any> = {
      'STATIC': 'primary',
      'DHCP': 'secondary',
      'RESERVED': 'default'
    };
    return colorMap[method] || 'default';
  };

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h6">
          IP Addresses for {deviceName}
        </Typography>
        {!readOnly && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => handleOpenDialog()}
            size="small"
          >
            Add IP Address
          </Button>
        )}
      </Box>

      {error && (
        <Alert severity="error" onClose={() => setError(null)} sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Primary</TableCell>
              <TableCell>IP Address</TableCell>
              <TableCell>Subnet</TableCell>
              <TableCell>Gateway</TableCell>
              <TableCell>Types</TableCell>
              <TableCell>Assignment</TableCell>
              <TableCell>VLAN</TableCell>
              <TableCell>Status</TableCell>
              {!readOnly && <TableCell align="right">Actions</TableCell>}
            </TableRow>
          </TableHead>
          <TableBody>
            {ipAddresses.length === 0 ? (
              <TableRow>
                <TableCell colSpan={readOnly ? 8 : 9} align="center">
                  No IP addresses configured
                </TableCell>
              </TableRow>
            ) : (
              ipAddresses.map((ip) => (
                <TableRow key={ip.id}>
                  <TableCell>
                    <IconButton
                      size="small"
                      onClick={() => ip.id && handleSetPrimary(ip.id)}
                      disabled={readOnly || ip.isPrimary}
                    >
                      {ip.isPrimary ? <StarIcon color="warning" /> : <StarBorderIcon />}
                    </IconButton>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" fontWeight={ip.isPrimary ? 'bold' : 'normal'}>
                      {ip.ipAddress}/{ip.subnetMask}
                    </Typography>
                  </TableCell>
                  <TableCell>{ip.subnetMask}</TableCell>
                  <TableCell>{ip.gateway || '-'}</TableCell>
                  <TableCell>
                    <Stack direction="row" spacing={0.5} flexWrap="wrap">
                      {ip.types.map((type) => (
                        <Chip
                          key={type}
                          label={type}
                          size="small"
                          color={getTypeColor(type)}
                        />
                      ))}
                    </Stack>
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={ip.assignmentMethod}
                      size="small"
                      color={getAssignmentMethodColor(ip.assignmentMethod)}
                    />
                  </TableCell>
                  <TableCell>{ip.vlanId || '-'}</TableCell>
                  <TableCell>
                    <Chip
                      label={ip.isActive ? 'Active' : 'Inactive'}
                      size="small"
                      color={ip.isActive ? 'success' : 'default'}
                    />
                  </TableCell>
                  {!readOnly && (
                    <TableCell align="right">
                      <Stack direction="row" spacing={0.5} justifyContent="flex-end">
                        <Tooltip title="Edit">
                          <IconButton
                            size="small"
                            onClick={() => handleOpenDialog(ip)}
                          >
                            <EditIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Delete">
                          <IconButton
                            size="small"
                            onClick={() => ip.id && handleDelete(ip.id)}
                            color="error"
                          >
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      </Stack>
                    </TableCell>
                  )}
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Add/Edit Dialog */}
      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="md" fullWidth>
        <DialogTitle>
          {editingIp ? 'Edit IP Address' : 'Add New IP Address'}
        </DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="IP Address"
                value={formData.ipAddress}
                onChange={(e) => handleFormChange('ipAddress', e.target.value)}
                required
                placeholder="192.168.1.100"
              />
            </Grid>
            <Grid item xs={12} md={3}>
              <TextField
                fullWidth
                label="Subnet Mask"
                type="number"
                value={formData.subnetMask}
                onChange={(e) => handleFormChange('subnetMask', parseInt(e.target.value))}
                inputProps={{ min: 0, max: 32 }}
                required
              />
            </Grid>
            <Grid item xs={12} md={3}>
              <TextField
                fullWidth
                label="Gateway"
                value={formData.gateway}
                onChange={(e) => handleFormChange('gateway', e.target.value)}
                placeholder="192.168.1.1"
              />
            </Grid>
            
            <Grid item xs={12} md={6}>
              <FormControl fullWidth>
                <InputLabel>IP Types</InputLabel>
                <Select
                  multiple
                  value={formData.types}
                  onChange={handleTypesChange}
                  input={<OutlinedInput label="IP Types" />}
                  renderValue={(selected) => (
                    <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                      {selected.map((value) => (
                        <Chip key={value} label={value} size="small" />
                      ))}
                    </Box>
                  )}
                >
                  {ipTypes.map((type) => (
                    <MenuItem key={type.value} value={type.value}>
                      <Checkbox checked={formData.types.indexOf(type.value) > -1} />
                      <ListItemText primary={type.label} />
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            
            <Grid item xs={12} md={6}>
              <FormControl fullWidth>
                <InputLabel>Assignment Method</InputLabel>
                <Select
                  value={formData.assignmentMethod}
                  onChange={(e) => handleFormChange('assignmentMethod', e.target.value)}
                  label="Assignment Method"
                >
                  {assignmentMethods.map((method) => (
                    <MenuItem key={method.value} value={method.value}>
                      {method.label}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                label="VLAN ID"
                value={formData.vlanId}
                onChange={(e) => handleFormChange('vlanId', e.target.value)}
              />
            </Grid>
            
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                label="MAC Address"
                value={formData.macAddress}
                onChange={(e) => handleFormChange('macAddress', e.target.value)}
                placeholder="00:11:22:33:44:55"
              />
            </Grid>
            
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                label="Network Zone"
                value={formData.networkZone}
                onChange={(e) => handleFormChange('networkZone', e.target.value)}
                placeholder="DMZ, Internal, External"
              />
            </Grid>
            
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="DNS Servers"
                value={formData.dnsServers}
                onChange={(e) => handleFormChange('dnsServers', e.target.value)}
                placeholder="8.8.8.8, 8.8.4.4"
              />
            </Grid>
            
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Network Segment"
                value={formData.networkSegment}
                onChange={(e) => handleFormChange('networkSegment', e.target.value)}
              />
            </Grid>
            
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Description"
                value={formData.description}
                onChange={(e) => handleFormChange('description', e.target.value)}
                multiline
                rows={2}
              />
            </Grid>
            
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Tags"
                value={formData.tags}
                onChange={(e) => handleFormChange('tags', e.target.value)}
                placeholder="production, web-server, frontend"
              />
            </Grid>
            
            <Grid item xs={12} md={3}>
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.isPrimary}
                    onChange={(e) => handleFormChange('isPrimary', e.target.checked)}
                  />
                }
                label="Primary IP"
              />
            </Grid>
            
            <Grid item xs={12} md={3}>
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.isActive}
                    onChange={(e) => handleFormChange('isActive', e.target.checked)}
                  />
                }
                label="Active"
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button onClick={() => { console.log('Add button clicked'); handleSave(); }} variant="contained">
            {editingIp ? 'Update' : 'Add'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default IPAddressManager;