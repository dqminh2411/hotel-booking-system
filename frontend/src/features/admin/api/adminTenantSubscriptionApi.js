import axiosClient from '@/shared/api/axiosClient';

/**
 * Lop goi API "tho" cho lich su su dung goi (subscription) cua tung tenant
 * (user-service, TenantSubscriptionController).
 * Endpoint cho phep ca PLATFORM_ADMIN va HOTEL_OWNER (chu khach san tu quan ly goi
 * cua chinh minh), nhung man hinh Admin nay chi goi khi dang nhap voi PLATFORM_ADMIN.
 *  - GET    /api/admin/tenants/{tenantId}/subscriptions
 *  - POST   /api/admin/tenants/subscriptions
 *  - GET    /api/admin/tenants/subscriptions/{id}
 *  - PATCH  /api/admin/tenants/subscriptions/{id}
 *  - DELETE /api/admin/tenants/subscriptions/{id}  (rieng DELETE chi PLATFORM_ADMIN)
 */
const BASE_PATH = '/api/admin/tenants';

export const adminTenantSubscriptionApi = {
  // GET /api/admin/tenants/{tenantId}/subscriptions?page=&size=
  // Server tu sap xep theo startedAt DESC, khong nhan tham so sort tu FE.
  getTenantSubscriptions(tenantId, { page = 0, size = 10 } = {}) {
    return axiosClient
      .get(`${BASE_PATH}/${tenantId}/subscriptions`, { params: { page, size } })
      .then((res) => res.data.data);
  },

  // POST /api/admin/tenants/subscriptions - body CreateTenantSubscriptionRequest { tenantId, subscriptionId }.
  createTenantSubscription(payload) {
    return axiosClient.post(`${BASE_PATH}/subscriptions`, payload).then((res) => res.data);
  },

  // PATCH /api/admin/tenants/subscriptions/{id} - body UpdateTenantSubscription { status }.
  updateTenantSubscriptionStatus(id, payload) {
    return axiosClient.patch(`${BASE_PATH}/subscriptions/${id}`, payload).then((res) => res.data);
  },

  // DELETE /api/admin/tenants/subscriptions/{id}
  deleteTenantSubscription(id) {
    return axiosClient.delete(`${BASE_PATH}/subscriptions/${id}`).then((res) => res.data);
  },
};