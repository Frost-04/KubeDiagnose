import apiClient from './client';
import type { NamespaceListResponse } from '../types';

// List namespaces
export async function getNamespaces(): Promise<NamespaceListResponse> {
  const response = await apiClient.get<NamespaceListResponse>('/namespaces');
  return response.data;
}
