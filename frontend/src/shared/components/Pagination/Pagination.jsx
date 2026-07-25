import styles from './Pagination.module.scss';

/**
 * Phan trang dung chung, khop voi PageResponse tra ve tu backend (page 0-based).
 * Props: page (0-based), totalPages, totalElements, onPageChange(page)
 */
export function Pagination({ page, totalPages, totalElements, onPageChange }) {
  if (totalPages <= 1) return null;

  const pages = buildPageList(page, totalPages);

  return (
    <div className={styles.wrap}>
      <span className={styles.info}>
        Trang {page + 1}/{totalPages} - {totalElements} kết quả
      </span>
      <div className={styles.controls}>
        <button
          type="button"
          className={styles.pageBtn}
          disabled={page === 0}
          onClick={() => onPageChange(page - 1)}
          aria-label="Trang truoc"
        >
          ‹
        </button>
        {pages.map((p, idx) =>
          p === '...' ? (
            <span key={`dots-${idx}`} style={{ padding: '0 4px' }}>
              …
            </span>
          ) : (
            <button
              key={p}
              type="button"
              className={[styles.pageBtn, p === page ? styles.active : ''].join(' ')}
              onClick={() => onPageChange(p)}
            >
              {p + 1}
            </button>
          ),
        )}
        <button
          type="button"
          className={styles.pageBtn}
          disabled={page >= totalPages - 1}
          onClick={() => onPageChange(page + 1)}
          aria-label="Trang sau"
        >
          ›
        </button>
      </div>
    </div>
  );
}

function buildPageList(current, total) {
  const delta = 1;
  const range = [];
  const start = Math.max(0, current - delta);
  const end = Math.min(total - 1, current + delta);

  if (start > 0) {
    range.push(0);
    if (start > 1) range.push('...');
  }
  for (let i = start; i <= end; i++) range.push(i);
  if (end < total - 1) {
    if (end < total - 2) range.push('...');
    range.push(total - 1);
  }
  return range;
}
