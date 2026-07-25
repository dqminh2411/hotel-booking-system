import styles from './EmptyState.module.scss';

export function EmptyState({ message = 'Không có dữ liệu', icon, action }) {
  return (
    <div className={styles.wrap}>
      <div className={styles.icon}>{icon ?? 'Ø'}</div>
      <div className={styles.message}>{message}</div>
      {action && <div className={styles.action}>{action}</div>}
    </div>
  );
}
