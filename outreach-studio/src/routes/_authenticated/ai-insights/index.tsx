/**
 * AI Insights Route
 *
 * Minimal admin UI over ai-service's async job API: feedback summarization,
 * anomaly detection, and natural-language queries. Feature-toggled — each
 * section reflects ai-service's own enabled/disabled status rather than
 * assuming all three are always available.
 */

import { createFileRoute } from '@tanstack/react-router';
import { ProtectedRoute } from '@/components/layout/ProtectedRoute';
import { AiInsightsContent } from './-components/AiInsightsContent';

export const Route = createFileRoute('/_authenticated/ai-insights/')({
  component: AiInsightsPage,
});

function AiInsightsPage() {
  return (
    <ProtectedRoute requiredRoles={['ROLE_ADMIN']}>
      <AiInsightsContent />
    </ProtectedRoute>
  );
}
