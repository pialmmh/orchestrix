import React, { useState } from 'react';
import { observer } from 'mobx-react-lite';
import {
  Box,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  IconButton,
  Chip,
  Tooltip,
  Typography,
  CircularProgress,
  Alert,
  Button,
} from '@mui/material';
import {
  Edit as EditIcon,
  Delete as DeleteIcon,
  Add as AddIcon,
  Refresh as RefreshIcon,
  Computer,
  Cloud as CloudIcon,
  Domain as DataCenterIcon,
  RouterOutlined as RouterIcon,
} from '@mui/icons-material';
import { useOrganizationInfraStore } from '../../stores/base/StoreProvider';

interface InfrastructureListViewProps {
  onEdit?: (item: any) => void;
  onDelete?: (item: any) => void;
  onAdd?: () => void;
}

const InfrastructureListView: React.FC<InfrastructureListViewProps> = observer(({
  onEdit,
  onDelete,
  onAdd,
}) => {
  const store = useOrganizationInfraStore();
  const {
    selectedNode,
    selectedNodeChildren,
    selectedNodeChildrenLoading,
    selectedNodeChildrenError,
    selectedNodeChildrenPage,
    selectedNodeChildrenPageSize,
    selectedNodeChildrenTotal,
  } = store;

  const handlePageChange = (event: unknown, newPage: number) => {
    store.setSelectedNodeChildrenPage(newPage + 1); // MUI uses 0-based, we use 1-based
  };

  const handleRowsPerPageChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    store.selectedNodeChildrenPageSize = parseInt(event.target.value, 10);
    store.setSelectedNodeChildrenPage(1);
  };

  const handleRefresh = () => {
    store.loadSelectedNodeChildren();
  };

  const getIcon = (type: string) => {
    switch (type) {
      case 'compute':
        return <Computer />;
      case 'cloud':
        return <CloudIcon />;
      case 'datacenter':
        return <DataCenterIcon />;
      case 'network-device':
        return <RouterIcon />;
      default:
        return null;
    }
  };

  const getChildType = () => {
    if (!selectedNode) return null;
    switch (selectedNode.type) {
      case 'partner':
        return 'cloud';
      case 'cloud':
        return 'datacenter';
      case 'datacenter':
        return 'compute';
      default:
        return null;
    }
  };

  const childType = getChildType();
  const canAdd = childType && onAdd;

  if (!selectedNode) {
    return (
      <Paper sx={{ p: 3 }}>
        <Typography color="text.secondary">
          Select a node from the tree to view its contents
        </Typography>
      </Paper>
    );
  }

  if (selectedNodeChildrenLoading) {
    return (
      <Paper sx={{ p: 3, display: 'flex', justifyContent: 'center' }}>
        <CircularProgress />
      </Paper>
    );
  }

  if (selectedNodeChildrenError) {
    return (
      <Paper sx={{ p: 3 }}>
        <Alert severity="error">{selectedNodeChildrenError}</Alert>
      </Paper>
    );
  }

  const renderTableContent = () => {
    if (!selectedNodeChildren || selectedNodeChildren.length === 0) {
      return (
        <TableRow>
          <TableCell colSpan={6} align="center">
            <Typography color="text.secondary" sx={{ py: 3 }}>
              No {childType || 'items'} found in {selectedNode.name}
            </Typography>
          </TableCell>
        </TableRow>
      );
    }

    return selectedNodeChildren.map((item) => (
      <TableRow key={item.id} hover>
        <TableCell>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            {getIcon(childType || item.type)}
            {item.name || item.hostname || item.id}
          </Box>
        </TableCell>
        <TableCell>{item.type || childType}</TableCell>
        <TableCell>
          {item.ip || item.ipAddress || item.location || '-'}
        </TableCell>
        <TableCell>
          <Chip
            label={item.status || 'active'}
            color={item.status === 'active' ? 'success' : 'default'}
            size="small"
          />
        </TableCell>
        <TableCell>
          {item.cpu && item.memory
            ? `${item.cpu} vCPU / ${item.memory} MB`
            : item.description || '-'}
        </TableCell>
        <TableCell align="right">
          <Tooltip title="Edit">
            <IconButton
              size="small"
              onClick={() => onEdit?.(item)}
              color="primary"
            >
              <EditIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="Delete">
            <IconButton
              size="small"
              onClick={() => onDelete?.(item)}
              color="error"
            >
              <DeleteIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </TableCell>
      </TableRow>
    ));
  };

  return (
    <Paper sx={{ width: '100%' }}>
      <Box sx={{ p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h6">
          {childType ? `${childType.charAt(0).toUpperCase() + childType.slice(1)}s` : 'Items'} in {selectedNode.name}
        </Typography>
        <Box sx={{ display: 'flex', gap: 1 }}>
          {canAdd && (
            <Button
              startIcon={<AddIcon />}
              variant="contained"
              size="small"
              onClick={onAdd}
            >
              Add {childType}
            </Button>
          )}
          <IconButton onClick={handleRefresh} size="small">
            <RefreshIcon />
          </IconButton>
        </Box>
      </Box>
      
      <TableContainer>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Name</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>IP/Location</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Details</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {renderTableContent()}
          </TableBody>
        </Table>
      </TableContainer>
      
      {selectedNodeChildren.length > 0 && (
        <TablePagination
          rowsPerPageOptions={[5, 10, 25, 50]}
          component="div"
          count={selectedNodeChildrenTotal}
          rowsPerPage={selectedNodeChildrenPageSize}
          page={selectedNodeChildrenPage - 1}
          onPageChange={handlePageChange}
          onRowsPerPageChange={handleRowsPerPageChange}
        />
      )}
    </Paper>
  );
});

export default InfrastructureListView;