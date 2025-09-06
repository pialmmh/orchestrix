import React, { useState, useEffect } from 'react';
import {
  Box,
  Paper,
  Typography,
  Button,
  IconButton,
  Chip,
  Divider,
  Card,
  CardContent,
  TextField,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Alert,
  Menu,
  ListItemIcon,
  ListItemText,
} from '@mui/material';
import Grid from '@mui/material/Grid';
import {
  Add,
  Edit,
  Delete,
  Refresh,
  ExpandMore,
  ChevronRight,
  Cloud as CloudIcon,
  Business,
  Computer,
  Storage as StorageIcon,
} from '@mui/icons-material';
import { SimpleTreeView } from '@mui/x-tree-view/SimpleTreeView';
import { TreeItem } from '@mui/x-tree-view/TreeItem';
import axios from 'axios';

interface TreeNode {
  id: string;
  name: string;
  type: 'root' | 'cloud' | 'datacenter' | 'compute' | 'container' | 'container-type';
  children?: TreeNode[];
  data?: any;
}

interface Partner {
  id: number;
  name: string;
  displayName: string;
  roles: string[];
}

const InfrastructureWithTree: React.FC = () => {
  const [treeData, setTreeData] = useState<TreeNode[]>([]);
  const [selectedNodeId, setSelectedNodeId] = useState<string>('');
  const [selectedNode, setSelectedNode] = useState<TreeNode | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [expanded, setExpanded] = useState<string[]>(['root', 'clouds']);
  
  // Dialog states
  const [openDialog, setOpenDialog] = useState(false);
  const [dialogType, setDialogType] = useState<'cloud' | 'datacenter' | 'compute' | null>(null);
  const [editMode, setEditMode] = useState(false);
  const [formData, setFormData] = useState<any>({});
  const [partners, setPartners] = useState<Partner[]>([]);
  
  // Context menu state
  const [contextMenu, setContextMenu] = useState<{
    mouseX: number;
    mouseY: number;
    node: TreeNode | null;
  } | null>(null);

  useEffect(() => {
    fetchInfrastructureData();
    fetchPartners();
  }, []);

  const fetchPartners = async () => {
    try {
      const response = await axios.get('/api/partners');
      const filtered = response.data.partners.filter((p: Partner) => 
        p.roles && (p.roles.includes('customer') || p.roles.includes('self'))
      );
      setPartners(filtered);
    } catch (error) {
      console.error('Error fetching partners:', error);
    }
  };

  const fetchInfrastructureData = async () => {
    setLoading(true);
    try {
      // Fetch clouds with their relationships
      const cloudsResponse = await axios.get('/api/clouds');
      const clouds = cloudsResponse.data || [];
      
      // Try to fetch datacenters, but don't fail if endpoint doesn't exist
      let datacenters: any[] = [];
      try {
        const datacentersResponse = await axios.get('/api/datacenters');
        datacenters = datacentersResponse.data || [];
      } catch (error) {
        console.log('Datacenters endpoint not available');
      }
      
      // Try to fetch computes, but don't fail if endpoint doesn't exist
      let computes: any[] = [];
      try {
        const computesResponse = await axios.get('/api/computes');
        computes = computesResponse.data || [];
      } catch (error) {
        console.log('Computes endpoint not available');
      }

      // Remove duplicate clouds based on ID
      const uniqueClouds = clouds.reduce((acc: any[], cloud: any) => {
        if (!acc.find(c => c.id === cloud.id)) {
          acc.push(cloud);
        }
        return acc;
      }, []);

      // Build tree structure
      const tree: TreeNode[] = [
        {
          id: 'root',
          name: 'Infrastructure',
          type: 'root',
          children: [
            {
              id: 'clouds',
              name: 'Cloud Instances',
              type: 'root',
              children: uniqueClouds.map((cloud: any, cloudIndex: number) => ({
                id: `cloud-${cloud.id}-${cloudIndex}`, // Add index to ensure uniqueness
                name: cloud.name,
                type: 'cloud',
                data: cloud,
                children: datacenters
                  .filter((dc: any) => dc.cloud?.id === cloud.id || dc.cloudId === cloud.id)
                  .map((dc: any, dcIndex: number) => ({
                    id: `datacenter-${dc.id}-${dcIndex}`,
                    name: dc.name,
                    type: 'datacenter',
                    data: dc,
                    children: computes
                      .filter((comp: any) => comp.datacenter?.id === dc.id || comp.datacenterId === dc.id)
                      .map((comp: any, compIndex: number) => ({
                        id: `compute-${comp.id}-${compIndex}`,
                        name: comp.name || comp.hostname,
                        type: 'compute',
                        data: comp,
                        children: comp.containers ? 
                          // Group containers by type
                          Object.entries(
                            comp.containers.reduce((acc: any, container: any) => {
                              const type = container.containerType || 'Unknown';
                              if (!acc[type]) acc[type] = [];
                              acc[type].push(container);
                              return acc;
                            }, {})
                          ).map(([type, containers]: [string, any], typeIndex: number) => ({
                            id: `container-type-${comp.id}-${type}-${typeIndex}`,
                            name: type,
                            type: 'container-type',
                            children: (containers as any[]).map((container: any, containerIndex: number) => ({
                              id: `container-${container.id}-${containerIndex}`,
                              name: container.name,
                              type: 'container',
                              data: container
                            }))
                          }))
                        : []
                      }))
                  }))
              }))
            }
          ]
        }
      ];
      
      setTreeData(tree);
      setError(null);
    } catch (error) {
      console.error('Error fetching infrastructure data:', error);
      // Don't show error, just set empty tree
      setTreeData([]);
    } finally {
      setLoading(false);
    }
  };

  const handleNodeSelect = (event: React.SyntheticEvent | null, nodeId: string | null) => {
    if (nodeId) {
      setSelectedNodeId(nodeId);
      const node = findNodeById(treeData, nodeId);
      setSelectedNode(node);
    }
  };

  const findNodeById = (nodes: TreeNode[], id: string): TreeNode | null => {
    for (const node of nodes) {
      if (node.id === id) return node;
      if (node.children) {
        const found = findNodeById(node.children, id);
        if (found) return found;
      }
    }
    return null;
  };

  const handleToggle = (event: React.SyntheticEvent | null, nodeIds: string[]) => {
    setExpanded(nodeIds);
  };

  const handleAdd = (type: 'cloud' | 'datacenter' | 'compute', parentNode?: TreeNode) => {
    setDialogType(type);
    setEditMode(false);
    
    // If adding from context menu, set the parent node
    if (parentNode) {
      setSelectedNode(parentNode);
      setSelectedNodeId(parentNode.id);
    }
    
    setFormData(getDefaultFormData(type));
    setOpenDialog(true);
  };

  const handleContextMenu = (event: React.MouseEvent, node: TreeNode) => {
    event.preventDefault();
    event.stopPropagation();
    setContextMenu(
      contextMenu === null
        ? {
            mouseX: event.clientX + 2,
            mouseY: event.clientY - 6,
            node,
          }
        : null,
    );
  };

  const handleCloseContextMenu = () => {
    setContextMenu(null);
  };

  const handleContextMenuAction = (action: string) => {
    if (!contextMenu?.node) return;
    
    const node = contextMenu.node;
    setSelectedNode(node);
    setSelectedNodeId(node.id);
    
    switch (action) {
      case 'add-cloud':
        handleAdd('cloud');
        break;
      case 'add-datacenter':
        handleAdd('datacenter', node);
        break;
      case 'add-compute':
        handleAdd('compute', node);
        break;
      case 'edit':
        setSelectedNode(node);
        handleEdit();
        break;
      case 'delete':
        setSelectedNode(node);
        handleDelete();
        break;
    }
    
    handleCloseContextMenu();
  };

  const handleEdit = () => {
    if (!selectedNode || !selectedNode.data) return;
    
    const type = selectedNode.type as 'cloud' | 'datacenter' | 'compute';
    setDialogType(type);
    setEditMode(true);
    setFormData(selectedNode.data);
    setOpenDialog(true);
  };

  const handleDelete = async () => {
    if (!selectedNode || !selectedNode.data) return;
    
    if (!window.confirm(`Are you sure you want to delete ${selectedNode.name}?`)) {
      return;
    }

    try {
      let endpoint = '';
      switch (selectedNode.type) {
        case 'cloud':
          endpoint = `/api/clouds/${selectedNode.data.id}`;
          break;
        case 'datacenter':
          endpoint = `/api/datacenters/${selectedNode.data.id}`;
          break;
        case 'compute':
          endpoint = `/api/computes/${selectedNode.data.id}`;
          break;
        default:
          return;
      }
      
      await axios.delete(endpoint);
      await fetchInfrastructureData();
      setSelectedNode(null);
      setSelectedNodeId('');
    } catch (error) {
      console.error('Error deleting:', error);
      alert('Failed to delete item');
    }
  };

  const handleSave = async () => {
    try {
      let endpoint = '';
      let payload = { ...formData };
      
      switch (dialogType) {
        case 'cloud':
          endpoint = editMode ? `/api/clouds/${formData.id}` : '/api/clouds';
          break;
        case 'datacenter':
          endpoint = editMode ? `/api/datacenters/${formData.id}` : '/api/datacenters';
          // Add cloud reference if creating datacenter under a cloud
          if (!editMode && selectedNode?.type === 'cloud') {
            payload.cloudId = selectedNode.data.id;
          }
          break;
        case 'compute':
          endpoint = editMode ? `/api/computes/${formData.id}` : '/api/computes';
          // Add datacenter reference if creating compute under a datacenter
          if (!editMode && selectedNode?.type === 'datacenter') {
            payload.datacenterId = selectedNode.data.id;
            payload.cloudId = selectedNode.data.cloud?.id;
          }
          break;
      }
      
      if (editMode) {
        await axios.put(endpoint, payload);
      } else {
        await axios.post(endpoint, payload);
      }
      
      setOpenDialog(false);
      await fetchInfrastructureData();
    } catch (error) {
      console.error('Error saving:', error);
      alert('Failed to save item');
    }
  };

  const getDefaultFormData = (type: string) => {
    switch (type) {
      case 'cloud':
        return {
          name: '',
          description: '',
          partnerId: null,
          deploymentRegion: '',
          status: 'ACTIVE'
        };
      case 'datacenter':
        return {
          name: '',
          location: '',
          type: 'PRIMARY',
          provider: '',
          isDrSite: false,
          status: 'ACTIVE'
        };
      case 'compute':
        return {
          name: '',
          hostname: '',
          ipAddress: '',
          computeType: 'DEDICATED',
          cpuCores: 0,
          memoryGb: 0,
          diskGb: 0,
          status: 'ACTIVE'
        };
      default:
        return {};
    }
  };

  const renderTreeItem = (node: TreeNode) => {
    let icon = <ChevronRight />;
    if (node.type === 'cloud') icon = <CloudIcon />;
    else if (node.type === 'datacenter') icon = <Business />;
    else if (node.type === 'compute') icon = <Computer />;
    else if (node.type === 'container') icon = <StorageIcon />;

    return (
      <TreeItem
        key={node.id}
        itemId={node.id}
        label={
          <Box 
            sx={{ display: 'flex', alignItems: 'center', py: 0.5 }}
            onContextMenu={(e) => handleContextMenu(e, node)}
          >
            {icon}
            <Typography sx={{ ml: 1 }}>{node.name}</Typography>
          </Box>
        }
      >
        {node.children?.map(child => renderTreeItem(child))}
      </TreeItem>
    );
  };

  const renderDetails = () => {
    if (!selectedNode) {
      return (
        <Box sx={{ p: 3, textAlign: 'center' }}>
          <Typography variant="h5" gutterBottom>
            Infrastructure Management
          </Typography>
          <Typography variant="body1" color="text.secondary" paragraph>
            Manage your cloud infrastructure, data centers, compute resources, and containers.
            Select an item from the tree on the left to view details or use the buttons below to add new resources.
          </Typography>
          <Box sx={{ mt: 4, display: 'flex', gap: 2, justifyContent: 'center' }}>
            <Button variant="contained" startIcon={<Add />} onClick={() => handleAdd('cloud')}>
              Add Cloud
            </Button>
          </Box>
        </Box>
      );
    }

    if (selectedNode.type === 'root' || selectedNode.id === 'clouds') {
      return (
        <Box sx={{ p: 3 }}>
          <Typography variant="h5" gutterBottom>
            {selectedNode.name}
          </Typography>
          <Typography variant="body1" color="text.secondary" paragraph>
            {selectedNode.id === 'clouds' 
              ? 'Manage your cloud instances. Each cloud can contain multiple data centers.'
              : 'Navigate through your infrastructure hierarchy using the tree on the left.'}
          </Typography>
          {selectedNode.id === 'clouds' && (
            <Button variant="contained" startIcon={<Add />} onClick={() => handleAdd('cloud')}>
              Add Cloud Instance
            </Button>
          )}
        </Box>
      );
    }

    const { data } = selectedNode;
    
    return (
      <Box sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
          <Box>
            <Typography variant="h5" gutterBottom>
              {selectedNode.name}
            </Typography>
            <Chip 
              label={selectedNode.type.toUpperCase()} 
              size="small" 
              color="primary" 
              variant="outlined"
            />
          </Box>
          <Box sx={{ display: 'flex', gap: 1 }}>
            {selectedNode.type === 'cloud' && (
              <Button 
                variant="outlined" 
                startIcon={<Add />} 
                onClick={() => handleAdd('datacenter')}
              >
                Add Datacenter
              </Button>
            )}
            {selectedNode.type === 'datacenter' && (
              <Button 
                variant="outlined" 
                startIcon={<Add />} 
                onClick={() => handleAdd('compute')}
              >
                Add Compute
              </Button>
            )}
            <IconButton onClick={handleEdit} color="primary">
              <Edit />
            </IconButton>
            <IconButton onClick={handleDelete} color="error">
              <Delete />
            </IconButton>
          </Box>
        </Box>

        <Divider sx={{ mb: 3 }} />

        {selectedNode.type === 'cloud' && data && (
          <Grid container spacing={3}>
            <Grid size={6}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>General Information</Typography>
                  <Typography><strong>Name:</strong> {data.name}</Typography>
                  <Typography><strong>Description:</strong> {data.description || 'N/A'}</Typography>
                  <Typography><strong>Partner:</strong> {partners.find(p => p.id === data.partnerId)?.displayName || 'N/A'}</Typography>
                  <Typography><strong>Region:</strong> {data.deploymentRegion || 'N/A'}</Typography>
                  <Typography><strong>Status:</strong> <Chip label={data.status} size="small" color={data.status === 'ACTIVE' ? 'success' : 'default'} /></Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid size={6}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>Resources</Typography>
                  <Typography><strong>Datacenters:</strong> {data.datacenterCount || 0}</Typography>
                  <Typography><strong>Compute Nodes:</strong> {data.computeCount || 0}</Typography>
                  <Typography><strong>Storage:</strong> {data.storageCount || 0} TB</Typography>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        )}

        {selectedNode.type === 'datacenter' && data && (
          <Grid container spacing={3}>
            <Grid size={6}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>General Information</Typography>
                  <Typography><strong>Name:</strong> {data.name}</Typography>
                  <Typography><strong>Location:</strong> {data.location || 'N/A'}</Typography>
                  <Typography><strong>Type:</strong> {data.type}</Typography>
                  <Typography><strong>Provider:</strong> {data.provider || 'N/A'}</Typography>
                  <Typography><strong>DR Site:</strong> {data.isDrSite ? 'Yes' : 'No'}</Typography>
                  <Typography><strong>Status:</strong> <Chip label={data.status} size="small" color={data.status === 'ACTIVE' ? 'success' : 'default'} /></Typography>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        )}

        {selectedNode.type === 'compute' && data && (
          <Grid container spacing={3}>
            <Grid size={6}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>General Information</Typography>
                  <Typography><strong>Name:</strong> {data.name}</Typography>
                  <Typography><strong>Hostname:</strong> {data.hostname}</Typography>
                  <Typography><strong>IP Address:</strong> {data.ipAddress}</Typography>
                  <Typography><strong>Type:</strong> {data.computeType}</Typography>
                  <Typography><strong>Status:</strong> <Chip label={data.status} size="small" color={data.status === 'ACTIVE' ? 'success' : 'default'} /></Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid size={6}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>Resources</Typography>
                  <Typography><strong>CPU Cores:</strong> {data.cpuCores}</Typography>
                  <Typography><strong>Memory:</strong> {data.memoryGb} GB</Typography>
                  <Typography><strong>Disk:</strong> {data.diskGb} GB</Typography>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        )}

        {selectedNode.type === 'container' && data && (
          <Grid container spacing={3}>
            <Grid size={12}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>Container Information</Typography>
                  <Typography><strong>Name:</strong> {data.name}</Typography>
                  <Typography><strong>Type:</strong> {data.containerType}</Typography>
                  <Typography><strong>Image:</strong> {data.image}</Typography>
                  <Typography><strong>IP Address:</strong> {data.ipAddress}</Typography>
                  <Typography><strong>Status:</strong> <Chip label={data.status} size="small" color={data.status === 'RUNNING' ? 'success' : 'default'} /></Typography>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        )}
      </Box>
    );
  };

  return (
    <Box sx={{ display: 'flex', height: 'calc(100vh - 64px)', overflow: 'hidden' }}>
      {/* Left Panel - Tree */}
      <Paper sx={{ width: 300, overflow: 'auto', borderRadius: 0 }}>
        <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Typography variant="h6">Infrastructure</Typography>
            <IconButton onClick={fetchInfrastructureData} size="small">
              <Refresh />
            </IconButton>
          </Box>
        </Box>
        {error && (
          <Alert severity="error" sx={{ m: 2 }}>
            {error}
          </Alert>
        )}
        <SimpleTreeView
          expandedItems={expanded}
          selectedItems={selectedNodeId}
          onExpandedItemsChange={handleToggle}
          onSelectedItemsChange={handleNodeSelect}
          slots={{
            collapseIcon: ExpandMore,
            expandIcon: ChevronRight,
          }}
          sx={{ p: 2 }}
        >
          {treeData.map(node => renderTreeItem(node))}
        </SimpleTreeView>
      </Paper>

      {/* Right Panel - Details */}
      <Box sx={{ flex: 1, overflow: 'auto', bgcolor: 'background.default' }}>
        {renderDetails()}
      </Box>

      {/* Add/Edit Dialog */}
      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>
          {editMode ? 'Edit' : 'Add'} {dialogType && dialogType.charAt(0).toUpperCase() + dialogType.slice(1)}
        </DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 2 }}>
            {dialogType === 'cloud' && (
              <>
                <TextField
                  label="Name"
                  value={formData.name || ''}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  fullWidth
                  required
                />
                <TextField
                  label="Description"
                  value={formData.description || ''}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  fullWidth
                  multiline
                  rows={2}
                />
                <FormControl fullWidth required>
                  <InputLabel>Partner</InputLabel>
                  <Select
                    value={formData.partnerId || ''}
                    onChange={(e) => setFormData({ ...formData, partnerId: e.target.value })}
                    label="Partner"
                  >
                    {partners.map((partner) => (
                      <MenuItem key={partner.id} value={partner.id}>
                        {partner.displayName}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
                <TextField
                  label="Deployment Region"
                  value={formData.deploymentRegion || ''}
                  onChange={(e) => setFormData({ ...formData, deploymentRegion: e.target.value })}
                  fullWidth
                />
                <FormControl fullWidth>
                  <InputLabel>Status</InputLabel>
                  <Select
                    value={formData.status || 'ACTIVE'}
                    onChange={(e) => setFormData({ ...formData, status: e.target.value })}
                    label="Status"
                  >
                    <MenuItem value="ACTIVE">Active</MenuItem>
                    <MenuItem value="INACTIVE">Inactive</MenuItem>
                    <MenuItem value="MAINTENANCE">Maintenance</MenuItem>
                  </Select>
                </FormControl>
              </>
            )}
            
            {dialogType === 'datacenter' && (
              <>
                <TextField
                  label="Name"
                  value={formData.name || ''}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  fullWidth
                  required
                />
                <TextField
                  label="Location"
                  value={formData.location || ''}
                  onChange={(e) => setFormData({ ...formData, location: e.target.value })}
                  fullWidth
                />
                <FormControl fullWidth>
                  <InputLabel>Type</InputLabel>
                  <Select
                    value={formData.type || 'PRIMARY'}
                    onChange={(e) => setFormData({ ...formData, type: e.target.value })}
                    label="Type"
                  >
                    <MenuItem value="PRIMARY">Primary</MenuItem>
                    <MenuItem value="SECONDARY">Secondary</MenuItem>
                    <MenuItem value="EDGE">Edge</MenuItem>
                  </Select>
                </FormControl>
                <TextField
                  label="Provider"
                  value={formData.provider || ''}
                  onChange={(e) => setFormData({ ...formData, provider: e.target.value })}
                  fullWidth
                />
              </>
            )}
            
            {dialogType === 'compute' && (
              <>
                <TextField
                  label="Name"
                  value={formData.name || ''}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  fullWidth
                  required
                />
                <TextField
                  label="Hostname"
                  value={formData.hostname || ''}
                  onChange={(e) => setFormData({ ...formData, hostname: e.target.value })}
                  fullWidth
                  required
                />
                <TextField
                  label="IP Address"
                  value={formData.ipAddress || ''}
                  onChange={(e) => setFormData({ ...formData, ipAddress: e.target.value })}
                  fullWidth
                />
                <FormControl fullWidth>
                  <InputLabel>Type</InputLabel>
                  <Select
                    value={formData.computeType || 'DEDICATED'}
                    onChange={(e) => setFormData({ ...formData, computeType: e.target.value })}
                    label="Type"
                  >
                    <MenuItem value="DEDICATED">Dedicated</MenuItem>
                    <MenuItem value="VM">Virtual Machine</MenuItem>
                  </Select>
                </FormControl>
                <TextField
                  label="CPU Cores"
                  type="number"
                  value={formData.cpuCores || 0}
                  onChange={(e) => setFormData({ ...formData, cpuCores: parseInt(e.target.value) })}
                  fullWidth
                />
                <TextField
                  label="Memory (GB)"
                  type="number"
                  value={formData.memoryGb || 0}
                  onChange={(e) => setFormData({ ...formData, memoryGb: parseInt(e.target.value) })}
                  fullWidth
                />
                <TextField
                  label="Disk (GB)"
                  type="number"
                  value={formData.diskGb || 0}
                  onChange={(e) => setFormData({ ...formData, diskGb: parseInt(e.target.value) })}
                  fullWidth
                />
              </>
            )}
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button onClick={handleSave} variant="contained">
            {editMode ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Context Menu */}
      <Menu
        open={contextMenu !== null}
        onClose={handleCloseContextMenu}
        anchorReference="anchorPosition"
        anchorPosition={
          contextMenu !== null
            ? { top: contextMenu.mouseY, left: contextMenu.mouseX }
            : undefined
        }
      >
        {contextMenu?.node?.type === 'root' && (
          <MenuItem onClick={() => handleContextMenuAction('add-cloud')}>
            <ListItemIcon>
              <Add fontSize="small" />
            </ListItemIcon>
            <ListItemText>Add Cloud</ListItemText>
          </MenuItem>
        )}
        
        {contextMenu?.node?.type === 'cloud' && (
          <>
            <MenuItem onClick={() => handleContextMenuAction('add-datacenter')}>
              <ListItemIcon>
                <Add fontSize="small" />
              </ListItemIcon>
              <ListItemText>Add Datacenter</ListItemText>
            </MenuItem>
            <MenuItem onClick={() => handleContextMenuAction('edit')}>
              <ListItemIcon>
                <Edit fontSize="small" />
              </ListItemIcon>
              <ListItemText>Edit Cloud</ListItemText>
            </MenuItem>
            <Divider />
            <MenuItem onClick={() => handleContextMenuAction('delete')}>
              <ListItemIcon>
                <Delete fontSize="small" />
              </ListItemIcon>
              <ListItemText>Delete Cloud</ListItemText>
            </MenuItem>
          </>
        )}
        
        {contextMenu?.node?.type === 'datacenter' && (
          <>
            <MenuItem onClick={() => handleContextMenuAction('add-compute')}>
              <ListItemIcon>
                <Add fontSize="small" />
              </ListItemIcon>
              <ListItemText>Add Compute Node</ListItemText>
            </MenuItem>
            <MenuItem onClick={() => handleContextMenuAction('edit')}>
              <ListItemIcon>
                <Edit fontSize="small" />
              </ListItemIcon>
              <ListItemText>Edit Datacenter</ListItemText>
            </MenuItem>
            <Divider />
            <MenuItem onClick={() => handleContextMenuAction('delete')}>
              <ListItemIcon>
                <Delete fontSize="small" />
              </ListItemIcon>
              <ListItemText>Delete Datacenter</ListItemText>
            </MenuItem>
          </>
        )}
        
        {contextMenu?.node?.type === 'compute' && (
          <>
            <MenuItem onClick={() => handleContextMenuAction('edit')}>
              <ListItemIcon>
                <Edit fontSize="small" />
              </ListItemIcon>
              <ListItemText>Edit Compute Node</ListItemText>
            </MenuItem>
            <Divider />
            <MenuItem onClick={() => handleContextMenuAction('delete')}>
              <ListItemIcon>
                <Delete fontSize="small" />
              </ListItemIcon>
              <ListItemText>Delete Compute Node</ListItemText>
            </MenuItem>
          </>
        )}
      </Menu>
    </Box>
  );
};

export default InfrastructureWithTree;