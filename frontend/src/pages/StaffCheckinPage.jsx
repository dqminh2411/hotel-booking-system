import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import { fetchBookingById } from '../features/booking/api/bookingApi';
import { confirmBookingCheckin, fetchAvailableRoomsByRoomTypes } from '../features/checkin/api/checkinApi';
import RoomTypeCheckinGroup from '../features/checkin/components/RoomTypeCheckinGroup';
import { getRequiredQuantity, normalizeBookingForCheckin } from '../features/checkin/utils/checkinUtils';
import ErrorState from '../shared/components/ErrorState';
import PublicHeader from '../shared/components/PublicHeader';
import { formatDateVi } from '../shared/utils/formatters';

export default function StaffCheckinPage() {
  const { bookingId } = useParams();
  const location = useLocation();

  const [booking, setBooking] = useState(() => normalizeBookingForCheckin(location.state?.booking));
  const [bookingStatus, setBookingStatus] = useState(location.state?.booking ? 'success' : 'loading');
  const [bookingError, setBookingError] = useState('');

  const [roomsByType, setRoomsByType] = useState({});
  const [roomsStatus, setRoomsStatus] = useState('idle');
  const [roomsError, setRoomsError] = useState('');

  // { [roomTypeId]: Set<roomId> }
  const [selectedByType, setSelectedByType] = useState({});

  const [submitStatus, setSubmitStatus] = useState('idle'); // idle | submitting | success | error
  const [submitError, setSubmitError] = useState('');

  const loadBooking = useCallback(async () => {
    if (!bookingId) return;
    setBookingStatus('loading');
    setBookingError('');

    try {
      const data = await fetchBookingById(bookingId);
      setBooking(normalizeBookingForCheckin(data));
      setBookingStatus('success');
    } catch (error) {
      setBookingError(error.response?.data?.message || error.message || 'Không thể tải thông tin đặt phòng.');
      setBookingStatus('error');
    }
  }, [bookingId]);

  useEffect(() => {
    loadBooking();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const roomTypeList = useMemo(() => booking?.roomTypeList || [], [booking]);

  const loadAvailableRooms = useCallback(async () => {
    if (roomTypeList.length === 0) return;
    setRoomsStatus('loading');
    setRoomsError('');

    try {
      const roomTypeIds = roomTypeList.map((roomType) => roomType.roomTypeId);
      const data = await fetchAvailableRoomsByRoomTypes(roomTypeIds);
      setRoomsByType(data);
      setRoomsStatus('success');
    } catch (error) {
      setRoomsError(error.response?.data?.message || error.message || 'Không thể tải danh sách phòng trống.');
      setRoomsStatus('error');
    }
  }, [roomTypeList]);

  useEffect(() => {
    if (booking?.status === 'CONFIRMED' && roomTypeList.length > 0) {
      loadAvailableRooms();
    }
  }, [booking?.status, roomTypeList, loadAvailableRooms]);

  function toggleRoom(roomTypeId, roomId) {
    setSubmitError('');
    setSelectedByType((current) => {
      const currentSet = current[roomTypeId] || new Set();
      const nextSet = new Set(currentSet);

      if (nextSet.has(roomId)) {
        nextSet.delete(roomId);
      } else {
        const roomType = roomTypeList.find((rt) => rt.roomTypeId === roomTypeId);
        const required = getRequiredQuantity(roomType);
        if (nextSet.size >= required) return current;
        nextSet.add(roomId);
      }

      return { ...current, [roomTypeId]: nextSet };
    });
  }

  const totalRequired = roomTypeList.reduce((sum, rt) => sum + getRequiredQuantity(rt), 0);
  const totalSelected = Object.values(selectedByType).reduce((sum, set) => sum + set.size, 0);
  const isFullySelected =
    roomTypeList.length > 0 &&
    roomTypeList.every((rt) => (selectedByType[rt.roomTypeId]?.size || 0) === getRequiredQuantity(rt));

  async function handleConfirmCheckin() {
    if (!isFullySelected || submitStatus === 'submitting') return;

    setSubmitStatus('submitting');
    setSubmitError('');

    // BE (CheckinRequest.listRoomId) nhận Map<roomTypeId, List<roomId>>, không phải mảng phẳng.
    // Duyệt theo roomTypeList để đảm bảo mọi roomTypeId của booking đều có key trong map,
    // kể cả trường hợp hi hữu selectedByType chưa có entry cho roomTypeId đó.
    const listRoomId = Object.fromEntries(
      roomTypeList.map((roomType) => [
        roomType.roomTypeId,
        Array.from(selectedByType[roomType.roomTypeId] || []),
      ]),
    );

    try {
      await confirmBookingCheckin(bookingId, listRoomId);
      setSubmitStatus('success');
    } catch (error) {
      // booking-service bọc mọi lỗi khi gọi Feign sang hotel-service (kể cả lỗi
      // nghiệp vụ như phòng đã bị chọn/checkin trước bởi request khác) thành
      // một lỗi chung HOTEL_SERVICE_ERROR/HOTEL_SERVICE_UNAVAILABLE (503), nên
      // message gốc từ hotel-service không tới được FE - phải tự suy luận và
      // hướng dẫn nhân viên tải lại danh sách phòng.
      const code = error.response?.data?.code;
      let message = error.response?.data?.message || error.message || 'Check-in thất bại. Vui lòng thử lại.';

      if (code === 'HOTEL_SERVICE_ERROR' || code === 'HOTEL_SERVICE_UNAVAILABLE') {
        message =
          'Không thể cập nhật trạng thái phòng ở hệ thống khách sạn. Có thể một phòng bạn chọn vừa được người khác chọn/check-in trước, hoặc hệ thống đang tạm gián đoạn. Vui lòng tải lại danh sách phòng và thử lại.';
      }

      setSubmitError(message);
      setSubmitStatus('error');
    }
  }

  return (
    <div className="min-h-screen bg-slate-50">
      <PublicHeader />

      <main className="mx-auto max-w-5xl px-4 py-6 md:px-6 lg:px-8">
        <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-sm font-medium text-blue-700">Nhân viên · Check-in</p>
            <h1 className="text-2xl font-bold text-slate-900">Xác nhận check-in</h1>
          </div>
          <Link
            to={`/bookings/${bookingId}`}
            className="rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-700"
          >
            ← Quay lại chi tiết đặt phòng
          </Link>
        </div>

        {bookingStatus === 'loading' && (
          <div className="rounded-lg border border-slate-200 bg-white p-8 text-center text-sm text-slate-600">
            Đang tải thông tin đặt phòng...
          </div>
        )}

        {bookingStatus === 'error' && <ErrorState message={bookingError} onRetry={loadBooking} />}

        {bookingStatus === 'success' && booking && booking.status !== 'CONFIRMED' && submitStatus !== 'success' && (
          <div className="rounded-lg border border-amber-200 bg-amber-50 p-5 text-sm text-amber-800">
            <p className="font-semibold">Không thể check-in.</p>
            <p className="mt-1">
              Booking này đang ở trạng thái <strong>{booking.status}</strong>. Chỉ có thể check-in cho booking đã ở
              trạng thái CONFIRMED.
            </p>
            <Link to={`/bookings/${bookingId}`} className="secondary-button mt-4 inline-flex">
              Về trang chi tiết đặt phòng
            </Link>
          </div>
        )}

        {submitStatus === 'success' && (
          <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-8 text-center">
            <span className="mx-auto grid h-12 w-12 place-items-center rounded-full bg-emerald-100 text-xl text-emerald-700">
              ✓
            </span>
            <p className="mt-4 text-lg font-semibold text-emerald-900">Check-in thành công.</p>
            <p className="mt-1 text-sm text-emerald-800">
              Booking đã chuyển sang trạng thái CHECKEDIN và các phòng đã chọn đã được cập nhật thành OCCUPIED.
            </p>
            <Link to={`/bookings/${bookingId}`} className="primary-button mt-5 inline-flex">
              Xem chi tiết đặt phòng
            </Link>
          </div>
        )}

        {bookingStatus === 'success' && booking && booking.status === 'CONFIRMED' && submitStatus !== 'success' && (
          <div className="space-y-6">
            <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
              <h2 className="text-lg font-semibold text-slate-900">{booking.hotel?.name || 'Khách sạn'}</h2>
              <p className="mt-1 text-sm text-slate-600">{booking.hotel?.address}</p>
              <dl className="mt-4 grid gap-4 text-sm md:grid-cols-3">
                <div>
                  <dt className="text-slate-500">Mã booking</dt>
                  <dd className="break-all font-semibold text-slate-900">{booking.bookingId}</dd>
                </div>
                <div>
                  <dt className="text-slate-500">Nhận phòng</dt>
                  <dd className="font-semibold text-slate-900">{formatDateVi(booking.checkin)}</dd>
                </div>
                <div>
                  <dt className="text-slate-500">Trả phòng</dt>
                  <dd className="font-semibold text-slate-900">{formatDateVi(booking.checkout)}</dd>
                </div>
                <div>
                  <dt className="text-slate-500">Khách</dt>
                  <dd className="font-semibold text-slate-900">
                    {booking.customer?.name || booking.customer?.fullName || 'Đang cập nhật'}
                  </dd>
                </div>
                <div>
                  <dt className="text-slate-500">Số khách</dt>
                  <dd className="font-semibold text-slate-900">{booking.numAdults || 0}</dd>
                </div>
              </dl>
            </section>

            <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <h2 className="text-lg font-semibold text-slate-900">Chọn phòng để check-in</h2>
                <button
                  type="button"
                  onClick={loadAvailableRooms}
                  disabled={roomsStatus === 'loading'}
                  className="secondary-button disabled:opacity-60"
                >
                  {roomsStatus === 'loading' ? 'Đang tải...' : 'Tải lại danh sách phòng'}
                </button>
              </div>
              <p className="mt-1 text-sm text-slate-600">
                Click chọn đủ số lượng phòng cho từng loại phòng đã đặt trong booking này.
              </p>

              {roomsStatus === 'error' && (
                <div className="mt-4">
                  <ErrorState message={roomsError} onRetry={loadAvailableRooms} />
                </div>
              )}

              <div className="mt-4 space-y-3">
                {roomTypeList.map((roomType) => (
                  <RoomTypeCheckinGroup
                    key={roomType.roomTypeId}
                    roomType={roomType}
                    availableRooms={roomsByType[roomType.roomTypeId]}
                    isLoading={roomsStatus === 'loading' || roomsStatus === 'idle'}
                    selectedRoomIds={selectedByType[roomType.roomTypeId] || new Set()}
                    onToggleRoom={toggleRoom}
                  />
                ))}
              </div>
            </section>

            <section className="sticky bottom-4 rounded-lg border border-blue-200 bg-white p-5 shadow-lg">
              <div className="flex flex-wrap items-center justify-between gap-4">
                <div>
                  <p className="font-semibold text-slate-900">
                    Đã chọn {totalSelected}/{totalRequired} phòng
                  </p>
                  {submitError && <p className="mt-1 text-sm text-red-700">{submitError}</p>}
                </div>
                <button
                  type="button"
                  onClick={handleConfirmCheckin}
                  disabled={!isFullySelected || submitStatus === 'submitting'}
                  className="primary-button disabled:opacity-60"
                >
                  {submitStatus === 'submitting' ? 'Đang xác nhận...' : 'Xác nhận check-in'}
                </button>
              </div>
            </section>
          </div>
        )}
      </main>
    </div>
  );
}