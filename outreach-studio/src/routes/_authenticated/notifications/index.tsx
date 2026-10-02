/**
 * Notifications Route
 *
 * Displays notification templates, delivery status, and scheduling.
 * Validates search params with Zod schema using .catch() to discard invalid params.
 */

import { createFileRoute } from '@tanstack/react-router';
import { z } from 'zod';
import { ProtectedRoute } from '@/components/layout/ProtectedRoute';
import { NotificationsContent } from './-components/NotificationsContent';

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
    <ProtectedRoute requiredRoles={['ROLE_ADMIN']}>
      <NotificationsContent />
    </ProtectedRoute>
  );
}
