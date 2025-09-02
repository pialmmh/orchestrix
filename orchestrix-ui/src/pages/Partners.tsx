import React, { useState } from 'react';
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
  Star,
  StarBorder,
} from '@mui/icons-material';

interface Partner {
  id: string;
  name: string;
  type: string;
  status: string;
  contact: string;
  email: string;
  phone: string;
  address: string;
  rating: number;
  contractStart: string;
  contractEnd: string;
}

const Partners: React.FC = () => {
  const [partners, setPartners] = useState<Partner[]>([
    {
      id: '1',
      name: 'TechCorp Solutions',
      type: 'Technology Partner',
      status: 'Active',
      contact: 'John Smith',
      email: 'john@techcorp.com',
      phone: '+1 234-567-8900',
      address: '123 Tech Street, Silicon Valley, CA',
      rating: 5,
      contractStart: '2023-01-01',
      contractEnd: '2025-12-31',
    },
    {
      id: '2',
      name: 'Global Networks Inc',
      type: 'Infrastructure Partner',
      status: 'Active',
      contact: 'Sarah Johnson',
      email: 'sarah@globalnet.com',
      phone: '+1 234-567-8901',
      address: '456 Network Ave, New York, NY',
      rating: 4,
      contractStart: '2023-06-01',
      contractEnd: '2024-05-31',
    },
    {
      id: '3',
      name: 'CloudBase Systems',
      type: 'Cloud Partner',
      status: 'Active',
      contact: 'Mike Chen',
      email: 'mike@cloudbase.com',
      phone: '+1 234-567-8902',
      address: '789 Cloud Way, Seattle, WA',
      rating: 5,
      contractStart: '2022-01-01',
      contractEnd: '2024-12-31',
    },
    {
      id: '4',
      name: 'SecureNet Partners',
      type: 'Security Partner',
      status: 'Inactive',
      contact: 'Emily Brown',
      email: 'emily@securenet.com',
      phone: '+1 234-567-8903',
      address: '321 Security Blvd, Austin, TX',
      rating: 3,
      contractStart: '2021-01-01',
      contractEnd: '2023-12-31',
    },
  ]);

  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [searchTerm, setSearchTerm] = useState('');
  const [openDialog, setOpenDialog] = useState(false);
  const [selectedPartner, setSelectedPartner] = useState<Partner | null>(null);
  const [formData, setFormData] = useState({
    name: '',
    type: 'Technology Partner',
    contact: '',
    email: '',
    phone: '',
    address: '',
    contractStart: '',
    contractEnd: '',
  });

  const handleChangePage = (event: unknown, newPage: number) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
  };

  const handleCreatePartner = () => {
    setFormData({
      name: '',
      type: 'Technology Partner',
      contact: '',
      email: '',
      phone: '',
      address: '',
      contractStart: '',
      contractEnd: '',
    });
    setSelectedPartner(null);
    setOpenDialog(true);
  };

  const handleEditPartner = (partner: Partner) => {
    setFormData({
      name: partner.name,
      type: partner.type,
      contact: partner.contact,
      email: partner.email,
      phone: partner.phone,
      address: partner.address,
      contractStart: partner.contractStart,
      contractEnd: partner.contractEnd,
    });
    setSelectedPartner(partner);
    setOpenDialog(true);
  };

  const handleDeletePartner = (id: string) => {
    setPartners(partners.filter((p) => p.id !== id));
  };

  const handleSubmit = () => {
    if (selectedPartner) {
      // Update existing partner
      setPartners(
        partners.map((p) =>
          p.id === selectedPartner.id
            ? {
                ...p,
                ...formData,
              }
            : p
        )
      );
    } else {
      // Create new partner
      const newPartner: Partner = {
        id: String(Date.now()),
        ...formData,
        status: 'Active',
        rating: 5,
      };
      setPartners([...partners, newPartner]);
    }
    setOpenDialog(false);
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
      case 'Technology Partner':
        return 'primary';
      case 'Infrastructure Partner':
        return 'secondary';
      case 'Cloud Partner':
        return 'info';
      case 'Security Partner':
        return 'warning';
      default:
        return 'default';
    }
  };

  const renderRating = (rating: number) => {
    return (
      <Box sx={{ display: 'flex' }}>
        {[1, 2, 3, 4, 5].map((star) => (
          <IconButton key={star} size="small" disabled>
            {star <= rating ? (
              <Star fontSize="small" sx={{ color: 'gold' }} />
            ) : (
              <StarBorder fontSize="small" />
            )}
          </IconButton>
        ))}
      </Box>
    );
  };

  const filteredPartners = partners.filter(
    (partner) =>
      partner.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      partner.contact.toLowerCase().includes(searchTerm.toLowerCase()) ||
      partner.email.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Partner Management
      </Typography>

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
              <IconButton>
                <Refresh />
              </IconButton>
            </Box>
          </Box>

          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Partner</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Contact</TableCell>
                  <TableCell>Email</TableCell>
                  <TableCell>Phone</TableCell>
                  <TableCell>Rating</TableCell>
                  <TableCell>Status</TableCell>
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
                            <Typography variant="body1">{partner.name}</Typography>
                            <Typography variant="caption" color="text.secondary">
                              {partner.address}
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
                      <TableCell>{partner.contact}</TableCell>
                      <TableCell>{partner.email}</TableCell>
                      <TableCell>{partner.phone}</TableCell>
                      <TableCell>{renderRating(partner.rating)}</TableCell>
                      <TableCell>
                        <Chip
                          label={partner.status}
                          color={getStatusColor(partner.status) as any}
                          size="small"
                        />
                      </TableCell>
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
            rowsPerPageOptions={[5, 10, 25]}
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
                label="Partner Name"
                name="name"
                value={formData.name}
                onChange={handleInputChange}
                required
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
                  <MenuItem value="Technology Partner">Technology Partner</MenuItem>
                  <MenuItem value="Infrastructure Partner">Infrastructure Partner</MenuItem>
                  <MenuItem value="Cloud Partner">Cloud Partner</MenuItem>
                  <MenuItem value="Security Partner">Security Partner</MenuItem>
                  <MenuItem value="Consulting Partner">Consulting Partner</MenuItem>
                </Select>
              </FormControl>
            </Box>
            <Box sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(50% - 8px)' } }}>
              <TextField
                fullWidth
                label="Contact Person"
                name="contact"
                value={formData.contact}
                onChange={handleInputChange}
                required
              />
            </Box>
            <Box sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(50% - 8px)' } }}>
              <TextField
                fullWidth
                label="Email"
                name="email"
                type="email"
                value={formData.email}
                onChange={handleInputChange}
                required
              />
            </Box>
            <Box sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(50% - 8px)' } }}>
              <TextField
                fullWidth
                label="Phone"
                name="phone"
                value={formData.phone}
                onChange={handleInputChange}
                required
              />
            </Box>
            <Box sx={{ flex: '0 0 100%' }}>
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
            <Box sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(50% - 8px)' } }}>
              <TextField
                fullWidth
                label="Contract Start Date"
                name="contractStart"
                type="date"
                value={formData.contractStart}
                onChange={handleInputChange}
                InputLabelProps={{ shrink: true }}
              />
            </Box>
            <Box sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(50% - 8px)' } }}>
              <TextField
                fullWidth
                label="Contract End Date"
                name="contractEnd"
                type="date"
                value={formData.contractEnd}
                onChange={handleInputChange}
                InputLabelProps={{ shrink: true }}
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