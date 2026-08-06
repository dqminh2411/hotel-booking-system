import { ROUTES } from '@/shared/constants/routes';

/**
 * Danh sach menu cho AdminSidebar.
 * - path: route dieu huong khi bam vao.
 * - matchPrefix: dung de highlight active menu cho ca cac route con (vi du
 *   /admin/tenants/{id}/subscriptions van phai sang menu "Tenant & Gói dịch vụ").
 *   Neu khong khai bao, mac dinh so sanh chinh xac path.
 * - disabled: true = man hinh chua duoc xay dung, chi hien de dinh hinh cau truc
 *   menu day du theo yeu cau (Dashboard / Bookings / Payments se lam o cac task sau).
 */
export const ADMIN_NAV_ITEMS = [
  {
    key: 'dashboard',
    label: 'Tổng quan',
    path: ROUTES.admin.dashboard,
  },
  {
    key: 'users',
    label: 'Người dùng',
    path: ROUTES.admin.users,
  },
  {
    key: 'tenants',
    label: 'Tenant & Gói dịch vụ',
    path: ROUTES.admin.tenants,
    matchPrefix: '/admin/tenants',
  },
  {
    key: 'hotels',
    label: 'Khách sạn',
    path: ROUTES.admin.hotelsPending,
    matchPrefix: '/admin/hotels',
  },
  {
    key: 'bookings',
    label: 'Đặt phòng',
    path: '/admin/bookings',
    disabled: true,
  },
  {
    key: 'payments',
    label: 'Thanh toán',
    path: '/admin/payments',
    disabled: true,
  },
];