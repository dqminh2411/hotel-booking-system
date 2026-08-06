import { useCallback, useEffect, useState } from 'react';
import { adminUserService } from '@/features/admin/services/adminUserService';
import { useDebounce } from '@/shared/hooks/useDebounce';
import { getApiErrorMessage } from '@/shared/api/getApiErrorMessage';

// Trung voi @Min(10) cua AdminController#getAllUsers - size khong duoc nho hon 10.
const PAGE_SIZE = 10;

/** Hook quan ly du lieu trang danh sach User: tim kiem + loc trang thai + phan trang. */
export function useAdminUsers() {
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [reloadTick, setReloadTick] = useState(0);

  const debouncedSearch = useDebounce(search, 400);

  // Doi tu khoa/trang thai loc thi quay ve trang dau tien.
  useEffect(() => {
    setPage(0);
  }, [debouncedSearch, status]);

  const fetchData = useCallback(() => {
    setIsLoading(true);
    setErrorMessage('');

    adminUserService
      .listUsers({ page, size: PAGE_SIZE, status: status || undefined, search: debouncedSearch })
      .then((res) => {
        setItems(res?.content ?? []);
        setTotalPages(res?.totalPages ?? 0);
        setTotalElements(res?.totalElements ?? 0);
      })
      .catch((err) => {
        setItems([]);
        setErrorMessage(getApiErrorMessage(err, 'Không thể tải danh sách người dùng. Vui lòng thử lại.'));
      })
      .finally(() => setIsLoading(false));
  }, [page, status, debouncedSearch]);

  useEffect(() => {
    fetchData();
  }, [fetchData, reloadTick]);

  const reload = useCallback(() => setReloadTick((t) => t + 1), []);

  return {
    items,
    page,
    setPage,
    totalPages,
    totalElements,
    isLoading,
    errorMessage,
    search,
    setSearch,
    status,
    setStatus,
    reload,
  };
}