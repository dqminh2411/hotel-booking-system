import { useEffect, useState } from 'react';
import { Modal } from '@/shared/components/Modal/Modal';
import { Button } from '@/shared/components/Button/Button';
import { Textarea } from '@/shared/components/Textarea/Textarea';

/**
 * Popup khoa tai khoan nguoi dung (PATCH /api/admin/users/{userId}/lock).
 * reason la BAT BUOC, dung theo LockRequest { @NotNull @NotBlank reason }.
 *
 * Props: isOpen, user, isSubmitting, errorMessage, onClose, onSubmit(reason)
 */
export function LockUserModal({ isOpen, user, isSubmitting, errorMessage, onClose, onSubmit }) {
  const [reason, setReason] = useState('');
  const [validationError, setValidationError] = useState('');

  useEffect(() => {
    if (isOpen) {
      setReason('');
      setValidationError('');
    }
  }, [isOpen]);

  function handleSubmit() {
    if (!reason.trim()) {
      setValidationError('Lý do không được bỏ trống khi khóa tài khoản');
      return;
    }
    setValidationError('');
    onSubmit(reason);
  }

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Khóa tài khoản người dùng"
      width={440}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
            Hủy
          </Button>
          <Button variant="danger" isLoading={isSubmitting} onClick={handleSubmit}>
            Xác nhận khóa
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-4">
        {errorMessage && (
          <div className="rounded-md bg-red-50 px-4 py-3 text-sm text-red-700">{errorMessage}</div>
        )}
        <p className="text-sm text-slate-500">
          Người dùng: <strong>{user?.fullname || user?.email}</strong>
        </p>
        <Textarea
          label="Lý do khóa"
          required
          placeholder="Ví dụ: Vi phạm điều khoản sử dụng dịch vụ..."
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          error={validationError}
        />
      </div>
    </Modal>
  );
}