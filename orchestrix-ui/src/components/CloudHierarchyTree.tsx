import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Collapse,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Chip,
  styled
} from '@mui/material';
import {
  Cloud as CloudIcon,
  Business as DatacenterIcon,
  Computer as ComputeIcon,
  ViewInAr as ContainerIcon,
  ExpandMore,
  ChevronRight,
  Dns as LXDIcon,
  Build as DockerIcon,
  Memory as LXCIcon,
  Settings as ContainerdIcon,
  Hub as KubernetesIcon,
  Apps as PodmanIcon
} from '@mui/icons-material';
import { TreeNode, Cloud, Container, ContainerType } from '../types/CloudHierarchy';

const StyledTreeItem = styled(ListItemButton, {
  shouldForwardProp: (prop) => prop !== 'depth'
})<{ depth: number }>(({ theme, depth }) => ({
  paddingLeft: theme.spacing(depth * 2),
  '&.Mui-selected': {
    backgroundColor: theme.palette.action.selected,
  },
  '&:hover': {
    backgroundColor: theme.palette.action.hover,
  },
}));

interface CloudHierarchyTreeProps {
  clouds: Cloud[];
  selectedNode?: TreeNode;
  onNodeSelect: (node: TreeNode) => void;
  onNodeToggle?: (nodeId: string) => void;
}

const getContainerIcon = (containerType: ContainerType) => {
  switch (containerType) {
    case ContainerType.LXD: return <LXDIcon />;
    case ContainerType.DOCKER: return <DockerIcon />;
    case ContainerType.LXC: return <LXCIcon />;
    case ContainerType.CONTAINERD: return <ContainerdIcon />;
    case ContainerType.KUBERNETES: return <KubernetesIcon />;
    case ContainerType.PODMAN: return <PodmanIcon />;
    default: return <ContainerIcon />;
  }
};

const getStatusColor = (status: string) => {
  switch (status?.toLowerCase()) {
    case 'active': return 'success';
    case 'running': return 'success';
    case 'inactive': return 'default';
    case 'stopped': return 'error';
    case 'maintenance': return 'warning';
    case 'paused': return 'warning';
    default: return 'default';
  }
};

