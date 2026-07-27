import { useCallback, useEffect, useState } from 'react';
import { promotionService } from '@/features/promotion/services/promotionService';

/** Hook lay chi tiet 1 Promotion theo id - dung cho Detail va Edit page. */
export function usePromotion(id) {
  const [data, setData] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [reloadTick, setReloadTick] = useState(0);

  const fetchData = useCallback(() => {
    if (!id) return;
    setIsLoading(true);
    setError(null);

    promotionService
      .getById(id)
      .then(setData)
      .catch((err) => setError(err))
      .finally(() => setIsLoading(false));
  }, [id]);

  useEffect(() => {
    fetchData();
  }, [fetchData, reloadTick]);

  const reload = useCallback(() => setReloadTick((t) => t + 1), []);

  return { data, isLoading, error, reload };
}
