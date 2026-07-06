const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PHONE_PATTERN = /^\+?[0-9]{9,15}$/;
const PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,72}$/;

export function validateLogin(values) {
  const errors = {};
  if (!values.email.trim()) errors.email = 'Vui lòng nhập email.';
  else if (!EMAIL_PATTERN.test(values.email)) errors.email = 'Email không đúng định dạng.';
  if (!values.password) errors.password = 'Vui lòng nhập mật khẩu.';
  return errors;
}

export function validateRegister(values) {
  const errors = validateLogin(values);

  if (!values.fullName.trim()) errors.fullName = 'Vui lòng nhập họ và tên.';
  else if (values.fullName.trim().length > 255) errors.fullName = 'Họ và tên tối đa 255 ký tự.';

  if (!values.phone.trim()) errors.phone = 'Vui lòng nhập số điện thoại.';
  else if (!PHONE_PATTERN.test(values.phone)) {
    errors.phone = 'Số điện thoại phải có từ 9 đến 15 chữ số.';
  }

  if (values.password && !PASSWORD_PATTERN.test(values.password)) {
    errors.password = 'Mật khẩu cần 8 ký tự, gồm chữ hoa, chữ thường và số.';
  }

  if (!values.confirmPassword) errors.confirmPassword = 'Vui lòng xác nhận mật khẩu.';
  else if (values.confirmPassword !== values.password) {
    errors.confirmPassword = 'Mật khẩu xác nhận không khớp.';
  }

  return errors;
}

export function isFormValid(errors) {
  return Object.keys(errors).length === 0;
}
