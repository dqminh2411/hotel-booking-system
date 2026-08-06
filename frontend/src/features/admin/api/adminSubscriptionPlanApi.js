import axiosClient from '@/shared/api/axiosClient';

/**
 * Lop goi API "tho" cho danh sach Subscription Plan (user-service, SubscriptionController).
 * Man hinh Tenant & Gói dịch vụ CHI dung endpoint GET o day de lay danh sach goi lam
 * <Select> khi dang ky goi moi cho 1 tenant. Quan ly catalog goi (tao/sua/xoa plan)
 * khong thuoc pham vi man hinh nay nen khong dua vao day.
 */
const BASE_PATH = '/api/admin/subscription-plans';

export const adminSubscriptionPlanApi = {
  // GET /api/admin/subscription-plans?page=&size=&search=&billingCycle=
  // Rang buoc: 10 <= size <= 30 -> lay toi da (30) de <Select> co day du lua chon.
  getSubscriptionPlans({ page = 0, size = 30, search, billingCycle } = {}) {
    return axiosClient
      .get(BASE_PATH, {
        params: { page, size, search: search || undefined, billingCycle: billingCycle || undefined },
      })
      .then((res) => res.data.data);
  },
};