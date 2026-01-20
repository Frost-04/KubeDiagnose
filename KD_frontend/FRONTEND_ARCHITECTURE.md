# Frontend Architecture

> KubeDiagnose React/TypeScript frontend - A dark-mode Kubernetes debugging dashboard

## Overview

The frontend is a single-page application (SPA) built with React 18, TypeScript, and Tailwind CSS. It provides a real-time visual interface for debugging Kubernetes Pods and Services by connecting to the KubeDiagnose backend API.

**Tech Stack:**
- React 18 (UI framework)
- TypeScript (type safety)
- Vite (dev server + bundler)
- Tailwind CSS (styling)
- Axios (HTTP client)
- React Router (navigation)
- Lucide React (icons)

---

## Project Structure

```
src/
├── api/                    # Backend communication layer
│   ├── client.ts          # Axios instance with error handling
│   ├── namespaces.ts      # GET /api/namespaces
│   ├── pods.ts            # GET /api/debug/pods/* 
│   ├── services.ts        # GET /api/debug/services/*
│   └── index.ts           # Barrel exports
│
├── components/            # Reusable UI components
│   ├── TopBar/           # Header with search + namespace selector
│   ├── NamespaceDropdown/ # Searchable namespace picker
│   ├── SearchBar/        # Global pod/service search
│   ├── StatusBadge/      # Status indicator (Critical/Warning/Healthy)
│   ├── PodTile/          # Pod list item + skeleton
│   ├── ServiceTile/      # Service list item + skeleton
│   ├── PodDetails/       # Full pod diagnostic view
│   ├── ServiceDetails/   # Full service diagnostic view
│   └── DetailsPanel/     # Container for detail views
│
├── pages/                # Page-level components
│   └── Dashboard.tsx     # Main dashboard (3-column layout)
│
├── types/                # TypeScript definitions
│   ├── namespace.ts      # NamespaceListResponse
│   ├── pod.ts            # Pod types (PodDiagnosticResult, etc.)
│   ├── service.ts        # Service types (ServiceDiagnosticResult, etc.)
│   └── index.ts          # Barrel exports + ApiError
│
├── utils/                # Helper functions
│   └── index.ts          # formatDate, truncate, cn
│
├── App.tsx               # Router setup (BrowserRouter)
├── main.tsx              # React entry point
└── index.css             # Global styles + Tailwind directives
```

---

## Application Flow

### 1. Entry Point

**`main.tsx`** → Renders `<App />` into `#root`

**`App.tsx`** → Sets up React Router:
- Route `/` → `<Dashboard />`
- Route `*` → Redirect to `/`

---

### 2. Dashboard Layout (Main Page)

**`pages/Dashboard.tsx`** is the core component with a **3-column layout**:

```
┌─────────────────────────────────────────────────────────────┐
│  TopBar (Search + Namespace Selector)                       │
├──────────────┬──────────────────────────┬────────────────────┤
│  Pods List   │   Details Panel          │   Services List    │
│  (Left)      │   (Center)               │   (Right)          │
│              │                          │                    │
│  PodTile     │   PodDetails             │   ServiceTile      │
│  PodTile     │      OR                  │   ServiceTile      │
│  PodTile     │   ServiceDetails         │   ServiceTile      │
│  ...         │      OR                  │   ...              │
│              │   Empty State            │                    │
└──────────────┴──────────────────────────┴────────────────────┘
```

---

### 3. Data Flow

#### **On Mount:**

1. **Fetch Namespaces** (`useEffect`)
   - Call `getNamespaces()` → `GET /api/namespaces`
   - Store in state: `namespaces[]`
   - Set `selectedNamespace` to `"default"` (if exists) or first available

#### **On Namespace Change:**

