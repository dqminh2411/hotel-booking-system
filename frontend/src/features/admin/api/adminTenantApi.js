import axiosClient from '@/shared/api/axiosClient';

/**
 * Lop goi API "tho" cho Admin - Tenant management (user-service, AdminController).
 * CHI goi cac endpoint /api/admin/tenants (KHONG bao gom /subscriptions - xem
 * adminTenantSubscriptionApi.js cho phan lich su goi cua tenant).
 *  - GET    /api/admin/tenants
 *  - GET    /api/admin/tenants/{tenantId}
 *  - POST   /api/admin/tenants
 *  - PATCH  /api/admin/tenants/{tenantId}
 *  - DELETE /api/admin/tenants/{tenantId}
 * Tat ca deu @PreAuthorize("hasRole('PLATFORM_ADMIN')").
 */
const BASE_PATH = '/api/admin/tenants';

export const adminTenantApi = {
  // GET /api/admin/tenants?page=&size=&status=&search=
  // Rang buoc: page >= 0, 10 <= size <= 30. Response ApiResponse<Page<TenantResponse>>.
  getTenants({ page = 0, size = 10, status, search } = {}) {
    return axiosClient
      .get(BASE_PATH, {
        params: { page, size, status: status || undefined, search: search || undefined },
      })
      .then((res) => res.data.data);
  },

  // GET /api/admin/tenants/{tenantId}
  // Response la ApiResponse<TenantDetailResponse> - co them updatedAt va
  // activeSubscription (goi dang dung, neu co). Dung cho trang quan ly Subscriptions.
  getTenantDetail(tenantId) {
    return axiosClient.get(`${BASE_PATH}/${tenantId}`).then((res) => res.data.data);
  },

  // POST /api/admin/tenants - body dung theo CreateTenantRequest { ownerId, name }.
  createTenant(payload) {
    return axiosClient.post(BASE_PATH, payload).then((res) => res.data);
  },

  // PATCH /api/admin/tenants/{tenantId} - body dung theo UpdateTenantRequest { name, status }.
  // LUU Y: UpdateTenantRequest.name co @Size(min = 20) - khac voi CreateTenantRequest
  // (chi @NotBlank, khong gioi han do dai toi thieu). Da rang buoc dung o TenantFormModal.
  updateTenant(tenantId, payload) {
    return axiosClient.patch(`${BASE_PATH}/${tenantId}`, payload).then((res) => res.data);
  },

  // DELETE /api/admin/tenants/{tenantId} - xoa mem (is_deleted).
  deleteTenant(tenantId) {
    return axiosClient.delete(`${BASE_PATH}/${tenantId}`).then((res) => res.data);
  },
};