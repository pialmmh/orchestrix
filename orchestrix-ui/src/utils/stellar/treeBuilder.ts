import { TreeNode, Partner, Cloud, Datacenter, Compute, NetworkDevice } from '../../models/entities/TreeNode';

export function transformStellarToTree(data: any[]): TreeNode[] {
  if (!data || !Array.isArray(data)) {
    return [];
  }

  // The API now returns hierarchical data directly, not flat data with prefixes
  // Data structure is an array of partners with nested clouds, datacenters, etc.
  return data.map(partner => transformPartnerToTreeNode(partner));
}

function transformPartnerToTreeNode(partner: any): TreeNode {
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

  // Add environments as children
  if (partner.environments && Array.isArray(partner.environments)) {
    partner.environments.forEach((env: any) => {
      partnerNode.children!.push(transformEnvironmentToTreeNode(env));
    });
  }

  // Add clouds as children
  if (partner.clouds && Array.isArray(partner.clouds)) {
    partner.clouds.forEach((cloud: any) => {
      partnerNode.children!.push(transformCloudToTreeNode(cloud));
    });
  }

  return partnerNode;
}

function transformCloudToTreeNode(cloud: any): TreeNode {
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

  // Add datacenters as children
  if (cloud.datacenters && Array.isArray(cloud.datacenters)) {
    cloudNode.children = cloud.datacenters.map((dc: any) => transformDatacenterToTreeNode(dc));
  }

  return cloudNode;
}

function transformDatacenterToTreeNode(datacenter: any): TreeNode {
  const dcNode: TreeNode = {
    id: `datacenter-${datacenter.id}`,
    name: datacenter.name,
    type: 'datacenter',
    data: {
      id: datacenter.id,
      name: datacenter.name,
      location: datacenter.location,
      status: datacenter.status,
      cloudId: datacenter.cloudId,
      tier: datacenter.tier,
    } as Datacenter,
    children: [],
    metadata: {
      tier: datacenter.tier,
    },
  };

  // Add availability zones as children
  if (datacenter.availabilityzones && Array.isArray(datacenter.availabilityzones)) {
    datacenter.availabilityzones.forEach((az: any) => {
      dcNode.children!.push(transformAvailabilityZoneToTreeNode(az));
    });
  }

  // Add racks as children
  if (datacenter.racks && Array.isArray(datacenter.racks)) {
    datacenter.racks.forEach((rack: any) => {
      dcNode.children!.push(transformRackToTreeNode(rack));
    });
  }

  // Add computes as children (not in racks)
  if (datacenter.computes && Array.isArray(datacenter.computes)) {
    datacenter.computes.forEach((compute: any) => {
      dcNode.children!.push(transformComputeToTreeNode(compute));
    });
  }

  // Add network devices as children
  if (datacenter.networkdevices && Array.isArray(datacenter.networkdevices)) {
    datacenter.networkdevices.forEach((device: any) => {
      dcNode.children!.push(transformNetworkDeviceToTreeNode(device));
    });
  }

  // Add storage as children
  if (datacenter.storages && Array.isArray(datacenter.storages)) {
    datacenter.storages.forEach((storage: any) => {
      dcNode.children!.push(transformStorageToTreeNode(storage));
    });
  }

  // Add virtual networks as children
  if (datacenter.virtualnetworks && Array.isArray(datacenter.virtualnetworks)) {
    datacenter.virtualnetworks.forEach((vn: any) => {
      dcNode.children!.push(transformVirtualNetworkToTreeNode(vn));
    });
  }

  return dcNode;
}

function transformComputeToTreeNode(compute: any): TreeNode {
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
    } as Compute,
    metadata: {
      hostname: compute.hostname,
      ipAddress: compute.ipAddress,
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
  return {
    id: `environment-${env.id}`,
    name: env.name || env.code,
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
  };
}

function transformAvailabilityZoneToTreeNode(az: any): TreeNode {
  return {
    id: `az-${az.id}`,
    name: az.name || az.zoneCode,
    type: 'az',
    data: {
      id: az.id,
      name: az.name,
      zoneCode: az.zoneCode,
      subnetRange: az.subnetRange,
      status: az.status,
      datacenterId: az.datacenterId,
    },
  };
}

function transformRackToTreeNode(rack: any): TreeNode {
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
      rackNode.children!.push(transformComputeToTreeNode(compute));
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