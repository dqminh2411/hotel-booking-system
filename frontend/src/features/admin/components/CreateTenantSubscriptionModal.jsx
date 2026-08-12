import { useEffect, useState } from 'react';
import { Modal } from '@/shared/components/Modal/Modal';
import { Button } from '@/shared/components/Button/Button';
import { Select } from '@/shared/components/Select/Select';
import { useSubscriptionPlanOptions } from '@/features/admin/hooks/useSubscriptionPlanOptions';

/**
 * Modal dang ky goi dich vu moi cho 1 tenant
 * (POST /api/admin/tenants/subscriptions - CreateTenantSubscriptionRequest { tenantId, subscriptionId }).
 *
 * Props: isOpen, tenantId, tenantName, isSubmitting, errorMessage, onClose, onSubmit(subscriptionId)
 */
export function CreateTenantSubscriptionModal({ isOpen, tenantId, tenantName, isSubmitting, errorMessage, onClose, onSubmit }) {
  const { options, status, errorMessage: plansError, reload } = useSubscriptionPlanOptions();
  const [subscriptionId, setSubscriptionId] = useState('');
  const [validationError, setValidationError] = useState('');

  useEffect(() => {
    if (isOpen) {
      setSubscriptionId('');
      setValidationError('');
      reload();
    }
  }, [isOpen, reload]);

  function handleSubmit() {
    if (!subscriptionId) {
      setValidationError('Vui lòng chọn một gói dịch vụ');
      return;
    }
    setValidationError('');
    onSubmit(subscriptionId);
  }

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Đăng ký gói dịch vụ"
      width={480}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
            Hủy
          </Button>
          <Button variant="primary" isLoading={isSubmitting} onClick={handleSubmit} disabled={status === 'loading'}>
            Đăng ký
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-4">
        {errorMessage && (
          <div className="rounded-md bg-red-50 px-4 py-3 text-sm text-red-700">{errorMessage}</div>
        )}
        {status === 'error' && (
          <div className="rounded-md bg-red-50 px-4 py-3 text-sm text-red-700">{plansError}</div>
        )}
        <p className="text-sm text-slate-500">
          Tenant: <strong>{tenantName}</strong>
        </p>

        {status === 'loading' ? (
          <p className="text-sm text-slate-400">Đang tải danh sách gói dịch vụ...</p>
        ) : (
          <Select
            label="Gói dịch vụ"
            required
            placeholder="-- Chọn gói dịch vụ --"
            options={options}
            value={subscriptionId}
            onChange={(e) => setSubscriptionId(e.target.value)}
            error={validationError}
          />
        )}
      </div>
    </Modal>
  );
}