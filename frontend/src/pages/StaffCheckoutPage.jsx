import { useCallback, useEffect, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import { fetchBookingById } from '../features/booking/api/bookingApi';
import { confirmBookingCheckout } from '../features/checkin/api/checkinApi';
// normalizeBookingForCheckin chỉ đọc các field chung (bookingId, status, hotel,
// customer, checkin, checkout, numAdults, roomTypeList) nên dùng chung được cho
// cả trang checkout, không cần viết lại hàm chuẩn hoá riêng.
import { normalizeBookingForCheckin } from '../features/checkin/utils/checkinUtils';
import ErrorState from '../shared/components/ErrorState';
import PublicHeader from '../shared/components/PublicHeader';
import { formatDateVi } from '../shared/utils/formatters';

export default function StaffCheckoutPage() {
    const { bookingId } = useParams();
    const location = useLocation();

    const [booking, setBooking] = useState(() => normalizeBookingForCheckin(location.state?.booking));
    const [bookingStatus, setBookingStatus] = useState(location.state?.booking ? 'success' : 'loading');
    const [bookingError, setBookingError] = useState('');

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

    async function handleConfirmCheckout() {
        if (submitStatus === 'submitting') return;

        setSubmitStatus('submitting');
        setSubmitError('');

        try {
            await confirmBookingCheckout(bookingId);
            setSubmitStatus('success');
        } catch (error) {
            // Cùng lý do như trang check-in: booking-service bọc mọi lỗi khi gọi Feign
            // sang hotel-service (kể cả lỗi nghiệp vụ) thành lỗi chung
            // HOTEL_SERVICE_ERROR/HOTEL_SERVICE_UNAVAILABLE (503), nên message gốc
            // không tới được FE - phải tự suy luận thông báo dễ hiểu hơn.
            const code = error.response?.data?.code;
            let message = error.response?.data?.message || error.message || 'Check-out thất bại. Vui lòng thử lại.';

            if (code === 'HOTEL_SERVICE_ERROR' || code === 'HOTEL_SERVICE_UNAVAILABLE') {
                message =
                    'Không thể cập nhật trạng thái phòng ở hệ thống khách sạn. Hệ thống có thể đang tạm gián đoạn. Vui lòng thử lại sau ít phút.';
            }

            setSubmitError(message);
            setSubmitStatus('error');
        }
    }

    return (
        <div className="min-h-screen bg-slate-50">
            <PublicHeader />

            <main className="mx-auto max-w-3xl px-4 py-6 md:px-6 lg:px-8">
                <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
                    <div>
                        <p className="text-sm font-medium text-blue-700">Nhân viên · Check-out</p>
                        <h1 className="text-2xl font-bold text-slate-900">Xác nhận check-out</h1>
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

                {bookingStatus === 'success' && booking && booking.status !== 'CHECKEDIN' && submitStatus !== 'success' && (
                    <div className="rounded-lg border border-amber-200 bg-amber-50 p-5 text-sm text-amber-800">
                        <p className="font-semibold">Không thể check-out.</p>
                        <p className="mt-1">
                            Booking này đang ở trạng thái <strong>{booking.status}</strong>. Chỉ có thể check-out cho booking đã ở
                            trạng thái CHECKEDIN.
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
                        <p className="mt-4 text-lg font-semibold text-emerald-900">Check-out thành công.</p>
                        <p className="mt-1 text-sm text-emerald-800">
                            Booking đã chuyển sang trạng thái COMPLETED và các phòng đã được cập nhật thành CLEANING (chờ dọn
                            trước khi mở bán lại).
                        </p>
                        <Link to={`/bookings/${bookingId}`} className="primary-button mt-5 inline-flex">
                            Xem chi tiết đặt phòng
                        </Link>
                    </div>
                )}

                {bookingStatus === 'success' && booking && booking.status === 'CHECKEDIN' && submitStatus !== 'success' && (
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
                            <h2 className="text-lg font-semibold text-slate-900">Phòng đã nhận</h2>
                            <p className="mt-1 text-sm text-slate-600">
                                Toàn bộ phòng đã check-in cho booking này sẽ được tự động chuyển sang trạng thái CLEANING khi xác
                                nhận check-out - không cần chọn lại phòng.
                            </p>
                            <div className="mt-3 space-y-2">
                                {(booking.roomTypeList || []).map((roomType) => (
                                    <div
                                        key={roomType.roomTypeId}
                                        className="flex items-center justify-between rounded-md bg-slate-50 p-3 text-sm"
                                    >
                                        <span className="font-medium text-slate-900">{roomType.name}</span>
                                        <span className="text-slate-600">{roomType.bookingQuantity ?? roomType.quantity} phòng</span>
                                    </div>
                                ))}
                            </div>
                        </section>

                        <section className="rounded-lg border border-blue-200 bg-white p-5 shadow-lg">
                            <div className="flex flex-col items-center gap-4">
                                <p className="w-full text-center text-sm text-slate-600">
                                    Xác nhận khách đã trả phòng và rời khách sạn. Hành động này sẽ hoàn tất booking.
                                </p>
                                <button
                                    type="button"
                                    onClick={handleConfirmCheckout}
                                    disabled={submitStatus === 'submitting'}
                                    className="primary-button whitespace-nowrap disabled:opacity-60"
                                >
                                    {submitStatus === 'submitting' ? 'Đang xác nhận...' : 'Xác nhận check-out'}
                                </button>
                            </div>
                            {submitError && <p className="mt-3 text-center text-sm text-red-700">{submitError}</p>}
                        </section>
                    </div>
                )}
            </main>
        </div>
    );
}