import {
  ConditionOperator,
  ConditionType,
  CouponStatus,
  PromotionDiscountType,
  PromotionStatus,
  PromotionType,
  ScopeType,
} from '@/features/promotion/types/promotion.types';

/**
 * Label tieng Viet cho tung enum + danh sach option dung cho Select.
 * Tach rieng khoi promotion.types.js de file do chi chua "hop dong" voi backend.
 */

export const PROMOTION_TYPE_LABEL = {
  SYSTEM: 'He thong (SYSTEM)',
  HOTEL: 'Khach san (HOTEL)',
  ROOM_TYPE: 'Loai phong (ROOM_TYPE)',
  COUPON: 'Coupon (COUPON)',
};
export const PROMOTION_TYPE_OPTIONS = Object.values(PromotionType).map((v) => ({
  value: v,
  label: PROMOTION_TYPE_LABEL[v],
}));

export const DISCOUNT_TYPE_LABEL = {
  PERCENTAGE: 'Theo phan tram (%)',
  FIXED_AMOUNT: 'So tien co dinh',
};
export const DISCOUNT_TYPE_OPTIONS = Object.values(PromotionDiscountType).map((v) => ({
  value: v,
  label: DISCOUNT_TYPE_LABEL[v],
}));

export const PROMOTION_STATUS_LABEL = {
  DRAFT: 'Nhap',
  ACTIVE: 'Dang hoat dong',
  PAUSED: 'Tam dung',
  EXPIRED: 'Het han',
};
export const PROMOTION_STATUS_OPTIONS = Object.values(PromotionStatus).map((v) => ({
  value: v,
  label: PROMOTION_STATUS_LABEL[v],
}));
export const PROMOTION_STATUS_TONE = {
  DRAFT: 'neutral',
  ACTIVE: 'success',
  PAUSED: 'warning',
  EXPIRED: 'danger',
};

// Sprint hien tai backend chi chap nhan SYSTEM (xem PromotionServiceImpl#validateScopes).
export const SCOPE_TYPE_LABEL = {
  SYSTEM: 'He thong (SYSTEM)',
  HOTEL: 'Khach san (HOTEL) - chua ho tro',
  ROOM_TYPE: 'Loai phong (ROOM_TYPE) - chua ho tro',
  USER_SEGMENT: 'Nhom nguoi dung (USER_SEGMENT) - chua ho tro',
};
export const SCOPE_TYPE_OPTIONS_CURRENT_SPRINT = [
  { value: ScopeType.SYSTEM, label: SCOPE_TYPE_LABEL.SYSTEM },
];

export const CONDITION_TYPE_LABEL = {
  FIRST_BOOKING: 'Dat phong lan dau',
  EARLY_BIRD_DAYS: 'So ngay dat truoc (Early bird)',
  LAST_MINUTE_DAYS: 'So ngay dat can (Last minute)',
  MIN_GUESTS: 'So khach toi thieu',
};
export const CONDITION_TYPE_OPTIONS = Object.values(ConditionType).map((v) => ({
  value: v,
  label: CONDITION_TYPE_LABEL[v],
}));

export const CONDITION_OPERATOR_LABEL = {
  EQ: 'Bang (=)',
  GTE: 'Lon hon hoac bang (>=)',
  LTE: 'Nho hon hoac bang (<=)',
};
export const CONDITION_OPERATOR_OPTIONS = Object.values(ConditionOperator).map((v) => ({
  value: v,
  label: CONDITION_OPERATOR_LABEL[v],
}));

export const COUPON_STATUS_LABEL = {
  ACTIVE: 'Dang hoat dong',
  PAUSED: 'Tam dung',
  EXPIRED: 'Het han',
};
export const COUPON_STATUS_OPTIONS = Object.values(CouponStatus).map((v) => ({
  value: v,
  label: COUPON_STATUS_LABEL[v],
}));
export const COUPON_STATUS_TONE = {
  ACTIVE: 'success',
  PAUSED: 'warning',
  EXPIRED: 'danger',
};
