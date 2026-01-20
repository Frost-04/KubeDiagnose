import { useState, useEffect, useCallback, useRef } from 'react';
import { Box, Network, AlertCircle, RefreshCw } from 'lucide-react';
import {
  TopBar,
  PodTile,
  PodTileSkeleton,
  ServiceTile,
  ServiceTileSkeleton,
  DetailsPanel,
} from '../components';
import {
  getNamespaces,
  getPods,
  getServices,
  getPodDetails,
  getServiceDetails,
} from '../api';
import type {
  PodDiagnosticResult,
  ServiceDiagnosticResult,
  BulkPodDiagnosticResult,
  BulkServiceDiagnosticResult,
} from '../types';

type SelectedResource =
  | { type: 'pod'; data: PodDiagnosticResult }
  | { type: 'service'; data: ServiceDiagnosticResult }
  | null;

export function Dashboard() {
  // Namespace
  const [namespaces, setNamespaces] = useState<string[]>([]);
  const [selectedNamespace, setSelectedNamespace] = useState('default');
  const [namespacesLoading, setNamespacesLoading] = useState(true);
  const [namespacesError, setNamespacesError] = useState<string | null>(null);

  // Pods
  const [podsData, setPodsData] = useState<BulkPodDiagnosticResult | null>(null);
  const [podsLoading, setPodsLoading] = useState(false);
  const [podsError, setPodsError] = useState<string | null>(null);

  // Services
  const [servicesData, setServicesData] = useState<BulkServiceDiagnosticResult | null>(null);
  const [servicesLoading, setServicesLoading] = useState(false);
  const [servicesError, setServicesError] = useState<string | null>(null);

  // Details panel
  const [selectedResource, setSelectedResource] = useState<SelectedResource>(null);
  const [detailsLoading, setDetailsLoading] = useState(false);
  const [detailsError, setDetailsError] = useState<string | null>(null);

  // Search
  const [searchLoading, setSearchLoading] = useState(false);

  // Prevent race conditions on namespace change
  const currentNamespaceRef = useRef(selectedNamespace);

  // Load namespaces
  useEffect(() => {
    async function fetchNamespaces() {
      try {
        setNamespacesLoading(true);
        setNamespacesError(null);
        const response = await getNamespaces();
        setNamespaces(response.namespaces);
        // Prefer 'default' if present
        if (response.namespaces.includes('default')) {
          setSelectedNamespace('default');
        } else if (response.namespaces.length > 0) {
          setSelectedNamespace(response.namespaces[0]);
        }
      } catch (err) {
        const error = err as { message?: string };
        setNamespacesError(error.message || 'Failed to fetch namespaces');
      } finally {
        setNamespacesLoading(false);
      }
    }
    fetchNamespaces();
  }, []);

  // Refetch when namespace changes
  useEffect(() => {
    currentNamespaceRef.current = selectedNamespace;

    async function fetchResources() {
      // Reset selection
      setSelectedResource(null);
      setDetailsError(null);

      // Pods
      setPodsLoading(true);
      setPodsError(null);
      try {
        const pods = await getPods(selectedNamespace);
        // Only if still current
        if (currentNamespaceRef.current === selectedNamespace) {
          setPodsData(pods);
        }
      } catch (err) {
        const error = err as { message?: string };
        if (currentNamespaceRef.current === selectedNamespace) {
          setPodsError(error.message || 'Failed to fetch pods');
          setPodsData(null);
        }
      } finally {
        if (currentNamespaceRef.current === selectedNamespace) {
          setPodsLoading(false);
        }
      }

      // Services
      setServicesLoading(true);
      setServicesError(null);
      try {
        const services = await getServices(selectedNamespace);
        // Only if still current
        if (currentNamespaceRef.current === selectedNamespace) {
          setServicesData(services);
        }
      } catch (err) {
        const error = err as { message?: string };
        if (currentNamespaceRef.current === selectedNamespace) {
          setServicesError(error.message || 'Failed to fetch services');
          setServicesData(null);
        }
      } finally {
        if (currentNamespaceRef.current === selectedNamespace) {
          setServicesLoading(false);
        }
      }
    }

    if (selectedNamespace) {
      fetchResources();
    }
  }, [selectedNamespace]);

  // Namespace change
  const handleNamespaceChange = useCallback((namespace: string) => {
    setSelectedNamespace(namespace);
  }, []);

  // Select pod
  const handlePodClick = useCallback(
    async (pod: PodDiagnosticResult) => {
      setDetailsLoading(true);
      setDetailsError(null);
      try {
        const details = await getPodDetails(pod.namespace, pod.resourceName);
        setSelectedResource({ type: 'pod', data: details });
      } catch (err) {
        const error = err as { message?: string };
        setDetailsError(error.message || 'Failed to fetch pod details');
        setSelectedResource(null);
      } finally {
        setDetailsLoading(false);
      }
    },
    []
  );

  // Select service
  const handleServiceClick = useCallback(
    async (service: ServiceDiagnosticResult) => {
      setDetailsLoading(true);
      setDetailsError(null);
      try {
        const details = await getServiceDetails(service.namespace, service.resourceName);
        setSelectedResource({ type: 'service', data: details });
      } catch (err) {
        const error = err as { message?: string };
        setDetailsError(error.message || 'Failed to fetch service details');
        setSelectedResource(null);
      } finally {
        setDetailsLoading(false);
      }
    },
    []
  );

  // Global search
  const handleSearch = useCallback(
    async (type: 'pod' | 'service', namespace: string, name: string) => {
      setSearchLoading(true);
      setDetailsLoading(true);
      setDetailsError(null);
      try {
        if (type === 'pod') {
          const details = await getPodDetails(namespace, name);
          setSelectedResource({ type: 'pod', data: details });
        } else {
          const details = await getServiceDetails(namespace, name);
          setSelectedResource({ type: 'service', data: details });
        }
      } catch (err) {
        const error = err as { message?: string };
        setDetailsError(error.message || `Failed to fetch ${type} details`);
        setSelectedResource(null);
      } finally {
        setSearchLoading(false);
        setDetailsLoading(false);
      }
    },
    []
  );

  // Close details
  const handleDetailsClose = useCallback(() => {
    setSelectedResource(null);
    setDetailsError(null);
  }, []);

  // Manual refresh
  const handleRefresh = useCallback(() => {
    // Trigger re-fetch by setting the same namespace
    setSelectedNamespace((prev) => prev);
  }, []);

  return (
    <div className="h-screen flex flex-col bg-background">
      {/* Top bar */}
      <TopBar
        namespaces={namespaces}
        selectedNamespace={selectedNamespace}
        onNamespaceChange={handleNamespaceChange}
        onSearch={handleSearch}
        isNamespacesLoading={namespacesLoading}
        isSearchLoading={searchLoading}
        namespacesError={namespacesError}
      />

      {/* Main */}
      <main className="flex-1 flex overflow-hidden">
        {/* Pods (left) */}
        <aside className="w-80 border-r border-border flex flex-col shrink-0">
          <PanelHeader
            title="Pods"
            icon={<Box className="w-4 h-4 text-blue-400" />}
            count={podsData?.totalPods ?? 0}
            criticalCount={podsData?.criticalCount ?? 0}
            warningCount={podsData?.warningCount ?? 0}
            healthyCount={podsData?.healthyCount ?? 0}
            onRefresh={handleRefresh}
            isLoading={podsLoading}
          />
          <div className="flex-1 overflow-y-auto p-3 space-y-2">
            {podsLoading ? (
              // Loading
              Array.from({ length: 5 }).map((_, i) => <PodTileSkeleton key={i} />)
            ) : podsError ? (
              // Error
              <ErrorState message={podsError} />
            ) : podsData?.results.length === 0 ? (
              // Empty
              <EmptyState message="No pods found in this namespace" />
            ) : (
              // List
              podsData?.results.map((pod) => (
                <PodTile
                  key={pod.resourceName}
                  pod={pod}
                  isSelected={
                    selectedResource?.type === 'pod' &&
                    selectedResource.data.resourceName === pod.resourceName
                  }
                  onClick={() => handlePodClick(pod)}
                />
              ))
            )}
          </div>
        </aside>

        {/* Details (center) */}
        <section className="flex-1 bg-card/50 border-r border-border overflow-hidden">
          <DetailsPanel
            selected={selectedResource}
            isLoading={detailsLoading}
            error={detailsError}
            onClose={handleDetailsClose}
          />
        </section>

        {/* Services (right) */}
        <aside className="w-80 flex flex-col shrink-0">
          <PanelHeader
            title="Services"
            icon={<Network className="w-4 h-4 text-purple-400" />}
            count={servicesData?.totalServices ?? 0}
            criticalCount={servicesData?.criticalCount ?? 0}
            warningCount={servicesData?.warningCount ?? 0}
            healthyCount={servicesData?.healthyCount ?? 0}
            onRefresh={handleRefresh}
            isLoading={servicesLoading}
          />
          <div className="flex-1 overflow-y-auto p-3 space-y-2">
            {servicesLoading ? (
              // Loading
              Array.from({ length: 5 }).map((_, i) => <ServiceTileSkeleton key={i} />)
            ) : servicesError ? (
              // Error
              <ErrorState message={servicesError} />
            ) : servicesData?.results.length === 0 ? (
              // Empty
              <EmptyState message="No services found in this namespace" />
            ) : (
              // List
              servicesData?.results.map((service) => (
                <ServiceTile
                  key={service.resourceName}
                  service={service}
                  isSelected={
                    selectedResource?.type === 'service' &&
                    selectedResource.data.resourceName === service.resourceName
                  }
                  onClick={() => handleServiceClick(service)}
                />
              ))
            )}
          </div>
        </aside>
      </main>
    </div>
  );
}

