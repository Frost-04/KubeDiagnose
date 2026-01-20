# Project Structure

## Overview

KubeDiagnose is a Spring Boot application that provides REST APIs for debugging Kubernetes Pods and Services. It follows a clean layered architecture that separates concerns and makes the codebase maintainable and testable.

The application supports both **single-resource debugging** (individual pods/services) and **bulk debugging** (all pods/services in a namespace), along with **namespace discovery**.

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
│  │  (reused for single &   │    │  (reused for single &       │     ││
│  │   bulk operations)      │    │   bulk operations)          │     ││
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
├── KubeDiagnoseApplication.java    # Main Spring Boot application entry point
├── config/                          # Configuration classes
│   ├── KubernetesClientConfig.java  # Kubernetes client configuration
│   └── LenientJSON.java             # JSON parser configuration
├── controller/                      # REST API controllers
│   ├── PodDebugController.java      # Pod debugging endpoints (single & bulk)
│   ├── ServiceDebugController.java  # Service debugging endpoints (single & bulk)
│   └── NamespaceController.java     # Namespace discovery endpoint
├── service/                         # Business logic services
│   ├── PodDebugService.java         # Pod debugging business logic
│   ├── ServiceDebugService.java     # Service debugging business logic
│   └── NamespaceService.java        # Namespace discovery logic
├── analyzer/                        # Analysis components
│   ├── PodAnalyzer.java             # Pod issue analysis
│   └── ServiceAnalyzer.java         # Service issue analysis
├── rules/                           # Diagnostic rule definitions
│   ├── PodDiagnosticRules.java      # Rules for pod issues
│   └── ServiceDiagnosticRules.java  # Rules for service issues
└── model/                           # Data transfer objects
    ├── PodDiagnosticResult.java     # Single pod diagnosis response DTO
    ├── ServiceDiagnosticResult.java # Single service diagnosis response DTO
    ├── BulkPodDiagnosticResult.java # Bulk pod diagnosis response DTO
    ├── BulkServiceDiagnosticResult.java # Bulk service diagnosis response DTO
    ├── NamespaceListResponse.java   # Namespace list response DTO
    └── ErrorResponse.java           # Error response DTO
