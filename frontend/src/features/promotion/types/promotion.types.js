/**
 * Toan bo hang so (enum) trong file nay duoc doc truc tiep tu source backend
 * (entity/dto/constant cua promotion-service) + doi chieu voi promotion-service.yaml.
 * KHONG duoc tu y doi ten field/enum - phai khop 100% voi backend.
 * Cac JSDoc typedef ben duoi chi la tai lieu tham khao (khong duoc kiem tra
 * o build time vi day la project JavaScript thuan).
 */

// ---------- Enums (constant/*) ----------

export const PromotionType = {
  SYSTEM: 'SYSTEM',
  HOTEL: 'HOTEL',
  ROOM_TYPE: 'ROOM_TYPE',
  COUPON: 'COUPON',
};

export const PromotionDiscountType = {
  PERCENTAGE: 'PERCENTAGE',
  FIXED_AMOUNT: 'FIXED_AMOUNT',
};

export const PromotionStatus = {
  DRAFT: 'DRAFT',
  ACTIVE: 'ACTIVE',
  PAUSED: 'PAUSED',
  EXPIRED: 'EXPIRED',
};

// Sprint hien tai: backend chi cho phep SYSTEM (validateScopes trong PromotionServiceImpl).
// Cac gia tri khac duoc giu lai de san sang cho Hotel Owner o sprint sau,
// nhung UI se chi cho chon SYSTEM.
export const ScopeType = {
  SYSTEM: 'SYSTEM',
  HOTEL: 'HOTEL',
  ROOM_TYPE: 'ROOM_TYPE',
  USER_SEGMENT: 'USER_SEGMENT',
};

export const ConditionType = {
  FIRST_BOOKING: 'FIRST_BOOKING',
  EARLY_BIRD_DAYS: 'EARLY_BIRD_DAYS',
  LAST_MINUTE_DAYS: 'LAST_MINUTE_DAYS',
  MIN_GUESTS: 'MIN_GUESTS',
};

export const ConditionOperator = {
  EQ: 'EQ',
  GTE: 'GTE',
  LTE: 'LTE',
};

export const CouponStatus = {
  ACTIVE: 'ACTIVE',
  PAUSED: 'PAUSED',
  EXPIRED: 'EXPIRED',
};

// ---------- Request payloads (dto/request/*) - chi de tai lieu, khong export runtime ----------

/**
 * @typedef {Object} PromotionScopeInput
 * @property {string} scopeType - 1 trong cac gia tri cua ScopeType
 * @property {string|null} [scopeRefId] - UUID, phai null khi scopeType = SYSTEM
 */

/**
 * @typedef {Object} PromotionConditionInput
 * @property {string} conditionType - 1 trong cac gia tri cua ConditionType
 * @property {string} operator - 1 trong cac gia tri cua ConditionOperator
 * @property {string} conditionValue
 */

/**
 * @typedef {Object} CouponInput
 * @property {string} code - pattern ^[A-Z0-9_-]+$, business key
 * @property {string} status - 1 trong cac gia tri cua CouponStatus
 * @property {number|null} [usageLimit]
 */

/**
 * Chung shape cho Create va Update (backend co 2 class rieng nhung cung field).
 * @typedef {Object} PromotionRequestPayload
 * @property {string|null} [tenantId] - Ignored trong sprint hien tai, van gui de dung contract
 * @property {string} name
 * @property {string|null} [description]
 * @property {string} type - 1 trong cac gia tri cua PromotionType
 * @property {string} discountType - 1 trong cac gia tri cua PromotionDiscountType
 * @property {number} discountValue
 * @property {number|null} [maxDiscountAmount]
 * @property {number|null} [minBookingAmount]
 * @property {number|null} [minNights]
 * @property {string} startAt - ISO OffsetDateTime
 * @property {string} endAt - ISO OffsetDateTime
 * @property {string} status - 1 trong cac gia tri cua PromotionStatus
 * @property {number|null} [totalUsageLimit]
 * @property {number|null} [perUserUsageLimit]
 * @property {boolean} stackable
 * @property {PromotionScopeInput[]} scopes
 * @property {CouponInput[]} coupons
 * @property {PromotionConditionInput[]} conditions
 */

/**
 * Theo OpenAPI: ChangePromotionStatusRequest { status, reason }.
 * Luu y: controller thuc te (PromotionController#changeStatus) dang nhan
 * `status` qua @RequestParam, KHONG doc @RequestBody, va `reason` hien
 * khong duoc backend luu o dau ca. FE van gui du 2 field theo dung OpenAPI
 * (xem promotionApi.changeStatus) de san sang khi backend cap nhat theo spec.
 * @typedef {Object} ChangePromotionStatusRequest
 * @property {string} status
 * @property {string} [reason]
 */

// ---------- Response payloads (dto/response/*) - chi de tai lieu ----------

/**
 * @typedef {Object} PromotionScopeResponse
 * @property {string} [id]
 * @property {string} scopeType
 * @property {string|null} [scopeRefId]
 */

/**
 * @typedef {Object} PromotionConditionResponse
 * @property {string} [id]
 * @property {string} conditionType
 * @property {string} operator
 * @property {string|null} [conditionValue]
 */

/**
 * @typedef {Object} CouponResponse
 * @property {string} [id]
 * @property {string} code
 * @property {string} status
 * @property {number|null} [usageLimit]
 * @property {number|null} [currentUsageCount]
 */

/**
 * @typedef {Object} PromotionResponse
 * @property {string} id
 * @property {string|null} [tenantId]
 * @property {string} name
 * @property {string|null} [description]
 * @property {string} type
 * @property {string} discountType
 * @property {number} discountValue
 * @property {number|null} [maxDiscountAmount]
 * @property {number|null} [minBookingAmount]
 * @property {number|null} [minNights]
 * @property {string} startAt
 * @property {string} endAt
 * @property {string} status
 * @property {number|null} [totalUsageLimit]
 * @property {number|null} [perUserUsageLimit]
 * @property {number|null} [currentUsageCount]
 * @property {boolean|null} [stackable]
 * @property {boolean|null} [isDeleted]
 * @property {PromotionScopeResponse[]} [scopes]
 * @property {CouponResponse[]} [coupons]
 * @property {PromotionConditionResponse[]} [conditions]
 * @property {string|null} [createdBy]
 * @property {string|null} [updatedBy]
 * @property {string|null} [createdAt]
 * @property {string|null} [updatedAt]
 * @property {string|null} [deletedAt]
 */

// Danh sach (GET /api/promotions) dung chung shape PromotionResponse nhung
// backend (PromotionServiceImpl#toPromotionResponse) chi map: id, name,
// description, type, discountType, discountValue, maxDiscountAmount,
// minBookingAmount, minNights, startAt, endAt, status - cac field con lai
// (scopes, coupons, conditions, currentUsageCount, stackable, timestamps...)
// se la null/undefined khi lay tu API danh sach. UI danh sach vi vay CHI
// hien thi cac field duoc dam bao co.

/**
 * @typedef {Object} PromotionListParams
 * @property {string} [keyword]
 * @property {string} [status]
 * @property {number} [page] - 0-based
 * @property {number} [size]
 */
