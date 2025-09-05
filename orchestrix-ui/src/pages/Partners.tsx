import React, { useState, useEffect } from 'react';
import axios from 'axios';
import {
  Box,
  Card,
  CardContent,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  Paper,
  Button,
  IconButton,
  Typography,
  Chip,
  TextField,
  InputAdornment,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Tooltip,
  Alert,
} from '@mui/material';
import {
  Add,
  Edit,
  Delete,
  Search,
  Refresh,
  Business,
  Phone,
  Email,
  LocationOn,
} from '@mui/icons-material';

interface Partner {
  id: number;
  name: string;
  displayName: string;
  type: string;
  roles?: string[];
  status?: string;
  contactPerson?: string;
  contactEmail?: string;
  contactPhone?: string;
  address?: string;
  createdAt?: string;
  updatedAt?: string;
}

const Partners: React.FC = () => {
  const [partners, setPartners] = useState<Partner[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(20);
  const [searchTerm, setSearchTerm] = useState('');
  const [openDialog, setOpenDialog] = useState(false);
  const [selectedPartner, setSelectedPartner] = useState<Partner | null>(null);
  const [formData, setFormData] = useState({
    displayName: '',
    name: '',
    type: 'vendor',
    roles: [] as string[],
    contactPerson: '',
    contactEmail: '',
    contactPhone: '',
    address: '',
  });

  useEffect(() => {
    fetchPartners();
  }, []);

  const fetchPartners = async () => {
    try {
      setLoading(true);
      const response = await axios.get('/api/partners');
      setPartners(response.data.partners || []);
      setError(null);
    } catch (error) {
      console.error('Error fetching partners:', error);
      setError('Failed to fetch partners');
    } finally {
      setLoading(false);
    }
  };

  const handleChangePage = (event: unknown, newPage: number) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
  };

  const handleCreatePartner = () => {
    setFormData({
      displayName: '',
      name: '',
      type: 'vendor',
      roles: [],
      contactPerson: '',
      contactEmail: '',
      contactPhone: '',
      address: '',
    });
    setSelectedPartner(null);
    setOpenDialog(true);
  };

  const handleEditPartner = (partner: Partner) => {
    setFormData({
      displayName: partner.displayName,
      name: partner.name,
      type: partner.type,
      roles: partner.roles || [],
      contactPerson: partner.contactPerson || '',
      contactEmail: partner.contactEmail || '',
      contactPhone: partner.contactPhone || '',
      address: partner.address || '',
    });
    setSelectedPartner(partner);
    setOpenDialog(true);
  };

  const handleDeletePartner = async (id: number) => {
    try {
      await axios.delete(`/api/partners/${id}`);
      fetchPartners();
    } catch (error) {
      console.error('Error deleting partner:', error);
      setError('Failed to delete partner');
    }
  };

  const handleSubmit = async () => {
    try {
      const payload = {
        ...formData,
        name: formData.name || formData.displayName.toLowerCase().replace(/\s+/g, '_'),
      };
      
      if (selectedPartner) {
        await axios.put(`/api/partners/${selectedPartner.id}`, payload);
      } else {
        await axios.post('/api/partners', payload);
      }
      setOpenDialog(false);
      fetchPartners();
    } catch (error: any) {
      console.error('Error saving partner:', error);
      setError(error.response?.data?.error || 'Failed to save partner');
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const getStatusColor = (status: string) => {
    return status === 'Active' ? 'success' : 'default';
  };

  const getTypeColor = (type: string) => {
    switch (type) {
      case 'vendor':
        return 'primary';
      case 'customer':
        return 'secondary';
      case 'self':
        return 'info';
      default:
        return 'default';
    }
  };
  
  const getRolesDisplay = (roles?: string[]) => {
    if (!roles || roles.length === 0) return '-';
    return roles.join(', ');
  };

  const filteredPartners = partners.filter(
    (partner) =>
      partner.displayName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      partner.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (partner.contactPerson && partner.contactPerson.toLowerCase().includes(searchTerm.toLowerCase())) ||
      (partner.contactEmail && partner.contactEmail.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Partner Management
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
            <TextField
              placeholder="Search partners..."
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
                onClick={handleCreatePartner}
                sx={{ mr: 1 }}
              >
                Add Partner
              </Button>
              <IconButton onClick={fetchPartners}>
                <Refresh />
              </IconButton>
            </Box>
          </Box>

          <TableContainer component={Paper}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Partner</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Roles</TableCell>
                  <TableCell>Contact Person</TableCell>
                  <TableCell>Email</TableCell>
                  <TableCell>Phone</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {filteredPartners
                  .slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage)
                  .map((partner) => (
                    <TableRow key={partner.id} hover>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center' }}>
                          <Business sx={{ mr: 2, color: 'text.secondary' }} />
                          <Box>
                            <Typography variant="body1">{partner.displayName}</Typography>
                            <Typography variant="caption" color="text.secondary">
                              {partner.address || partner.name}
                            </Typography>
                          </Box>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={partner.type}
                          color={getTypeColor(partner.type) as any}
                          size="small"
                        />
                      </TableCell>
                      <TableCell>{getRolesDisplay(partner.roles)}</TableCell>
                      <TableCell>{partner.contactPerson || '-'}</TableCell>
                      <TableCell>{partner.contactEmail || '-'}</TableCell>
                      <TableCell>{partner.contactPhone || '-'}</TableCell>
                      <TableCell align="right">
                        <Tooltip title="Edit">
                          <IconButton
                            size="small"
                            onClick={() => handleEditPartner(partner)}
                          >
                            <Edit fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Delete">
                          <IconButton
                            size="small"
                            color="error"
                            onClick={() => handleDeletePartner(partner.id)}
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

          <TablePagination
            rowsPerPageOptions={[10, 20, 50]}
            component="div"
            count={filteredPartners.length}
            rowsPerPage={rowsPerPage}
            page={page}
            onPageChange={handleChangePage}
            onRowsPerPageChange={handleChangeRowsPerPage}
          />
        </CardContent>
      </Card>

      {/* Create/Edit Partner Dialog */}
      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="md" fullWidth>
        <DialogTitle>
          {selectedPartner ? 'Edit Partner' : 'Create New Partner'}
        </DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2, mt: 1 }}>
            <Box sx={{ flex: '0 0 100%' }}>
              <TextField
                fullWidth
                label="Display Name"
                name="displayName"
                value={formData.displayName}
                onChange={handleInputChange}
                required
                helperText="The name that will be displayed in the UI"
              />
            </Box>
            <Box sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(50% - 8px)' } }}>
              <TextField
                fullWidth
                label="System Name"
                name="name"
                value={formData.name}
                onChange={handleInputChange}
                helperText="Leave empty to auto-generate from display name"
              />
            </Box>
            <Box sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(50% - 8px)' } }}>
              <FormControl fullWidth>
                <InputLabel>Partner Type</InputLabel>
                <Select
                  name="type"
                  value={formData.type}
                  onChange={(e) =>
                    setFormData({ ...formData, type: e.target.value })
                  }
                  label="Partner Type"
                >
                  <MenuItem value="vendor">Vendor</MenuItem>
                  <MenuItem value="customer">Customer</MenuItem>
                  <MenuItem value="self">Self (Own Company)</MenuItem>
                </Select>
              </FormControl>
            </Box>
            <Box sx={{ flex: '0 0 100%' }}>
              <FormControl fullWidth>
                <InputLabel>Roles</InputLabel>
                <Select
                  multiple
                  value={formData.roles}
                  onChange={(e) =>
                    setFormData({ ...formData, roles: e.target.value as string[] })
                  }
                  label="Roles"
                  renderValue={(selected) => (selected as string[]).join(', ')}
                  disabled={!!(selectedPartner && selectedPartner.roles && selectedPartner.roles.includes('self'))}
                >
                  <MenuItem value="vendor">Vendor</MenuItem>
                  <MenuItem value="customer">Customer</MenuItem>
                  <MenuItem value="self">Self</MenuItem>
                  <MenuItem value="partner">Partner</MenuItem>
                </Select>
                {selectedPartner && selectedPartner.roles && selectedPartner.roles.includes('self') && (
                  <Typography variant="caption" color="text.secondary" sx={{ mt: 1 }}>
                    The 'self' role cannot be changed once assigned
                  </Typography>
                )}
              </FormControl>
            </Box>
            <Box sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(50% - 8px)' } }}>
              <TextField
                fullWidth
                label="Contact Person"
                name="contactPerson"
                value={formData.contactPerson}
                onChange={handleInputChange}
              />
            </Box>
            <Box sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(50% - 8px)' } }}>
              <TextField
                fullWidth
                label="Contact Email"
                name="contactEmail"
                type="email"
                value={formData.contactEmail}
                onChange={handleInputChange}
              />
            </Box>
            <Box sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(50% - 8px)' } }}>
              <TextField
                fullWidth
                label="Contact Phone"
                name="contactPhone"
                value={formData.contactPhone}
                onChange={handleInputChange}
              />
            </Box>
            <Box sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(50% - 8px)' } }}>
              <TextField
                fullWidth
                label="Address"
                name="address"
                value={formData.address}
                onChange={handleInputChange}
                multiline
                rows={2}
              />
            </Box>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button onClick={handleSubmit} variant="contained">
            {selectedPartner ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Partners;