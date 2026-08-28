import { useCallback, useEffect, useState } from 'react';
import { adminApi } from '@/features/admin/api/adminApi';
import { getApiErrorMessage } from '@/shared/api/getApiErrorMessage';

/**
 * Giong useHotelDetail (features/hotel/hooks) ve mat shape, nhung goi
 * GET /api/admin/hotels/{hotelId} thay vi GET /api/hotels/{hotelId} public.
 *
 * Ly do can hook rieng: hotel-service loc GET /api/hotels/{hotelId} theo
 * status = APPROVED, nen AdminHotelDetailPage (can xem ca hotel PENDING/SUSPENDED
 * de duyet/tu choi) se bi 404 neu dung lai useHotelDetail cu. Endpoint admin
 * khong nhan checkinDate/checkoutDate/guestNum/roomNum vi day la man hinh
 * CHI XEM, khong dat phong.
 */
export function useAdminHotelDetail(hotelId) {
  const [hotel, setHotel] = useState(null);
  const [status, setStatus] = useState('loading');
  const [errorMessage, setErrorMessage] = useState('');

  const loadHotelDetail = useCallback(async () => {
    if (!hotelId) return;

    setStatus('loading');
    setErrorMessage('');

    try {
      const data = await adminApi.getHotelDetail(hotelId);
      setHotel(data);
      setStatus('success');
    } catch (error) {
      setHotel(null);
      setErrorMessage(getApiErrorMessage(error, 'Không thể tải thông tin khách sạn. Vui lòng thử lại.'));
      setStatus('error');
    }
  }, [hotelId]);

  useEffect(() => {
    loadHotelDetail();
  }, [loadHotelDetail]);

  return { hotel, status, errorMessage, reload: loadHotelDetail };
}