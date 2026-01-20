import { type PodStatus, type ServiceStatus } from '../../types';

interface StatusBadgeProps {
  status: PodStatus | ServiceStatus;
  size?: 'sm' | 'md';
}

const statusStyles: Record<string, string> = {
  Critical: 'bg-critical-bg text-critical border-critical-border',
  Warning: 'bg-warning-bg text-warning border-warning-border',
  Healthy: 'bg-healthy-bg text-healthy border-healthy-border',
  Unknown: 'bg-gray-800 text-gray-400 border-gray-700',
  Completed: 'bg-blue-900/30 text-blue-400 border-blue-800',
};

const sizeStyles = {
  sm: 'px-2 py-0.5 text-xs',
  md: 'px-2.5 py-1 text-sm',
};

export function StatusBadge({ status, size = 'sm' }: StatusBadgeProps) {
  return (
    <span
      className={`
        inline-flex items-center font-medium rounded border
        ${statusStyles[status] || statusStyles.Unknown}
        ${sizeStyles[size]}
      `}
    >
      <span
        className={`
          w-1.5 h-1.5 rounded-full mr-1.5
          ${status === 'Critical' ? 'bg-critical' : ''}
          ${status === 'Warning' ? 'bg-warning' : ''}
          ${status === 'Healthy' ? 'bg-healthy' : ''}
          ${status === 'Unknown' ? 'bg-gray-500' : ''}
          ${status === 'Completed' ? 'bg-blue-400' : ''}
        `}
      />
      {status}
    </span>
  );
}
