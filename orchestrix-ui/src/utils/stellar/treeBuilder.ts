import { TreeNode, Partner, Cloud, Datacenter, Compute, NetworkDevice } from '../../models/entities/TreeNode';

export function transformStellarToTree(data: any, environments?: any[]): TreeNode[] {
  console.log('🎯 transformStellarToTree called with:', data, 'environments:', environments);

  // Handle both direct array and response object with data property
  let dataArray = data;
  if (data && typeof data === 'object' && !Array.isArray(data)) {
    if ('data' in data) {
      console.log('📦 Extracting data array from response object');
      dataArray = data.data;
    } else {
      console.warn('⚠️ Data is object but no data property found:', data);
      return [];
    }
  }

  if (!dataArray || !Array.isArray(dataArray)) {
    console.warn('⚠️ transformStellarToTree: Invalid data - not an array or null:', dataArray);
    return [];
  }

  console.log(`📊 transformStellarToTree: Processing ${dataArray.length} partners`);

  // Create environment map for quick lookup
  const environmentMap = new Map<number, any>();
  if (environments && Array.isArray(environments)) {
    environments.forEach(env => {
      environmentMap.set(env.id, env);
    });
  }

  // The API now returns hierarchical data directly, not flat data with prefixes
  // Data structure is an array of partners with nested clouds, datacenters, etc.
  const result = dataArray.map((partner, index) => {
    console.log(`  🏢 Processing partner ${index + 1}:`, partner.name || partner.id);
    return transformPartnerToTreeNode(partner, environmentMap);
  });

  console.log('✅ transformStellarToTree result:', JSON.stringify(result, null, 2));
  return result;
}

function transformPartnerToTreeNode(partner: any, environmentMap?: Map<number, any>): TreeNode {
  const partnerNode: TreeNode = {
    id: `partner-${partner.id}`,
    name: partner.displayName || partner.name,
    type: 'partner',
    data: {
      id: partner.id,
      name: partner.name,
      displayName: partner.displayName,
      type: partner.type,
      roles: partner.roles || [],
      status: partner.status,
      contactEmail: partner.contactEmail,
      website: partner.website,
    } as Partner,
    children: [],
  };

  // Store environments for reference but don't add as separate nodes
  // They'll be shown as metadata on datacenters
  if (partner.environments && Array.isArray(partner.environments)) {
    partner.environments.forEach((env: any) => {
      if (environmentMap) {
        environmentMap.set(env.id, env);
      }
    });
  }

  // Add clouds as children - these form the primary hierarchy
  if (partner.clouds && Array.isArray(partner.clouds)) {
    partner.clouds.forEach((cloud: any) => {
      partnerNode.children!.push(transformCloudToTreeNode(cloud, environmentMap));
    });
  }

  return partnerNode;
}

function transformCloudToTreeNode(cloud: any, environmentMap?: Map<number, any>): TreeNode {
  const cloudNode: TreeNode = {
    id: `cloud-${cloud.id}`,
    name: cloud.name,
    type: 'cloud',
    data: {
      id: cloud.id,
      name: cloud.name,
      description: cloud.description,
      deploymentRegion: cloud.deploymentRegion,
      status: cloud.status,
      partnerId: cloud.partnerId,
    } as Cloud,
    children: [],
  };

  // Add regions as children
  if (cloud.regions && Array.isArray(cloud.regions)) {
    cloud.regions.forEach((region: any) => {
      cloudNode.children!.push(transformRegionToTreeNode(region, environmentMap));
    });
  }

  // Add datacenters as direct children (if not under regions)
  if (cloud.datacenters && Array.isArray(cloud.datacenters)) {
    cloudNode.children!.push(...cloud.datacenters.map((dc: any) => transformDatacenterToTreeNode(dc, environmentMap)));
  }

  return cloudNode;
}

