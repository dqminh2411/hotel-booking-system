import useAuth from '../../features/auth/hooks/useAuth';
import useBookingNotifications from '../../features/booking/hooks/useBookingNotifications';

export default function GlobalNotificationToast() {
  const { user } = useAuth();
  const { foregroundMessage, clearForegroundMessage } = useBookingNotifications({ user });

  if (!foregroundMessage) return null;

  const isPromotion =
    foregroundMessage.data?.promotionId ||
    foregroundMessage.data?.couponId ||
    (foregroundMessage.data?.eventType &&
      (foregroundMessage.data.eventType.includes('Promotion') || foregroundMessage.data.eventType.includes('Coupon')));

  return (
    <div className="fixed top-5 right-5 z-50 max-w-md rounded-xl border border-blue-200 bg-white p-4 shadow-2xl transition-all animate-bounce-once">
      <div className="flex items-start justify-between gap-3">
        <div className="flex gap-3">
          <div className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-lg ${isPromotion ? 'bg-amber-100 text-amber-700' : 'bg-blue-100 text-blue-700'}`}>
            {isPromotion ? '🎉' : '🔔'}
          </div>
          <div>
            <h4 className="font-semibold text-slate-900">{foregroundMessage.title || 'Thông báo khuyến mãi mới'}</h4>
            <p className="mt-1 text-sm text-slate-600">{foregroundMessage.body}</p>
            {foregroundMessage.reason && (
              <p className="mt-1 text-xs font-medium text-red-600">{foregroundMessage.reason}</p>
            )}
          </div>
        </div>
        <button
          type="button"
          onClick={clearForegroundMessage}
          className="rounded-lg p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-700"
          aria-label="Đóng"
        >
          ✕
        </button>
      </div>
    </div>
  );
}
