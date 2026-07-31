import { useCallback, useState } from 'react';
import { adminService } from '@/features/admin/services/adminService';
import { getApiErrorMessage } from '@/shared/api/getApiErrorMessage';

/** Hook gom cac thao tac ghi (duyet/tu choi hotel, xoa anh) + trang thai loading/error rieng. */
export function useAdminHotelMutations() {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const updateStatus = useCallback(async (hotelId, hotelStatus, reason) => {
    setIsSubmitting(true);
    setErrorMessage('');
    try {
      await adminService.updateHotelStatus(hotelId, hotelStatus, reason);
      return true;
    } catch (err) {
      setErrorMessage(
        getApiErrorMessage(err, 'Không thể cập nhật trạng thái khách sạn. Vui lòng thử lại.'),
      );
      return false;
    } finally {
      setIsSubmitting(false);
    }
  }, []);

  const deleteImages = useCallback(async (hotelId, imgIds) => {
    setIsSubmitting(true);
    setErrorMessage('');
    try {
      await adminService.deleteHotelImages(hotelId, imgIds);
      return true;
    } catch (err) {
      setErrorMessage(getApiErrorMessage(err, 'Không thể xóa ảnh. Vui lòng thử lại.'));
      return false;
    } finally {
      setIsSubmitting(false);
    }
  }, []);

  return {
    isSubmitting,
    errorMessage,
    updateStatus,
    deleteImages,
    clearError: () => setErrorMessage(''),
  };
}