function transformRegionToTreeNode(region: any, environmentMap?: Map<number, any>): TreeNode {
  const regionNode: TreeNode = {
    id: `region-${region.id}`,
    name: region.name,
    type: 'region',
    data: {
      id: region.id,
      name: region.name,
      code: region.code,
      geographicArea: region.geographicArea || region.geographic_area,
      complianceZones: region.complianceZones || region.compliance_zones,
      status: region.status,
      cloudId: region.cloudId || region.cloud_id,
    },
    children: [],
  };

  // Add availability zones as children
  if (region.availabilityzones && Array.isArray(region.availabilityzones)) {
    region.availabilityzones.forEach((az: any) => {
      regionNode.children!.push(transformAvailabilityZoneToTreeNode(az, environmentMap));
    });
  }

  return regionNode;
}

function transformDatacenterToTreeNode(datacenter: any, environmentMap?: Map<number, any>): TreeNode {
  // Get environment info if available
  let environmentInfo = null;
  const envId = datacenter.environmentId || datacenter.environment_id;
  if (environmentMap && envId) {
    environmentInfo = environmentMap.get(envId);
  }

  const dcNode: TreeNode = {
    id: `datacenter-${datacenter.id}`,
    name: environmentInfo ? `${datacenter.name} (${environmentInfo.type || environmentInfo.name})` : datacenter.name,
    type: 'datacenter',
    data: {
      id: datacenter.id,
      name: datacenter.name,
      location: datacenter.location,
      status: datacenter.status,
      cloudId: datacenter.cloudId,
      tier: datacenter.tier,
      environmentId: envId,
      environment: environmentInfo,
    } as Datacenter & { environmentId?: number; environment?: any },
    children: [],
    metadata: {
      tier: datacenter.tier,
      environment: environmentInfo?.type || environmentInfo?.name,
      environmentId: envId,
    },
  };

  // Add resource pools as children
  if (datacenter.resourcepools && Array.isArray(datacenter.resourcepools)) {
    datacenter.resourcepools.forEach((pool: any) => {
      dcNode.children!.push(transformResourcePoolToTreeNode(pool, environmentMap));
    });
  }

  // Add computes as children (not in resource pools)
  if (datacenter.computes && Array.isArray(datacenter.computes)) {
    datacenter.computes.forEach((compute: any) => {
      dcNode.children!.push(transformComputeToTreeNode(compute, environmentMap));
    });
  }

  // Add network devices as children
  if (datacenter.networkdevices && Array.isArray(datacenter.networkdevices)) {
    datacenter.networkdevices.forEach((device: any) => {
      dcNode.children!.push(transformNetworkDeviceToTreeNode(device));
    });
  }

  return dcNode;
}

function transformComputeToTreeNode(compute: any, environmentMap?: Map<number, any>): TreeNode {
  // Get environment associations for this compute
  let computeEnvironments: any[] = [];
  if (compute.environmentassociations && Array.isArray(compute.environmentassociations)) {
    computeEnvironments = compute.environmentassociations
      .filter((ea: any) => ea.resource_type === 'compute' && ea.resource_id === compute.id)
      .map((ea: any) => {
        const env = environmentMap?.get(ea.environment_id);
        return {
          id: ea.environment_id,
          name: env?.type || env?.name || 'Unknown',
          percentage: ea.allocated_percentage,
          isPrimary: ea.is_primary
        };
      });
  }

  // Build environment display string
  const envDisplay = computeEnvironments
    .map(e => e.percentage ? `${e.name}: ${e.percentage}%` : e.name)
    .join(', ');

  const computeNode: TreeNode = {
    id: `compute-${compute.id}`,
    name: compute.name || compute.hostname || `Compute-${compute.id}`,
    type: 'compute',
    data: {
      id: compute.id,
      name: compute.name,
      hostname: compute.hostname,
      ipAddress: compute.ipAddress,
      status: compute.status,
      datacenterId: compute.datacenterId,
      cpuCores: compute.cpuCores,
      memoryGb: compute.memoryGb || compute.ramGb,
      diskGb: compute.diskGb,
      nodeType: compute.nodeType || compute.hostType,
      environments: computeEnvironments,
      capabilities: compute.computecapabilities,
    } as Compute & { environments?: any[]; capabilities?: any[] },
    metadata: {
      hostname: compute.hostname,
      ipAddress: compute.ipAddress,
      environments: envDisplay,
    },
    children: [],
  };

  // Add containers as children
  if (compute.containers && Array.isArray(compute.containers)) {
    compute.containers.forEach((container: any) => {
      computeNode.children!.push(transformContainerToTreeNode(container));
    });
  }

  // Add IP addresses as children
  if (compute.ipaddresses && Array.isArray(compute.ipaddresses)) {
    compute.ipaddresses.forEach((ip: any) => {
      computeNode.children!.push(transformIPAddressToTreeNode(ip));
    });
  }

  return computeNode;
}

