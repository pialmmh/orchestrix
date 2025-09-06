import React, { useState, useEffect, ReactElement } from 'react';
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
  FormControlLabel,
  Checkbox,
  Stack,
  Tooltip,
  Breadcrumbs,
  Link,
} from '@mui/material';
import Grid from '@mui/material/Grid';
import {
  Add,
  Edit,
  Delete,
  Refresh,
  ExpandMore,
  ChevronRight,
  NavigateNext,
  Cloud as CloudIcon,
  Business,
  Computer,
  Storage as StorageIcon,
  Public as PublicIcon,
  LanOutlined as LanIcon,
  HubOutlined as HubIcon,
  Domain,
  Domain as DataCenterIcon,
  Settings as SettingsIcon,
  AccountTree as AccountTreeIcon,
} from '@mui/icons-material';
import { SimpleTreeView } from '@mui/x-tree-view/SimpleTreeView';
import { TreeItem } from '@mui/x-tree-view/TreeItem';
import axios from 'axios';

interface TreeNode {
  id: string;
  name: string;
  type: 'organization' | 'environment' | 'cloud' | 'region' | 'az' | 'datacenter' | 'pool' | 'compute' | 'container';
  data?: any;
  children?: TreeNode[];
  metadata?: {
    tier?: number;
    drPaired?: boolean;
    compliance?: string[];
    capabilities?: string[];
    utilization?: number;
  };
}

interface Partner {
  id: number;
  name: string;
  displayName: string;
  roles: string[];
}

