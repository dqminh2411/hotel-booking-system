import { Link } from 'react-router-dom';
import BrandLogo from '../shared/components/BrandLogo';

export default function NotFoundPage() {
  return (
    <main className="grid min-h-screen place-items-center bg-slate-50 px-4">
      <div className="text-center">
        <BrandLogo />
        <p className="mt-10 text-sm font-semibold text-blue-700">404</p>
        <h1 className="mt-2 text-3xl font-bold text-slate-900">Không tìm thấy trang</h1>
        <p className="mt-3 text-sm text-slate-600">Đường dẫn bạn truy cập không tồn tại.</p>
        <Link to="/" className="primary-button mt-6">
          Về trang chủ
        </Link>
      </div>
    </main>
  );
}
