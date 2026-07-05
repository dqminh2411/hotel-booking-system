import { useCallback, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import AuthLayout from '../features/auth/components/AuthLayout';
import FormField from '../features/auth/components/FormField';
import GoogleSignInButton from '../features/auth/components/GoogleSignInButton';
import useAuth from '../features/auth/hooks/useAuth';
import { isFormValid, validateLogin } from '../features/auth/utils/authValidation';
import { getApiErrorMessage } from '../shared/api/getApiErrorMessage';

const initialValues = { email: '', password: '' };

export default function LoginPage() {
  const [values, setValues] = useState(initialValues);
  const [touched, setTouched] = useState({});
  const [submitError, setSubmitError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const { login, googleLogin } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const validationErrors = useMemo(() => validateLogin(values), [values]);
  const redirectTo = location.state?.from || '/';

  function updateField(event) {
    const { name, value } = event.target;
    setValues((current) => ({ ...current, [name]: value }));
    setSubmitError('');
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setTouched({ email: true, password: true });
    if (!isFormValid(validationErrors)) return;

    setIsSubmitting(true);
    setSubmitError('');
    try {
      await login(values);
      navigate(redirectTo, { replace: true });
    } catch (error) {
      setSubmitError(
        error.response?.status === 401
          ? 'Tài khoản hoặc mật khẩu không đúng'
          : getApiErrorMessage(error, 'Đăng nhập không thành công. Vui lòng thử lại.'),
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  const handleGoogleCredential = useCallback(
    async (idToken) => {
      setIsSubmitting(true);
      setSubmitError('');
      try {
        await googleLogin(idToken);
        navigate(redirectTo, { replace: true });
      } catch (error) {
        setSubmitError(getApiErrorMessage(error, 'Đăng nhập Google không thành công.'));
      } finally {
        setIsSubmitting(false);
      }
    },
    [googleLogin, navigate, redirectTo],
  );

  return (
    <AuthLayout
      eyebrow="Chào mừng trở lại"
      title="Đăng nhập HotelHub"
      description="Tiếp tục để quản lý đặt phòng và khám phá ưu đãi dành cho bạn."
    >
      <form onSubmit={handleSubmit} noValidate className="space-y-4">
        <FormField
          id="email"
          name="email"
          type="email"
          label="Địa chỉ email"
          placeholder="ban@email.com"
          autoComplete="email"
          value={values.email}
          onChange={updateField}
          onBlur={() => setTouched((current) => ({ ...current, email: true }))}
          error={touched.email ? validationErrors.email : ''}
        />
        <FormField
          id="password"
          name="password"
          type="password"
          label="Mật khẩu"
          placeholder="Nhập mật khẩu"
          autoComplete="current-password"
          value={values.password}
          onChange={updateField}
          onBlur={() => setTouched((current) => ({ ...current, password: true }))}
          error={touched.password ? validationErrors.password : ''}
        />

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
          {isSubmitting ? 'Đang đăng nhập...' : 'Đăng nhập'}
        </button>
      </form>

      <div className="my-6 flex items-center gap-3" aria-hidden="true">
        <span className="h-px flex-1 bg-slate-200" />
        <span className="text-xs text-slate-500">hoặc</span>
        <span className="h-px flex-1 bg-slate-200" />
      </div>

      <GoogleSignInButton onCredential={handleGoogleCredential} disabled={isSubmitting} />

      <p className="mt-6 text-center text-sm text-slate-600">
        Chưa có tài khoản?{' '}
        <Link to="/register" className="font-semibold text-blue-700 hover:text-blue-800">
          Đăng ký miễn phí
        </Link>
      </p>
    </AuthLayout>
  );
}
