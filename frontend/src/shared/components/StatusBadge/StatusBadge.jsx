import styles from './StatusBadge.module.scss';

/**
 * Badge trang thai dung chung (Promotion status, Coupon status...).
 * tone: 'success' | 'warning' | 'danger' | 'info' | 'neutral'
 */
export function StatusBadge({ label, tone }) {
  return (
    <span className={[styles.badge, styles[tone]].join(' ')}>
      <span className={styles.dot} />
      {label}
    </span>
  );
}
