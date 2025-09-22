import React, { useEffect, useState } from 'react';
import { observer } from 'mobx-react-lite';
import {
  Box,
  Paper,
  Typography,
  Button,
  IconButton,
  Chip,
  Card,
  CardContent,
  Alert,
  Breadcrumbs,
  Link,
  CircularProgress,
  Divider,
  Tooltip,
  Avatar,
} from '@mui/material';
import {
  Refresh,
  NavigateNext,
  Cloud as CloudIcon,
  Business,
  Computer,
  RouterOutlined as RouterIcon,
  Domain as DataCenterIcon,
  Add,
  Edit as EditIcon,
  Delete as DeleteIcon,
  ExpandMore,
  ExpandLess,
  UnfoldMore,
  UnfoldLess,
} from '@mui/icons-material';
import { TreeView } from '@mui/x-tree-view/TreeView';
import { TreeItem } from '@mui/x-tree-view/TreeItem';
import { useOrganizationInfraStore } from '../stores/base/StoreProvider';
import ComputeEditDialog from '../components/ComputeEditDialog';
import NetworkDeviceEditDialog from '../components/NetworkDeviceEditDialog';
import InfrastructureListView from '../components/infrastructure/InfrastructureListView';

const InfrastructureStellar: React.FC = observer(() => {
  const store = useOrganizationInfraStore();
  const [openComputeDialog, setOpenComputeDialog] = useState(false);
  const [openNetworkDialog, setOpenNetworkDialog] = useState(false);
  const [editMode, setEditMode] = useState(false);
  const [formData, setFormData] = useState<any>({});
  const [filterMode, setFilterMode] = useState<'organization' | 'partners'>('organization');

  useEffect(() => {
    // Load infrastructure tree based on filter mode
    const storeParam = filterMode === 'organization' ? 'self' : 'all';
    store.loadInfrastructureTree(storeParam);
  }, [store, filterMode]);

  const handleNodeSelect = (event: React.SyntheticEvent, nodeId: string) => {
    const node = store.findNode(nodeId);
    store.setSelectedNode(node);
  };

  const handleNodeToggle = (event: React.SyntheticEvent, nodeIds: string[]) => {
    // MobX store handles expanded state internally
  };

  const handleRefresh = () => {
    store.loadInfrastructureTree();
  };

  const handleExpandAll = () => {
    const allNodeIds: string[] = [];
    const collectNodeIds = (nodes: any[]) => {
      nodes.forEach((node) => {
        allNodeIds.push(node.id);
        if (node.children) {
          collectNodeIds(node.children);
        }
      });
    };
    collectNodeIds(store.treeData);
    store.setExpandedNodeIds(allNodeIds);
  };

  const handleCollapseAll = () => {
    store.setExpandedNodeIds([]);
  };

  const handleAddCompute = () => {
    if (store.selectedNode && store.selectedNode.type === 'datacenter') {
      setFormData({
        datacenterId: store.selectedNode.data.id,
      });
      setEditMode(false);
      setOpenComputeDialog(true);
    }
  };

  const handleEditCompute = () => {
    if (store.selectedNode && store.selectedNode.type === 'compute') {
      setFormData(store.selectedNode.data);
      setEditMode(true);
      setOpenComputeDialog(true);
    }
  };

  const handleDeleteCompute = async () => {
    if (store.selectedNode && store.selectedNode.type === 'compute') {
      if (window.confirm('Are you sure you want to delete this compute resource?')) {
        await store.deleteCompute(store.selectedNode.data.id);
      }
    }
  };

  const handleAddNetworkDevice = () => {
    if (store.selectedNode && store.selectedNode.type === 'datacenter') {
      setFormData({
        datacenterId: store.selectedNode.data.id,
      });
      setEditMode(false);
      setOpenNetworkDialog(true);
    }
  };

  const handleEditNetworkDevice = () => {
    if (store.selectedNode && store.selectedNode.type === 'network-device') {
      setFormData(store.selectedNode.data);
      setEditMode(true);
      setOpenNetworkDialog(true);
    }
  };

  const handleDeleteNetworkDevice = async () => {
    if (store.selectedNode && store.selectedNode.type === 'network-device') {
      if (window.confirm('Are you sure you want to delete this network device?')) {
        await store.deleteNetworkDevice(store.selectedNode.data.id);
      }
    }
  };

  const handleSaveCompute = async (data: any) => {
    try {
      if (editMode) {
        await store.updateCompute(data.id, data);
      } else {
        await store.createCompute(data.datacenterId, data);
      }
      setOpenComputeDialog(false);
      setFormData({});
    } catch (error) {
      console.error('Error saving compute:', error);
    }
  };

  const handleSaveNetworkDevice = async (data: any) => {
    try {
      if (editMode) {
        await store.updateNetworkDevice(data.id, data);
      } else {
        await store.createNetworkDevice(data.datacenterId, data);
      }
      setOpenNetworkDialog(false);
      setFormData({});
    } catch (error) {
      console.error('Error saving network device:', error);
    }
  };

  const getNodeIcon = (type: string) => {
    // Avatar style for text-based icons
    const avatarStyle = {
      width: 24,
      height: 24,
      fontSize: 10,
      fontWeight: 600,
    };

    switch (type) {
      case 'partner':
        // Partner nodes keep the people icon
        return <Business sx={{ fontSize: 20, color: '#757575', opacity: 0.9 }} />;
      case 'organization':
        return (
          <Avatar sx={{
            ...avatarStyle,
            backgroundColor: '#e8eaf6',
            color: '#5e35b1'
          }}>
            O
          </Avatar>
        );
      case 'cloud':
        return (
          <Avatar sx={{
            ...avatarStyle,
            backgroundColor: '#e1f5fe',
            color: '#0277bd',
            fontSize: 9
          }}>
            CL
          </Avatar>
        );
      case 'region':
        return (
          <Avatar sx={{
            ...avatarStyle,
            backgroundColor: '#f5f5f5',
            color: '#757575',
            fontSize: 9
          }}>
            RG
          </Avatar>
        );
      case 'availability-zone':
      case 'az':
        return (
          <Avatar sx={{
            ...avatarStyle,
            backgroundColor: '#ede7f6',
            color: '#512da8',
            fontSize: 9
          }}>
            AZ
          </Avatar>
        );
      case 'datacenter':
        return (
          <Avatar sx={{
            ...avatarStyle,
            backgroundColor: '#eceff1',
            color: '#546e7a',
            fontSize: 9
          }}>
            DC
          </Avatar>
        );
      case 'compute':
        return (
          <Avatar sx={{
            ...avatarStyle,
            backgroundColor: '#e8f5e9',
            color: '#2e7d32',
            fontSize: 9
          }}>
            VM
          </Avatar>
        );
      case 'network-device':
        return (
          <Avatar sx={{
            ...avatarStyle,
            backgroundColor: '#fff3e0',
            color: '#e65100',
            fontSize: 9
          }}>
            ND
          </Avatar>
        );
      default:
        return null;
    }
  };

  const renderTreeNodes = (nodes: any[]): React.ReactElement[] => {
    return nodes.map((node) => (
      <TreeItem
        key={node.id}
        nodeId={node.id}
        label={
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
            {getNodeIcon(node.type)}
            <Typography variant="body2" sx={{ ml: 0.5 }}>{node.name}</Typography>
            {node.metadata?.ipAddress && (
              <Chip
                size="small"
                label={node.metadata.ipAddress}
                sx={{ ml: 1, height: 20 }}
              />
            )}
          </Box>
        }
        onClick={() => store.toggleNodeExpanded(node.id)}
      >
        {node.children && renderTreeNodes(node.children)}
      </TreeItem>
    ));
  };

  const renderBreadcrumbs = () => {
    if (!store.selectedNodePath || store.selectedNodePath.length === 0) {
      return null;
    }

    return (
      <Breadcrumbs separator={<NavigateNext fontSize="small" />}>
        {store.selectedNodePath.map((node: any, index: number) => (
          <Link
            key={node.id}
            color={index === store.selectedNodePath.length - 1 ? 'text.primary' : 'inherit'}
            href="#"
            onClick={(e) => {
              e.preventDefault();
              store.setSelectedNode(node);
            }}
            underline="hover"
          >
            {node.name}
          </Link>
        ))}
      </Breadcrumbs>
    );
  };

  const handleEditItem = (item: any) => {
    if (item.type === 'compute' || store.selectedNode?.type === 'datacenter') {
      setFormData(item);
      setEditMode(true);
      setOpenComputeDialog(true);
    } else if (item.type === 'network-device') {
      setFormData(item);
      setEditMode(true);
      setOpenNetworkDialog(true);
    }
  };

  const handleDeleteItem = async (item: any) => {
    if (item.type === 'compute' || store.selectedNode?.type === 'datacenter') {
      if (window.confirm('Are you sure you want to delete this compute resource?')) {
        await store.deleteCompute(item.id);
      }
    } else if (item.type === 'network-device') {
      if (window.confirm('Are you sure you want to delete this network device?')) {
        await store.deleteNetworkDevice(item.id);
      }
    }
  };

  const handleAddItem = () => {
    if (store.selectedNode?.type === 'datacenter') {
      // Default to adding compute, but could be enhanced to choose type
      handleAddCompute();
    }
  };

  if (store.loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Infrastructure
      </Typography>

      <Box sx={{ display: 'flex', height: 'calc(100vh - 60px)' }}>
        {/* Left Panel - Tree */}
        <Paper sx={{ width: 320, m: 2, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
          {/* Tree Header */}
          <Box sx={{
            borderBottom: '1px solid rgba(0, 0, 0, 0.12)',
            p: 2,
            backgroundColor: '#fafafa'
          }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              {/* Filter Buttons */}
              <Box sx={{ display: 'flex', gap: 1 }}>
                <Button
                  variant="outlined"
                  size="small"
                  onClick={() => setFilterMode('organization')}
                  sx={{
                    textTransform: 'none',
                    minWidth: 'auto',
                    py: 0.5,
                    px: 1.5,
                    border: '1px solid #1976d2',
                    backgroundColor: filterMode === 'organization' ? '#E3F2FD' : 'transparent',
                    color: '#616161',
                    fontWeight: filterMode === 'organization' ? 600 : 400,
                    '&:hover': {
                      backgroundColor: filterMode === 'organization' ? '#BBDEFB' : '#F5F5F5',
                      border: '1px solid #1976d2',
                    },
                    borderRadius: 1,
                    transition: 'all 0.2s ease'
                  }}
                >
                  Organization
                </Button>
                <Button
                  variant="outlined"
                  size="small"
                  onClick={() => setFilterMode('partners')}
                  sx={{
                    textTransform: 'none',
                    minWidth: 'auto',
                    py: 0.5,
                    px: 1.5,
                    border: '1px solid #1976d2',
                    backgroundColor: filterMode === 'partners' ? '#E3F2FD' : 'transparent',
                    color: '#616161',
                    fontWeight: filterMode === 'partners' ? 600 : 400,
                    '&:hover': {
                      backgroundColor: filterMode === 'partners' ? '#BBDEFB' : '#F5F5F5',
                      border: '1px solid #1976d2',
                    },
                    borderRadius: 1,
                    transition: 'all 0.2s ease'
                  }}
                >
                  Partners
                </Button>
              </Box>
              {/* Action Buttons */}
              <Box sx={{ display: 'flex', gap: 0.5 }}>
                <Tooltip title="Expand All">
                  <IconButton onClick={handleExpandAll} size="small">
                    <UnfoldMore fontSize="small" />
                  </IconButton>
                </Tooltip>
                <Tooltip title="Collapse All">
                  <IconButton onClick={handleCollapseAll} size="small">
                    <UnfoldLess fontSize="small" />
                  </IconButton>
                </Tooltip>
                <Tooltip title="Refresh">
                  <IconButton onClick={handleRefresh} size="small">
                    <Refresh fontSize="small" />
                  </IconButton>
                </Tooltip>
              </Box>
            </Box>
          </Box>

          {/* Tree Content */}
          <Box sx={{ p: 2, overflow: 'auto', flex: 1 }}>
            {store.error && (
              <Alert severity="error" sx={{ mb: 2 }}>
                {store.error}
              </Alert>
            )}

            <TreeView
              defaultCollapseIcon={<ExpandMore sx={{ fontSize: 18, opacity: 0.6 }} />}
              defaultExpandIcon={<ExpandLess sx={{ fontSize: 18, opacity: 0.6 }} />}
              expanded={store.expandedNodeIds}
              selected={store.selectedNode?.id || ''}
              onNodeSelect={handleNodeSelect}
              onNodeToggle={handleNodeToggle}
              sx={{
                flexGrow: 1,
                '& .MuiTreeItem-content': {
                  paddingLeft: '4px',
                  paddingTop: '3px',
                  paddingBottom: '3px',
                  '&:hover': {
                    backgroundColor: 'rgba(0, 0, 0, 0.03)'
                  },
                  '&.Mui-selected': {
                    backgroundColor: 'rgba(25, 118, 210, 0.08)',
                    '&:hover': {
                      backgroundColor: 'rgba(25, 118, 210, 0.12)'
                    }
                  }
                },
                '& .MuiTreeItem-group': {
                  marginLeft: '12px',
                  borderLeft: '1px solid rgba(0, 0, 0, 0.05)'
                },
                '& .MuiTreeItem-label': {
                  fontSize: '0.9rem',
                  fontWeight: 400
                }
              }}
            >
              {renderTreeNodes(store.treeData)}
            </TreeView>
          </Box>
        </Paper>

      {/* Right Panel - List View */}
      <Box sx={{ flex: 1, p: 2, overflow: 'auto' }}>
        {renderBreadcrumbs()}
        <Box sx={{ mt: 2 }}>
          <InfrastructureListView
            onEdit={handleEditItem}
            onDelete={handleDeleteItem}
            onAdd={handleAddItem}
          />
        </Box>
      </Box>

      {/* Dialogs */}
      {openComputeDialog && (
        <ComputeEditDialog
          open={openComputeDialog}
          formData={formData}
          setFormData={setFormData}
          editMode={editMode}
          onClose={() => {
            setOpenComputeDialog(false);
            setFormData({});
          }}
          onSave={handleSaveCompute}
        />
      )}

      {openNetworkDialog && (
        <NetworkDeviceEditDialog
          open={openNetworkDialog}
          formData={formData}
          setFormData={setFormData}
          editMode={editMode}
          onClose={() => {
            setOpenNetworkDialog(false);
            setFormData({});
          }}
          onSave={handleSaveNetworkDevice}
        />
      )}
      </Box>
    </Box>
  );
});

export default InfrastructureStellar;