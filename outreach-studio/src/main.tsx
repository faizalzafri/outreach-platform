import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { createRouter, RouterProvider } from '@tanstack/react-router';
import { routeTree } from './routeTree.gen';
import { PageSkeleton } from './components/feedback/PageSkeleton';
import './index.css';

// Preloading on intent fetches a page's code when its link is hovered; the skeleton shows while
// a page's code is still loading.
const router = createRouter({
  routeTree,
  defaultPreload: 'intent',
  defaultPendingComponent: () => <PageSkeleton />,
});

// Register the router instance for type safety
declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router;
  }
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <RouterProvider router={router} />
  </StrictMode>,
);
