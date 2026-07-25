import { useCallback, useEffect, useRef, useState } from 'react';
import { listenForegroundMessages, requestFcmToken } from '../../../firebase';
import { setStoredFcmToken } from '../../auth/services/authStorage';
import { subscribeTopic, upsertDeviceToken } from '../api/notificationApi';
import {
  getBookingFailureReason,
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
      await upsertDeviceToken(fcmToken);
      setStoredFcmToken(fcmToken);

      // Auto-subscribe user to system-wide active promotion & coupon topics
      try {
        await subscribeTopic(fcmToken, 'promotion-active-notification');
        await subscribeTopic(fcmToken, 'coupon-active-notification');
      } catch (subErr) {
        console.warn('Failed to subscribe FCM token to promotion/coupon topics:', subErr);
      }

      setPushStatus('ENABLED');
    } catch (error) {
      setPushStatus('ERROR');
      setPushError(error.message);
    }
  }, [user]);

  const handleBookingPayload = useCallback(async (payload, { showNotification = false } = {}) => {
    const data = payload?.data || {};
    const isPromotionEvent = data.promotionId || data.couponId || (data.eventType && (data.eventType.includes('Promotion') || data.eventType.includes('Coupon')));
    
    let title = payload?.notification?.title || data.title;
    let body = payload?.notification?.body || data.body;

    if (!isPromotionEvent) {
      const notificationStatus = getBookingStatusFromNotification(payload);
      title = title || getBookingNotificationTitle(payload, notificationStatus);
      body = body || getBookingNotificationBody(payload, notificationStatus);
    }

    const reason = getBookingFailureReason(payload);
    const currentBookingId = activeBookingIdRef.current;
    const isActiveBooking = data.bookingId ? data.bookingId === currentBookingId : true;

    if (showNotification && title && body) {
      try {
        await showBrowserNotification(title, body, data);
      } catch (error) {
        setPushError(error.message);
      }
    }

    if (!isActiveBooking && !isPromotionEvent) return;

    setForegroundMessage({ title, body, reason, data });
    if (!isPromotionEvent) {
      onBookingUpdateRef.current?.({
        bookingId: data.bookingId,
        status: getBookingStatusFromNotification(payload),
        reason,
        data,
        title,
        body,
      });
    }
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
      if (event.data?.type !== 'FCM_BOOKING_UPDATE' && event.data?.type !== 'FCM_PROMOTION_UPDATE') return;
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