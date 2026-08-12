import { useState } from 'react';
import useAuth from '@/features/auth/hooks/useAuth';
import { MenuIcon, ChevronDownIcon, LogoutIcon } from './AdminIcons';

/**
 * Header tren cung cua khu vuc Admin: nut mo sidebar (mobile) + menu tai khoan.
 * Tieu de rieng cua tung trang (vi du "Quan ly nguoi dung") van dat trong noi dung
 * trang thong qua PageHeader nhu cac trang admin hien co (AdminHotelPendingListPage),
 * de tranh trung lap va giu AdminHeader don gian, tai su dung duoc cho moi trang.
 *
 * Props: onOpenSidebar
 */
export function AdminHeader({ onOpenSidebar }) {
  const { user, logout } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);

  const displayName = user?.fullName || user?.email || 'Quản trị viên';
  const initial = displayName.trim().charAt(0).toUpperCase() || 'A';

  return (
    <header className="sticky top-0 z-20 flex h-16 items-center justify-between border-b border-slate-200 bg-white px-4 md:px-6 lg:px-8">
      <button
        type="button"
        onClick={onOpenSidebar}
        className="rounded-md p-2 text-slate-500 hover:bg-slate-100 hover:text-slate-700 lg:hidden"
        aria-label="Mở menu"
      >
        <MenuIcon className="h-5 w-5" />
      </button>

      <span className="hidden text-sm font-semibold text-slate-500 lg:block">Bảng quản trị hệ thống</span>

      <div className="relative">
        <button
          type="button"
          onClick={() => setMenuOpen((v) => !v)}
          className="flex items-center gap-2 rounded-full py-1 pl-1 pr-2 hover:bg-slate-100"
        >
          <span className="grid h-8 w-8 place-items-center rounded-full bg-sky-700 text-sm font-semibold text-white">
            {initial}
          </span>
          <span className="hidden text-left text-sm sm:block">
            <span className="block font-semibold text-slate-800 leading-tight">{displayName}</span>
            {user?.email && <span className="block text-xs text-slate-400 leading-tight">{user.email}</span>}
          </span>
          <ChevronDownIcon className="hidden h-4 w-4 text-slate-400 sm:block" />
        </button>

        {menuOpen && (
          <>
            {/* Lop phu trong suot de bam ra ngoai la dong menu */}
            <button
              type="button"
              className="fixed inset-0 z-10 cursor-default"
              aria-label="Đóng menu tài khoản"
              onClick={() => setMenuOpen(false)}
            />
            <div className="absolute right-0 z-20 mt-2 w-56 rounded-lg border border-slate-200 bg-white py-2 shadow-lg">
              <div className="border-b border-slate-100 px-4 py-2 sm:hidden">
                <p className="truncate text-sm font-semibold text-slate-800">{displayName}</p>
                {user?.email && <p className="truncate text-xs text-slate-400">{user.email}</p>}
              </div>
              <button
                type="button"
                onClick={() => {
                  setMenuOpen(false);
                  logout();
                }}
                className="flex w-full items-center gap-2 px-4 py-2 text-sm font-medium text-red-600 hover:bg-red-50"
              >
                <LogoutIcon className="h-4 w-4" />
                Đăng xuất
              </button>
            </div>
          </>
        )}
      </div>
    </header>
  );
}