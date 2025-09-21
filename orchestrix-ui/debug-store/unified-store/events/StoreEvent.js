// Auto-generated from TypeScript source
// Original: events/StoreEvent.ts
// Generated: 2025-09-19T10:56:20.506Z

// Store Event Types and Interfaces

export type StoreEventType = 'query' | 'mutation';
export type StoreOperation = 'create' | 'read' | 'update' | 'delete' | 'list' | 
  'QUERY_START' | 'QUERY_SUCCESS' | 'QUERY_ERROR' |
  'INSERT_START' | 'INSERT_SUCCESS' | 'INSERT_ERROR' |
  'UPDATE_START' | 'UPDATE_SUCCESS' | 'UPDATE_ERROR' |
  'DELETE_START' | 'DELETE_SUCCESS' | 'DELETE_ERROR';
export type StoreEventStatus = 'pending' | 'success' | 'error';



export interface StoreEventResponse extends StoreEvent {
  status?: StoreEventStatus;
  success?: boolean;
  data?: any;
  error?: string;
  errorDetails?: any;
  duration?: number; // Time taken in ms
}

// Event ID generator - short unique IDs
export function generateEventId(): string {
  const timestamp = Date.now().toString(36);
  const random = Math.random().toString(36).substring(2, 6);
  return `${timestamp}-${random}`;
}

// Event factory functions
export function createQueryEvent(
  entity,
  operation: StoreOperation = 'list',
  payload?): StoreEvent {
  return {
    id: generateEventId(),
    type: 'query',
    entity,
    operation,
    payload,
    timestamp: Date.now(),
  };
}

export function createMutationEvent(
  entity,
  operation,
  payload): StoreEvent {
  return {
    id: generateEventId(),
    type: 'mutation',
    entity,
    operation,
    payload,
    timestamp: Date.now(),
  };
}

// Response factory
export function createEventResponse(
  event,
  status,
  data?,
  error?): StoreEventResponse {
  const response: StoreEventResponse = {
    ...event,
    status,
    duration: Date.now() - event.timestamp,
  };

  if (data !== undefined) {
    response.data = data;
  }
  
  if (error) {
    response.error = error;
  }

  return response;
}