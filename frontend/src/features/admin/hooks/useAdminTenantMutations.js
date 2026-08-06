import { useCallback, useState } from 'react';
import { adminTenantService } from '@/features/admin/services/adminTenantService';
import { getApiErrorMessage } from '@/shared/api/getApiErrorMessage';

/** Hook gom cac thao tac ghi cho Tenant: tao / sua (ten, trang thai) / xóa mềm. */
export function useAdminTenantMutations() {
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

  const createTenant = useCallback(
    (payload) => run(() => adminTenantService.createTenant(payload), 'Không thể tạo tenant. Vui lòng thử lại.'),
    [run],
  );

  const updateTenant = useCallback(
    (tenantId, payload) =>
      run(() => adminTenantService.updateTenant(tenantId, payload), 'Không thể cập nhật tenant. Vui lòng thử lại.'),
    [run],
  );

  const deleteTenant = useCallback(
    (tenantId) => run(() => adminTenantService.deleteTenant(tenantId), 'Không thể xóa tenant. Vui lòng thử lại.'),
    [run],
  );

  return {
    isSubmitting,
    errorMessage,
    createTenant,
    updateTenant,
    deleteTenant,
    clearError: () => setErrorMessage(''),
  };
}