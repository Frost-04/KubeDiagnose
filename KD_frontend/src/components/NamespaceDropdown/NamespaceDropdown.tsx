import { useState, useEffect, useRef, useCallback } from 'react';
import { ChevronDown, Search, X } from 'lucide-react';

interface NamespaceDropdownProps {
  namespaces: string[];
  selectedNamespace: string;
  onSelect: (namespace: string) => void;
  isLoading: boolean;
  error: string | null;
}

export function NamespaceDropdown({
  namespaces,
  selectedNamespace,
  onSelect,
  isLoading,
  error,
}: NamespaceDropdownProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const dropdownRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const filteredNamespaces = namespaces.filter((ns) =>
    ns.toLowerCase().includes(searchTerm.toLowerCase())
  );

  // Close on outside click
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
        setSearchTerm('');
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Focus input when open
  useEffect(() => {
    if (isOpen && inputRef.current) {
      inputRef.current.focus();
    }
  }, [isOpen]);

  const handleSelect = useCallback(
    (namespace: string) => {
      onSelect(namespace);
      setIsOpen(false);
      setSearchTerm('');
    },
    [onSelect]
  );

  // Keyboard shortcuts
  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === 'Escape') {
        setIsOpen(false);
        setSearchTerm('');
      } else if (e.key === 'Enter' && filteredNamespaces.length > 0) {
        handleSelect(filteredNamespaces[0]);
      }
    },
    [filteredNamespaces, handleSelect]
  );

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        disabled={isLoading}
        className={`
          flex items-center gap-2 px-3 py-2 bg-card border border-border rounded-lg
          hover:border-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500/40
          transition-colors min-w-[180px] justify-between
          ${isLoading ? 'opacity-50 cursor-not-allowed' : ''}
        `}
      >
        <span className="text-sm text-gray-200 truncate">
          {isLoading ? 'Loading...' : selectedNamespace}
        </span>
        <ChevronDown
          className={`w-4 h-4 text-gray-400 transition-transform ${isOpen ? 'rotate-180' : ''}`}
        />
      </button>

      {isOpen && (
        <div className="absolute top-full left-0 mt-1 w-64 bg-card border border-border rounded-lg shadow-xl z-50">
          {/* Search */}
          <div className="p-2 border-b border-border">
            <div className="relative">
              <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" />
              <input
                ref={inputRef}
                type="text"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="Search namespaces..."
                className="w-full pl-8 pr-8 py-1.5 bg-background border border-border rounded text-sm text-gray-200 placeholder-gray-500 focus:outline-none focus:border-blue-500"
              />
              {searchTerm && (
                <button
                  onClick={() => setSearchTerm('')}
                  className="absolute right-2 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-300"
                >
                  <X className="w-4 h-4" />
                </button>
              )}
            </div>
          </div>

          <div className="max-h-60 overflow-y-auto py-1">
            {error ? (
              <div className="px-3 py-2 text-sm text-critical">{error}</div>
            ) : filteredNamespaces.length === 0 ? (
              <div className="px-3 py-2 text-sm text-gray-500">No namespaces found</div>
            ) : (
              filteredNamespaces.map((namespace) => (
                <button
                  key={namespace}
                  onClick={() => handleSelect(namespace)}
                  className={`
                    w-full px-3 py-2 text-left text-sm transition-colors
                    ${
                      namespace === selectedNamespace
                        ? 'bg-blue-600/20 text-blue-400'
                        : 'text-gray-300 hover:bg-gray-800'
                    }
                  `}
                >
                  {namespace}
                </button>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}
