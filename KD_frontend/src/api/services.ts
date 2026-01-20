import apiClient from './client';
import type { ServiceDiagnosticResult, BulkServiceDiagnosticResult } from '../types';

// Bulk services
export async function getServices(namespace: string): Promise<BulkServiceDiagnosticResult> {
  const response = await apiClient.get<BulkServiceDiagnosticResult>(
    `/debug/services/${namespace}`
  );
  return response.data;
}

// Single service
export async function getServiceDetails(
  namespace: string,
  serviceName: string
): Promise<ServiceDiagnosticResult> {
  const response = await apiClient.get<ServiceDiagnosticResult>(
    `/debug/service/${namespace}/${serviceName}`
  );
  return response.data;
}
