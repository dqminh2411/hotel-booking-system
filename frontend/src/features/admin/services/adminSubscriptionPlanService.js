import { adminSubscriptionPlanApi } from '@/features/admin/api/adminSubscriptionPlanApi';

/** Lop service - orchestration nghiep vu phia FE cho danh sach Subscription Plan. */
export const adminSubscriptionPlanService = {
  listPlans(params) {
    return adminSubscriptionPlanApi.getSubscriptionPlans(params);
  },
};