// Panel header with counts
function PanelHeader({
  title,
  icon,
  count,
  criticalCount,
  warningCount,
  healthyCount,
  onRefresh,
  isLoading,
}: {
  title: string;
  icon: React.ReactNode;
  count: number;
  criticalCount: number;
  warningCount: number;
  healthyCount: number;
  onRefresh: () => void;
  isLoading: boolean;
}) {
  return (
    <div className="px-4 py-3 border-b border-border shrink-0">
      <div className="flex items-center justify-between mb-2">
        <div className="flex items-center gap-2">
          {icon}
          <h2 className="text-sm font-medium text-gray-200">{title}</h2>
          <span className="text-xs text-gray-500">({count})</span>
        </div>
        <button
          onClick={onRefresh}
          disabled={isLoading}
          className={`p-1 rounded text-gray-500 hover:text-gray-300 hover:bg-gray-700 transition-colors ${isLoading ? 'animate-spin' : ''}`}
        >
          <RefreshCw className="w-4 h-4" />
        </button>
      </div>
      {count > 0 && (
        <div className="flex items-center gap-3 text-xs">
          {criticalCount > 0 && (
            <span className="text-critical">{criticalCount} critical</span>
          )}
          {warningCount > 0 && (
            <span className="text-warning">{warningCount} warning</span>
          )}
          {healthyCount > 0 && (
            <span className="text-healthy">{healthyCount} healthy</span>
          )}
        </div>
      )}
    </div>
  );
}

// Error state component
function ErrorState({ message }: { message: string }) {
  return (
    <div className="flex flex-col items-center justify-center py-8 text-center">
      <AlertCircle className="w-8 h-8 text-critical mb-3" />
      <p className="text-sm text-gray-400">{message}</p>
    </div>
  );
}

// Empty state component
function EmptyState({ message }: { message: string }) {
  return (
    <div className="flex flex-col items-center justify-center py-8 text-center">
      <p className="text-sm text-gray-500">{message}</p>
    </div>
  );
}
