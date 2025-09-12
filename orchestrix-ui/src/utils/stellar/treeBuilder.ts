import { TreeNode, Partner, Cloud, Datacenter, Compute, NetworkDevice } from '../../models/entities/TreeNode';

export function transformStellarToTree(data: any[]): TreeNode[] {
  const treeMap = new Map<string, TreeNode>();
  const rootNodes: TreeNode[] = [];

  // Process flat data from Stellar
  data.forEach(row => {
    // Extract partner data
    if (row.p__id && !treeMap.has(`partner-${row.p__id}`)) {
      const partnerNode: TreeNode = {
        id: `partner-${row.p__id}`,
        name: row.p__display_name || row.p__name,
        type: 'organization',
        data: {
          id: row.p__id,
          name: row.p__name,
          displayName: row.p__display_name,
          type: row.p__type,
          roles: JSON.parse(row.p__roles || '[]'),
          status: row.p__status,
        } as Partner,
        children: [],
      };
      treeMap.set(partnerNode.id, partnerNode);
      rootNodes.push(partnerNode);
    }

    // Extract cloud data
    if (row.c__id && !treeMap.has(`cloud-${row.c__id}`)) {
      const cloudNode: TreeNode = {
        id: `cloud-${row.c__id}`,
        name: row.c__name,
        type: 'cloud',
        data: {
          id: row.c__id,
          name: row.c__name,
          description: row.c__description,
          status: row.c__status,
          partnerId: row.c__partner_id,
        } as Cloud,
        children: [],
      };
      treeMap.set(cloudNode.id, cloudNode);

      // Add to parent
      const parentNode = treeMap.get(`partner-${row.c__partner_id}`);
      if (parentNode && !parentNode.children?.find(c => c.id === cloudNode.id)) {
        parentNode.children!.push(cloudNode);
      }
    }

    // Extract datacenter data
    if (row.dc__id && !treeMap.has(`datacenter-${row.dc__id}`)) {
      const datacenterNode: TreeNode = {
        id: `datacenter-${row.dc__id}`,
        name: row.dc__name,
        type: 'datacenter',
        data: {
          id: row.dc__id,
          name: row.dc__name,
          type: row.dc__type,
          status: row.dc__status,
          cloudId: row.dc__cloud_id,
          tier: row.dc__tier,
        } as Datacenter,
        children: [],
        metadata: {
          tier: row.dc__tier,
          utilization: row.dc__utilization,
        },
      };
      treeMap.set(datacenterNode.id, datacenterNode);

      // Add to parent cloud
      const parentNode = treeMap.get(`cloud-${row.dc__cloud_id}`);
      if (parentNode && !parentNode.children?.find(c => c.id === datacenterNode.id)) {
        parentNode.children!.push(datacenterNode);
      }
    }

    // Extract compute data
    if (row.comp__id && !treeMap.has(`compute-${row.comp__id}`)) {
      const computeNode: TreeNode = {
        id: `compute-${row.comp__id}`,
        name: row.comp__name,
        type: 'compute',
        data: {
          id: row.comp__id,
          name: row.comp__name,
          hostname: row.comp__hostname,
          ipAddress: row.comp__ip_address,
          status: row.comp__status,
          cpuCores: row.comp__cpu_cores,
          memoryGb: row.comp__memory_gb,
          diskGb: row.comp__disk_gb,
          datacenterId: row.comp__datacenter_id,
        } as Compute,
        metadata: {
          hostname: row.comp__hostname,
          ipAddress: row.comp__ip_address,
        },
      };
      treeMap.set(computeNode.id, computeNode);

      // Add to parent datacenter
      const parentNode = treeMap.get(`datacenter-${row.comp__datacenter_id}`);
      if (parentNode && !parentNode.children?.find(c => c.id === computeNode.id)) {
        parentNode.children!.push(computeNode);
      }
    }

    // Extract network device data
    if (row.nd__id && !treeMap.has(`network-device-${row.nd__id}`)) {
      const networkDeviceNode: TreeNode = {
        id: `network-device-${row.nd__id}`,
        name: row.nd__name,
        type: 'network-device',
        data: {
          id: row.nd__id,
          name: row.nd__name,
          deviceType: row.nd__device_type,
          vendor: row.nd__vendor,
          model: row.nd__model,
          managementIp: row.nd__management_ip,
          status: row.nd__status,
          datacenterId: row.nd__datacenter_id,
        } as NetworkDevice,
        metadata: {
          ipAddress: row.nd__management_ip,
        },
      };
      treeMap.set(networkDeviceNode.id, networkDeviceNode);

      // Add to parent datacenter
      const parentNode = treeMap.get(`datacenter-${row.nd__datacenter_id}`);
      if (parentNode && !parentNode.children?.find(c => c.id === networkDeviceNode.id)) {
        parentNode.children!.push(networkDeviceNode);
      }
    }
  });

  return rootNodes;
}

export function findNodeById(tree: TreeNode[], nodeId: string): TreeNode | null {
  for (const node of tree) {
    if (node.id === nodeId) {
      return node;
    }
    if (node.children) {
      const found = findNodeById(node.children, nodeId);
      if (found) return found;
    }
  }
  return null;
}

export function getNodePath(tree: TreeNode[], nodeId: string): TreeNode[] {
  const path: TreeNode[] = [];
  
  function findPath(nodes: TreeNode[], targetId: string, currentPath: TreeNode[]): boolean {
    for (const node of nodes) {
      const newPath = [...currentPath, node];
      
      if (node.id === targetId) {
        path.push(...newPath);
        return true;
      }
      
      if (node.children && findPath(node.children, targetId, newPath)) {
        return true;
      }
    }
    return false;
  }
  
  findPath(tree, nodeId, []);
  return path;
}