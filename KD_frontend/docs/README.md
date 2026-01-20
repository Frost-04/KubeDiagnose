# KubeDiagnose

A Spring Boot application that helps debug Kubernetes Pods and Services using the official Kubernetes Java client. KubeDiagnose acts as a simple diagnostic assistant that explains why a pod or service is failing and how to fix it.

## Features

### Pod Debugging
Detects and diagnoses the following pod issues:
- **CrashLoopBackOff** - Container repeatedly crashing after starting
- **ImagePullBackOff / ErrImagePull** - Cannot pull container image
- **OOMKilled** - Container killed due to out-of-memory
- **Failed Probes** - Liveness or readiness probe failures
- **High Restart Count** - Container restarted multiple times

### Service / Network Debugging
Detects and diagnoses the following service issues:
- **Selector Mismatch** - Service selector doesn't match any pod labels
- **No Endpoints** - Service has no backing pods
- **Port Mismatch** - Service targetPort doesn't match container port
- **CoreDNS Status** - Checks if CoreDNS is running for DNS resolution

### Bulk Debugging
- Debug ALL pods in a namespace at once
- Debug ALL services in a namespace at once
- Results sorted by severity (Critical → Warning → Healthy)
- Graceful handling of individual failures

### Namespace Discovery
- List all available namespaces in the cluster

## Technology Stack

- Java 17
- Spring Boot 3.2.2
- Maven
- Official Kubernetes Java Client 21.0.2

## Quick Start

### Prerequisites

1. Java 17+
2. Maven 3.6+
3. A running Kubernetes cluster (minikube or kind)
4. kubectl configured with cluster access
5. kubeconfig at `~/.kube/config`

### Run the Application

```bash
# Clone and navigate to the project
cd KubeDiagnose

# Run with Maven
./mvnw spring-boot:run

# Or build and run JAR
./mvnw clean package -DskipTests
java -jar target/kube-diagnose-0.0.1-SNAPSHOT.jar
```

The application will automatically load your kubeconfig from `~/.kube/config`.

### Test the API

```bash
# List namespaces
curl http://localhost:8080/api/namespaces

# Debug a single pod
curl http://localhost:8080/api/debug/pod/default/my-pod

# Debug a single service
curl http://localhost:8080/api/debug/service/default/my-service

# Debug ALL pods in a namespace
curl http://localhost:8080/api/debug/pods/default

# Debug ALL services in a namespace
curl http://localhost:8080/api/debug/services/default
```

## REST API

### Single Resource Debugging

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/debug/pod/{namespace}/{podName}` | GET | Debug a specific pod |
| `/api/debug/service/{namespace}/{serviceName}` | GET | Debug a specific service |

### Bulk Debugging

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/debug/pods/{namespace}` | GET | Debug ALL pods in a namespace |
| `/api/debug/services/{namespace}` | GET | Debug ALL services in a namespace |

### Discovery

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/namespaces` | GET | List all available namespaces |

### Pod Debug API

```
GET /api/debug/pod/{namespace}/{podName}
```

Returns diagnostic information about a pod including:
- Resource name and namespace
- Status (Healthy, Warning, Critical)
- Probable causes of issues
- Evidence used for diagnosis
- Suggested remediation actions

### Service Debug API

```
GET /api/debug/service/{namespace}/{serviceName}
```

Returns diagnostic information about a service including:
- Resource name and namespace
- Service type and selector
- Endpoint information
- Probable causes of issues
- Evidence used for diagnosis
- Suggested remediation actions

## Example Response

```json
{
  "resourceName": "crashloop-pod",
  "namespace": "default",
  "status": "Critical",
  "phase": "Running",
  "probableCauses": [
    "Container 'app' is in CrashLoopBackOff"
  ],
  "evidence": [
    "Container state: Waiting, Reason: CrashLoopBackOff",
    "Restart count: 5"
  ],
  "suggestedActions": [
    "Check container logs: kubectl logs crashloop-pod -c app --previous",
    "Review application startup logic and exit codes"
  ]
}
```

## Testing with Sample Manifests

Deploy test resources to verify the diagnostic capabilities:

```bash
# Deploy a crashing pod
kubectl apply -f manifests/crashloop-pod.yaml

# Deploy a pod with bad image
kubectl apply -f manifests/imagepull-fail.yaml

# Deploy misconfigured services
kubectl apply -f manifests/bad-service.yaml

# Test the debug API
curl http://localhost:8080/api/debug/pod/default/crashloop-pod | jq
curl http://localhost:8080/api/debug/service/default/bad-selector-service | jq
```

## Project Structure

```
kube-diagnose/
├── src/main/java/com/kubediagnose/
│   ├── KubeDiagnoseApplication.java  # Main entry point
│   ├── config/                        # Kubernetes client config
│   ├── controller/                    # REST API controllers
│   ├── service/                       # Business logic
│   ├── analyzer/                      # Diagnostic analyzers
│   ├── rules/                         # Diagnostic rules
│   └── model/                         # DTOs
├── src/main/resources/
│   └── application.yml                # Configuration
├── manifests/                         # Test Kubernetes manifests
└── docs/                              # Documentation
```

## Documentation

- [Project Structure](docs/PROJECT_STRUCTURE.md) - Architecture and code organization
- [API Reference](docs/API_REFERENCE.md) - Detailed API documentation
- [Usage Guide](docs/USAGE_GUIDE.md) - How to run and test the application

## Configuration

### application.yml

```yaml
server:
  port: 8080

kubernetes:
  kubeconfig-path: ${user.home}/.kube/config
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | HTTP server port | 8080 |
| `KUBERNETES_KUBECONFIG_PATH` | Path to kubeconfig | ~/.kube/config |

## License

This project is for educational and demonstration purposes.
