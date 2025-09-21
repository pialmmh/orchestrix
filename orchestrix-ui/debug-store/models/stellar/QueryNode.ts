export interface QueryNode {
  kind: string;
  select?: string[];
  criteria?: Record<string, any>;
  include?: QueryNode[];
  page?: {
    limit: number;
    offset: number;
  };
  lazy?: boolean;
  lazyLoadKey?: string;
}

export interface QueryResponse<T = any> {
  success: boolean;
  data?: T[];
  error?: string;
  count?: number;
}