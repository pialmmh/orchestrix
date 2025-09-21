const axios = require('axios');

class ApiService {
  constructor(baseUrl = 'http://localhost:8090') {
    this.baseUrl = baseUrl;
    this.client = axios.create({
      baseURL: baseUrl,
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json'
      }
    });
  }

  /**
   * Execute a query to fetch data
   */
  async query(queryPayload) {
    try {
      const response = await this.client.post('/api/stellar/query', queryPayload);
      return {
        success: true,
        data: response.data.result || response.data,
        metadata: {
          duration: response.headers['x-response-time'] || 0
        }
      };
    } catch (error) {
      console.error('Query failed:', error);
      return {
        success: false,
        error: error.message,
        data: null
      };
    }
  }

  /**
   * Execute mutations (CREATE, UPDATE, DELETE)
   */
  async mutate(entity, operation, data) {
    try {
      let response;

      switch (operation) {
        case 'CREATE':
          response = await this.create(entity, data);
          break;
        case 'UPDATE':
          response = await this.update(entity, data.id, data);
          break;
        case 'DELETE':
          response = await this.delete(entity, data.id);
          break;
        case 'DELETE_COMPUTE':
          // Special case for compute deletion
          response = await this.deleteCompute(data.computeId);
          break;
        default:
          throw new Error(`Unknown operation: ${operation}`);
      }

      return response;
    } catch (error) {
      console.error(`Mutation ${operation} failed:`, error);
      return {
        success: false,
        error: error.message
      };
    }
  }

  /**
   * CREATE operation
   */
  async create(entity, data) {
    const response = await this.client.post(`/stellar/${entity}`, data);
    return {
      success: response.data.success,
      data: response.data.data,
      operation: 'CREATE'
    };
  }

  /**
   * UPDATE operation
   */
  async update(entity, id, data) {
    const response = await this.client.put(`/stellar/${entity}/${id}`, data);
    return {
      success: response.data.success,
      data: response.data.data,
      operation: 'UPDATE'
    };
  }

  /**
   * DELETE operation
   */
  async delete(entity, id) {
    const response = await this.client.delete(`/stellar/${entity}/${id}`);
    return {
      success: response.data.success,
      data: { id },
      operation: 'DELETE'
    };
  }

  /**
   * Special: Delete a compute resource
   */
  async deleteCompute(computeId) {
    const response = await this.client.delete(`/stellar/compute/${computeId}`);
    return {
      success: response.data.success,
      data: { id: computeId },
      operation: 'DELETE_COMPUTE'
    };
  }

  /**
   * Fetch infrastructure data
   */
  async fetchInfrastructure(query) {
    // Build the complex infrastructure query
    const infrastructureQuery = {
      kind: 'partner',
      criteria: query.criteria || { name: 'telcobright' },
      page: query.page || { limit: 10, offset: 0 },
      include: [
        {
          kind: 'environment',
          page: { limit: 20, offset: 0 },
          include: [
            {
              kind: 'environmentassociation',
              page: { limit: 1000, offset: 0 }
            }
          ]
        },
        {
          kind: 'cloud',
          page: { limit: 20, offset: 0 },
          include: [
            {
              kind: 'region',
              page: { limit: 50, offset: 0 },
              include: [
                {
                  kind: 'availabilityzone',
                  page: { limit: 100, offset: 0 },
                  include: [
                    {
                      kind: 'datacenter',
                      page: { limit: 200, offset: 0 },
                      include: [
                        {
                          kind: 'compute',
                          page: { limit: 500, offset: 0 }
                        },
                        {
                          kind: 'networkdevice',
                          page: { limit: 200, offset: 0 }
                        }
                      ]
                    }
                  ]
                }
              ]
            }
          ]
        }
      ]
    };

    const result = await this.query(infrastructureQuery);

    if (result.success) {
      // Transform to infrastructure store format
      return {
        partners: result.data || []
      };
    }

    return null;
  }
}

module.exports = ApiService;