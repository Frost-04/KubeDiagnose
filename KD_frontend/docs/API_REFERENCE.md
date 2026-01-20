# API Reference

## Overview

KubeDiagnose provides REST API endpoints for debugging Kubernetes resources:

### Single Resource Debugging
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/debug/pod/{namespace}/{podName}` | GET | Debug a specific pod |
| `/api/debug/service/{namespace}/{serviceName}` | GET | Debug a specific service |

### Bulk Debugging (Namespace-wide)
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/debug/pods/{namespace}` | GET | Debug ALL pods in a namespace |
| `/api/debug/services/{namespace}` | GET | Debug ALL services in a namespace |

### Discovery
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/namespaces` | GET | List all available namespaces |

All responses are in JSON format with pretty-printing enabled.

---

## Pod Debug API

### Endpoint

```
GET /api/debug/pod/{namespace}/{podName}
```

### Description

Analyzes a Kubernetes pod and returns diagnostic information including detected issues, evidence, and suggested remediation actions.

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `namespace` | string | Yes | The Kubernetes namespace containing the pod |
| `podName` | string | Yes | The name of the pod to debug |

### Issues Detected

- **CrashLoopBackOff**: Container is repeatedly crashing after starting
- **ImagePullBackOff / ErrImagePull**: Cannot pull container image
- **OOMKilled**: Container was killed due to out-of-memory
- **Failed Probes**: Liveness or readiness probe failures
- **High Restart Count**: Container has been restarted many times (threshold: 5)

### Example Request

```bash
curl -X GET http://localhost:8080/api/debug/pod/default/my-app-pod
```

### Success Response (200 OK)

```json
{
  "resourceName": "crashloop-pod",
  "namespace": "default",
  "status": "Critical",
  "phase": "Running",
  "probableCauses": [
    "Container 'crashloop-container' is in CrashLoopBackOff",
    "Container 'crashloop-container' has high restart count: 8"
  ],
  "evidence": [
    "Container state: Waiting, Reason: CrashLoopBackOff",
    "Message: back-off 5m0s restarting failed container",
    "Restart count: 8",
    "Container 'crashloop-container' restart count: 8",
    "Ready status: false"
  ],
  "suggestedActions": [
    "Check container logs: kubectl logs crashloop-pod -c crashloop-container --previous",
    "Review application startup logic and exit codes",
    "Verify environment variables and configuration",
    "Check if required dependencies or services are available",
    "Check previous container logs: kubectl logs crashloop-pod -c crashloop-container --previous",
    "Review application stability and error handling",
    "Check resource limits (CPU/Memory)"
  ],
  "containerStatuses": [
    {
      "name": "crashloop-container",
      "state": "Waiting",
      "reason": "CrashLoopBackOff",
      "message": "back-off 5m0s restarting failed container",
      "restartCount": 8,
      "ready": false
    }
  ],
  "restartCount": 8
}
```

### Healthy Pod Response

```json
{
  "resourceName": "healthy-pod",
  "namespace": "default",
  "status": "Healthy",
  "phase": "Running",
  "probableCauses": [
    "No issues detected"
  ],
  "evidence": [
    "Pod phase: Running",
    "All containers appear healthy"
  ],
  "suggestedActions": [
    "No action required - pod appears to be running normally"
  ],
  "containerStatuses": [
    {
      "name": "app-container",
      "state": "Running",
      "reason": null,
      "message": null,
      "restartCount": 0,
      "ready": true
    }
  ],
  "restartCount": 0
}
```

### Error Response (404 Not Found)

```json
{
  "error": "Not Found",
  "message": "Pod 'nonexistent-pod' not found in namespace 'default'",
  "timestamp": "2025-01-21T10:30:00.000Z",
  "status": 404
}
```

---

## Service Debug API

### Endpoint

```
GET /api/debug/service/{namespace}/{serviceName}
```

### Description

Analyzes a Kubernetes service and returns diagnostic information about networking configuration, endpoint status, and potential issues.

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `namespace` | string | Yes | The Kubernetes namespace containing the service |
| `serviceName` | string | Yes | The name of the service to debug |

### Issues Detected

- **Selector Mismatch**: Service selector doesn't match any pod labels
- **No Endpoints**: Service has no backing pods
- **Port Mismatch**: Service targetPort doesn't match container port
- **CoreDNS Missing**: CoreDNS is not running in the cluster

### Example Request

```bash
curl -X GET http://localhost:8080/api/debug/service/default/my-service
```

### Success Response (200 OK) - Service with Issues

```json
{
  "resourceName": "bad-selector-service",
  "namespace": "default",
  "status": "Critical",
  "serviceType": "ClusterIP",
  "selector": {
    "app": "frontend"
  },
  "ports": [
    {
      "name": null,
      "protocol": "TCP",
      "port": 80,
      "targetPort": 80,
      "nodePort": null
    }
  ],
  "probableCauses": [
    "Service selector does not match any pods",
    "Service has no endpoints"
  ],
  "evidence": [
    "Service selector: {app=frontend}",
    "Total pods in namespace: 3",
    "Matching pods: 0",
    "Pod 'backend-pod' labels: {app=backend, version=v1}",
    "No endpoint subsets found for this service",
    "CoreDNS is running (2 pod(s))"
  ],
  "suggestedActions": [
    "Verify the service selector labels match pod labels",
    "Use 'kubectl get pods --show-labels' to see pod labels",
    "Update service selector or pod labels to match",
    "Ensure pods matching the service selector are running",
    "Check if pods are in Ready state",
    "Verify service selector matches pod labels"
  ],
  "endpointInfo": {
    "readyEndpoints": 0,
    "notReadyEndpoints": 0,
    "addresses": []
  },
  "coreDnsExists": true
}
```

### Healthy Service Response

```json
{
  "resourceName": "healthy-service",
  "namespace": "default",
  "status": "Healthy",
  "serviceType": "ClusterIP",
  "selector": {
    "app": "backend"
  },
  "ports": [
    {
      "name": "http",
      "protocol": "TCP",
      "port": 80,
      "targetPort": 8080,
      "nodePort": null
    }
  ],
  "probableCauses": [
    "No issues detected"
  ],
  "evidence": [
    "Service type: ClusterIP",
    "Ready endpoints: 3",
    "CoreDNS is operational"
  ],
  "suggestedActions": [
    "No action required - service appears to be configured correctly"
  ],
  "endpointInfo": {
    "readyEndpoints": 3,
    "notReadyEndpoints": 0,
    "addresses": [
      "10.244.0.5 (Ready)",
      "10.244.0.6 (Ready)",
      "10.244.0.7 (Ready)"
    ]
  },
  "coreDnsExists": true
}
```

### Error Response (404 Not Found)

```json
{
  "error": "Not Found",
  "message": "Service 'nonexistent-service' not found in namespace 'default'",
  "timestamp": "2025-01-21T10:30:00.000Z",
  "status": 404
}
```

---

## Bulk Pod Debug API

### Endpoint

```
GET /api/debug/pods/{namespace}
```

### Description

Analyzes ALL pods in a namespace and returns aggregated diagnostic results. Results are sorted by severity (Critical → Warning → Healthy). The request does not fail if analysis of individual pods fails.

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `namespace` | string | Yes | The Kubernetes namespace to scan for pods |

### Example Request

```bash
curl -X GET http://localhost:8080/api/debug/pods/default
```

```powershell
# Windows PowerShell
Invoke-RestMethod -Uri http://localhost:8080/api/debug/pods/default | ConvertTo-Json -Depth 10
```

### Success Response (200 OK)

```json
{
  "summary": {
    "diagnosticTime": "2026-01-21T10:30:00.000Z",
    "resourceType": "Pods (Bulk)",
    "overallHealth": "Critical",
    "message": "Namespace 'default': 5 pods analyzed - 2 critical, 1 warning, 2 healthy."
  },
  "namespace": "default",
  "totalPods": 5,
  "criticalCount": 2,
  "warningCount": 1,
  "healthyCount": 2,
  "results": [
    {
      "summary": {
        "diagnosticTime": "2026-01-21T10:30:00.000Z",
        "resourceType": "Pod",
        "overallHealth": "Critical",
        "issueCount": 2,
        "message": "Pod 'crashloop-pod' has 2 issues requiring attention. Status: Critical"
      },
      "resourceName": "crashloop-pod",
      "namespace": "default",
      "status": "Critical",
      "phase": "Running",
      "restartCount": 15,
      "probableCauses": [
        "Container 'app' is in CrashLoopBackOff",
        "Container 'app' has high restart count: 15"
      ],
      "evidence": ["..."],
      "suggestedActions": ["..."],
      "containerStatuses": [{"..."}]
    },
    {
      "resourceName": "imagepull-fail-pod",
      "namespace": "default",
      "status": "Critical",
      "phase": "Pending",
      "probableCauses": ["Container cannot pull image: ImagePullBackOff"],
      "...": "..."
    },
    {
      "resourceName": "healthy-pod",
      "namespace": "default",
      "status": "Healthy",
      "phase": "Running",
      "probableCauses": ["No issues detected"],
      "...": "..."
    }
  ]
}
```

### Response with No Pods

```json
{
  "summary": {
    "diagnosticTime": "2026-01-21T10:30:00.000Z",
    "resourceType": "Pods (Bulk)",
    "overallHealth": "Healthy",
    "message": "No pods found in namespace 'empty-namespace'."
  },
  "namespace": "empty-namespace",
  "totalPods": 0,
  "criticalCount": 0,
  "warningCount": 0,
  "healthyCount": 0,
  "results": []
}
```

---

## Bulk Service Debug API

### Endpoint

```
GET /api/debug/services/{namespace}
```

### Description

Analyzes ALL services in a namespace and returns aggregated diagnostic results. Results are sorted by severity (Critical → Warning → Healthy). The request does not fail if analysis of individual services fails.

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `namespace` | string | Yes | The Kubernetes namespace to scan for services |

### Example Request

```bash
curl -X GET http://localhost:8080/api/debug/services/default
```

```powershell
# Windows PowerShell
Invoke-RestMethod -Uri http://localhost:8080/api/debug/services/default | ConvertTo-Json -Depth 10
```

### Success Response (200 OK)

```json
{
  "summary": {
    "diagnosticTime": "2026-01-21T10:30:00.000Z",
    "resourceType": "Services (Bulk)",
    "overallHealth": "Warning",
    "message": "Namespace 'default': 4 services analyzed - 1 critical, 2 warning, 1 healthy."
  },
  "namespace": "default",
  "totalServices": 4,
  "criticalCount": 1,
  "warningCount": 2,
  "healthyCount": 1,
  "results": [
    {
      "summary": {
        "diagnosticTime": "2026-01-21T10:30:00.000Z",
        "resourceType": "Service",
        "overallHealth": "Critical",
        "issueCount": 2,
        "message": "Service 'bad-selector-service' has 2 issues requiring attention. Status: Critical"
      },
      "resourceName": "bad-selector-service",
      "namespace": "default",
      "status": "Critical",
      "serviceType": "ClusterIP",
      "selector": {"app": "frontend"},
      "probableCauses": [
        "Service selector does not match any pods",
        "Service has no endpoints"
      ],
      "evidence": ["..."],
      "suggestedActions": ["..."],
      "endpointInfo": {
        "readyEndpoints": 0,
        "notReadyEndpoints": 0,
        "addresses": []
      },
      "coreDnsExists": true
    },
    {
      "resourceName": "healthy-service",
      "namespace": "default",
      "status": "Healthy",
      "serviceType": "ClusterIP",
      "probableCauses": ["No issues detected"],
      "...": "..."
    }
  ]
}
```

### Response with No Services

```json
{
  "summary": {
    "diagnosticTime": "2026-01-21T10:30:00.000Z",
    "resourceType": "Services (Bulk)",
    "overallHealth": "Healthy",
    "message": "No services found in namespace 'empty-namespace'."
  },
  "namespace": "empty-namespace",
  "totalServices": 0,
  "criticalCount": 0,
  "warningCount": 0,
  "healthyCount": 0,
  "results": []
}
```

---

## Namespace List API

### Endpoint

```
GET /api/namespaces
```

### Description

Lists all available namespaces in the Kubernetes cluster. Results are sorted alphabetically. This is a lightweight discovery endpoint to help users identify which namespaces to debug.

### Example Request

```bash
curl -X GET http://localhost:8080/api/namespaces
```

```powershell
# Windows PowerShell
Invoke-RestMethod -Uri http://localhost:8080/api/namespaces | ConvertTo-Json -Depth 5
```

### Success Response (200 OK)

```json
{
  "total": 5,
  "namespaces": [
    "default",
    "dev",
    "kube-node-lease",
    "kube-public",
    "kube-system"
  ]
}
```

### Error Response (403 Forbidden)

```json
{
  "error": "Forbidden",
  "message": "Access denied to list namespaces. Check RBAC permissions.",
  "timestamp": "2026-01-21T10:30:00.000Z",
  "status": 403
}
```

---

## Response Field Descriptions

### Common Fields

| Field | Type | Description |
|-------|------|-------------|
| `resourceName` | string | Name of the Kubernetes resource |
| `namespace` | string | Namespace of the resource |
| `status` | string | Overall status: `Healthy`, `Warning`, `Critical`, `Unknown`, `Completed` |
| `probableCauses` | array | List of detected issues or "No issues detected" |
| `evidence` | array | List of observations supporting the diagnosis |
| `suggestedActions` | array | List of recommended remediation steps |
| `summary` | object | Quick overview with diagnostic time, health status, and message |

### Pod-Specific Fields

| Field | Type | Description |
|-------|------|-------------|
| `phase` | string | Pod phase: `Pending`, `Running`, `Succeeded`, `Failed`, `Unknown` |
| `restartCount` | integer | Total restart count across all containers |
| `containerStatuses` | array | Detailed status of each container |

### Service-Specific Fields

| Field | Type | Description |
|-------|------|-------------|
| `serviceType` | string | Service type: `ClusterIP`, `NodePort`, `LoadBalancer` |
| `selector` | object | Service's label selector |
| `ports` | array | Service port configurations |
| `endpointInfo` | object | Endpoint statistics and addresses |
| `coreDnsExists` | boolean | Whether CoreDNS is running in the cluster |

### Bulk Response Fields (Pods)

| Field | Type | Description |
|-------|------|-------------|
| `totalPods` | integer | Total number of pods in the namespace |
| `criticalCount` | integer | Number of pods with Critical status |
| `warningCount` | integer | Number of pods with Warning status |
| `healthyCount` | integer | Number of pods with Healthy status |
| `results` | array | List of `PodDiagnosticResult` objects sorted by severity |

### Bulk Response Fields (Services)

| Field | Type | Description |
|-------|------|-------------|
| `totalServices` | integer | Total number of services in the namespace |
| `criticalCount` | integer | Number of services with Critical status |
| `warningCount` | integer | Number of services with Warning status |
| `healthyCount` | integer | Number of services with Healthy status |
| `results` | array | List of `ServiceDiagnosticResult` objects sorted by severity |

### Namespace List Fields

| Field | Type | Description |
|-------|------|-------------|
| `total` | integer | Total number of namespaces |
| `namespaces` | array | Alphabetically sorted list of namespace names |

---

## HTTP Status Codes

| Status Code | Description |
|-------------|-------------|
| 200 | Success - diagnostic result returned |
| 400 | Bad Request - invalid parameters |
| 403 | Forbidden - access denied (RBAC) |
| 404 | Not Found - resource doesn't exist |
| 500 | Internal Server Error - unexpected error |

---

## Error Response Format

All error responses follow this format:

```json
{
  "error": "Error Type",
  "message": "Detailed error message",
  "timestamp": "ISO 8601 timestamp",
  "status": 404
}
```
