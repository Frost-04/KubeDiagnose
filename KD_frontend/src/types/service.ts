// Types based on API_REFERENCE.md - Service diagnostic responses

export type ServiceStatus = 'Healthy' | 'Warning' | 'Critical' | 'Unknown';

export type ServiceType = 'ClusterIP' | 'NodePort' | 'LoadBalancer';

export interface ServicePort {
  name: string | null;
  protocol: string;
  port: number;
  targetPort: number;
  nodePort: number | null;
}

export interface EndpointInfo {
  readyEndpoints: number;
  notReadyEndpoints: number;
  addresses: string[];
}

export interface ServiceDiagnosticSummary {
  diagnosticTime: string;
  resourceType: string;
  overallHealth: ServiceStatus;
  message: string;
}

export interface ServiceDiagnosticResult {
  summary?: ServiceDiagnosticSummary;
  resourceName: string;
  namespace: string;
  status: ServiceStatus;
  serviceType: ServiceType;
  selector: Record<string, string>;
  ports: ServicePort[];
  probableCauses: string[];
  evidence: string[];
  suggestedActions: string[];
  endpointInfo: EndpointInfo;
  coreDnsExists: boolean;
}

export interface BulkServiceDiagnosticResult {
  summary: ServiceDiagnosticSummary;
  namespace: string;
  totalServices: number;
  criticalCount: number;
  warningCount: number;
  healthyCount: number;
  results: ServiceDiagnosticResult[];
}
