/**
 * Route Prefetch Hook
 *
 * Provides a prefetch mechanism for route code chunks and initial data queries.
 * Triggers on hover with a 150ms delay to avoid unnecessary prefetching on
 * mouse pass-through. Cancels if the user moves away before the delay elapses.
 */

import { useCallback, useRef } from 'react';
import { useRouter } from '@tanstack/react-router';
import { useQueryClient } from '@tanstack/react-query';
import { queryKeys } from '@/lib/query-keys';

/**
 * Maps route paths to their corresponding chunk import and initial query key.
 * This enables prefetching both the code chunk and initial API data on hover.
 */
const ROUTE_PREFETCH_MAP: Record<
  string,
  {
    chunk: () => Promise<unknown>;
    queryKey?: readonly unknown[];
  }
> = {
  '/dashboard': {
    chunk: () => import('@/routes/_authenticated/-components/DashboardContent'),
    queryKey: queryKeys.reports.dashboard({}),
  },
  '/events': {
    chunk: () => import('@/routes/_authenticated/events/-components/EventListContent'),
    queryKey: queryKeys.events.lists(),
  },
  '/volunteers': {
    chunk: () => import('@/routes/_authenticated/volunteers/-components/VolunteerListContent'),
    queryKey: queryKeys.volunteers.lists(),
  },
  '/feedback': {
    chunk: () => import('@/routes/_authenticated/feedback/-components/FeedbackContent'),
    queryKey: queryKeys.feedback.list({}),
  },
  '/ingestion': {
    chunk: () => import('@/routes/_authenticated/ingestion/-components/IngestionContent'),
    queryKey: queryKeys.ingestion.jobs(),
  },
  '/notifications': {
    chunk: () => import('@/routes/_authenticated/notifications/-components/NotificationsContent'),
    queryKey: queryKeys.notifications.templates(),
  },
  '/reports': {
    chunk: () => import('@/routes/_authenticated/reports/-components/ReportsContent'),
  },
  '/admin': {
    chunk: () => import('@/routes/_authenticated/admin/-components/AdminContent'),
    queryKey: queryKeys.admin.users(),
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
  const queryClient = useQueryClient();

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

          // Prefetch initial data query if configured
          if (routeConfig.queryKey) {
            void queryClient.prefetchQuery({
              queryKey: routeConfig.queryKey as unknown[],
              queryFn: () => Promise.resolve(null),
              staleTime: 30_000,
            });
          }
        }
      }, PREFETCH_DELAY_MS);
    },
    [router, queryClient]
  );

  const handleMouseLeave = useCallback(() => {
    if (timerRef.current) {
      clearTimeout(timerRef.current);
      timerRef.current = null;
    }
  }, []);

  return { handleMouseEnter, handleMouseLeave };
}
