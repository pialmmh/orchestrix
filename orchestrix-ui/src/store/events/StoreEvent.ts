// Store Event Types and Interfaces

export type StoreEventType = 'query' | 'mutation';
export type StoreOperation = 'create' | 'read' | 'update' | 'delete' | 'list';
export type StoreEventStatus = 'pending' | 'success' | 'error';

export interface StoreEvent {
  id: string;
  type: StoreEventType;
  entity: string;
  operation: StoreOperation;
  payload?: any;
  timestamp: number;
  userId?: string;
  sessionId?: string;
}

export interface StoreEventResponse extends StoreEvent {
  status: StoreEventStatus;
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
  entity: string,
  operation: StoreOperation = 'list',
  payload?: any
): StoreEvent {
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
  entity: string,
  operation: StoreOperation,
  payload: any
): StoreEvent {
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
  event: StoreEvent,
  status: StoreEventStatus,
  data?: any,
  error?: string
): StoreEventResponse {
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