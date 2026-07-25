import styles from './EmptyState.module.scss';

export function EmptyState({ message = 'Khong co du lieu', icon, action }) {
  return (
    <div className={styles.wrap}>
      <div className={styles.icon}>{icon ?? 'Ø'}</div>
      <div className={styles.message}>{message}</div>
      {action && <div className={styles.action}>{action}</div>}
    </div>
  );
}
