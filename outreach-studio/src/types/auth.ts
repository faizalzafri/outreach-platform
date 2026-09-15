// Auth-related types for the Outreach FMS SPA

export interface UserProfile {
  sub: string;
  name: string;
  email: string;
  roles: string[];
}

export interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: UserProfile | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  /**
   * True when the token's `tenant_selection_required` claim is set — the user belongs to
   * multiple tenants and hasn't explicitly chosen one yet. Route guards use this to redirect to
   * `/select-tenant` rather than trusting the token's (arbitrarily-picked) tenant_id.
   */
  tenantSelectionRequired: boolean;
}

export interface AuthModule {
  login(): void;
  logout(): Promise<void>;
  handleCallback(code: string, state: string): Promise<void>;
  getAccessToken(): string | null;
  silentRefresh(): Promise<string | null>;
  getUser(): UserProfile | null;
  getState(): AuthState;
  onStateChange(listener: (state: AuthState) => void): () => void;
  initialize(): Promise<void>;
}

export interface JwtClaims {
  sub: string;
  name?: string;
  preferred_username?: string;
  email?: string;
  realm_access?: {
    roles?: string[];
  };
  exp: number;
  iat: number;
  tenant_id?: string;
  tenant_roles?: string[];
  platform_admin?: boolean;
  tenant_selection_required?: boolean;
}

export interface TokenResponse {
  access_token: string;
  refresh_token: string;
  expires_in: number;
  token_type: string;
}

export interface KeycloakConfig {
  url: string;
  realm: string;
  clientId: string;
}
