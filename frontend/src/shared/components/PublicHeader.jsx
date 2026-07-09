import { Link } from 'react-router-dom';
import useAuth from '../../features/auth/hooks/useAuth';
import BrandLogo from './BrandLogo';

export default function PublicHeader() {
  const { user, isAuthenticated, logout } = useAuth();

  return (
    <header className="bg-blue-700 text-white">
      <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-4 md:px-6 lg:px-8">
        <BrandLogo inverse />
        <nav className="hidden items-center gap-6 text-sm font-medium md:flex" aria-label="Điều hướng chính">
          <a href="#search" className="hover:text-blue-100">Lưu trú</a>
          <a href="#benefits" className="hover:text-blue-100">Ưu đãi</a>
          <a href="#support" className="hover:text-blue-100">Hỗ trợ</a>
        </nav>
        {isAuthenticated ? (
          <div className="flex items-center gap-3">
            <span className="hidden text-sm sm:inline">{user?.fullName || user?.email}</span>
            <button
              type="button"
              onClick={logout}
              className="rounded-md border border-blue-300 px-3 py-2 text-sm font-semibold hover:bg-blue-800"
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
              className="rounded-md border border-blue-300 px-3 py-2 text-sm font-semibold hover:bg-blue-800"
            >
              Đăng nhập
            </Link>
          </div>
        )}
      </div>
    </header>
  );
}
