import styles from './PageHeader.module.scss';

/** Tieu de trang dung chung, dam bao moi trang co layout header dong nhat. */
export function PageHeader({ title, subtitle, actions }) {
  return (
    <div className={styles.wrap}>
      <div className={styles.titleGroup}>
        <h1 className={styles.title}>{title}</h1>
        {subtitle && <p className={styles.subtitle}>{subtitle}</p>}
      </div>
      {actions && <div className={styles.actions}>{actions}</div>}
    </div>
  );
}
