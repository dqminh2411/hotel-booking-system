// Chuẩn hoá dữ liệu booking (từ GET /bookings/{bookingId} của booking-service)
// riêng cho trang check-in nhân viên. Viết độc lập với normalizeBooking() ở
// BookingDetailPage.jsx để không phải sửa/ràng buộc logic của trang đó.
export function normalizeBookingForCheckin(rawBooking) {
  if (!rawBooking) return null;
  const details = rawBooking.details || rawBooking;

  return {
    bookingId: rawBooking.bookingId || details.bookingId,
    status: rawBooking.status || details.status || 'PENDING',
    hotel: details.hotel || {},
    customer: details.customer || details.guest || {},
    checkin: details.checkin || details.checkinDate,
    checkout: details.checkout || details.checkoutDate,
    numAdults: details.numAdults || details.guestNum,
    // BookingDetail.RoomType (booking-service): { roomTypeId, name, bedCount,
    // bookingQuantity, totalQuantity, price }. bookingQuantity là số phòng của
    // loại phòng này mà khách đã đặt - cũng là số roomId nhân viên cần chọn đủ.
    roomTypeList: details.roomTypeList || details.roomTypes || [],
  };
}

export function getRequiredQuantity(roomType) {
  return Number(roomType?.bookingQuantity ?? roomType?.quantity ?? 0);
}