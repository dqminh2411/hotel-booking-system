import { Link, useLocation } from 'react-router-dom';
import BrandLogo from '@/shared/components/BrandLogo';
import { ADMIN_NAV_ITEMS } from '@/shared/constants/adminNav';
import { ROUTES } from '@/shared/constants/routes';
import { ADMIN_ICON_BY_KEY, CloseIcon } from './AdminIcons';

function isItemActive(item, pathname) {
  const prefix = item.matchPrefix ?? item.path;
  return pathname === prefix || pathname.startsWith(`${prefix}/`);
}

/**
 * Sidebar trai cho khu vuc Admin.
 * - Desktop (lg+): luon hien, chiem 16rem (256px), noi dung ben phai chua
 *   padding-left tuong ung (xem AdminLayout.jsx).
 * - Mobile: an mac dinh, truot ra tu trai khi isOpen = true, kem lop phu (backdrop).
 *
 * Props: isOpen (mobile), onClose (mobile)
 */
export function AdminSidebar({ isOpen, onClose }) {
  const { pathname } = useLocation();

  return (
    <>
      {/* Backdrop - chi hien tren mobile khi sidebar dang mo */}
      {isOpen && (
        <div
          className="fixed inset-0 z-30 bg-slate-900/50 lg:hidden"
          onClick={onClose}
          aria-hidden="true"
        />
      )}

      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-64 flex-col bg-gradient-to-b from-slate-900 via-slate-900 to-slate-800 text-slate-100 transition-transform duration-200 ease-out lg:translate-x-0 ${
          isOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="flex h-16 items-center justify-between border-b border-white/10 px-5">
          <BrandLogo inverse />
          <button
            type="button"
            onClick={onClose}
            className="rounded-md p-1.5 text-slate-300 hover:bg-white/10 hover:text-white lg:hidden"
            aria-label="Đóng menu"
          >
            <CloseIcon className="h-5 w-5" />
          </button>
        </div>

        <nav className="flex-1 space-y-1 overflow-y-auto px-3 py-4" aria-label="Điều hướng quản trị">
          {ADMIN_NAV_ITEMS.map((item) => {
            const Icon = ADMIN_ICON_BY_KEY[item.key];
            const active = !item.disabled && isItemActive(item, pathname);

            if (item.disabled) {
              return (
                <div
                  key={item.key}
                  className="flex cursor-not-allowed items-center justify-between gap-3 rounded-md px-3 py-2.5 text-sm font-medium text-slate-500"
                  title="Sắp ra mắt"
                >
                  <span className="flex items-center gap-3">
                    <Icon className="h-5 w-5" />
                    {item.label}
                  </span>
                  <span className="rounded-full bg-white/5 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-slate-500">
                    Sắp có
                  </span>
                </div>
              );
            }

            return (
              <Link
                key={item.key}
                to={item.path}
                onClick={onClose}
                className={`flex items-center gap-3 rounded-md border-l-2 px-3 py-2.5 text-sm font-medium transition-colors ${
                  active
                    ? 'border-sky-400 bg-white/10 text-white'
                    : 'border-transparent text-slate-300 hover:bg-white/5 hover:text-white'
                }`}
              >
                <Icon className={`h-5 w-5 ${active ? 'text-sky-400' : 'text-slate-400'}`} />
                {item.label}
              </Link>
            );
          })}
        </nav>

        <div className="border-t border-white/10 px-5 py-4">
          <Link to={ROUTES.home} className="text-xs font-medium text-slate-400 hover:text-white">
            ← Về trang người dùng
          </Link>
        </div>
      </aside>
    </>
  );
}