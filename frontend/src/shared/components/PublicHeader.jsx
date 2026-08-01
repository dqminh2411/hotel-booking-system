import { Link } from 'react-router-dom';
import useAuth from '../../features/auth/hooks/useAuth';
import BrandLogo from './BrandLogo';

export default function PublicHeader() {
  const { user, isAuthenticated, logout } = useAuth();
  // realm_access.roles cua Keycloak duoc AuthContext map ve user.roles (xem
  // mapKeycloakUser trong features/auth/context/AuthContext.jsx). Nut "Quan tri"
  // chi la loi tat UI - viec chan quyen thuc su van do backend (@PreAuthorize
  // hasRole('PLATFORM_ADMIN') tren AdminController) tra ve 403 dam nhiem.
  const isPlatformAdmin = Boolean(user?.roles?.includes('PLATFORM_ADMIN'));

  return (
    <header className="bg-gradient-to-r from-slate-800 via-blue-800 to-sky-800 text-white">
      <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-4 md:px-6 lg:px-8">
        <BrandLogo inverse />
        <nav className="hidden items-center gap-6 text-sm font-medium md:flex" aria-label="Điều hướng chính">
          <a href="#search" className="hover:text-blue-100">Lưu trú</a>
          <Link to="/promotions" className="hover:text-blue-100">Khuyến mãi</Link>
          <a href="#support" className="hover:text-blue-100">Hỗ trợ</a>
          {isPlatformAdmin && (
            <Link to="/admin/hotels/pending" className="hover:text-blue-100"> Quản trị </Link>
          )}
        </nav>
        {isAuthenticated ? (
          <div className="flex items-center gap-3">
            {isPlatformAdmin && (
              <Link
                to="/admin/hotels/pending"
                className="rounded-md border border-white/40 px-3 py-2 text-sm font-semibold hover:bg-white/10 md:hidden"
              >
                Quản trị
              </Link>
            )}
            <span className="hidden text-sm sm:inline">{user?.fullName || user?.email}</span>
            <button
              type="button"
              onClick={logout}
              className="rounded-md border border-white/30 px-3 py-2 text-sm font-semibold hover:bg-white/10"
            >
              Đăng xuất
            </button>
          </div>
        ) : (
          <div className="flex items-center gap-2">
            <Link
              to="/register"
              className="rounded-md bg-white px-3 py-2 text-sm font-semibold text-blue-700 hover:bg-blue-50"
            >
              Đăng ký
            </Link>
            <Link
              to="/login"
              className="rounded-md border border-white/30 px-3 py-2 text-sm font-semibold hover:bg-white/10"
            >
              Đăng nhập
            </Link>
          </div>
        )}
      </div>
    </header>
  );
}