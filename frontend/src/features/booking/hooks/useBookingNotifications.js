import { useCallback, useEffect, useRef, useState } from 'react';
import { listenForegroundMessages, requestFcmToken } from '../../../firebase';
import { setStoredFcmToken } from '../../auth/services/authStorage';
import { upsertDeviceToken } from '../api/notificationApi';
import {
  getBookingNotificationBody,
  getBookingNotificationTitle,
  getBookingStatusFromNotification,
  showBrowserNotification,
} from '../utils/bookingUtils';

export default function useBookingNotifications({ user, activeBookingId, onBookingUpdate }) {
  const [pushStatus, setPushStatus] = useState('IDLE');
  const [pushError, setPushError] = useState('');
  const [foregroundMessage, setForegroundMessage] = useState(null);
  const activeBookingIdRef = useRef(activeBookingId || '');
  const onBookingUpdateRef = useRef(onBookingUpdate);

  useEffect(() => {
    activeBookingIdRef.current = activeBookingId || '';
  }, [activeBookingId]);

  useEffect(() => {
    onBookingUpdateRef.current = onBookingUpdate;
  }, [onBookingUpdate]);

  const enablePushNotifications = useCallback(async () => {
    const userId = user?.userId || user?.id;
    if (!userId) return;

    setPushStatus('REGISTERING');
    setPushError('');

    try {
      const fcmToken = await requestFcmToken();
      await upsertDeviceToken(userId, fcmToken);
      setStoredFcmToken(fcmToken);
      setPushStatus('ENABLED');
    } catch (error) {
      setPushStatus('ERROR');
      setPushError(error.message);
    }
  }, [user]);

  const handleBookingPayload = useCallback(async (payload, { showNotification = false } = {}) => {
    const data = payload?.data || {};
    const notificationStatus = getBookingStatusFromNotification(payload);
    const title = getBookingNotificationTitle(payload, notificationStatus);
    const body = getBookingNotificationBody(payload);
    const currentBookingId = activeBookingIdRef.current;
    const isActiveBooking = data.bookingId ? data.bookingId === currentBookingId : true;

    if (showNotification) {
      try {
        await showBrowserNotification(title, body, data);
      } catch (error) {
        setPushError(error.message);
      }
    }

    if (!isActiveBooking) return;

    setForegroundMessage({ title, body, data });
    onBookingUpdateRef.current?.({
      bookingId: data.bookingId,
      status: notificationStatus,
      data,
      title,
      body,
    });
  }, []);

  useEffect(() => {
    if (!user) return undefined;

    enablePushNotifications();
    return undefined;
  }, [enablePushNotifications, user]);

  useEffect(() => {
    if (!user) return undefined;

    let unsubscribe;
    listenForegroundMessages((payload) => handleBookingPayload(payload, { showNotification: true }))
      .then((stopListening) => {
        unsubscribe = stopListening;
      })
      .catch((error) => setPushError(error.message));

    return () => unsubscribe?.();
  }, [handleBookingPayload, user]);

  useEffect(() => {
    if (!user || !('serviceWorker' in navigator)) return undefined;

    const handleServiceWorkerMessage = (event) => {
      if (event.data?.type !== 'FCM_BOOKING_UPDATE') return;
      handleBookingPayload(event.data.payload);
    };

    navigator.serviceWorker.addEventListener('message', handleServiceWorkerMessage);
    return () => navigator.serviceWorker.removeEventListener('message', handleServiceWorkerMessage);
  }, [handleBookingPayload, user]);

  return {
    pushStatus,
    pushError,
    foregroundMessage,
    clearForegroundMessage: () => setForegroundMessage(null),
    enablePushNotifications,
  };
}
