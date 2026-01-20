import { useState, useCallback } from 'react';
import { Search, Box, Network } from 'lucide-react';

type ResourceType = 'pod' | 'service';

interface SearchBarProps {
  onSearch: (type: ResourceType, namespace: string, name: string) => void;
  isLoading: boolean;
  defaultNamespace: string;
}

export function SearchBar({ onSearch, isLoading, defaultNamespace }: SearchBarProps) {
  const [resourceType, setResourceType] = useState<ResourceType>('pod');
  const [namespace, setNamespace] = useState(defaultNamespace);
  const [resourceName, setResourceName] = useState('');

  // Sync namespace
  useState(() => {
    setNamespace(defaultNamespace);
  });

  const handleSubmit = useCallback(
    (e: React.FormEvent) => {
      e.preventDefault();
      if (resourceName.trim() && namespace.trim()) {
        onSearch(resourceType, namespace.trim(), resourceName.trim());
      }
    },
    [resourceType, namespace, resourceName, onSearch]
  );

  return (
    <form onSubmit={handleSubmit} className="flex items-center gap-2">
      {/* Type toggle */}
      <div className="flex bg-card border border-border rounded-lg overflow-hidden">
        <button
          type="button"
          onClick={() => setResourceType('pod')}
          className={`
            flex items-center gap-1.5 px-3 py-2 text-sm transition-colors
            ${
              resourceType === 'pod'
                ? 'bg-blue-600/20 text-blue-400 border-r border-border'
                : 'text-gray-400 hover:text-gray-200 border-r border-border'
            }
          `}
        >
          <Box className="w-4 h-4" />
          Pod
        </button>
        <button
          type="button"
          onClick={() => setResourceType('service')}
          className={`
            flex items-center gap-1.5 px-3 py-2 text-sm transition-colors
            ${
              resourceType === 'service'
                ? 'bg-blue-600/20 text-blue-400'
                : 'text-gray-400 hover:text-gray-200'
            }
          `}
        >
          <Network className="w-4 h-4" />
          Service
        </button>
      </div>

      <input
        type="text"
        value={namespace}
        onChange={(e) => setNamespace(e.target.value)}
        placeholder="namespace"
        className="w-32 px-3 py-2 bg-card border border-border rounded-lg text-sm text-gray-200 placeholder-gray-500 focus:outline-none focus:border-blue-500 transition-colors"
      />

      <div className="relative flex-1 min-w-[200px] max-w-md">
        <input
          type="text"
          value={resourceName}
          onChange={(e) => setResourceName(e.target.value)}
          placeholder={`Enter ${resourceType} name...`}
          className="w-full pl-3 pr-10 py-2 bg-card border border-border rounded-lg text-sm text-gray-200 placeholder-gray-500 focus:outline-none focus:border-blue-500 transition-colors"
        />
        <button
          type="submit"
          disabled={isLoading || !resourceName.trim() || !namespace.trim()}
          className={`
            absolute right-1 top-1/2 -translate-y-1/2 p-1.5 rounded
            ${
              isLoading || !resourceName.trim() || !namespace.trim()
                ? 'text-gray-600 cursor-not-allowed'
                : 'text-gray-400 hover:text-blue-400 hover:bg-blue-600/10'
            }
            transition-colors
          `}
        >
          <Search className="w-4 h-4" />
        </button>
      </div>
    </form>
  );
}
