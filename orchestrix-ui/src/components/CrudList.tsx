import React, { useState, useMemo } from 'react';
import {
  Box,
  Paper,
  Typography,
  TextField,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  IconButton,
  Chip,
  InputAdornment,
  Toolbar,
  MenuItem,
  Select,
  FormControl,
  InputLabel,
  Tooltip
} from '@mui/material';
import {
  Add as AddIcon,
  Search as SearchIcon,
  Visibility as ViewIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  FilterList as FilterIcon
} from '@mui/icons-material';

interface Column {
  id: string;
  label: string;
  minWidth?: number;
  align?: 'right' | 'left' | 'center';
  format?: (value: any) => string | React.ReactNode;
}

interface CrudListProps {
  title: string;
  data: any[];
  columns: Column[];
  searchFields: string[];
  onAdd?: () => void;
  onView: (item: any) => void;
  onEdit: (item: any) => void;
  onDelete: (item: any) => void;
  filterField?: string;
  filterOptions?: { label: string; value: string | null }[];
  loading?: boolean;
  emptyMessage?: string;
}

const CrudList: React.FC<CrudListProps> = ({
  title,
  data,
  columns,
  searchFields,
  onAdd,
  onView,
  onEdit,
  onDelete,
  filterField,
  filterOptions,
  loading = false,
  emptyMessage = 'No data available'
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [filterValue, setFilterValue] = useState<string | null>(null);

  const filteredData = useMemo(() => {
    let filtered = data;

    // Apply search filter
    if (searchTerm) {
      filtered = filtered.filter((item) =>
        searchFields.some((field) => {
          const value = field.split('.').reduce((obj, key) => obj?.[key], item);
          return value?.toString().toLowerCase().includes(searchTerm.toLowerCase());
        })
      );
    }

    // Apply type filter
    if (filterValue && filterField) {
      filtered = filtered.filter((item) => {
        const value = filterField.split('.').reduce((obj, key) => obj?.[key], item);
        return value === filterValue;
      });
    }

    return filtered;
  }, [data, searchTerm, filterValue, searchFields, filterField]);

  const getStatusColor = (status: string) => {
    switch (status?.toLowerCase()) {
      case 'active':
      case 'running':
        return 'success';
      case 'inactive':
      case 'stopped':
        return 'error';
      case 'maintenance':
      case 'paused':
        return 'warning';
      default:
        return 'default';
    }
  };

  return (
    <Paper sx={{ width: '100%', height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Toolbar sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Typography variant="h6" sx={{ flexGrow: 1 }}>
          {title}
        </Typography>
        {onAdd && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={onAdd}
            sx={{ ml: 2 }}
          >
            Add New
          </Button>
        )}
      </Toolbar>

      <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
        <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
          <TextField
            size="small"
            placeholder="Search..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon />
                </InputAdornment>
              ),
            }}
            sx={{ minWidth: 300 }}
          />
          
          {filterOptions && filterField && (
            <FormControl size="small" sx={{ minWidth: 150 }}>
              <InputLabel>Filter</InputLabel>
              <Select
                value={filterValue || ''}
                label="Filter"
                onChange={(e) => setFilterValue(e.target.value || null)}
                startAdornment={<FilterIcon sx={{ mr: 1 }} />}
              >
                <MenuItem value="">All</MenuItem>
                {filterOptions.map((option) => (
                  <MenuItem key={option.value || 'all'} value={option.value || ''}>
                    {option.label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          )}
        </Box>
      </Box>

      <TableContainer sx={{ flexGrow: 1, overflow: 'auto' }}>
        <Table stickyHeader>
          <TableHead>
            <TableRow>
              {columns.map((column) => (
                <TableCell
                  key={column.id}
                  align={column.align}
                  style={{ minWidth: column.minWidth }}
                  sx={{ fontWeight: 'bold' }}
                >
                  {column.label}
                </TableCell>
              ))}
              <TableCell align="center" sx={{ minWidth: 120, fontWeight: 'bold' }}>
                Actions
              </TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={columns.length + 1} align="center" sx={{ py: 4 }}>
                  Loading...
                </TableCell>
              </TableRow>
            ) : filteredData.length === 0 ? (
              <TableRow>
                <TableCell colSpan={columns.length + 1} align="center" sx={{ py: 4 }}>
                  {emptyMessage}
                </TableCell>
              </TableRow>
            ) : (
              filteredData.map((row, index) => (
                <TableRow key={row.id || index} hover>
                  {columns.map((column) => {
                    const value = column.id.split('.').reduce((obj, key) => obj?.[key], row);
                    
                    return (
                      <TableCell key={column.id} align={column.align}>
                        {column.format ? (
                          column.format(value)
                        ) : column.id === 'status' ? (
                          <Chip
                            label={value}
                            size="small"
                            color={getStatusColor(value) as any}
                            sx={{ minWidth: 80 }}
                          />
                        ) : (
                          value
                        )}
                      </TableCell>
                    );
                  })}
                  <TableCell align="center">
                    <Box sx={{ display: 'flex', gap: 1, justifyContent: 'center' }}>
                      <Tooltip title="View">
                        <IconButton size="small" onClick={() => onView(row)} color="info">
                          <ViewIcon />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Edit">
                        <IconButton size="small" onClick={() => onEdit(row)} color="primary">
                          <EditIcon />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Delete">
                        <IconButton size="small" onClick={() => onDelete(row)} color="error">
                          <DeleteIcon />
                        </IconButton>
                      </Tooltip>
                    </Box>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </Paper>
  );
};

export default CrudList;