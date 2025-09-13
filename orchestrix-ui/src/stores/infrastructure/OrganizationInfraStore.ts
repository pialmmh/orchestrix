import { makeObservable, observable, action, computed, runInAction } from 'mobx';
import { StellarStore } from '../base/StellarStore';
import { TreeNode, Partner, Cloud, Datacenter, Compute, NetworkDevice } from '../../models/entities/TreeNode';
import { QueryNode } from '../../models/stellar/QueryNode';
import { EntityModificationRequest } from '../../models/stellar/MutationRequest';
import { transformStellarToTree, findNodeById, getNodePath } from '../../utils/stellar/treeBuilder';

export class OrganizationInfraStore extends StellarStore {
  treeData: TreeNode[] = [];
  selectedNode: TreeNode | null = null;
  selectedNodePath: TreeNode[] = [];
  expandedNodeIds: string[] = [];
  partners: Partner[] = [];
  environments: any[] = [];
  
  // List view data for right pane
  selectedNodeChildren: any[] = [];
  selectedNodeChildrenLoading: boolean = false;
  selectedNodeChildrenError: string | null = null;
  selectedNodeChildrenPage: number = 1;
  selectedNodeChildrenPageSize: number = 10;
  selectedNodeChildrenTotal: number = 0;

  constructor() {
    super();
    makeObservable(this, {
      treeData: observable,
      selectedNode: observable,
      selectedNodePath: observable,
      expandedNodeIds: observable,
      partners: observable,
      environments: observable,
      selectedNodeChildren: observable,
      selectedNodeChildrenLoading: observable,
      selectedNodeChildrenError: observable,
      selectedNodeChildrenPage: observable,
      selectedNodeChildrenPageSize: observable,
      selectedNodeChildrenTotal: observable,
      setTreeData: action,
      setSelectedNode: action,
      toggleNodeExpanded: action,
      expandNode: action,
      collapseNode: action,
      loadInfrastructureTree: action,
      loadSelectedNodeChildren: action,
      setSelectedNodeChildrenPage: action,
      createCompute: action,
      updateCompute: action,
      deleteCompute: action,
      createNetworkDevice: action,
      updateNetworkDevice: action,
      deleteNetworkDevice: action,
      isNodeExpanded: computed,
    });

    // Initialize with some default expanded nodes
    this.expandedNodeIds = ['org-root', 'environments'];
  }

  get isNodeExpanded() {
    return (nodeId: string) => this.expandedNodeIds.includes(nodeId);
  }

  setTreeData(data: TreeNode[]) {
    this.treeData = data;
  }

  setSelectedNode(node: TreeNode | null) {
    this.selectedNode = node;
    if (node) {
      this.selectedNodePath = getNodePath(this.treeData, node.id);
      // Automatically load children when a node is selected
      this.loadSelectedNodeChildren();
    } else {
      this.selectedNodePath = [];
      this.selectedNodeChildren = [];
    }
  }
  
  async loadSelectedNodeChildren() {
    if (!this.selectedNode) {
      this.selectedNodeChildren = [];
      return;
    }

    const { type, data } = this.selectedNode;
    let query: QueryNode | null = null;

    // Build query based on selected node type
    switch (type) {
      case 'partner':
        // Load all clouds and their datacenters/computes under this partner
        query = {
          kind: 'cloud',
          criteria: { partnerId: data.id },
          page: { 
            limit: this.selectedNodeChildrenPageSize, 
            offset: (this.selectedNodeChildrenPage - 1) * this.selectedNodeChildrenPageSize 
          }
        };
        break;
        
      case 'cloud':
        // Load all datacenters under this cloud
        query = {
          kind: 'datacenter',
          criteria: { cloudId: data.id },
          page: { 
            limit: this.selectedNodeChildrenPageSize, 
            offset: (this.selectedNodeChildrenPage - 1) * this.selectedNodeChildrenPageSize 
          }
        };
        break;
        
      case 'datacenter':
        // Load all computes under this datacenter
        query = {
          kind: 'compute',
          criteria: { datacenterId: data.id },
          page: { 
            limit: this.selectedNodeChildrenPageSize, 
            offset: (this.selectedNodeChildrenPage - 1) * this.selectedNodeChildrenPageSize 
          }
        };
        break;
        
      case 'compute':
        // For compute node, just show its details, no children
        this.selectedNodeChildren = [data];
        return;
        
      case 'network-device':
        // For network device, just show its details, no children
        this.selectedNodeChildren = [data];
        return;
        
      default:
        this.selectedNodeChildren = [];
        return;
    }

    if (!query) {
      this.selectedNodeChildren = [];
      return;
    }

    runInAction(() => {
      this.selectedNodeChildrenLoading = true;
      this.selectedNodeChildrenError = null;
    });

    try {
      const result = await this.executeQuery(query);
      
      runInAction(() => {
        this.selectedNodeChildren = result || [];
        this.selectedNodeChildrenTotal = result?.length || 0; // In real app, get from backend
        this.selectedNodeChildrenLoading = false;
      });
    } catch (error: any) {
      runInAction(() => {
        this.selectedNodeChildrenError = error.message || 'Failed to load items';
        this.selectedNodeChildrenLoading = false;
        this.selectedNodeChildren = [];
      });
    }
  }
  