const CloudHierarchyTree: React.FC<CloudHierarchyTreeProps> = ({
  clouds,
  selectedNode,
  onNodeSelect,
  onNodeToggle
}) => {
  const [expandedNodes, setExpandedNodes] = useState<Set<string>>(new Set());

  const handleNodeToggle = (nodeId: string) => {
    const newExpandedNodes = new Set(expandedNodes);
    if (expandedNodes.has(nodeId)) {
      newExpandedNodes.delete(nodeId);
    } else {
      newExpandedNodes.add(nodeId);
    }
    setExpandedNodes(newExpandedNodes);
    onNodeToggle?.(nodeId);
  };

  const renderTreeNode = (node: TreeNode, depth: number = 0): React.ReactNode => {
    const isExpanded = expandedNodes.has(node.id);
    const isSelected = selectedNode?.id === node.id;
    const hasChildren = node.children && node.children.length > 0;

    return (
      <Box key={node.id}>
        <StyledTreeItem
          depth={depth}
          onClick={() => onNodeSelect(node)}
          sx={{
            backgroundColor: isSelected ? 'action.selected' : 'transparent'
          }}
        >
          <ListItemIcon sx={{ minWidth: 32 }}>
            {hasChildren ? (
              <IconButton 
                size="small" 
                onClick={(e) => {
                  e.stopPropagation();
                  handleNodeToggle(node.id);
                }}
                sx={{ p: 0.5 }}
              >
                {isExpanded ? <ExpandMore fontSize="small" /> : <ChevronRight fontSize="small" />}
              </IconButton>
            ) : (
              <Box sx={{ width: 24, height: 24, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                {node.icon}
              </Box>
            )}
          </ListItemIcon>
          <ListItemText
            primary={
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Typography variant="body2" noWrap>
                  {node.name}
                </Typography>
                {node.data?.status && (
                  <Chip
                    label={node.data.status}
                    size="small"
                    color={getStatusColor(node.data.status) as any}
                    sx={{ height: 16, fontSize: 10 }}
                  />
                )}
              </Box>
            }
          />
        </StyledTreeItem>
        {hasChildren && (
          <Collapse in={isExpanded} timeout="auto" unmountOnExit>
            <List disablePadding>
              {node.children?.map((child) => renderTreeNode(child, depth + 1))}
            </List>
          </Collapse>
        )}
      </Box>
    );
  };

  const buildTreeData = (): TreeNode[] => {
    console.log('CloudHierarchyTree: Building tree data for clouds:', clouds);
    console.log('CloudHierarchyTree: Number of clouds:', clouds.length);
    return clouds.map((cloud) => {
      const cloudNode: TreeNode = {
        id: `cloud-${cloud.id}`,
        name: cloud.name,
        type: 'cloud',
        entityId: cloud.id,
        icon: <CloudIcon fontSize="small" color="primary" />,
        data: cloud,
        children: []
      };

      // Add datacenters
      if (cloud.datacenters && cloud.datacenters.length > 0) {
        const datacenterNodes = cloud.datacenters.map((datacenter) => {
          const datacenterNode: TreeNode = {
            id: `datacenter-${datacenter.id}`,
            name: datacenter.name,
            type: 'datacenter',
            entityId: datacenter.id,
            icon: <DatacenterIcon fontSize="small" color="secondary" />,
            data: datacenter,
            children: []
          };

          // Add computes for this datacenter
          const datacenterComputes = cloud.computes?.filter(c => c.datacenter?.id === datacenter.id) || [];
          if (datacenterComputes.length > 0) {
            const computeNodes = datacenterComputes.map((compute) => {
              const computeNode: TreeNode = {
                id: `compute-${compute.id}`,
                name: compute.name,
                type: 'compute',
                entityId: compute.id,
                icon: <ComputeIcon fontSize="small" color="info" />,
                data: compute,
                children: []
              };

              // Group containers by type
              if (compute.containers && compute.containers.length > 0) {
                const containersByType = compute.containers.reduce((acc, container) => {
                  if (!acc[container.containerType]) {
                    acc[container.containerType] = [];
                  }
                  acc[container.containerType].push(container);
                  return acc;
                }, {} as Record<ContainerType, Container[]>);

                Object.entries(containersByType).forEach(([type, containers]) => {
                  const containerTypeNode: TreeNode = {
                    id: `container-group-${compute.id}-${type}`,
                    name: type,
                    type: 'container-group',
                    icon: getContainerIcon(type as ContainerType),
                    children: containers.map((container) => ({
                      id: `container-${container.id}`,
                      name: container.name,
                      type: 'container',
                      entityId: container.id,
                      icon: getContainerIcon(container.containerType),
                      data: container
                    }))
                  };
                  computeNode.children?.push(containerTypeNode);
                });
              }

              return computeNode;
            });
            datacenterNode.children?.push(...computeNodes);
          }

          return datacenterNode;
        });
        cloudNode.children?.push(...datacenterNodes);
      }

      // Add direct computes (not associated with datacenters)
      const directComputes = cloud.computes?.filter(c => !c.datacenter) || [];
      if (directComputes.length > 0) {
        const computeNodes = directComputes.map((compute) => ({
          id: `compute-${compute.id}`,
          name: compute.name,
          type: 'compute' as const,
          entityId: compute.id,
          icon: <ComputeIcon fontSize="small" color="info" />,
          data: compute,
          children: compute.containers?.map((container) => ({
            id: `container-${container.id}`,
            name: container.name,
            type: 'container' as const,
            entityId: container.id,
            icon: getContainerIcon(container.containerType),
            data: container
          })) || []
        }));
        cloudNode.children?.push(...computeNodes);
      }

      return cloudNode;
    });
  };

  const treeData = buildTreeData();

  // Auto-expand root level
  useEffect(() => {
    const rootNodes = new Set(treeData.map(node => node.id));
    setExpandedNodes(rootNodes);
  }, [treeData]);

  return (
    <Box sx={{ width: '100%', maxHeight: '80vh', overflow: 'auto' }}>
      <List disablePadding>
        {treeData.map((node) => renderTreeNode(node))}
      </List>
    </Box>
  );
};

export default CloudHierarchyTree;