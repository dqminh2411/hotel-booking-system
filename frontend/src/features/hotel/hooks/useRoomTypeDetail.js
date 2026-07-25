import { useEffect, useState } from 'react';
import { fetchRoomTypeDetail } from '../api/roomTypeApi';
import { getApiErrorMessage } from '../../../shared/api/getApiErrorMessage';

export default function useRoomTypeDetail(roomTypeId) {
  const [roomType, setRoomType] = useState(null);
  const [status, setStatus] = useState('idle');
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    if (!roomTypeId) {
      setRoomType(null);
      setStatus('idle');
      return undefined;
    }

    let isCancelled = false;
    setStatus('loading');
    setErrorMessage('');

    fetchRoomTypeDetail(roomTypeId)
      .then((data) => {
        if (isCancelled) return;
        setRoomType(data);
        setStatus('success');
      })
      .catch((error) => {
        if (isCancelled) return;
        setErrorMessage(getApiErrorMessage(error, 'Không thể tải chi tiết loại phòng. Vui lòng thử lại.'));
        setStatus('error');
      });

    return () => {
      isCancelled = true;
    };
  }, [roomTypeId]);

  return { roomType, status, errorMessage };
}
