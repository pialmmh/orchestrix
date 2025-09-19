const StoreManager = require('./stores/StoreManager');
const EventBus = require('./services/EventBus');
const {
  InfrastructureStore,
  CountryStore,
  PartnerStore,
  EnvironmentStore
} = require('./stores/StoreStructure');

// Configuration
const config = {
  port: process.env.WS_PORT || 8081,
  debugMode: process.env.DEBUG !== 'false'
};

// Initialize store manager
const storeManager = new StoreManager(config.debugMode);

// Create stores
storeManager.createStore('infrastructure', InfrastructureStore);
storeManager.createStore('country', CountryStore);
storeManager.createStore('partner', PartnerStore);
storeManager.createStore('environment', EnvironmentStore);

// Initialize EventBus
const eventBus = new EventBus(config.port);
eventBus.setStoreManager(storeManager);

// Start server
eventBus.start();

console.log(`
╔════════════════════════════════════════╗
║     Clean Store Server Started         ║
╠════════════════════════════════════════╣
║  WebSocket Port: ${config.port}                 ║
║  Debug Mode: ${config.debugMode ? 'ON ' : 'OFF'}                     ║
║  Debug Path: ./store-debug             ║
╠════════════════════════════════════════╣
║  Stores:                               ║
║    • infrastructure                    ║
║    • country                           ║
║    • partner                           ║
║    • environment                       ║
╚════════════════════════════════════════╝
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