const InfrastructureCloudNative: React.FC = () => {
  const [treeData, setTreeData] = useState<TreeNode[]>([]);
  const [selectedNodeId, setSelectedNodeId] = useState<string>('');
  const [selectedNode, setSelectedNode] = useState<TreeNode | null>(null);
  const [selectedNodePath, setSelectedNodePath] = useState<TreeNode[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [expanded, setExpanded] = useState<string[]>(['org-root', 'environments']);
  
  // Dialog states
  const [openDialog, setOpenDialog] = useState(false);
  const [dialogType, setDialogType] = useState<string | null>(null);
  const [editMode, setEditMode] = useState(false);
  const [formData, setFormData] = useState<any>({});
  const [partners, setPartners] = useState<Partner[]>([]);
  const [environments, setEnvironments] = useState<any[]>([]);
  const [regions, setRegions] = useState<any[]>([]);
  const [availabilityZones, setAvailabilityZones] = useState<any[]>([]);
  
  // Context menu state
  const [contextMenu, setContextMenu] = useState<{
    mouseX: number;
    mouseY: number;
    node: TreeNode | null;
  } | null>(null);

  useEffect(() => {
    fetchInfrastructureData();
    fetchPartners();
    fetchEnvironments();
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

  const fetchEnvironments = async () => {
    try {
      const response = await axios.get('/api/environments');
      setEnvironments(response.data || []);
    } catch (error) {
      // If environments endpoint doesn't exist yet, create default ones
      setEnvironments([
        { id: 1, name: 'Production', code: 'PROD', type: 'PRODUCTION' },
        { id: 2, name: 'Development', code: 'DEV', type: 'DEVELOPMENT' },
        { id: 3, name: 'Staging', code: 'STAGE', type: 'STAGING' }
      ]);
    }
  };

  const fetchInfrastructureData = async () => {
    setLoading(true);
    try {
      // Fetch all data in parallel
      const [cloudsRes, regionsRes, azsRes, datacentersRes, poolsRes, computesRes] = await Promise.allSettled([
        axios.get('/api/clouds'),
        axios.get('/api/regions').catch(() => ({ data: [] })),
        axios.get('/api/availability-zones').catch(() => ({ data: [] })),
        axios.get('/api/datacenters'),
        axios.get('/api/resource-pools').catch(() => ({ data: [] })),
        axios.get('/api/computes').catch(() => ({ data: [] }))
      ]);

      const clouds = cloudsRes.status === 'fulfilled' ? cloudsRes.value.data : [];
      const regions = regionsRes.status === 'fulfilled' ? regionsRes.value.data : [];
      const azs = azsRes.status === 'fulfilled' ? azsRes.value.data : [];
      const datacenters = datacentersRes.status === 'fulfilled' ? datacentersRes.value.data : [];
      const pools = poolsRes.status === 'fulfilled' ? poolsRes.value.data : [];
      const computes = computesRes.status === 'fulfilled' ? computesRes.value.data : [];

      // Build the hierarchical tree structure
      const tree: TreeNode[] = [
        {
          id: 'org-root',
          name: 'Organization',
          type: 'organization',
          children: [
            // Group by environments first
            {
              id: 'env-prod',
              name: 'Production Environment',
              type: 'environment',
              data: { type: 'PRODUCTION' },
              children: buildEnvironmentTree(clouds, regions, azs, datacenters, pools, computes, 'PRODUCTION')
            },
            {
              id: 'env-dev',
              name: 'Development Environment',
              type: 'environment',
              data: { type: 'DEVELOPMENT' },
              children: buildEnvironmentTree(clouds, regions, azs, datacenters, pools, computes, 'DEVELOPMENT')
            },
            {
              id: 'env-staging',
              name: 'Staging Environment',
              type: 'environment',
              data: { type: 'STAGING' },
              children: buildEnvironmentTree(clouds, regions, azs, datacenters, pools, computes, 'STAGING')
            }
          ]
        }
      ];
      
      setTreeData(tree);
      setRegions(regions);
      setAvailabilityZones(azs);
      setError(null);
    } catch (error) {
      console.error('Error fetching infrastructure data:', error);
      setTreeData([]);
    } finally {
      setLoading(false);
    }
  };

  const buildEnvironmentTree = (
    clouds: any[], 
    regions: any[], 
    azs: any[], 
    datacenters: any[], 
    pools: any[], 
    computes: any[],
    envType: string
  ): TreeNode[] => {
    // Filter datacenters by environment
    const envDatacenters = datacenters.filter(dc => 
      !dc.environment || dc.environment.type === envType
    );
    
    return clouds.map((cloud: any, cloudIndex: number) => ({
      id: `cloud-${cloud.id}-${envType}-${cloudIndex}`,
      name: cloud.name,
      type: 'cloud' as const,
      data: cloud,
      children: regions
        .filter(r => r.cloud?.id === cloud.id || r.cloudId === cloud.id)
        .map((region: any, regionIndex: number) => ({
          id: `region-${cloud.id}-${region.id}-${envType}`,
          name: region.name,
          type: 'region' as const,
          data: region,
          metadata: {
            compliance: region.complianceZones?.split(',') || []
          },
          children: azs
            .filter(az => az.region?.id === region.id || az.regionId === region.id)
            .map((az: any, azIndex: number) => ({
              id: `az-${region.id}-${az.id}-${envType}`,
              name: az.name,
              type: 'az' as const,
              data: az,
              metadata: {
                capabilities: az.capabilities?.split(',') || []
              },
              children: envDatacenters
                .filter(dc => dc.availabilityZone?.id === az.id || dc.availabilityZoneId === az.id)
                .map((dc: any, dcIndex: number) => ({
                  id: `datacenter-${az.id}-${dc.id}-${envType}`,
                  name: dc.name,
                  type: 'datacenter' as const,
                  data: dc,
                  metadata: {
                    tier: dc.tier || 3,
                    drPaired: dc.drPairedDatacenter != null,
                    utilization: dc.utilization || 0
                  },
                  children: pools
                    .filter(p => p.datacenter?.id === dc.id || p.datacenterId === dc.id)
                    .map((pool: any, poolIndex: number) => ({
                      id: `pool-${dc.id}-${pool.id}-${envType}`,
                      name: pool.name,
                      type: 'pool' as const,
                      data: pool,
                      metadata: {
                        utilization: pool.cpuUtilizationPercent || 0
                      },
                      children: computes
                        .filter(c => c.resourcePool?.id === pool.id || c.resourcePoolId === pool.id)
                        .map((compute: any, compIndex: number) => ({
                          id: `compute-${pool.id}-${compute.id}-${envType}`,
                          name: compute.name || compute.hostname,
                          type: 'compute' as const,
                          data: compute,
                          children: []
                        }))
                    }))
                }))
            }))
        }))
    }));
  };

  const getChildTypeForNode = (nodeType: string): string | null => {
    const childTypeMap: { [key: string]: string } = {
      'organization': 'environment',
      'environment': 'cloud',
      'cloud': 'region',
      'region': 'availability-zone',
      'az': 'datacenter',
      'datacenter': 'resource-pool',
      'pool': 'compute',
      'compute': 'container'
    };
    return childTypeMap[nodeType] || null;
  };

  const buildNodePath = (nodes: TreeNode[], targetId: string, path: TreeNode[] = []): TreeNode[] | null => {
    for (const node of nodes) {
      if (node.id === targetId) {
        return [...path, node];
      }
      if (node.children) {
        const result = buildNodePath(node.children, targetId, [...path, node]);
        if (result) return result;
      }
    }
    return null;
  };

  const handleNodeSelect = (event: React.SyntheticEvent | null, nodeIds: string | string[] | null) => {
    const nodeId = Array.isArray(nodeIds) ? nodeIds[0] : nodeIds;
    if (nodeId) {
      setSelectedNodeId(nodeId);
      const node = findNodeById(treeData, nodeId);
      setSelectedNode(node);
      
      // Build the path to the selected node
      const path = buildNodePath(treeData, nodeId);
      setSelectedNodePath(path || []);
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

  const handleContextMenu = (event: React.MouseEvent, node: TreeNode) => {
    event.preventDefault();
    event.stopPropagation();
    setContextMenu({
      mouseX: event.clientX + 2,
      mouseY: event.clientY - 6,
      node,
    });
  };

  const handleCloseContextMenu = () => {
    setContextMenu(null);
  };

  const handleAdd = (type: string, parentNode?: TreeNode) => {
    setDialogType(type);
    setEditMode(false);
    
    if (parentNode) {
      setSelectedNode(parentNode);
      setSelectedNodeId(parentNode.id);
    }
    
    setFormData(getDefaultFormData(type));
    setOpenDialog(true);
  };

  const handleContextMenuAction = (action: string) => {
    if (!contextMenu?.node) return;
    
    const node = contextMenu.node;
    setSelectedNode(node);
    setSelectedNodeId(node.id);
    
    const actionMap: { [key: string]: () => void } = {
      'add-cloud': () => handleAdd('cloud'),
      'add-region': () => handleAdd('region', node),
      'add-az': () => handleAdd('availability-zone', node),
      'add-datacenter': () => handleAdd('datacenter', node),
      'add-pool': () => handleAdd('resource-pool', node),
      'add-compute': () => handleAdd('compute', node),
      'edit': () => {
        setDialogType(node.type);
        setEditMode(true);
        setFormData(node.data);
        setOpenDialog(true);
      },
      'delete': () => handleDelete(),
      'set-dr': () => handleSetDRPairing(node),
    };
    
    if (actionMap[action]) {
      actionMap[action]();
    }
    
    handleCloseContextMenu();
  };

  const handleSetDRPairing = (node: TreeNode) => {
    // TODO: Implement DR pairing dialog
    console.log('Setting DR pairing for', node);
  };

  const handleDelete = async () => {
    if (!selectedNode || !selectedNode.data) return;
    
    if (!window.confirm(`Are you sure you want to delete ${selectedNode.name}?`)) {
      return;
    }

    try {
      const endpointMap: { [key: string]: string } = {
        'cloud': `/api/clouds/${selectedNode.data.id}`,
        'region': `/api/regions/${selectedNode.data.id}`,
        'az': `/api/availability-zones/${selectedNode.data.id}`,
        'datacenter': `/api/datacenters/${selectedNode.data.id}`,
        'pool': `/api/resource-pools/${selectedNode.data.id}`,
        'compute': `/api/computes/${selectedNode.data.id}`,
      };
      
      const endpoint = endpointMap[selectedNode.type];
      if (endpoint) {
        await axios.delete(endpoint);
        await fetchInfrastructureData();
        setSelectedNode(null);
        setSelectedNodeId('');
      }
    } catch (error) {
      console.error('Error deleting:', error);
      alert('Failed to delete item');
    }
  };

  const handleSave = async () => {
    try {
      let endpoint = '';
      let payload = { ...formData };
      
      // Map dialog type to API endpoint and prepare payload
      const saveConfig: { [key: string]: any } = {
        'cloud': {
          endpoint: editMode ? `/api/clouds/${formData.id}` : '/api/clouds',
          prepare: () => payload
        },
        'region': {
          endpoint: editMode ? `/api/regions/${formData.id}` : '/api/regions',
          prepare: () => {
            if (!editMode && selectedNode?.type === 'cloud') {
              payload.cloudId = selectedNode.data.id;
            }
            return payload;
          }
        },
        'availability-zone': {
          endpoint: editMode ? `/api/availability-zones/${formData.id}` : '/api/availability-zones',
          prepare: () => {
            if (!editMode && selectedNode?.type === 'region') {
              payload.regionId = selectedNode.data.id;
            }
            return payload;
          }
        },
        'datacenter': {
          endpoint: editMode ? `/api/datacenters/${formData.id}` : '/api/datacenters',
          prepare: () => {
            if (!editMode && selectedNode?.type === 'az') {
              payload.availabilityZoneId = selectedNode.data.id;
            }
            return payload;
          }
        },
        'resource-pool': {
          endpoint: editMode ? `/api/resource-pools/${formData.id}` : '/api/resource-pools',
          prepare: () => {
            if (!editMode && selectedNode?.type === 'datacenter') {
              payload.datacenterId = selectedNode.data.id;
            }
            return payload;
          }
        },
        'compute': {
          endpoint: editMode ? `/api/computes/${formData.id}` : '/api/computes',
          prepare: () => {
            if (!editMode && selectedNode?.type === 'pool') {
              payload.resourcePoolId = selectedNode.data.id;
            }
            return payload;
          }
        }
      };
      
      const config = saveConfig[dialogType || ''];
      if (!config) return;
      
      payload = config.prepare();
      
      if (editMode) {
        await axios.put(config.endpoint, payload);
      } else {
        await axios.post(config.endpoint, payload);
      }
      
      setOpenDialog(false);
      await fetchInfrastructureData();
    } catch (error) {
      console.error('Error saving:', error);
      alert('Failed to save item');
    }
  };

  const getDefaultFormData = (type: string) => {
    const defaults: { [key: string]: any } = {
      'cloud': {
        name: '',
        description: '',
        partnerId: null,
        deploymentRegion: '',
        status: 'ACTIVE'
      },
      'region': {
        name: '',
        code: '',
        geographicArea: '',
        complianceZones: '',
        description: '',
        status: 'ACTIVE'
      },
      'availability-zone': {
        name: '',
        code: '',
        zoneType: 'STANDARD',
        isDefault: false,
        capabilities: '',
        status: 'ACTIVE'
      },
      'datacenter': {
        name: '',
        type: 'PRIMARY',
        tier: 3,
        provider: '',
        isDrSite: false,
        environmentId: null,
        status: 'ACTIVE'
      },
      'resource-pool': {
        name: '',
        type: 'COMPUTE',
        hypervisor: '',
        orchestrator: '',
        totalCpuCores: 0,
        totalMemoryGb: 0,
        totalStorageTb: 0,
        status: 'ACTIVE'
      },
      'compute': {
        name: '',
        hostname: '',
        ipAddress: '',
        nodeType: 'DEDICATED',
        hypervisor: '',
        isPhysical: false,
        cpuCores: 0,
        memoryGb: 0,
        diskGb: 0,
        status: 'ACTIVE'
      }
    };
    
    return defaults[type] || {};
  };

  const renderTreeItem = (node: TreeNode) => {
    const iconMap: { [key: string]: ReactElement } = {
      'organization': <Business />,
      'environment': <AccountTreeIcon />,
      'cloud': <CloudIcon />,
      'region': <PublicIcon />,
      'az': <LanIcon />,
      'datacenter': <DataCenterIcon />,
      'pool': <HubIcon />,
      'compute': <Computer />,
      'container': <StorageIcon />
    };
    
    const icon = iconMap[node.type] || <ChevronRight />;
    
    return (
      <TreeItem
        key={node.id}
        itemId={node.id}
        label={
          <Box 
            sx={{ display: 'flex', alignItems: 'center', py: 0.5, gap: 1 }}
            onContextMenu={(e) => handleContextMenu(e, node)}
          >
            {icon}
            <Typography sx={{ flexGrow: 1 }}>{node.name}</Typography>
            {node.metadata?.tier && (
              <Chip label={`Tier ${node.metadata.tier}`} size="small" />
            )}
            {node.metadata?.drPaired && (
              <Chip label="DR" size="small" color="warning" />
            )}
            {node.metadata?.utilization !== undefined && (
              <Chip 
                label={`${node.metadata.utilization}%`} 
                size="small" 
                color={node.metadata.utilization > 80 ? 'error' : 'success'}
              />
            )}
          </Box>
        }
      >
        {node.children?.map(child => renderTreeItem(child))}
      </TreeItem>
    );
  };

  const renderDetailView = () => {
    if (!selectedNode) {
      return (
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Select an item from the tree
            </Typography>
            <Typography color="text.secondary">
              Click on any node in the tree to view details, or right-click to see available actions.
            </Typography>
          </CardContent>
        </Card>
      );
    }

    return (
      <Card>
        <CardContent>
          {/* Breadcrumb navigation */}
          <Breadcrumbs 
            separator={<NavigateNext fontSize="small" />} 
            sx={{ mb: 2 }}
          >
            {selectedNodePath.map((pathNode, index) => (
              <Link
                key={pathNode.id}
                component="button"
                variant="body2"
                onClick={() => {
                  setSelectedNodeId(pathNode.id);
                  setSelectedNode(pathNode);
                  const newPath = selectedNodePath.slice(0, index + 1);
                  setSelectedNodePath(newPath);
                }}
                underline="hover"
                color={index === selectedNodePath.length - 1 ? 'text.primary' : 'inherit'}
                sx={{ fontWeight: index === selectedNodePath.length - 1 ? 'bold' : 'normal' }}
              >
                {pathNode.name}
              </Link>
            ))}
          </Breadcrumbs>
          
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h5">
              {selectedNode.name}
              <Typography component="span" variant="h6" color="text.secondary" sx={{ ml: 1 }}>
                ({(() => {
                  const typeMap: { [key: string]: string } = {
                    'organization': 'Organization',
                    'environment': 'Environment',
                    'cloud': 'Cloud',
                    'region': 'Region',
                    'az': 'Availability Zone',
                    'datacenter': 'Datacenter',
                    'pool': 'Resource Pool',
                    'compute': 'Compute Node',
                    'container': 'Container'
                  };
                  return typeMap[selectedNode.type] || selectedNode.type.charAt(0).toUpperCase() + selectedNode.type.slice(1);
                })()})
              </Typography>
            </Typography>
            <Stack direction="row" spacing={1}>
              <IconButton 
                size="small" 
                onClick={() => {
                  setDialogType(selectedNode.type);
                  setEditMode(true);
                  setFormData(selectedNode.data);
                  setOpenDialog(true);
                }}
                title="Edit"
              >
                <Edit />
              </IconButton>
              <IconButton size="small" onClick={handleDelete} title="Delete">
                <Delete />
              </IconButton>
            </Stack>
          </Box>
          
          <Divider sx={{ my: 2 }} />
          
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Path: {selectedNodePath.map(n => n.name).join(' / ')}
          </Typography>
          
          {/* Add child section - separate from entity actions */}
          {getChildTypeForNode(selectedNode.type) && (
            <Box sx={{ mb: 2, p: 2, bgcolor: 'action.hover', borderRadius: 1 }}>
              <Typography variant="subtitle2" gutterBottom>
                Child Resources
              </Typography>
              <Button
                variant="outlined"
                size="small"
                startIcon={<Add />}
                onClick={() => {
                  const childType = getChildTypeForNode(selectedNode.type);
                  if (childType) {
                    handleAdd(childType, selectedNode);
                  }
                }}
                sx={{ mt: 1 }}
              >
                Add {getChildTypeForNode(selectedNode.type)?.replace('-', ' ')}
              </Button>
            </Box>
          )}
          
          {selectedNode.data && (
            <Box sx={{ mt: 2 }}>
              {Object.entries(selectedNode.data).map(([key, value]) => (
                <Box key={key} sx={{ py: 0.5 }}>
                  <Typography variant="body2">
                    <strong>{key}:</strong> {String(value)}
                  </Typography>
                </Box>
              ))}
            </Box>
          )}
          
          {selectedNode.metadata && (
            <Box sx={{ mt: 2 }}>
              <Typography variant="subtitle2" gutterBottom>Metadata</Typography>
              {selectedNode.metadata.compliance && (
                <Box sx={{ py: 0.5 }}>
                  <Typography variant="body2">
                    <strong>Compliance:</strong> {selectedNode.metadata.compliance.join(', ')}
                  </Typography>
                </Box>
              )}
              {selectedNode.metadata.capabilities && (
                <Box sx={{ py: 0.5 }}>
                  <Typography variant="body2">
                    <strong>Capabilities:</strong> {selectedNode.metadata.capabilities.join(', ')}
                  </Typography>
                </Box>
              )}
            </Box>
          )}
        </CardContent>
      </Card>
    );
  };

  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
        <Typography variant="h4" gutterBottom>
          Cloud Infrastructure Management
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Hierarchical view of your cloud-native infrastructure across environments, regions, and availability zones
        </Typography>
      </Box>

      <Grid container sx={{ flexGrow: 1, overflow: 'hidden' }}>
        <Grid size={{ xs: 12, md: 4 }} sx={{ borderRight: 1, borderColor: 'divider', overflow: 'auto', height: '100%' }}>
          <Box sx={{ p: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Typography variant="h6">Infrastructure Tree</Typography>
              <IconButton size="small" onClick={fetchInfrastructureData}>
                <Refresh />
              </IconButton>
            </Box>
            
            {error && (
              <Alert severity="error" sx={{ mb: 2 }}>
                {error}
              </Alert>
            )}
            
            <SimpleTreeView
              onSelectedItemsChange={handleNodeSelect}
              expandedItems={expanded}
              onExpandedItemsChange={handleToggle}
            >
              {treeData.map(node => renderTreeItem(node))}
            </SimpleTreeView>
          </Box>
        </Grid>
        
        <Grid size={{ xs: 12, md: 8 }} sx={{ overflow: 'auto', height: '100%' }}>
          <Box sx={{ p: 2 }}>
            {renderDetailView()}
          </Box>
        </Grid>
      </Grid>

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
        {contextMenu?.node?.type === 'environment' && (
          <MenuItem onClick={() => handleContextMenuAction('add-cloud')}>
            <ListItemIcon><Add fontSize="small" /></ListItemIcon>
            <ListItemText>Add Cloud Provider</ListItemText>
          </MenuItem>
        )}
        
        {contextMenu?.node?.type === 'cloud' && (
          <>
            <MenuItem onClick={() => handleContextMenuAction('add-region')}>
              <ListItemIcon><Add fontSize="small" /></ListItemIcon>
              <ListItemText>Add Region</ListItemText>
            </MenuItem>
            <MenuItem onClick={() => handleContextMenuAction('edit')}>
              <ListItemIcon><Edit fontSize="small" /></ListItemIcon>
              <ListItemText>Edit Cloud</ListItemText>
            </MenuItem>
            <Divider />
            <MenuItem onClick={() => handleContextMenuAction('delete')}>
              <ListItemIcon><Delete fontSize="small" /></ListItemIcon>
              <ListItemText>Delete Cloud</ListItemText>
            </MenuItem>
          </>
        )}
        
        {contextMenu?.node?.type === 'region' && (
          <>
            <MenuItem onClick={() => handleContextMenuAction('add-az')}>
              <ListItemIcon><Add fontSize="small" /></ListItemIcon>
              <ListItemText>Add Availability Zone</ListItemText>
            </MenuItem>
            <MenuItem onClick={() => handleContextMenuAction('edit')}>
              <ListItemIcon><Edit fontSize="small" /></ListItemIcon>
              <ListItemText>Edit Region</ListItemText>
            </MenuItem>
            <Divider />
            <MenuItem onClick={() => handleContextMenuAction('delete')}>
              <ListItemIcon><Delete fontSize="small" /></ListItemIcon>
              <ListItemText>Delete Region</ListItemText>
            </MenuItem>
          </>
        )}
        
        {contextMenu?.node?.type === 'az' && (
          <>
            <MenuItem onClick={() => handleContextMenuAction('add-datacenter')}>
              <ListItemIcon><Add fontSize="small" /></ListItemIcon>
              <ListItemText>Add Datacenter</ListItemText>
            </MenuItem>
            <MenuItem onClick={() => handleContextMenuAction('edit')}>
              <ListItemIcon><Edit fontSize="small" /></ListItemIcon>
              <ListItemText>Edit Availability Zone</ListItemText>
            </MenuItem>
            <Divider />
            <MenuItem onClick={() => handleContextMenuAction('delete')}>
              <ListItemIcon><Delete fontSize="small" /></ListItemIcon>
              <ListItemText>Delete Availability Zone</ListItemText>
            </MenuItem>
          </>
        )}
        
        {contextMenu?.node?.type === 'datacenter' && (
          <>
            <MenuItem onClick={() => handleContextMenuAction('add-pool')}>
              <ListItemIcon><Add fontSize="small" /></ListItemIcon>
              <ListItemText>Add Resource Pool</ListItemText>
            </MenuItem>
            <MenuItem onClick={() => handleContextMenuAction('set-dr')}>
              <ListItemIcon><SettingsIcon fontSize="small" /></ListItemIcon>
              <ListItemText>Set DR Pairing</ListItemText>
            </MenuItem>
            <MenuItem onClick={() => handleContextMenuAction('edit')}>
              <ListItemIcon><Edit fontSize="small" /></ListItemIcon>
              <ListItemText>Edit Datacenter</ListItemText>
            </MenuItem>
            <Divider />
            <MenuItem onClick={() => handleContextMenuAction('delete')}>
              <ListItemIcon><Delete fontSize="small" /></ListItemIcon>
              <ListItemText>Delete Datacenter</ListItemText>
            </MenuItem>
          </>
        )}
        
        {contextMenu?.node?.type === 'pool' && (
          <>
            <MenuItem onClick={() => handleContextMenuAction('add-compute')}>
              <ListItemIcon><Add fontSize="small" /></ListItemIcon>
              <ListItemText>Add Compute Node</ListItemText>
            </MenuItem>
            <MenuItem onClick={() => handleContextMenuAction('edit')}>
              <ListItemIcon><Edit fontSize="small" /></ListItemIcon>
              <ListItemText>Edit Resource Pool</ListItemText>
            </MenuItem>
            <Divider />
            <MenuItem onClick={() => handleContextMenuAction('delete')}>
              <ListItemIcon><Delete fontSize="small" /></ListItemIcon>
              <ListItemText>Delete Resource Pool</ListItemText>
            </MenuItem>
          </>
        )}
        
        {contextMenu?.node?.type === 'compute' && (
          <>
            <MenuItem onClick={() => handleContextMenuAction('edit')}>
              <ListItemIcon><Edit fontSize="small" /></ListItemIcon>
              <ListItemText>Edit Compute Node</ListItemText>
            </MenuItem>
            <Divider />
            <MenuItem onClick={() => handleContextMenuAction('delete')}>
              <ListItemIcon><Delete fontSize="small" /></ListItemIcon>
              <ListItemText>Delete Compute Node</ListItemText>
            </MenuItem>
          </>
        )}
      </Menu>

      {/* Dialog for Add/Edit - Simplified for brevity */}
      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="sm">
        <DialogTitle>
          {editMode ? 'Edit' : 'Add'} {dialogType?.replace('-', ' ').replace(/\b\w/g, l => l.toUpperCase())}
        </DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 2, px: 2, minWidth: 400 }}>
            <TextField
              label="Name"
              value={formData.name || ''}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              required
            />
            
            {/* Add type-specific fields here based on dialogType */}
            {dialogType === 'region' && (
              <>
                <TextField
                  label="Code"
                  value={formData.code || ''}
                  onChange={(e) => setFormData({ ...formData, code: e.target.value })}
                      required
                />
                <TextField
                  label="Geographic Area"
                  value={formData.geographicArea || ''}
                  onChange={(e) => setFormData({ ...formData, geographicArea: e.target.value })}
                    />
                <TextField
                  label="Compliance Zones (comma-separated)"
                  value={formData.complianceZones || ''}
                  onChange={(e) => setFormData({ ...formData, complianceZones: e.target.value })}
                      placeholder="GDPR, HIPAA, SOC2"
                />
              </>
            )}
            
            {dialogType === 'datacenter' && (
              <>
                <FormControl fullWidth>
                  <InputLabel>Type</InputLabel>
                  <Select
                    value={formData.type || 'PRIMARY'}
                    onChange={(e) => setFormData({ ...formData, type: e.target.value })}
                    label="Type"
                  >
                    <MenuItem value="PRIMARY">Primary</MenuItem>
                    <MenuItem value="SECONDARY">Secondary</MenuItem>
                    <MenuItem value="DR">Disaster Recovery</MenuItem>
                    <MenuItem value="EDGE">Edge</MenuItem>
                  </Select>
                </FormControl>
                <FormControl fullWidth>
                  <InputLabel>Tier</InputLabel>
                  <Select
                    value={formData.tier || 3}
                    onChange={(e) => setFormData({ ...formData, tier: e.target.value })}
                    label="Tier"
                  >
                    <MenuItem value={1}>Tier 1 (99.995% uptime)</MenuItem>
                    <MenuItem value={2}>Tier 2 (99.982% uptime)</MenuItem>
                    <MenuItem value={3}>Tier 3 (99.982% uptime)</MenuItem>
                    <MenuItem value={4}>Tier 4 (99.671% uptime)</MenuItem>
                  </Select>
                </FormControl>
                <FormControlLabel
                  control={
                    <Checkbox
                      checked={formData.isDrSite || false}
                      onChange={(e) => setFormData({ ...formData, isDrSite: e.target.checked })}
                    />
                  }
                  label="Is DR Site"
                />
              </>
            )}
            
            <TextField
              label="Description"
              value={formData.description || ''}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              multiline
              rows={2}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button onClick={handleSave} variant="contained">
            {editMode ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default InfrastructureCloudNative;