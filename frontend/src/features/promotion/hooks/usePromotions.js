import { useCallback, useEffect, useState } from 'react';
import { promotionService } from '@/features/promotion/services/promotionService';
import { useDebounce } from '@/shared/hooks/useDebounce';

const PAGE_SIZE = 10;

/** Hook quan ly du lieu cho trang danh sach Promotion: search + filter + pagination. */
export function usePromotions() {
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState('');
  const [reloadTick, setReloadTick] = useState(0);

  const debouncedKeyword = useDebounce(keyword, 400);

  // Moi lan doi keyword/status thi quay ve trang dau tien.
  useEffect(() => {
    setPage(0);
  }, [debouncedKeyword, status]);

  const fetchData = useCallback(() => {
    setIsLoading(true);
    setError(null);

    promotionService
      .list({
        keyword: debouncedKeyword || undefined,
        status: status || undefined,
        page,
        size: PAGE_SIZE,
      })
      .then((res) => {
        setItems(res.content);
        setTotalPages(res.totalPages);
        setTotalElements(res.totalElements);
      })
      .catch((err) => setError(err))
      .finally(() => setIsLoading(false));
  }, [debouncedKeyword, status, page]);

  useEffect(() => {
    fetchData();
  }, [fetchData, reloadTick]);

  const reload = useCallback(() => setReloadTick((t) => t + 1), []);

  return {
    items,
    page,
    totalPages,
    totalElements,
    isLoading,
    error,
    keyword,
    setKeyword,
    status,
    setStatus,
    setPage,
    reload,
  };
}
