import React, { useState } from 'react';
import {
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Collapse,
  Box,
  Typography
} from '@mui/material';
import {
  Folder as FolderIcon,
  Cloud as CloudIcon,
  Business as DatacenterIcon,
  Computer as ComputeIcon,
  ViewInAr as ContainerIcon,
  Circle as ItemIcon
} from '@mui/icons-material';
import { alpha } from '@mui/material/styles';
import { TreeNode } from '../types/CloudHierarchy';

interface InfrastructureTreeProps {
  clouds: any[];
  selectedNode: TreeNode | undefined;
  onNodeSelect: (node: TreeNode) => void;
}

// Tree expand/collapse icon with cool arrows and tree lines
const TreeExpandIcon: React.FC<{ 
  expanded: boolean; 
  level: number; 
  hasChildren?: boolean;
  isLast?: boolean;
}> = ({ expanded, level, hasChildren = true, isLast = false }) => (
  <Box 
    sx={{ 
      width: 16, 
      height: 16, 
      display: 'flex', 
      alignItems: 'center', 
      justifyContent: 'center',
      ml: level <= 1 ? level * 0.8 : 0.8 + (level - 1) * 0.5, // Less indentation after cloud level
      position: 'relative'
    }}
  >
    {/* Tree connecting lines */}
    {level > 0 && (
      <>
        {/* Vertical line */}
        <Box
          sx={{
            position: 'absolute',
            left: -8,
            top: isLast ? -8 : -16,
            width: 1,
            height: isLast ? 16 : 32,
            backgroundColor: 'rgba(0, 0, 0, 0.08)',
            pointerEvents: 'none'
          }}
        />
        {/* Horizontal line */}
        <Box
          sx={{
            position: 'absolute',
            left: -8,
            top: '50%',
            width: 8,
            height: 1,
            backgroundColor: 'rgba(0, 0, 0, 0.08)',
            transform: 'translateY(-50%)',
            pointerEvents: 'none'
          }}
        />
      </>
    )}
    
    {/* Cool arrow icons for expandable nodes */}
    {hasChildren && (
      <Box
        sx={{
          width: 16,
          height: 16,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: '#666',
          cursor: 'pointer',
          transition: 'color 0.2s ease',
          zIndex: 1,
          backgroundColor: 'white',
          '&:hover': {
            color: '#333'
          }
        }}
      >
        {expanded ? '▼' : '▶'}
      </Box>
    )}
    
    {/* Small dot for leaf nodes */}
    {!hasChildren && level > 0 && (
      <Box
        sx={{
          width: 4,
          height: 4,
          backgroundColor: '#999',
          borderRadius: '50%',
          zIndex: 1
        }}
      />
    )}
  </Box>
);

