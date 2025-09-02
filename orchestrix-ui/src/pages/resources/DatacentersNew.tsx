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
  FormControlLabel,
  Switch,
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

interface Country {
  id: number;
  code: string;
  code3: string;
  name: string;
  region: string;
  hasStates: boolean;
}

interface State {
  id: number;
  code: string;
  name: string;
  countryId: number;
}

interface City {
  id: number;
  name: string;
  countryId: number;
  stateId?: number;
  isCapital: boolean;
}

interface Partner {
  id: number;
  name: string;
  displayName: string;
  status: string;
}

interface Datacenter {
  id: number;
  name: string;
  countryId?: number;
  countryCode?: string;
  countryName?: string;
  stateId?: number;
  stateName?: string;
  cityId?: number;
  cityName?: string;
  locationOther?: string;
  type: string;
  status: string;
  provider: string;
  partnerId?: number;
  partnerName?: string;
  partnerDisplayName?: string;
  servers: number;
  storageTb: number;
  utilization: number;
}

const DatacentersNew: React.FC = () => {
  const [datacenters, setDatacenters] = useState<Datacenter[]>([]);
  const [countries, setCountries] = useState<Country[]>([]);
  const [states, setStates] = useState<State[]>([]);
  const [cities, setCities] = useState<City[]>([]);
  const [partners, setPartners] = useState<Partner[]>([]);
  const [loading, setLoading] = useState(true);
  const [openDialog, setOpenDialog] = useState(false);
  const [selectedDatacenter, setSelectedDatacenter] = useState<Datacenter | null>(null);
  const [useOtherLocation, setUseOtherLocation] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    countryId: '',
    stateId: '',
    cityId: '',
    locationOther: '',
    type: 'Primary',
    provider: 'AWS',
    partnerId: '',
    servers: 0,
    storageTb: 0,
    utilization: 0,
  });

  useEffect(() => {
    fetchDatacenters();
    fetchCountries();
    fetchPartners();
  }, []);

  useEffect(() => {
    if (formData.countryId) {
      fetchStates(formData.countryId);
      fetchCities(formData.countryId);
    } else {
      setStates([]);
      setCities([]);
    }
  }, [formData.countryId]);

  useEffect(() => {
    if (formData.stateId && formData.countryId) {
      fetchCitiesByState(formData.stateId);
    }
  }, [formData.stateId]);

  const fetchDatacenters = async () => {
    try {
      const response = await axios.get('/api/locations/datacenters');
      setDatacenters(response.data.datacenters || []);
    } catch (error) {
      console.error('Error fetching datacenters:', error);
    } finally {
      setLoading(false);
    }
  };

  const fetchCountries = async () => {
    try {
      const response = await axios.get('/api/locations/countries');
      setCountries(response.data.countries || []);
    } catch (error) {
      console.error('Error fetching countries:', error);
    }
  };

  const fetchStates = async (countryId: string) => {
    try {
      const country = countries.find(c => c.id === parseInt(countryId));
      if (!country || !country.hasStates) {
        setStates([]);
        return;
      }
      const response = await axios.get(`/api/locations/countries/${country.code}/states`);
      setStates(response.data.states || []);
    } catch (error) {
      console.error('Error fetching states:', error);
      setStates([]);
    }
  };

  const fetchCities = async (countryId: string) => {
    try {
      const country = countries.find(c => c.id === parseInt(countryId));
      if (!country) return;
      const response = await axios.get(`/api/locations/countries/${country.code}/cities`);
      setCities(response.data.cities || []);
    } catch (error) {
      console.error('Error fetching cities:', error);
      setCities([]);
    }
  };

  const fetchCitiesByState = async (stateId: string) => {
    try {
      const response = await axios.get(`/api/locations/states/${stateId}/cities`);
      setCities(response.data.cities || []);
    } catch (error) {
      console.error('Error fetching cities by state:', error);
    }
  };

  const fetchPartners = async () => {
    try {
      const response = await axios.get('/api/partners/cloud-providers');
      setPartners(response.data.providers || []);
    } catch (error) {
      console.error('Error fetching partners:', error);
    }
  };

  const handleCreateDatacenter = () => {
    setFormData({
      name: '',
      countryId: '',
      stateId: '',
      cityId: '',
      locationOther: '',
      type: 'Primary',
      provider: 'AWS',
      partnerId: '',
      servers: 0,
      storageTb: 0,
      utilization: 0,
    });
    setSelectedDatacenter(null);
    setUseOtherLocation(false);
    setOpenDialog(true);
  };

  const handleEditDatacenter = (datacenter: Datacenter) => {
    setFormData({
      name: datacenter.name,
      countryId: datacenter.countryId?.toString() || '',
      stateId: datacenter.stateId?.toString() || '',
      cityId: datacenter.cityId?.toString() || '',
      locationOther: datacenter.locationOther || '',
      type: datacenter.type,
      provider: datacenter.provider,
      partnerId: datacenter.partnerId?.toString() || '',
      servers: datacenter.servers,
      storageTb: datacenter.storageTb,
      utilization: datacenter.utilization,
    });
    setSelectedDatacenter(datacenter);
    setUseOtherLocation(!!datacenter.locationOther && !datacenter.cityId);
    setOpenDialog(true);
  };

  const handleDeleteDatacenter = async (id: number) => {
    try {
      await axios.delete(`/api/locations/datacenters/${id}`);
      fetchDatacenters();
    } catch (error) {
      console.error('Error deleting datacenter:', error);
    }
  };

  const handleSubmit = async () => {
    try {
      const payload: any = {
        name: formData.name,
        type: formData.type,
        provider: formData.provider,
        partnerId: formData.partnerId ? parseInt(formData.partnerId) : null,
        servers: formData.servers,
        storageTb: formData.storageTb,
        utilization: formData.utilization,
      };

      if (useOtherLocation) {
        payload.locationOther = formData.locationOther;
      } else {
        if (formData.countryId) payload.countryId = parseInt(formData.countryId);
        if (formData.stateId) payload.stateId = parseInt(formData.stateId);
        if (formData.cityId) payload.cityId = parseInt(formData.cityId);
      }

      if (selectedDatacenter) {
        await axios.put(`/api/locations/datacenters/${selectedDatacenter.id}`, payload);
      } else {
        await axios.post('/api/locations/datacenters', payload);
      }
      
      setOpenDialog(false);
      fetchDatacenters();
    } catch (error) {
      console.error('Error saving datacenter:', error);
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData({
      ...formData,
      [name]: name === 'servers' || name === 'storageTb' || name === 'utilization' 
        ? parseInt(value) || 0 
        : value,
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

  const getLocationDisplay = (datacenter: Datacenter) => {
    if (datacenter.locationOther) {
      return datacenter.locationOther;
    }
    const parts = [];
    if (datacenter.cityName) parts.push(datacenter.cityName);
    if (datacenter.stateName) parts.push(datacenter.stateName);
    if (datacenter.countryName) parts.push(datacenter.countryName);
    return parts.join(', ') || 'Unknown';
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
        <Typography>Loading datacenters...</Typography>
      </Box>
    );
  }

  const selectedCountry = countries.find(c => c.id === parseInt(formData.countryId));
  const showStateDropdown = selectedCountry?.hasStates && !useOtherLocation;

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
                  {datacenters.reduce((sum, dc) => sum + dc.storageTb, 0)}TB
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
                      <Typography variant="body2">
                        {getLocationDisplay(datacenter)}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={datacenter.type}
                        color={getTypeColor(datacenter.type) as any}
                        size="small"
                      />
                    </TableCell>
                    <TableCell>
                      {datacenter.partnerDisplayName || datacenter.provider}
                    </TableCell>
                    <TableCell>{datacenter.servers}</TableCell>
                    <TableCell>{datacenter.storageTb}TB</TableCell>
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

            <FormControlLabel
              control={
                <Switch
                  checked={useOtherLocation}
                  onChange={(e) => setUseOtherLocation(e.target.checked)}
                />
              }
              label="Enter custom location"
            />

            {useOtherLocation ? (
              <TextField
                fullWidth
                label="Location Description"
                name="locationOther"
                value={formData.locationOther}
                onChange={handleInputChange}
                placeholder="e.g., Cloud Region US-East-1, Private Facility"
                required
              />
            ) : (
              <>
                <FormControl fullWidth required>
                  <InputLabel>Country</InputLabel>
                  <Select
                    name="countryId"
                    value={formData.countryId}
                    onChange={(e) => setFormData({ 
                      ...formData, 
                      countryId: e.target.value as string,
                      stateId: '',
                      cityId: ''
                    })}
                    label="Country"
                  >
                    {countries
                      .sort((a, b) => a.name.localeCompare(b.name))
                      .map((country) => (
                        <MenuItem key={country.id} value={country.id}>
                          {country.name} ({country.region})
                        </MenuItem>
                      ))}
                  </Select>
                </FormControl>

                {showStateDropdown && (
                  <FormControl fullWidth>
                    <InputLabel>State/Province</InputLabel>
                    <Select
                      name="stateId"
                      value={formData.stateId}
                      onChange={(e) => setFormData({ 
                        ...formData, 
                        stateId: e.target.value as string,
                        cityId: ''
                      })}
                      label="State/Province"
                    >
                      <MenuItem value="">
                        <em>None</em>
                      </MenuItem>
                      {states.map((state) => (
                        <MenuItem key={state.id} value={state.id}>
                          {state.name}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                )}

                {formData.countryId && (
                  <FormControl fullWidth>
                    <InputLabel>City</InputLabel>
                    <Select
                      name="cityId"
                      value={formData.cityId}
                      onChange={(e) => setFormData({ ...formData, cityId: e.target.value as string })}
                      label="City"
                    >
                      <MenuItem value="">
                        <em>None</em>
                      </MenuItem>
                      {cities.map((city) => (
                        <MenuItem key={city.id} value={city.id}>
                          {city.name} {city.isCapital && '(Capital)'}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                )}
              </>
            )}

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
              <InputLabel>Cloud Provider Partner</InputLabel>
              <Select
                name="partnerId"
                value={formData.partnerId}
                onChange={(e) => setFormData({ ...formData, partnerId: e.target.value as string })}
                label="Cloud Provider Partner"
              >
                <MenuItem value="">
                  <em>None</em>
                </MenuItem>
                {partners.map((partner) => (
                  <MenuItem key={partner.id} value={partner.id}>
                    {partner.displayName}
                  </MenuItem>
                ))}
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

            <TextField
              fullWidth
              label="Number of Servers"
              name="servers"
              type="number"
              value={formData.servers}
              onChange={handleInputChange}
              InputProps={{ inputProps: { min: 0 } }}
            />

            <TextField
              fullWidth
              label="Storage (TB)"
              name="storageTb"
              type="number"
              value={formData.storageTb}
              onChange={handleInputChange}
              InputProps={{ inputProps: { min: 0 } }}
            />

            <TextField
              fullWidth
              label="Utilization (%)"
              name="utilization"
              type="number"
              value={formData.utilization}
              onChange={handleInputChange}
              InputProps={{ inputProps: { min: 0, max: 100 } }}
            />
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

export default DatacentersNew;