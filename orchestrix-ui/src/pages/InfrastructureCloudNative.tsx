import React, { useState, useEffect } from 'react';
import { observer } from 'mobx-react-lite';
import { useOrganizationInfraStore } from '../stores/base/StoreProvider';
import { TreeNode as StellarTreeNode } from '../models/entities/TreeNode';
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
  Tabs,
  Tab,
  ToggleButton,
  ToggleButtonGroup,
  Menu,
  MenuItem,
  Divider,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Tooltip,
} from '@mui/material';
import {
  Refresh,
  NavigateNext,
  Cloud as CloudIcon,
  Business,
  Computer,
  Public as PublicIcon,
  RouterOutlined as RouterIcon,
  Domain as DataCenterIcon,
  Settings as SettingsIcon,
  AccountTree as AccountTreeIcon,
  DataObject,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Add,
  ExpandMore,
  ChevronRight,
} from '@mui/icons-material';
import { TreeView } from '@mui/x-tree-view/TreeView';
import { TreeItem } from '@mui/x-tree-view/TreeItem';
import axios from 'axios';
import ComputeEditDialog from '../components/ComputeEditDialog';
import NetworkDeviceEditDialog from '../components/NetworkDeviceEditDialog';
import config from '../config';

interface TreeNode {
  id: string;
  name: string;
  type: 'organization' | 'environment' | 'cloud' | 'region' | 'az' | 'datacenter' | 'pool' | 'resourcepool' | 'compute' | 'container' | 'resource-group' | 'service' | 'network-device' | 'partner';
  data?: any;
  children?: TreeNode[];
  metadata?: {
    tier?: number;
    drPaired?: boolean;
    compliance?: string[];
    capabilities?: string[];
    utilization?: number;
    category?: string;
    icon?: string;
    color?: string;
    resourceGroup?: string;
    hostname?: string;
    ipAddress?: string;
  };
}

interface Partner {
  id: number;
  name: string;
  displayName: string;
  roles: string[];
}

