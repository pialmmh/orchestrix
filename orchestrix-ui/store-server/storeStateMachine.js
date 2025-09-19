const { createMachine, createActor } = require('xstate');
const { makeObservable, observable, action, toJS } = require('mobx');
const HistoryLogger = require('./historyLogger');

// Example MobX Store class
class BaseStore {
  constructor() {
    this.data = [];
    this.loading = false;
    this.error = null;
    
    makeObservable(this, {
      data: observable,
      loading: observable,
      error: observable,
      setData: action,
      setLoading: action,
      setError: action,
      reset: action,
    });
  }

  setData(data) {
    this.data = data;
    this.error = null;
  }

  setLoading(loading) {
    this.loading = loading;
  }

  setError(error) {
    this.error = error;
    this.loading = false;
  }

  reset() {
    this.data = [];
    this.loading = false;
    this.error = null;
  }

  // Get plain JS snapshot for history
  getSnapshot() {
    return {
      data: toJS(this.data),
      loading: this.loading,
      error: this.error,
    };
  }

  // Restore from snapshot
  restoreSnapshot(snapshot) {
    this.data = snapshot.data || [];
    this.loading = snapshot.loading || false;
    this.error = snapshot.error || null;
  }
}

// Create store state machine that embeds MobX store
const createStoreMachine = (storeName, store = null) => {
  return createMachine({
    id: `${storeName}Machine`,
    initial: 'idle',
    context: {
      store: store || new BaseStore(),
      operationCount: 0,
      lastOperation: null,
      sessionId: null,
    },
    states: {
      idle: {
        on: {
          QUERY: {
            target: 'querying',
            actions: ['setLoading', 'logEvent'],
          },
          MUTATION: {
            target: 'mutating',
            actions: ['setLoading', 'logEvent'],
          },
          RESTORE: {
            target: 'restoring',
            actions: ['logEvent'],
          },
        },
      },
      querying: {
        invoke: {
          id: 'queryService',
          src: 'executeQuery',
          onDone: {
            target: 'idle',
            actions: ['handleQuerySuccess', 'logEvent'],
          },
          onError: {
            target: 'idle',
            actions: ['handleError', 'logEvent'],
          },
        },
      },
      mutating: {
        invoke: {
          id: 'mutationService',
          src: 'executeMutation',
          onDone: {
            target: 'idle',
            actions: ['handleMutationSuccess', 'logEvent'],
          },
          onError: {
            target: 'idle',
            actions: ['handleError', 'logEvent'],
          },
        },
      },
      restoring: {
        invoke: {
          id: 'restoreService',
          src: 'restoreFromHistory',
          onDone: {
            target: 'idle',
            actions: ['handleRestoreSuccess', 'logEvent'],
          },
          onError: {
            target: 'idle',
            actions: ['handleError', 'logEvent'],
          },
        },
      },
    },
  }, {
    actions: {
      setLoading: ({ context }) => {
        context.store.setLoading(true);
        context.operationCount++;
      },
      handleQuerySuccess: ({ context, event }) => {
        context.store.setData(event.data.result);
        context.store.setLoading(false);
        context.lastOperation = {
          type: 'query',
          timestamp: Date.now(),
          success: true,
        };
      },
      handleMutationSuccess: ({ context, event }) => {
        // Update store based on mutation result
        if (event.data.operation === 'INSERT') {
          const currentData = toJS(context.store.data);
          context.store.setData([...currentData, event.data.result]);
        } else if (event.data.operation === 'UPDATE') {
          const currentData = toJS(context.store.data);
          const index = currentData.findIndex(item => item.id === event.data.result.id);
          if (index !== -1) {
            currentData[index] = event.data.result;
            context.store.setData(currentData);
          }
        } else if (event.data.operation === 'DELETE') {
          const currentData = toJS(context.store.data);
          context.store.setData(currentData.filter(item => item.id !== event.data.id));
        }
        context.store.setLoading(false);
        context.lastOperation = {
          type: 'mutation',
          operation: event.data.operation,
          timestamp: Date.now(),
          success: true,
        };
      },
      handleError: ({ context, event }) => {
        context.store.setError(event.data?.message || 'Operation failed');
        context.lastOperation = {
          type: context.lastOperation?.type || 'unknown',
          timestamp: Date.now(),
          success: false,
          error: event.data,
        };
      },
      handleRestoreSuccess: ({ context, event }) => {
        context.store.restoreSnapshot(event.data.snapshot);
        context.lastOperation = {
          type: 'restore',
          timestamp: Date.now(),
          success: true,
          restoredTo: event.data.timestamp,
        };
      },
      logEvent: ({ context, event }) => {
        // Log will be handled by the machine service
        // This is just a placeholder for the action
      },
    },
    actors: {
      executeQuery: async ({ context, event }) => {
        // This will be overridden by the actual implementation
        throw new Error('Query service not implemented');
      },
      executeMutation: async ({ context, event }) => {
        // This will be overridden by the actual implementation
        throw new Error('Mutation service not implemented');
      },
      restoreFromHistory: async ({ context, event }) => {
        // This will be overridden by the actual implementation
        throw new Error('Restore service not implemented');
      },
    },
  });
};

