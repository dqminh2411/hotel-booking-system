import { useParams } from 'react-router-dom';
import { fetchTodayCheckins } from '../features/checkin/api/checkinApi';
import StaffBookingListPage from '../features/checkin/components/StaffBookingListPage';

export default function StaffTodayCheckinsPage() {
  const { hotelId } = useParams();

  return (
    <StaffBookingListPage
      hotelId={hotelId}
      fetchList={fetchTodayCheckins}
      eyebrow="Nhân viên · Check-in hôm nay"
      title="Danh sách khách check-in hôm nay"
      showTodayDate
      idlePlaceholder="Nhập mã khách sạn (hotelId) ở trên để xem danh sách khách check-in hôm nay."
      emptyTitle="Không có booking nào check-in hôm nay"
      emptyDescription="Chưa có đơn đặt phòng nào ở trạng thái CONFIRMED với ngày nhận phòng là hôm nay cho khách sạn này."
      defaultErrorMessage="Không thể tải danh sách check-in hôm nay."
      buildChangeHotelRoute={(trimmedHotelId) => `/staff/hotels/${trimmedHotelId}/today-checkins`}
      badgeClassName="inline-flex items-center rounded-full border border-amber-200 bg-amber-50 px-2.5 py-1 text-xs font-semibold text-amber-700"
    />
  );
}