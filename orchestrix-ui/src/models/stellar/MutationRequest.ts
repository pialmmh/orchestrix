export interface EntityModificationRequest {
  entityName: string;
  operation: 'INSERT' | 'UPDATE' | 'DELETE';
  data?: Record<string, any>;
  id?: number | string;
  nested?: EntityModificationRequest[];
}

export interface MutationResponse {
  success: boolean;
  message?: string;
  data?: any;
  error?: string;
  entityId?: number | string;
}