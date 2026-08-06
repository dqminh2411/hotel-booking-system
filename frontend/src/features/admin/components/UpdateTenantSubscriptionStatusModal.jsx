import { useEffect, useState } from 'react';
import { Modal } from '@/shared/components/Modal/Modal';
import { Button } from '@/shared/components/Button/Button';
import { Select } from '@/shared/components/Select/Select';
import { TENANT_SUBSCRIPTION_STATUS_OPTIONS } from '@/features/admin/constants/tenantOptions';

/**
 * Modal doi trang thai 1 subscription cua tenant
 * (PATCH /api/admin/tenants/subscriptions/{id} - UpdateTenantSubscription { status }).
 *
 * Props: isOpen, subscription (row hien tai), isSubmitting, errorMessage, onClose, onSubmit(status)
 */
export function UpdateTenantSubscriptionStatusModal({ isOpen, subscription, isSubmitting, errorMessage, onClose, onSubmit }) {
  const [status, setStatus] = useState('ACTIVE');

  useEffect(() => {
    if (isOpen && subscription) {
      setStatus(subscription.status || 'ACTIVE');
    }
  }, [isOpen, subscription]);

  function handleSubmit() {
    onSubmit(status);
  }

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Cập nhật trạng thái gói dịch vụ"
      width={440}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
            Hủy
          </Button>
          <Button variant="primary" isLoading={isSubmitting} onClick={handleSubmit}>
            Lưu thay đổi
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-4">
        {errorMessage && (
          <div className="rounded-md bg-red-50 px-4 py-3 text-sm text-red-700">{errorMessage}</div>
        )}
        <p className="text-sm text-slate-500">
          Gói: <strong>{subscription?.planName}</strong>
        </p>
        <Select
          label="Trạng thái"
          options={TENANT_SUBSCRIPTION_STATUS_OPTIONS}
          value={status}
          onChange={(e) => setStatus(e.target.value)}
        />
      </div>
    </Modal>
  );
}