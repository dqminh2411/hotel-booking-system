import { useCallback, useMemo, useRef, useState, useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import useAuth from '../features/auth/hooks/useAuth';
import { createBooking } from '../features/booking/api/bookingApi';
import useBookingNotifications from '../features/booking/hooks/useBookingNotifications';
import DuplicateBookingModal from '../features/booking/components/DuplicateBookingModal';
import {
  buildBookingDetailFromDraft,
  buildPlaceBookingPayload,
  createIdempotencyKey,
} from '../features/booking/utils/bookingUtils';
import EmptyState from '../shared/components/EmptyState';
import PublicHeader from '../shared/components/PublicHeader';
import { formatCurrency, formatDateVi } from '../shared/utils/formatters';

const initialPayment = {
  cardholderName: '',
  cardNumber: '',
  expiryDate: '',
  cvv: '',
};

function normalizeCheckoutDraft(locationState) {
  const draft = locationState?.checkoutDraft || locationState;
  if (!draft?.hotel || !draft?.booking || !Array.isArray(draft?.roomTypes) || draft.roomTypes.length === 0) {
    return null;
  }
  return draft;
}

function validateForm(customer, payment) {
  const errors = {};

  if (!customer.fullName?.trim()) errors.fullName = 'Vui lòng nhập họ tên.';
  if (!customer.email?.trim()) {
    errors.email = 'Vui lòng nhập email.';
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(customer.email)) {
    errors.email = 'Email không hợp lệ.';
  }
  if (!customer.phone?.trim()) errors.phone = 'Vui lòng nhập số điện thoại.';
  if (!payment.cardholderName?.trim()) errors.cardholderName = 'Vui lòng nhập tên chủ thẻ.';
  if (!payment.cardNumber?.trim()) errors.cardNumber = 'Vui lòng nhập số thẻ.';
  if (!/^\d{3,4}$/.test(payment.cvv || '')) errors.cvv = 'CVV phải có 3-4 chữ số.';
  if (!/^(0[1-9]|1[0-2])\/\d{2}$/.test(payment.expiryDate || '')) {
    errors.expiryDate = 'Ngày hết hạn phải có định dạng MM/YY.';
  }

  return errors;
}

function FieldError({ message }) {
  if (!message) return null;
  return <p className="mt-1 text-xs text-red-600">{message}</p>;
}

function Spinner() {
  return <div className="h-5 w-5 animate-spin rounded-full border-2 border-blue-200 border-t-blue-700" />;
}

export default function CheckoutPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, isAuthenticated } = useAuth();
  const checkoutDraft = useMemo(() => normalizeCheckoutDraft(location.state), [location.state]);
  const idempotencyKeyRef = useRef(createIdempotencyKey());
  const [customer, setCustomer] = useState(() => ({
    fullName: user?.fullName || user?.name || '',
    email: user?.email || '',
    phone: user?.phone || '',
  }));
  const [payment, setPayment] = useState(() => ({
    ...initialPayment,
    cardholderName: user?.fullName || user?.name || '',
  }));
  const [formErrors, setFormErrors] = useState({});
  const [submitError, setSubmitError] = useState('');
  const [bookingStatus, setBookingStatus] = useState('IDLE');
  const [activeBookingId, setActiveBookingId] = useState('');
  const [createdBooking, setCreatedBooking] = useState(null);
  // Lý do thất bại/hủy do BE trả kèm khi có race condition hết phòng, hoặc
  // thanh toán lỗi ở booking-service (đến qua Kafka -> notification-service -> FCM).
  const [failureReason, setFailureReason] = useState('');
  // Cảnh báo trùng yêu cầu trong 5 phút (code=409 từ POST /place-booking, kèm forceToken).
  const [duplicateNotice, setDuplicateNotice] = useState(null);
  const [duplicateLoading, setDuplicateLoading] = useState(false);

  useEffect(() => {
    if (!checkoutDraft || bookingStatus === 'CONFIRMED') return undefined;

    const handleBeforeUnload = (event) => {
      event.preventDefault();
      event.returnValue = '';
    };

    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => window.removeEventListener('beforeunload', handleBeforeUnload);
  }, [bookingStatus, checkoutDraft]);

  useEffect(() => {
    if (isAuthenticated || !checkoutDraft) return;
    navigate('/login', {
      replace: true,
      state: { from: '/checkout', checkoutDraft },
    });
  }, [checkoutDraft, isAuthenticated, navigate]);

  const handleBookingUpdate = useCallback(
    (update) => {
      if (!update.status) return;
      setBookingStatus(update.status);

      // update.reason đến từ SendBookingFailed.reason (place-booking-service),
      // là nguyên nhân thật của BE - ví dụ hết phòng do race condition khi
      // booking-service xác nhận, hoặc thanh toán thất bại.
      if (update.status === 'FAILED' || update.status === 'CANCELLED') {
        setFailureReason(update.reason || '');
      } else {
        setFailureReason('');
      }

      const nextBooking = buildBookingDetailFromDraft(
        { ...checkoutDraft, customer },
        update.bookingId || activeBookingId,
        update.status,
      );
      if (nextBooking) setCreatedBooking(nextBooking);

      if (update.status === 'CONFIRMED') {
        navigate(`/bookings/${update.bookingId || activeBookingId}`, {
          replace: true,
          state: { booking: nextBooking, checkoutDraft: { ...checkoutDraft, customer } },
        });
      }
    },
    [activeBookingId, checkoutDraft, customer, navigate],
  );

  const {
    pushStatus,
    pushError,
    foregroundMessage,
    clearForegroundMessage,
    enablePushNotifications,
  } = useBookingNotifications({
    user,
    activeBookingId,
    onBookingUpdate: handleBookingUpdate,
  });

  const totalRooms = checkoutDraft?.roomTypes?.reduce((sum, room) => sum + Number(room.quantity || 0), 0) || 0;

  function updateCustomerField(event) {
    const { name, value } = event.target;
    setCustomer((current) => ({ ...current, [name]: value }));
    setFormErrors((current) => ({ ...current, [name]: '' }));
  }

  function updatePaymentField(event) {
    const { name, value } = event.target;
    setPayment((current) => ({ ...current, [name]: value }));
    setFormErrors((current) => ({ ...current, [name]: '' }));
  }

  // Gửi POST /place-booking. Dùng chung cho lần gửi đầu tiên và lần gửi lại
  // kèm forceToken sau khi người dùng xác nhận ở DuplicateBookingModal.
  //
  // Lưu ý quan trọng về contract của BE (PlaceBookingController):
  // - Case "trùng request trong 5 phút" KHÔNG throw exception, controller trả
  //   thẳng ApiResponse với code=409 nhưng HTTP status vẫn là 200. Vì vậy phải
  //   kiểm tra response.code === 409 ở nhánh try (KHÔNG rơi vào catch).
  // - Case hợp lệ luôn trả code=200, data.status luôn là "PENDING" (vì booking
  //   được xử lý bất đồng bộ qua saga/outbox), CONFIRMED/FAILED thật sự sẽ đến
  //   sau qua Kafka -> FCM push (useBookingNotifications).
  // - Các lỗi thật (400/404/...) như hết phòng ngay tại thời điểm gửi
  //   (RoomTypeNotAvailableException), sai ngày, không tìm thấy khách sạn/user...
  //   được GlobalExceptionHandler trả về đúng HTTP status lỗi -> axios reject -> catch.
  const runPlaceBooking = useCallback(
    async (forceToken) => {
      const userId = user?.userId || user?.id;
      if (!userId || !checkoutDraft) return;

      setBookingStatus('PENDING');
      setActiveBookingId('');
      setCreatedBooking(null);
      setFailureReason('');
      setSubmitError('');

      const payload = buildPlaceBookingPayload({
        checkoutDraft,
        userId,
        idempotencyKey: idempotencyKeyRef.current,
        forceToken,
      });

      try {
        const response = await createBooking(payload);

        if (response.code === 409) {
          setBookingStatus('IDLE');
          setDuplicateNotice({
            message: response.message,
            hint: response.data?.hint,
            forceToken: response.data?.forceToken,
          });
          return;
        }

        setDuplicateNotice(null);

        const bookingId = response.data?.bookingId;
        if (!bookingId) throw new Error('Backend không trả về bookingId.');

        setActiveBookingId(bookingId);
        const optimisticBooking = buildBookingDetailFromDraft(
          { ...checkoutDraft, customer },
          bookingId,
          response.data?.status || 'PENDING',
        );
        setCreatedBooking(optimisticBooking);
        setBookingStatus(response.data?.status || 'PENDING');
      } catch (error) {
        setBookingStatus('FAILED');
        setDuplicateNotice(null);
        setSubmitError(error.response?.data?.message || error.message || 'Không thể tạo đặt phòng.');
      } finally {
        setDuplicateLoading(false);
      }
    },
    [checkoutDraft, customer, user],
  );

  async function handleSubmit(event) {
    event.preventDefault();
    setSubmitError('');

    const errors = validateForm(customer, payment);
    setFormErrors(errors);
    if (Object.keys(errors).length > 0 || !checkoutDraft) return;

    if (!user?.userId && !user?.id) {
      setSubmitError('Không tìm thấy thông tin người dùng. Vui lòng đăng nhập lại.');
      return;
    }

    await runPlaceBooking();
  }

  async function handleConfirmDuplicateBooking() {
    if (!duplicateNotice?.forceToken) return;
    setDuplicateLoading(true);
    await runPlaceBooking(duplicateNotice.forceToken);
  }

  function handleCancelDuplicateBooking() {
    setDuplicateNotice(null);
    setBookingStatus('IDLE');
  }

  // Đặt lại từ đầu sau khi nhận thông báo thất bại (thường là do race
  // condition hết phòng ở booking-service). Tạo idempotencyKey mới để tránh
  // dính vào saga cũ đã FAILED.
  function handleRetryAfterFailure() {
    idempotencyKeyRef.current = createIdempotencyKey();
    setActiveBookingId('');
    setCreatedBooking(null);
    setFailureReason('');
    setSubmitError('');
    setBookingStatus('IDLE');
  }

  if (!checkoutDraft) {
    return (
      <div className="min-h-screen bg-slate-50">
        <PublicHeader />
        <main className="mx-auto max-w-3xl px-4 py-10">
          <EmptyState
            title="Thiếu thông tin đặt phòng"
            description="Vui lòng chọn khách sạn, ngày lưu trú và loại phòng trước khi thanh toán."
            action={
              <Link to="/" className="accent-button inline-flex">
                Về trang chủ
              </Link>
            }
          />
        </main>
      </div>
    );
  }

  const isSubmitting = bookingStatus === 'PENDING';

  return (
    <div className="min-h-screen bg-slate-50">
      <PublicHeader />

      <DuplicateBookingModal
        open={!!duplicateNotice}
        message={duplicateNotice?.message}
        hint={duplicateNotice?.hint}
        loading={duplicateLoading}
        onConfirm={handleConfirmDuplicateBooking}
        onCancel={handleCancelDuplicateBooking}
      />

      <main className="mx-auto max-w-7xl px-4 py-6 md:px-6 lg:px-8">
        <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-sm font-medium text-blue-700">Thanh toán</p>
            <h1 className="text-2xl font-bold text-slate-900">Xác nhận đặt phòng</h1>
          </div>
          <Link to={`/hotels/${checkoutDraft.hotel.hotelId}`} className="text-sm font-semibold text-blue-700">
            ← Quay lại khách sạn
          </Link>
        </div>

        {/* <section className="mb-6 rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
          <p className="font-semibold">Lưu ý về phiên thanh toán</p>
          <p className="mt-1">
            Nếu bạn gửi lại một yêu cầu đặt phòng giống hệt yêu cầu trong vòng 5 phút trước (cùng khách sạn, ngày ở,
            số khách và loại phòng), hệ thống sẽ hỏi xác nhận trước khi tạo đơn mới, để tránh việc vô tình đặt trùng.
          </p>
        </section> */}

        <section className="mb-6 rounded-lg border border-blue-100 bg-blue-50 p-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="font-semibold text-blue-900">Thông báo đặt phòng</p>
              <p className="text-sm text-blue-700">
                {pushStatus === 'ENABLED' && 'Đã cấp quyền và lưu FCM token.'}
                {pushStatus === 'REGISTERING' && 'Đang xin quyền và đăng ký FCM token...'}
                {pushStatus === 'ERROR' && 'Chưa đăng ký được thông báo.'}
                {pushStatus === 'IDLE' && 'Chưa bật thông báo.'}
              </p>
            </div>
            {pushStatus !== 'ENABLED' && (
              <button type="button" onClick={enablePushNotifications} className="accent-button">
                Bật thông báo
              </button>
            )}
          </div>
          {pushStatus !== 'ENABLED' && (
            <p className="mt-2 text-xs text-blue-700">
              Kết quả đặt phòng (thành công hoặc thất bại) được xử lý bất đồng bộ và báo về qua thông báo đẩy. Bật
              thông báo để nhận cập nhật ngay khi hệ thống xử lý xong, kể cả khi phòng bất ngờ hết do có người khác
              đặt cùng lúc.
            </p>
          )}
          {pushError && <p className="mt-3 rounded bg-white p-2 text-sm text-red-700">{pushError}</p>}
        </section>

        {foregroundMessage && (
          <section className="mb-6 rounded-lg border border-emerald-200 bg-emerald-50 p-4 text-emerald-800">
            <div className="flex justify-between gap-3">
              <div>
                <p className="font-semibold">{foregroundMessage.title}</p>
                <p className="text-sm">{foregroundMessage.body}</p>
              </div>
              <button type="button" onClick={clearForegroundMessage} aria-label="Đóng">
                ×
              </button>
            </div>
          </section>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          <div className="grid gap-6 lg:grid-cols-[1fr_420px]">
            <div className="space-y-6">
              <section className="overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm">
                <img
                  src={checkoutDraft.hotel.coverImageUrl}
                  alt={checkoutDraft.hotel.name}
                  className="h-56 w-full object-cover"
                />
                <div className="p-5">
                  <h2 className="text-xl font-semibold text-slate-900">{checkoutDraft.hotel.name}</h2>
                  <p className="mt-1 text-sm text-slate-600">{checkoutDraft.hotel.address}</p>
                </div>
              </section>

              <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
                <h2 className="text-lg font-semibold text-slate-900">Thông tin lưu trú</h2>
                <dl className="mt-4 grid gap-4 text-sm md:grid-cols-2">
                  <div>
                    <dt className="text-slate-500">Nhận phòng</dt>
                    <dd className="font-semibold text-slate-900">{formatDateVi(checkoutDraft.booking.checkinDate)}</dd>
                  </div>
                  <div>
                    <dt className="text-slate-500">Trả phòng</dt>
                    <dd className="font-semibold text-slate-900">{formatDateVi(checkoutDraft.booking.checkoutDate)}</dd>
                  </div>
                  <div>
                    <dt className="text-slate-500">Số khách</dt>
                    <dd className="font-semibold text-slate-900">{checkoutDraft.booking.guestNum}</dd>
                  </div>
                  <div>
                    <dt className="text-slate-500">Số phòng / số đêm</dt>
                    <dd className="font-semibold text-slate-900">
                      {totalRooms} phòng · {checkoutDraft.booking.nights} đêm
                    </dd>
                  </div>
                </dl>

                <div className="mt-5 space-y-3">
                  {checkoutDraft.roomTypes.map((room) => (
                    <div key={room.roomTypeId} className="rounded-md bg-slate-50 p-3 text-sm">
                      <div className="flex items-start justify-between gap-3">
                        <div>
                          <p className="font-semibold text-slate-900">{room.name}</p>
                          <p className="text-slate-600">
                            {room.quantity} phòng × {checkoutDraft.booking.nights} đêm
                          </p>
                        </div>
                        <p className="font-semibold text-slate-900">{formatCurrency(room.subtotal)}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </section>

              <section className="rounded-lg border border-blue-200 bg-blue-50 p-5 shadow-sm">
                <h2 className="text-lg font-semibold text-blue-950">Tổng tiền</h2>
                <div className="mt-4 space-y-2 text-sm">
                  <div className="flex justify-between">
                    <span>Tạm tính</span>
                    <span>{formatCurrency(checkoutDraft.price.totalPrice)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span>Giảm giá</span>
                    <span>{formatCurrency(checkoutDraft.price.discount || 0)}</span>
                  </div>
                  <div className="flex justify-between border-t border-blue-200 pt-3 text-xl font-bold text-blue-950">
                    <span>Thanh toán</span>
                    <span>{formatCurrency(checkoutDraft.price.finalPrice)}</span>
                  </div>
                </div>
              </section>
            </div>

            <div className="space-y-6">
              <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
                <h2 className="text-lg font-semibold text-slate-900">Thông tin người đặt</h2>
                <div className="mt-4 space-y-4">
                  <label className="block text-sm font-medium text-slate-700">
                    Họ tên
                    <input name="fullName" value={customer.fullName} onChange={updateCustomerField} className="form-input mt-1" />
                    <FieldError message={formErrors.fullName} />
                  </label>
                  <label className="block text-sm font-medium text-slate-700">
                    Email
                    <input name="email" value={customer.email} onChange={updateCustomerField} className="form-input mt-1" />
                    <FieldError message={formErrors.email} />
                  </label>
                  <label className="block text-sm font-medium text-slate-700">
                    Số điện thoại
                    <input name="phone" value={customer.phone} onChange={updateCustomerField} className="form-input mt-1" />
                    <FieldError message={formErrors.phone} />
                  </label>
                </div>
              </section>

              <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
                <h2 className="text-lg font-semibold text-slate-900">Thanh toán bằng thẻ</h2>
                <div className="mt-4 space-y-4">
                  <label className="block text-sm font-medium text-slate-700">
                    Tên chủ thẻ
                    <input name="cardholderName" value={payment.cardholderName} onChange={updatePaymentField} className="form-input mt-1" />
                    <FieldError message={formErrors.cardholderName} />
                  </label>
                  <label className="block text-sm font-medium text-slate-700">
                    Số thẻ
                    <input name="cardNumber" value={payment.cardNumber} onChange={updatePaymentField} className="form-input mt-1" />
                    <FieldError message={formErrors.cardNumber} />
                  </label>
                  <div className="grid grid-cols-2 gap-3">
                    <label className="block text-sm font-medium text-slate-700">
                      Hết hạn
                      <input name="expiryDate" placeholder="MM/YY" value={payment.expiryDate} onChange={updatePaymentField} className="form-input mt-1" />
                      <FieldError message={formErrors.expiryDate} />
                    </label>
                    <label className="block text-sm font-medium text-slate-700">
                      CVV
                      <input name="cvv" value={payment.cvv} onChange={updatePaymentField} className="form-input mt-1" />
                      <FieldError message={formErrors.cvv} />
                    </label>
                  </div>
                </div>
              </section>
            </div>
          </div>

          {activeBookingId && (
            <div className="rounded-lg border border-blue-200 bg-white p-4 text-sm">
              {isSubmitting && (
                <div className="mt-2 flex items-center gap-2 text-blue-700">
                  <Spinner />
                  <span>
                    Hệ thống đang xử lý booking (kiểm tra phòng, thanh toán, xác nhận). Đang chờ kết quả qua thông
                    báo FCM...
                  </span>
                </div>
              )}

              {bookingStatus === 'FAILED' && (
                <div className="mt-3 rounded-md border border-red-200 bg-red-50 p-3 text-red-800">
                  <p className="font-semibold">Đặt phòng thất bại.</p>
                  <p className="mt-1">
                    {failureReason ||
                      'Có thể do phòng vừa hết trong lúc xử lý (nhiều người đặt cùng lúc) hoặc thanh toán không thành công. Vui lòng thử lại hoặc chọn phòng khác.'}
                  </p>
                  <button
                    type="button"
                    onClick={handleRetryAfterFailure}
                    className="accent-button mt-3 inline-flex"
                  >
                    Thử đặt lại
                  </button>
                </div>
              )}
            </div>
          )}

          {submitError && <p className="rounded-lg bg-red-50 p-3 text-sm text-red-700">{submitError}</p>}

          <button type="submit" disabled={isSubmitting} className="primary-button w-full justify-center py-3 disabled:opacity-60">
            {isSubmitting ? 'Đang xử lý...' : 'Xác nhận đặt phòng'}
          </button>

          {createdBooking?.bookingId && bookingStatus === 'CONFIRMED' && (
            <Link to={`/bookings/${createdBooking.bookingId}`} state={{ booking: createdBooking }} className="accent-button inline-flex">
              Xem chi tiết đặt phòng
            </Link>
          )}
        </form>
      </main>
    </div>
  );
}