import { useCallback, useEffect, useState } from 'react';
import { adminTenantSubscriptionService } from '@/features/admin/services/adminTenantSubscriptionService';
import { getApiErrorMessage } from '@/shared/api/getApiErrorMessage';

// Trung voi @Min(10) cua TenantSubscriptionController#getSubscriptionTenant.
const PAGE_SIZE = 10;

/** Hook quan ly du lieu lich su goi (subscription) cua 1 tenant: phan trang + reload. */
export function useTenantSubscriptions(tenantId) {
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [reloadTick, setReloadTick] = useState(0);

  const fetchData = useCallback(() => {
    if (!tenantId) return;

    setIsLoading(true);
    setErrorMessage('');

    adminTenantSubscriptionService
      .listTenantSubscriptions(tenantId, { page, size: PAGE_SIZE })
      .then((res) => {
        setItems(res?.content ?? []);
        setTotalPages(res?.totalPages ?? 0);
        setTotalElements(res?.totalElements ?? 0);
      })
      .catch((err) => {
        setItems([]);
        setErrorMessage(getApiErrorMessage(err, 'Không thể tải lịch sử gói dịch vụ. Vui lòng thử lại.'));
      })
      .finally(() => setIsLoading(false));
  }, [tenantId, page]);

  useEffect(() => {
    fetchData();
  }, [fetchData, reloadTick]);

  const reload = useCallback(() => setReloadTick((t) => t + 1), []);

  return { items, page, setPage, totalPages, totalElements, isLoading, errorMessage, reload };
}