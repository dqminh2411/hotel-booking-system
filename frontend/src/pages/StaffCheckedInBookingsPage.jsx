import { useParams } from 'react-router-dom';
import { fetchCheckedInBookings } from '../features/checkin/api/checkinApi';
import StaffBookingListPage from '../features/checkin/components/StaffBookingListPage';

// Danh sách booking đang lưu trú (status = CHECKEDIN, chưa check-out) của 1
// khách sạn - dùng cho nút "Check-out" ở trang chủ. Bấm vào 1 booking sẽ tới
// trang chi tiết booking, từ đó có nút "Xác nhận checkout" (chỉ hiện khi
// status = CHECKEDIN, xem BookingDetailPage) dẫn sang StaffCheckoutPage để
// thực hiện check-out.
export default function StaffCheckedInBookingsPage() {
  const { hotelId } = useParams();

  return (
    <StaffBookingListPage
      hotelId={hotelId}
      fetchList={fetchCheckedInBookings}
      eyebrow="Nhân viên · Check-out"
      title="Danh sách khách đang lưu trú (chờ check-out)"
      idlePlaceholder="Nhập mã khách sạn (hotelId) ở trên để xem danh sách khách đang lưu trú."
      emptyTitle="Không có booking nào đang chờ check-out"
      emptyDescription="Chưa có đơn đặt phòng nào ở trạng thái CHECKEDIN cho khách sạn này."
      defaultErrorMessage="Không thể tải danh sách khách đang lưu trú."
      buildChangeHotelRoute={(trimmedHotelId) => `/staff/hotels/${trimmedHotelId}/checkins`}
      badgeClassName="inline-flex items-center rounded-full border border-sky-200 bg-sky-50 px-2.5 py-1 text-xs font-semibold text-sky-700"
    />
  );
}