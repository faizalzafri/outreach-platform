/**
 * Event Detail Route
 *
 * Displays details for a single event with lifecycle transition controls.
 * Supports nested tabs: Overview, Volunteers, Feedback, Notifications, Audit History.
 */

import { createFileRoute } from '@tanstack/react-router';
import { z } from 'zod';
import { EventDetailContent } from './-components/EventDetailContent';

const eventDetailSearchSchema = z.object({
  tab: z
    .enum(['overview', 'volunteers', 'pocs', 'beneficiaries', 'teams', 'feedback', 'notifications', 'audit'])
    .default('overview')
    .catch('overview'),
});

export type EventDetailSearch = z.infer<typeof eventDetailSearchSchema>;

export const Route = createFileRoute('/_authenticated/events/$eventId')({
  validateSearch: (search: Record<string, unknown>): EventDetailSearch =>
    eventDetailSearchSchema.parse(search),
  component: EventDetailPage,
});

function EventDetailPage() {
  return (
    <EventDetailContent />
  );
}
