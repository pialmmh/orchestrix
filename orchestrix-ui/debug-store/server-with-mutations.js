const StoreManager = require('./stores/StoreManager');
const EventBusWithMutations = require('./services/EventBusWithMutations');
const {
  InfrastructureStore,
  CountryStore,
  PartnerStore,
  EnvironmentStore
} = require('./stores/StoreStructure');

// Configuration
const config = {
  port: process.env.WS_PORT || 3013,
  debugMode: process.env.DEBUG !== 'false',
  apiUrl: process.env.API_URL || 'http://localhost:8090'
};

// Initialize store manager
const storeManager = new StoreManager(config.debugMode);

// Create stores
storeManager.createStore('infrastructure', InfrastructureStore);
storeManager.createStore('country', CountryStore);
storeManager.createStore('partner', PartnerStore);
storeManager.createStore('environment', EnvironmentStore);
storeManager.createStore('compute', EnvironmentStore); // For compute operations

// Initialize EventBus with mutations
const eventBus = new EventBusWithMutations(config.port);
eventBus.setStoreManager(storeManager);

// Start server
eventBus.start();

console.log(`
╔════════════════════════════════════════════╗
║   Store Server with Mutations Started      ║
╠════════════════════════════════════════════╣
║  WebSocket Port: ${config.port}                    ║
║  Debug Mode: ${config.debugMode ? 'ON ' : 'OFF'}                        ║
║  API URL: ${config.apiUrl}    ║
║  Debug Path: ./store-debug                 ║
╠════════════════════════════════════════════╣
║  Features:                                  ║
║    ✓ Query with caching                    ║
║    ✓ Mutations (CREATE, UPDATE, DELETE)    ║
║    ✓ Auto-refresh after mutations          ║
║    ✓ Real-time subscriptions               ║
╠════════════════════════════════════════════╣
║  Stores:                                    ║
║    • infrastructure                         ║
║    • country                                ║
║    • partner                                ║
║    • environment                            ║
║    • compute                                ║
╚════════════════════════════════════════════╝
`);

// Handle shutdown
process.on('SIGINT', () => {
  console.log('\nShutting down store server...');
  eventBus.stop();
  process.exit(0);
});

process.on('SIGTERM', () => {
  console.log('\nShutting down store server...');
  eventBus.stop();
  process.exit(0);
});