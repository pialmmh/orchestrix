import { useContext } from 'react';
import { StoreContext } from './StoreProvider';
import { RootStore } from '../RootStore';

export const useStores = (): RootStore => {
  const context = useContext(StoreContext);
  if (!context) {
    throw new Error('useStores must be used within a StoreProvider');
  }
  return context;
};