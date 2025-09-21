// Auto-generated from TypeScript source
// Original: base/useStores.ts
// Generated: 2025-09-19T10:56:20.504Z

const { useContext } = require('react');
const { StoreContext } = require('./StoreProvider');
const { RootStore } = require('../RootStore');

exports.useStores = (): RootStore => {
  const context = useContext(StoreContext);
  if (!context) {
    throw new Error('useStores must be used within a StoreProvider');
  }
  return context;
};