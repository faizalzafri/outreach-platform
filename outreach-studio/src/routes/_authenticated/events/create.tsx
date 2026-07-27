/**
 * Event Create Route
 *
 * Form for creating a new event with Zod validation.
 *
 * Requirements: 3.2, 3.3
 */

import { createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute('/_authenticated/events/create')({
  component: EventCreatePage,
});

function EventCreatePage() {
  return (
    <div>
      <h1>Create Event</h1>
      <p>Event creation form placeholder.</p>
    </div>
  );
}
