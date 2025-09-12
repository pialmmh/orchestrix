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

  useEffect(() => {
    // Load infrastructure tree on component mount
    store.loadInfrastructureTree();
  }, [store]);

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
    switch (type) {
      case 'partner':
      case 'organization':
        return <Business />;
      case 'cloud':
        return <CloudIcon />;
      case 'datacenter':
        return <DataCenterIcon />;
      case 'compute':
        return <Computer />;
      case 'network-device':
        return <RouterIcon />;
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
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            {getNodeIcon(node.type)}
            <Typography variant="body2">{node.name}</Typography>
            {node.metadata?.ipAddress && (
              <Chip size="small" label={node.metadata.ipAddress} />
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
    <Box sx={{ display: 'flex', height: '100vh', bgcolor: 'background.default' }}>
      {/* Left Panel - Tree */}
      <Paper sx={{ width: 400, p: 2, m: 2, overflow: 'auto' }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="h6">Infrastructure</Typography>
          <IconButton onClick={handleRefresh}>
            <Refresh />
          </IconButton>
        </Box>

        {store.error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {store.error}
          </Alert>
        )}

        <TreeView
          defaultCollapseIcon={<ExpandMore />}
          defaultExpandIcon={<ExpandLess />}
          expanded={store.expandedNodeIds}
          selected={store.selectedNode?.id || ''}
          onNodeSelect={handleNodeSelect}
          onNodeToggle={handleNodeToggle}
        >
          {renderTreeNodes(store.treeData)}
        </TreeView>
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
  );
});

export default InfrastructureStellar;