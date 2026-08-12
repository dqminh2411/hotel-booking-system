import { useCallback, useState } from 'react';
import { adminTenantSubscriptionService } from '@/features/admin/services/adminTenantSubscriptionService';
import { getApiErrorMessage } from '@/shared/api/getApiErrorMessage';

/** Hook gom cac thao tac ghi cho Subscription cua 1 tenant: dang ky moi / doi trang thai / xoa. */
export function useTenantSubscriptionMutations() {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const run = useCallback(async (action, fallbackMessage) => {
    setIsSubmitting(true);
    setErrorMessage('');
    try {
      await action();
      return true;
    } catch (err) {
      setErrorMessage(getApiErrorMessage(err, fallbackMessage));
      return false;
    } finally {
      setIsSubmitting(false);
    }
  }, []);

  const createSubscription = useCallback(
    (tenantId, subscriptionId) =>
      run(
        () => adminTenantSubscriptionService.createTenantSubscription(tenantId, subscriptionId),
        'Không thể đăng ký gói dịch vụ. Vui lòng thử lại.',
      ),
    [run],
  );

  const updateStatus = useCallback(
    (id, status) =>
      run(
        () => adminTenantSubscriptionService.updateStatus(id, status),
        'Không thể cập nhật trạng thái gói. Vui lòng thử lại.',
      ),
    [run],
  );

  const deleteSubscription = useCallback(
    (id) =>
      run(
        () => adminTenantSubscriptionService.deleteTenantSubscription(id),
        'Không thể xóa gói dịch vụ. Vui lòng thử lại.',
      ),
    [run],
  );

  return {
    isSubmitting,
    errorMessage,
    createSubscription,
    updateStatus,
    deleteSubscription,
    clearError: () => setErrorMessage(''),
  };
}