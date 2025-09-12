import { useState, useEffect } from 'react';
import axios from 'axios';
import config from '../config';

interface QueryNode {
  kind: string;
  include?: QueryNode[];
  select?: string[];
  criteria?: Record<string, any>;
}

interface UseStellarQueryOptions {
  enabled?: boolean;
  onSuccess?: (data: any) => void;
  onError?: (error: any) => void;
}

export const useStellarQuery = (
  query: QueryNode | null,
  options: UseStellarQueryOptions = {}
) => {
  const [data, setData] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<any>(null);

  const { enabled = true, onSuccess, onError } = options;

  useEffect(() => {
    if (!query || !enabled) {
      return;
    }

    const fetchData = async () => {
      setLoading(true);
      setError(null);

      try {
        const response = await axios.post(
          config.getApiEndpoint('/stellar/query'),
          query
        );

        if (response.data.success) {
          setData(response.data.data);
          onSuccess?.(response.data.data);
        } else {
          throw new Error(response.data.error || 'Query failed');
        }
      } catch (err: any) {
        console.error('Stellar query error:', err);
        setError(err);
        onError?.(err);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [query, enabled, onSuccess, onError]);

  const refetch = () => {
    if (query) {
      setLoading(true);
      setError(null);
      
      axios.post(config.getApiEndpoint('/stellar/query'), query)
        .then(response => {
          if (response.data.success) {
            setData(response.data.data);
            onSuccess?.(response.data.data);
          } else {
            throw new Error(response.data.error || 'Query failed');
          }
        })
        .catch(err => {
          console.error('Stellar query error:', err);
          setError(err);
          onError?.(err);
        })
        .finally(() => {
          setLoading(false);
        });
    }
  };

  return { data, loading, error, refetch };
};

// Helper function to build infrastructure hierarchy query
export const buildInfrastructureQuery = (): QueryNode => {
  return {
    kind: 'partner',
    include: [
      {
        kind: 'cloud',
        include: [
          {
            kind: 'datacenter',
            include: [
              {
                kind: 'compute'
              },
              {
                kind: 'networkdevice'
              }
            ]
          }
        ]
      }
    ]
  };
};