/**
 * Volunteer Detail Route
 *
 * Displays profile and participation history for a specific volunteer.
 */

import { createFileRoute } from '@tanstack/react-router';
import { VolunteerDetailContent } from './-components/VolunteerDetailContent';

export const Route = createFileRoute('/_authenticated/volunteers/$employeeId')({
  component: VolunteerDetailPage,
});

function VolunteerDetailPage() {
  return (
    <VolunteerDetailContent />
  );
}
