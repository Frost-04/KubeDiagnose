import { Box } from 'lucide-react';
import { NamespaceDropdown } from '../NamespaceDropdown';
import { SearchBar } from '../SearchBar';

interface TopBarProps {
  namespaces: string[];
  selectedNamespace: string;
  onNamespaceChange: (namespace: string) => void;
  onSearch: (type: 'pod' | 'service', namespace: string, name: string) => void;
  isNamespacesLoading: boolean;
  isSearchLoading: boolean;
  namespacesError: string | null;
}

export function TopBar({
  namespaces,
  selectedNamespace,
  onNamespaceChange,
  onSearch,
  isNamespacesLoading,
  isSearchLoading,
  namespacesError,
}: TopBarProps) {
  return (
    <header className="sticky top-0 z-40 bg-card/95 backdrop-blur border-b border-border">
      <div className="flex items-center justify-between gap-4 px-4 py-3">
        {/* Logo */}
        <div className="flex items-center gap-2 shrink-0">
          <div className="flex items-center justify-center w-8 h-8 bg-blue-600/20 rounded-lg">
            <Box className="w-5 h-5 text-blue-400" />
          </div>
          <h1 className="text-lg font-semibold text-gray-100">KubeDiagnose</h1>
        </div>

        {/* Search */}
        <div className="flex-1 flex justify-center">
          <SearchBar
            onSearch={onSearch}
            isLoading={isSearchLoading}
            defaultNamespace={selectedNamespace}
          />
        </div>

        {/* Namespace */}
        <div className="shrink-0 flex items-center gap-2">
          <span className="text-sm text-gray-500">Namespace:</span>
          <NamespaceDropdown
            namespaces={namespaces}
            selectedNamespace={selectedNamespace}
            onSelect={onNamespaceChange}
            isLoading={isNamespacesLoading}
            error={namespacesError}
          />
        </div>
      </div>
    </header>
  );
}
