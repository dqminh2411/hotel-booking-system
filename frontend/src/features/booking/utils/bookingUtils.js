export const TERMINAL_BOOKING_STATUSES = new Set(['CONFIRMED', 'FAILED', 'CANCELLED']);

export function createPaymentToken() {
  return `tok_${Math.random().toString(36).slice(2, 12)}`;
}

export function createIdempotencyKey() {
  if (window.crypto?.randomUUID) {
    return window.crypto.randomUUID();
  }
  return `idemp_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`;
}

export function getBookingStatusFromNotification(payload) {
  const data = payload?.data || {};
  if (data.status) {
    return data.status.toUpperCase();
  }

  const eventType = (data.eventType || '').toUpperCase();
  if (eventType.includes('CONFIRM')) return 'CONFIRMED';
  if (eventType.includes('CANCEL')) return 'CANCELLED';
  if (eventType.includes('FAIL')) return 'FAILED';
  return '';
}

export function getBookingNotificationTitle(payload, status) {
  const data = payload?.data || {};
  if (status === 'CONFIRMED') return 'Đặt phòng thành công';
  if (status === 'CANCELLED') return 'Đặt phòng đã hủy';
  if (status === 'FAILED') return 'Đặt phòng thất bại';
  return data.title || payload?.notification?.title || 'Cập nhật đặt phòng';
}

export function getBookingNotificationBody(payload) {
  const data = payload?.data || {};
  return data.body || payload?.notification?.body || 'Trạng thái đặt phòng vừa thay đổi.';
}

export async function showBrowserNotification(title, body, data = {}) {
  if (!('Notification' in window) || Notification.permission !== 'granted') {
    return;
  }

  if ('serviceWorker' in navigator) {
    const registration = await navigator.serviceWorker.ready;
    await registration.showNotification(title, { body, data });
    return;
  }

  new Notification(title, { body, data });
}

export function buildBookingDetailFromDraft(draft, bookingId, status = 'PENDING') {
  if (!draft) return null;

  return {
    bookingId,
    status,
    details: {
      bookingId,
      hotel: {
        hotelId: draft.hotel?.hotelId,
        name: draft.hotel?.name,
        address: draft.hotel?.address,
        coverImageUrl: draft.hotel?.coverImageUrl,
      },
      customer: draft.customer || null,
      checkin: draft.booking?.checkinDate,
      checkout: draft.booking?.checkoutDate,
      numAdults: draft.booking?.guestNum,
      roomTypeList: draft.roomTypes || [],
      totalAmount: draft.price?.finalPrice ?? draft.price?.totalPrice,
      discount: draft.price?.discount || 0,
      paymentMethod: 'CREDIT_CARD',
      paymentStatus: status === 'CONFIRMED' ? 'PAID' : 'PENDING',
      nights: draft.booking?.nights,
      roomNum: draft.booking?.roomNum,
    },
  };
}
