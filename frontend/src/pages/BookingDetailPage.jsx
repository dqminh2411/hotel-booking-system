import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import { fetchBookingById } from '../features/booking/api/bookingApi';
import ErrorState from '../shared/components/ErrorState';
import PublicHeader from '../shared/components/PublicHeader';
import { formatCurrency, formatDateVi } from '../shared/utils/formatters';

const STATUS_CONFIG = {
  CONFIRMED: {
    label: 'Đã xác nhận',
    badge: 'border-emerald-200 bg-emerald-50 text-emerald-700',
    message: 'Đặt phòng đã sẵn sàng. Vui lòng kiểm tra lại thông tin trước ngày nhận phòng.',
  },
  PENDING: {
    label: 'Đang xử lý',
    badge: 'border-amber-200 bg-amber-50 text-amber-700',
    message: 'Hệ thống đang xử lý xác nhận và thanh toán.',
  },
  FAILED: {
    label: 'Thất bại',
    badge: 'border-red-200 bg-red-50 text-red-700',
    message: 'Đặt phòng chưa hoàn tất. Vui lòng thử lại hoặc chọn phòng khác.',
  },
  CANCELLED: {
    label: 'Đã hủy',
    badge: 'border-red-200 bg-red-50 text-red-700',
    message: 'Đặt phòng đã bị hủy.',
  },
  CHECKEDIN: {
    label: 'Đã check-in',
    badge: 'border-blue-200 bg-blue-50 text-blue-700',
    message: 'Khách đã nhận phòng thành công.',
  },
};

function normalizeBooking(rawBooking) {
  if (!rawBooking) return null;
  const details = rawBooking.details || rawBooking;
  return {
    bookingId: rawBooking.bookingId || details.bookingId,
    status: rawBooking.status || details.status || 'PENDING',
    createdAt: rawBooking.createdAt || details.createdAt,
    hotel: details.hotel || {},
    customer: details.customer || details.guest || {},
    checkin: details.checkin || details.checkinDate,
    checkout: details.checkout || details.checkoutDate,
    numAdults: details.numAdults || details.guestNum,
    roomNum: details.roomNum,
    nights: details.nights,
    roomTypeList: details.roomTypeList || details.roomTypes || [],
    totalAmount: details.totalAmount ?? details.price?.totalPrice ?? details.price?.finalPrice,
    discount: details.discount ?? details.price?.discount ?? 0,
    finalPrice: details.finalPrice ?? details.price?.finalPrice ?? details.totalAmount,
    paymentMethod: details.paymentMethod || 'CREDIT_CARD',
    paymentStatus: details.paymentStatus || (rawBooking.status === 'CONFIRMED' ? 'PAID' : 'PENDING'),
  };
}

function countNights(checkin, checkout) {
  if (!checkin || !checkout) return 0;
  const diff = new Date(checkout).getTime() - new Date(checkin).getTime();
  if (Number.isNaN(diff) || diff <= 0) return 0;
  return Math.round(diff / 86400000);
}

function getRoomQuantity(room) {
  return Number(room.bookingQuantity || room.quantity || 0);
}

function getRoomPrice(room) {
  return Number(room.price || room.pricePerNight || room.basePricePerNight || 0);
}