2. **Fetch Pods + Services** (`useEffect` triggered by `selectedNamespace`)
   - Call `getPods(namespace)` → `GET /api/debug/pods/{namespace}`
   - Call `getServices(namespace)` → `GET /api/debug/services/{namespace}`
   - Store results in `podsData` and `servicesData`
   - Both requests run in parallel
   - Use `currentNamespaceRef` to prevent race conditions (only update state if namespace hasn't changed during async call)

#### **On Pod/Service Click:**

3. **Fetch Details**
   - User clicks `PodTile` → `handlePodClick(pod)`
     - Call `getPodDetails(namespace, podName)` → `GET /api/debug/pod/{ns}/{name}`
     - Store in `selectedResource: { type: 'pod', data: PodDiagnosticResult }`
   - User clicks `ServiceTile` → `handleServiceClick(service)`
     - Call `getServiceDetails(namespace, serviceName)` → `GET /api/debug/service/{ns}/{name}`
     - Store in `selectedResource: { type: 'service', data: ServiceDiagnosticResult }`

#### **On Global Search:**

4. **Manual Debug**
   - User types in `SearchBar` (pod/service toggle + namespace + name)
   - Submits → `handleSearch(type, namespace, name)`
   - Fetches single resource details (bypasses list)
   - Opens in `DetailsPanel`

---

## Component Hierarchy

### **TopBar**
- `NamespaceDropdown` - Dropdown with search filter
- `SearchBar` - Resource type toggle + namespace + name inputs

### **Dashboard → Pods Panel (Left)**
- `PanelHeader` - Title + counts (critical/warning/healthy) + refresh button
- `PodTile[]` - List of pods sorted by severity
- `PodTileSkeleton` - Loading state (5 skeletons)
- Error/Empty states

### **Dashboard → Details Panel (Center)**
- `DetailsPanel` - Container with loading/error/empty states
  - `PodDetails` - Pod diagnostic view:
    - Container table (status, restarts, ready)
    - Probable causes (bullets)
    - Evidence (monospace logs)
    - Suggested actions (copyable commands)
  - `ServiceDetails` - Service diagnostic view:
    - Selector labels (key:value chips)
    - Ports table
    - Endpoint info (ready/not ready counts)
    - CoreDNS status
    - Causes/Evidence/Actions

### **Dashboard → Services Panel (Right)**
- `PanelHeader` - Title + counts + refresh button
- `ServiceTile[]` - List of services sorted by severity
- `ServiceTileSkeleton` - Loading state (5 skeletons)
- Error/Empty states

---

## State Management

All state is managed in **`Dashboard.tsx`** using React hooks:

```typescript
// Namespace
const [namespaces, setNamespaces] = useState<string[]>([])
const [selectedNamespace, setSelectedNamespace] = useState('default')
const [namespacesLoading, setNamespacesLoading] = useState(true)
const [namespacesError, setNamespacesError] = useState<string | null>(null)

// Pods
const [podsData, setPodsData] = useState<BulkPodDiagnosticResult | null>(null)
const [podsLoading, setPodsLoading] = useState(false)
const [podsError, setPodsError] = useState<string | null>(null)

// Services
const [servicesData, setServicesData] = useState<BulkServiceDiagnosticResult | null>(null)
const [servicesLoading, setServicesLoading] = useState(false)
const [servicesError, setServicesError] = useState<string | null>(null)

// Details panel
const [selectedResource, setSelectedResource] = useState<SelectedResource>(null)
const [detailsLoading, setDetailsLoading] = useState(false)
const [detailsError, setDetailsError] = useState<string | null>(null)

// Search
const [searchLoading, setSearchLoading] = useState(false)

// Race condition prevention
const currentNamespaceRef = useRef(selectedNamespace)
```

---

## Key Interactions

### **1. Namespace Selection**
- User clicks namespace dropdown
- Searches/filters namespaces
- Selects new namespace
- **Effect:** Dashboard clears selection, refetches pods + services

### **2. Pod Selection**
- User clicks a `PodTile`
- Dashboard calls `getPodDetails()`
- **Result:** `DetailsPanel` shows full pod diagnostics

### **3. Service Selection**
- User clicks a `ServiceTile`
- Dashboard calls `getServiceDetails()`
- **Result:** `DetailsPanel` shows full service diagnostics

### **4. Global Search**
- User toggles Pod/Service in `SearchBar`
- Enters namespace + resource name
- Presses Enter or clicks search icon
- Dashboard fetches details directly
- **Result:** Opens in `DetailsPanel` (overrides current selection)

### **5. Manual Refresh**
- User clicks refresh icon in `PanelHeader`
- Dashboard triggers re-fetch by setting `selectedNamespace` to itself
- **Effect:** Pods + services reload

### **6. Copy Command**
- User views suggested actions in detail view
- Clicks copy icon next to kubectl command
- **Effect:** Command copied to clipboard (2s confirmation)

---

## API Layer

### **client.ts** - Axios Configuration
- Base URL: `import.meta.env.VITE_API_BASE_URL || '/api'`
- Timeout: 30s
- Error interceptor: Normalizes API errors into consistent format
- Handles: Network errors, timeouts, API error payloads

### **API Functions**
| Function | Endpoint | Returns |
|----------|----------|---------|
| `getNamespaces()` | `GET /api/namespaces` | `NamespaceListResponse` |
| `getPods(namespace)` | `GET /api/debug/pods/{ns}` | `BulkPodDiagnosticResult` |
| `getPodDetails(ns, name)` | `GET /api/debug/pod/{ns}/{name}` | `PodDiagnosticResult` |
| `getServices(namespace)` | `GET /api/debug/services/{ns}` | `BulkServiceDiagnosticResult` |
| `getServiceDetails(ns, name)` | `GET /api/debug/service/{ns}/{name}` | `ServiceDiagnosticResult` |

---

## Type System

All API response shapes are strictly typed based on backend docs:

- **Pod Types** (`types/pod.ts`):
  - `PodDiagnosticResult` - Single pod
  - `BulkPodDiagnosticResult` - All pods in namespace
  - `ContainerStatus` - Container details
  - `PodStatus` = `'Healthy' | 'Warning' | 'Critical' | 'Unknown' | 'Completed'`
  - `PodPhase` = `'Pending' | 'Running' | 'Succeeded' | 'Failed' | 'Unknown'`

- **Service Types** (`types/service.ts`):
  - `ServiceDiagnosticResult` - Single service
  - `BulkServiceDiagnosticResult` - All services in namespace
  - `ServicePort` - Port config
  - `EndpointInfo` - Ready/not-ready counts + addresses
  - `ServiceStatus` = `'Healthy' | 'Warning' | 'Critical' | 'Unknown'`
  - `ServiceType` = `'ClusterIP' | 'NodePort' | 'LoadBalancer'`

- **Namespace Types** (`types/namespace.ts`):
  - `NamespaceListResponse` - List of namespace names

---

## Styling System

### **Tailwind Config** (`tailwind.config.js`)
Custom dark mode color palette:
```js
colors: {
  background: '#0b0f14',  // Near-black background
  card: '#111827',        // Card background
  border: '#1f2937',      // Subtle borders
  
  // Status colors (with bg + border variants)
  critical: '#ef4444',    // Red
  warning: '#f59e0b',     // Amber
  healthy: '#22c55e',     // Green
}
```

### **Global Styles** (`index.css`)
- Custom scrollbar (dark theme)
- Focus-visible for keyboard navigation
- Selection highlight (blue)

---

## Design Patterns

### **1. Skeleton Loading**
- Use skeleton components during data fetch
- Maintain layout stability (no layout shift)
- Example: `PodTileSkeleton`, `ServiceTileSkeleton`, `DetailsSkeleton`

### **2. Error Boundaries**
- Display user-friendly error messages
- Show backend error message when available
- Fallback: "Failed to fetch..." messages

### **3. Empty States**
- Guide user when no data exists
- Examples:
  - "No pods found in this namespace"
  - "No resource selected - Click a Pod or Service..."

### **4. Race Condition Prevention**
- Use `useRef` to track current namespace
- Only update state if namespace hasn't changed during async request
- Prevents stale data from appearing after rapid namespace switches

### **5. Keyboard Accessibility**
- Escape key closes dropdowns
- Enter key submits search
- Focus management (auto-focus search inputs)
- Proper focus-visible styling

---

## Performance Optimizations

1. **Parallel API Calls**: Pods + Services fetched simultaneously
2. **useCallback**: Memoize event handlers to prevent re-renders
3. **Conditional Rendering**: Only render detail view when selected
4. **CSS Transitions**: Use Tailwind's `transition-*` utilities (no JS animations)
5. **Code Splitting**: React Router lazy-loads pages (if expanded)

---

## Development Workflow

### **Dev Server**
```bash
npm run dev
```
- Starts Vite dev server on port 5173
- Proxies `/api` requests to `http://localhost:8080`
- Hot Module Replacement (HMR) enabled

### **Build**
```bash
npm run build
```
- TypeScript compilation (`tsc -b`)
- Vite production build → `dist/`

### **Preview**
```bash
npm run preview
```
- Preview production build locally

---

## Environment Configuration

**.env Variables:**
- `VITE_API_BASE_URL` - Backend API URL (defaults to `/api` for dev proxy)

**vite.config.ts Proxy:**
```ts
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true,
  },
}
```

---

## User Journey Example

1. **User opens dashboard** → Sees "default" namespace pods + services
2. **Clicks "crashloop-pod"** → Details panel shows CrashLoopBackOff diagnosis
3. **Reads "Suggested Actions"** → Copies `kubectl logs ...` command
4. **Switches to "kube-system" namespace** → Sees different pods/services
5. **Uses search bar** → Types "coredns" + "kube-system" → Views CoreDNS pod details
6. **Clicks service tile** → Views service selector + endpoint info
7. **Clicks close (X)** → Returns to empty state in details panel

---

## Future Enhancements (Ideas)

- [ ] Add filtering (show only Critical/Warning)
- [ ] Add sorting options (name, status, restart count)
- [ ] Real-time updates (WebSocket connection)
- [ ] Multi-namespace view
- [ ] Export diagnostic reports (JSON/PDF)
- [ ] Dark/light mode toggle
- [ ] Collapse/expand side panels
- [ ] Command history in search bar
- [ ] Persistent state (localStorage for last namespace)

---

## Summary

The frontend is a **data-driven, reactive UI** that mirrors the backend API structure. It follows React best practices with TypeScript for type safety, uses functional components with hooks for state management, and maintains a clean separation between presentation (components) and data (API layer). The 3-column layout provides instant visual feedback on cluster health while allowing deep-dive diagnostics on-demand.
