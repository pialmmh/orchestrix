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
} from '@mui/material';
import {
  Add,
  Edit,
  Delete,
  LocationOn,
  Storage,
  Dns,
  Cloud,
  Warning,
  CheckCircle,
} from '@mui/icons-material';
import axios from 'axios';

interface Datacenter {
  id: string;
  name: string;
  location: string;
  country: string;
  type: string;
  status: string;
  provider: string;
  coordinates: string;
  servers: number;
  storage: string;
  utilization: number;
}

interface Country {
  code: string;
  name: string;
  region: string;
  code3: string;
}

const Datacenters: React.FC = () => {
  const [datacenters, setDatacenters] = useState<Datacenter[]>([]);
  const [countries, setCountries] = useState<Country[]>([]);
  const [loading, setLoading] = useState(true);
  const [countriesLoading, setCountriesLoading] = useState(true);
  const [openDialog, setOpenDialog] = useState(false);
  const [selectedDatacenter, setSelectedDatacenter] = useState<Datacenter | null>(null);
  const [formData, setFormData] = useState({
    name: '',
    location: '',
    country: '',
    type: 'Primary',
    provider: 'AWS',
  });

  useEffect(() => {
    fetchDatacenters();
    fetchCountries();
  }, []);

  const fetchDatacenters = async () => {
    try {
      const response = await axios.get('/api/api/resources/datacenters');
      setDatacenters(response.data.datacenters || []);
    } catch (error) {
      console.error('Error fetching datacenters:', error);
    } finally {
      setLoading(false);
    }
  };

  const fetchCountries = async () => {
    try {
      const response = await axios.get('/api/api/countries');
      const countriesData = response.data.countries || [];
      setCountries(countriesData);
      return countriesData;
    } catch (error) {
      console.error('Error fetching countries:', error);
      return [];
    } finally {
      setCountriesLoading(false);
    }
  };

  const handleCreateDatacenter = () => {
    setFormData({
      name: '',
      location: '',
      country: '',
      type: 'Primary',
      provider: 'AWS',
    });
    setSelectedDatacenter(null);
    setOpenDialog(true);
  };

  const handleEditDatacenter = (datacenter: Datacenter) => {
    setFormData({
      name: datacenter.name,
      location: datacenter.location,
      country: datacenter.country,
      type: datacenter.type,
      provider: datacenter.provider,
    });
    setSelectedDatacenter(datacenter);
    setOpenDialog(true);
  };

  const handleDeleteDatacenter = (id: string) => {
    setDatacenters(datacenters.filter(dc => dc.id !== id));
  };

  const handleSubmit = () => {
    // In a real app, this would make an API call
    setOpenDialog(false);
    fetchDatacenters();
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
      case 'MAINTENANCE': return 'warning';
      case 'INACTIVE': return 'error';
      default: return 'default';
    }
  };

  const getTypeColor = (type: string) => {
    switch (type) {
      case 'Primary': return 'primary';
      case 'Secondary': return 'secondary';
      case 'Backup': return 'info';
      case 'Regional': return 'warning';
      default: return 'default';
    }
  };

  const getUtilizationColor = (utilization: number) => {
    if (utilization < 60) return 'success';
    if (utilization < 80) return 'warning';
    return 'error';
  };

  const getCountryName = (code: string) => {
    const country = countries.find(c => c.code === code);
    return country ? country.name : code;
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
        <Typography>Loading datacenters...</Typography>
      </Box>
    );
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Datacenters</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={handleCreateDatacenter}>
          Add Datacenter
        </Button>
      </Box>

      {/* Summary Cards */}
      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 3, mb: 3 }}>
        <Box sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(50% - 12px)', md: '0 0 calc(25% - 18px)' } }}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                <LocationOn sx={{ mr: 1, color: 'primary.main' }} />
                <Typography variant="h6">{datacenters.length}</Typography>
              </Box>
              <Typography variant="body2" color="text.secondary">
                Total Datacenters
              </Typography>
            </CardContent>
          </Card>
        </Box>
        <Box sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(50% - 12px)', md: '0 0 calc(25% - 18px)' } }}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                <CheckCircle sx={{ mr: 1, color: 'success.main' }} />
                <Typography variant="h6">
                  {datacenters.filter(dc => dc.status === 'ACTIVE').length}
                </Typography>
              </Box>
              <Typography variant="body2" color="text.secondary">
                Active
              </Typography>
            </CardContent>
          </Card>
        </Box>
        <Box sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(50% - 12px)', md: '0 0 calc(25% - 18px)' } }}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                <Dns sx={{ mr: 1, color: 'info.main' }} />
                <Typography variant="h6">
                  {datacenters.reduce((sum, dc) => sum + dc.servers, 0)}
                </Typography>
              </Box>
              <Typography variant="body2" color="text.secondary">
                Total Servers
              </Typography>
            </CardContent>
          </Card>
        </Box>
        <Box sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(50% - 12px)', md: '0 0 calc(25% - 18px)' } }}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                <Storage sx={{ mr: 1, color: 'warning.main' }} />
                <Typography variant="h6">
                  {datacenters.reduce((sum, dc) => {
                    const storage = parseInt(dc.storage);
                    return sum + (isNaN(storage) ? 0 : storage);
                  }, 0)}TB
                </Typography>
              </Box>
              <Typography variant="body2" color="text.secondary">
                Total Storage
              </Typography>
            </CardContent>
          </Card>
        </Box>
      </Box>

      {/* Datacenters Table */}
      <Card>
        <CardContent>
          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Name</TableCell>
                  <TableCell>Location</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Provider</TableCell>
                  <TableCell>Servers</TableCell>
                  <TableCell>Storage</TableCell>
                  <TableCell>Utilization</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {datacenters.map((datacenter) => (
                  <TableRow key={datacenter.id} hover>
                    <TableCell>
                      <Box sx={{ display: 'flex', alignItems: 'center' }}>
                        <Cloud sx={{ mr: 1, color: 'text.secondary' }} />
                        <Typography variant="body1" fontWeight="medium">
                          {datacenter.name}
                        </Typography>
                      </Box>
                    </TableCell>
                    <TableCell>
                      <Box>
                        <Typography variant="body2">{datacenter.location}</Typography>
                        <Typography variant="caption" color="text.secondary">
                          {getCountryName(datacenter.country)}
                        </Typography>
                      </Box>
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={datacenter.type}
                        color={getTypeColor(datacenter.type) as any}
                        size="small"
                      />
                    </TableCell>
                    <TableCell>{datacenter.provider}</TableCell>
                    <TableCell>{datacenter.servers}</TableCell>
                    <TableCell>{datacenter.storage}</TableCell>
                    <TableCell>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <LinearProgress
                          variant="determinate"
                          value={datacenter.utilization}
                          color={getUtilizationColor(datacenter.utilization) as any}
                          sx={{ width: 60, height: 6, borderRadius: 3 }}
                        />
                        <Typography variant="body2">{datacenter.utilization}%</Typography>
                      </Box>
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={datacenter.status}
                        color={getStatusColor(datacenter.status) as any}
                        size="small"
                        icon={datacenter.status === 'MAINTENANCE' ? <Warning /> : undefined}
                      />
                    </TableCell>
                    <TableCell align="right">
                      <Tooltip title="View Location">
                        <IconButton size="small">
                          <LocationOn fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Edit">
                        <IconButton size="small" onClick={() => handleEditDatacenter(datacenter)}>
                          <Edit fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Delete">
                        <IconButton
                          size="small"
                          color="error"
                          onClick={() => handleDeleteDatacenter(datacenter.id)}
                        >
                          <Delete fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>

      {/* Create/Edit Dialog */}
      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>
          {selectedDatacenter ? 'Edit Datacenter' : 'Add New Datacenter'}
        </DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            <TextField
              fullWidth
              label="Datacenter Name"
              name="name"
              value={formData.name}
              onChange={handleInputChange}
              required
            />
            <TextField
              fullWidth
              label="Location/City"
              name="location"
              value={formData.location}
              onChange={handleInputChange}
              placeholder="e.g., Virginia, Frankfurt, Singapore"
              required
            />
            <FormControl fullWidth required>
              <InputLabel>Country</InputLabel>
              <Select
                name="country"
                value={formData.country}
                onChange={(e) => setFormData({ ...formData, country: e.target.value })}
                label="Country"
                disabled={countriesLoading}
              >
                {countriesLoading ? (
                  <MenuItem disabled>Loading countries...</MenuItem>
                ) : countries.length === 0 ? (
                  <MenuItem disabled>No countries available</MenuItem>
                ) : (
                  countries
                    .sort((a, b) => a.name.localeCompare(b.name))
                    .map((country) => (
                      <MenuItem key={country.code} value={country.code}>
                        {country.name} ({country.region})
                      </MenuItem>
                    ))
                )}
              </Select>
            </FormControl>
            <FormControl fullWidth>
              <InputLabel>Type</InputLabel>
              <Select
                name="type"
                value={formData.type}
                onChange={(e) => setFormData({ ...formData, type: e.target.value })}
                label="Type"
              >
                <MenuItem value="Primary">Primary</MenuItem>
                <MenuItem value="Secondary">Secondary</MenuItem>
                <MenuItem value="Backup">Backup</MenuItem>
                <MenuItem value="Regional">Regional</MenuItem>
              </Select>
            </FormControl>
            <FormControl fullWidth>
              <InputLabel>Provider</InputLabel>
              <Select
                name="provider"
                value={formData.provider}
                onChange={(e) => setFormData({ ...formData, provider: e.target.value })}
                label="Provider"
              >
                <MenuItem value="AWS">AWS</MenuItem>
                <MenuItem value="Azure">Azure</MenuItem>
                <MenuItem value="GCP">Google Cloud</MenuItem>
                <MenuItem value="On-Premise">On-Premise</MenuItem>
                <MenuItem value="Hybrid">Hybrid</MenuItem>
              </Select>
            </FormControl>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button onClick={handleSubmit} variant="contained">
            {selectedDatacenter ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Datacenters;