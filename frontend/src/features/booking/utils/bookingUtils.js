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

// place-booking-service (KafkaConsumerService.handleBookingFailed / handleBookingCancelled)
// luôn kèm theo "reason" khi publish SendBookingFailed sang notification-commands.
// Đây thường là nội dung mô tả rõ nhất nguyên nhân that bai (vd: het phong do
// race condition khi booking-service xac nhan, thanh toan that bai, ...), nen
// uu tien hien thi truc tiep field nay cho nguoi dung thay vi tu dat lai text.
export function getBookingFailureReason(payload) {
  const data = payload?.data || {};
  return data.reason || data.failReason || data.errorReason || '';
}

export function getBookingNotificationTitle(payload, status) {
  const data = payload?.data || {};
  if (status === 'CONFIRMED') return 'Đặt phòng thành công';
  if (status === 'CANCELLED') return 'Đặt phòng đã hủy';
  if (status === 'FAILED') return 'Đặt phòng thất bại';
  return data.title || payload?.notification?.title || 'Cập nhật đặt phòng';
}

export function getBookingNotificationBody(payload, status) {
  const data = payload?.data || {};
  const reason = getBookingFailureReason(payload);

  // Với trạng thái thất bại/hủy (thường do race condition hết phòng hoặc thanh
  // toán lỗi ở booking-service), ưu tiên hiển thị "reason" thật từ BE.
  if ((status === 'FAILED' || status === 'CANCELLED') && reason) {
    return reason;
  }

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

// Payload gửi lên POST /place-booking chỉ ảnh hưởng tới hash chống trùng của BE
// (Helpler.toStringPlaceBookingRequest) bởi các field: userId, hotelId, checkin,
// checkout, numAdults và roomTypeList{roomTypeId,bookingQuantity} (đã sort theo
// roomTypeId). Hàm này tách riêng phần "khoá theo nội dung" để CheckoutPage có
// thể build lại đúng payload khi gửi kèm forceToken mà không sợ lệch hash.
export function buildPlaceBookingPayload({ checkoutDraft, userId, idempotencyKey, forceToken }) {
  const payload = {
    userId,
    hotelId: checkoutDraft.hotel.hotelId,
    roomTypeList: checkoutDraft.roomTypes.map((room) => ({
      roomTypeId: room.roomTypeId,
      name: room.name,
      bedCount: Number(room.bedCount || room.bedCounts || 1),
      bookingQuantity: Number(room.quantity),
      totalQuantity: Number(room.totalQuantity || room.totalRooms || room.availableRooms || room.quantity),
      price: Number(room.pricePerNight || room.basePricePerNight || room.price),
    })),
    checkin: checkoutDraft.booking.checkinDate,
    checkout: checkoutDraft.booking.checkoutDate,
    numAdults: Number(checkoutDraft.booking.guestNum || 1),
    totalAmount: Number(checkoutDraft.price.finalPrice ?? checkoutDraft.price.totalPrice),
    currency: 'VND',
    paymentMethod: 'CREDIT_CARD',
    paymentToken: createPaymentToken(),
    idempotencyKey,
  };

  if (forceToken) {
    payload.forceToken = forceToken;
  }

  return payload;
}