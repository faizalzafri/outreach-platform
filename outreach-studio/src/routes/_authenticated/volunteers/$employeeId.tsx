/**
 * Volunteer Detail Route
 *
 * Displays profile, participation history, and feedback score trends
 * for a specific volunteer.
 *
 * Requirements: 3.2, 3.3
 */

import { createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute('/_authenticated/volunteers/$employeeId')({
  component: VolunteerDetailPage,
});

function VolunteerDetailPage() {
  const { employeeId } = Route.useParams();

  return (
    <div>
      <h1>Volunteer Detail</h1>
      <p>Employee ID: {employeeId}</p>
    </div>
  );
}
