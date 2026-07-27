/**
 * Notifications Route
 *
 * Displays notification templates, delivery status, and scheduling.
 *
 * Requirements: 3.2, 3.3
 */

import { createFileRoute } from '@tanstack/react-router';
import { z } from 'zod';

const notificationSearchSchema = z.object({
  page: z.number().int().positive().default(1),
  size: z.number().int().positive().default(10),
  type: z.enum(['EMAIL', 'SMS', 'PUSH']).optional(),
});

type NotificationSearch = z.infer<typeof notificationSearchSchema>;

export const Route = createFileRoute('/_authenticated/notifications/')({
  validateSearch: (search: Record<string, unknown>): NotificationSearch => {
    const result = notificationSearchSchema.safeParse(search);
    if (result.success) {
      return result.data;
    }
    return notificationSearchSchema.parse({});
  },
  component: NotificationsPage,
});

function NotificationsPage() {
  const search = Route.useSearch();

  return (
    <div>
      <h1>Notifications</h1>
      <p>
        Page {search.page}, Size {search.size}
        {search.type && `, Type: ${search.type}`}
      </p>
    </div>
  );
}