const InfrastructureTree: React.FC<InfrastructureTreeProps> = ({
  clouds,
  selectedNode,
  onNodeSelect
}) => {
  const [expanded, setExpanded] = useState<{ [key: string]: boolean }>({
    clouds: true
  });

  const handleToggle = (nodeId: string) => {
    setExpanded(prev => ({
      ...prev,
      [nodeId]: !prev[nodeId]
    }));
  };

  const isSelected = (nodeId: string, nodeType: string) => {
    return selectedNode?.id === nodeId && selectedNode?.type === nodeType;
  };

  const handleNodeClick = (node: TreeNode) => {
    onNodeSelect(node);
  };

  // Get hierarchy-based styling
  const getHierarchyStyle = (level: number, isSelected: boolean) => {
    const hierarchyBg = level === 0 ? 'rgba(240, 248, 255, 0.3)' : // Light blue for top level
                        level === 1 ? 'rgba(245, 255, 245, 0.2)' : // Light green for clouds  
                        level === 2 ? 'rgba(255, 248, 240, 0.2)' : // Light orange for categories
                        level >= 3 ? `rgba(250, 250, 250, ${0.5 - (level * 0.05)})` : 'transparent'; // Fading gray for deeper levels
    
    return {
      backgroundColor: isSelected ? alpha('#000', 0.08) : hierarchyBg,
      borderLeft: level > 0 ? `2px solid rgba(100, 100, 100, ${0.2 - (level * 0.02)})` : 'none',
      pl: level > 0 ? 1 : 0,
      '&:hover': { 
        backgroundColor: isSelected ? alpha('#000', 0.08) : alpha('#000', 0.05)
      }
    };
  };

  // Group containers by type
  const groupContainersByType = (containers: any[]) => {
    const groups: { [key: string]: any[] } = {};
    containers.forEach(container => {
      const type = container.containerType || 'OTHER';
      if (!groups[type]) {
        groups[type] = [];
      }
      groups[type].push(container);
    });
    return groups;
  };

  return (
    <List dense sx={{ width: '100%' }}>
      {/* Clouds Category */}
      <ListItem disablePadding>
        <ListItemButton
          onClick={() => handleToggle('clouds')}
          sx={{
            borderRadius: 1,
            ...getHierarchyStyle(0, isSelected('clouds', 'category'))
          }}
        >
          <ListItemIcon sx={{ minWidth: 24 }}>
            <TreeExpandIcon expanded={expanded.clouds} level={0} />
          </ListItemIcon>
          <ListItemIcon sx={{ color: '#424242', minWidth: 28 }}>
            <FolderIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText 
            primary="Clouds" 
            primaryTypographyProps={{ fontSize: '0.8rem', fontWeight: 500 }}
            onClick={(e) => {
              e.stopPropagation();
              handleNodeClick({
                id: 'clouds',
                name: 'Clouds',
                type: 'category',
                entityId: 'clouds',
                data: { clouds }
              });
            }}
          />
        </ListItemButton>
      </ListItem>

      <Collapse in={expanded.clouds} timeout="auto" unmountOnExit>
        <List component="div" disablePadding sx={{ pl: 1 }}>
          {clouds.length === 0 ? (
            <ListItem sx={{ py: 1 }}>
              <Typography variant="body2" color="textSecondary" sx={{ fontSize: '0.75rem', pl: 2 }}>
                No clouds found
              </Typography>
            </ListItem>
          ) : (
            clouds.map((cloud) => (
              <Box key={cloud.id}>
                {/* Individual Cloud */}
                <ListItem disablePadding>
                  <ListItemButton
                    onClick={() => handleToggle(`cloud-${cloud.id}`)}
                    sx={{
                      borderRadius: 1,
                      backgroundColor: isSelected(cloud.id.toString(), 'cloud') ? alpha('#000', 0.08) : 'transparent',
                      '&:hover': { backgroundColor: alpha('#000', 0.05) }
                    }}
                  >
                    <ListItemIcon sx={{ minWidth: 24 }}>
                      <TreeExpandIcon expanded={expanded[`cloud-${cloud.id}`]} level={1} />
                    </ListItemIcon>
                    <ListItemIcon sx={{ color: '#424242', minWidth: 28 }}>
                      <CloudIcon fontSize="small" />
                    </ListItemIcon>
                    <ListItemText 
                      primary={cloud.name || `Cloud ${cloud.id}`}
                      primaryTypographyProps={{ fontSize: '0.8rem' }}
                      onClick={(e) => {
                        e.stopPropagation();
                        handleNodeClick({
                          id: cloud.id.toString(),
                          name: cloud.name || `Cloud ${cloud.id}`,
                          type: 'cloud',
                          entityId: cloud.id,
                          data: cloud
                        });
                      }}
                    />
                  </ListItemButton>
                </ListItem>

                <Collapse in={expanded[`cloud-${cloud.id}`]} timeout="auto" unmountOnExit>
                  <List component="div" disablePadding sx={{ pl: 1 }}>
                    {/* Datacenters Category */}
                    <ListItem disablePadding>
                      <ListItemButton
                        onClick={() => handleToggle(`cloud-${cloud.id}-datacenters`)}
                        sx={{
                          borderRadius: 1,
                          backgroundColor: isSelected(`cloud-${cloud.id}-datacenters`, 'category') ? alpha('#000', 0.08) : 'transparent',
                          '&:hover': { backgroundColor: alpha('#000', 0.05) }
                        }}
                      >
                        <ListItemIcon sx={{ minWidth: 24 }}>
                          <TreeExpandIcon expanded={expanded[`cloud-${cloud.id}-datacenters`]} level={2} />
                        </ListItemIcon>
                        <ListItemIcon sx={{ color: '#424242', minWidth: 28 }}>
                          <FolderIcon fontSize="small" />
                        </ListItemIcon>
                        <ListItemText 
                          primary="Datacenters"
                          primaryTypographyProps={{ fontSize: '0.8rem', fontWeight: 500 }}
                          onClick={(e) => {
                            e.stopPropagation();
                            handleNodeClick({
                              id: `cloud-${cloud.id}-datacenters`,
                              name: 'Datacenters',
                              type: 'category',
                              entityId: cloud.id,
                              data: { cloud, datacenters: cloud.datacenters || [] }
                            });
                          }}
                        />
                                              </ListItemButton>
                    </ListItem>

                    <Collapse in={expanded[`cloud-${cloud.id}-datacenters`]} timeout="auto" unmountOnExit>
                      <List component="div" disablePadding sx={{ pl: 1 }}>
                        {(cloud.datacenters || []).map((datacenter: any) => (
                          <Box key={datacenter.id}>
                            {/* Individual Datacenter */}
                            <ListItem disablePadding>
                              <ListItemButton
                                onClick={() => handleToggle(`datacenter-${datacenter.id}`)}
                                sx={{
                                  borderRadius: 1,
                                  backgroundColor: isSelected(datacenter.id.toString(), 'datacenter') ? alpha('#000', 0.08) : 'transparent',
                                  '&:hover': { backgroundColor: alpha('#000', 0.05) }
                                }}
                              >
                                <ListItemIcon sx={{ minWidth: 24 }}>
                                  <TreeExpandIcon expanded={expanded[`datacenter-${datacenter.id}`]} level={3} />
                                </ListItemIcon>
                                <ListItemIcon sx={{ color: '#424242', minWidth: 28 }}>
                                  <DatacenterIcon fontSize="small" />
                                </ListItemIcon>
                                <ListItemText 
                                  primary={datacenter.name || `Datacenter ${datacenter.id}`}
                                  primaryTypographyProps={{ fontSize: '0.8rem' }}
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    onNodeSelect({
                                      id: datacenter.id.toString(),
                                      name: datacenter.name || `Datacenter ${datacenter.id}`,
                                      type: 'datacenter',
                                      entityId: datacenter.id,
                                      data: { ...datacenter, cloud }
                                    });
                                  }}
                                />
                                                              </ListItemButton>
                            </ListItem>

                            <Collapse in={expanded[`datacenter-${datacenter.id}`]} timeout="auto" unmountOnExit>
                              <List component="div" disablePadding sx={{ pl: 1 }}>
                                {/* Computes Category */}
                                <ListItem disablePadding>
                                  <ListItemButton
                                    onClick={() => handleToggle(`datacenter-${datacenter.id}-computes`)}
                                    sx={{
                                      borderRadius: 1,
                                      backgroundColor: isSelected(`datacenter-${datacenter.id}-computes`, 'category') ? alpha('#000', 0.08) : 'transparent',
                                      '&:hover': { backgroundColor: alpha('#000', 0.05) }
                                    }}
                                  >
                                    <ListItemIcon sx={{ minWidth: 24 }}>
                                      <TreeExpandIcon expanded={expanded[`datacenter-${datacenter.id}-computes`]} level={4} />
                                    </ListItemIcon>
                                    <ListItemIcon sx={{ color: '#424242', minWidth: 28 }}>
                                      <FolderIcon fontSize="small" />
                                    </ListItemIcon>
                                    <ListItemText 
                                      primary="Computes"
                                      primaryTypographyProps={{ fontSize: '0.8rem', fontWeight: 500 }}
                                      onClick={(e) => {
                                        e.stopPropagation();
                                        handleNodeClick({
                                          id: `datacenter-${datacenter.id}-computes`,
                                          name: 'Computes',
                                          type: 'category',
                                          entityId: datacenter.id,
                                          data: { datacenter, cloud, computes: cloud.computes?.filter((c: any) => c.datacenter?.id === datacenter.id) || [] }
                                        });
                                      }}
                                    />
                                  </ListItemButton>
                                </ListItem>

                                <Collapse in={expanded[`datacenter-${datacenter.id}-computes`]} timeout="auto" unmountOnExit>
                                  <List component="div" disablePadding sx={{ pl: 1 }}>
                                    {(cloud.computes || [])
                                      .filter((compute: any) => compute.datacenter?.id === datacenter.id)
                                      .map((compute: any) => (
                                        <Box key={compute.id}>
                                          {/* Individual Compute */}
                                          <ListItem disablePadding>
                                            <ListItemButton
                                              onClick={() => handleToggle(`compute-${compute.id}`)}
                                              sx={{
                                                borderRadius: 1,
                                                backgroundColor: isSelected(compute.id.toString(), 'compute') ? alpha('#000', 0.08) : 'transparent',
                                                '&:hover': { backgroundColor: alpha('#000', 0.05) }
                                              }}
                                            >
                                              <ListItemIcon sx={{ minWidth: 24 }}>
                                                <TreeExpandIcon expanded={expanded[`compute-${compute.id}`]} level={5} />
                                              </ListItemIcon>
                                              <ListItemIcon sx={{ color: '#424242', minWidth: 28 }}>
                                                <ComputeIcon fontSize="small" />
                                              </ListItemIcon>
                                              <ListItemText 
                                                primary={compute.name || `Compute ${compute.id}`}
                                                primaryTypographyProps={{ fontSize: '0.8rem' }}
                                                onClick={(e) => {
                                                  e.stopPropagation();
                                                  handleNodeClick({
                                                    id: compute.id.toString(),
                                                    name: compute.name || `Compute ${compute.id}`,
                                                    type: 'compute',
                                                    entityId: compute.id,
                                                    data: { ...compute, datacenter, cloud }
                                                  });
                                                }}
                                              />
                                                                                          </ListItemButton>
                                          </ListItem>

                                          <Collapse in={expanded[`compute-${compute.id}`]} timeout="auto" unmountOnExit>
                                            <List component="div" disablePadding sx={{ pl: 1 }}>
                                              {/* Containers Category */}
                                              <ListItem disablePadding>
                                                <ListItemButton
                                                  onClick={() => handleToggle(`compute-${compute.id}-containers`)}
                                                  sx={{
                                                    borderRadius: 1,
                                                    backgroundColor: isSelected(`compute-${compute.id}-containers`, 'category') ? alpha('#000', 0.08) : 'transparent',
                                                    '&:hover': { backgroundColor: alpha('#000', 0.05) }
                                                  }}
                                                >
                                                  <ListItemIcon sx={{ minWidth: 24 }}>
                                                    <TreeExpandIcon expanded={expanded[`compute-${compute.id}-containers`]} level={6} />
                                                  </ListItemIcon>
                                                  <ListItemIcon sx={{ color: '#424242', minWidth: 28 }}>
                                                    <FolderIcon fontSize="small" />
                                                  </ListItemIcon>
                                                  <ListItemText 
                                                    primary="Containers"
                                                    primaryTypographyProps={{ fontSize: '0.8rem', fontWeight: 500 }}
                                                    onClick={(e) => {
                                                      e.stopPropagation();
                                                      handleNodeClick({
                                                        id: `compute-${compute.id}-containers`,
                                                        name: 'Containers',
                                                        type: 'category',
                                                        entityId: compute.id,
                                                        data: { compute, datacenter, cloud, containers: compute.containers || [] }
                                                      });
                                                    }}
                                                  />
                                                                                                  </ListItemButton>
                                              </ListItem>

                                              <Collapse in={expanded[`compute-${compute.id}-containers`]} timeout="auto" unmountOnExit>
                                                <List component="div" disablePadding sx={{ pl: 1 }}>
                                                  {/* Container Type Groups */}
                                                  {Object.entries(groupContainersByType(compute.containers || [])).map(([type, containers]) => (
                                                    <Box key={type}>
                                                      {/* Container Type Group */}
                                                      <ListItem disablePadding>
                                                        <ListItemButton
                                                          onClick={() => handleToggle(`compute-${compute.id}-${type}`)}
                                                          sx={{
                                                            borderRadius: 1,
                                                            backgroundColor: isSelected(`compute-${compute.id}-${type}`, 'container-group') ? alpha('#000', 0.08) : 'transparent',
                                                            '&:hover': { backgroundColor: alpha('#000', 0.05) }
                                                          }}
                                                        >
                                                          <ListItemIcon sx={{ minWidth: 24 }}>
                                                            <TreeExpandIcon expanded={expanded[`compute-${compute.id}-${type}`]} level={7} />
                                                          </ListItemIcon>
                                                          <ListItemIcon sx={{ color: '#424242', minWidth: 28 }}>
                                                            <ItemIcon fontSize="small" />
                                                          </ListItemIcon>
                                                          <ListItemText 
                                                            primary={type.toLowerCase()}
                                                            primaryTypographyProps={{ fontSize: '0.8rem', fontWeight: 500 }}
                                                            onClick={(e) => {
                                                              e.stopPropagation();
                                                              handleNodeClick({
                                                                id: `compute-${compute.id}-${type}`,
                                                                name: type,
                                                                type: 'container-group',
                                                                entityId: compute.id,
                                                                data: { compute, datacenter, cloud, containers, containerType: type }
                                                              });
                                                            }}
                                                          />
                                                                                                                  </ListItemButton>
                                                      </ListItem>

                                                      <Collapse in={expanded[`compute-${compute.id}-${type}`]} timeout="auto" unmountOnExit>
                                                        <List component="div" disablePadding sx={{ pl: 1 }}>
                                                          {/* Individual Containers */}
                                                          {containers.map((container: any) => (
                                                            <ListItem key={container.id} disablePadding>
                                                              <ListItemButton
                                                                onClick={() => handleNodeClick({
                                                                  id: container.id.toString(),
                                                                  name: container.name || `Container ${container.id}`,
                                                                  type: 'container',
                                                                  entityId: container.id,
                                                                  data: { ...container, compute, datacenter, cloud }
                                                                })}
                                                                sx={{
                                                                  borderRadius: 1,
                                                                  backgroundColor: isSelected(container.id.toString(), 'container') ? alpha('#000', 0.08) : 'transparent',
                                                                  '&:hover': { backgroundColor: alpha('#000', 0.05) }
                                                                }}
                                                              >
                                                                <ListItemIcon sx={{ color: '#424242', minWidth: 32 }}>
                                                                  <ItemIcon fontSize="small" />
                                                                </ListItemIcon>
                                                                <ListItemText 
                                                                  primary={container.name || `Container ${container.id}`}
                                                                  primaryTypographyProps={{ fontSize: '0.8rem' }}
                                                                />
                                                              </ListItemButton>
                                                            </ListItem>
                                                          ))}
                                                        </List>
                                                      </Collapse>
                                                    </Box>
                                                  ))}
                                                </List>
                                              </Collapse>
                                            </List>
                                          </Collapse>
                                        </Box>
                                      ))}
                                  </List>
                                </Collapse>
                              </List>
                            </Collapse>
                          </Box>
                        ))}
                      </List>
                    </Collapse>
                  </List>
                </Collapse>
              </Box>
            ))
          )}
        </List>
      </Collapse>
    </List>
  );
};

export default InfrastructureTree;