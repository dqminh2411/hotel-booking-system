import { Modal } from '@/shared/components/Modal/Modal';
import { Button } from '@/shared/components/Button/Button';

/** Dialog xac nhan dung chung, vi du: xoa Promotion. */
export function ConfirmDialog({
  isOpen,
  title = 'Xac nhan',
  message,
  confirmLabel = 'Dong y',
  cancelLabel = 'Huy',
  isLoading,
  danger,
  onConfirm,
  onCancel,
}) {
  return (
    <Modal
      isOpen={isOpen}
      onClose={onCancel}
      title={title}
      width={420}
      footer={
        <>
          <Button variant="secondary" onClick={onCancel} disabled={isLoading}>
            {cancelLabel}
          </Button>
          <Button variant={danger ? 'danger' : 'primary'} onClick={onConfirm} isLoading={isLoading}>
            {confirmLabel}
          </Button>
        </>
      }
    >
      <p>{message}</p>
    </Modal>
  );
}
