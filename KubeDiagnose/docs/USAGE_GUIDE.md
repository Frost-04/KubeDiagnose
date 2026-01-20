# Usage Guide

## Prerequisites

Before running KubeDiagnose, ensure you have the following installed:

### Required

1. **Java 17 or later**
   ```bash
   java -version
   ```

2. **Maven 3.6+** (or use the included Maven wrapper)
   ```bash
   mvn -version
   ```

3. **A running Kubernetes cluster** (minikube or kind)
   ```bash
   # For minikube
   minikube status
   
   # For kind
   kind get clusters
   ```

4. **kubectl configured** with access to your cluster
   ```bash
   kubectl cluster-info
   ```

5. **Kubeconfig file** at `~/.kube/config`
   ```bash
   # Verify kubeconfig exists
   cat ~/.kube/config
   ```

### Setting Up a Local Cluster

#### Option 1: Minikube

```bash
# Install minikube (if not installed)
# See: https://minikube.sigs.k8s.io/docs/start/

# Start minikube
minikube start

# Verify cluster is running
kubectl get nodes
```

#### Option 2: Kind (Kubernetes in Docker)

```bash
# Install kind (if not installed)
# See: https://kind.sigs.k8s.io/docs/user/quick-start/

# Create a cluster
kind create cluster --name kubediagnose-test

# Verify cluster is running
kubectl get nodes
```

---

## Running the Application

### Option 1: Using Maven

```bash
# Navigate to project directory
cd KubeDiagnose

# Build and run
./mvnw spring-boot:run

# On Windows
mvnw.cmd spring-boot:run
```

### Option 2: Using IDE

1. Import the project as a Maven project
2. Run `KubeDiagnoseApplication.java` as a Java application
3. The application will automatically load kubeconfig from `~/.kube/config`

### Option 3: Build and Run JAR

```bash
# Build the JAR
./mvnw clean package -DskipTests

# Run the JAR
java -jar target/kube-diagnose-0.0.1-SNAPSHOT.jar
```

### Verify Application is Running

```bash
# The application starts on port 8080 by default
# Try a debug request for an existing pod
curl http://localhost:8080/api/debug/pod/default/crashloop-pod
```

```powershell
# Windows PowerShell
curl.exe http://localhost:8080/api/debug/pod/default/crashloop-pod

# Or using Invoke-RestMethod for formatted output
Invoke-RestMethod -Uri http://localhost:8080/api/debug/pod/default/crashloop-pod | ConvertTo-Json -Depth 10
```

> **Note:** A 404 error means the pod doesn't exist. Deploy the test manifests first (see below).

---

## Testing with Example Scenarios

### Deploy Test Manifests

First, deploy the test resources to your cluster:

```bash
# Deploy a pod that will crash (CrashLoopBackOff)
kubectl apply -f manifests/crashloop-pod.yaml

# Deploy a pod with bad image (ImagePullBackOff)
kubectl apply -f manifests/imagepull-fail.yaml

# Deploy services with misconfigurations
kubectl apply -f manifests/bad-service.yaml
```

### Wait for Issues to Manifest

```bash
# Check pod status (wait for CrashLoopBackOff)
kubectl get pods -w

# You should see something like:
# NAME                  READY   STATUS             RESTARTS   AGE
# crashloop-pod         0/1     CrashLoopBackOff   3          1m
# imagepull-fail-pod    0/1     ImagePullBackOff   0          1m
# backend-pod           1/1     Running            0          1m
```

---

## Example Debugging Scenarios

### Scenario 1: Debug a CrashLoopBackOff Pod

```bash
# Using curl (Linux/Mac)
curl -s http://localhost:8080/api/debug/pod/default/crashloop-pod | jq
```

```powershell
# Using PowerShell (Windows)
Invoke-RestMethod -Uri http://localhost:8080/api/debug/pod/default/crashloop-pod | ConvertTo-Json -Depth 10

# Or using curl in PowerShell
curl.exe http://localhost:8080/api/debug/pod/default/crashloop-pod
```

```
# Using Postman
# GET http://localhost:8080/api/debug/pod/default/crashloop-pod
```

**Expected Response:**
```json
{
  "resourceName": "crashloop-pod",
  "namespace": "default",
  "status": "Critical",
  "phase": "Running",
  "probableCauses": [
    "Container 'crashloop-container' is in CrashLoopBackOff"
  ],
  "evidence": [
    "Container state: Waiting, Reason: CrashLoopBackOff",
    "Restart count: 5"
  ],
  "suggestedActions": [
    "Check container logs: kubectl logs crashloop-pod -c crashloop-container --previous",
    "Review application startup logic and exit codes"
  ]
}
```

### Scenario 2: Debug an ImagePullBackOff Pod

```bash
# Linux/Mac
curl -s http://localhost:8080/api/debug/pod/default/imagepull-fail-pod | jq
```

```powershell
# Windows PowerShell
Invoke-RestMethod -Uri http://localhost:8080/api/debug/pod/default/imagepull-fail-pod | ConvertTo-Json -Depth 10
```