  setSelectedNodeChildrenPage(page: number) {
    this.selectedNodeChildrenPage = page;
    this.loadSelectedNodeChildren();
  }

  toggleNodeExpanded(nodeId: string) {
    const index = this.expandedNodeIds.indexOf(nodeId);
    if (index >= 0) {
      this.expandedNodeIds.splice(index, 1);
    } else {
      this.expandedNodeIds.push(nodeId);
    }
  }

  expandNode(nodeId: string) {
    if (!this.expandedNodeIds.includes(nodeId)) {
      this.expandedNodeIds.push(nodeId);
    }
  }

  collapseNode(nodeId: string) {
    const index = this.expandedNodeIds.indexOf(nodeId);
    if (index >= 0) {
      this.expandedNodeIds.splice(index, 1);
    }
  }

  async loadInfrastructureTree(partner: string = 'self') {
    // Build criteria based on partner parameter
    const criteria: any = {};

    if (partner === 'self') {
      // Load organization's own infrastructure - temporarily load all for testing
      // criteria.roles = ['self'];
    } else if (partner === 'all') {
      // Load all partners' infrastructure (no criteria filter)
    } else {
      // Load specific partner's infrastructure
      criteria.name = partner;
    }

    const query: QueryNode = {
      kind: 'partner',
      ...(Object.keys(criteria).length > 0 && { criteria }),
      include: [
        {
          kind: 'cloud',
          include: [
            {
              kind: 'datacenter',
              include: [
                {
                  kind: 'compute',
                },
                {
                  kind: 'networkdevice',
                },
              ],
            },
          ],
        },
      ],
    };

    const data = await this.executeQuery(query);
    
    if (data) {
      runInAction(() => {
        this.treeData = transformStellarToTree(data);
        // Auto-expand first few levels
        this.treeData.forEach(partner => {
          this.expandNode(partner.id);
          partner.children?.forEach(cloud => {
            this.expandNode(cloud.id);
          });
        });
      });
    }
  }

  async loadPartners() {
    const query: QueryNode = {
      kind: 'partner',
    };

    const data = await this.executeQuery<Partner>(query);
    
    if (data) {
      runInAction(() => {
        this.partners = data.map(row => ({
          id: (row as any).p__id || (row as any).id,
          name: (row as any).p__name || (row as any).name,
          displayName: (row as any).p__display_name || (row as any).displayName,
          type: (row as any).p__type || (row as any).type,
          roles: JSON.parse((row as any).p__roles || (row as any).roles || '[]'),
          status: (row as any).p__status || (row as any).status,
        }));
      });
    }
  }

  // CRUD operations for Compute
  async createCompute(datacenterId: number, computeData: Partial<Compute>) {
    const request: EntityModificationRequest = {
      entityName: 'compute',
      operation: 'INSERT',
      data: {
        ...computeData,
        datacenterId,
      },
    };

    const response = await this.executeMutation(request);
    
    if (response.success) {
      await this.loadInfrastructureTree('self');
    }
    
    return response;
  }

  async updateCompute(computeId: number, updates: Partial<Compute>) {
    const request: EntityModificationRequest = {
      entityName: 'compute',
      operation: 'UPDATE',
      id: computeId,
      data: updates,
    };

    const response = await this.executeMutation(request);
    
    if (response.success) {
      await this.loadInfrastructureTree('self');
    }
    
    return response;
  }

  async deleteCompute(computeId: number) {
    const request: EntityModificationRequest = {
      entityName: 'compute',
      operation: 'DELETE',
      id: computeId,
    };

    const response = await this.executeMutation(request);
    
    if (response.success) {
      await this.loadInfrastructureTree('self');
      if (this.selectedNode?.data?.id === computeId) {
        this.setSelectedNode(null);
      }
    }
    
    return response;
  }

  // CRUD operations for NetworkDevice
  async createNetworkDevice(datacenterId: number, deviceData: Partial<NetworkDevice>) {
    const request: EntityModificationRequest = {
      entityName: 'networkdevice',
      operation: 'INSERT',
      data: {
        ...deviceData,
        datacenterId,
      },
    };

    const response = await this.executeMutation(request);
    
    if (response.success) {
      await this.loadInfrastructureTree('self');
    }
    
    return response;
  }

  async updateNetworkDevice(deviceId: number, updates: Partial<NetworkDevice>) {
    const request: EntityModificationRequest = {
      entityName: 'networkdevice',
      operation: 'UPDATE',
      id: deviceId,
      data: updates,
    };

    const response = await this.executeMutation(request);
    
    if (response.success) {
      await this.loadInfrastructureTree('self');
    }
    
    return response;
  }

  async deleteNetworkDevice(deviceId: number) {
    const request: EntityModificationRequest = {
      entityName: 'networkdevice',
      operation: 'DELETE',
      id: deviceId,
    };

    const response = await this.executeMutation(request);
    
    if (response.success) {
      await this.loadInfrastructureTree('self');
      if (this.selectedNode?.data?.id === deviceId) {
        this.setSelectedNode(null);
      }
    }
    
    return response;
  }

  // Utility methods
  findNode(nodeId: string): TreeNode | null {
    return findNodeById(this.treeData, nodeId);
  }

  getParentNode(node: TreeNode): TreeNode | null {
    const path = getNodePath(this.treeData, node.id);
    return path.length > 1 ? path[path.length - 2] : null;
  }
}