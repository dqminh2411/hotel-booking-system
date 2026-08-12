import { useCallback, useEffect, useState } from 'react';
import { adminService } from '@/features/admin/services/adminService';
import { getApiErrorMessage } from '@/shared/api/getApiErrorMessage';

// Trung voi @Min(10) cua AdminController#getListHotelPending - size khong duoc nho hon 10.
const PAGE_SIZE = 10;

/**
 * Hook quan ly du lieu cho trang danh sach khach san cho duyet: phan trang + reload.
 * Endpoint nay khong ho tro tim kiem/loc theo status (server luon loc cung HotelStatus.PENDING
 * va sap xep theo createdAt ASC), nen hook chi can quan ly page.
 */
export function usePendingHotels() {
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [reloadTick, setReloadTick] = useState(0);

  const fetchData = useCallback(() => {
    setIsLoading(true);
    setErrorMessage('');

    adminService
      .listPendingHotels({ page, size: PAGE_SIZE })
      .then((res) => {
        setItems(res?.content ?? []);
        setTotalPages(res?.totalPages ?? 0);
        setTotalElements(res?.totalElements ?? 0);
      })
      .catch((err) => {
        setItems([]);
        setErrorMessage(
          getApiErrorMessage(err, 'Không thể tải danh sách khách sạn chờ duyệt. Vui lòng thử lại.'),
        );
      })
      .finally(() => setIsLoading(false));
  }, [page]);

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
    reload,
  };
}