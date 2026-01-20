import { Copy, Check, AlertTriangle, FileText, Lightbulb, Globe, Tag, CheckCircle, XCircle } from 'lucide-react';
import { useState, useCallback } from 'react';
import { StatusBadge } from '../StatusBadge';
import type { ServiceDiagnosticResult } from '../../types';

interface ServiceDetailsProps {
  service: ServiceDiagnosticResult;
}

export function ServiceDetails({ service }: ServiceDetailsProps) {
  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-start justify-between">
        <div>
          <h2 className="text-xl font-semibold text-gray-100">{service.resourceName}</h2>
          <p className="text-sm text-gray-500 mt-1">
            Namespace: <span className="text-gray-400">{service.namespace}</span>
          </p>
        </div>
        <div className="flex items-center gap-3">
          <StatusBadge status={service.status} size="md" />
          <span className="px-2 py-1 text-sm bg-purple-900/30 text-purple-400 border border-purple-800 rounded">
            {service.serviceType}
          </span>
        </div>
      </div>

      {/* Selector */}
      {service.selector && Object.keys(service.selector).length > 0 && (
        <Section title="Selector Labels" icon={<Tag className="w-4 h-4" />}>
          <div className="flex flex-wrap gap-2">
            {Object.entries(service.selector).map(([key, value]) => (
              <span
                key={key}
                className="inline-flex items-center px-2 py-1 bg-background rounded text-xs font-mono"
              >
                <span className="text-blue-400">{key}</span>
                <span className="text-gray-600 mx-1">:</span>
                <span className="text-green-400">{value}</span>
              </span>
            ))}
          </div>
        </Section>
      )}

      {/* Ports */}
      {service.ports && service.ports.length > 0 && (
        <Section title="Ports" icon={<Globe className="w-4 h-4" />}>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-gray-500 border-b border-border">
                  <th className="pb-2 font-medium">Name</th>
                  <th className="pb-2 font-medium">Protocol</th>
                  <th className="pb-2 font-medium">Port</th>
                  <th className="pb-2 font-medium">Target Port</th>
                  <th className="pb-2 font-medium">Node Port</th>
                </tr>
              </thead>
              <tbody className="text-gray-300">
                {service.ports.map((port, index) => (
                  <tr key={index} className="border-b border-border/50">
                    <td className="py-2 text-gray-400">{port.name || '—'}</td>
                    <td className="py-2 font-mono text-xs">{port.protocol}</td>
                    <td className="py-2 font-mono text-blue-400">{port.port}</td>
                    <td className="py-2 font-mono text-green-400">{port.targetPort}</td>
                    <td className="py-2 font-mono text-gray-400">{port.nodePort || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Section>
      )}

      {/* Endpoints */}
      {service.endpointInfo && (
        <Section title="Endpoints" icon={<CheckCircle className="w-4 h-4" />}>
          <div className="space-y-3">
            <div className="flex items-center gap-6">
              <div className="flex items-center gap-2">
                <span className="text-sm text-gray-500">Ready:</span>
                <span
                  className={`text-sm font-medium ${
                    service.endpointInfo.readyEndpoints > 0 ? 'text-healthy' : 'text-critical'
                  }`}
                >
                  {service.endpointInfo.readyEndpoints}
                </span>
              </div>
              <div className="flex items-center gap-2">
                <span className="text-sm text-gray-500">Not Ready:</span>
                <span
                  className={`text-sm font-medium ${
                    service.endpointInfo.notReadyEndpoints > 0 ? 'text-warning' : 'text-gray-400'
                  }`}
                >
                  {service.endpointInfo.notReadyEndpoints}
                </span>
              </div>
            </div>
            {service.endpointInfo.addresses.length > 0 && (
              <div className="bg-background rounded-lg p-3 font-mono text-xs text-gray-400 space-y-1 max-h-32 overflow-y-auto">
                {service.endpointInfo.addresses.map((addr, index) => (
                  <div key={index} className={addr.includes('Ready') ? 'text-healthy' : 'text-warning'}>
                    {addr}
                  </div>
                ))}
              </div>
            )}
          </div>
        </Section>
      )}

      {/* CoreDNS */}
      <div className="flex items-center gap-2 px-4 py-3 rounded-lg border bg-gray-800/50 border-border">
        {service.coreDnsExists ? (
          <>
            <CheckCircle className="w-5 h-5 text-healthy" />
            <span className="text-sm text-gray-300">CoreDNS is running</span>
          </>
        ) : (
          <>
            <XCircle className="w-5 h-5 text-critical" />
            <span className="text-sm text-gray-300">CoreDNS is not running</span>
          </>
        )}
      </div>

      {/* Causes */}
      <Section title="Probable Causes" icon={<AlertTriangle className="w-4 h-4" />}>
        <ul className="space-y-2">
          {service.probableCauses.map((cause, index) => (
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
          {service.evidence.map((item, index) => (
            <div key={index}>{item}</div>
          ))}
        </div>
      </Section>

      {/* Actions */}
      <Section title="Suggested Actions" icon={<Lightbulb className="w-4 h-4" />}>
        <div className="space-y-2">
          {service.suggestedActions.map((action, index) => (
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