**Expected Response:**
```json
{
  "resourceName": "imagepull-fail-pod",
  "namespace": "default",
  "status": "Critical",
  "probableCauses": [
    "Container 'bad-image-container' cannot pull image: ImagePullBackOff"
  ],
  "evidence": [
    "Container state: Waiting, Reason: ImagePullBackOff",
    "Image: non-existent-registry.io/fake-image:v999.999.999"
  ],
  "suggestedActions": [
    "Verify the image name and tag are correct",
    "Check if the image exists in the registry",
    "Ensure image pull secrets are configured if using private registry"
  ]
}
```

### Scenario 3: Debug a Service with Selector Mismatch

```bash
# Linux/Mac
curl -s http://localhost:8080/api/debug/service/default/bad-selector-service | jq
```

```powershell
# Windows PowerShell
Invoke-RestMethod -Uri http://localhost:8080/api/debug/service/default/bad-selector-service | ConvertTo-Json -Depth 10
```

**Expected Response:**
```json
{
  "resourceName": "bad-selector-service",
  "namespace": "default",
  "status": "Critical",
  "serviceType": "ClusterIP",
  "selector": {
    "app": "frontend"
  },
  "probableCauses": [
    "Service selector does not match any pods",
    "Service has no endpoints"
  ],
  "suggestedActions": [
    "Verify the service selector labels match pod labels",
    "Use 'kubectl get pods --show-labels' to see pod labels"
  ]
}
```

### Scenario 4: Debug a Service with Port Mismatch

```bash
# Linux/Mac
curl -s http://localhost:8080/api/debug/service/default/bad-port-service | jq
```

```powershell
# Windows PowerShell
Invoke-RestMethod -Uri http://localhost:8080/api/debug/service/default/bad-port-service | ConvertTo-Json -Depth 10
```

**Expected Response:**
```json
{
  "resourceName": "bad-port-service",
  "namespace": "default",
  "status": "Warning",
  "probableCauses": [
    "Service targetPort 9999 may not match any container port"
  ],
  "evidence": [
    "Service port 80 -> targetPort 9999",
    "Container ports found: [80]"
  ],
  "suggestedActions": [
    "Verify service targetPort matches container port",
    "Update service targetPort to match actual container port"
  ]
}
```

### Scenario 5: Debug a Service with No Endpoints

```bash
# Linux/Mac
curl -s http://localhost:8080/api/debug/service/default/no-endpoints-service | jq
```

```powershell
# Windows PowerShell
Invoke-RestMethod -Uri http://localhost:8080/api/debug/service/default/no-endpoints-service | ConvertTo-Json -Depth 10
```

### Scenario 6: Debug a Healthy Pod

```bash
# Linux/Mac
curl -s http://localhost:8080/api/debug/pod/default/backend-pod | jq
```

```powershell
# Windows PowerShell
Invoke-RestMethod -Uri http://localhost:8080/api/debug/pod/default/backend-pod | ConvertTo-Json -Depth 10
```

**Expected Response:**
```json
{
  "resourceName": "backend-pod",
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
  ]
}
```

---

## Debugging Entire Namespaces

KubeDiagnose supports bulk debugging of all resources in a namespace. This is useful for getting a quick health overview or identifying all issues at once.

### List Available Namespaces

First, discover which namespaces are available:

```bash
# Linux/Mac
curl -s http://localhost:8080/api/namespaces | jq
```

```powershell
# Windows PowerShell
Invoke-RestMethod -Uri http://localhost:8080/api/namespaces | ConvertTo-Json -Depth 5
```

**Expected Response:**
```json
{
  "total": 5,
  "namespaces": [
    "default",
    "kube-node-lease",
    "kube-public",
    "kube-system",
    "local-path-storage"
  ]
}
```

### Debug All Pods in a Namespace

Analyze all pods in a namespace at once:

```bash
# Linux/Mac
curl -s http://localhost:8080/api/debug/pods/default | jq
```

```powershell
# Windows PowerShell
Invoke-RestMethod -Uri http://localhost:8080/api/debug/pods/default | ConvertTo-Json -Depth 10
```

**Expected Response:**
```json
{
  "summary": {
    "diagnosticTime": "2026-01-21T10:30:00.000Z",
    "resourceType": "Pods (Bulk)",
    "overallHealth": "Critical",
    "message": "Namespace 'default': 3 pods analyzed - 2 critical, 0 warning, 1 healthy."
  },
  "namespace": "default",
  "totalPods": 3,
  "criticalCount": 2,
  "warningCount": 0,
  "healthyCount": 1,
  "results": [
    {
      "resourceName": "crashloop-pod",
      "status": "Critical",
      "probableCauses": ["Container is in CrashLoopBackOff"]
    },
    {
      "resourceName": "imagepull-fail-pod",
      "status": "Critical",
      "probableCauses": ["Cannot pull image: ImagePullBackOff"]
    },
    {
      "resourceName": "backend-pod",
      "status": "Healthy",
      "probableCauses": ["No issues detected"]
    }
  ]
}
```

> **Note:** Results are sorted by severity - Critical pods appear first, then Warning, then Healthy.

### Debug All Services in a Namespace

