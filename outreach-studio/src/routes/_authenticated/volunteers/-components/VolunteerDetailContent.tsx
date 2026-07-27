/**
 * Volunteer Detail Content (lazy-loaded)
 *
 * Placeholder for the volunteer detail page implementation.
 * Actual profile display and participation history will be implemented in task 13.
 */

import { Route } from '../$employeeId';

export function VolunteerDetailContent() {
  const { employeeId } = Route.useParams();

  return (
    <div>
      <h1>Volunteer Detail</h1>
      <p>Employee ID: {employeeId}</p>
    </div>
  );
}
