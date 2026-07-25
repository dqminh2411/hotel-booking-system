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
      title="Đổi trạng thái Promotion"
      width={440}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
            Hủy
          </Button>
          <Button variant="primary" isLoading={isSubmitting} onClick={() => onSubmit(status, reason)}>
            Xác nhận
          </Button>
        </>
      }
    >
      <div className={styles.form}>
        {error && (
          <div className={styles.error}>
            {error.status ? `Lỗi ${error.status}: ` : ''}
            {error.message}
          </div>
        )}
        <Select
          label="Trạng thái mới"
          required
          options={PROMOTION_STATUS_OPTIONS}
          value={status}
          onChange={(e) => setStatus(e.target.value)}
        />
        <Textarea
          label="Lý do"
          placeholder="Ví dụ: Chủ khách sạn tạm dừng chương trình."
          value={reason}
          onChange={(e) => setReason(e.target.value)}
        />
        <span className={styles.hint}>
          Lý do hiện backend chưa lưu lại, nhưng vẫn được gửi lên để đúng theo OpenAPI.
        </span>
      </div>
    </Modal>
  );
}
