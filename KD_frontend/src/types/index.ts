export * from './namespace';
export * from './pod';
export * from './service';

// Common API error response
export interface ApiError {
  error: string;
  message: string;
  timestamp: string;
  status: number;
}
