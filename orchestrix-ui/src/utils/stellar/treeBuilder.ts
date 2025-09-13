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

  // Add clouds as children
  if (partner.clouds && Array.isArray(partner.clouds)) {
    partnerNode.children = partner.clouds.map((cloud: any) => transformCloudToTreeNode(cloud));
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
    } as Datacenter,
    children: [],
  };

  // Add computes as children
  if (datacenter.computes && Array.isArray(datacenter.computes)) {
    datacenter.computes.forEach((compute: any) => {
      dcNode.children!.push(transformComputeToTreeNode(compute));
    });
  }

  // Add network devices as children
  if (datacenter.networkDevices && Array.isArray(datacenter.networkDevices)) {
    datacenter.networkDevices.forEach((device: any) => {
      dcNode.children!.push(transformNetworkDeviceToTreeNode(device));
    });
  }

  return dcNode;
}

function transformComputeToTreeNode(compute: any): TreeNode {
  return {
    id: `compute-${compute.id}`,
    name: compute.name || compute.hostname,
    type: 'compute',
    data: {
      id: compute.id,
      name: compute.name,
      hostname: compute.hostname,
      ipAddress: compute.ipAddress,
      status: compute.status,
      datacenterId: compute.datacenterId,
      cpuCores: compute.cpuCores,
      memoryGb: compute.memoryGb,
      storageGb: compute.storageGb,
    } as Compute,
    metadata: {
      ipAddress: compute.ipAddress,
    },
  };
}

function transformNetworkDeviceToTreeNode(device: any): TreeNode {
  return {
    id: `network-device-${device.id}`,
    name: device.name || device.hostname,
    type: 'network-device',
    data: {
      id: device.id,
      name: device.name,
      hostname: device.hostname,
      ipAddress: device.ipAddress,
      deviceType: device.deviceType,
      status: device.status,
      datacenterId: device.datacenterId,
    } as NetworkDevice,
    metadata: {
      ipAddress: device.ipAddress,
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