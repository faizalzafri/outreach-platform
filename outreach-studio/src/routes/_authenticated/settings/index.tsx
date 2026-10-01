/**
 * Settings Route
 *
 * The signed-in user's own profile: name, phone and password. Open to every role.
 */

import { createFileRoute } from '@tanstack/react-router';
import { lazy, Suspense } from 'react';
import { PageSkeleton } from '@/components/feedback/PageSkeleton';

const ProfileContent = lazy(() =>
  import('./-components/ProfileContent').then((mod) => ({
    default: mod.ProfileContent,
  }))
);

export const Route = createFileRoute('/_authenticated/settings/')({
  component: SettingsPage,
});

function SettingsPage() {
  return (
    <Suspense fallback={<PageSkeleton title="Your profile" />}>
      <ProfileContent />
    </Suspense>
  );
}
