/**
 * Row-level action button for DataTable.
 *
 * Shows a loading spinner on the affected row without blocking other rows.
 * Works with the useRowAction hook for state management.
 */

import styles from './RowActionButton.module.css';

interface RowActionButtonProps {
  /** Whether this specific row action is loading */
  isLoading: boolean;
  /** Click handler for the action */
  onClick: () => void;
  /** Button label */
  label: string;
  /** Optional variant for styling (default, danger) */
  variant?: 'default' | 'danger';
  /** Whether the button is disabled for reasons other than loading */
  disabled?: boolean;
  /** Optional aria-label override */
  ariaLabel?: string;
}

export function RowActionButton({
  isLoading,
  onClick,
  label,
  variant = 'default',
  disabled = false,
  ariaLabel,
}: RowActionButtonProps) {
  return (
    <button
      type="button"
      className={`${styles['rowActionBtn']} ${styles[`rowActionBtn--${variant}`]}`}
      onClick={onClick}
      disabled={isLoading || disabled}
      aria-label={ariaLabel ?? label}
      aria-busy={isLoading}
    >
      {isLoading ? (
        <span className={styles['spinner']} aria-hidden="true" />
      ) : (
        label
      )}
    </button>
  );
}
