/**
 * Hang so hien thi cho domain Tenant & Subscription (Admin quan ly).
 * Khop voi cac entity trong user-service: TenantStatus, TenantSubscriptionPlanStatus,
 * BillingCycle.
 */

// entity/TenantStatus.java: ACTIVE | SUSPENDED
export const TENANT_STATUS_OPTIONS = [
  { value: 'ACTIVE', label: 'Đang hoạt động' },
  { value: 'SUSPENDED', label: 'Tạm ngưng' },
];

const TENANT_STATUS_LABELS = {
  ACTIVE: 'Đang hoạt động',
  SUSPENDED: 'Tạm ngưng',
};

export const TENANT_STATUS_TONE = {
  ACTIVE: 'success',
  SUSPENDED: 'danger',
};

export function getTenantStatusLabel(status) {
  return TENANT_STATUS_LABELS[status] ?? status ?? '-';
}

// entity/TenantSubscriptionPlanStatus.java: ACTIVE | EXPIRED | CANCELLED | SUSPENDED
export const TENANT_SUBSCRIPTION_STATUS_OPTIONS = [
  { value: 'ACTIVE', label: 'Đang hiệu lực' },
  { value: 'EXPIRED', label: 'Đã hết hạn' },
  { value: 'CANCELLED', label: 'Đã hủy' },
  { value: 'SUSPENDED', label: 'Tạm ngưng' },
];

const TENANT_SUBSCRIPTION_STATUS_LABELS = {
  ACTIVE: 'Đang hiệu lực',
  EXPIRED: 'Đã hết hạn',
  CANCELLED: 'Đã hủy',
  SUSPENDED: 'Tạm ngưng',
};

export const TENANT_SUBSCRIPTION_STATUS_TONE = {
  ACTIVE: 'success',
  EXPIRED: 'neutral',
  CANCELLED: 'danger',
  SUSPENDED: 'warning',
};

export function getTenantSubscriptionStatusLabel(status) {
  return TENANT_SUBSCRIPTION_STATUS_LABELS[status] ?? status ?? '-';
}

// entity/BillingCycle.java: MONTHLY | YEARLY
const BILLING_CYCLE_LABELS = {
  MONTHLY: 'Hàng tháng',
  YEARLY: 'Hàng năm',
};

export function getBillingCycleLabel(cycle) {
  return BILLING_CYCLE_LABELS[cycle] ?? cycle ?? '-';
}

export function formatCurrencyVnd(amount) {
  if (amount === null || amount === undefined) return '-';
  const value = Number(amount);
  if (Number.isNaN(value)) return '-';
  return value.toLocaleString('vi-VN', { style: 'currency', currency: 'VND' });
}