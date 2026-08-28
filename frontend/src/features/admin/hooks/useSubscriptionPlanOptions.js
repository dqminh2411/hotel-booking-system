import { useCallback, useEffect, useState } from 'react';
import { adminSubscriptionPlanService } from '@/features/admin/services/adminSubscriptionPlanService';
import { getApiErrorMessage } from '@/shared/api/getApiErrorMessage';
import { getBillingCycleLabel, formatCurrencyVnd } from '@/features/admin/constants/tenantOptions';

/**
 * Nap danh sach Subscription Plan (GET /api/admin/subscription-plans) mot lan de lam
 * <Select> khi Admin dang ky goi moi cho tenant (CreateTenantSubscriptionModal).
 * Chi lay 1 trang (size toi da = 30, xem adminSubscriptionPlanApi.js) - neu catalog
 * vuot qua 30 goi thi can nang cap thanh combobox co tim kiem/phan trang rieng.
 */
export function useSubscriptionPlanOptions() {
  const [plans, setPlans] = useState([]);
  const [status, setStatus] = useState('loading'); // loading | success | error
  const [errorMessage, setErrorMessage] = useState('');

  const load = useCallback(() => {
    setStatus('loading');
    setErrorMessage('');

    adminSubscriptionPlanService
      .listPlans({ size: 30 })
      .then((res) => {
        setPlans(res?.content ?? []);
        setStatus('success');
      })
      .catch((err) => {
        setPlans([]);
        setErrorMessage(getApiErrorMessage(err, 'Không thể tải danh sách gói dịch vụ. Vui lòng thử lại.'));
        setStatus('error');
      });
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const options = plans.map((plan) => ({
    value: plan.id,
    label: `${plan.code} - ${plan.name} (${formatCurrencyVnd(plan.price)} / ${getBillingCycleLabel(plan.billingCycle)})`,
  }));

  return { plans, options, status, errorMessage, reload: load };
}