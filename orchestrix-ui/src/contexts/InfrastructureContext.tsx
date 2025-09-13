import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { Cloud, TreeNode } from '../types/CloudHierarchy';
import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8090/api';

interface InfrastructureContextType {
  clouds: Cloud[];
  loading: boolean;
  error: string | null;
  selectedNode: TreeNode | undefined;
  setSelectedNode: (node: TreeNode | undefined) => void;
  refreshClouds: () => Promise<void>;
}

const InfrastructureContext = createContext<InfrastructureContextType | undefined>(undefined);

export const useInfrastructure = () => {
  const context = useContext(InfrastructureContext);
  if (!context) {
    throw new Error('useInfrastructure must be used within an InfrastructureProvider');
  }
  return context;
};

interface InfrastructureProviderProps {
  children: ReactNode;
}

export const InfrastructureProvider: React.FC<InfrastructureProviderProps> = ({ children }) => {
  const [clouds, setClouds] = useState<Cloud[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedNode, setSelectedNode] = useState<TreeNode | undefined>();

  const fetchClouds = async () => {
    try {
      setLoading(true);
      const response = await axios.get(`${API_BASE_URL}/clouds`);
      setClouds(response.data);
      setError(null);
    } catch (err) {
      console.error('Error fetching clouds:', err);
      setError('Failed to load cloud infrastructure data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchClouds();
  }, []);

  const value: InfrastructureContextType = {
    clouds,
    loading,
    error,
    selectedNode,
    setSelectedNode,
    refreshClouds: fetchClouds,
  };

  return (
    <InfrastructureContext.Provider value={value}>
      {children}
    </InfrastructureContext.Provider>
  );
};