const InfrastructureCloudNative: React.FC = () => {
  const organizationInfraStore = useOrganizationInfraStore();
  const [treeData, setTreeData] = useState<TreeNode[]>([]);
  const [selectedNodeId, setSelectedNodeId] = useState<string>('');
  const [selectedNode, setSelectedNode] = useState<TreeNode | null>(null);
  const [selectedNodePath, setSelectedNodePath] = useState<TreeNode[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [expanded, setExpanded] = useState<string[]>(['org-root', 'environments']);
  
  // Tenant state - default to organization to show self partner's infrastructure
  const [tenant, setTenant] = useState<'organization' | 'partners'>('organization');
  
  // Dialog states
  const [openDialog, setOpenDialog] = useState(false);
  const [dialogType, setDialogType] = useState<string | null>(null);
  const [editMode, setEditMode] = useState(false);
  const [formData, setFormData] = useState<any>({});
  const [partners, setPartners] = useState<Partner[]>([]);
  const [environments, setEnvironments] = useState<any[]>([]);
  const [regions, setRegions] = useState<any[]>([]);
  const [availabilityZones, setAvailabilityZones] = useState<any[]>([]);
  const [viewerTabIndex, setViewerTabIndex] = useState(0);
  const [jsonModalOpen, setJsonModalOpen] = useState(false);
  
  // Compute and Network Device Edit Dialog states
  const [openComputeEditDialog, setOpenComputeEditDialog] = useState(false);
  const [computeEditData, setComputeEditData] = useState<any>(null);
  const [openNetworkDeviceEditDialog, setOpenNetworkDeviceEditDialog] = useState(false);
  const [networkDeviceEditData, setNetworkDeviceEditData] = useState<any>(null);

  // Context menu state
  const [contextMenu, setContextMenu] = useState<{
    anchorEl: HTMLElement | null;
    node: TreeNode | null;
  }>({ anchorEl: null, node: null });

  // Environment filter state
  const [environmentFilter, setEnvironmentFilter] = useState<string | null>(null);

  useEffect(() => {
    console.log('🏗️ InfrastructureCloudNative mounted, tenant:', tenant);
    config.printConfiguration();
    fetchInfrastructureData();
    fetchPartners();
    fetchEnvironments();
  }, [tenant]);

  useEffect(() => {
    setViewerTabIndex(0);
  }, [selectedNodeId]);

  const fetchPartners = async () => {
    // Partners are loaded with the infrastructure tree in Stellar
    const partnersFromStore = organizationInfraStore.partners;
    setPartners(partnersFromStore);
  };

  const fetchEnvironments = async () => {
    // Environments are loaded with the infrastructure tree in Stellar
    const environmentsFromStore = organizationInfraStore.environments;
    if (environmentsFromStore && environmentsFromStore.length > 0) {
      setEnvironments(environmentsFromStore);
    } else {
      // Fallback to default environments
      setEnvironments([
        { id: 1, name: 'Production', code: 'PROD', type: 'PRODUCTION' },
        { id: 2, name: 'Development', code: 'DEV', type: 'DEVELOPMENT' },
        { id: 3, name: 'Staging', code: 'STAGE', type: 'STAGING' }
      ]);
    }
  };

  const handleEnvironmentFilterChange = (env: string | null) => {
    setEnvironmentFilter(env);
    organizationInfraStore.setEnvironmentFilter(env);
    // Refresh tree display
    const displayData = organizationInfraStore.displayTreeData;
    setTreeData(displayData);
  };

  const fetchInfrastructureData = async () => {
    setLoading(true);
    try {
      console.log('🔍 fetchInfrastructureData - using Stellar store, tenant:', tenant);
      console.log('🔍 Starting infrastructure data load...');
      console.log('🔍 Current time:', new Date().toISOString());

      // Use Stellar store to load infrastructure tree
      // Map 'organization' to 'self' for the store
      // Map 'partners' to 'all' to show all partners
      const storeParam = tenant === 'organization' ? 'self' : tenant === 'partners' ? 'all' : tenant;
      await organizationInfraStore.loadInfrastructureTree(storeParam);

      // Get data from the store
      const treeDataFromStore = organizationInfraStore.treeData;
      const partnersFromStore = organizationInfraStore.partners;

      console.log('🔍 fetchInfrastructureData - tree from store:', treeDataFromStore);
      console.log('🔍 fetchInfrastructureData - partners from store:', partnersFromStore);

      // Update local state with data from Stellar store
      // Get filtered data if environment filter is active
      const displayData = organizationInfraStore.displayTreeData;
      setTreeData(displayData);
      setPartners(partnersFromStore);
      setError(null);

      // Synchronize expanded state with store's expandedNodeIds
      setExpanded([...organizationInfraStore.expandedNodeIds]);
    } catch (error) {
      console.error('❌ Error fetching infrastructure data:', error);
      setTreeData([]);
    } finally {
      setLoading(false);
    }
  };

  const buildTenantTree = (
    tenantType: 'organization' | 'partners',
    partners: Partner[],
    clouds: any[], 
    regions: any[], 
    azs: any[], 
    datacenters: any[], 
    pools: any[], 
    computes: any[],
    networkDevices: any[],
    resourceGroups: any[]
  ): TreeNode[] => {
    console.log('🌳 buildTenantTree called with:');
    console.log('  - tenantType:', tenantType);
    console.log('  - partners:', partners);
    console.log('  - clouds:', clouds.length);
    console.log('  - regions:', regions.length);
    console.log('  - azs:', azs.length);
    console.log('  - datacenters:', datacenters.length);
    
    if (tenantType === 'organization') {
      // Show Telcobright's own infrastructure (partners with 'self' role)
      const selfPartners = partners.filter(p => p.roles && p.roles.includes('self'));
      console.log('🏢 Organization mode - selfPartners:', selfPartners);
      
      if (selfPartners.length === 0) {
        console.log('⚠️ No self partners found, returning empty tree');
        return [];
      }
      
      return selfPartners.map(partner => ({
        id: `partner-${partner.id}`,
        name: partner.displayName,
        type: 'organization' as const,
        data: partner,
        children: [
          {
            id: `${partner.id}-env-prod`,
            name: 'Production Environment',
            type: 'environment' as const,
            data: { type: 'PRODUCTION', partnerId: partner.id },
            children: buildEnvironmentTree(clouds, regions, azs, datacenters, pools, computes, networkDevices, resourceGroups, 'PRODUCTION', partner.id)
          },
          {
            id: `${partner.id}-env-dev`,
            name: 'Development Environment', 
            type: 'environment' as const,
            data: { type: 'DEVELOPMENT', partnerId: partner.id },
            children: buildEnvironmentTree(clouds, regions, azs, datacenters, pools, computes, networkDevices, resourceGroups, 'DEVELOPMENT', partner.id)
          },
          {
            id: `${partner.id}-env-staging`,
            name: 'Staging Environment',
            type: 'environment' as const,
            data: { type: 'STAGING', partnerId: partner.id },
            children: buildEnvironmentTree(clouds, regions, azs, datacenters, pools, computes, networkDevices, resourceGroups, 'STAGING', partner.id)
          }
        ]
      }));
    } else {
      // Show other partners' infrastructure (partners without 'self' role)
      const otherPartners = partners.filter(p => !p.roles.includes('self'));
      
      if (otherPartners.length === 0) {
        // No partners available - show infrastructure directly
        console.log('⚠️ No partners found, showing infrastructure directly');
        
        // Create a generic partner structure to hold the infrastructure
        return [{
          id: 'partners-root',
          name: 'Partners Infrastructure',
          type: 'organization' as const,
          data: { type: 'partners-root' },
          children: [
            {
              id: 'env-prod',
              name: 'Production Environment',
              type: 'environment' as const,
              data: { type: 'PRODUCTION' },
              children: buildEnvironmentTree(clouds, regions, azs, datacenters, pools, computes, networkDevices, resourceGroups, 'PRODUCTION')
            },
            {
              id: 'env-dev',
              name: 'Development Environment',
              type: 'environment' as const,
              data: { type: 'DEVELOPMENT' },
              children: buildEnvironmentTree(clouds, regions, azs, datacenters, pools, computes, networkDevices, resourceGroups, 'DEVELOPMENT')
            },
            {
              id: 'env-staging',
              name: 'Staging Environment',
              type: 'environment' as const,
              data: { type: 'STAGING' },
              children: buildEnvironmentTree(clouds, regions, azs, datacenters, pools, computes, networkDevices, resourceGroups, 'STAGING')
            }
          ]
        }];
      }
      
      return otherPartners.map(partner => ({
        id: `partner-${partner.id}`,
        name: `${partner.displayName}${partner.roles.includes('customer') ? ' (Customer)' : partner.roles.includes('vendor') ? ' (Vendor)' : ''}`,
        type: 'organization' as const,
        data: partner,
        children: [
          {
            id: `${partner.id}-env-prod`,
            name: 'Production Environment',
            type: 'environment' as const,
            data: { type: 'PRODUCTION', partnerId: partner.id },
            children: buildEnvironmentTree(clouds, regions, azs, datacenters, pools, computes, networkDevices, resourceGroups, 'PRODUCTION', partner.id)
          },
          {
            id: `${partner.id}-env-dev`,
            name: 'Development Environment',
            type: 'environment' as const,
            data: { type: 'DEVELOPMENT', partnerId: partner.id },
            children: buildEnvironmentTree(clouds, regions, azs, datacenters, pools, computes, networkDevices, resourceGroups, 'DEVELOPMENT', partner.id)
          },
          {
            id: `${partner.id}-env-staging`,
            name: 'Staging Environment',
            type: 'environment' as const,
            data: { type: 'STAGING', partnerId: partner.id },
            children: buildEnvironmentTree(clouds, regions, azs, datacenters, pools, computes, networkDevices, resourceGroups, 'STAGING', partner.id)
          }
        ]
      }));
    }
  };

  const buildServiceChildren = (
    serviceName: string, 
    resourceGroupName: string, 
    datacenterId: number,
    computes: any[],
    networkDevices: any[]
  ): TreeNode[] => {
    const children: TreeNode[] = [];
    
    if (serviceName === 'Compute') {
      const dcComputes = computes.filter(c => c.datacenterId === datacenterId);
      children.push(...dcComputes.map((compute: any) => ({
        id: `compute-${compute.id}`,
        name: compute.name || compute.hostname || `Compute-${compute.id}`,
        type: 'compute' as const,
        data: compute,
        metadata: {
          hostname: compute.hostname,
          ipAddress: compute.primaryIpAddress
        },
        children: []
      })));
    }
    
    if (serviceName === 'Router' && resourceGroupName === 'network_element') {
      const dcNetworkDevices = networkDevices.filter(nd => nd.datacenterId === datacenterId && nd.deviceType === 'Router');
      children.push(...dcNetworkDevices.map((device: any) => ({
        id: `network-device-${device.id}`,
        name: device.displayName || device.name || `Router-${device.id}`,
        type: 'network-device' as const,
        data: device,
        metadata: {
          hostname: device.managementIp,
          ipAddress: device.managementIp
        },
        children: []
      })));
    }
    
    if (serviceName === 'Switch' && resourceGroupName === 'network_element') {
      const dcNetworkDevices = networkDevices.filter(nd => nd.datacenterId === datacenterId && nd.deviceType === 'Switch');
      children.push(...dcNetworkDevices.map((device: any) => ({
        id: `network-device-${device.id}`,
        name: device.displayName || device.name || `Switch-${device.id}`,
        type: 'network-device' as const,
        data: device,
        metadata: {
          hostname: device.managementIp,
          ipAddress: device.managementIp
        },
        children: []
      })));
    }
    
    if (serviceName === 'Firewall' && resourceGroupName === 'network_element') {
      const dcNetworkDevices = networkDevices.filter(nd => nd.datacenterId === datacenterId && nd.deviceType === 'Firewall');
      children.push(...dcNetworkDevices.map((device: any) => ({
        id: `network-device-${device.id}`,
        name: device.displayName || device.name || `Firewall-${device.id}`,
        type: 'network-device' as const,
        data: device,
        metadata: {
          hostname: device.managementIp,
          ipAddress: device.managementIp
        },
        children: []
      })));
    }
    
    return children;
  };

  const buildEnvironmentTree = (
    clouds: any[], 
    regions: any[], 
    azs: any[], 
    datacenters: any[], 
    pools: any[], 
    computes: any[],
    networkDevices: any[],
    resourceGroups: any[],
    envType: string,
    partnerId?: number | null
  ): TreeNode[] => {
    const partnerClouds = partnerId 
      ? clouds.filter(cloud => cloud.partner && cloud.partner.id === partnerId)
      : clouds;
    
    const envDatacenters = datacenters.filter(dc => 
      (!dc.environment || dc.environment.type === envType) &&
      (!partnerId || (dc.partner && dc.partner.id === partnerId))
    );
    
    return partnerClouds.map((cloud: any, cloudIndex: number) => ({
      id: `cloud-${cloud.id}-${envType}-${cloudIndex}-${partnerId}`,
      name: cloud.name,
      type: 'cloud' as const,
      data: cloud,
      children: regions
        .filter(r => r.cloud?.id === cloud.id || r.cloudId === cloud.id)
        .map((region: any, regionIndex: number) => ({
          id: `region-${cloud.id}-${region.id}-${envType}-${cloudIndex}-${partnerId}`,
          name: region.name,
          type: 'region' as const,
          data: region,
          metadata: {
            compliance: region.complianceZones?.split(',') || []
          },
          children: azs
            .filter(az => az.region?.id === region.id || az.regionId === region.id)
            .map((az: any, azIndex: number) => ({
              id: `az-${cloud.id}-${region.id}-${az.id}-${envType}-${cloudIndex}-${partnerId}`,
              name: az.name,
              type: 'az' as const,
              data: az,
              metadata: {
                capabilities: az.capabilities?.split(',') || []
              },
              children: datacenters
                .filter(dc => (dc.availabilityZone?.id === az.id || dc.availabilityZoneId === az.id) &&
                  (!dc.environment || dc.environment.type === envType))
                .map((dc: any, dcIndex: number) => {
                  const dcAssignedGroups = dc.datacenterResourceGroups || [];
                  
                  let resourceGroupNodes: TreeNode[] = [];
                  
                  if (dcAssignedGroups.length > 0) {
                    const uniqueRgs = new Map();
                    dcAssignedGroups.forEach((dcRg: any) => {
                      const rg = dcRg.resourceGroup;
                      if (!uniqueRgs.has(rg.id)) {
                        uniqueRgs.set(rg.id, rg);
                      }
                    });
                    
                    resourceGroupNodes = Array.from(uniqueRgs.values()).map((rg: any, rgIndex: number) => ({
                      id: `resource-group-${dc.id}-${rg.id}-${envType}-${rgIndex}-${dcIndex}-${partnerId}`,
                      name: rg.displayName || rg.name,
                      type: 'resource-group' as const,
                      data: rg,
                      metadata: {
                        category: rg.category,
                        icon: rg.icon,
                        color: rg.color
                      },
                      children: (rg.serviceTypes || []).map((service: string, serviceIndex: number) => ({
                        id: `service-${dc.id}-${rg.id}-${serviceIndex}-${envType}-${rgIndex}-${dcIndex}-${partnerId}`,
                        name: service,
                        type: 'service' as const,
                        data: { name: service, resourceGroupId: rg.id, datacenterId: dc.id },
                        metadata: {
                          resourceGroup: rg.name
                        },
                        children: buildServiceChildren(service, rg.name, dc.id, computes, networkDevices)
                      }))
                    }));
                  } else {
                    const activeResourceGroups = resourceGroups.filter((rg: any) => 
                      rg.isActive !== false
                    );
                    
                    resourceGroupNodes = activeResourceGroups.map((rg: any, rgIndex: number) => ({
                      id: `resource-group-${dc.id}-${rg.id}-${envType}-${rgIndex}-${dcIndex}-${partnerId}`,
                      name: rg.displayName || rg.name,
                      type: 'resource-group' as const,
                      data: rg,
                      metadata: {
                        category: rg.category,
                        icon: rg.icon,
                        color: rg.color
                      },
                      children: (rg.serviceTypes || []).map((service: string, serviceIndex: number) => ({
                        id: `service-${dc.id}-${rg.id}-${serviceIndex}-${envType}-${rgIndex}-${dcIndex}-${partnerId}`,
                        name: service,
                        type: 'service' as const,
                        data: { name: service, resourceGroupId: rg.id, datacenterId: dc.id },
                        metadata: {
                          resourceGroup: rg.name
                        },
                        children: buildServiceChildren(service, rg.name, dc.id, computes, networkDevices)
                      }))
                    }));
                  }
                  
                  return {
                    id: `datacenter-${cloud.id}-${az.id}-${dc.id}-${envType}-${cloudIndex}-${partnerId}`,
                    name: dc.name,
                    type: 'datacenter' as const,
                    data: dc,
                    metadata: {
                      tier: dc.tier || 3,
                      drPaired: dc.drPairedDatacenter != null,
                      utilization: dc.utilization || 0
                    },
                    children: resourceGroupNodes
                  };
                })
            }))
        }))
    }));
  };

  const findNodeById = (nodes: TreeNode[], id: string): TreeNode | null => {
    for (const node of nodes) {
      if (node.id === id) {
        return node;
      }
      if (node.children) {
        const found = findNodeById(node.children, id);
        if (found) {
          return found;
        }
      }
    }
    return null;
  };

  const buildNodePath = (nodes: TreeNode[], targetId: string, path: TreeNode[] = []): TreeNode[] | null => {
    for (const node of nodes) {
      const newPath = [...path, node];
      if (node.id === targetId) {
        return newPath;
      }
      if (node.children) {
        const result = buildNodePath(node.children, targetId, newPath);
        if (result) {
          return result;
        }
      }
    }
    return null;
  };

  const handleNodeSelect = (event: React.SyntheticEvent | null, nodeId: string | null) => {
    if (nodeId) {
      setSelectedNodeId(nodeId);
      const node = findNodeById(treeData, nodeId);
      setSelectedNode(node);
      const path = buildNodePath(treeData, nodeId);
      setSelectedNodePath(path || []);
    }
  };

  const handleToggle = (event: React.SyntheticEvent | null, nodeIds: string[]) => {
    setExpanded(nodeIds);
  };

  const handleExpandAll = () => {
    const getAllNodeIds = (nodes: any[]): string[] => {
      let ids: string[] = [];
      nodes.forEach(node => {
        ids.push(node.id);
        if (node.children && node.children.length > 0) {
          ids = ids.concat(getAllNodeIds(node.children));
        }
      });
      return ids;
    };
    const allIds = getAllNodeIds(organizationInfraStore.treeData);
    setExpanded(allIds);
    // Also update store's expanded nodes
    allIds.forEach(id => organizationInfraStore.expandNode(id));
  };

  const handleCollapseAll = () => {
    setExpanded([]);
    // Clear store's expanded nodes
    organizationInfraStore.expandedNodeIds = [];
  };

  const handleTenantChange = (event: React.MouseEvent<HTMLElement>, newTenant: 'organization' | 'partners' | null) => {
    if (newTenant !== null) {
      setTenant(newTenant);
    }
  };

  const handleAdd = (type: 'cloud' | 'region' | 'az' | 'datacenter' | 'compute' | 'network-device', parentNode?: TreeNode) => {
    if (type === 'compute') {
      setComputeEditData({});
      setOpenComputeEditDialog(true);
    } else if (type === 'network-device') {
      setNetworkDeviceEditData({});
      setOpenNetworkDeviceEditDialog(true);
    } else {
      setDialogType(type);
      setEditMode(false);
      
      if (parentNode) {
        setSelectedNode(parentNode);
        setSelectedNodeId(parentNode.id);
      }
      
      setFormData(getDefaultFormData(type));
      setOpenDialog(true);
    }
  };

  const handleEdit = () => {
    if (!selectedNode) return;
    
    const type = selectedNode.type;
    
    if (type === 'compute') {
      setComputeEditData(selectedNode.data || {});
      setOpenComputeEditDialog(true);
    } else if (type === 'network-device') {
      setNetworkDeviceEditData(selectedNode.data || {});
      setOpenNetworkDeviceEditDialog(true);
    } else {
      // For other types (cloud, region, az, datacenter, service), use generic form dialog
      setDialogType(type);
      setEditMode(true);
      setFormData(selectedNode.data || {});
      setOpenDialog(true);
    }
  };

  const handleComputeSave = async (updatedData: any) => {
    try {
      if (updatedData.id) {
        await organizationInfraStore.updateCompute(updatedData.id, updatedData);
      } else {
        // For new compute, we need a datacenter ID - get it from selected node or data
        const datacenterId = selectedNode?.type === 'datacenter' ? selectedNode.data?.id : updatedData.datacenterId;
        if (!datacenterId) {
          alert('Datacenter ID is required to create a compute');
          return;
        }
        await organizationInfraStore.createCompute(datacenterId, updatedData);
      }
      setOpenComputeEditDialog(false);
      await fetchInfrastructureData();
    } catch (error) {
      console.error('Error saving compute:', error);
      alert('Failed to save compute');
    }
  };

  const handleNetworkDeviceSave = async (updatedData: any) => {
    try {
      if (updatedData.id) {
        await organizationInfraStore.updateNetworkDevice(updatedData.id, updatedData);
      } else {
        // For new network device, we need a datacenter ID - get it from selected node or data
        const datacenterId = selectedNode?.type === 'datacenter' ? selectedNode.data?.id : updatedData.datacenterId;
        if (!datacenterId) {
          alert('Datacenter ID is required to create a network device');
          return;
        }
        await organizationInfraStore.createNetworkDevice(datacenterId, updatedData);
      }
      setOpenNetworkDeviceEditDialog(false);
      await fetchInfrastructureData();
    } catch (error) {
      console.error('Error saving network device:', error);
      alert('Failed to save network device');
    }
  };

  const handleContextMenu = (event: React.MouseEvent<HTMLElement>, node: TreeNode) => {
    event.preventDefault();
    event.stopPropagation();
    setContextMenu({
      anchorEl: event.currentTarget,
      node: node,
    });
  };

  const handleCloseContextMenu = () => {
    setContextMenu({ anchorEl: null, node: null });
  };

  const handleContextMenuAction = (action: string) => {
    if (!contextMenu?.node) return;
    
    const node = contextMenu.node;
    setSelectedNode(node);
    setSelectedNodeId(node.id);
    
    switch (action) {
      case 'add-cloud':
        handleAdd('cloud', node);
        break;
      case 'add-region':
        handleAdd('region', node);
        break;
      case 'add-az':
        handleAdd('az', node);
        break;
      case 'add-datacenter':
        handleAdd('datacenter', node);
        break;
      case 'add-compute':
        handleAdd('compute', node);
        break;
      case 'add-network-device':
        handleAdd('network-device', node);
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

  const handleDelete = async () => {
    if (!selectedNode || !selectedNode.data) return;
    
    if (!window.confirm(`Are you sure you want to delete ${selectedNode.name}?`)) {
      return;
    }

    try {
      switch (selectedNode.type) {
        case 'compute':
          await organizationInfraStore.deleteCompute(selectedNode.data.id);
          break;
        case 'network-device':
          await organizationInfraStore.deleteNetworkDevice(selectedNode.data.id);
          break;
        default:
          // For other types, still use axios for now
          let endpoint = '';
          switch (selectedNode.type) {
            case 'cloud':
              endpoint = config.getApiEndpoint(`/clouds/${selectedNode.data.id}`);
              break;
            case 'datacenter':
              endpoint = config.getApiEndpoint(`/datacenters/${selectedNode.data.id}`);
              break;
            default:
              return;
          }
          await axios.delete(endpoint);
      }

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
          endpoint = editMode ? config.getApiEndpoint(`/clouds/${formData.id}`) : config.getApiEndpoint('/clouds');
          // Add partner reference if creating cloud under an organization
          if (!editMode && selectedNode?.type === 'environment') {
            payload.partnerId = selectedNode.data.partnerId;
          }
          break;
        case 'region':
          endpoint = editMode ? config.getApiEndpoint(`/regions/${formData.id}`) : config.getApiEndpoint('/regions');
          // Add cloud reference if creating region under a cloud
          if (!editMode && selectedNode?.type === 'cloud') {
            payload.cloudId = selectedNode.data.id;
          }
          break;
        case 'az':
          endpoint = editMode ? config.getApiEndpoint(`/availability-zones/${formData.id}`) : config.getApiEndpoint('/availability-zones');
          // Add region reference if creating AZ under a region
          if (!editMode && selectedNode?.type === 'region') {
            payload.regionId = selectedNode.data.id;
          }
          break;
        case 'datacenter':
          endpoint = editMode ? config.getApiEndpoint(`/datacenters/${formData.id}`) : config.getApiEndpoint('/datacenters');
          // Add AZ reference if creating datacenter under an AZ
          if (!editMode && selectedNode?.type === 'az') {
            payload.availabilityZoneId = selectedNode.data.id;
          }
          // Add cloud reference if creating datacenter under a cloud
          if (!editMode && selectedNode?.type === 'cloud') {
            payload.cloudId = selectedNode.data.id;
          }
          break;
        case 'compute':
          endpoint = editMode ? config.getApiEndpoint(`/computes/${formData.id}`) : config.getApiEndpoint('/computes');
          // Add datacenter reference if creating compute under a datacenter
          if (!editMode && selectedNode?.type === 'datacenter') {
            payload.datacenterId = selectedNode.data.id;
            payload.cloudId = selectedNode.data.cloud?.id;
          }
          break;
      }
      
      if (endpoint) {
        if (editMode) {
          await axios.put(endpoint, payload);
        } else {
          await axios.post(endpoint, payload);
        }
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
        return { name: '', description: '', status: 'ACTIVE', deploymentRegion: '', clientName: '' };
      case 'region':
        return { name: '', code: '', geographicArea: '', complianceZones: '', description: '', status: 'ACTIVE' };
      case 'az':
        return { name: '', code: '', zoneType: 'STANDARD', isDefault: false, capabilities: '', status: 'ACTIVE' };
      case 'datacenter':
        return { name: '', provider: '', type: 'PRIMARY', status: 'ACTIVE', tier: 3, utilization: 0, country: '', state: '', city: '' };
      case 'compute':
        return { name: '', hostname: '', status: 'ACTIVE', osType: 'Linux' };
      case 'network-device':
        return { name: '', deviceType: 'Router', status: 'ACTIVE', operationalStatus: 'UP' };
      default:
        return { name: '', status: 'ACTIVE' };
    }
  };

  const getDisplayNameForType = (type: string): string => {
    switch (type) {
      case 'organization':
        return 'Organization';
      case 'environment':
        return 'Environment';
      case 'cloud':
        return 'Cloud';
      case 'region':
        return 'Region';
      case 'az':
        return 'Availability Zone';
      case 'datacenter':
        return 'Datacenter';
      case 'resource-group':
        return 'Resource Group';
      case 'service':
        return 'Service';
      case 'compute':
        return 'Compute';
      case 'network-device':
        return 'Network Device';
      case 'pool':
        return 'Resource Pool';
      case 'container':
        return 'Container';
      default:
        return type;
    }
  };

  const getIconForNodeType = (type: string, metadata?: any) => {
    switch (type) {
      case 'organization':
        return <Business />;
      case 'environment':
        return <PublicIcon />;
      case 'cloud':
        return <CloudIcon />;
      case 'region':
        return <PublicIcon />;
      case 'az':
        return <DataCenterIcon />;
      case 'datacenter':
        return <DataCenterIcon />;
      case 'resource-group':
        return metadata?.icon === 'cloud' ? <CloudIcon /> : 
               metadata?.icon === 'router' ? <RouterIcon /> : <AccountTreeIcon />;
      case 'service':
        return <SettingsIcon />;
      case 'compute':
        return <Computer />;
      case 'network-device':
        return <RouterIcon />;
      default:
        return <DataObject />;
    }
  };

  const renderTreeItems = (nodes: TreeNode[]): React.ReactNode => {
    console.log('📊 renderTreeItems called with', nodes.length, 'nodes:', nodes);
    if (nodes.length === 0) {
      console.log('⚠️ renderTreeItems: No nodes to render!');
      return null;
    }
    return nodes.map((node) => {
      console.log('🌲 Rendering node:', node.id, node.name, node.type);
      return (
        <TreeItem
          key={node.id}
          nodeId={node.id}
          label={
            <Box 
              sx={{ display: 'flex', alignItems: 'center', py: 0.5 }}
              onContextMenu={(e) => handleContextMenu(e, node)}
            >
              {getIconForNodeType(node.type, node.metadata)}
              <Typography variant="body2" sx={{ ml: 1 }}>
                {node.name}
              </Typography>
              {node.metadata?.tier && (
                <Chip 
                  label={`Tier ${node.metadata.tier}`} 
                  size="small" 
                  sx={{ ml: 1 }} 
                />
              )}
              {node.metadata?.utilization !== undefined && (
                <Chip 
                  label={`${node.metadata.utilization}%`} 
                  size="small" 
                  color={node.metadata.utilization > 80 ? 'error' : 'default'}
                  sx={{ ml: 1 }} 
                />
              )}
            </Box>
          }
        >
          {node.children && node.children.length > 0 && renderTreeItems(node.children)}
        </TreeItem>
      );
    });
  };

  const renderNodeViewer = () => {
    if (!selectedNode) {
      return (
        <Card>
          <CardContent>
            <Typography variant="h6">Select a node to view details</Typography>
          </CardContent>
        </Card>
      );
    }

    return (
      <Card>
        <CardContent>
          <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}>
            <Tabs value={viewerTabIndex} onChange={(e, v) => setViewerTabIndex(v)}>
              <Tab label="Overview" />
              <Tab label="Properties" />
              {(selectedNode.type === 'compute' || selectedNode.type === 'network-device') && (
                <Tab label="Edit" />
              )}
            </Tabs>
          </Box>

          {viewerTabIndex === 0 && (
            <Box>
              <Typography variant="h6" gutterBottom>
                {selectedNode.name}
              </Typography>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                Type: {getDisplayNameForType(selectedNode.type)}
              </Typography>
              {selectedNode.metadata && (
                <Box sx={{ mt: 2 }}>
                  {Object.entries(selectedNode.metadata).map(([key, value]) => (
                    <Typography key={key} variant="body2">
                      <strong>{key}:</strong> {Array.isArray(value) ? value.join(', ') : String(value)}
                    </Typography>
                  ))}
                </Box>
              )}
            </Box>
          )}

          {viewerTabIndex === 1 && (
            <Box>
              <Typography variant="h6" gutterBottom>Properties</Typography>
              <pre style={{ fontSize: '12px', overflow: 'auto' }}>
                {JSON.stringify(selectedNode.data || {}, null, 2)}
              </pre>
            </Box>
          )}

          {viewerTabIndex === 2 && (selectedNode.type === 'compute' || selectedNode.type === 'network-device') && (
            <Box>
              <Typography variant="h6" gutterBottom>Edit {selectedNode.type}</Typography>
              <Button variant="contained" onClick={handleEdit}>
                Open Editor
              </Button>
            </Box>
          )}
        </CardContent>
      </Card>
    );
  };

  return (
    <Box sx={{ height: '100vh', display: 'flex', flexDirection: 'column' }}>
      {/* Header */}
      <Paper sx={{ px: 3, py: 1.5, marginTop: 0 }}>
        <Typography variant="h4" gutterBottom>
          Manage Infrastructure
        </Typography>
        <Typography variant="body1" color="text.secondary" gutterBottom>
          Hierarchical view of your cloud-native infrastructure across environments, regions, and availability zones
        </Typography>
      </Paper>

      {/* Main Content */}
      <Box sx={{ flexGrow: 1, display: 'flex', mt: 2, gap: 2 }}>
        {/* Tree View */}
        <Box sx={{ width: '35%', height: '100%' }}>
          <Paper sx={{ p: 2, height: '100%', overflow: 'auto' }}>
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
                {/* Tenant Toggle */}
                <ToggleButtonGroup
                  value={tenant}
                  exclusive
                  onChange={handleTenantChange}
                  aria-label="tenant selection"
                  size="small"
                >
                  <ToggleButton value="organization" aria-label="organization">
                    Organization
                  </ToggleButton>
                  <ToggleButton value="partners" aria-label="partners">
                    Partners
                  </ToggleButton>
                </ToggleButtonGroup>

                <Box sx={{ display: 'flex', gap: 0.5 }}>
                  <Tooltip title="Expand All">
                    <IconButton onClick={handleExpandAll} size="small">
                      <ExpandMore />
                    </IconButton>
                  </Tooltip>
                  <Tooltip title="Collapse All">
                    <IconButton onClick={handleCollapseAll} size="small">
                      <ChevronRight />
                    </IconButton>
                  </Tooltip>
                  <Tooltip title="Refresh Data">
                    <IconButton onClick={fetchInfrastructureData} size="small">
                      <Refresh />
                    </IconButton>
                  </Tooltip>
                </Box>
              </Box>

              {error && (
                <Alert severity="error" sx={{ mb: 2 }}>
                  {error}
                </Alert>
              )}

              {loading && (
                <Typography variant="body2" color="text.secondary">
                  Loading infrastructure data...
                </Typography>
              )}
              
              {!loading && treeData.length === 0 && (
                <Typography variant="body2" color="text.secondary">
                  No infrastructure data available. Check the console for debugging information.
                </Typography>
              )}
              
              {!loading && treeData.length > 0 && (
                <TreeView
                  selected={selectedNodeId}
                  onNodeSelect={handleNodeSelect}
                  expanded={expanded}
                  onNodeToggle={handleToggle}
                  defaultCollapseIcon={<ExpandMore />}
                  defaultExpandIcon={<ChevronRight />}
                  sx={{ flexGrow: 1 }}
                >
                  {renderTreeItems(treeData)}
                </TreeView>
              )}
          </Paper>
        </Box>

        {/* Details View */}
        <Box sx={{ width: '65%', height: '100%' }}>
          <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
              {/* Action Buttons */}
              <Paper sx={{ p: 2, mb: 2 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                  <Typography variant="h6">
                    {selectedNode ? (
                      <strong>{selectedNode.name} ({getDisplayNameForType(selectedNode.type)})</strong>
                    ) : (
                      'Actions'
                    )}
                  </Typography>
                  {selectedNode && (['cloud', 'region', 'az', 'datacenter', 'service', 'compute', 'network-device'].includes(selectedNode.type)) && (
                    <Box sx={{ display: 'flex', gap: 1 }}>
                      <IconButton
                        color="primary"
                        size="small"
                        onClick={handleEdit}
                        title="Edit"
                      >
                        <EditIcon />
                      </IconButton>
                      <IconButton
                        color="error"
                        size="small"
                        onClick={handleDelete}
                        title="Delete"
                      >
                        <DeleteIcon />
                      </IconButton>
                    </Box>
                  )}
                </Box>
                <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                  {selectedNode && (
                    <>
                      {/* Add buttons based on node type */}
                      {selectedNode.type === 'environment' && (
                        <Button
                          variant="outlined"
                          size="small"
                          onClick={() => handleAdd('cloud', selectedNode)}
                        >
                          Add Cloud
                        </Button>
                      )}
                      {selectedNode.type === 'cloud' && (
                        <>
                          <Button
                            variant="outlined"
                            size="small"
                            onClick={() => handleAdd('region', selectedNode)}
                          >
                            Add Region
                          </Button>
                          <Button
                            variant="outlined"
                            size="small"
                            onClick={() => handleAdd('datacenter', selectedNode)}
                          >
                            Add Datacenter
                          </Button>
                        </>
                      )}
                      {selectedNode.type === 'region' && (
                        <Button
                          variant="outlined"
                          size="small"
                          onClick={() => handleAdd('az', selectedNode)}
                        >
                          Add Availability Zone
                        </Button>
                      )}
                      {selectedNode.type === 'az' && (
                        <Button
                          variant="outlined"
                          size="small"
                          onClick={() => handleAdd('datacenter', selectedNode)}
                        >
                          Add Datacenter
                        </Button>
                      )}
                      {(selectedNode.type === 'datacenter' || selectedNode.type === 'service') && (
                        <>
                          <Button
                            variant="outlined"
                            size="small"
                            onClick={() => handleAdd('compute', selectedNode)}
                          >
                            Add Compute
                          </Button>
                          <Button
                            variant="outlined"
                            size="small"
                            onClick={() => handleAdd('network-device', selectedNode)}
                          >
                            Add Network Device
                          </Button>
                        </>
                      )}
                    </>
                  )}
                  {!selectedNode && (
                    <Typography variant="body2" color="text.secondary">
                      Select a node to see available actions
                    </Typography>
                  )}
                </Box>
              </Paper>
              
              {selectedNodePath.length > 0 && (
                <Paper sx={{ p: 2, mb: 2 }}>
                  <Breadcrumbs separator={<NavigateNext fontSize="small" />}>
                    {selectedNodePath.map((node, index) => (
                      <Link
                        key={node.id}
                        component="button"
                        variant="body2"
                        onClick={() => handleNodeSelect(null, node.id)}
                        sx={{ textDecoration: 'none' }}
                      >
                        {node.name}
                      </Link>
                    ))}
                  </Breadcrumbs>
                </Paper>
              )}

              <Box sx={{ flexGrow: 1, overflow: 'auto' }}>
                {renderNodeViewer()}
              </Box>
          </Box>
        </Box>
      </Box>

      {/* Context Menu */}
      <Menu
        anchorEl={contextMenu.anchorEl}
        open={Boolean(contextMenu.anchorEl)}
        onClose={() => setContextMenu({ anchorEl: null, node: null })}
      >
        {contextMenu.node && (
          <>
            {contextMenu.node.type === 'environment' && (
              <MenuItem onClick={() => handleContextMenuAction('add-cloud')}>
                <Add fontSize="small" sx={{ mr: 1 }} /> Add Cloud
              </MenuItem>
            )}
            {contextMenu.node.type === 'cloud' && (
              <>
                <MenuItem onClick={() => handleContextMenuAction('add-region')}>
                  <Add fontSize="small" sx={{ mr: 1 }} /> Add Region
                </MenuItem>
                <MenuItem onClick={() => handleContextMenuAction('add-datacenter')}>
                  <Add fontSize="small" sx={{ mr: 1 }} /> Add Datacenter
                </MenuItem>
              </>
            )}
            {contextMenu.node.type === 'region' && (
              <MenuItem onClick={() => handleContextMenuAction('add-az')}>
                <Add fontSize="small" sx={{ mr: 1 }} /> Add Availability Zone
              </MenuItem>
            )}
            {contextMenu.node.type === 'az' && (
              <MenuItem onClick={() => handleContextMenuAction('add-datacenter')}>
                <Add fontSize="small" sx={{ mr: 1 }} /> Add Datacenter
              </MenuItem>
            )}
            {(contextMenu.node.type === 'datacenter' || contextMenu.node.type === 'service') && (
              <>
                <MenuItem onClick={() => handleContextMenuAction('add-compute')}>
                  <Add fontSize="small" sx={{ mr: 1 }} /> Add Compute
                </MenuItem>
                <MenuItem onClick={() => handleContextMenuAction('add-network-device')}>
                  <Add fontSize="small" sx={{ mr: 1 }} /> Add Network Device
                </MenuItem>
              </>
            )}
            {(['cloud', 'region', 'az', 'datacenter', 'compute', 'network-device'].includes(contextMenu.node.type)) && (
              <>
                <Divider />
                <MenuItem onClick={() => handleContextMenuAction('edit')}>
                  <EditIcon fontSize="small" sx={{ mr: 1 }} /> Edit
                </MenuItem>
                <MenuItem onClick={() => handleContextMenuAction('delete')}>
                  <DeleteIcon fontSize="small" sx={{ mr: 1 }} /> Delete
                </MenuItem>
              </>
            )}
          </>
        )}
      </Menu>

      {/* Dialogs */}
      {/* Generic Infrastructure Dialog for Cloud, Region, AZ, Datacenter */}
      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="md" fullWidth>
        <DialogTitle>
          {editMode ? 'Edit' : 'Add'} {dialogType && getDisplayNameForType(dialogType)}
        </DialogTitle>
        <DialogContent>
          <Box sx={{ pt: 2, px: 3, display: 'flex', flexDirection: 'column', gap: 2, maxWidth: 600, mx: 'auto' }}>
            <TextField
              label="Name"
              value={formData.name || ''}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              fullWidth
              required
            />
            {dialogType === 'cloud' && (
              <>
                <TextField
                  label="Description"
                  value={formData.description || ''}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  fullWidth
                  multiline
                  rows={3}
                />
                <TextField
                  label="Deployment Region"
                  value={formData.deploymentRegion || ''}
                  onChange={(e) => setFormData({ ...formData, deploymentRegion: e.target.value })}
                  fullWidth
                />
              </>
            )}
            {dialogType === 'region' && (
              <>
                <TextField
                  label="Code"
                  value={formData.code || ''}
                  onChange={(e) => setFormData({ ...formData, code: e.target.value })}
                  fullWidth
                />
                <TextField
                  label="Geographic Area"
                  value={formData.geographicArea || ''}
                  onChange={(e) => setFormData({ ...formData, geographicArea: e.target.value })}
                  fullWidth
                />
                <TextField
                  label="Compliance Zones"
                  value={formData.complianceZones || ''}
                  onChange={(e) => setFormData({ ...formData, complianceZones: e.target.value })}
                  fullWidth
                  helperText="Comma-separated values"
                />
              </>
            )}
            {dialogType === 'az' && (
              <>
                <TextField
                  label="Code"
                  value={formData.code || ''}
                  onChange={(e) => setFormData({ ...formData, code: e.target.value })}
                  fullWidth
                />
                <TextField
                  label="Zone Type"
                  value={formData.zoneType || 'STANDARD'}
                  onChange={(e) => setFormData({ ...formData, zoneType: e.target.value })}
                  fullWidth
                  select
                  SelectProps={{ native: true }}
                >
                  <option value="STANDARD">Standard</option>
                  <option value="HIGH_AVAILABILITY">High Availability</option>
                  <option value="EDGE">Edge</option>
                </TextField>
                <TextField
                  label="Capabilities"
                  value={formData.capabilities || ''}
                  onChange={(e) => setFormData({ ...formData, capabilities: e.target.value })}
                  fullWidth
                  helperText="Comma-separated values"
                />
              </>
            )}
            {dialogType === 'datacenter' && (
              <>
                <TextField
                  label="Country"
                  value={formData.country || ''}
                  onChange={(e) => setFormData({ ...formData, country: e.target.value })}
                  fullWidth
                />
                <TextField
                  label="State"
                  value={formData.state || ''}
                  onChange={(e) => setFormData({ ...formData, state: e.target.value })}
                  fullWidth
                />
                <TextField
                  label="City"
                  value={formData.city || ''}
                  onChange={(e) => setFormData({ ...formData, city: e.target.value })}
                  fullWidth
                />
                <TextField
                  label="Provider"
                  value={formData.provider || ''}
                  onChange={(e) => setFormData({ ...formData, provider: e.target.value })}
                  fullWidth
                />
                <TextField
                  label="Tier"
                  value={formData.tier || 3}
                  onChange={(e) => setFormData({ ...formData, tier: parseInt(e.target.value) })}
                  fullWidth
                  type="number"
                  inputProps={{ min: 1, max: 4 }}
                />
              </>
            )}
            <TextField
              label="Status"
              value={formData.status || 'ACTIVE'}
              onChange={(e) => setFormData({ ...formData, status: e.target.value })}
              fullWidth
              select
              SelectProps={{ native: true }}
            >
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
              <option value="MAINTENANCE">Maintenance</option>
            </TextField>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button onClick={handleSave} variant="contained" color="primary">
            {editMode ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>

      <ComputeEditDialog
        open={openComputeEditDialog}
        onClose={() => setOpenComputeEditDialog(false)}
        formData={computeEditData || {}}
        setFormData={setComputeEditData}
        onSave={handleComputeSave}
        editMode={true}
      />

      <NetworkDeviceEditDialog
        open={openNetworkDeviceEditDialog}
        onClose={() => setOpenNetworkDeviceEditDialog(false)}
        formData={networkDeviceEditData || {}}
        setFormData={setNetworkDeviceEditData}
        onSave={handleNetworkDeviceSave}
        editMode={true}
      />
    </Box>
  );
};

export default observer(InfrastructureCloudNative);