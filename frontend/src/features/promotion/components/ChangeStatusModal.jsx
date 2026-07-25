import { useEffect, useState } from 'react';
import { Modal } from '@/shared/components/Modal/Modal';
import { Button } from '@/shared/components/Button/Button';
import { Select } from '@/shared/components/Select/Select';
import { Textarea } from '@/shared/components/Textarea/Textarea';
import styles from './ChangeStatusModal.module.scss';
import { PROMOTION_STATUS_OPTIONS } from '@/features/promotion/constants/promotionOptions';

/**
 * Popup doi trang thai Promotion.
 * Field "Ly do" duoc gui len backend dung theo OpenAPI (ChangePromotionStatusRequest.reason),
 * du backend hien tai chua su dung field nay (xem promotionApi.changeStatus).
 *
 * Props: isOpen, currentStatus, isSubmitting, error, onClose, onSubmit(status, reason)
 */
export function ChangeStatusModal({
  isOpen,
  currentStatus,
  isSubmitting,
  error,
  onClose,
  onSubmit,
}) {
  const [status, setStatus] = useState(currentStatus ?? 'DRAFT');
  const [reason, setReason] = useState('');

  // Moi lan mo lai modal, dong bo status ve trang thai hien tai cua promotion.
  useEffect(() => {
    if (isOpen && currentStatus) {
      setStatus(currentStatus);
      setReason('');
    }
  }, [isOpen, currentStatus]);

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Doi trang thai Promotion"
      width={440}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
            Huy
          </Button>
          <Button variant="primary" isLoading={isSubmitting} onClick={() => onSubmit(status, reason)}>
            Xac nhan
          </Button>
        </>
      }
    >
      <div className={styles.form}>
        {error && (
          <div className={styles.error}>
            {error.status ? `Loi ${error.status}: ` : ''}
            {error.message}
          </div>
        )}
        <Select
          label="Trang thai moi"
          required
          options={PROMOTION_STATUS_OPTIONS}
          value={status}
          onChange={(e) => setStatus(e.target.value)}
        />
        <Textarea
          label="Ly do"
          placeholder="Vi du: Hotel owner tam dung chuong trinh."
          value={reason}
          onChange={(e) => setReason(e.target.value)}
        />
        <span className={styles.hint}>
          Ly do hien backend chua luu lai, nhung van duoc gui len de dung theo OpenAPI.
        </span>
      </div>
    </Modal>
  );
}
