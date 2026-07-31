import { useCallback, useEffect, useState } from 'react';
import { fetchHotelDetail } from '@/features/hotel/api/hotelApi';
import { getApiErrorMessage } from '@/shared/api/getApiErrorMessage';

/**
 * AdminController KHONG co endpoint rieng de liet ke anh (kem imageId) cua 1 hotel -
 * endpoint DELETE /api/admin/hotels/images chi nhan san imgIds can xoa.
 * De co imageId hien thi checkbox, man hinh quan ly anh tai su dung lai
 * GET /api/hotels/{hotelId} (public, dang dung cho HotelDetailPage/HotelGallery)
 * va doc field hotel.imageUrls, moi phan tu co { id, url }.
 */
export function useHotelImages(hotelId, enabled) {
  const [images, setImages] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const load = useCallback(() => {
    if (!hotelId || !enabled) return;
    setIsLoading(true);
    setErrorMessage('');

    fetchHotelDetail(hotelId)
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