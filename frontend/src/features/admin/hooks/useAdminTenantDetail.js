import { useCallback, useEffect, useState } from 'react';
import { adminTenantService } from '@/features/admin/services/adminTenantService';
import { getApiErrorMessage } from '@/shared/api/getApiErrorMessage';

/**
 * Nap chi tiet 1 tenant (GET /api/admin/tenants/{tenantId}) - tra ve TenantDetailResponse,
 * co them "activeSubscription" (goi dang dung, neu co) so voi TenantResponse (danh sach).
 * Dung lam header cho trang Quan ly Subscriptions cua 1 tenant.
 */
export function useAdminTenantDetail(tenantId) {
  const [tenant, setTenant] = useState(null);
  const [status, setStatus] = useState('loading'); // loading | success | error
  const [errorMessage, setErrorMessage] = useState('');

  const load = useCallback(async () => {
    if (!tenantId) return;

    setStatus('loading');
    setErrorMessage('');
    try {
      const data = await adminTenantService.getTenantDetail(tenantId);
      setTenant(data);
      setStatus('success');
    } catch (error) {
      setTenant(null);
      setErrorMessage(getApiErrorMessage(error, 'Không thể tải thông tin tenant. Vui lòng thử lại.'));
      setStatus('error');
    }
  }, [tenantId]);

  useEffect(() => {
    load();
  }, [load]);

  return { tenant, status, errorMessage, reload: load };
}