/**
 * Route Prefetch Hook
 *
 * Prefetches a route's code chunk on hover with a 150ms delay to avoid
 * unnecessary prefetching on mouse pass-through. Cancels if the user moves
 * away before the delay elapses.
 *
 * This deliberately does not also prefetch each route's initial data query:
 * a prior version seeded the query cache with a stub `queryFn` that always
 * resolved to `null` under the same query key the route's real query uses.
 * Since a normal click is preceded by a mouseenter, that stub reliably won
 * the race against the real fetch — the page's query saw fresh (if stale-far)
 * cached `null` data and never called its real queryFn, landing on an empty
 * or error state until something (a manual retry, staleTime elapsing)
 * triggered a real fetch. Only prefetch what's actually correct: the chunk.
 */

import { useCallback, useRef } from 'react';
import { useRouter } from '@tanstack/react-router';

/**
 * Maps route paths to their corresponding code chunk import, for prefetching
 * on hover.
 */
const ROUTE_PREFETCH_MAP: Record<string, { chunk: () => Promise<unknown> }> = {
  '/dashboard': {
    chunk: () => import('@/routes/_authenticated/-components/DashboardContent'),
  },
  '/events': {
    chunk: () => import('@/routes/_authenticated/events/-components/EventListContent'),
  },
  '/volunteers': {
    chunk: () => import('@/routes/_authenticated/volunteers/-components/VolunteerListContent'),
  },
  '/feedback': {
    chunk: () => import('@/routes/_authenticated/feedback/-components/FeedbackContent'),
  },
  '/ingestion': {
    chunk: () => import('@/routes/_authenticated/ingestion/-components/IngestionContent'),
  },
  '/notifications': {
    chunk: () => import('@/routes/_authenticated/notifications/-components/NotificationsContent'),
  },
  '/reports': {
    chunk: () => import('@/routes/_authenticated/reports/-components/ReportsContent'),
  },
  '/admin': {
    chunk: () => import('@/routes/_authenticated/admin/-components/AdminContent'),
  },
  '/audit-log': {
    chunk: () => import('@/routes/_authenticated/audit-log/-components/AuditLogContent'),
  },
};

const PREFETCH_DELAY_MS = 150;

/**
 * Returns onMouseEnter/onMouseLeave handlers that prefetch route chunks
 * and initial data queries after a 150ms hover delay.
 */
export function usePrefetch() {
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const prefetchedRef = useRef<Set<string>>(new Set());
  const router = useRouter();

  const handleMouseEnter = useCallback(
    (href: string) => {
      // Skip if already prefetched
      if (prefetchedRef.current.has(href)) return;

      timerRef.current = setTimeout(() => {
        prefetchedRef.current.add(href);

        // Prefetch the route via TanStack Router (loads code chunk)
        void router.preloadRoute({ to: href }).catch(() => {
          // Silently ignore prefetch failures
        });

        // Additionally prefetch the route's code chunk directly for coverage
        const routeConfig = ROUTE_PREFETCH_MAP[href];
        if (routeConfig) {
          void routeConfig.chunk().catch(() => {
            // Silently ignore chunk prefetch failures
          });
        }
      }, PREFETCH_DELAY_MS);
    },
    [router]
  );

  const handleMouseLeave = useCallback(() => {
    if (timerRef.current) {
      clearTimeout(timerRef.current);
      timerRef.current = null;
    }
  }, []);

  return { handleMouseEnter, handleMouseLeave };
}
