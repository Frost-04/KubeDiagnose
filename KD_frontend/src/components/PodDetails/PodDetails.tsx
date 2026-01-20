import { Copy, Check, AlertTriangle, FileText, Lightbulb, RotateCcw } from 'lucide-react';
import { useState, useCallback } from 'react';
import { StatusBadge } from '../StatusBadge';
import type { PodDiagnosticResult } from '../../types';

interface PodDetailsProps {
  pod: PodDiagnosticResult;
}

export function PodDetails({ pod }: PodDetailsProps) {
  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-start justify-between">
        <div>
          <h2 className="text-xl font-semibold text-gray-100">{pod.resourceName}</h2>
          <p className="text-sm text-gray-500 mt-1">
            Namespace: <span className="text-gray-400">{pod.namespace}</span>
          </p>
        </div>
        <div className="flex items-center gap-3">
          <StatusBadge status={pod.status} size="md" />
          <span
            className={`
              px-2 py-1 text-sm rounded border
              ${pod.phase === 'Running' ? 'bg-healthy-bg text-healthy border-healthy-border' : ''}
              ${pod.phase === 'Pending' ? 'bg-warning-bg text-warning border-warning-border' : ''}
              ${pod.phase === 'Failed' ? 'bg-critical-bg text-critical border-critical-border' : ''}
              ${pod.phase === 'Succeeded' ? 'bg-blue-900/30 text-blue-400 border-blue-800' : ''}
              ${pod.phase === 'Unknown' ? 'bg-gray-800 text-gray-400 border-gray-700' : ''}
            `}
          >
            {pod.phase}
          </span>
        </div>
      </div>

      {/* Restarts */}
      {pod.restartCount > 0 && (
        <div
          className={`
            flex items-center gap-2 px-4 py-3 rounded-lg border
            ${pod.restartCount >= 5 ? 'bg-warning-bg border-warning-border' : 'bg-gray-800/50 border-border'}
          `}
        >
          <RotateCcw className={`w-5 h-5 ${pod.restartCount >= 5 ? 'text-warning' : 'text-gray-400'}`} />
          <span className="text-sm text-gray-300">
            Total restarts: <strong className={pod.restartCount >= 5 ? 'text-warning' : 'text-gray-100'}>{pod.restartCount}</strong>
          </span>
        </div>
      )}

      {/* Containers */}
      {pod.containerStatuses && pod.containerStatuses.length > 0 && (
        <Section title="Container Statuses" icon={<FileText className="w-4 h-4" />}>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-gray-500 border-b border-border">
                  <th className="pb-2 font-medium">Container</th>
                  <th className="pb-2 font-medium">State</th>
                  <th className="pb-2 font-medium">Reason</th>
                  <th className="pb-2 font-medium">Restarts</th>
                  <th className="pb-2 font-medium">Ready</th>
                </tr>
              </thead>
              <tbody className="text-gray-300">
                {pod.containerStatuses.map((container) => (
                  <tr key={container.name} className="border-b border-border/50">
                    <td className="py-2 font-mono text-xs text-blue-400">{container.name}</td>
                    <td className="py-2">
                      <span
                        className={`
                          ${container.state === 'Running' ? 'text-healthy' : ''}
                          ${container.state === 'Waiting' ? 'text-warning' : ''}
                          ${container.state === 'Terminated' ? 'text-critical' : ''}
                        `}
                      >
                        {container.state}
                      </span>
                    </td>
                    <td className="py-2 text-gray-400">{container.reason || '—'}</td>
                    <td className="py-2">
                      <span className={container.restartCount >= 5 ? 'text-warning font-medium' : ''}>
                        {container.restartCount}
                      </span>
                    </td>
                    <td className="py-2">
                      {container.ready ? (
                        <span className="text-healthy">Yes</span>
                      ) : (
                        <span className="text-critical">No</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Section>
      )}

      {/* Causes */}
      <Section title="Probable Causes" icon={<AlertTriangle className="w-4 h-4" />}>
        <ul className="space-y-2">
          {pod.probableCauses.map((cause, index) => (
            <li key={index} className="flex items-start gap-2 text-sm text-gray-300">
              <span className="text-critical mt-0.5">•</span>
              {cause}
            </li>
          ))}
        </ul>
      </Section>

      {/* Evidence */}
      <Section title="Evidence" icon={<FileText className="w-4 h-4" />}>
        <div className="bg-background rounded-lg p-3 font-mono text-xs text-gray-400 space-y-1 max-h-48 overflow-y-auto">
          {pod.evidence.map((item, index) => (
            <div key={index}>{item}</div>
          ))}
        </div>
      </Section>

      {/* Actions */}
      <Section title="Suggested Actions" icon={<Lightbulb className="w-4 h-4" />}>
        <div className="space-y-2">
          {pod.suggestedActions.map((action, index) => (
            <ActionItem key={index} action={action} />
          ))}
        </div>
      </Section>
    </div>
  );
}

// Section
function Section({
  title,
  icon,
  children,
}: {
  title: string;
  icon: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <div>
      <div className="flex items-center gap-2 mb-3">
        <span className="text-gray-500">{icon}</span>
        <h3 className="text-sm font-medium text-gray-300">{title}</h3>
      </div>
      {children}
    </div>
  );
}

// Copy action
function ActionItem({ action }: { action: string }) {
  const [copied, setCopied] = useState(false);

  // Detect commands
  const isCommand = action.includes('kubectl') || action.startsWith('docker') || action.includes('--');

  const handleCopy = useCallback(() => {
    navigator.clipboard.writeText(action);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }, [action]);

  if (isCommand) {
    return (
      <div className="flex items-center gap-2 bg-background rounded-lg p-2 group">
        <code className="flex-1 font-mono text-xs text-green-400 break-all">{action}</code>
        <button
          onClick={handleCopy}
          className="shrink-0 p-1.5 rounded text-gray-500 hover:text-gray-300 hover:bg-gray-700 transition-colors"
          title="Copy to clipboard"
        >
          {copied ? <Check className="w-4 h-4 text-healthy" /> : <Copy className="w-4 h-4" />}
        </button>
      </div>
    );
  }

  return (
    <div className="flex items-start gap-2 text-sm text-gray-300">
      <Lightbulb className="w-4 h-4 text-warning shrink-0 mt-0.5" />
      {action}
    </div>
  );
}
