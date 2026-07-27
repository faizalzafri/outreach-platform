/**
 * Notifications Route
 *
 * Displays notification templates, delivery status, and scheduling.
 * Validates search params with Zod schema using .catch() to discard invalid params.
 * Uses React.lazy + Suspense for code splitting.
 *
 * Requirements: 3.2, 3.4, 3.5
 */

import { createFileRoute } from '@tanstack/react-router';
import { lazy, Suspense } from 'react';
import { z } from 'zod';
import { PageSkeleton } from '@/components/feedback/PageSkeleton';

const NotificationsContent = lazy(() =>
  import('./-components/NotificationsContent').then((mod) => ({
    default: mod.NotificationsContent,
  }))
);

const notificationSearchSchema = z.object({
  page: z.number().int().positive().default(1).catch(1),
  size: z.number().int().positive().default(10).catch(10),
  type: z.enum(['EMAIL', 'SMS', 'PUSH']).optional().catch(undefined),
});

export type NotificationSearch = z.infer<typeof notificationSearchSchema>;

export const Route = createFileRoute('/_authenticated/notifications/')({
  validateSearch: (search: Record<string, unknown>): NotificationSearch =>
    notificationSearchSchema.parse(search),
  component: NotificationsPage,
});

function NotificationsPage() {
  return (
    <Suspense fallback={<PageSkeleton title="Notifications" />}>
      <NotificationsContent />
    </Suspense>
  );
}