// Machine service that manages state machines with history
class StoreMachineService {
  constructor(config = {}) {
    this.machines = new Map();
    this.historyLogger = new HistoryLogger({
      logDir: config.logDir || './logs',
      maxFileSize: config.maxFileSize || 10 * 1024 * 1024,
      maxFiles: config.maxFiles || 30,
    });
  }

  createMachine(sessionId, storeName, store = null) {
    const machine = createStoreMachine(storeName, store);

    // Create actor with custom services
    const service = createActor(machine.provide({
      actors: {
        executeQuery: ({ input }) => {
          // Implement actual query logic here
          const axios = require('axios');
          const STELLAR_API_URL = process.env.STELLAR_API_URL || 'http://localhost:8090/api';
          const url = `${STELLAR_API_URL}/stellar/query`;

          console.log('[XState] Executing query to:', url);
          console.log('[XState] Query payload:', JSON.stringify(input.query));

          return axios.post(url, input.query).then(response => {
            console.log('[XState] Query success, response:', response.data);
            return { result: response.data };
          }).catch(error => {
            console.error('[XState] Query error:', error.message);
            console.error('[XState] Error response:', error.response?.data);
            console.error('[XState] Error status:', error.response?.status);
            throw new Error(error.response?.data?.error || error.message || 'Query failed');
          });
        },
        executeMutation: async ({ context, event }) => {
          // Implement actual mutation logic here
          const axios = require('axios');
          const STELLAR_API_URL = process.env.STELLAR_API_URL || 'http://localhost:8090/api';
          try {
            const response = await axios.post(`${STELLAR_API_URL}/stellar/modify`, event.request);
            return {
              result: response.data,
              operation: event.request.operation,
              id: event.request.id,
            };
          } catch (error) {
            console.error('[XState] Mutation error:', error.message);
            throw error;
          }
        },
        restoreFromHistory: async ({ context, event }) => {
          // Restore from a specific point in history
          const events = await this.historyLogger.searchEvents({
            sessionId: context.sessionId,
            endTime: event.timestamp,
            limit: 1,
          });

          if (events.length > 0 && events[0].snapshot) {
            return {
              snapshot: events[0].snapshot,
              timestamp: events[0].timestamp,
            };
          }

          throw new Error('No snapshot found for the specified timestamp');
        },
      },
      actions: {
        logEvent: ({ context, event, self }) => {
          // Log every transition and event
          const logEntry = {
            type: event.type,
            sessionId: context.sessionId || sessionId,
            operation: event.type,
            stateSnapshot: context.store.getSnapshot(),
            machineState: self.getSnapshot().value,
            context: {
              operationCount: context.operationCount,
              lastOperation: context.lastOperation,
            },
            event: event,
          };
          
          this.historyLogger.logEvent(logEntry);
        },
      },
    }));

    // Start the service
    service.start();
    
    // Store the service
    this.machines.set(sessionId, {
      service,
      storeName,
      createdAt: Date.now(),
    });

    // Log machine creation
    this.historyLogger.logEvent({
      type: 'machine',
      operation: 'CREATE',
      sessionId,
      storeName,
      timestamp: Date.now(),
    });

    return service;
  }

  getMachine(sessionId) {
    const machine = this.machines.get(sessionId);
    return machine?.service;
  }

  sendEvent(sessionId, event) {
    const service = this.getMachine(sessionId);
    if (!service) {
      throw new Error(`No machine found for session ${sessionId}`);
    }
    
    service.send(event);
    return service.getSnapshot().context.store.getSnapshot();
  }

  async replayEvents(sessionId, fromTimestamp, toTimestamp) {
    // Get historical events
    const events = await this.historyLogger.searchEvents({
      sessionId,
      startTime: fromTimestamp,
      endTime: toTimestamp,
    });

    // Create a new machine for replay
    const replayService = this.createMachine(`${sessionId}-replay`, 'replay');
    
    // Replay events
    for (const logEntry of events) {
      if (logEntry.event && logEntry.event.type) {
        replayService.send(logEntry.event);
      }
    }

    return replayService.getSnapshot().context.store.getSnapshot();
  }

  async getHistory(sessionId, limit = 100) {
    return this.historyLogger.searchEvents({
      sessionId,
      limit,
    });
  }

  async getStats() {
    const stats = {
      activeMachines: this.machines.size,
      machines: [],
      historyStats: this.historyLogger.getStats(),
    };

    for (const [sessionId, machine] of this.machines) {
      stats.machines.push({
        sessionId,
        storeName: machine.storeName,
        createdAt: machine.createdAt,
        state: machine.service.getSnapshot().value,
        operationCount: machine.service.getSnapshot().context.operationCount,
      });
    }

    return stats;
  }

  cleanup() {
    // Stop all machines
    for (const [sessionId, machine] of this.machines) {
      machine.service.stop();
    }
    this.machines.clear();
    
    // Close history logger
    this.historyLogger.close();
  }
}

module.exports = {
  BaseStore,
  createStoreMachine,
  StoreMachineService,
};
