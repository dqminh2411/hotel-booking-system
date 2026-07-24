/**
 * Validate CO BAN o phia FE: required, dinh dang so, dinh dang ngay.
 * Business rule (vi du: startAt < endAt, percentage <= 100, coupon code trung...)
 * duoc de Backend xu ly - FE chi hien thi loi 400 tra ve tu server.
 */
export function validatePromotionForm(values) {
  const errors = {};

  if (!values.name.trim()) {
    errors.name = 'Vui long nhap ten promotion';
  } else if (values.name.trim().length > 150) {
    errors.name = 'Ten khong duoc vuot qua 150 ky tu';
  }

  if (values.description && values.description.length > 2000) {
    errors.description = 'Mo ta khong duoc vuot qua 2000 ky tu';
  }

  if (!values.discountValue.trim()) {
    errors.discountValue = 'Vui long nhap gia tri giam gia';
  } else if (!isValidNumber(values.discountValue)) {
    errors.discountValue = 'Gia tri giam gia phai la so';
  }

  if (values.maxDiscountAmount && !isValidNumber(values.maxDiscountAmount)) {
    errors.maxDiscountAmount = 'Phai la so';
  }

  if (values.minBookingAmount && !isValidNumber(values.minBookingAmount)) {
    errors.minBookingAmount = 'Phai la so';
  }

  if (values.minNights && !isValidInteger(values.minNights)) {
    errors.minNights = 'Phai la so nguyen';
  }

  if (!values.startAt) {
    errors.startAt = 'Vui long chon ngay bat dau';
  }

  if (!values.endAt) {
    errors.endAt = 'Vui long chon ngay ket thuc';
  }

  if (values.totalUsageLimit && !isValidInteger(values.totalUsageLimit)) {
    errors.totalUsageLimit = 'Phai la so nguyen';
  }

  if (values.perUserUsageLimit && !isValidInteger(values.perUserUsageLimit)) {
    errors.perUserUsageLimit = 'Phai la so nguyen';
  }

  const conditionErrors = {};
  values.conditions.forEach((row) => {
    const rowErrors = {};
    if (!row.conditionValue.trim()) {
      rowErrors.conditionValue = 'Bat buoc nhap gia tri';
    }
    if (Object.keys(rowErrors).length > 0) conditionErrors[row.rowId] = rowErrors;
  });
  if (Object.keys(conditionErrors).length > 0) errors.conditions = conditionErrors;

  const couponErrors = {};
  values.coupons.forEach((row) => {
    const rowErrors = {};
    if (!row.code.trim()) {
      rowErrors.code = 'Bat buoc nhap coupon code';
    }
    if (row.usageLimit !== undefined && row.usageLimit !== null && !isValidInteger(String(row.usageLimit))) {
      rowErrors.usageLimit = 'Phai la so nguyen';
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
