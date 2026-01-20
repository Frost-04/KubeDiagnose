# KubeDiagnose

KubeDiagnose is a full-stack Kubernetes diagnostics platform that helps identify, explain, and resolve issues with Pods and Services.
It combines a Spring Boot backend that analyzes cluster state with a modern React dashboard for visual debugging.

## What It Does

### Kubernetes Diagnostics

**Detects common Pod issues:**

- CrashLoopBackOff
- ImagePullBackOff / ErrImagePull
- OOMKilled
- Failed liveness/readiness probes
- High restart counts

**Detects Service & networking issues:**

- Selector mismatch
- No endpoints
- Port mismatches
- CoreDNS availability

### Visual Dashboard

- View all Pods and Services in a namespace
- Status-based severity (Critical / Warning / Healthy)
- Detailed explanations, evidence, and fix suggestions
- Search and filter resources
- Clean, dark-mode UI

## Architecture

```
KubeDiagnose
├── backend/   # Spring Boot Kubernetes diagnostics API
└── frontend/  # React + TypeScript debugging dashboard
```

## Tech Stack

**Backend**

- Java 17
- Spring Boot 3
- Maven
- Kubernetes Java Client

**Frontend**

- React 18
- TypeScript
- Vite
- Tailwind CSS
- Axios

## Prerequisites

- Java 17+
- Maven 3.6+
- Node.js 18+
- Kubernetes cluster (minikube / kind)
- kubectl configured (~/.kube/config)

## Running the Project

### Start Backend

```bash
cd backend
./mvnw spring-boot:run
```

Backend runs on http://localhost:8080

### Start Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on http://localhost:5173

## Key API Endpoints

**List namespaces**
```
GET /api/namespaces
```

**Debug a pod**
```
GET /api/debug/pod/{namespace}/{podName}
```

**Debug a service**
```
GET /api/debug/service/{namespace}/{serviceName}
```

**Debug all pods in a namespace**
```
GET /api/debug/pods/{namespace}
```

**Debug all services in a namespace**
```
GET /api/debug/services/{namespace}
```

## Environment Configuration

**Backend**
```
SERVER_PORT=8080
KUBERNETES_KUBECONFIG_PATH=~/.kube/config
```

**Frontend (Production)**
```
VITE_API_BASE_URL=http://your-backend-url
```

## Use Case

KubeDiagnose is ideal for:

- Kubernetes learners
- DevOps engineers
- Platform teams
- Faster root-cause analysis of cluster issues
