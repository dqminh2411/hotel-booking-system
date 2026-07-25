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
  SYSTEM: 'Hệ thống (SYSTEM)',
  HOTEL: 'Khách sạn (HOTEL)',
  ROOM_TYPE: 'Loại phòng (ROOM_TYPE)',
  COUPON: 'Mã giảm giá (COUPON)',
};
export const PROMOTION_TYPE_OPTIONS = Object.values(PromotionType).map((v) => ({
  value: v,
  label: PROMOTION_TYPE_LABEL[v],
}));

export const DISCOUNT_TYPE_LABEL = {
  PERCENTAGE: 'Theo phần trăm (%)',
  FIXED_AMOUNT: 'Số tiền cố định',
};
export const DISCOUNT_TYPE_OPTIONS = Object.values(PromotionDiscountType).map((v) => ({
  value: v,
  label: DISCOUNT_TYPE_LABEL[v],
}));

export const PROMOTION_STATUS_LABEL = {
  DRAFT: 'Nháp',
  ACTIVE: 'Đang hoạt động',
  PAUSED: 'Tạm dừng',
  EXPIRED: 'Hết hạn',
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
  SYSTEM: 'Hệ thống (SYSTEM)',
  HOTEL: 'Khách sạn (HOTEL) - chưa hỗ trợ',
  ROOM_TYPE: 'Loại phòng (ROOM_TYPE) - chưa hỗ trợ',
  USER_SEGMENT: 'Nhóm người dùng (USER_SEGMENT) - chưa hỗ trợ',
};
export const SCOPE_TYPE_OPTIONS_CURRENT_SPRINT = [
  { value: ScopeType.SYSTEM, label: SCOPE_TYPE_LABEL.SYSTEM },
];

export const CONDITION_TYPE_LABEL = {
  FIRST_BOOKING: 'Đặt phòng lần đầu',
  EARLY_BIRD_DAYS: 'Số ngày đặt trước (Early bird)',
  LAST_MINUTE_DAYS: 'Số ngày đặt cận (Last minute)',
  MIN_GUESTS: 'Số khách tối thiểu',
};
export const CONDITION_TYPE_OPTIONS = Object.values(ConditionType).map((v) => ({
  value: v,
  label: CONDITION_TYPE_LABEL[v],
}));

export const CONDITION_OPERATOR_LABEL = {
  EQ: 'Bằng (=)',
  GTE: 'Lớn hơn hoặc bằng (>=)',
  LTE: 'Nhỏ hơn hoặc bằng (<=)',
};
export const CONDITION_OPERATOR_OPTIONS = Object.values(ConditionOperator).map((v) => ({
  value: v,
  label: CONDITION_OPERATOR_LABEL[v],
}));

export const COUPON_STATUS_LABEL = {
  ACTIVE: 'Đang hoạt động',
  PAUSED: 'Tạm dừng',
  EXPIRED: 'Hết hạn',
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
