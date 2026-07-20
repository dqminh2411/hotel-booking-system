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