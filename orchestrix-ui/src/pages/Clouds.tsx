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
  Button,
  IconButton,
  Chip,
  TextField,
  InputAdornment,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Tooltip,
  Alert,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material';
import {
  Add,
  Edit,
  Delete,
  Search,
  Refresh,
  Cloud as CloudIcon,
  Business,
  LocationOn,
} from '@mui/icons-material';
import axios from 'axios';

interface Cloud {
  id: number;
  name: string;
  description?: string;
  clientName?: string;
  partnerId?: number;
  deploymentRegion?: string;
  status: string;
  createdAt?: string;
  updatedAt?: string;
  datacenterCount?: number;
  computeCount?: number;
  storageCount?: number;
}

interface Partner {
  id: number;
  name: string;
  displayName: string;
  type: string;
  roles: string[];
}

const Clouds: React.FC = () => {
  const [clouds, setClouds] = useState<Cloud[]>([]);
  const [partners, setPartners] = useState<Partner[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [openDialog, setOpenDialog] = useState(false);
  const [openDeleteDialog, setOpenDeleteDialog] = useState(false);
  const [selectedCloud, setSelectedCloud] = useState<Cloud | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    partnerId: null as number | null,
    deploymentRegion: '',
    status: 'ACTIVE',
  });

  useEffect(() => {
    fetchClouds();
    fetchPartners();
  }, []);

  const fetchClouds = async () => {
    try {
      setLoading(true);
      const response = await axios.get('/api/clouds');
      setClouds(response.data || []);
      setError(null);
    } catch (error) {
      console.error('Error fetching clouds:', error);
      setError('Failed to fetch clouds');
    } finally {
      setLoading(false);
    }
  };

  const fetchPartners = async () => {
    try {
      const response = await axios.get('/api/partners');
      // Get partners with 'customer' or 'self' roles
      const filteredPartners = response.data.partners.filter((p: Partner) => 
        p.roles && (p.roles.includes('customer') || p.roles.includes('self'))
      );
      setPartners(filteredPartners);
    } catch (error) {
      console.error('Error fetching partners:', error);
    }
  };

  const handleCreateCloud = () => {
    setFormData({
      name: '',
      description: '',
      partnerId: null,
      deploymentRegion: '',
      status: 'ACTIVE',
    });
    setSelectedCloud(null);
    setOpenDialog(true);
  };

  const handleEditCloud = (cloud: Cloud) => {
    setFormData({
      name: cloud.name,
      description: cloud.description || '',
      partnerId: cloud.partnerId || null,
      deploymentRegion: cloud.deploymentRegion || '',
      status: cloud.status,
    });
    setSelectedCloud(cloud);
    setOpenDialog(true);
  };

  const handleDeleteClick = (cloud: Cloud) => {
    setSelectedCloud(cloud);
    setOpenDeleteDialog(true);
  };

  const handleDeleteConfirm = async () => {
    if (!selectedCloud) return;
    
    try {
      await axios.delete(`/api/clouds/${selectedCloud.id}`);
      setOpenDeleteDialog(false);
      setSelectedCloud(null);
      fetchClouds();
    } catch (error) {
      console.error('Error deleting cloud:', error);
      setError('Failed to delete cloud');
    }
  };

  const handleSubmit = async () => {
    // Validate partner is selected
    if (!formData.partnerId) {
      setError('Partner selection is mandatory');
      return;
    }
    
    try {
      if (selectedCloud) {
        await axios.put(`/api/clouds/${selectedCloud.id}`, formData);
      } else {
        await axios.post('/api/clouds', formData);
      }
      setOpenDialog(false);
      fetchClouds();
    } catch (error: any) {
      console.error('Error saving cloud:', error);
      setError(error.response?.data || 'Failed to save cloud');
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return 'success';
      case 'INACTIVE':
        return 'default';
      case 'MAINTENANCE':
        return 'warning';
      case 'ERROR':
        return 'error';
      default:
        return 'default';
    }
  };

  const filteredClouds = clouds.filter(
    (cloud) =>
      cloud.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      cloud.description?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      cloud.clientName?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Clouds Management
      </Typography>
      <Typography variant="body2" color="text.secondary" gutterBottom>
        Manage cloud environments and their configurations
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Card sx={{ mt: 3 }}>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
            <TextField
              placeholder="Search clouds..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <Search />
                  </InputAdornment>
                ),
              }}
              sx={{ width: 300 }}
            />
            <Box>
              <Button
                variant="contained"
                startIcon={<Add />}
                onClick={handleCreateCloud}
                sx={{ mr: 1 }}
              >
                Add Cloud
              </Button>
              <IconButton onClick={fetchClouds}>
                <Refresh />
              </IconButton>
            </Box>
          </Box>

          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Cloud Name</TableCell>
                  <TableCell>Description</TableCell>
                  <TableCell>Client</TableCell>
                  <TableCell>Region</TableCell>
                  <TableCell align="center">Resources</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {filteredClouds.map((cloud) => (
                  <TableRow key={cloud.id} hover>
                    <TableCell>
                      <Box sx={{ display: 'flex', alignItems: 'center' }}>
                        <CloudIcon sx={{ mr: 1, color: 'text.secondary' }} />
                        <Typography variant="subtitle2">{cloud.name}</Typography>
                      </Box>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" color="text.secondary">
                        {cloud.description || '-'}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      {cloud.partnerId !== undefined ? 
                        partners.find(p => p.id === cloud.partnerId)?.displayName || '-' : 
                        cloud.clientName || '-'
                      }
                    </TableCell>
                    <TableCell>
                      {cloud.deploymentRegion && (
                        <Box sx={{ display: 'flex', alignItems: 'center' }}>
                          <LocationOn fontSize="small" sx={{ mr: 0.5 }} />
                          <Typography variant="body2">{cloud.deploymentRegion}</Typography>
                        </Box>
                      )}
                      {!cloud.deploymentRegion && '-'}
                    </TableCell>
                    <TableCell align="center">
                      <Box sx={{ display: 'flex', gap: 1, justifyContent: 'center' }}>
                        {cloud.datacenterCount !== undefined && cloud.datacenterCount > 0 && (
                          <Tooltip title="Datacenters">
                            <Chip
                              icon={<Business fontSize="small" />}
                              label={cloud.datacenterCount}
                              size="small"
                              variant="outlined"
                            />
                          </Tooltip>
                        )}
                        {cloud.computeCount !== undefined && cloud.computeCount > 0 && (
                          <Tooltip title="Compute Resources">
                            <Chip
                              label={`${cloud.computeCount} Compute`}
                              size="small"
                              variant="outlined"
                            />
                          </Tooltip>
                        )}
                      </Box>
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={cloud.status}
                        color={getStatusColor(cloud.status) as any}
                        size="small"
                      />
                    </TableCell>
                    <TableCell align="right">
                      <Tooltip title="Edit">
                        <IconButton
                          size="small"
                          onClick={() => handleEditCloud(cloud)}
                        >
                          <Edit fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Delete">
                        <IconButton
                          size="small"
                          color="error"
                          onClick={() => handleDeleteClick(cloud)}
                        >
                          <Delete fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                ))}
                {filteredClouds.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={7} align="center" sx={{ py: 3 }}>
                      <Typography variant="body2" color="text.secondary">
                        {searchTerm ? 'No clouds found matching your search' : 'No clouds found'}
                      </Typography>
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>

      {/* Create/Edit Dialog */}
      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{selectedCloud ? 'Edit Cloud' : 'Create New Cloud'}</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 1 }}>
            <TextField
              fullWidth
              label="Cloud Name"
              name="name"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              required
            />
            <TextField
              fullWidth
              label="Description"
              name="description"
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              multiline
              rows={2}
            />
            <FormControl fullWidth required>
              <InputLabel>Partner/Client *</InputLabel>
              <Select
                value={formData.partnerId || ''}
                onChange={(e) => setFormData({ ...formData, partnerId: e.target.value as number })}
                label="Partner/Client *"
                error={!formData.partnerId}
              >
                {partners.map((partner) => (
                  <MenuItem key={partner.id} value={partner.id}>
                    {partner.displayName}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              fullWidth
              label="Deployment Region"
              name="deploymentRegion"
              value={formData.deploymentRegion}
              onChange={(e) => setFormData({ ...formData, deploymentRegion: e.target.value })}
              placeholder="e.g., US-East, Europe-West, Asia-Pacific"
            />
            <FormControl fullWidth>
              <InputLabel>Status</InputLabel>
              <Select
                value={formData.status}
                onChange={(e) => setFormData({ ...formData, status: e.target.value })}
                label="Status"
              >
                <MenuItem value="ACTIVE">Active</MenuItem>
                <MenuItem value="INACTIVE">Inactive</MenuItem>
                <MenuItem value="MAINTENANCE">Maintenance</MenuItem>
                <MenuItem value="ERROR">Error</MenuItem>
              </Select>
            </FormControl>
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button onClick={handleSubmit} variant="contained">
            {selectedCloud ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog open={openDeleteDialog} onClose={() => setOpenDeleteDialog(false)}>
        <DialogTitle>Confirm Delete</DialogTitle>
        <DialogContent>
          <Typography>
            Are you sure you want to delete cloud "{selectedCloud?.name}"? This action
            cannot be undone and will remove all associated resources.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDeleteDialog(false)}>Cancel</Button>
          <Button onClick={handleDeleteConfirm} color="error" variant="contained">
            Delete
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Clouds;