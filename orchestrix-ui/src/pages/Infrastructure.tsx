import React, { useState, useMemo } from 'react';
import {
  Box,
  Typography,
  CircularProgress,
  Alert,
  Button
} from '@mui/material';
import CrudList from '../components/CrudList';
import TabbedEntityModal, { FieldGroup } from '../components/TabbedEntityModal';
import { useInfrastructure } from '../contexts/InfrastructureContext';
import { TreeNode } from '../types/CloudHierarchy';
import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8090/api/api';

const Infrastructure: React.FC = () => {
  const { clouds, selectedNode, setSelectedNode, loading, error, refreshClouds } = useInfrastructure();
  const [modalOpen, setModalOpen] = useState(false);
  const [modalData, setModalData] = useState<any>(null);
  const [modalMode, setModalMode] = useState<'view' | 'edit' | 'create'>('view');

  const handleNodeSelect = (node: TreeNode) => {
    setSelectedNode(node);
  };

  // Determine what data to show based on selected node
  const { listData, listTitle, entityType, columns, searchFields, filterConfig } = useMemo((): {
    listData: any[];
    listTitle: string;
    entityType: string;
    columns: any[];
    searchFields: string[];
    filterConfig: any;
  } => {
    if (!selectedNode) {
      // Show clouds by default
      return {
        listData: clouds,
        listTitle: 'Clouds',
        entityType: 'cloud',
        columns: [
          { id: 'name', label: 'Name', minWidth: 170 },
          { id: 'clientName', label: 'Client', minWidth: 120 },
          { id: 'deploymentRegion', label: 'Region', minWidth: 120 },
          { id: 'status', label: 'Status', minWidth: 100 }
        ],
        searchFields: ['name', 'clientName', 'deploymentRegion'],
        filterConfig: null
      };
    }

    switch (selectedNode.type) {
      case 'category':
        // Handle category nodes (Clouds, Datacenters, Computes, Containers)
        if (selectedNode.id === 'clouds') {
          // Show all clouds
          return {
            listData: selectedNode.data?.clouds || clouds,
            listTitle: 'Clouds',
            entityType: 'cloud',
            columns: [
              { id: 'name', label: 'Name', minWidth: 170 },
              { id: 'clientName', label: 'Client', minWidth: 120 },
              { id: 'deploymentRegion', label: 'Region', minWidth: 120 },
              { id: 'status', label: 'Status', minWidth: 100 }
            ],
            searchFields: ['name', 'clientName', 'deploymentRegion'],
            filterConfig: null
          };
        } else if (selectedNode.id.includes('-datacenters')) {
          // Show datacenters for a specific cloud
          return {
            listData: selectedNode.data?.datacenters || [],
            listTitle: 'Datacenters',
            entityType: 'datacenter',
            columns: [
              { id: 'name', label: 'Name', minWidth: 170 },
              { id: 'locationOther', label: 'Location', minWidth: 150 },
              { id: 'type', label: 'Type', minWidth: 100 },
              { id: 'provider', label: 'Provider', minWidth: 120 },
              { id: 'isDrSite', label: 'DR Site', format: (value: boolean) => value ? 'Yes' : 'No' },
              { id: 'status', label: 'Status', minWidth: 100 }
            ],
            searchFields: ['name', 'locationOther', 'type', 'provider'],
            filterConfig: null
          };
        } else if (selectedNode.id.includes('-computes')) {
          // Show computes for a specific datacenter
          return {
            listData: selectedNode.data?.computes || [],
            listTitle: 'Compute Resources',
            entityType: 'compute',
            columns: [
              { id: 'name', label: 'Name', minWidth: 170 },
              { id: 'computeType', label: 'Type', minWidth: 100 },
              { id: 'hostname', label: 'Hostname', minWidth: 150 },
              { id: 'ipAddress', label: 'IP Address', minWidth: 120 },
              { id: 'cpuCores', label: 'CPU Cores', minWidth: 80 },
              { id: 'memoryGb', label: 'RAM (GB)', minWidth: 80 },
              { id: 'status', label: 'Status', minWidth: 100 }
            ],
            searchFields: ['name', 'hostname', 'ipAddress'],
            filterConfig: {
              field: 'computeType',
              options: [
                { label: 'All Types', value: null },
                { label: 'Dedicated', value: 'DEDICATED' },
                { label: 'Virtual Machine', value: 'VM' }
              ]
            }
          };
        } else if (selectedNode.id.includes('-containers')) {
          // Show all containers for a specific compute
          return {
            listData: selectedNode.data?.containers || [],
            listTitle: 'Containers',
            entityType: 'container',
            columns: [
              { id: 'name', label: 'Name', minWidth: 170 },
              { id: 'containerType', label: 'Type', minWidth: 100 },
              { id: 'image', label: 'Image', minWidth: 150 },
              { id: 'ipAddress', label: 'IP Address', minWidth: 120 },
              { id: 'status', label: 'Status', minWidth: 100 },
              { id: 'autoStart', label: 'Auto Start', format: (value: boolean) => value ? 'Yes' : 'No' }
            ],
            searchFields: ['name', 'image', 'ipAddress'],
            filterConfig: {
              field: 'containerType',
              options: [
                { label: 'All Types', value: null },
                { label: 'LXD', value: 'LXD' },
                { label: 'Docker', value: 'DOCKER' },
                { label: 'LXC', value: 'LXC' },
                { label: 'Podman', value: 'PODMAN' },
                { label: 'Containerd', value: 'CONTAINERD' },
                { label: 'Kubernetes', value: 'KUBERNETES' }
              ]
            }
          };
        }
        // Fallback for category case that doesn't match any condition
        return {
          listData: [],
          listTitle: 'Select an item from the tree',
          entityType: '',
          columns: [],
          searchFields: [],
          filterConfig: null
        };

      case 'cloud':
        // Show datacenters for this cloud
        const datacenters = selectedNode.data?.datacenters || [];
        return {
          listData: datacenters,
          listTitle: `Datacenters in ${selectedNode.name}`,
          entityType: 'datacenter',
          columns: [
            { id: 'name', label: 'Name', minWidth: 170 },
            { id: 'locationOther', label: 'Location', minWidth: 150 },
            { id: 'type', label: 'Type', minWidth: 100 },
            { id: 'provider', label: 'Provider', minWidth: 120 },
            { id: 'isDrSite', label: 'DR Site', format: (value: boolean) => value ? 'Yes' : 'No' },
            { id: 'status', label: 'Status', minWidth: 100 }
          ],
          searchFields: ['name', 'locationOther', 'type', 'provider'],
          filterConfig: null
        };

      case 'datacenter':
        // Show computes for this datacenter
        const cloudData = clouds.find(c => c.id === selectedNode.data?.cloud?.id);
        const computes = cloudData?.computes?.filter(c => c.datacenter?.id === selectedNode.entityId) || [];
        return {
          listData: computes,
          listTitle: `Compute Resources in ${selectedNode.name}`,
          entityType: 'compute',
          columns: [
            { id: 'name', label: 'Name', minWidth: 170 },
            { id: 'computeType', label: 'Type', minWidth: 100 },
            { id: 'hostname', label: 'Hostname', minWidth: 150 },
            { id: 'ipAddress', label: 'IP Address', minWidth: 120 },
            { id: 'cpuCores', label: 'CPU Cores', minWidth: 80 },
            { id: 'memoryGb', label: 'RAM (GB)', minWidth: 80 },
            { id: 'status', label: 'Status', minWidth: 100 }
          ],
          searchFields: ['name', 'hostname', 'ipAddress'],
          filterConfig: {
            field: 'computeType',
            options: [
              { label: 'All Types', value: null },
              { label: 'Dedicated', value: 'DEDICATED' },
              { label: 'Virtual Machine', value: 'VM' }
            ]
          }
        };

      case 'compute':
        // Show containers for this compute
        const containers = selectedNode.data?.containers || [];
        return {
          listData: containers,
          listTitle: `Containers on ${selectedNode.name}`,
          entityType: 'container',
          columns: [
            { id: 'name', label: 'Name', minWidth: 170 },
            { id: 'containerType', label: 'Type', minWidth: 100 },
            { id: 'image', label: 'Image', minWidth: 150 },
            { id: 'ipAddress', label: 'IP Address', minWidth: 120 },
            { id: 'status', label: 'Status', minWidth: 100 },
            { id: 'autoStart', label: 'Auto Start', format: (value: boolean) => value ? 'Yes' : 'No' }
          ],
          searchFields: ['name', 'image', 'ipAddress'],
          filterConfig: {
            field: 'containerType',
            options: [
              { label: 'All Types', value: null },
              { label: 'LXD', value: 'LXD' },
              { label: 'Docker', value: 'DOCKER' },
              { label: 'LXC', value: 'LXC' },
              { label: 'Podman', value: 'PODMAN' },
              { label: 'Containerd', value: 'CONTAINERD' },
              { label: 'Kubernetes', value: 'KUBERNETES' }
            ]
          }
        };

      case 'container-group':
        // Show containers of specific type
        const computeData = clouds
          .flatMap(c => c.computes || [])
          .find(comp => comp.id === parseInt(selectedNode.id.split('-')[2]));
        const filteredContainers = computeData?.containers?.filter(c => c.containerType === selectedNode.name) || [];
        return {
          listData: filteredContainers,
          listTitle: `${selectedNode.name} Containers`,
          entityType: 'container',
          columns: [
            { id: 'name', label: 'Name', minWidth: 170 },
            { id: 'image', label: 'Image', minWidth: 150 },
            { id: 'ipAddress', label: 'IP Address', minWidth: 120 },
            { id: 'status', label: 'Status', minWidth: 100 },
            { id: 'autoStart', label: 'Auto Start', format: (value: boolean) => value ? 'Yes' : 'No' }
          ],
          searchFields: ['name', 'image', 'ipAddress'],
          filterConfig: null
        };

      default:
        return {
          listData: [],
          listTitle: 'Select an item from the tree',
          entityType: '',
          columns: [],
          searchFields: [],
          filterConfig: null
        };
    }
  }, [selectedNode, clouds]);

  // Define organized field groups for each entity type
  const getFieldGroups = (entityType: string): { [tabName: string]: FieldGroup[]; } => {
    switch (entityType) {
      case 'cloud':
        return {
          "General": [{
            title: "Basic Information",
            fields: [
              { name: 'name', label: 'Name', type: 'text' as const, required: true },
              { name: 'description', label: 'Description', type: 'textarea' as const },
              { name: 'clientName', label: 'Client Name', type: 'text' as const },
              { name: 'deploymentRegion', label: 'Deployment Region', type: 'text' as const },
              { name: 'status', label: 'Status', type: 'select' as const, options: [
                { label: 'Active', value: 'ACTIVE' },
                { label: 'Inactive', value: 'INACTIVE' },
                { label: 'Maintenance', value: 'MAINTENANCE' }
              ]}
            ]
          }],
          "Configuration": [{
            title: "Cloud Configuration",
            fields: [
              { name: 'cloudProvider', label: 'Provider', type: 'select' as const, options: [
                { label: 'AWS', value: 'AWS' },
                { label: 'Azure', value: 'AZURE' },
                { label: 'Google Cloud', value: 'GCP' },
                { label: 'Private Cloud', value: 'PRIVATE' },
                { label: 'Hybrid Cloud', value: 'HYBRID' },
                { label: 'Other', value: 'OTHER' }
              ]},
              { name: 'environment', label: 'Environment', type: 'select' as const, options: [
                { label: 'Production', value: 'PRODUCTION' },
                { label: 'Staging', value: 'STAGING' },
                { label: 'Development', value: 'DEVELOPMENT' },
                { label: 'Test', value: 'TEST' },
                { label: 'Disaster Recovery', value: 'DR' }
              ]},
              { name: 'cloudAccountId', label: 'Account ID', type: 'text' as const },
              { name: 'subscriptionId', label: 'Subscription ID', type: 'text' as const },
              { name: 'projectId', label: 'Project ID', type: 'text' as const }
            ]
          }, {
            title: "Features & Capabilities",
            fields: [
              { name: 'monitoringEnabled', label: 'Monitoring Enabled', type: 'boolean' as const },
              { name: 'loggingEnabled', label: 'Logging Enabled', type: 'boolean' as const },
              { name: 'autoScalingEnabled', label: 'Auto Scaling Enabled', type: 'boolean' as const },
              { name: 'maintenanceWindow', label: 'Maintenance Window', type: 'text' as const }
            ]
          }],
          "Governance": [{
            title: "Business & Compliance",
            fields: [
              { name: 'costCenter', label: 'Cost Center', type: 'text' as const },
              { name: 'businessUnit', label: 'Business Unit', type: 'text' as const },
              { name: 'technicalContact', label: 'Technical Contact', type: 'text' as const },
              { name: 'businessContact', label: 'Business Contact', type: 'text' as const },
              { name: 'complianceLevel', label: 'Compliance Level', type: 'select' as const, options: [
                { label: 'Public', value: 'PUBLIC' },
                { label: 'Internal', value: 'INTERNAL' },
                { label: 'Confidential', value: 'CONFIDENTIAL' },
                { label: 'Restricted', value: 'RESTRICTED' }
              ]}
            ]
          }, {
            title: "Disaster Recovery",
            fields: [
              { name: 'backupStrategy', label: 'Backup Strategy', type: 'textarea' as const },
              { name: 'disasterRecoveryRto', label: 'RTO (hours)', type: 'number' as const },
              { name: 'disasterRecoveryRpo', label: 'RPO (hours)', type: 'number' as const }
            ]
          }]
        };
      case 'datacenter':
        return {
          "General": [{
            title: "Basic Information",
            fields: [
              { name: 'name', label: 'Name', type: 'text' as const, required: true },
              { name: 'locationOther', label: 'Location', type: 'text' as const },
              { name: 'type', label: 'Type', type: 'text' as const },
              { name: 'provider', label: 'Provider', type: 'text' as const },
              { name: 'isDrSite', label: 'DR Site', type: 'boolean' as const },
              { name: 'status', label: 'Status', type: 'select' as const, options: [
                { label: 'Active', value: 'ACTIVE' },
                { label: 'Inactive', value: 'INACTIVE' },
                { label: 'Maintenance', value: 'MAINTENANCE' }
              ]}
            ]
          }],
          "Infrastructure": [{
            title: "Facility Details",
            fields: [
              { name: 'facilityType', label: 'Facility Type', type: 'select' as const, options: [
                { label: 'Colocation', value: 'COLOCATION' },
                { label: 'Owned', value: 'OWNED' },
                { label: 'Leased', value: 'LEASED' },
                { label: 'Edge', value: 'EDGE' },
                { label: 'Private', value: 'PRIVATE' },
                { label: 'Public', value: 'PUBLIC' }
              ]},
              { name: 'tier', label: 'Tier Level', type: 'select' as const, options: [
                { label: 'Tier 1', value: 'TIER_1' },
                { label: 'Tier 2', value: 'TIER_2' },
                { label: 'Tier 3', value: 'TIER_3' },
                { label: 'Tier 4', value: 'TIER_4' }
              ]},
              { name: 'floorSpaceSqFt', label: 'Floor Space (sq ft)', type: 'number' as const },
              { name: 'rackCount', label: 'Total Racks', type: 'number' as const },
              { name: 'rackUsed', label: 'Used Racks', type: 'number' as const }
            ]
          }, {
            title: "Power & Cooling",
            fields: [
              { name: 'powerCapacityKw', label: 'Power Capacity (kW)', type: 'number' as const },
              { name: 'powerUsageKw', label: 'Power Usage (kW)', type: 'number' as const },
              { name: 'coolingType', label: 'Cooling Type', type: 'select' as const, options: [
                { label: 'Air Cooling', value: 'AIR' },
                { label: 'Liquid Cooling', value: 'LIQUID' },
                { label: 'Hybrid Cooling', value: 'HYBRID' }
              ]},
              { name: 'redundancyLevel', label: 'Redundancy', type: 'select' as const, options: [
                { label: 'N', value: 'N' },
                { label: 'N+1', value: 'N_PLUS_1' },
                { label: 'N+N', value: 'N_PLUS_N' },
                { label: '2N', value: '2N' }
              ]}
            ]
          }],
          "Operations": [{
            title: "Operational Data",
            fields: [
              { name: 'servers', label: 'Server Count', type: 'number' as const },
              { name: 'storageTb', label: 'Storage (TB)', type: 'number' as const },
              { name: 'utilization', label: 'Utilization (%)', type: 'number' as const },
              { name: 'internetBandwidthGbps', label: 'Internet Bandwidth (Gbps)', type: 'number' as const },
              { name: 'slaUptime', label: 'SLA Uptime (%)', type: 'number' as const }
            ]
          }, {
            title: "Environmental",
            fields: [
              { name: 'temperatureRangeMin', label: 'Min Temperature (°C)', type: 'number' as const },
              { name: 'temperatureRangeMax', label: 'Max Temperature (°C)', type: 'number' as const },
              { name: 'humidityRangeMin', label: 'Min Humidity (%)', type: 'number' as const },
              { name: 'humidityRangeMax', label: 'Max Humidity (%)', type: 'number' as const }
            ]
          }]
        };
      case 'compute':
        return {
          "General": [{
            title: "Basic Information",
            fields: [
              { name: 'name', label: 'Name', type: 'text' as const, required: true },
              { name: 'description', label: 'Description', type: 'textarea' as const },
              { name: 'hostname', label: 'Hostname', type: 'text' as const },
              { name: 'ipAddress', label: 'Primary IP', type: 'text' as const },
              { name: 'macAddress', label: 'MAC Address', type: 'text' as const },
              { name: 'status', label: 'Status', type: 'select' as const, options: [
                { label: 'Active', value: 'ACTIVE' },
                { label: 'Inactive', value: 'INACTIVE' },
                { label: 'Maintenance', value: 'MAINTENANCE' }
              ]}
            ]
          }],
          "Hardware": [{
            title: "System Specifications",
            fields: [
              { name: 'computeType', label: 'Type', type: 'select' as const, options: [
                { label: 'Dedicated', value: 'DEDICATED' },
                { label: 'Virtual Machine', value: 'VM' }
              ]},
              { name: 'manufacturer', label: 'Manufacturer', type: 'text' as const },
              { name: 'model', label: 'Model', type: 'text' as const },
              { name: 'serialNumber', label: 'Serial Number', type: 'text' as const },
              { name: 'architecture', label: 'Architecture', type: 'select' as const, options: [
                { label: 'x86_64', value: 'x86_64' },
                { label: 'ARM64', value: 'ARM64' },
                { label: 'x86', value: 'x86' },
                { label: 'ARM', value: 'ARM' }
              ]}
            ]
          }, {
            title: "CPU & Memory",
            fields: [
              { name: 'cpuModel', label: 'CPU Model', type: 'text' as const },
              { name: 'cpuCores', label: 'CPU Cores', type: 'number' as const },
              { name: 'cpuSpeed', label: 'CPU Speed (GHz)', type: 'number' as const },
              { name: 'cpuSockets', label: 'CPU Sockets', type: 'number' as const },
              { name: 'memoryGb', label: 'Memory (GB)', type: 'number' as const },
              { name: 'memoryType', label: 'Memory Type', type: 'select' as const, options: [
                { label: 'DDR3', value: 'DDR3' },
                { label: 'DDR4', value: 'DDR4' },
                { label: 'DDR5', value: 'DDR5' }
              ]}
            ]
          }, {
            title: "Storage",
            fields: [
              { name: 'diskGb', label: 'Disk Space (GB)', type: 'number' as const },
              { name: 'diskType', label: 'Disk Type', type: 'select' as const, options: [
                { label: 'HDD', value: 'HDD' },
                { label: 'SSD', value: 'SSD' },
                { label: 'NVMe', value: 'NVME' },
                { label: 'Hybrid', value: 'HYBRID' }
              ]},
              { name: 'diskInterface', label: 'Disk Interface', type: 'select' as const, options: [
                { label: 'SATA', value: 'SATA' },
                { label: 'SAS', value: 'SAS' },
                { label: 'NVMe', value: 'NVME' },
                { label: 'IDE', value: 'IDE' }
              ]},
              { name: 'raidConfiguration', label: 'RAID Configuration', type: 'text' as const }
            ]
          }],
          "Software": [{
            title: "Operating System",
            fields: [
              { name: 'operatingSystem', label: 'OS', type: 'text' as const },
              { name: 'osVersion', label: 'OS Version', type: 'text' as const },
              { name: 'osDistribution', label: 'OS Distribution', type: 'text' as const },
              { name: 'biosVersion', label: 'BIOS Version', type: 'text' as const }
            ]
          }, {
            title: "Virtualization",
            fields: [
              { name: 'virtualizationPlatform', label: 'Platform', type: 'select' as const, options: [
                { label: 'VMware', value: 'VMWARE' },
                { label: 'Hyper-V', value: 'HYPER_V' },
                { label: 'KVM', value: 'KVM' },
                { label: 'Xen', value: 'XEN' },
                { label: 'Proxmox', value: 'PROXMOX' },
                { label: 'Bare Metal', value: 'BARE_METAL' }
              ]},
              { name: 'hypervisorVersion', label: 'Hypervisor Version', type: 'text' as const }
            ]
          }],
          "Management": [{
            title: "Security & Compliance",
            fields: [
              { name: 'encryptionEnabled', label: 'Encryption Enabled', type: 'boolean' as const },
              { name: 'complianceProfile', label: 'Compliance Profile', type: 'text' as const },
              { name: 'lastPatchDate', label: 'Last Patch Date', type: 'text' as const },
              { name: 'vulnerabilityScanDate', label: 'Last Vulnerability Scan', type: 'text' as const }
            ]
          }, {
            title: "Monitoring & Management",
            fields: [
              { name: 'managementIp', label: 'Management IP', type: 'text' as const },
              { name: 'iloIp', label: 'iLO/IPMI IP', type: 'text' as const },
              { name: 'monitoringAgent', label: 'Monitoring Agent', type: 'text' as const },
              { name: 'backupAgent', label: 'Backup Agent', type: 'text' as const },
              { name: 'lastHealthCheck', label: 'Last Health Check', type: 'text' as const }
            ]
          }]
        };
      case 'container':
        return {
          "General": [{
            title: "Basic Information",
            fields: [
              { name: 'name', label: 'Name', type: 'text' as const, required: true },
              { name: 'description', label: 'Description', type: 'textarea' as const },
              { name: 'containerType', label: 'Type', type: 'select' as const, options: [
                { label: 'LXD', value: 'LXD' },
                { label: 'Docker', value: 'DOCKER' },
                { label: 'LXC', value: 'LXC' },
                { label: 'Podman', value: 'PODMAN' },
                { label: 'Containerd', value: 'CONTAINERD' },
                { label: 'Kubernetes', value: 'KUBERNETES' }
              ]},
              { name: 'containerId', label: 'Container ID', type: 'text' as const },
              { name: 'status', label: 'Status', type: 'select' as const, options: [
                { label: 'Running', value: 'RUNNING' },
                { label: 'Stopped', value: 'STOPPED' },
                { label: 'Paused', value: 'PAUSED' }
              ]}
            ]
          }],
          "Image & Runtime": [{
            title: "Container Image",
            fields: [
              { name: 'image', label: 'Image', type: 'text' as const },
              { name: 'imageVersion', label: 'Image Version', type: 'text' as const },
              { name: 'imageRegistry', label: 'Image Registry', type: 'text' as const },
              { name: 'imagePullPolicy', label: 'Pull Policy', type: 'select' as const, options: [
                { label: 'Always', value: 'ALWAYS' },
                { label: 'If Not Present', value: 'IF_NOT_PRESENT' },
                { label: 'Never', value: 'NEVER' }
              ]}
            ]
          }, {
            title: "Runtime Configuration",
            fields: [
              { name: 'restartPolicy', label: 'Restart Policy', type: 'select' as const, options: [
                { label: 'No', value: 'NO' },
                { label: 'On Failure', value: 'ON_FAILURE' },
                { label: 'Always', value: 'ALWAYS' },
                { label: 'Unless Stopped', value: 'UNLESS_STOPPED' }
              ]},
              { name: 'privileged', label: 'Privileged', type: 'boolean' as const },
              { name: 'readOnly', label: 'Read Only', type: 'boolean' as const },
              { name: 'autoStart', label: 'Auto Start', type: 'boolean' as const }
            ]
          }],
          "Resources": [{
            title: "Resource Limits",
            fields: [
              { name: 'cpuLimit', label: 'CPU Limit', type: 'text' as const },
              { name: 'cpuRequest', label: 'CPU Request', type: 'text' as const },
              { name: 'memoryLimit', label: 'Memory Limit', type: 'text' as const },
              { name: 'memoryRequest', label: 'Memory Request', type: 'text' as const },
              { name: 'diskLimit', label: 'Disk Limit', type: 'text' as const }
            ]
          }],
          "Networking": [{
            title: "Network Configuration",
            fields: [
              { name: 'ipAddress', label: 'IP Address', type: 'text' as const },
              { name: 'exposedPorts', label: 'Exposed Ports', type: 'text' as const },
              { name: 'networkMode', label: 'Network Mode', type: 'select' as const, options: [
                { label: 'Bridge', value: 'BRIDGE' },
                { label: 'Host', value: 'HOST' },
                { label: 'None', value: 'NONE' },
                { label: 'Container', value: 'CONTAINER' },
                { label: 'Custom', value: 'CUSTOM' }
              ]},
              { name: 'publishAllPorts', label: 'Publish All Ports', type: 'boolean' as const }
            ]
          }],
          "Configuration": [{
            title: "Environment & Storage",
            fields: [
              { name: 'environmentVars', label: 'Environment Variables', type: 'textarea' as const },
              { name: 'mountPoints', label: 'Mount Points', type: 'textarea' as const },
              { name: 'workingDirectory', label: 'Working Directory', type: 'text' as const },
              { name: 'user', label: 'User', type: 'text' as const }
            ]
          }, {
            title: "Monitoring & Health",
            fields: [
              { name: 'monitoringEnabled', label: 'Monitoring Enabled', type: 'boolean' as const },
              { name: 'healthCheckCommand', label: 'Health Check Command', type: 'text' as const },
              { name: 'healthCheckInterval', label: 'Health Check Interval (s)', type: 'number' as const },
              { name: 'logDriver', label: 'Log Driver', type: 'select' as const, options: [
                { label: 'JSON File', value: 'JSON_FILE' },
                { label: 'Syslog', value: 'SYSLOG' },
                { label: 'Journald', value: 'JOURNALD' },
                { label: 'GELF', value: 'GELF' },
                { label: 'Fluentd', value: 'FLUENTD' }
              ]}
            ]
          }]
        };
      default:
        return {
          "General": [{
            title: "No Configuration Available",
            fields: []
          }]
        };
    }
  };

  const handleView = (item: any) => {
    setModalData(item);
    setModalMode('view');
    setModalOpen(true);
  };

  const handleEdit = (item: any) => {
    setModalData(item);
    setModalMode('edit');
    setModalOpen(true);
  };

  const handleAdd = () => {
    setModalData(null);
    setModalMode('create');
    setModalOpen(true);
  };

  const handleDelete = async (item: any) => {
    if (window.confirm(`Are you sure you want to delete "${item.name}"?`)) {
      try {
        let endpoint = '';
        switch (entityType) {
          case 'cloud':
            endpoint = `${API_BASE_URL}/clouds/${item.id}`;
            break;
          case 'datacenter':
            endpoint = `${API_BASE_URL}/datacenters/${item.id}`;
            break;
          case 'compute':
            endpoint = `${API_BASE_URL}/computes/${item.id}`;
            break;
          case 'container':
            endpoint = `${API_BASE_URL}/containers/${item.id}`;
            break;
        }
        
        await axios.delete(endpoint);
        await refreshClouds();
      } catch (error) {
        console.error('Error deleting item:', error);
        alert('Failed to delete item');
      }
    }
  };

  const handleSave = async (data: any) => {
    try {
      let endpoint = '';
      let method = modalMode === 'create' ? 'post' : 'put';
      
      switch (entityType) {
        case 'cloud':
          endpoint = modalMode === 'create' ? `${API_BASE_URL}/clouds` : `${API_BASE_URL}/clouds/${data.id}`;
          break;
        case 'datacenter':
          endpoint = modalMode === 'create' ? `${API_BASE_URL}/datacenters` : `${API_BASE_URL}/datacenters/${data.id}`;
          if (modalMode === 'create' && selectedNode?.type === 'cloud') {
            data.cloud = { id: selectedNode.entityId };
          }
          break;
        case 'compute':
          endpoint = modalMode === 'create' ? `${API_BASE_URL}/computes` : `${API_BASE_URL}/computes/${data.id}`;
          if (modalMode === 'create') {
            if (selectedNode?.type === 'datacenter') {
              data.datacenter = { id: selectedNode.entityId };
              data.cloud = { id: selectedNode.data?.cloud?.id };
            } else if (selectedNode?.type === 'cloud') {
              data.cloud = { id: selectedNode.entityId };
            }
          }
          break;
        case 'container':
          endpoint = modalMode === 'create' ? `${API_BASE_URL}/containers` : `${API_BASE_URL}/containers/${data.id}`;
          if (modalMode === 'create' && selectedNode?.type === 'compute') {
            data.compute = { id: selectedNode.entityId };
          }
          break;
      }
      
      if (method === 'post') {
        await axios.post(endpoint, data);
      } else {
        await axios.put(endpoint, data);
      }
      
      await refreshClouds();
    } catch (error) {
      console.error('Error saving item:', error);
      throw error;
    }
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" height="100vh">
        <CircularProgress />
      </Box>
    );
  }

  // Generate breadcrumb navigation
  const getBreadcrumbs = () => {
    if (!selectedNode) return ['Infrastructure'];
    
    const breadcrumbs = ['Infrastructure'];
    
    switch (selectedNode.type) {
      case 'category':
        if (selectedNode.id === 'clouds') {
          breadcrumbs.push('Cloud Instances');
        } else if (selectedNode.id.includes('-datacenters')) {
          const cloudName = selectedNode.data?.cloud?.name || 'Cloud';
          breadcrumbs.push('Cloud', cloudName, 'Data Centers');
        } else if (selectedNode.id.includes('-computes')) {
          const cloudName = selectedNode.data?.cloud?.name || 'Cloud';
          const datacenterName = selectedNode.data?.datacenter?.name || 'Datacenter';
          breadcrumbs.push('Cloud', cloudName, 'Datacenter', datacenterName, 'Compute Nodes');
        } else if (selectedNode.id.includes('-containers')) {
          const cloudName = selectedNode.data?.cloud?.name || 'Cloud';
          const datacenterName = selectedNode.data?.datacenter?.name || 'Datacenter';
          const computeName = selectedNode.data?.compute?.name || 'Compute';
          breadcrumbs.push('Cloud', cloudName, 'Datacenter', datacenterName, 'Compute', computeName, 'Containers');
        }
        break;
      case 'cloud':
        breadcrumbs.push('Cloud', selectedNode.name);
        break;
      case 'datacenter':
        const cloudName = selectedNode.data?.cloud?.name || 'Cloud';
        breadcrumbs.push('Cloud', cloudName, 'Datacenter', selectedNode.name);
        break;
      case 'compute':
        const computeCloudName = selectedNode.data?.cloud?.name || 'Cloud';
        const computeDatacenterName = selectedNode.data?.datacenter?.name || 'Datacenter';
        breadcrumbs.push('Cloud', computeCloudName, 'Datacenter', computeDatacenterName, 'Compute', selectedNode.name);
        break;
      case 'container':
        const containerCloudName = selectedNode.data?.cloud?.name || 'Cloud';
        const containerDatacenterName = selectedNode.data?.datacenter?.name || 'Datacenter';
        const containerComputeName = selectedNode.data?.compute?.name || 'Compute';
        breadcrumbs.push('Cloud', containerCloudName, 'Datacenter', containerDatacenterName, 'Compute', containerComputeName, 'Container', selectedNode.name);
        break;
      case 'container-group':
        const groupCloudName = selectedNode.data?.cloud?.name || 'Cloud';
        const groupDatacenterName = selectedNode.data?.datacenter?.name || 'Datacenter';
        const groupComputeName = selectedNode.data?.compute?.name || 'Compute';
        breadcrumbs.push('Cloud', groupCloudName, 'Datacenter', groupDatacenterName, 'Compute', groupComputeName, selectedNode.name + ' Containers');
        break;
    }
    
    return breadcrumbs;
  };

  // Render content based on selected node type
  const renderContent = () => {
    if (!selectedNode) {
      return (
        <Box sx={{ p: 4, textAlign: 'center' }}>
          <Typography variant="h5" gutterBottom color="primary">
            Infrastructure
          </Typography>
          <Typography variant="body1" color="textSecondary" sx={{ mt: 2, maxWidth: 600, mx: 'auto', lineHeight: 1.6 }}>
            Manage your complete infrastructure ecosystem from this centralized dashboard. 
            Navigate through your cloud instances, data centers, compute nodes, and containers. 
            Use the sidebar to explore your infrastructure hierarchy and manage resources efficiently.
          </Typography>
          <Box sx={{ mt: 4, display: 'flex', justifyContent: 'center', gap: 2, flexWrap: 'wrap' }}>
            <Box sx={{ textAlign: 'center', minWidth: 120 }}>
              <Typography variant="h6" color="primary">{clouds.length}</Typography>
              <Typography variant="body2" color="textSecondary">Cloud Instances</Typography>
            </Box>
            <Box sx={{ textAlign: 'center', minWidth: 120 }}>
              <Typography variant="h6" color="primary">
                {clouds.reduce((total, cloud) => total + (cloud.datacenters?.length || 0), 0)}
              </Typography>
              <Typography variant="body2" color="textSecondary">Data Centers</Typography>
            </Box>
            <Box sx={{ textAlign: 'center', minWidth: 120 }}>
              <Typography variant="h6" color="primary">
                {clouds.reduce((total, cloud) => total + (cloud.computes?.length || 0), 0)}
              </Typography>
              <Typography variant="body2" color="textSecondary">Compute Nodes</Typography>
            </Box>
          </Box>
        </Box>
      );
    }

    // For individual entities (cloud, datacenter, compute, container), show entity details
    if (['cloud', 'datacenter', 'compute', 'container'].includes(selectedNode.type)) {
      return (
        <Box sx={{ p: 3 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
            <Box>
              <Typography variant="h5" gutterBottom>
                {selectedNode.name}
              </Typography>
              <Typography variant="body2" color="textSecondary">
                {selectedNode.type.charAt(0).toUpperCase() + selectedNode.type.slice(1)} Details
              </Typography>
            </Box>
            <Box sx={{ display: 'flex', gap: 1 }}>
              <Button variant="outlined" onClick={() => selectedNode.data && handleEdit(selectedNode.data)}>
                Edit
              </Button>
              <Button variant="outlined" color="error" onClick={() => selectedNode.data && handleDelete(selectedNode.data)}>
                Delete
              </Button>
            </Box>
          </Box>
          
          <Box sx={{ bgcolor: 'background.paper', borderRadius: 1, p: 3, border: 1, borderColor: 'divider' }}>
            {/* Render entity details based on type */}
            {selectedNode.type === 'cloud' && selectedNode.data && (
              <>
                <Typography variant="body1"><strong>Client:</strong> {selectedNode.data.clientName}</Typography>
                <Typography variant="body1"><strong>Region:</strong> {selectedNode.data.deploymentRegion}</Typography>
                <Typography variant="body1"><strong>Status:</strong> {selectedNode.data.status}</Typography>
                <Typography variant="body1"><strong>Description:</strong> {selectedNode.data.description}</Typography>
              </>
            )}
            {selectedNode.type === 'datacenter' && selectedNode.data && (
              <>
                <Typography variant="body1"><strong>Location:</strong> {selectedNode.data.locationOther}</Typography>
                <Typography variant="body1"><strong>Type:</strong> {selectedNode.data.type}</Typography>
                <Typography variant="body1"><strong>Provider:</strong> {selectedNode.data.provider}</Typography>
                <Typography variant="body1"><strong>DR Site:</strong> {selectedNode.data.isDrSite ? 'Yes' : 'No'}</Typography>
                <Typography variant="body1"><strong>Servers:</strong> {selectedNode.data.servers}</Typography>
                <Typography variant="body1"><strong>Storage:</strong> {selectedNode.data.storageTb} TB</Typography>
                <Typography variant="body1"><strong>Utilization:</strong> {selectedNode.data.utilization}%</Typography>
              </>
            )}
            {selectedNode.type === 'compute' && selectedNode.data && (
              <>
                <Typography variant="body1"><strong>Type:</strong> {selectedNode.data.computeType}</Typography>
                <Typography variant="body1"><strong>Hostname:</strong> {selectedNode.data.hostname}</Typography>
                <Typography variant="body1"><strong>IP Address:</strong> {selectedNode.data.ipAddress}</Typography>
                <Typography variant="body1"><strong>Operating System:</strong> {selectedNode.data.operatingSystem}</Typography>
                <Typography variant="body1"><strong>CPU Cores:</strong> {selectedNode.data.cpuCores}</Typography>
                <Typography variant="body1"><strong>Memory:</strong> {selectedNode.data.memoryGb} GB</Typography>
                <Typography variant="body1"><strong>Disk:</strong> {selectedNode.data.diskGb} GB</Typography>
              </>
            )}
            {selectedNode.type === 'container' && selectedNode.data && (
              <>
                <Typography variant="body1"><strong>Type:</strong> {selectedNode.data.containerType}</Typography>
                <Typography variant="body1"><strong>Image:</strong> {selectedNode.data.image}</Typography>
                <Typography variant="body1"><strong>Version:</strong> {selectedNode.data.imageVersion}</Typography>
                <Typography variant="body1"><strong>IP Address:</strong> {selectedNode.data.ipAddress}</Typography>
                <Typography variant="body1"><strong>Status:</strong> {selectedNode.data.status}</Typography>
                <Typography variant="body1"><strong>Auto Start:</strong> {selectedNode.data.autoStart ? 'Yes' : 'No'}</Typography>
                <Typography variant="body1"><strong>CPU Limit:</strong> {selectedNode.data.cpuLimit}</Typography>
                <Typography variant="body1"><strong>Memory Limit:</strong> {selectedNode.data.memoryLimit}</Typography>
              </>
            )}
          </Box>
        </Box>
      );
    }

    // For categories and lists, show CRUD interface
    return (
      <Box sx={{ p: 3, height: '100%', display: 'flex', flexDirection: 'column' }}>
        <CrudList
          title={listTitle}
          data={listData}
          columns={columns}
          searchFields={searchFields}
          onAdd={entityType ? handleAdd : undefined}
          onView={handleView}
          onEdit={handleEdit}
          onDelete={handleDelete}
          filterField={filterConfig?.field}
          filterOptions={filterConfig?.options}
          loading={loading}
          emptyMessage={selectedNode ? `No ${listTitle.toLowerCase()} found` : 'Select an item from the sidebar to view details'}
        />
      </Box>
    );
  };

  return (
    <Box sx={{ flexGrow: 1, height: 'calc(100vh - 100px)' }}>
      {/* Breadcrumb Navigation */}
      <Box sx={{ px: 3, py: 2, bgcolor: 'background.paper', borderBottom: 1, borderColor: 'divider' }}>
        <Typography variant="h6" sx={{ fontWeight: 500 }}>
          {getBreadcrumbs().join(' / ')}
        </Typography>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mx: 3, mt: 2 }}>
          {error}
        </Alert>
      )}

      <Box sx={{ height: 'calc(100% - 80px)', overflow: 'auto' }}>
        {renderContent()}
      </Box>

      <TabbedEntityModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        title={entityType}
        data={modalData}
        fieldGroups={getFieldGroups(entityType)}
        mode={modalMode}
        onSave={handleSave}
        onDelete={modalMode !== 'create' ? handleDelete : undefined}
      />
    </Box>
  );
};

export default Infrastructure;