import { describe, it, expect, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useToast } from '../useToast';
import { useUIStore } from '@/stores/ui-store';

describe('useToast', () => {
  beforeEach(() => {
    // Reset store to clean slate
    useUIStore.setState({ toasts: [] });
  });

  it('enqueues a toast with the toast() method', () => {
    const { result } = renderHook(() => useToast());

    act(() => {
      result.current.toast({ severity: 'info', message: 'Hello' });
    });

    const toasts = useUIStore.getState().toasts;
    expect(toasts).toHaveLength(1);
    expect(toasts[0].severity).toBe('info');
    expect(toasts[0].message).toBe('Hello');
  });

  it('enqueues a success toast via the success() helper', () => {
    const { result } = renderHook(() => useToast());

    act(() => {
      result.current.success('Operation completed');
    });

    const toasts = useUIStore.getState().toasts;
    expect(toasts).toHaveLength(1);
    expect(toasts[0].severity).toBe('success');
    expect(toasts[0].message).toBe('Operation completed');
  });

  it('enqueues an error toast with correlationId via the error() helper', () => {
    const { result } = renderHook(() => useToast());

    act(() => {
      result.current.error('Something went wrong', 'corr-id-456');
    });

    const toasts = useUIStore.getState().toasts;
    expect(toasts).toHaveLength(1);
    expect(toasts[0].severity).toBe('error');
    expect(toasts[0].message).toBe('Something went wrong');
    expect(toasts[0].correlationId).toBe('corr-id-456');
  });

  it('enqueues an error toast without correlationId', () => {
    const { result } = renderHook(() => useToast());

    act(() => {
      result.current.error('Failed request');
    });

    const toasts = useUIStore.getState().toasts;
    expect(toasts).toHaveLength(1);
    expect(toasts[0].severity).toBe('error');
    expect(toasts[0].correlationId).toBeUndefined();
  });

  it('enqueues a warning toast via the warning() helper', () => {
    const { result } = renderHook(() => useToast());

    act(() => {
      result.current.warning('Rate limited');
    });

    const toasts = useUIStore.getState().toasts;
    expect(toasts).toHaveLength(1);
    expect(toasts[0].severity).toBe('warning');
    expect(toasts[0].message).toBe('Rate limited');
  });

  it('enqueues an info toast via the info() helper', () => {
    const { result } = renderHook(() => useToast());

    act(() => {
      result.current.info('New update available');
    });

    const toasts = useUIStore.getState().toasts;
    expect(toasts).toHaveLength(1);
    expect(toasts[0].severity).toBe('info');
    expect(toasts[0].message).toBe('New update available');
  });

  it('dismisses a toast by id', () => {
    const { result } = renderHook(() => useToast());

    act(() => {
      result.current.success('First');
      result.current.error('Second');
    });

    const toasts = useUIStore.getState().toasts;
    expect(toasts).toHaveLength(2);

    act(() => {
      result.current.dismiss(toasts[0].id);
    });

    const remaining = useUIStore.getState().toasts;
    expect(remaining).toHaveLength(1);
    expect(remaining[0].message).toBe('Second');
  });

  it('supports enqueueing multiple toasts in sequence', () => {
    const { result } = renderHook(() => useToast());

    act(() => {
      result.current.success('One');
      result.current.warning('Two');
      result.current.info('Three');
    });

    const toasts = useUIStore.getState().toasts;
    expect(toasts).toHaveLength(3);
    expect(toasts.map((t) => t.severity)).toEqual(['success', 'warning', 'info']);
  });
});
