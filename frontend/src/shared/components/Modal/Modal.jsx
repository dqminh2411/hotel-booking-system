import { useEffect } from 'react';
import { createPortal } from 'react-dom';
import styles from './Modal.module.scss';

/**
 * Modal dung chung, dung cho ConfirmDialog, ChangeStatus popup, va cac popup sau nay.
 * Props: isOpen, onClose, title?, children, footer?, width?
 */
export function Modal({ isOpen, onClose, title, children, footer, width }) {
  useEffect(() => {
    if (!isOpen) return;
    const onKeyDown = (e) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', onKeyDown);
    return () => document.removeEventListener('keydown', onKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return createPortal(
    <div className={styles.overlay} onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div
        className={styles.modal}
        style={width ? { '--modal-width': `${width}px` } : undefined}
        role="dialog"
        aria-modal="true"
      >
        {title && (
          <div className={styles.header}>
            <h3 className={styles.title}>{title}</h3>
            <button type="button" className={styles.closeBtn} onClick={onClose} aria-label="Dong">
              ✕
            </button>
          </div>
        )}
        <div className={styles.body}>{children}</div>
        {footer && <div className={styles.footer}>{footer}</div>}
      </div>
    </div>,
    document.body,
  );
}
