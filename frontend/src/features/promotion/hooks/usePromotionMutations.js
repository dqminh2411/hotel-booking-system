import { useCallback, useState } from 'react';
import { promotionService } from '@/features/promotion/services/promotionService';

/** Hook gom cac thao tac ghi (create/update/changeStatus/delete) + trang thai loading/error rieng. */
export function usePromotionMutations() {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const create = useCallback(async (payload) => {
    setIsSubmitting(true);
    setError(null);
    try {
      return await promotionService.create(payload);
    } catch (err) {
      setError(err);
      return null;
    } finally {
      setIsSubmitting(false);
    }
  }, []);

  const update = useCallback(async (id, payload) => {
    setIsSubmitting(true);
    setError(null);
    try {
      return await promotionService.update(id, payload);
    } catch (err) {
      setError(err);
      return null;
    } finally {
      setIsSubmitting(false);
    }
  }, []);

  const changeStatus = useCallback(async (id, status, reason) => {
    setIsSubmitting(true);
    setError(null);
    try {
      return await promotionService.changeStatus(id, status, reason);
    } catch (err) {
      setError(err);
      return null;
    } finally {
      setIsSubmitting(false);
    }
  }, []);

  const remove = useCallback(async (id) => {
    setIsSubmitting(true);
    setError(null);
    try {
      await promotionService.remove(id);
      return true;
    } catch (err) {
      setError(err);
      return false;
    } finally {
      setIsSubmitting(false);
    }
  }, []);

  return { isSubmitting, error, create, update, changeStatus, remove, clearError: () => setError(null) };
}
