/**
 * PageSkeleton Component
 *
 * A loading skeleton displayed as the Suspense fallback while
 * route components are being lazily loaded via code splitting.
 * Mimics the general page layout to reduce layout shift.
 */

import type { CSSProperties } from 'react';

interface PageSkeletonProps {
  /** Optional title to hint what page is loading */
  title?: string;
}

const containerStyle: CSSProperties = {
  padding: '2rem',
  width: '100%',
  maxWidth: '1200px',
  margin: '0 auto',
};

const pulseKeyframes = `
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
`;

const skeletonBarStyle: CSSProperties = {
  background: 'var(--bg-muted)',
  borderRadius: '4px',
  animation: 'pulse 1.5s ease-in-out infinite',
};

export function PageSkeleton({ title }: PageSkeletonProps) {
  return (
    <div style={containerStyle} role="status" aria-label={title ? `Loading ${title}` : 'Loading page'}>
      {/* Inject keyframes */}
      <style>{pulseKeyframes}</style>

      {/* Title skeleton */}
      <div style={{ ...skeletonBarStyle, width: '200px', height: '32px', marginBottom: '1.5rem' }} />

      {/* Description skeleton */}
      <div style={{ ...skeletonBarStyle, width: '60%', height: '16px', marginBottom: '0.75rem' }} />
      <div style={{ ...skeletonBarStyle, width: '40%', height: '16px', marginBottom: '2rem' }} />

      {/* Table header skeleton */}
      <div style={{ display: 'flex', gap: '1rem', marginBottom: '1rem' }}>
        <div style={{ ...skeletonBarStyle, flex: 1, height: '40px' }} />
        <div style={{ ...skeletonBarStyle, width: '120px', height: '40px' }} />
      </div>

      {/* Table rows skeleton */}
      {Array.from({ length: 5 }).map((_, i) => (
        <div
          key={i}
          style={{
            ...skeletonBarStyle,
            width: '100%',
            height: '48px',
            marginBottom: '0.5rem',
            animationDelay: `${i * 0.1}s`,
          }}
        />
      ))}
    </div>
  );
}
