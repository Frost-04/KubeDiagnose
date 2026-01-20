# Project Structure

## Overview

KubeDiagnose is a Spring Boot application that exposes REST APIs for debugging Kubernetes Pods and Services. It uses a layered architecture to keep concerns separated and the codebase easy to work with.

The application supports **single-resource debugging** (individual pods/services), **bulk debugging** (all pods/services in a namespace), and **namespace discovery**.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                                REST API Layer                                        │
│  ┌───────────────────────┐  ┌─────────────────────────┐  ┌────────────────────────┐ │
│  │  PodDebugController   │  │ ServiceDebugController  │  │  NamespaceController   │ │
│  │  - Single pod debug   │  │  - Single svc debug     │  │  - List namespaces     │ │
│  │  - Bulk pods debug    │  │  - Bulk svcs debug      │  │                        │ │
│  └──────────┬────────────┘  └───────────┬─────────────┘  └───────────┬────────────┘ │
└─────────────┼───────────────────────────┼────────────────────────────┼──────────────┘
              │                           │                            │
              ▼                           ▼                            ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                                Service Layer                                         │
│  ┌───────────────────────┐  ┌─────────────────────────┐  ┌────────────────────────┐ │
│  │   PodDebugService     │  │   ServiceDebugService   │  │   NamespaceService     │ │
│  │  - debugPod()         │  │  - debugService()       │  │  - listNamespaces()    │ │
│  │  - debugAllPods()     │  │  - debugAllServices()   │  │                        │ │
│  └──────────┬────────────┘  └───────────┬─────────────┘  └───────────┬────────────┘ │
└─────────────┼───────────────────────────┼────────────────────────────┼──────────────┘
              │                           │                            │
              ▼                           ▼                            │
┌─────────────────────────────────────────────────────────────────────┐│
│                        Analyzer Layer                                ││
│  ┌─────────────────────────┐    ┌─────────────────────────────┐     ││
│  │      PodAnalyzer        │    │      ServiceAnalyzer        │     ││
│  │  (single + bulk)        │    │  (single + bulk)            │     ││
│  └───────────┬─────────────┘    └──────────────┬──────────────┘     ││
└──────────────┼──────────────────────────────────┼────────────────────┘│
               │                                  │                     │
               ▼                                  ▼                     │
┌─────────────────────────────────────────────────────────────────────┐│
│                         Rules Layer                                  ││
│  ┌─────────────────────────┐    ┌─────────────────────────────┐     ││
│  │  PodDiagnosticRules     │    │  ServiceDiagnosticRules     │     ││
│  └─────────────────────────┘    └─────────────────────────────┘     ││
└─────────────────────────────────────────────────────────────────────┘│
               │                                  │                     │
               ▼                                  ▼                     ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                           Kubernetes Java Client                                     │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐│
│  │                               CoreV1Api                                          ││
│  │   - readNamespacedPod()    - listNamespacedPod()    - listNamespace()           ││
│  │   - readNamespacedService() - listNamespacedService() - readNamespacedEndpoints()││
│  └─────────────────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                           Kubernetes Cluster                                         │
│                          (minikube / kind / etc.)                                   │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

## Package Structure

```
src/main/java/com/kubediagnose/
├── KubeDiagnoseApplication.java
├── config/
│   ├── KubernetesClientConfig.java
│   └── LenientJSON.java
├── controller/
│   ├── PodDebugController.java
│   ├── ServiceDebugController.java
│   └── NamespaceController.java
├── service/
│   ├── PodDebugService.java
│   ├── ServiceDebugService.java
│   └── NamespaceService.java
├── analyzer/
│   ├── PodAnalyzer.java
│   └── ServiceAnalyzer.java
├── rules/
│   ├── PodDiagnosticRules.java
│   └── ServiceDiagnosticRules.java
└── model/
    ├── PodDiagnosticResult.java
    ├── ServiceDiagnosticResult.java
    ├── BulkPodDiagnosticResult.java
    ├── BulkServiceDiagnosticResult.java
    ├── NamespaceListResponse.java
    └── ErrorResponse.java
```

## Package Descriptions

### `config`