function transformNetworkDeviceToTreeNode(device: any): TreeNode {
  const deviceNode: TreeNode = {
    id: `network-device-${device.id}`,
    name: device.name || device.hostname || `NetworkDevice-${device.id}`,
    type: 'network-device',
    data: {
      id: device.id,
      name: device.name,
      hostname: device.hostname,
      ipAddress: device.managementIp || device.ipAddress,
      deviceType: device.deviceType,
      status: device.status,
      datacenterId: device.datacenterId,
    } as NetworkDevice,
    metadata: {
      ipAddress: device.managementIp || device.ipAddress,
    },
    children: [],
  };

  // Add IP addresses as children
  if (device.ipaddresses && Array.isArray(device.ipaddresses)) {
    device.ipaddresses.forEach((ip: any) => {
      deviceNode.children!.push(transformIPAddressToTreeNode(ip));
    });
  }

  return deviceNode;
}

function transformEnvironmentToTreeNode(env: any): TreeNode {
  const envNode: TreeNode = {
    id: `environment-${env.id}`,
    name: `${env.name || env.code} (${env.type || 'Environment'})`,
    type: 'environment',
    data: {
      id: env.id,
      name: env.name,
      code: env.code,
      description: env.description,
      type: env.type,
      status: env.status,
      partnerId: env.partnerId,
    },
    metadata: {
      category: env.type,
    },
    children: [],
  };

  // Add datacenters as children if present
  if (env.datacenters && Array.isArray(env.datacenters)) {
    env.datacenters.forEach((dc: any) => {
      envNode.children!.push(transformDatacenterToTreeNode(dc));
    });
  }

  return envNode;
}

function transformAvailabilityZoneToTreeNode(az: any, environmentMap?: Map<number, any>): TreeNode {
  const azNode: TreeNode = {
    id: `az-${az.id}`,
    name: az.name || az.code || `AZ-${az.id}`,
    type: 'az',
    data: {
      id: az.id,
      name: az.name,
      code: az.code,
      zoneType: az.zoneType || az.zone_type,
      capabilities: az.capabilities,
      isDefault: az.isDefault || az.is_default,
      status: az.status,
      regionId: az.regionId || az.region_id,
    },
    children: [],
  };

  // Add datacenters as children
  if (az.datacenters && Array.isArray(az.datacenters)) {
    az.datacenters.forEach((dc: any) => {
      azNode.children!.push(transformDatacenterToTreeNode(dc, environmentMap));
    });
  }

  return azNode;
}

function transformResourcePoolToTreeNode(pool: any, environmentMap?: Map<number, any>): TreeNode {
  const poolNode: TreeNode = {
    id: `resourcepool-${pool.id}`,
    name: pool.name,
    type: 'pool',
    data: {
      id: pool.id,
      name: pool.name,
      description: pool.description,
      type: pool.type,
      hypervisor: pool.hypervisor,
      orchestrator: pool.orchestrator,
      totalCpuCores: pool.totalCpuCores || pool.total_cpu_cores,
      totalMemoryGb: pool.totalMemoryGb || pool.total_memory_gb,
      totalStorageTb: pool.totalStorageTb || pool.total_storage_tb,
      usedCpuCores: pool.usedCpuCores || pool.used_cpu_cores,
      usedMemoryGb: pool.usedMemoryGb || pool.used_memory_gb,
      usedStorageTb: pool.usedStorageTb || pool.used_storage_tb,
      status: pool.status,
      datacenterId: pool.datacenterId || pool.datacenter_id,
    },
    children: [],
  };

  // Add computes as children
  if (pool.computes && Array.isArray(pool.computes)) {
    pool.computes.forEach((compute: any) => {
      poolNode.children!.push(transformComputeToTreeNode(compute, environmentMap));
    });
  }

  return poolNode;
}

