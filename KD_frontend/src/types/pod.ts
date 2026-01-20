// Types based on API_REFERENCE.md - Pod diagnostic responses

export type PodStatus = 'Healthy' | 'Warning' | 'Critical' | 'Unknown' | 'Completed';

export type PodPhase = 'Pending' | 'Running' | 'Succeeded' | 'Failed' | 'Unknown';

export interface ContainerStatus {
  name: string;
  state: string;
  reason: string | null;
  message: string | null;
  restartCount: number;
  ready: boolean;
}

export interface DiagnosticSummary {
  diagnosticTime: string;
  resourceType: string;
  overallHealth: PodStatus;
  message: string;
}

export interface PodDiagnosticResult {
  summary?: DiagnosticSummary;
  resourceName: string;
  namespace: string;
  status: PodStatus;
  phase: PodPhase;
  probableCauses: string[];
  evidence: string[];
  suggestedActions: string[];
  containerStatuses: ContainerStatus[];
  restartCount: number;
}

export interface BulkPodDiagnosticResult {
  summary: DiagnosticSummary;
  namespace: string;
  totalPods: number;
  criticalCount: number;
  warningCount: number;
  healthyCount: number;
  results: PodDiagnosticResult[];
}
