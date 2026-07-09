import { useCallback, useEffect, useState } from 'react';
import { fetchHotelDetail } from '../api/hotelApi';
import { getApiErrorMessage } from '../../../shared/api/getApiErrorMessage';


export default function useHotelDetail(hotelId, { checkinDate, checkoutDate, guestNum, roomNum } = {}) {
  const [hotel, setHotel] = useState(null);
  const [status, setStatus] = useState('loading');
  const [errorMessage, setErrorMessage] = useState('');

  const loadHotelDetail = useCallback(async () => {
    if (!hotelId) return;

    setStatus('loading');
    setErrorMessage('');


    try {
      const data = await fetchHotelDetail(hotelId, { checkinDate, checkoutDate, guestNum, roomNum });
      setHotel(data);
      setStatus('success');
    } catch (error) {
      setHotel(null);
      setErrorMessage(getApiErrorMessage(error, 'Không thể tải thông tin khách sạn. Vui lòng thử lại.'));
      setStatus('error');
    }
  }, [hotelId, checkinDate, checkoutDate, guestNum, roomNum]);

  useEffect(() => {
    loadHotelDetail();
  }, [loadHotelDetail]);

  return { hotel, status, errorMessage, reload: loadHotelDetail };
}
