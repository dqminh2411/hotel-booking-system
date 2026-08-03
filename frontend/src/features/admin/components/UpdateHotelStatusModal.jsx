import { useEffect, useState } from 'react';
import { Modal } from '@/shared/components/Modal/Modal';
import { Button } from '@/shared/components/Button/Button';
import { Textarea } from '@/shared/components/Textarea/Textarea';
import { HOTEL_UPDATABLE_STATUS } from '@/features/admin/constants/adminOptions';

/**
 * Popup duyet / tu choi khach san (PATCH /api/admin/hotels/{hotelId}/status).
 * targetStatus do nut bam tren bang quyet dinh truoc (Duyet -> APPROVED, Tu choi -> SUSPENDED),
 * khop voi AdminServiceImpl#updateHotelStatus (chi cho phep 2 trang thai nay).
 * reason la BAT BUOC khi targetStatus = SUSPENDED, dung theo validate cua backend:
 *   `if (request.hotelStatus() == HotelStatus.SUSPENDED && !StringUtils.hasText(request.reason()))`
 *
 * Props: isOpen, hotel, targetStatus ('APPROVED' | 'SUSPENDED'), isSubmitting, errorMessage,
 *        onClose, onSubmit(reason)
 */
export function UpdateHotelStatusModal({
  isOpen,
  hotel,
  targetStatus,
  isSubmitting,
  errorMessage,
  onClose,
  onSubmit,
}) {
  const [reason, setReason] = useState('');
  const [validationError, setValidationError] = useState('');
  const isSuspend = targetStatus === HOTEL_UPDATABLE_STATUS.SUSPENDED;

  useEffect(() => {
    if (isOpen) {
      setReason('');
      setValidationError('');
    }
  }, [isOpen, targetStatus]);

  const handleSubmit = () => {
    if (isSuspend && !reason.trim()) {
      setValidationError('Bắt buộc phải nhập lý do khi từ chối / đình chỉ khách sạn');
      return;
    }
    setValidationError('');
    onSubmit(reason);
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={isSuspend ? 'Từ chối khách sạn' : 'Duyệt khách sạn'}
      width={440}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
            Hủy
          </Button>
          <Button variant={isSuspend ? 'danger' : 'primary'} isLoading={isSubmitting} onClick={handleSubmit}>
            {isSuspend ? 'Xác nhận từ chối' : 'Xác nhận duyệt'}
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-4">
        {errorMessage && (
          <div className="rounded-md bg-red-50 px-4 py-3 text-sm text-red-700">{errorMessage}</div>
        )}
        <p className="text-sm text-slate-500">
          Khách sạn: <strong>{hotel?.name}</strong>
        </p>
        <Textarea
          label="Lý do"
          required={isSuspend}
          placeholder={
            isSuspend
              ? 'Ví dụ: Hồ sơ khách sạn chưa đầy đủ giấy tờ pháp lý...'
              : 'Không bắt buộc'
          }
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          error={validationError}
        />
        {!isSuspend && (
          <span className="text-xs text-slate-400">Lý do không bắt buộc khi duyệt khách sạn.</span>
        )}
      </div>
    </Modal>
  );
}