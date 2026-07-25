import { toDateInputValue } from '@/shared/utils/date';

/**
 * Model rieng cho form (khac PromotionRequestPayload o cho: cac truong so/ngay
 * duoc giu duoi dang string de bind vao <input> de dang, se convert khi submit).
 * Xem @/features/promotion/types/promotion.types.js de biet shape day du
 * cua PromotionRequestPayload/PromotionResponse ma cac ham duoi day tao ra/doc vao.
 */

export function createEmptyFormValues() {
  return {
    name: '',
    description: '',
    type: 'SYSTEM',
    discountType: 'PERCENTAGE',
    discountValue: '',
    maxDiscountAmount: '',
    minBookingAmount: '',
    minNights: '',
    startAt: '',
    endAt: '',
    status: 'DRAFT',
    totalUsageLimit: '',
    perUserUsageLimit: '',
    stackable: false,
    conditions: [],
    coupons: [],
  };
}

export function createEmptyConditionRow() {
  return {
    rowId: crypto.randomUUID(),
    conditionType: 'FIRST_BOOKING',
    operator: 'EQ',
    conditionValue: '',
  };
}

export function createEmptyCouponRow() {
  return {
    rowId: crypto.randomUUID(),
    code: '',
    status: 'ACTIVE',
    usageLimit: undefined,
  };
}

/** Chuyen form values -> dung payload backend can (CreatePromotionRequest/UpdatePromotionRequest). */
export function toRequestPayload(values) {
  return {
    name: values.name.trim(),
    description: values.description.trim() || undefined,
    type: values.type,
    discountType: values.discountType,
    discountValue: Number(values.discountValue),
    maxDiscountAmount: values.maxDiscountAmount ? Number(values.maxDiscountAmount) : undefined,
    minBookingAmount: values.minBookingAmount ? Number(values.minBookingAmount) : undefined,
    minNights: values.minNights ? Number(values.minNights) : undefined,
    startAt: toIsoStart(values.startAt),
    endAt: toIsoStart(values.endAt),
    status: values.status,
    totalUsageLimit: values.totalUsageLimit ? Number(values.totalUsageLimit) : undefined,
    perUserUsageLimit: values.perUserUsageLimit ? Number(values.perUserUsageLimit) : undefined,
    stackable: values.stackable,
    // Sprint hien tai backend chi chap nhan scope SYSTEM va scopeRefId = null
    // (xem PromotionServiceImpl#validateScopes) - FE luon gui dung 1 scope co dinh.
    scopes: [{ scopeType: 'SYSTEM', scopeRefId: null }],
    conditions: values.conditions.map((item) => ({
      conditionType: item.conditionType,
      operator: item.operator,
      conditionValue: item.conditionValue,
    })),
    coupons: values.coupons.map((item) => ({
      code: item.code,
      status: item.status,
      usageLimit: item.usageLimit || undefined,
    })),
  };
}

function toIsoStart(dateValue) {
  return new Date(`${dateValue}T00:00:00`).toISOString();
}

/**
 * Chuyen PromotionResponse (tu GET /api/promotions/{id}) -> form values
 * de nap san du lieu cho man hinh Edit.
 * Luu y: id cua coupon KHONG duoc gui lai len backend, vi PUT dong bo coupon
 * theo "code" (business key), khop voi CouponInput ma backend can.
 */
export function fromResponseToFormValues(promotion) {
  return {
    name: promotion.name,
    description: promotion.description ?? '',
    type: promotion.type,
    discountType: promotion.discountType,
    discountValue: String(promotion.discountValue),
    maxDiscountAmount: promotion.maxDiscountAmount != null ? String(promotion.maxDiscountAmount) : '',
    minBookingAmount: promotion.minBookingAmount != null ? String(promotion.minBookingAmount) : '',
    minNights: promotion.minNights != null ? String(promotion.minNights) : '',
    startAt: toDateInputValue(promotion.startAt),
    endAt: toDateInputValue(promotion.endAt),
    status: promotion.status,
    totalUsageLimit: promotion.totalUsageLimit != null ? String(promotion.totalUsageLimit) : '',
    perUserUsageLimit: promotion.perUserUsageLimit != null ? String(promotion.perUserUsageLimit) : '',
    stackable: !!promotion.stackable,
    conditions: (promotion.conditions ?? []).map((c) => ({
      rowId: crypto.randomUUID(),
      conditionType: c.conditionType,
      operator: c.operator,
      conditionValue: c.conditionValue ?? '',
    })),
    coupons: (promotion.coupons ?? []).map((c) => ({
      rowId: crypto.randomUUID(),
      code: c.code,
      status: c.status,
      usageLimit: c.usageLimit ?? undefined,
    })),
  };
}