Spring configuration:
- `KubernetesClientConfig`: builds `ApiClient` / `CoreV1Api` from kubeconfig or in-cluster config.
- `LenientJSON`: JSON configuration compatible with Kubernetes API responses.

### `controller`

HTTP entrypoints:
- **PodDebugController**
  - `GET /api/debug/pod/{namespace}/{podName}` – single pod
  - `GET /api/debug/pods/{namespace}` – all pods in namespace
- **ServiceDebugController**
  - `GET /api/debug/service/{namespace}/{serviceName}` – single service
  - `GET /api/debug/services/{namespace}` – all services in namespace
- **NamespaceController**
  - `GET /api/namespaces` – list namespaces

### `service`

Business logic:
- **PodDebugService**
  - `debugPod(namespace, podName)` – single pod
  - `debugAllPods(namespace)` – all pods (bulk)
- **ServiceDebugService**
  - `debugService(namespace, serviceName)` – single service
  - `debugAllServices(namespace)` – all services (bulk)
- **NamespaceService**
  - `listNamespaces()` – all namespaces

### `analyzer`

Runs rules and builds diagnosis:
- **PodAnalyzer** – aggregates pod rules and status.
- **ServiceAnalyzer** – aggregates service rules and status.

Same analyzers are used for single and bulk operations.

### `rules`

Stateless rule sets:
- **PodDiagnosticRules** – CrashLoopBackOff, image pull errors, OOMKilled, probes, restarts.
- **ServiceDiagnosticRules** – selector mismatch, missing endpoints, port mismatch, CoreDNS.

Each rule adds causes, evidence, and actions.

### `model`

API DTOs:
- **PodDiagnosticResult** – single pod.
- **ServiceDiagnosticResult** – single service.
- **BulkPodDiagnosticResult** – many pods + summary.
- **BulkServiceDiagnosticResult** – many services + summary.
- **NamespaceListResponse** – namespace names.
- **ErrorResponse** – error body.

## Request Flows

### Single Pod Debug

1. `GET /api/debug/pod/{namespace}/{podName}`
2. `PodDebugController` → `PodDebugService.debugPod()`
3. Service fetches pod via `CoreV1Api.readNamespacedPod()`
4. `PodAnalyzer.analyze()` calls `PodDiagnosticRules`
5. Returns `PodDiagnosticResult` as JSON

### Single Service Debug

1. `GET /api/debug/service/{namespace}/{serviceName}`
2. `ServiceDebugController` → `ServiceDebugService.debugService()`
3. Service fetches service, endpoints, pods, and CoreDNS pods
4. `ServiceAnalyzer.analyze()` calls `ServiceDiagnosticRules`
5. Returns `ServiceDiagnosticResult`

### Bulk Pod Debug

1. `GET /api/debug/pods/{namespace}`
2. `PodDebugController.debugAllPods()` → `PodDebugService.debugAllPods()`
3. Service lists pods, runs `PodAnalyzer.analyze()` per pod, handles per-pod failures
4. Sorts results by severity and counts Critical/Warning/Healthy
5. Returns `BulkPodDiagnosticResult`

### Bulk Service Debug

1. `GET /api/debug/services/{namespace}`
2. `ServiceDebugController.debugAllServices()` → `ServiceDebugService.debugAllServices()`
3. Service lists services, pre-fetches pods and CoreDNS pods, runs `ServiceAnalyzer.analyze()` per service
4. Sorts results by severity and counts Critical/Warning/Healthy
5. Returns `BulkServiceDiagnosticResult`

### Namespace Discovery

1. `GET /api/namespaces`
2. `NamespaceController.listNamespaces()` → `NamespaceService.listNamespaces()`
3. Service calls `CoreV1Api.listNamespace()`, extracts and sorts names
4. Returns `NamespaceListResponse`

## Configuration Files

### `application.yml`

- Server port (default 8080)
- Kubernetes kubeconfig path
- Logging levels

### `pom.xml`

- Spring Boot 3.2.2
- Java 17
- Kubernetes Java Client

## Test Manifests

In `manifests/`:
- `crashloop-pod.yaml` – pod that CrashLoopBackOffs
- `imagepull-fail.yaml` – pod with image pull failure
- `bad-service.yaml` – services with selector/port issues
