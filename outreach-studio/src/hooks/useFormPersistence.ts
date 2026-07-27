/**
 * Form Persistence Hook
 *
 * Preserves user form input in session storage so data is not lost
 * on mutation failure or page errors. Clears on successful submission.
 */

import { useCallback, useRef } from 'react';

const SESSION_KEY_PREFIX = 'outreach-form-';

export interface UseFormPersistenceReturn<T> {
  /** Load previously saved form data, if any */
  load: () => T | null;
  /** Save current form data to session storage */
  save: (data: T) => void;
  /** Clear saved form data (call on successful submission) */
  clear: () => void;
}

/**
 * Provides save/load/clear operations for form data persistence.
 * Uses sessionStorage so data survives within the session but not across tabs.
 *
 * @param formId Unique identifier for the form (e.g., 'feedback-create', 'event-create')
 */
export function useFormPersistence<T>(formId: string): UseFormPersistenceReturn<T> {
  const keyRef = useRef(`${SESSION_KEY_PREFIX}${formId}`);

  const load = useCallback((): T | null => {
    try {
      const stored = sessionStorage.getItem(keyRef.current);
      if (!stored) return null;
      return JSON.parse(stored) as T;
    } catch {
      return null;
    }
  }, []);

  const save = useCallback((data: T): void => {
    try {
      sessionStorage.setItem(keyRef.current, JSON.stringify(data));
    } catch {
      // Silently fail if storage is full or unavailable
    }
  }, []);

  const clear = useCallback((): void => {
    try {
      sessionStorage.removeItem(keyRef.current);
    } catch {
      // Silently fail
    }
  }, []);

  return { load, save, clear };
}
