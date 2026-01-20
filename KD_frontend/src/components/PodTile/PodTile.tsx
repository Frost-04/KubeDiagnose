import { Box, RotateCcw } from 'lucide-react';
import { StatusBadge } from '../StatusBadge';
import type { PodDiagnosticResult } from '../../types';

interface PodTileProps {
  pod: PodDiagnosticResult;
  isSelected: boolean;
  onClick: () => void;
}

export function PodTile({ pod, isSelected, onClick }: PodTileProps) {
  return (
    <button
      onClick={onClick}
      className={`
        w-full p-3 rounded-lg border text-left transition-all
        ${
          isSelected
            ? 'bg-blue-600/10 border-blue-500/50 ring-1 ring-blue-500/30'
            : 'bg-card border-border hover:border-gray-600 hover:bg-gray-800/50'
        }
        focus:outline-none focus:ring-2 focus:ring-blue-500/40
      `}
    >
      <div className="flex items-start justify-between gap-2 mb-2">
        <div className="flex items-center gap-2 min-w-0">
          <Box className="w-4 h-4 text-blue-400 shrink-0" />
          <span className="text-sm font-medium text-gray-200 truncate">{pod.resourceName}</span>
        </div>
        <StatusBadge status={pod.status} size="sm" />
      </div>

      <div className="flex items-center gap-4 text-xs text-gray-500">
        <span className="flex items-center gap-1">
          <span className="text-gray-600">Phase:</span>
          <span
            className={`
              ${pod.phase === 'Running' ? 'text-healthy' : ''}
              ${pod.phase === 'Pending' ? 'text-warning' : ''}
              ${pod.phase === 'Failed' ? 'text-critical' : ''}
            `}
          >
            {pod.phase}
          </span>
        </span>
        {pod.restartCount > 0 && (
          <span className="flex items-center gap-1">
            <RotateCcw className="w-3 h-3" />
            <span className={pod.restartCount >= 5 ? 'text-warning' : ''}>
              {pod.restartCount} restarts
            </span>
          </span>
        )}
      </div>
    </button>
  );
}

// Skeleton
export function PodTileSkeleton() {
  return (
    <div className="w-full p-3 rounded-lg border border-border bg-card animate-pulse">
      <div className="flex items-start justify-between gap-2 mb-2">
        <div className="flex items-center gap-2">
          <div className="w-4 h-4 bg-gray-700 rounded" />
          <div className="w-32 h-4 bg-gray-700 rounded" />
        </div>
        <div className="w-16 h-5 bg-gray-700 rounded" />
      </div>
      <div className="flex items-center gap-4">
        <div className="w-20 h-3 bg-gray-800 rounded" />
        <div className="w-16 h-3 bg-gray-800 rounded" />
      </div>
    </div>
  );
}
