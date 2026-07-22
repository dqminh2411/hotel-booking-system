import axiosClient from '../../../shared/api/axiosClient';

// hotel-service: RoomTypeController#getListRoomAvailable
// GET /api/room-types/rooms?roomTypeIds=...&roomTypeIds=...
// (chỉ trả về room đang RoomStatus.AVAILABLE)
// -> ApiResponse.data = { listRoomResponse: { [roomTypeId]: [{ roomId, roomNumber, floor }] } }
export async function fetchAvailableRoomsByRoomTypes(roomTypeIds) {
  if (!roomTypeIds || roomTypeIds.length === 0) {
    return {};
  }

  const searchParams = new URLSearchParams();
  roomTypeIds.forEach((roomTypeId) => searchParams.append('roomTypeIds', roomTypeId));

  const { data } = await axiosClient.get(`/api/room-types/rooms?${searchParams.toString()}`);
  return data?.data?.listRoomResponse || {};
}

// booking-service: BookingController#checkinBooking
// PATCH /bookings/checkin { bookingId, listRoomId }
// listRoomId khớp CheckinRequest.listRoomId ở BE: Map<roomTypeId, List<roomId>>
// (object dạng { [roomTypeId]: [roomId, ...] }), KHÔNG phải mảng roomId phẳng.
// BE validate: booking phải đang CONFIRMED, tổng số roomId trong map phải khớp
// tổng bookingQuantity của booking, rồi gọi Feign sang hotel-service để set các
// phòng này thành OCCUPIED. Nếu hợp lệ, booking chuyển sang CHECKEDIN.
export async function confirmBookingCheckin(bookingId, listRoomId) {
  const { data } = await axiosClient.patch('/bookings/checkin', { bookingId, listRoomId });
  return data;
}

// booking-service: BookingController#checkout
// PATCH /bookings/checkout/{bookingId} (không cần body).
// BE validate: booking phải đang CHECKEDIN, tự lấy lại danh sách roomId đã lưu
// từ lúc check-in (BookedRoomTypeEntity.roomIds) - nhân viên không cần chọn lại
// phòng. Nếu hợp lệ: phòng OCCUPIED -> CLEANING, booking chuyển sang COMPLETED.
export async function confirmBookingCheckout(bookingId) {
  const { data } = await axiosClient.patch(`/bookings/checkout/${bookingId}`);
  return data;
}

// booking-service: BookingController#getBookingCheckinToday
// GET /bookings/today/{hotelId}?page=&size= (size: 10-30, mặc định 10)
// -> ApiResponse.data = Page<BookingCheckinInfo> (Spring Data mặc định serialize
// dạng { content: [...], totalElements, totalPages, number, size, last, first }).
// Chỉ trả về booking CONFIRMED có checkinDate = hôm nay của hotelId tương ứng.
export async function fetchTodayCheckins(hotelId, page = 0, size = 10) {
  const { data } = await axiosClient.get(`/bookings/today/${hotelId}`, { params: { page, size } });
  return data?.data;
}

// booking-service: BookingController#getBookingIsCheckin
// GET /bookings/{hotelId}/checkin?page=&size= (size: 10-30, mặc định 10)
// -> ApiResponse.data = Page<BookingCheckinInfo>, sort theo checkoutDate ASC.
// Chỉ trả về booking đang ở trạng thái CHECKEDIN (đã nhận phòng, chưa trả
// phòng) của hotelId tương ứng - dùng cho danh sách "chờ check-out".
export async function fetchCheckedInBookings(hotelId, page = 0, size = 10) {
  const { data } = await axiosClient.get(`/bookings/${hotelId}/checkin`, { params: { page, size } });
  return data?.data;
}