import apiClient from './client';
import type { PodDiagnosticResult, BulkPodDiagnosticResult } from '../types';

// Bulk pods
export async function getPods(namespace: string): Promise<BulkPodDiagnosticResult> {
  const response = await apiClient.get<BulkPodDiagnosticResult>(`/debug/pods/${namespace}`);
  return response.data;
}

// Single pod
export async function getPodDetails(
  namespace: string,
  podName: string
): Promise<PodDiagnosticResult> {
  const response = await apiClient.get<PodDiagnosticResult>(
    `/debug/pod/${namespace}/${podName}`
  );
  return response.data;
}
