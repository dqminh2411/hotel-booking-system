import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import AuthLayout from '../features/auth/components/AuthLayout';
import FormField from '../features/auth/components/FormField';
import { registerUser } from '../features/auth/services/authService';
import { isFormValid, validateRegister } from '../features/auth/utils/authValidation';
import { getApiErrorMessage } from '../shared/api/getApiErrorMessage';

const initialValues = {
  fullName: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: '',
};

export default function RegisterPage() {
  const [values, setValues] = useState(initialValues);
  const [touched, setTouched] = useState({});
  const [submitError, setSubmitError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [registeredEmail, setRegisteredEmail] = useState('');
  const validationErrors = useMemo(() => validateRegister(values), [values]);

  function updateField(event) {
    const { name, value } = event.target;
    setValues((current) => ({ ...current, [name]: value }));
    setSubmitError('');
  }

  function touchField(name) {
    setTouched((current) => ({ ...current, [name]: true }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setTouched(Object.fromEntries(Object.keys(initialValues).map((key) => [key, true])));
    if (!isFormValid(validationErrors)) return;

    setIsSubmitting(true);
    setSubmitError('');
    try {
      await registerUser(values);
      setRegisteredEmail(values.email);
    } catch (error) {
      setSubmitError(getApiErrorMessage(error, 'Không thể tạo tài khoản. Vui lòng thử lại.'));
    } finally {
      setIsSubmitting(false);
    }
  }

  if (registeredEmail) {
    return (
      <AuthLayout
        eyebrow="Đăng ký thành công"
        title="Kiểm tra hộp thư của bạn"
        description="Tài khoản đã được tạo và đang chờ xác thực."
      >
        <div className="rounded-lg border border-green-200 bg-green-50 p-5 text-center">
          <span className="mx-auto grid h-12 w-12 place-items-center rounded-full bg-green-100 text-xl text-green-700">
            ✓
          </span>
          <p className="mt-4 text-sm leading-6 text-green-800">
            Chúng tôi đã gửi liên kết xác thực tới <strong>{registeredEmail}</strong>.
            Hãy mở email và nhấn vào liên kết để kích hoạt tài khoản.
          </p>
        </div>
        <Link to="/login" className="primary-button mt-5 w-full">
          Về trang đăng nhập
        </Link>
      </AuthLayout>
    );
  }

  return (
    <AuthLayout
      eyebrow="Tạo tài khoản"
      title="Bắt đầu cùng HotelHub"
      description="Đăng ký để lưu hành trình và quản lý các đặt phòng của bạn."
    >
      <form onSubmit={handleSubmit} noValidate className="space-y-4">
        <FormField
          id="fullName"
          name="fullName"
          label="Họ và tên"
          placeholder="Nguyễn Văn A"
          autoComplete="name"
          value={values.fullName}
          onChange={updateField}
          onBlur={() => touchField('fullName')}
          error={touched.fullName ? validationErrors.fullName : ''}
        />
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField
            id="email"
            name="email"
            type="email"
            label="Email"
            placeholder="ban@email.com"
            autoComplete="email"
            value={values.email}
            onChange={updateField}
            onBlur={() => touchField('email')}
            error={touched.email ? validationErrors.email : ''}
          />
          <FormField
            id="phone"
            name="phone"
            type="tel"
            label="Số điện thoại"
            placeholder="0901234567"
            autoComplete="tel"
            value={values.phone}
            onChange={updateField}
            onBlur={() => touchField('phone')}
            error={touched.phone ? validationErrors.phone : ''}
          />
        </div>
        <FormField
          id="password"
          name="password"
          type="password"
          label="Mật khẩu"
          placeholder="Tối thiểu 8 ký tự"
          autoComplete="new-password"
          value={values.password}
          onChange={updateField}
          onBlur={() => touchField('password')}
          error={touched.password ? validationErrors.password : ''}
        />
        <FormField
          id="confirmPassword"
          name="confirmPassword"
          type="password"
          label="Xác nhận mật khẩu"
          placeholder="Nhập lại mật khẩu"
          autoComplete="new-password"
          value={values.confirmPassword}
          onChange={updateField}
          onBlur={() => touchField('confirmPassword')}
          error={touched.confirmPassword ? validationErrors.confirmPassword : ''}
        />
        <p className="text-xs leading-5 text-slate-500">
          Mật khẩu cần từ 8 ký tự, có ít nhất một chữ hoa, một chữ thường và một số.
        </p>

        {submitError && (
          <div role="alert" className="rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-700">
            {submitError}
          </div>
        )}

        <button
          type="submit"
          disabled={!isFormValid(validationErrors) || isSubmitting}
          className="primary-button w-full"
        >
          {isSubmitting ? 'Đang tạo tài khoản...' : 'Tạo tài khoản'}
        </button>
      </form>

      <p className="mt-6 text-center text-sm text-slate-600">
        Đã có tài khoản?{' '}
        <Link to="/login" className="font-semibold text-blue-700 hover:text-blue-800">
          Đăng nhập
        </Link>
      </p>
    </AuthLayout>
  );
}
