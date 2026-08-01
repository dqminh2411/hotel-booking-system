import { useCallback, useEffect, useState } from 'react';
import { adminApi } from '@/features/admin/api/adminApi';
import { getApiErrorMessage } from '@/shared/api/getApiErrorMessage';

/**
 * AdminController KHONG co endpoint rieng de liet ke anh (kem imageId) cua 1 hotel -
 * endpoint DELETE /api/admin/hotels/images chi nhan san imgIds can xoa.
 * De co imageId hien thi checkbox, man hinh quan ly anh tai su dung lai
 * GET /api/admin/hotels/{hotelId} va doc field hotel.imageUrls, moi phan tu
 * co { id, url }.
 *
 * Truoc day cho nay tai su dung tam GET /api/hotels/{hotelId} (public), nhung
 * endpoint do chi tra ve hotel co status = APPROVED nen bi loi voi hotel dang
 * PENDING/SUSPENDED. Da doi sang endpoint admin rieng o tren de sua triet de.
 */
export function useHotelImages(hotelId, enabled) {
  const [images, setImages] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const load = useCallback(() => {
    if (!hotelId || !enabled) return;
    setIsLoading(true);
    setErrorMessage('');

    adminApi
      .getHotelDetail(hotelId)
      .then((hotel) => setImages(hotel?.imageUrls ?? []))
      .catch((err) => {
        setImages([]);
        setErrorMessage(getApiErrorMessage(err, 'Không thể tải danh sách ảnh của khách sạn.'));
      })
      .finally(() => setIsLoading(false));
  }, [hotelId, enabled]);

  useEffect(() => {
    load();
  }, [load]);

  return { images, isLoading, errorMessage, reload: load };
}