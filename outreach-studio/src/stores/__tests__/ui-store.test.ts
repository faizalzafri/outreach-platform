import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useUIStore } from '../ui-store';

describe('UI Store', () => {
  beforeEach(() => {
    // Reset Zustand store to its initial state between tests
    useUIStore.setState({
      sidebarCollapsed: false,
      theme: 'system',
      toasts: [],
      isOffline: false,
    });
    localStorage.clear();
  });

  describe('Sidebar', () => {
    it('starts with sidebar expanded (not collapsed)', () => {
      const state = useUIStore.getState();
      expect(state.sidebarCollapsed).toBe(false);
    });

    it('toggles sidebar collapsed state', () => {
      useUIStore.getState().toggleSidebar();
      expect(useUIStore.getState().sidebarCollapsed).toBe(true);

      useUIStore.getState().toggleSidebar();
      expect(useUIStore.getState().sidebarCollapsed).toBe(false);
    });

    it('persists sidebar state to localStorage', () => {
      useUIStore.getState().toggleSidebar();

      // The persist middleware writes to localStorage under 'outreach-ui-state'
      const stored = localStorage.getItem('outreach-ui-state');
      expect(stored).not.toBeNull();
      const parsed = JSON.parse(stored!);
      expect(parsed.state.sidebarCollapsed).toBe(true);
    });
  });

  describe('Theme', () => {
    it('defaults to "system" theme', () => {
      const state = useUIStore.getState();
      expect(state.theme).toBe('system');
    });

    it('sets theme to "dark"', () => {
      useUIStore.getState().setTheme('dark');
      expect(useUIStore.getState().theme).toBe('dark');
    });

    it('sets theme to "light"', () => {
      useUIStore.getState().setTheme('light');
      expect(useUIStore.getState().theme).toBe('light');
    });

    it('persists theme to localStorage', () => {
      useUIStore.getState().setTheme('dark');

      const stored = localStorage.getItem('outreach-ui-state');
      expect(stored).not.toBeNull();
      const parsed = JSON.parse(stored!);
      expect(parsed.state.theme).toBe('dark');
    });

    it('falls back to defaults when localStorage contains invalid theme', () => {
      // Simulate corrupted localStorage data
      localStorage.setItem(
        'outreach-ui-state',
        JSON.stringify({ state: { theme: 'invalid-value', sidebarCollapsed: 'not-a-boolean' }, version: 0 }),
      );

      // Re-create the store by calling persist rehydration
      useUIStore.persist.rehydrate();

      const state = useUIStore.getState();
      // The merge function should reject invalid values and use defaults
      expect(state.theme).toBe('system');
      expect(state.sidebarCollapsed).toBe(false);
    });

    it('falls back to defaults when localStorage is unavailable', () => {
      // Mock localStorage to throw on getItem
      const originalGetItem = Storage.prototype.getItem;
      Storage.prototype.getItem = vi.fn(() => {
        throw new Error('Storage unavailable');
      });

      useUIStore.persist.rehydrate();

      const state = useUIStore.getState();
      expect(state.theme).toBe('system');
      expect(state.sidebarCollapsed).toBe(false);

      Storage.prototype.getItem = originalGetItem;
    });
  });

  describe('Toasts', () => {
    it('starts with an empty toast queue', () => {
      expect(useUIStore.getState().toasts).toEqual([]);
    });

    it('adds a toast with generated id and timestamp', () => {
      useUIStore.getState().addToast({ severity: 'success', message: 'Done!' });

      const toasts = useUIStore.getState().toasts;
      expect(toasts).toHaveLength(1);
      expect(toasts[0].severity).toBe('success');
      expect(toasts[0].message).toBe('Done!');
      expect(toasts[0].id).toBeDefined();
      expect(toasts[0].timestamp).toBeGreaterThan(0);
    });

    it('adds multiple toasts preserving order', () => {
      useUIStore.getState().addToast({ severity: 'info', message: 'First' });
      useUIStore.getState().addToast({ severity: 'warning', message: 'Second' });
      useUIStore.getState().addToast({ severity: 'error', message: 'Third' });

      const toasts = useUIStore.getState().toasts;
      expect(toasts).toHaveLength(3);
      expect(toasts[0].message).toBe('First');
      expect(toasts[1].message).toBe('Second');
      expect(toasts[2].message).toBe('Third');
    });

    it('dismisses a toast by id', () => {
      useUIStore.getState().addToast({ severity: 'success', message: 'Keep' });
      useUIStore.getState().addToast({ severity: 'error', message: 'Remove' });

      const toasts = useUIStore.getState().toasts;
      const removeId = toasts[1].id;

      useUIStore.getState().dismissToast(removeId);

      const remaining = useUIStore.getState().toasts;
      expect(remaining).toHaveLength(1);
      expect(remaining[0].message).toBe('Keep');
    });

    it('does nothing when dismissing a non-existent id', () => {
      useUIStore.getState().addToast({ severity: 'info', message: 'Stays' });
      useUIStore.getState().dismissToast('non-existent-id');

      expect(useUIStore.getState().toasts).toHaveLength(1);
    });

    it('enforces max queue size of 50 by dropping oldest toasts', () => {
      // Add 55 toasts
      for (let i = 0; i < 55; i++) {
        useUIStore.getState().addToast({ severity: 'info', message: `Toast ${i}` });
      }

      const toasts = useUIStore.getState().toasts;
      expect(toasts).toHaveLength(50);
      // The oldest 5 should have been dropped, so first remaining is Toast 5
      expect(toasts[0].message).toBe('Toast 5');
      expect(toasts[49].message).toBe('Toast 54');
    });

    it('includes correlationId on error toasts when provided', () => {
      useUIStore.getState().addToast({
        severity: 'error',
        message: 'Something broke',
        correlationId: 'abc-123-def',
      });

      const toast = useUIStore.getState().toasts[0];
      expect(toast.correlationId).toBe('abc-123-def');
    });

    it('does not persist toasts to localStorage', () => {
      useUIStore.getState().addToast({ severity: 'info', message: 'Ephemeral' });

      const stored = localStorage.getItem('outreach-ui-state');
      if (stored) {
        const parsed = JSON.parse(stored);
        expect(parsed.state.toasts).toBeUndefined();
      }
    });
  });

  describe('Offline', () => {
    it('defaults to online (isOffline = false)', () => {
      expect(useUIStore.getState().isOffline).toBe(false);
    });

    it('sets offline state', () => {
      useUIStore.getState().setOffline(true);
      expect(useUIStore.getState().isOffline).toBe(true);

      useUIStore.getState().setOffline(false);
      expect(useUIStore.getState().isOffline).toBe(false);
    });
  });
});
