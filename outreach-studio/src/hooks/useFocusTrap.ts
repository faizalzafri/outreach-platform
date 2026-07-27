/**
 * Focus Trap Hook
 *
 * Traps keyboard focus within a container element (e.g., modals/dialogs).
 * - Moves focus to the first focusable element on mount
 * - Cycles Tab/Shift+Tab within the container
 * - Closes on Escape key press
 * - Returns focus to the previously focused element on unmount
 */

import { useEffect, useRef, useCallback } from 'react';

const FOCUSABLE_SELECTOR = [
  'a[href]',
  'button:not([disabled])',
  'input:not([disabled])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(', ');

interface UseFocusTrapOptions {
  /** Whether the trap is active */
  active: boolean;
  /** Callback invoked when Escape is pressed */
  onEscape?: () => void;
}

export function useFocusTrap<T extends HTMLElement = HTMLElement>(
  options: UseFocusTrapOptions
) {
  const containerRef = useRef<T>(null);
  const previousFocusRef = useRef<HTMLElement | null>(null);

  const { active, onEscape } = options;

  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (!containerRef.current) return;

      if (e.key === 'Escape') {
        onEscape?.();
        return;
      }

      if (e.key !== 'Tab') return;

      const focusable = containerRef.current.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR);
      if (focusable.length === 0) return;

      const first = focusable[0]!;
      const last = focusable[focusable.length - 1]!;

      if (e.shiftKey) {
        if (document.activeElement === first) {
          e.preventDefault();
          last.focus();
        }
      } else {
        if (document.activeElement === last) {
          e.preventDefault();
          first.focus();
        }
      }
    },
    [onEscape]
  );

  useEffect(() => {
    if (!active) return;

    // Save the currently focused element to restore later
    previousFocusRef.current = document.activeElement as HTMLElement | null;

    // Move focus to the first focusable element within the container
    const timer = requestAnimationFrame(() => {
      if (!containerRef.current) return;
      const focusable = containerRef.current.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR);
      if (focusable.length > 0) {
        focusable[0]!.focus();
      }
    });

    document.addEventListener('keydown', handleKeyDown);

    return () => {
      cancelAnimationFrame(timer);
      document.removeEventListener('keydown', handleKeyDown);

      // Return focus to the element that was focused before the trap activated
      if (previousFocusRef.current && typeof previousFocusRef.current.focus === 'function') {
        previousFocusRef.current.focus();
      }
    };
  }, [active, handleKeyDown]);

  return containerRef;
}
