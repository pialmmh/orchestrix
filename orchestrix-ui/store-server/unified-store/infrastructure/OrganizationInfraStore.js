// Auto-generated from TypeScript source
// Original: infrastructure/OrganizationInfraStore.ts
// Generated: 2025-09-19T03:59:30.869Z

const { makeObservable, observable, action, computed, runInAction } = require('mobx');
const { StellarStore } = require('../base/StellarStore');
const { TreeNode, Partner, Cloud, Datacenter, Compute, NetworkDevice } = require('../../models/entities/TreeNode');
const { QueryNode } = require('../../models/stellar/QueryNode');
const { EntityModificationRequest } = require('../../models/stellar/MutationRequest');
const { transformStellarToTree, findNodeById, getNodePath } = require('../../utils/stellar/treeBuilder');

export class OrganizationInfraStore extends StellarStore {
  treeData: TreeNode[] = [];
  filteredTreeData: TreeNode[] = [];
  selectedNode: TreeNode | null = null;
  selectedNodePath: TreeNode[] = [];
  expandedNodeIds: string[] = [];
  partners: Partner[] = [];
  environments: any[] = [];
  selectedEnvironmentFilter: string | null = null;
  
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
      treeData,
      filteredTreeData,
      selectedNode,
      selectedNodePath,
      expandedNodeIds,
      partners,
      environments,
      selectedEnvironmentFilter,
      selectedNodeChildren,
      selectedNodeChildrenLoading,
      selectedNodeChildrenError,
      selectedNodeChildrenPage,
      selectedNodeChildrenPageSize,
      selectedNodeChildrenTotal,
      setTreeData,
      setSelectedNode,
      toggleNodeExpanded,
      expandNode,
      collapseNode,
      loadInfrastructureTree,
      setEnvironmentFilter,
      applyEnvironmentFilter,
      loadSelectedNodeChildren,
      setSelectedNodeChildrenPage,
      createCompute,
      updateCompute,
      deleteCompute,
      createNetworkDevice,
      updateNetworkDevice,
      deleteNetworkDevice,
      isNodeExpanded,
      displayTreeData,
    });

    // Start with fully collapsed tree - no expanded nodes by default
    this.expandedNodeIds = [];
  }

  get isNodeExpanded() {
    return (nodeId) => this.expandedNodeIds.includes(nodeId);
  }

  get displayTreeData() {
    return this.selectedEnvironmentFilter ? this.filteredTreeData : this.treeData;
  }

  setTreeData(data) {
    this.treeData = data;
    this.applyEnvironmentFilter();
  }

  setEnvironmentFilter(environmentType) {
    this.selectedEnvironmentFilter = environmentType;
    this.applyEnvironmentFilter();
  }

  applyEnvironmentFilter() {
    if (!this.selectedEnvironmentFilter) {
      this.filteredTreeData = this.treeData;
      return;
    }

    // Deep filter tree to only show datacenters with matching environment
    const filterTree = (nodes): TreeNode[] => {
      return nodes.map(node => {
        if (node.type === 'datacenter') {
          // Check if this datacenter matches the filter
          const envType = node.metadata?.environment;
          if (!envType || envType !== this.selectedEnvironmentFilter) {
            return null; // Will be filtered out
          }
          return node;
        }

        // For non-datacenter nodes, recursively filter children
        if (node.children && node.children.length > 0) {
          const filteredChildren = filterTree(node.children).filter(Boolean);
          if (filteredChildren.length > 0) {
            return { ...node, children: filteredChildren };
          }
          return null; // Node has no matching children
        }

        return node;
      }).filter(Boolean) as TreeNode[];
    };

    this.filteredTreeData = filterTree(this.treeData);
  }

  setSelectedNode(node) {
    this.selectedNode = node;
    if (node) {
      this.selectedNodePath = getNodePath(this.displayTreeData, node.id);
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
    } catch (error) {
      runInAction(() => {
        this.selectedNodeChildrenError = error.message || 'Failed to load items';
        this.selectedNodeChildrenLoading = false;
        this.selectedNodeChildren = [];
      });
    }
  }
  
  setSelectedNodeChildrenPage(page) {
    this.selectedNodeChildrenPage = page;
    this.loadSelectedNodeChildren();
  }

  toggleNodeExpanded(nodeId) {
    const index = this.expandedNodeIds.indexOf(nodeId);
    if (index >= 0) {
      this.expandedNodeIds.splice(index, 1);
    } else {
      this.expandedNodeIds.push(nodeId);
    }
  }

  expandNode(nodeId) {
    if (!this.expandedNodeIds.includes(nodeId)) {
      this.expandedNodeIds.push(nodeId);
    }
  }

  collapseNode(nodeId) {
    const index = this.expandedNodeIds.indexOf(nodeId);
    if (index >= 0) {
      this.expandedNodeIds.splice(index, 1);
    }
  }

  async loadComputeEnvironments() {
    // Load environment associations for computes
    const query: QueryNode = {
      kind: 'environmentassociation',
      criteria: { resource_type: 'compute' },
      page: { limit, offset: 0 },
    };

    try {
      const associations = await this.executeQuery(query);
      return associations || [];
    } catch (error) {
      console.error('Failed to load compute environments:', error);
      return [];
    }
  }

  async loadInfrastructureTree(partner: string = 'self') {
    console.log('🚀 loadInfrastructureTree called with partner:', partner);

    // Build criteria based on partner parameter
    const criteria: any = {};

    if (partner === 'self') {
      // Load organization's own infrastructure - filter by name
      criteria.name = 'telcobright';
    } else if (partner === 'all') {
      // Load all partners' infrastructure (no criteria filter)
    } else {
      // Load specific partner's infrastructure
      criteria.name = partner;
    }

    console.log('🔍 Using criteria:', criteria);

    // Single unified query that loads cloud hierarchy with environment associations
    const unifiedQuery: QueryNode = {
      kind: 'partner',
      ...(Object.keys(criteria).length > 0 && { criteria }),
      page: { limit, offset: 0 },
      include: [
        {
          kind: 'environment',
          page: { limit, offset: 0 },
          include: [
            {
              kind: 'environmentassociation',
              page: { limit, offset: 0 },
            },
          ],
        },
        {
          kind: 'cloud',
          page: { limit, offset: 0 },
          include: [
            {
              kind: 'region',
              page: { limit, offset: 0 },
              include: [
                {
                  kind: 'availabilityzone',
                  page: { limit, offset: 0 },
                  include: [
                    {
                      kind: 'datacenter',
                      page: { limit, offset: 0 },
                      include: [
                        {
                          kind: 'compute',
                          page: { limit, offset: 0 },
                        },
                        {
                          kind: 'networkdevice',
                          page: { limit, offset: 0 },
                        },
                      ],
                    },
                  ],
                },
              ],
            },
          ],
        },
      ],
    };

    try {
      console.log('📡 Loading unified infrastructure hierarchy...');
      const [data, computeAssociations] = await Promise.all([
        this.executeQuery(unifiedQuery),
        this.loadComputeEnvironments()
      ]);

      console.log('📊 Unified data:', JSON.stringify(data, null, 2));
      console.log('📊 Compute associations:', computeAssociations);

      if (data) {
        runInAction(() => {
          // Store environments separately for reference
          if (data[0]?.environments) {
            this.environments = data[0].environments;
          }

          // Add compute associations to the data
          if (computeAssociations && data[0]?.clouds) {
            // Attach associations to computes
            const attachAssociations = (nodes) => {
              nodes.forEach(node => {
                if (node.computes) {
                  node.computes.forEach((compute) => {
                    compute.environmentassociations = computeAssociations.filter(
                      (ea) => ea.resource_type === 'compute' && ea.resource_id === compute.id
                    );
                  });
                }
                if (node.datacenters) attachAssociations(node.datacenters);
                if (node.regions) attachAssociations(node.regions);
                if (node.availabilityzones) attachAssociations(node.availabilityzones);
              });
            };
            attachAssociations(data[0].clouds);
          }

          // Transform to tree with environment metadata added
          this.treeData = transformStellarToTree(data, this.environments);
          this.applyEnvironmentFilter();

          console.log('🌲 Unified tree:', JSON.stringify(this.treeData, null, 2));

          // Auto-expand first few levels
          this.treeData.forEach(partner => {
            this.expandNode(partner.id);
            partner.children?.forEach(child => {
              this.expandNode(child.id);
              // Expand one more level for regions
              if (child.type === 'cloud') {
                child.children?.forEach(region => {
                  this.expandNode(region.id);
                });
              }
            });
          });
        });
      } else {
        console.warn('⚠️ No data returned from Stellar API');
        runInAction(() => {
          this.treeData = [];
        });
      }
    } catch (error) {
      console.error('❌ Error in loadInfrastructureTree:', error);
      runInAction(() => {
        this.treeData = [];
      });
      throw error;
    }
  }

  async loadPartners() {
    const query: QueryNode = {
      kind: 'partner',
    };

    const data = await this.executeQuery(query);
    
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
  async createCompute(datacenterId, computeData) {
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

  async updateCompute(computeId, updates) {
    const request: EntityModificationRequest = {
      entityName: 'compute',
      operation: 'UPDATE',
      id,
      data,
    };

    const response = await this.executeMutation(request);
    
    if (response.success) {
      await this.loadInfrastructureTree('self');
    }
    
    return response;
  }

  async deleteCompute(computeId) {
    const request: EntityModificationRequest = {
      entityName: 'compute',
      operation: 'DELETE',
      id,
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
  async createNetworkDevice(datacenterId, deviceData) {
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

  async updateNetworkDevice(deviceId, updates) {
    const request: EntityModificationRequest = {
      entityName: 'networkdevice',
      operation: 'UPDATE',
      id,
      data,
    };

    const response = await this.executeMutation(request);
    
    if (response.success) {
      await this.loadInfrastructureTree('self');
    }
    
    return response;
  }

  async deleteNetworkDevice(deviceId) {
    const request: EntityModificationRequest = {
      entityName: 'networkdevice',
      operation: 'DELETE',
      id,
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
  findNode(nodeId): TreeNode | null {
    return findNodeById(this.displayTreeData, nodeId);
  }

  getParentNode(node): TreeNode | null {
    const path = getNodePath(this.displayTreeData, node.id);
    return path.length > 1 ? path[path.length - 2] : null;
  }
}