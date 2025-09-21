// Auto-generated index file for Node.js store
// This file exports all stores and services for use in the backend

const path = require('path');

// Export all stores
module.exports = {
  // Base stores
  ...requireIfExists('./base/StellarStore'),

  // Infrastructure stores
  ...requireIfExists('./infrastructure/OrganizationInfraStore'),

  // Event system
  ...requireIfExists('./events/EventBus'),
  ...requireIfExists('./events/LocalStore'),

  // Services
  ...requireIfExists('./services/QueryService'),
  ...requireIfExists('./services/MutationService'),

  // Helpers
  ...requireIfExists('./helpers')
};

function requireIfExists(modulePath) {
  try {
    return require(modulePath);
  } catch (e) {
    console.log(`Module not found: ${modulePath}`);
    return {};
  }
}
