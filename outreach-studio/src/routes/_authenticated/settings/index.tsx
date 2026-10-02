/**
 * Settings Route
 *
 * The signed-in user's own profile: name, phone and password. Open to every role.
 */

import { createFileRoute } from '@tanstack/react-router';
import { ProfileContent } from './-components/ProfileContent';

export const Route = createFileRoute('/_authenticated/settings/')({
  component: SettingsPage,
});

function SettingsPage() {
  return (
    <ProfileContent />
  );
}