Analyze all services in a namespace at once:

```bash
# Linux/Mac
curl -s http://localhost:8080/api/debug/services/default | jq
```

```powershell
# Windows PowerShell
Invoke-RestMethod -Uri http://localhost:8080/api/debug/services/default | ConvertTo-Json -Depth 10
```

**Expected Response:**
```json
{
  "summary": {
    "diagnosticTime": "2026-01-21T10:30:00.000Z",
    "resourceType": "Services (Bulk)",
    "overallHealth": "Critical",
    "message": "Namespace 'default': 4 services analyzed - 2 critical, 1 warning, 1 healthy."
  },
  "namespace": "default",
  "totalServices": 4,
  "criticalCount": 2,
  "warningCount": 1,
  "healthyCount": 1,
  "results": [
    {
      "resourceName": "bad-selector-service",
      "status": "Critical",
      "probableCauses": ["Service selector does not match any pods"]
    },
    {
      "resourceName": "no-endpoints-service",
      "status": "Critical",
      "probableCauses": ["Service has no endpoints"]
    },
    {
      "resourceName": "bad-port-service",
      "status": "Warning",
      "probableCauses": ["Port mismatch detected"]
    },
    {
      "resourceName": "kubernetes",
      "status": "Healthy",
      "probableCauses": ["No issues detected"]
    }
  ]
}
```

### Quick Health Check Workflow

A typical workflow for debugging a namespace:

```powershell
# 1. List namespaces to find what's available
Invoke-RestMethod -Uri http://localhost:8080/api/namespaces

# 2. Get overview of all pods in a namespace
Invoke-RestMethod -Uri http://localhost:8080/api/debug/pods/default | ConvertTo-Json -Depth 3

# 3. Get overview of all services in a namespace
Invoke-RestMethod -Uri http://localhost:8080/api/debug/services/default | ConvertTo-Json -Depth 3

# 4. Deep-dive into specific problematic resource
Invoke-RestMethod -Uri http://localhost:8080/api/debug/pod/default/crashloop-pod | ConvertTo-Json -Depth 10
```

---

## Using with Postman

### Import Collection

1. Open Postman
2. Create a new collection named "KubeDiagnose"
3. Add the following requests:

**Request 1: List Namespaces**
- Method: GET
- URL: `http://localhost:8080/api/namespaces`

**Request 2: Debug Single Pod**
- Method: GET
- URL: `http://localhost:8080/api/debug/pod/{{namespace}}/{{podName}}`
- Variables:
  - `namespace`: default
  - `podName`: crashloop-pod

**Request 3: Debug Single Service**
- Method: GET
- URL: `http://localhost:8080/api/debug/service/{{namespace}}/{{serviceName}}`
- Variables:
  - `namespace`: default
  - `serviceName`: bad-selector-service

**Request 4: Debug All Pods in Namespace**
- Method: GET
- URL: `http://localhost:8080/api/debug/pods/{{namespace}}`
- Variables:
  - `namespace`: default

**Request 5: Debug All Services in Namespace**
- Method: GET
- URL: `http://localhost:8080/api/debug/services/{{namespace}}`
- Variables:
  - `namespace`: default

---

## Clean Up Test Resources

```bash
# Remove test pods
kubectl delete -f manifests/crashloop-pod.yaml
kubectl delete -f manifests/imagepull-fail.yaml
kubectl delete -f manifests/bad-service.yaml

# Or delete all at once
kubectl delete pod crashloop-pod imagepull-fail-pod backend-pod
kubectl delete service bad-selector-service bad-port-service no-endpoints-service
```

---

## Troubleshooting

### Application Won't Start

**Error: "Could not load kubeconfig"**
- Ensure `~/.kube/config` exists and is valid
- Run `kubectl cluster-info` to verify cluster access

**Error: "Connection refused"**
- Ensure your Kubernetes cluster is running
- For minikube: `minikube status`
- For kind: `docker ps` (cluster runs in Docker)

### API Returns 403 Forbidden

- Check if your kubeconfig has proper permissions
- Ensure the service account has access to pods and services

### API Returns 404 Not Found

- Verify the pod/service name and namespace are correct
- Run `kubectl get pods -n <namespace>` to list available pods
- Run `kubectl get services -n <namespace>` to list available services

### Cluster Connection Issues

```bash
# Reset kubeconfig context
kubectl config use-context <your-context>

# For minikube
minikube update-context

# For kind
kind export kubeconfig --name <cluster-name>
```

---

## Configuration Options

### Change Server Port

Edit `src/main/resources/application.yml`:
```yaml
server:
  port: 9090  # Change from default 8080
```

Or use environment variable:
```bash
java -jar target/kube-diagnose-0.0.1-SNAPSHOT.jar --server.port=9090
```

### Custom Kubeconfig Path

Edit `src/main/resources/application.yml`:
```yaml
kubernetes:
  kubeconfig-path: /path/to/custom/kubeconfig
```

Or use environment variable:
```bash
java -jar target/kube-diagnose-0.0.1-SNAPSHOT.jar --kubernetes.kubeconfig-path=/custom/path
```
