/**
 * No-Tenant Error Route
 *
 * Dedicated error page shown when the auth server rejects login with
 * NO_TENANT_ASSOCIATION — the authenticated user isn't a member of any tenant.
 * Distinct from a generic authentication failure: the user's credentials are
 * valid, but their account has no tenant to operate in.
 */

import { createFileRoute } from '@tanstack/react-router';
import { authModule } from '@/lib/auth';
import styles from './login.module.css';

export const Route = createFileRoute('/no-tenant')({
  component: NoTenantPage,
});

export function NoTenantPage() {
  return (
    <div className={styles.page}>
      <div className={styles.card} role="alert">
        <div className={styles.brand}>
          <span className={styles.brandIcon} aria-hidden="true">
            ◈
          </span>
          <span className={styles.brandName}>Outreach Studio</span>
        </div>

        <h1 className={styles.heading}>No Tenant Access</h1>
        <p className={styles.subtitle}>
          Your account isn't associated with any organization yet. Contact your
          administrator to be added to a tenant, then try signing in again.
        </p>

        <button type="button" className={styles.button} onClick={() => authModule.login()}>
          Try Again
        </button>

        <p className={styles.footer}>
          Secured with OAuth 2.0 + PKCE
        </p>
      </div>
    </div>
  );
}
