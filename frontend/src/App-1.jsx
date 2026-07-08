import { useEffect, useMemo, useRef, useState } from 'react';
import { listenForegroundMessages, requestFcmToken } from './firebase';

const API_BASE_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/$/, '');
const SESSION_STORAGE_KEY = 'hotel-booking-demo-session';

function readStoredSession() {
  try {
    return JSON.parse(localStorage.getItem(SESSION_STORAGE_KEY)) || null;
  } catch {
    localStorage.removeItem(SESSION_STORAGE_KEY);
    return null;
  }
}

function LoginScreen({ onLogin }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleLogin(event) {
    event.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await fetch(`${API_BASE_URL}/api/users/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(data.message || 'Đăng nhập thất bại.');
      }
      if (!data.accessToken || !data.user) {
        throw new Error('Phản hồi đăng nhập thiếu accessToken hoặc user.');
      }
      await onLogin(data);
    } catch (loginError) {
      setError(loginError.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-100 p-4">
      <form onSubmit={handleLogin} className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-lg">
        <p className="text-sm font-medium text-blue-600">Hotel Booking FCM Demo</p>
        <h1 className="mt-1 text-2xl font-bold text-slate-800">Đăng nhập</h1>
        <p className="mt-2 text-sm text-slate-500">
          Sau khi đăng nhập, trình duyệt sẽ yêu cầu quyền nhận thông báo đặt phòng.
        </p>

        <label className="mt-6 block text-sm font-medium text-slate-700">
          Email
          <input
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            className="mt-1 w-full rounded-lg border p-2.5"
            placeholder="customer@example.com"
            required
          />
        </label>

        <label className="mt-4 block text-sm font-medium text-slate-700">
          Mật khẩu
          <input
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            className="mt-1 w-full rounded-lg border p-2.5"
            required
          />
        </label>

        {error && <p className="mt-4 rounded-lg bg-red-50 p-3 text-sm text-red-700">{error}</p>}

        <button
          type="submit"
          disabled={loading}
          className="mt-5 w-full rounded-lg bg-blue-600 px-4 py-2.5 font-semibold text-white hover:bg-blue-700 disabled:opacity-60"
        >
          {loading ? 'Đang đăng nhập...' : 'Đăng nhập và bật thông báo'}
        </button>
      </form>
    </main>
  );
}

function toLocalDateInputValue(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

const HOTEL = {
  id: 'b0000000-0000-0000-0000-000000000001',
  name: 'Hanoi Heritage Hotel',
  description:
    'Khách sạn 5 sao nằm ngay trung tâm phố cổ Hà Nội, cách Hồ Hoàn Kiếm 200m.',
  address: '12 Hàng Bạc, Hoàn Kiếm, Hà Nội',
  imageUrl: 'https://images.unsplash.com/photo-1566073771259-6a8506099945'
};

const ROOM_TYPES = [
  {
    id: 'd1000000-0000-0000-0000-000000000001',
    name: 'Standard Room',
    description: 'Phòng tiêu chuẩn thoải mái, view thành phố, phù hợp cho cặp đôi hoặc khách đi công tác.',
    maxGuests: 2,
    bedCounts: 1,
    basePricePerNight: 1200000,
    availableQuantity: 10,
    area: 28,
    imageUrl: 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304'
  },
  {
    id: 'd1000000-0000-0000-0000-000000000002',
    name: 'Deluxe Room',
    description: 'Phòng Deluxe rộng rãi với nội thất cao cấp, ban công nhỏ nhìn ra phố cổ.',
    maxGuests: 2,
    bedCounts: 1,
    basePricePerNight: 1800000,
    availableQuantity: 8,
    area: 35,
    imageUrl: 'https://images.unsplash.com/photo-1611892440504-42a792e24d32'
  },
  {
    id: 'd1000000-0000-0000-0000-000000000003',
    name: 'Superior Twin Room',
    description: 'Phòng Superior với 2 giường đơn, phù hợp cho 2 người đi du lịch cùng nhau.',
    maxGuests: 2,
    bedCounts: 2,
    basePricePerNight: 2000000,
    availableQuantity: 6,
    area: 32,
    imageUrl: 'https://images.unsplash.com/photo-1595576508898-0ad5c879a061'
  },
  {
    id: 'd1000000-0000-0000-0000-000000000004',
    name: 'Junior Suite',
    description: 'Phòng Suite nhỏ với phòng khách riêng, bồn tắm đứng, tầm nhìn ra Hồ Hoàn Kiếm.',
    maxGuests: 3,
    bedCounts: 1,
    basePricePerNight: 3500000,
    availableQuantity: 4,
    area: 55,
    imageUrl: 'https://images.unsplash.com/photo-1590490360182-c33d57733427'
  },
  {
    id: 'd1000000-0000-0000-0000-000000000005',
    name: 'Presidential Suite',
    description: 'Suite hạng sang rộng 80m², phòng ăn riêng, bồn tắm jacuzzi, tầm nhìn toàn cảnh phố cổ.',
    maxGuests: 4,
    bedCounts: 1,
    basePricePerNight: 8500000,
    availableQuantity: 2,
    area: 80,
    imageUrl: 'https://images.unsplash.com/photo-1571896349842-33c89424de2d'
  }
];

const today = toLocalDateInputValue(new Date());
const tomorrow = toLocalDateInputValue(new Date(Date.now() + 86400000));

function formatCurrency(value) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value || 0);
}

function nightsBetween(checkin, checkout) {
  if (!checkin || !checkout) {
    return 0;
  }
  const c1 = new Date(checkin + 'T00:00:00');
  const c2 = new Date(checkout + 'T00:00:00');
  const diff = Math.floor((c2 - c1) / 86400000);
  return diff > 0 ? diff : 0;
}

function createFakeToken() {
  return `tok_${Math.random().toString(36).slice(2, 12)}`;
}

function createIdempotencyKey() {
  if (window.crypto && window.crypto.randomUUID) {
    return window.crypto.randomUUID();
  }
  return `idemp_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`;
}

function Spinner() {
  return <div className="h-8 w-8 animate-spin rounded-full border-4 border-slate-300 border-t-blue-600" />;
}

function getBookingStatusFromNotification(payload) {
  const data = payload.data || {};
  if (data.status) {
    return data.status.toUpperCase();
  }
  const eventType = (data.eventType || '').toUpperCase();
  if (eventType.includes('CONFIRM')) {
    return 'CONFIRMED';
  }
  if (eventType.includes('FAIL')) {
    return 'FAILED';
  }
  if (eventType.includes('CANCEL')) {
    return 'CANCELLED';
  }
  return '';
}

function getBookingNotificationTitle(payload, status) {
  const data = payload.data || {};
  if (status === 'CONFIRMED') {
    return 'Đặt phòng thành công';
  }
  if (status === 'CANCELLED') {
    return 'Đặt phòng đã hủy';
  }
  if (status === 'FAILED') {
    return 'Đặt phòng thất bại';
  }
  return data.title || payload.notification?.title || 'Cập nhật đặt phòng';
}

function getBookingNotificationBody(payload) {
  const data = payload.data || {};
  return data.body || payload.notification?.body || 'Trạng thái đặt phòng vừa thay đổi.';
}

async function showBrowserNotification(title, body, data = {}) {
  if (!('Notification' in window) || Notification.permission !== 'granted') {
    return;
  }

  if ('serviceWorker' in navigator) {
    const registration = await navigator.serviceWorker.ready;
    await registration.showNotification(title, {
      body,
      data
    });
    return;
  }

  new Notification(title, {
    body,
    data
  });
}

export default function App() {
  const [session, setSession] = useState(readStoredSession);
  const [pushStatus, setPushStatus] = useState('IDLE');
  const [pushError, setPushError] = useState('');
  const [foregroundMessage, setForegroundMessage] = useState(null);
  const [checkin, setCheckin] = useState(tomorrow);
  const [checkout, setCheckout] = useState('');
  const [numAdults, setNumAdults] = useState(2);
  const [payment, setPayment] = useState({ cardNumber: '0123456789', cardholderName: 'ĐOÀN QUANG MINH', expiryDate: '05/34', cvv: '111' });
  const [selectedRooms, setSelectedRooms] = useState({
    'd1000000-0000-0000-0000-000000000001': 1
  });
  const [submitError, setSubmitError] = useState('');
  const [submitLoading, setSubmitLoading] = useState(false);
  const [bookingId, setBookingId] = useState('');
  const [bookingStatus, setBookingStatus] = useState('IDLE');
  const [bookingDetails, setBookingDetails] = useState(null);
  const activeBookingIdRef = useRef('');

  const currentUser = useMemo(() => {
    const user = session?.user || {};
    return {
      id: user.userId || user.id,
      name: user.name || user.fullName || user.email,
      email: user.email || '',
      phone: user.phone || ''
    };
  }, [session]);

  const nights = useMemo(() => nightsBetween(checkin, checkout), [checkin, checkout]);

  const selectedRoomItems = useMemo(
    () =>
      ROOM_TYPES.filter((room) => Number(selectedRooms[room.id] || 0) > 0).map((room) => ({
        roomTypeId: room.id,
        name: room.name,
        bedCount: room.bedCounts,
        bookingQuantity: Number(selectedRooms[room.id]),
        totalQuantity: room.availableQuantity,
        price: room.basePricePerNight,
      })),
    [selectedRooms]
  );

  const totalAmount = useMemo(
    () => selectedRoomItems.reduce((sum, item) => sum + item.price * item.bookingQuantity * nights, 0),
    [selectedRoomItems, nights]
  );

  async function handleFcmBookingUpdate(payload, { showNotification = false } = {}) {
    const data = payload.data || {};
    console.log('Received FCM booking update:', data);
    const notificationStatus = getBookingStatusFromNotification(payload);
    const title = getBookingNotificationTitle(payload, notificationStatus);
    const body = getBookingNotificationBody(payload);
    const activeBookingId = activeBookingIdRef.current || bookingId;
    const isActiveBooking = data.bookingId ? data.bookingId === activeBookingId : true;

    if (showNotification) {
      try {
        await showBrowserNotification(title, body, data);
      } catch (error) {
        setPushError(error.message);
      }
    }

    if (!isActiveBooking) {
      return;
    }

    setForegroundMessage({
      title,
      body,
      data
    });

    if (notificationStatus) {
      setBookingStatus(notificationStatus);
    }
  }

  useEffect(() => {
    if (!session) {
      return undefined;
    }

    let unsubscribe;
    listenForegroundMessages((payload) => handleFcmBookingUpdate(payload, { showNotification: true }))
    /*
    listenForegroundMessages((payload) => {
      const data = payload.data || {};
      setForegroundMessage({
        title: payload.notification?.title || 'Cập nhật đặt phòng',
        body: payload.notification?.body || 'Trạng thái đặt phòng vừa thay đổi.',
        data
      });

      const notificationStatus = getBookingStatusFromNotification(payload);
      const isActiveBooking =
        !data.bookingId || !activeBookingIdRef.current || data.bookingId === activeBookingIdRef.current;
      if (notificationStatus && isActiveBooking) {
        setBookingStatus(notificationStatus);
      }
    })
    */
      .then((stopListening) => {
        unsubscribe = stopListening;
      })
      .catch((error) => setPushError(error.message));

    return () => unsubscribe?.();
  }, [session, bookingId]);

  useEffect(() => {
    if (!session || !('serviceWorker' in navigator)) {
      return undefined;
    }

    const handleServiceWorkerMessage = (event) => {
      if (event.data?.type !== 'FCM_BOOKING_UPDATE') {
        return;
      }
      handleFcmBookingUpdate(event.data.payload);
    };

    navigator.serviceWorker.addEventListener('message', handleServiceWorkerMessage);
    return () => navigator.serviceWorker.removeEventListener('message', handleServiceWorkerMessage);
  }, [session, bookingId]);

  useEffect(() => {
    if (session && 'Notification' in window && Notification.permission === 'granted') {
      enablePushNotifications(session);
    }
  }, [session]);

  async function enablePushNotifications(activeSession = session) {
    if (!activeSession) {
      return;
    }

    setPushStatus('REGISTERING');
    setPushError('');

    try {
      const fcmToken = await requestFcmToken();
      const userId = activeSession.user?.userId || activeSession.user?.id;
      if (!userId) {
        throw new Error('Không tìm thấy userId trong phiên đăng nhập.');
      }

      const response = await fetch(`${API_BASE_URL}/api/notifications/device-token`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${activeSession.accessToken}`,
          'Content-Type': 'application/json',
          'X-User-Id': userId
        },
        body: JSON.stringify({ fcmToken, platform: 'WEB' })
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(data.message || 'Không thể lưu FCM token vào Notification Service.');
      }

      setPushStatus('ENABLED');
    } catch (error) {
      setPushStatus('ERROR');
      setPushError(error.message);
      console.log("Push error: ", error.message);
    }
  }

  async function handleLoggedIn(authSession) {
    localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(authSession));
    setSession(authSession);
    await enablePushNotifications(authSession);
  }

  function handleLogout() {
    localStorage.removeItem(SESSION_STORAGE_KEY);
    activeBookingIdRef.current = '';
    setSession(null);
    setPushStatus('IDLE');
    setPushError('');
    setForegroundMessage(null);
  }

  function validateForm() {
    if (!checkin || !checkout) {
      return 'Vui long chon checkin va checkout.';
    }
    if (new Date(checkin) < new Date(today)) {
      return 'Checkin phai la ngay hien tai hoac tuong lai.';
    }
    if (nights <= 0) {
      return 'Checkout phai sau checkin it nhat 1 ngay.';
    }
    if (!selectedRoomItems.length) {
      return 'Vui long chon it nhat 1 loai phong.';
    }
    if (!payment.cardNumber || !payment.cardholderName || !payment.expiryDate || !payment.cvv) {
      return 'Vui long nhap day du thong tin the thanh toan.';
    }
    return '';
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setSubmitError('');

    const error = validateForm();
    if (error) {
      setSubmitError(error);
      return;
    }

    activeBookingIdRef.current = '';
    setBookingId('');
    setBookingStatus('PENDING');
    setBookingDetails(null);

    const payload = {
      userId: currentUser.id,
      hotelId: HOTEL.id,
      roomTypeList: selectedRoomItems.map((room) => ({
        roomTypeId: room.roomTypeId,
        name: room.name,
        bedCount: room.bedCount,
        bookingQuantity: room.bookingQuantity,
        totalQuantity: room.totalQuantity,
        price: room.price
      })),
      checkin,
      checkout,
      numAdults: Number(numAdults),
      totalAmount,
      currency: 'VND',
      paymentMethod: 'CREDIT_CARD',
      paymentToken: createFakeToken(),
      idempotencyKey: createIdempotencyKey()
    };

    setSubmitLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/place-booking`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(data.message || 'Dat phong that bai.');
      }

      const createdBookingId = data.bookingId;
      if (!createdBookingId) {
        throw new Error('Khong nhan duoc bookingId tu backend.');
      }

      activeBookingIdRef.current = createdBookingId;
      setBookingId(createdBookingId);
      setBookingStatus((currentStatus) =>
        ['CONFIRMED', 'FAILED', 'CANCELLED'].includes(currentStatus) ? currentStatus : 'PENDING'
      );
      setBookingDetails({
        bookingId: createdBookingId,
        status: 'PENDING',
        checkin,
        checkout,
        totalAmount,
        roomTypeList: selectedRoomItems,
        hotel: { name: HOTEL.name }
      });
    } catch (submitErr) {
      setSubmitError(submitErr.message);
    } finally {
      setSubmitLoading(false);
    }
  }

  if (!session) {
    return <LoginScreen onLogin={handleLoggedIn} />;
  }

  return (
    <main className="mx-auto max-w-5xl p-4 md:p-8">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <p className="text-sm text-slate-500">Đăng nhập: {currentUser.email}</p>
          <h1 className="text-3xl font-bold text-slate-800">Demo đặt phòng khách sạn</h1>
        </div>
        <button type="button" onClick={handleLogout} className="rounded border px-4 py-2 text-sm">
          Đăng xuất
        </button>
      </div>

      <section className="mb-6 rounded-xl border border-blue-100 bg-blue-50 p-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="font-semibold text-blue-900">Push notification</p>
            <p className="text-sm text-blue-700">
              {pushStatus === 'ENABLED' && 'Đã cấp quyền và lưu FCM token.'}
              {pushStatus === 'REGISTERING' && 'Đang lấy và đăng ký FCM token...'}
              {pushStatus === 'ERROR' && 'Chưa đăng ký được FCM token.'}
              {pushStatus === 'IDLE' && 'Chưa bật thông báo.'}
            </p>
          </div>
          {pushStatus !== 'ENABLED' && (
            <button
              type="button"
              onClick={() => enablePushNotifications()}
              disabled={pushStatus === 'REGISTERING'}
              className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white disabled:opacity-60"
            >
              Bật thông báo
            </button>
          )}
        </div>
        {pushError && <p className="mt-3 rounded bg-white p-2 text-sm text-red-700">{pushError}</p>}
      </section>

      {foregroundMessage && (
        <section className="mb-6 rounded-xl border border-emerald-200 bg-emerald-50 p-4">
          <div className="flex justify-between gap-3">
            <div>
              <p className="font-semibold text-emerald-900">{foregroundMessage.title}</p>
              <p className="text-sm text-emerald-800">{foregroundMessage.body}</p>
            </div>
            <button type="button" onClick={() => setForegroundMessage(null)} aria-label="Đóng">
              ×
            </button>
          </div>
        </section>
      )}

      <section className="mb-6 overflow-hidden rounded-xl bg-white shadow">
        <img className="h-60 w-full object-cover" src={HOTEL.imageUrl} alt={HOTEL.name} />
        <div className="p-5">
          <p className="text-sm text-slate-500">ID: {HOTEL.id}</p>
          <h2 className="mt-1 text-2xl font-semibold">{HOTEL.name}</h2>
          <p className="mt-2 text-slate-600">{HOTEL.description}</p>
          <p className="mt-2 text-sm text-slate-500">{HOTEL.address}</p>
        </div>
      </section>

      <form onSubmit={handleSubmit} className="space-y-6 rounded-xl bg-white p-5 shadow">
        <section>
          <h3 className="mb-3 text-lg font-semibold">Thong tin nguoi dat</h3>
          <div className="grid gap-3 md:grid-cols-2">
            <input className="rounded border bg-slate-100 p-2" value={`${currentUser.id} - ${currentUser.name}`} readOnly />
            <input className="rounded border bg-slate-100 p-2" value={currentUser.email} readOnly />
            <input className="rounded border bg-slate-100 p-2 md:col-span-2" value={currentUser.phone} readOnly />
          </div>
        </section>

        <section>
          <h3 className="mb-3 text-lg font-semibold">Thong tin dat phong</h3>
          <div className="mb-4 grid gap-3 md:grid-cols-3">
            <label className="text-sm">
              Checkin
              <input
                type="date"
                min={today}
                value={checkin}
                onChange={(e) => setCheckin(e.target.value)}
                className="mt-1 w-full rounded border p-2"
                required
              />
            </label>
            <label className="text-sm">
              Checkout
              <input
                type="date"
                min={checkin || today}
                value={checkout}
                onChange={(e) => setCheckout(e.target.value)}
                className="mt-1 w-full rounded border p-2"
                required
              />
            </label>
            <label className="text-sm">
              So nguoi lon
              <input
                type="number"
                min="1"
                value={numAdults}
                onChange={(e) => setNumAdults(e.target.value)}
                className="mt-1 w-full rounded border p-2"
                required
              />
            </label>
          </div>

          <div className="space-y-3">
            {ROOM_TYPES.map((room) => {
              const quantity = Number(selectedRooms[room.id] || 0);
              return (
                <div key={room.id} className="rounded-lg border p-3">
                  <div className="mb-2 flex items-start justify-between gap-3">
                    <div>
                      <p className="font-medium">{room.name}</p>
                      <p className="text-sm text-slate-500">{room.description}</p>
                      <p className="mt-1 text-xs text-slate-500">
                        {room.maxGuests} khach | {room.bedCounts} giuong | {room.area} m2 | Con {room.availableQuantity}
                      </p>
                    </div>
                    <span className="text-sm font-semibold text-blue-700">{formatCurrency(room.basePricePerNight)} / dem</span>
                  </div>
                  <div className="flex items-center gap-3">
                    <label className="text-sm">So luong:</label>
                    <input
                      type="number"
                      min="0"
                      max={room.availableQuantity}
                      value={quantity}
                      onChange={(e) =>
                        setSelectedRooms((prev) => ({
                          ...prev,
                          [room.id]: e.target.value
                        }))
                      }
                      className="w-24 rounded border p-2"
                    />
                  </div>
                </div>
              );
            })}
          </div>
        </section>

        <section>
          <h3 className="mb-3 text-lg font-semibold">Thanh toan (demo)</h3>
          <div className="grid gap-3 md:grid-cols-2">
            <input
              className="rounded border p-2"
              placeholder="So the"
              value={payment.cardNumber}
              onChange={(e) => setPayment((prev) => ({ ...prev, cardNumber: e.target.value }))}
            />
            <input
              className="rounded border p-2"
              placeholder="Ten chu the"
              value={payment.cardholderName}
              onChange={(e) => setPayment((prev) => ({ ...prev, cardholderName: e.target.value }))}
            />
            <input
              className="rounded border p-2"
              placeholder="MM/YY"
              value={payment.expiryDate}
              onChange={(e) => setPayment((prev) => ({ ...prev, expiryDate: e.target.value }))}
            />
            <input
              className="rounded border p-2"
              placeholder="CVV"
              value={payment.cvv}
              onChange={(e) => setPayment((prev) => ({ ...prev, cvv: e.target.value }))}
            />
          </div>
        </section>

        <section className="rounded-lg bg-slate-50 p-4">
          <p className="text-sm text-slate-500">Đơn vị tiền: VND</p>
          <p className="mt-2 text-xl font-bold text-slate-800">Tổng tiền: {formatCurrency(totalAmount)}</p>
          <p className="text-sm text-slate-500">Số đêm: {nights}</p>
        </section>

        {submitError && <p className="rounded bg-red-50 p-3 text-sm text-red-600">{submitError}</p>}

        <button
          type="submit"
          disabled={submitLoading}
          className="inline-flex items-center rounded bg-blue-600 px-5 py-2.5 font-medium text-white hover:bg-blue-700 disabled:opacity-60"
        >
          {submitLoading ? 'Dang gui...' : 'Đặt phòng'}
        </button>
      </form>

      {bookingId && (
        <section className="mt-6 rounded-xl bg-white p-5 shadow">
          <h3 className="mb-3 text-lg font-semibold">Trạng thái booking: {bookingStatus}</h3>

          {bookingStatus === 'PENDING' && (
            <div className="flex items-center gap-3 text-blue-700">
              <Spinner />
              <p>Hệ thống đang xử lý booking. Đang chờ kết quả qua thông báo FCM...</p>
            </div>
          )}

          {bookingStatus === 'CONFIRMED' && (
            <div className="rounded bg-emerald-50 p-4 text-emerald-700">
              <p className="font-semibold">Đặt phòng thành công!</p>
              <p>Booking ID: {bookingDetails?.bookingId || bookingId}</p>
              <p>Khach san: {bookingDetails?.hotel?.name || HOTEL.name}</p>
              <p>
                Checkin - Checkout: {bookingDetails?.checkin || checkin} - {bookingDetails?.checkout || checkout}
              </p>
              <p>Tong tien: {formatCurrency(bookingDetails?.totalAmount || totalAmount)}</p>
            </div>
          )}

          {(bookingStatus === 'CANCELLED' || bookingStatus === 'FAILED') && (
            <div className="rounded bg-red-50 p-4 text-red-700">
              <p className="font-semibold">Đặt phòng thất bại.</p>
              <p>Booking ID: {bookingDetails?.bookingId || bookingId}</p>
              <p>Trang thai: {bookingStatus}</p>
            </div>
          )}
        </section>
      )}
    </main>
  );
}

