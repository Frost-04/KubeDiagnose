import axios, { AxiosError } from 'axios';
import type { ApiError } from '../types';

// Axios client (configurable base URL)
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Normalize API errors
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiError>) => {
    // API error payload
    if (error.response?.data) {
      const apiError = error.response.data;
      return Promise.reject({
        message: apiError.message || 'An unexpected error occurred',
        status: apiError.status || error.response.status,
        error: apiError.error || 'Error',
      });
    }
    
    // Timeout
    if (error.code === 'ECONNABORTED') {
      return Promise.reject({
        message: 'Request timed out. Please try again.',
        status: 408,
        error: 'Timeout',
      });
    }
    
    // Network/connection issue
    if (!error.response) {
      return Promise.reject({
        message: 'Unable to connect to the server. Is the backend running?',
        status: 0,
        error: 'Network Error',
      });
    }
    
    // Fallback
    return Promise.reject({
      message: error.message || 'An unexpected error occurred',
      status: error.response?.status || 500,
      error: 'Error',
    });
  }
);

export default apiClient;
