// Auto-generated from TypeScript source
// Original: base/StoreProvider.tsx
// Generated: 2025-09-19T03:59:30.865Z

import React, { createContext, useContext, ReactNode } from 'react';
const { RootStore } = require('../RootStore');
const rootStore = require('../RootStore');

exports.StoreContext = createContext<RootStore | null>(null);



exports.StoreProvider: React.FC = ({ 
  children, 
  store = rootStore 
}) => {
  return (
    <StoreContext.Provider value={store}>
      {children}
    </StoreContext.Provider>
  );
};

exports.useStore = (): RootStore => {
  const store = useContext(StoreContext);
  if (!store) {
    throw new Error('useStore must be used within a StoreProvider');
  }
  return store;
};

exports.useOrganizationInfraStore = () => {
  const store = useStore();
  return store.organizationInfraStore;
};