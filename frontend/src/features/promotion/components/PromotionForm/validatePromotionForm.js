/**
 * Validate CO BAN o phia FE: required, dinh dang so, dinh dang ngay.
 * Business rule (vi du: startAt < endAt, percentage <= 100, coupon code trung...)
 * duoc de Backend xu ly - FE chi hien thi loi 400 tra ve tu server.
 */
export function validatePromotionForm(values) {
  const errors = {};

  if (!values.name.trim()) {
    errors.name = 'Vui lòng nhập tên promotion';
  } else if (values.name.trim().length > 150) {
    errors.name = 'Tên không được vượt quá 150 ký tự';
  }

  if (values.description && values.description.length > 2000) {
    errors.description = 'Mô tả không được vượt quá 2000 ký tự';
  }

  if (!values.discountValue.trim()) {
    errors.discountValue = 'Vui lòng nhập giá trị giảm giá';
  } else if (!isValidNumber(values.discountValue)) {
    errors.discountValue = 'Giá trị giảm giá phải là số';
  }

  if (values.maxDiscountAmount && !isValidNumber(values.maxDiscountAmount)) {
    errors.maxDiscountAmount = 'Phải là số';
  }

  if (values.minBookingAmount && !isValidNumber(values.minBookingAmount)) {
    errors.minBookingAmount = 'Phải là số';
  }

  if (values.minNights && !isValidInteger(values.minNights)) {
    errors.minNights = 'Phải là số nguyên';
  }

  if (!values.startAt) {
    errors.startAt = 'Vui lòng chọn ngày bắt đầu';
  }

  if (!values.endAt) {
    errors.endAt = 'Vui lòng chọn ngày kết thúc';
  }

  if (values.totalUsageLimit && !isValidInteger(values.totalUsageLimit)) {
    errors.totalUsageLimit = 'Phải là số nguyên';
  }

  if (values.perUserUsageLimit && !isValidInteger(values.perUserUsageLimit)) {
    errors.perUserUsageLimit = 'Phải là số nguyên';
  }

  const conditionErrors = {};
  values.conditions.forEach((row) => {
    const rowErrors = {};
    if (!row.conditionValue.trim()) {
      rowErrors.conditionValue = 'Bắt buộc nhập giá trị';
    }
    if (Object.keys(rowErrors).length > 0) conditionErrors[row.rowId] = rowErrors;
  });
  if (Object.keys(conditionErrors).length > 0) errors.conditions = conditionErrors;

  const couponErrors = {};
  values.coupons.forEach((row) => {
    const rowErrors = {};
    if (!row.code.trim()) {
      rowErrors.code = 'Bắt buộc nhập coupon code';
    }
    if (row.usageLimit !== undefined && row.usageLimit !== null && !isValidInteger(String(row.usageLimit))) {
      rowErrors.usageLimit = 'Phải là số nguyên';
    }
    if (Object.keys(rowErrors).length > 0) couponErrors[row.rowId] = rowErrors;
  });
  if (Object.keys(couponErrors).length > 0) errors.coupons = couponErrors;

  return errors;
}

export function hasFormErrors(errors) {
  return Object.keys(errors).length > 0;
}

function isValidNumber(value) {
  return value.trim() !== '' && !Number.isNaN(Number(value));
}

function isValidInteger(value) {
  return isValidNumber(value) && Number.isInteger(Number(value));
}