function transformRackToTreeNode(rack: any, environmentMap?: Map<number, any>): TreeNode {
  const rackNode: TreeNode = {
    id: `rack-${rack.id}`,
    name: rack.name || `Rack ${rack.rowNumber}-${rack.position}`,
    type: 'pool', // Using 'pool' type for racks as it's in the TreeNode type
    data: {
      id: rack.id,
      name: rack.name,
      rowNumber: rack.rowNumber,
      position: rack.position,
      heightUnits: rack.heightUnits,
      powerCapacity: rack.powerCapacity,
      status: rack.status,
      datacenterId: rack.datacenterId,
    },
    children: [],
  };

  // Add computes in this rack
  if (rack.computes && Array.isArray(rack.computes)) {
    rack.computes.forEach((compute: any) => {
      rackNode.children!.push(transformComputeToTreeNode(compute, environmentMap));
    });
  }

  // Add network devices in this rack
  if (rack.networkdevices && Array.isArray(rack.networkdevices)) {
    rack.networkdevices.forEach((device: any) => {
      rackNode.children!.push(transformNetworkDeviceToTreeNode(device));
    });
  }

  return rackNode;
}

function transformContainerToTreeNode(container: any): TreeNode {
  return {
    id: `container-${container.id}`,
    name: container.name || container.containerId,
    type: 'container',
    data: {
      id: container.id,
      name: container.name,
      containerId: container.containerId,
      image: container.image,
      status: container.status,
      cpuLimit: container.cpuLimit,
      memoryLimit: container.memoryLimit,
      computeId: container.computeId,
    },
    metadata: {
      category: 'container',
    },
  };
}

function transformStorageToTreeNode(storage: any): TreeNode {
  return {
    id: `storage-${storage.id}`,
    name: storage.name,
    type: 'resource-group',
    data: {
      id: storage.id,
      name: storage.name,
      storageType: storage.storageType,
      protocol: storage.protocol,
      capacityGb: storage.capacityGb,
      usedGb: storage.usedGb,
      status: storage.status,
      datacenterId: storage.datacenterId,
    },
    metadata: {
      category: 'storage',
      utilization: storage.usedGb ? (storage.usedGb / storage.capacityGb) * 100 : 0,
    },
  };
}

function transformVirtualNetworkToTreeNode(vn: any): TreeNode {
  return {
    id: `vn-${vn.id}`,
    name: vn.name,
    type: 'service',
    data: {
      id: vn.id,
      name: vn.name,
      networkType: vn.networkType,
      cidr: vn.cidr,
      gateway: vn.gateway,
      vlanId: vn.vlanId,
      status: vn.status,
      cloudId: vn.cloudId,
      datacenterId: vn.datacenterId,
    },
    metadata: {
      category: 'network',
    },
  };
}

function transformIPAddressToTreeNode(ip: any): TreeNode {
  return {
    id: `ip-${ip.id}`,
    name: ip.address,
    type: 'service',
    data: {
      id: ip.id,
      address: ip.address,
      subnetMask: ip.subnetMask,
      gateway: ip.gateway,
      allocationType: ip.allocationType,
      status: ip.status,
    },
    metadata: {
      category: 'network',
      ipAddress: ip.address,
    },
  };
}

// Helper function to find a node by ID
export function findNodeById(nodes: TreeNode[], id: string): TreeNode | null {
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
}

// Helper function to get path to a node
export function getNodePath(nodes: TreeNode[], targetId: string, currentPath: TreeNode[] = []): TreeNode[] {
  for (const node of nodes) {
    if (node.id === targetId) {
      return [...currentPath, node];
    }
    if (node.children) {
      const path = getNodePath(node.children, targetId, [...currentPath, node]);
      if (path.length > currentPath.length + 1) {
        return path;
      }
    }
  }
  return [];
}