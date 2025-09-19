/**
 * Shared Store Structure - Used by both frontend and backend
 * Each store follows this exact structure
 */

export class Store {
  constructor(entity) {
    this.entity = entity;
    this.query = null;
    this.data = [];
    this.meta = {
      timestamp: null,
      totalCount: 0,
      pageSize: 20,
      pagesLoaded: []
    };
  }

  setQuery(query) {
    this.query = query;
    this.meta.pageSize = query.pageSize || 20;
  }

  setData(data, pageNumbers = [1]) {
    this.data = data;
    this.meta.pagesLoaded = pageNumbers;
    this.meta.timestamp = new Date().toISOString();
  }

  appendData(newData, pageNumber) {
    this.data = [...this.data, ...newData];
    if (!this.meta.pagesLoaded.includes(pageNumber)) {
      this.meta.pagesLoaded.push(pageNumber);
    }
    this.meta.timestamp = new Date().toISOString();
  }

  clear() {
    this.query = null;
    this.data = [];
    this.meta.pagesLoaded = [];
    this.meta.totalCount = 0;
    this.meta.timestamp = null;
  }

  toJSON() {
    return {
      entity: this.entity,
      query: this.query,
      data: this.data,
      meta: this.meta
    };
  }
}

/**
 * Infrastructure Store - Special hierarchical structure
 */
export class InfrastructureStore extends Store {
  constructor() {
    super('infrastructure');
    this.data = {
      partners: [],
      index: {
        environments: {},
        datacenters: {},
        computes: {},
        networkDevices: {}
      }
    };
    this.meta = {
      timestamp: null,
      counts: {
        partners: 0,
        environments: 0,
        clouds: 0,
        regions: 0,
        availabilityZones: 0,
        datacenters: 0,
        computes: 0,
        networkDevices: 0
      },
      pagesLoaded: {}  // e.g., "datacenter:38:computes": [1, 2]
    };
  }

  setData(partners) {
    this.data.partners = partners;
    this.rebuildIndex();
    this.updateCounts();
    this.meta.timestamp = new Date().toISOString();
  }

  rebuildIndex() {
    // Clear index
    this.data.index = {
      environments: {},
      datacenters: {},
      computes: {},
      networkDevices: {}
    };

    // Rebuild from partner data
    this.data.partners.forEach(partner => {
      // Index environments
      partner.environments?.forEach(env => {
        this.data.index.environments[env.id] = {
          ...env,
          partnerId: partner.id
        };
      });

      // Index cloud resources
      partner.clouds?.forEach(cloud => {
        cloud.regions?.forEach(region => {
          region.availabilityZones?.forEach(az => {
            az.datacenters?.forEach(dc => {
              // Index datacenter
              this.data.index.datacenters[dc.id] = {
                ...dc,
                cloudId: cloud.id,
                regionId: region.id,
                azId: az.id
              };

              // Index computes
              dc.computes?.forEach(compute => {
                this.data.index.computes[compute.id] = {
                  ...compute,
                  datacenterId: dc.id
                };
              });

              // Index network devices
              dc.networkDevices?.forEach(device => {
                this.data.index.networkDevices[device.id] = {
                  ...device,
                  datacenterId: dc.id
                };
              });
            });
          });
        });
      });
    });
  }

  updateCounts() {
    this.meta.counts = {
      partners: this.data.partners.length,
      environments: Object.keys(this.data.index.environments).length,
      datacenters: Object.keys(this.data.index.datacenters).length,
      computes: Object.keys(this.data.index.computes).length,
      networkDevices: Object.keys(this.data.index.networkDevices).length,
      clouds: 0,
      regions: 0,
      availabilityZones: 0
    };

    // Count nested entities
    this.data.partners.forEach(partner => {
      this.meta.counts.clouds += partner.clouds?.length || 0;
      partner.clouds?.forEach(cloud => {
        this.meta.counts.regions += cloud.regions?.length || 0;
        cloud.regions?.forEach(region => {
          this.meta.counts.availabilityZones += region.availabilityZones?.length || 0;
        });
      });
    });
  }
}