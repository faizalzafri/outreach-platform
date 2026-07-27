import { create } from 'zustand';
import { persist, createJSONStorage, type StateStorage } from 'zustand/middleware';

export interface ToastNotification {
  id: string;
  severity: 'success' | 'error' | 'warning' | 'info';
  message: string;
  correlationId?: string;
  timestamp: number;
}

export interface UIState {
  // Sidebar
  sidebarCollapsed: boolean;
  toggleSidebar: () => void;

  // Theme
  theme: 'light' | 'dark' | 'system';
  setTheme: (theme: 'light' | 'dark' | 'system') => void;

  // Toasts
  toasts: ToastNotification[];
  addToast: (toast: Omit<ToastNotification, 'id' | 'timestamp'>) => void;
  dismissToast: (id: string) => void;

  // Offline
  isOffline: boolean;
  setOffline: (offline: boolean) => void;
}

const MAX_TOASTS = 50;

const DEFAULT_SIDEBAR_COLLAPSED = false;
const DEFAULT_THEME: UIState['theme'] = 'system';

/**
 * Safe localStorage wrapper that falls back gracefully if localStorage
 * is unavailable (e.g., private browsing mode in some browsers) or corrupted.
 */
const safeStorage: StateStorage = {
  getItem: (name: string): string | null => {
    try {
      return localStorage.getItem(name);
    } catch {
      return null;
    }
  },
  setItem: (name: string, value: string): void => {
    try {
      localStorage.setItem(name, value);
    } catch {
      // Silently fail — store continues with in-memory state
    }
  },
  removeItem: (name: string): void => {
    try {
      localStorage.removeItem(name);
    } catch {
      // Silently fail
    }
  },
};

function generateId(): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  // Fallback for environments without crypto.randomUUID
  return `${Date.now()}-${Math.random().toString(36).slice(2, 11)}`;
}

export const useUIStore = create<UIState>()(
  persist(
    (set) => ({
      // Sidebar
      sidebarCollapsed: DEFAULT_SIDEBAR_COLLAPSED,
      toggleSidebar: () =>
        set((state) => ({ sidebarCollapsed: !state.sidebarCollapsed })),

      // Theme
      theme: DEFAULT_THEME,
      setTheme: (theme) => set({ theme }),

      // Toasts
      toasts: [],
      addToast: (toast) =>
        set((state) => {
          const newToast: ToastNotification = {
            ...toast,
            id: generateId(),
            timestamp: Date.now(),
          };
          const updatedToasts = [...state.toasts, newToast];
          // Enforce max queue size by dropping oldest toasts
          if (updatedToasts.length > MAX_TOASTS) {
            return { toasts: updatedToasts.slice(updatedToasts.length - MAX_TOASTS) };
          }
          return { toasts: updatedToasts };
        }),
      dismissToast: (id) =>
        set((state) => ({
          toasts: state.toasts.filter((t) => t.id !== id),
        })),

      // Offline
      isOffline: false,
      setOffline: (offline) => set({ isOffline: offline }),
    }),
    {
      name: 'outreach-ui-state',
      storage: createJSONStorage(() => safeStorage),
      partialize: (state) => ({
        sidebarCollapsed: state.sidebarCollapsed,
        theme: state.theme,
      }),
      // If deserialization fails (corrupted data), merge with defaults
      merge: (persistedState, currentState) => {
        if (
          persistedState &&
          typeof persistedState === 'object' &&
          !Array.isArray(persistedState)
        ) {
          const persisted = persistedState as Partial<Pick<UIState, 'sidebarCollapsed' | 'theme'>>;
          return {
            ...currentState,
            sidebarCollapsed:
              typeof persisted.sidebarCollapsed === 'boolean'
                ? persisted.sidebarCollapsed
                : DEFAULT_SIDEBAR_COLLAPSED,
            theme:
              persisted.theme === 'light' ||
              persisted.theme === 'dark' ||
              persisted.theme === 'system'
                ? persisted.theme
                : DEFAULT_THEME,
          };
        }
        // Corrupted or null persisted state — use defaults
        return currentState;
      },
    }
  )
);
