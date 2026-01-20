import { X, Box, Network, AlertCircle } from 'lucide-react';
import { PodDetails } from '../PodDetails';
import { ServiceDetails } from '../ServiceDetails';
import type { PodDiagnosticResult, ServiceDiagnosticResult } from '../../types';

type SelectedResource =
  | { type: 'pod'; data: PodDiagnosticResult }
  | { type: 'service'; data: ServiceDiagnosticResult }
  | null;

interface DetailsPanelProps {
  selected: SelectedResource;
  isLoading: boolean;
  error: string | null;
  onClose: () => void;
}

export function DetailsPanel({ selected, isLoading, error, onClose }: DetailsPanelProps) {
  // Loading
  if (isLoading) {
    return (
      <div className="h-full flex flex-col">
        <PanelHeader title="Loading..." onClose={onClose} />
        <div className="flex-1 p-6">
          <DetailsSkeleton />
        </div>
      </div>
    );
  }

  // Error
  if (error) {
    return (
      <div className="h-full flex flex-col">
        <PanelHeader title="Error" onClose={onClose} />
        <div className="flex-1 flex items-center justify-center p-6">
          <div className="text-center">
            <AlertCircle className="w-12 h-12 text-critical mx-auto mb-4" />
            <p className="text-gray-300 mb-2">Failed to load resource details</p>
            <p className="text-sm text-gray-500">{error}</p>
          </div>
        </div>
      </div>
    );
  }

  // Empty
  if (!selected) {
    return (
      <div className="h-full flex items-center justify-center">
        <div className="text-center">
          <div className="flex items-center justify-center gap-4 mb-4">
            <Box className="w-8 h-8 text-gray-600" />
            <Network className="w-8 h-8 text-gray-600" />
          </div>
          <p className="text-gray-400 mb-2">No resource selected</p>
          <p className="text-sm text-gray-600">
            Click a Pod or Service from the lists to view diagnostic details
          </p>
        </div>
      </div>
    );
  }

  // Details
  return (
    <div className="h-full flex flex-col">
      <PanelHeader
        title={selected.type === 'pod' ? 'Pod Details' : 'Service Details'}
        icon={selected.type === 'pod' ? <Box className="w-4 h-4 text-blue-400" /> : <Network className="w-4 h-4 text-purple-400" />}
        onClose={onClose}
      />
      <div className="flex-1 overflow-y-auto p-6">
        {selected.type === 'pod' ? (
          <PodDetails pod={selected.data} />
        ) : (
          <ServiceDetails service={selected.data} />
        )}
      </div>
    </div>
  );
}

// Header
function PanelHeader({
  title,
  icon,
  onClose,
}: {
  title: string;
  icon?: React.ReactNode;
  onClose: () => void;
}) {
  return (
    <div className="flex items-center justify-between px-6 py-4 border-b border-border shrink-0">
      <div className="flex items-center gap-2">
        {icon}
        <h2 className="text-lg font-medium text-gray-200">{title}</h2>
      </div>
      <button
        onClick={onClose}
        className="p-1.5 rounded text-gray-500 hover:text-gray-300 hover:bg-gray-700 transition-colors"
      >
        <X className="w-5 h-5" />
      </button>
    </div>
  );
}

// Skeleton
function DetailsSkeleton() {
  return (
    <div className="space-y-6 animate-pulse">
      {/* Header */}
      <div className="flex items-start justify-between">
        <div>
          <div className="w-48 h-6 bg-gray-700 rounded mb-2" />
          <div className="w-32 h-4 bg-gray-800 rounded" />
        </div>
        <div className="flex gap-2">
          <div className="w-20 h-6 bg-gray-700 rounded" />
          <div className="w-20 h-6 bg-gray-700 rounded" />
        </div>
      </div>

      {/* Sections */}
      {[1, 2, 3].map((i) => (
        <div key={i}>
          <div className="w-32 h-4 bg-gray-700 rounded mb-3" />
          <div className="space-y-2">
            <div className="w-full h-4 bg-gray-800 rounded" />
            <div className="w-3/4 h-4 bg-gray-800 rounded" />
            <div className="w-5/6 h-4 bg-gray-800 rounded" />
          </div>
        </div>
      ))}
    </div>
  );
}