export default function BookingDetailPage() {
  const { bookingId } = useParams();
  const location = useLocation();
  const [booking, setBooking] = useState(() => normalizeBooking(location.state?.booking));
  const [status, setStatus] = useState(location.state?.booking ? 'success' : 'loading');
  const [errorMessage, setErrorMessage] = useState('');

  const loadBooking = useCallback(async () => {
    if (!bookingId) return;
    setStatus((current) => (current === 'success' ? current : 'loading'));
    setErrorMessage('');

    try {
      const data = await fetchBookingById(bookingId);
      setBooking(normalizeBooking(data));
      setStatus('success');
    } catch (error) {
      setErrorMessage(error.response?.data?.message || error.message || 'Không thể tải thông tin đặt phòng.');
      setStatus('error');
    }
  }, [bookingId]);

  useEffect(() => {
    loadBooking();
  }, [loadBooking]);

  const normalized = booking;
  const statusConfig = STATUS_CONFIG[normalized?.status] || {
    label: normalized?.status || 'Không xác định',
    badge: 'border-slate-200 bg-slate-50 text-slate-600',
    message: 'Trạng thái đặt phòng chưa xác định.',
  };

  const nights = normalized?.nights || countNights(normalized?.checkin, normalized?.checkout);
  const roomCount = useMemo(
    () => normalized?.roomTypeList?.reduce((sum, room) => sum + getRoomQuantity(room), 0) || normalized?.roomNum || 0,
    [normalized],
  );

  return (
    <div className="min-h-screen bg-slate-50">
      <PublicHeader />

      <main className="mx-auto max-w-7xl px-4 py-6 md:px-6 lg:px-8">
        <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-sm font-medium text-blue-700">Chi tiết đặt phòng</p>
            <h1 className="text-2xl font-bold text-slate-900">Booking #{bookingId}</h1>
          </div>
          <div className="flex flex-wrap gap-2">
            {normalized?.status === 'CONFIRMED' && (
              <Link
                to={`/staff/bookings/${bookingId}/checkin`}
                state={{ booking: normalized }}
                className="accent-button"
              >
                Xác nhận checkin
              </Link>
            )}
            <Link to="/" className="rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-700">
              Về trang chủ
            </Link>
            <Link to="/" className="accent-button">
              Đặt phòng khác
            </Link>
          </div>
        </div>

        {status === 'loading' && (
          <div className="rounded-lg border border-slate-200 bg-white p-8 text-center text-sm text-slate-600">
            Đang tải thông tin đặt phòng...
          </div>
        )}

        {status === 'error' && !normalized && <ErrorState message={errorMessage} onRetry={loadBooking} />}

        {normalized && (
          <div className="space-y-6">
            {status === 'error' && (
              <ErrorState
                message={`${errorMessage} Dữ liệu bên dưới là dữ liệu tạm thời từ trang thanh toán.`}
                onRetry={loadBooking}
              />
            )}

            <section className="overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm">
              {normalized.hotel.coverImageUrl && (
                <img src={normalized.hotel.coverImageUrl} alt={normalized.hotel.name} className="h-64 w-full object-cover" />
              )}
              <div className="grid gap-6 p-5 lg:grid-cols-[1fr_320px]">
                <div>
                  <span className={`inline-flex rounded-full border px-3 py-1 text-xs font-semibold ${statusConfig.badge}`}>
                    {statusConfig.label}
                  </span>
                  <h2 className="mt-3 text-2xl font-bold text-slate-900">{normalized.hotel.name || 'Khách sạn'}</h2>
                  <p className="mt-1 text-sm text-slate-600">{normalized.hotel.address}</p>
                  <p className="mt-4 text-sm text-slate-700">{statusConfig.message}</p>
                </div>
                <dl className="rounded-lg bg-slate-50 p-4 text-sm">
                  <div>
                    <dt className="text-slate-500">Mã booking</dt>
                    <dd className="break-all font-semibold text-slate-900">{normalized.bookingId}</dd>
                  </div>
                  <div className="mt-3">
                    <dt className="text-slate-500">Ngày tạo</dt>
                    <dd className="font-semibold text-slate-900">{formatDateVi(normalized.createdAt) || 'Đang cập nhật'}</dd>
                  </div>
                </dl>
              </div>
            </section>

            <div className="grid gap-6 lg:grid-cols-[1fr_380px]">
              <div className="space-y-6">
                <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
                  <h2 className="text-lg font-semibold text-slate-900">Thông tin lưu trú</h2>
                  <dl className="mt-4 grid gap-4 text-sm md:grid-cols-2">
                    <div>
                      <dt className="text-slate-500">Nhận phòng</dt>
                      <dd className="font-semibold text-slate-900">{formatDateVi(normalized.checkin)}</dd>
                    </div>
                    <div>
                      <dt className="text-slate-500">Trả phòng</dt>
                      <dd className="font-semibold text-slate-900">{formatDateVi(normalized.checkout)}</dd>
                    </div>
                    <div>
                      <dt className="text-slate-500">Số khách</dt>
                      <dd className="font-semibold text-slate-900">{normalized.numAdults || 0}</dd>
                    </div>
                    <div>
                      <dt className="text-slate-500">Số phòng / số đêm</dt>
                      <dd className="font-semibold text-slate-900">
                        {roomCount} phòng · {nights} đêm
                      </dd>
                    </div>
                  </dl>
                </section>

                <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
                  <h2 className="text-lg font-semibold text-slate-900">Phòng đã đặt</h2>
                  <div className="mt-4 space-y-3">
                    {normalized.roomTypeList.map((room) => {
                      const quantity = getRoomQuantity(room);
                      const price = getRoomPrice(room);
                      return (
                        <div key={room.roomTypeId || room.name} className="rounded-md bg-slate-50 p-3 text-sm">
                          <div className="flex items-start justify-between gap-3">
                            <div>
                              <p className="font-semibold text-slate-900">{room.name}</p>
                              <p className="text-slate-600">
                                {quantity} phòng × {nights} đêm · {formatCurrency(price)}/đêm
                              </p>
                            </div>
                            <p className="font-semibold text-slate-900">{formatCurrency(price * quantity * nights)}</p>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </section>

                <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
                  <h2 className="text-lg font-semibold text-slate-900">Thông tin khách</h2>
                  <dl className="mt-4 grid gap-4 text-sm md:grid-cols-3">
                    <div>
                      <dt className="text-slate-500">Họ tên</dt>
                      <dd className="font-semibold text-slate-900">{normalized.customer.name || normalized.customer.fullName || 'Đang cập nhật'}</dd>
                    </div>
                    <div>
                      <dt className="text-slate-500">Email</dt>
                      <dd className="font-semibold text-slate-900">{normalized.customer.email || 'Đang cập nhật'}</dd>
                    </div>
                    <div>
                      <dt className="text-slate-500">Số điện thoại</dt>
                      <dd className="font-semibold text-slate-900">{normalized.customer.phone || 'Đang cập nhật'}</dd>
                    </div>
                  </dl>
                </section>
              </div>

              <aside className="h-fit rounded-lg border border-blue-200 bg-blue-50 p-5 shadow-sm">
                <h2 className="text-lg font-semibold text-blue-950">Tổng kết thanh toán</h2>
                <div className="mt-4 space-y-3 text-sm">
                  <div className="flex justify-between">
                    <span>Tạm tính</span>
                    <span>{formatCurrency(normalized.totalAmount)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span>Giảm giá</span>
                    <span>{formatCurrency(normalized.discount)}</span>
                  </div>
                  <div className="flex justify-between border-t border-blue-200 pt-3 text-xl font-bold text-blue-950">
                    <span>Thành tiền</span>
                    <span>{formatCurrency(normalized.finalPrice ?? normalized.totalAmount)}</span>
                  </div>
                  <div className="border-t border-blue-200 pt-3">
                    <p>Phương thức: {normalized.paymentMethod === 'CREDIT_CARD' ? 'Thẻ tín dụng' : normalized.paymentMethod}</p>
                    <p>Trạng thái thanh toán: {normalized.paymentStatus}</p>
                  </div>
                </div>
              </aside>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}