```

## Package Descriptions

### `config`

Contains Spring configuration classes. The `KubernetesClientConfig` class is responsible for:
- Loading kubeconfig from `~/.kube/config` for local development
- Falling back to in-cluster configuration when deployed to Kubernetes
- Creating and configuring the `CoreV1Api` bean used throughout the application

### `controller`

Contains REST API controllers that handle HTTP requests:
- **PodDebugController**: Exposes pod debugging endpoints
  - `GET /api/debug/pod/{namespace}/{podName}` - Debug single pod
  - `GET /api/debug/pods/{namespace}` - Debug all pods in namespace
- **ServiceDebugController**: Exposes service debugging endpoints
  - `GET /api/debug/service/{namespace}/{serviceName}` - Debug single service
  - `GET /api/debug/services/{namespace}` - Debug all services in namespace
- **NamespaceController**: Exposes namespace discovery endpoint
  - `GET /api/namespaces` - List all namespaces

Controllers handle request mapping, error handling, and HTTP status code mapping.

### `service`

Contains business logic services that orchestrate the debugging workflow:
- **PodDebugService**: Pod debugging business logic
  - `debugPod(namespace, podName)` - Debug a single pod
  - `debugAllPods(namespace)` - Debug all pods in a namespace (bulk)
- **ServiceDebugService**: Service debugging business logic
  - `debugService(namespace, serviceName)` - Debug a single service
  - `debugAllServices(namespace)` - Debug all services in a namespace (bulk)
- **NamespaceService**: Namespace discovery logic
  - `listNamespaces()` - List all namespaces in the cluster

### `analyzer`

Contains analyzer components that coordinate diagnostic rule execution:
- **PodAnalyzer**: Runs all pod diagnostic rules and aggregates results
- **ServiceAnalyzer**: Runs all service diagnostic rules and aggregates results

Analyzers determine the overall status (Healthy, Warning, Critical) based on findings.

**Key Design Decision**: Analyzers are reused for both single-resource and bulk operations. The bulk operations in services iterate over resources and call the same analyzer for each one.

### `rules`

Contains static diagnostic rule definitions:
- **PodDiagnosticRules**: Rules for detecting CrashLoopBackOff, ImagePullBackOff, OOMKilled, probe failures, high restart count
- **ServiceDiagnosticRules**: Rules for detecting selector mismatch, no endpoints, port mismatch, CoreDNS issues

Rules are stateless and receive Kubernetes objects as input, returning causes, evidence, and suggested actions.

### `model`

Contains Data Transfer Objects (DTOs) for API responses:
- **PodDiagnosticResult**: Single pod diagnostic response
- **ServiceDiagnosticResult**: Single service diagnostic response
- **BulkPodDiagnosticResult**: Bulk pod diagnostic response (wraps multiple PodDiagnosticResult)
- **BulkServiceDiagnosticResult**: Bulk service diagnostic response (wraps multiple ServiceDiagnosticResult)
- **NamespaceListResponse**: Namespace list response
- **ErrorResponse**: Error response for failed requests

## Request Flows

### Single Pod Debug Flow

1. **Request**: Client sends `GET /api/debug/pod/{namespace}/{podName}`
2. **Controller**: `PodDebugController` receives the request
3. **Service**: `PodDebugService.debugPod()` fetches the pod from Kubernetes API
4. **Analyzer**: `PodAnalyzer.analyze()` runs all diagnostic rules
5. **Rules**: Each rule in `PodDiagnosticRules` checks for specific issues
6. **Response**: Aggregated `PodDiagnosticResult` is returned as JSON

### Single Service Debug Flow

1. **Request**: Client sends `GET /api/debug/service/{namespace}/{serviceName}`
2. **Controller**: `ServiceDebugController` receives the request
3. **Service**: `ServiceDebugService.debugService()` fetches:
   - The service itself
   - Associated endpoints
   - All pods in the namespace
   - CoreDNS pods from kube-system
4. **Analyzer**: `ServiceAnalyzer.analyze()` runs all diagnostic rules
5. **Rules**: Each rule in `ServiceDiagnosticRules` checks for specific issues
6. **Response**: Aggregated `ServiceDiagnosticResult` is returned as JSON

### Bulk Pod Debug Flow

1. **Request**: Client sends `GET /api/debug/pods/{namespace}`
2. **Controller**: `PodDebugController.debugAllPods()` receives the request
3. **Service**: `PodDebugService.debugAllPods()`:
   - Fetches ALL pods in the namespace via `listNamespacedPod()`
   - Iterates over each pod, calling `PodAnalyzer.analyze()` for each
   - Handles individual pod failures gracefully (doesn't fail entire request)
   - Counts Critical/Warning/Healthy pods
   - Sorts results by severity (Critical → Warning → Healthy)
4. **Response**: `BulkPodDiagnosticResult` with summary and sorted results

### Bulk Service Debug Flow

1. **Request**: Client sends `GET /api/debug/services/{namespace}`
2. **Controller**: `ServiceDebugController.debugAllServices()` receives the request
3. **Service**: `ServiceDebugService.debugAllServices()`:
   - Fetches ALL services in the namespace via `listNamespacedService()`
   - Pre-fetches pods and CoreDNS pods (shared across all service analyses)
   - Iterates over each service, calling `ServiceAnalyzer.analyze()` for each
   - Handles individual service failures gracefully
   - Counts Critical/Warning/Healthy services
   - Sorts results by severity (Critical → Warning → Healthy)
4. **Response**: `BulkServiceDiagnosticResult` with summary and sorted results

### Namespace Discovery Flow

1. **Request**: Client sends `GET /api/namespaces`
2. **Controller**: `NamespaceController.listNamespaces()` receives the request
3. **Service**: `NamespaceService.listNamespaces()`:
   - Fetches all namespaces via `listNamespace()`
   - Extracts namespace names
   - Sorts alphabetically
4. **Response**: `NamespaceListResponse` with total count and sorted names

## Configuration Files

### `application.yml`

Main Spring Boot configuration:
- Server port (default: 8080)
- Kubernetes kubeconfig path
- Logging levels

### `pom.xml`

Maven build configuration:
- Spring Boot 3.2.2
- Java 17
- Kubernetes Java Client 20.0.1

## Test Manifests

Located in `manifests/` directory:
- **crashloop-pod.yaml**: Creates a pod that enters CrashLoopBackOff
- **imagepull-fail.yaml**: Creates a pod with ImagePullBackOff
- **bad-service.yaml**: Creates services with selector and port mismatches
