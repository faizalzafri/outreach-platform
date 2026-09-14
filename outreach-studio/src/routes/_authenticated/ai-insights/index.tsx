/**
 * AI Insights Route
 *
 * Minimal admin UI over ai-service's async job API: feedback summarization,
 * anomaly detection, and natural-language queries. Feature-toggled — each
 * section reflects ai-service's own enabled/disabled status rather than
 * assuming all three are always available.
 */

import { createFileRoute } from '@tanstack/react-router';
import { lazy, Suspense } from 'react';
import { PageSkeleton } from '@/components/feedback/PageSkeleton';
import { ProtectedRoute } from '@/components/layout/ProtectedRoute';

const AiInsightsContent = lazy(() =>
  import('./-components/AiInsightsContent').then((mod) => ({
    default: mod.AiInsightsContent,
  }))
);

export const Route = createFileRoute('/_authenticated/ai-insights/')({
  component: AiInsightsPage,
});

function AiInsightsPage() {
  return (
    <ProtectedRoute requiredRoles={['ROLE_ADMIN']}>
      <Suspense fallback={<PageSkeleton title="AI Insights" />}>
        <AiInsightsContent />
      </Suspense>
    </ProtectedRoute>
  );
}
