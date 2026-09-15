/**
 * AuthProvider React Context
 *
 * Provides authentication state and actions to the component tree.
 * - Initializes auth module on mount
 * - Subscribes to auth state changes
 * - Exposes user, isAuthenticated, isLoading, login, logout
 */

import {
  createContext,
  useContext,
  useEffect,
  useState,
  useCallback,
  type ReactNode,
} from 'react';
import { authModule } from '@/lib/auth';
import type { AuthState, UserProfile } from '@/types/auth';

export interface AuthContextValue {
  user: UserProfile | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  tenantSelectionRequired: boolean;
  login: () => void;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export interface AuthProviderProps {
  children: ReactNode;
}

/**
 * When VITE_AUTH_BYPASS=true, the app uses a mock admin user instead of
 * requiring a real Keycloak connection. Set in .env.development for local dev.
 */
const AUTH_BYPASS = import.meta.env.VITE_AUTH_BYPASS === 'true';

const MOCK_USER: UserProfile = {
  sub: 'dev-user-001',
  name: 'Dev Admin',
  email: 'admin@outreach.dev',
  roles: ['ROLE_ADMIN', 'ROLE_PMO', 'ROLE_POC'],
};

export function AuthProvider({ children }: AuthProviderProps) {
  const [authState, setAuthState] = useState<AuthState>(() =>
    AUTH_BYPASS
      ? {
          accessToken: 'mock-token',
          refreshToken: null,
          user: MOCK_USER,
          isAuthenticated: true,
          isLoading: false,
          tenantSelectionRequired: false,
        }
      : authModule.getState()
  );

  useEffect(() => {
    if (AUTH_BYPASS) return; // Skip real auth initialization

    // Subscribe to auth state changes
    const unsubscribe = authModule.onStateChange((newState) => {
      setAuthState(newState);
    });

    // Initialize auth module on mount
    void authModule.initialize();

    return unsubscribe;
  }, []);

  const login = useCallback(() => {
    authModule.login();
  }, []);

  const logout = useCallback(async () => {
    if (AUTH_BYPASS) {
      // In bypass mode, just reload the page
      window.location.reload();
      return;
    }
    await authModule.logout();
  }, []);

  const contextValue: AuthContextValue = {
    user: authState.user,
    isAuthenticated: authState.isAuthenticated,
    isLoading: authState.isLoading,
    tenantSelectionRequired: authState.tenantSelectionRequired,
    login,
    logout,
  };

  if (authState.isLoading) {
    return (
      <AuthContext.Provider value={contextValue}>
        <div
          role="status"
          aria-label="Loading authentication"
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            height: '100vh',
            width: '100%',
          }}
        >
          <span>Loading...</span>
        </div>
      </AuthContext.Provider>
    );
  }

  return (
    <AuthContext.Provider value={contextValue}>
      {children}
    </AuthContext.Provider>
  );
}

/**
 * Hook to consume authentication context.
 * Must be used within an AuthProvider.
 */
export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
