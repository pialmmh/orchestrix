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
  Avatar,
  InputAdornment,
} from '@mui/material';
import {
  Add,
  Edit,
  Delete,
  Public,
  Search,
  Flag,
  LocationOn,
} from '@mui/icons-material';
import axios from 'axios';

interface Country {
  code: string;
  name: string;
  region: string;
  code3: string;
}

const Countries: React.FC = () => {
  const [countries, setCountries] = useState<Country[]>([]);
  const [filteredCountries, setFilteredCountries] = useState<Country[]>([]);
  const [loading, setLoading] = useState(true);
  const [openDialog, setOpenDialog] = useState(false);
  const [selectedCountry, setSelectedCountry] = useState<Country | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [formData, setFormData] = useState({
    code: '',
    name: '',
    region: 'Europe',
    code3: '',
  });

  const regions = [
    'North America',
    'South America',
    'Europe',
    'Asia',
    'Africa',
    'Oceania',
    'Middle East',
    'Europe/Asia',
  ];

  useEffect(() => {
    fetchCountries();
  }, []);

  useEffect(() => {
    const filtered = countries.filter(
      (country) =>
        country.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        country.code.toLowerCase().includes(searchTerm.toLowerCase()) ||
        country.region.toLowerCase().includes(searchTerm.toLowerCase())
    );
    setFilteredCountries(filtered);
  }, [searchTerm, countries]);

  const fetchCountries = async () => {
    try {
      const response = await axios.get('/api/api/countries');
      const countryData = response.data.countries || [];
      setCountries(countryData);
      setFilteredCountries(countryData);
    } catch (error) {
      console.error('Error fetching countries:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateCountry = () => {
    setFormData({
      code: '',
      name: '',
      region: 'Europe',
      code3: '',
    });
    setSelectedCountry(null);
    setOpenDialog(true);
  };

  const handleEditCountry = (country: Country) => {
    setFormData({
      code: country.code,
      name: country.name,
      region: country.region,
      code3: country.code3,
    });
    setSelectedCountry(country);
    setOpenDialog(true);
  };

  const handleDeleteCountry = (code: string) => {
    setCountries(countries.filter(c => c.code !== code));
    setFilteredCountries(filteredCountries.filter(c => c.code !== code));
  };

  const handleSubmit = () => {
    setOpenDialog(false);
    fetchCountries();
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const getRegionColor = (region: string) => {
    const colors: { [key: string]: string } = {
      'North America': '#1976d2',
      'South America': '#388e3c',
      'Europe': '#7b1fa2',
      'Asia': '#d32f2f',
      'Africa': '#f57c00',
      'Oceania': '#0097a7',
      'Middle East': '#5d4037',
      'Europe/Asia': '#455a64',
    };
    return colors[region] || '#666';
  };

  const getRegionStats = () => {
    const stats: { [key: string]: number } = {};
    countries.forEach((country) => {
      stats[country.region] = (stats[country.region] || 0) + 1;
    });
    return stats;
  };

  const regionStats = getRegionStats();

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
        <Typography>Loading countries...</Typography>
      </Box>
    );
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Countries</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={handleCreateCountry}>
          Add Country
        </Button>
      </Box>

      {/* Summary Cards */}
      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 3, mb: 3 }}>
        <Box sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(33.333% - 16px)' } }}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                <Avatar sx={{ bgcolor: 'primary.main', mr: 2 }}>
                  <Public />
                </Avatar>
                <Box>
                  <Typography variant="h5">{countries.length}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    Total Countries
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Box>
        <Box sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(33.333% - 16px)' } }}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                <Avatar sx={{ bgcolor: 'success.main', mr: 2 }}>
                  <LocationOn />
                </Avatar>
                <Box>
                  <Typography variant="h5">{Object.keys(regionStats).length}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    Regions
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Box>
        <Box sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(33.333% - 16px)' } }}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                <Avatar sx={{ bgcolor: 'info.main', mr: 2 }}>
                  <Flag />
                </Avatar>
                <Box>
                  <Typography variant="h5">
                    {Math.max(...Object.values(regionStats))}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Max Countries per Region
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Box>
      </Box>

      {/* Search Bar */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <TextField
            fullWidth
            placeholder="Search countries by name, code, or region..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <Search />
                </InputAdornment>
              ),
            }}
          />
        </CardContent>
      </Card>

      {/* Countries Table */}
      <Card>
        <CardContent>
          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Code</TableCell>
                  <TableCell>Country Name</TableCell>
                  <TableCell>ISO3 Code</TableCell>
                  <TableCell>Region</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {filteredCountries
                  .sort((a, b) => a.name.localeCompare(b.name))
                  .map((country) => (
                    <TableRow key={country.code} hover>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <Avatar
                            sx={{
                              width: 32,
                              height: 32,
                              bgcolor: 'background.default',
                              border: '1px solid #e0e0e0',
                              fontSize: '0.875rem',
                              fontWeight: 'bold',
                            }}
                          >
                            {country.code}
                          </Avatar>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body1" fontWeight="medium">
                          {country.name}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" fontFamily="monospace">
                          {country.code3}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={country.region}
                          size="small"
                          sx={{
                            bgcolor: getRegionColor(country.region),
                            color: 'white',
                          }}
                        />
                      </TableCell>
                      <TableCell align="right">
                        <Tooltip title="Edit">
                          <IconButton
                            size="small"
                            onClick={() => handleEditCountry(country)}
                          >
                            <Edit fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Delete">
                          <IconButton
                            size="small"
                            color="error"
                            onClick={() => handleDeleteCountry(country.code)}
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
          {selectedCountry ? 'Edit Country' : 'Add New Country'}
        </DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            <TextField
              fullWidth
              label="Country Code (2 letters)"
              name="code"
              value={formData.code}
              onChange={handleInputChange}
              required
              inputProps={{ maxLength: 2, style: { textTransform: 'uppercase' } }}
              placeholder="e.g., US, GB, DE"
            />
            <TextField
              fullWidth
              label="Country Name"
              name="name"
              value={formData.name}
              onChange={handleInputChange}
              required
              placeholder="e.g., United States, Germany"
            />
            <TextField
              fullWidth
              label="ISO3 Code (3 letters)"
              name="code3"
              value={formData.code3}
              onChange={handleInputChange}
              required
              inputProps={{ maxLength: 3, style: { textTransform: 'uppercase' } }}
              placeholder="e.g., USA, GBR, DEU"
            />
            <FormControl fullWidth>
              <InputLabel>Region</InputLabel>
              <Select
                name="region"
                value={formData.region}
                onChange={(e) => setFormData({ ...formData, region: e.target.value })}
                label="Region"
              >
                {regions.map((region) => (
                  <MenuItem key={region} value={region}>
                    {region}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button onClick={handleSubmit} variant="contained">
            {selectedCountry ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Countries;