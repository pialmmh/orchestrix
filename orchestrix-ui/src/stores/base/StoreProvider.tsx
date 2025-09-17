import React, { createContext, useContext, ReactNode } from 'react';
import { RootStore } from '../RootStore';
import rootStore from '../RootStore';

export const StoreContext = createContext<RootStore | null>(null);

interface StoreProviderProps {
  children: ReactNode;
  store?: RootStore;
}

export const StoreProvider: React.FC<StoreProviderProps> = ({ 
  children, 
  store = rootStore 
}) => {
  return (
    <StoreContext.Provider value={store}>
      {children}
    </StoreContext.Provider>
  );
};

export const useStore = (): RootStore => {
  const store = useContext(StoreContext);
  if (!store) {
    throw new Error('useStore must be used within a StoreProvider');
  }
  return store;
};

export const useOrganizationInfraStore = () => {
  const store = useStore();
  return store.organizationInfraStore;
};