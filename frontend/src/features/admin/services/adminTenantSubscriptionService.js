import { adminTenantSubscriptionApi } from '@/features/admin/api/adminTenantSubscriptionApi';

/** Lop service - orchestration nghiep vu phia FE cho lich su goi cua Tenant. */
export const adminTenantSubscriptionService = {
  listTenantSubscriptions(tenantId, params) {
    return adminTenantSubscriptionApi.getTenantSubscriptions(tenantId, params);
  },

  createTenantSubscription(tenantId, subscriptionId) {
    return adminTenantSubscriptionApi.createTenantSubscription({ tenantId, subscriptionId });
  },

  updateStatus(id, status) {
    return adminTenantSubscriptionApi.updateTenantSubscriptionStatus(id, { status });
  },

  deleteTenantSubscription(id) {
    return adminTenantSubscriptionApi.deleteTenantSubscription(id);
  },
};