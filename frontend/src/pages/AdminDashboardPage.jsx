import { Link } from 'react-router-dom';
import { AdminLayout } from '@/shared/components/AdminLayout/AdminLayout';
import { PageHeader } from '@/shared/components/PageHeader/PageHeader';
import { ADMIN_NAV_ITEMS } from '@/shared/constants/adminNav';
import { ADMIN_ICON_BY_KEY } from '@/shared/components/AdminLayout/AdminIcons';

const CARD_DESCRIPTIONS = {
  users: 'Tạo, sửa, khóa/mở khóa và xóa tài khoản người dùng.',
  tenants: 'Quản lý tenant và lịch sử gói dịch vụ đang sử dụng.',
  hotels: 'Duyệt, từ chối và quản lý khách sạn chờ duyệt.',
  bookings: 'Theo dõi và xử lý các đơn đặt phòng trong hệ thống.',
  payments: 'Theo dõi giao dịch thanh toán và đối soát.',
};

/**
 * Trang tổng quan quản trị (hub) - điểm đến khi bấm "Quản trị" ở PublicHeader.
 * Liệt kê toàn bộ khu vực quản lý dưới dạng thẻ, lấy trực tiếp từ ADMIN_NAV_ITEMS
 * (shared/constants/adminNav.js) để luôn đồng bộ với menu sidebar - thêm/bớt 1 khu
 * vực trong adminNav.js là tự động phản ánh ở đây, không cần sửa 2 nơi.
 * Khu vực chưa xây dựng (disabled) hiện mờ, không bấm được, kèm nhãn "Sắp có".
 */
export default function AdminDashboardPage() {
  const items = ADMIN_NAV_ITEMS.filter((item) => item.key !== 'dashboard');

  return (
    <AdminLayout>
      <PageHeader title="Tổng quan quản trị" subtitle="Chọn khu vực bạn muốn quản lý." />

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {items.map((item) => {
          const Icon = ADMIN_ICON_BY_KEY[item.key];

          if (item.disabled) {
            return (
              <div
                key={item.key}
                className="flex cursor-not-allowed items-start gap-4 rounded-lg border border-slate-200 bg-slate-50 p-5 opacity-60"
              >
                <span className="grid h-11 w-11 shrink-0 place-items-center rounded-lg bg-slate-200 text-slate-500">
                  <Icon className="h-5 w-5" />
                </span>
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="font-bold text-slate-700">{item.label}</h3>
                    <span className="rounded-full bg-slate-200 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-slate-500">
                      Sắp có
                    </span>
                  </div>
                  <p className="mt-1 text-sm text-slate-400">Tính năng đang được xây dựng.</p>
                </div>
              </div>
            );
          }

          return (
            <Link
              key={item.key}
              to={item.path}
              className="flex items-start gap-4 rounded-lg border border-slate-200 bg-white p-5 transition-colors hover:border-sky-300 hover:bg-sky-50"
            >
              <span className="grid h-11 w-11 shrink-0 place-items-center rounded-lg bg-sky-100 text-sky-700">
                <Icon className="h-5 w-5" />
              </span>
              <div>
                <h3 className="font-bold text-sky-900">{item.label}</h3>
                <p className="mt-1 text-sm text-slate-500">{CARD_DESCRIPTIONS[item.key]}</p>
              </div>
            </Link>
          );
        })}
      </div>
    </AdminLayout>
  );
}