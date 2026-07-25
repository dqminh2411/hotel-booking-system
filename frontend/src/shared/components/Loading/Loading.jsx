import styles from './Loading.module.scss';

export function Loading({ label = 'Dang tai du lieu...' }) {
  return (
    <div className={styles.wrap} role="status" aria-live="polite">
      <div className={styles.spinner} />
      <span>{label}</span>
    </div>
  );
}
