import { adminTenantApi } from '@/features/admin/api/adminTenantApi';

/** Lop service - orchestration nghiep vu phia FE cho Tenant cua Admin. */
export const adminTenantService = {
  listTenants(params) {
    return adminTenantApi.getTenants(params);
  },

  getTenantDetail(tenantId) {
    return adminTenantApi.getTenantDetail(tenantId);
  },

  createTenant(payload) {
    return adminTenantApi.createTenant({
      ownerId: payload.ownerId?.trim(),
      name: payload.name?.trim(),
    });
  },

  updateTenant(tenantId, payload) {
    return adminTenantApi.updateTenant(tenantId, {
      name: payload.name?.trim(),
      status: payload.status || undefined,
    });
  },

  deleteTenant(tenantId) {
    return adminTenantApi.deleteTenant(tenantId);
  },
};