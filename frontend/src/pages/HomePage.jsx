import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import useAuth from '../features/auth/hooks/useAuth';
import useBookingNotifications from '../features/booking/hooks/useBookingNotifications';
import PublicHeader from '../shared/components/PublicHeader';

function toDateInputValue(date) {
  return date.toISOString().slice(0, 10);
}

const today = toDateInputValue(new Date());
const tomorrow = toDateInputValue(new Date(Date.now() + 86400000));
const DEMO_HOTEL_ID = 'b0000000-0000-0000-0000-000000000001';

const features = [
  {
    title: 'Giá rõ ràng',
    description: 'So sánh lựa chọn phù hợp với ngân sách của bạn.',
    icon: '₫',
  },
  {
    title: 'Đặt phòng đơn giản',
    description: 'Hoàn thành hành trình chỉ trong vài bước.',
    icon: '✓',
  },
  {
    title: 'Hỗ trợ hành trình',
    description: 'Theo dõi trạng thái đặt phòng tại một nơi.',
    icon: '24',
  },
];

export default function HomePage() {
  const navigate = useNavigate();
  const { user, isAuthenticated } = useAuth();
  const isHotelStaff = user?.roles?.includes('HOTEL_STAFF');
  const [searchMessage, setSearchMessage] = useState('');
  const { pushStatus, pushError, enablePushNotifications } = useBookingNotifications({ user });

  function handleSearch(event) {
    event.preventDefault();
    setSearchMessage('Tính năng tìm kiếm khách sạn sẽ được kết nối ở bước tiếp theo.');
  }

  function handleSubscribeNotifications() {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: '/' } });
      return;
    }
    enablePushNotifications();
  }

  return (
    <div className="min-h-screen bg-slate-50">
      <PublicHeader />
      <section className="bg-gradient-to-br from-slate-800 via-blue-800 to-sky-700 pb-14 text-white">
        <div className="mx-auto max-w-7xl px-4 pt-10 md:px-6 md:pt-14 lg:px-8">
          <p className="text-sm font-semibold text-blue-200">Hơn cả một nơi để nghỉ</p>
          <h1 className="mt-2 max-w-3xl text-3xl font-bold leading-tight md:text-5xl">
            Tìm chỗ nghỉ phù hợp cho hành trình tiếp theo
          </h1>
          <p className="mt-4 max-w-2xl text-base text-blue-100 md:text-lg">
            Khám phá khách sạn, khu nghỉ dưỡng và những điểm đến đáng nhớ.
          </p>

          <form
            id="search"
            onSubmit={handleSearch}
            className="mt-8 grid gap-1 rounded-lg bg-amber-400 p-1.5 text-slate-900 shadow-lg md:grid-cols-[2fr_1fr_1fr_1fr_auto]"
          >
            <label className="rounded-md bg-white p-2">
              <span className="block text-xs font-semibold text-slate-600">Điểm đến</span>
              <input
                required
                name="destination"
                placeholder="Bạn muốn đi đâu?"
                className="mt-1 w-full border-0 p-0 text-sm outline-none placeholder:text-slate-400"
              />
            </label>
            <label className="rounded-md bg-white p-2">
              <span className="block text-xs font-semibold text-slate-600">Nhận phòng</span>
              <input type="date" name="checkin" min={today} defaultValue={today} className="mt-1 w-full text-sm outline-none" />
            </label>
            <label className="rounded-md bg-white p-2">
              <span className="block text-xs font-semibold text-slate-600">Trả phòng</span>
              <input type="date" name="checkout" min={tomorrow} defaultValue={tomorrow} className="mt-1 w-full text-sm outline-none" />
            </label>
            <label className="rounded-md bg-white p-2">
              <span className="block text-xs font-semibold text-slate-600">Khách</span>
              <select name="guests" className="mt-1 w-full bg-white text-sm outline-none">
                <option>2 người lớn</option>
                <option>1 người lớn</option>
                <option>3 người lớn</option>
                <option>4 người lớn</option>
              </select>
            </label>
            <button
              type="submit"
              className="rounded-md bg-sky-700 px-6 py-3 text-sm font-semibold text-white hover:bg-sky-800"
            >
              Tìm kiếm
            </button>
          </form>
          {searchMessage && (
            <p className="mt-3 rounded-md bg-white/10 px-3 py-2 text-sm text-blue-50 ring-1 ring-white/15" role="status">
              {searchMessage}
            </p>
          )}

          <div className="mt-5 flex flex-wrap gap-3">
            <Link
              to={`/hotels/${DEMO_HOTEL_ID}?checkinDate=${today}&checkoutDate=${tomorrow}&guestNum=2&roomNum=1`}
              className="inline-flex items-center justify-center rounded-md bg-white px-5 py-3 text-sm font-semibold text-blue-700 shadow-sm hover:bg-blue-50"
            >
              Xem chi tiết khách sạn demo
            </Link>

            {isHotelStaff && (
              <>
                <Link
                  to={`/staff/hotels/${DEMO_HOTEL_ID}/today-checkins`}
                  className="inline-flex items-center justify-center rounded-md bg-white px-5 py-3 text-sm font-semibold text-blue-700 shadow-sm hover:bg-blue-50"
                >
                  Xem check-in hôm nay (demo)
                </Link>
                <Link
                  to={`/staff/hotels/${DEMO_HOTEL_ID}/checkins`}
                  className="inline-flex items-center justify-center rounded-md bg-white px-5 py-3 text-sm font-semibold text-blue-700 shadow-sm hover:bg-blue-50"
                >
                  Check-out (demo)
                </Link>
              </>
            )}
          </div>
        </div>
      </section>

      <main className="mx-auto max-w-7xl px-4 py-10 md:px-6 lg:px-8 space-y-8">
        {/* PROMOTION NOTIFICATION SUBSCRIPTION CARD */}
        <section className="rounded-xl border border-amber-200 bg-amber-50 p-6 shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div>
              <div className="flex items-center gap-2">
                <span className="text-2xl">🎁</span>
                <h2 className="text-lg font-bold text-amber-950">Đăng ký nhận ưu đãi &amp; khuyến mãi mới</h2>
              </div>
              <p className="mt-1 text-sm text-amber-800">
                Nhận thông báo push realtime ngay khi có mã giảm giá hoặc chương trình ưu đãi đặc biệt từ khách sạn.
              </p>
              {pushStatus === 'ENABLED' && (
                <p className="mt-2 text-xs font-semibold text-green-700">
                  ✓ Đã đăng ký nhận tin thành công! Bạn sẽ nhận thông báo đẩy FCM ngay khi có ưu đãi mới.
                </p>
              )}
              {pushError && (
                <p className="mt-2 text-xs font-medium text-red-600">
                  ✗ Lỗi đăng ký: {pushError}
                </p>
              )}
            </div>

            <div>
              {pushStatus === 'ENABLED' ? (
                <span className="inline-flex items-center gap-1.5 rounded-lg bg-green-100 px-4 py-2.5 text-sm font-semibold text-green-800">
                  ✓ Đã đăng ký thông báo
                </span>
              ) : (
                <button
                  type="button"
                  onClick={handleSubscribeNotifications}
                  disabled={pushStatus === 'REGISTERING'}
                  className="rounded-lg bg-amber-600 px-5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-amber-700 disabled:opacity-60"
                >
                  {pushStatus === 'REGISTERING' ? 'Đang đăng ký FCM...' : 'Bật thông báo ưu đãi ngay'}
                </button>
              )}
            </div>
          </div>
        </section>

        <section id="benefits">
          <h2 className="text-xl font-semibold text-slate-900">Vì sao chọn HotelHub?</h2>
          <div className="mt-5 grid gap-4 md:grid-cols-3">
            {features.map((feature) => (
              <article key={feature.title} className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
                <span className="grid h-10 w-10 place-items-center rounded-md bg-blue-50 text-sm font-bold text-blue-700">
                  {feature.icon}
                </span>
                <h3 className="mt-4 text-lg font-semibold text-slate-900">{feature.title}</h3>
                <p className="mt-1 text-sm leading-6 text-slate-600">{feature.description}</p>
              </article>
            ))}
          </div>
        </section>
      </main>

      <footer id="support" className="border-t border-slate-200 bg-white">
        <div className="mx-auto max-w-7xl px-4 py-6 text-sm text-slate-500 md:px-6 lg:px-8">
          HotelHub · Nền tảng đặt phòng khách sạn
        </div>
      </footer>
    </div>
  );
}