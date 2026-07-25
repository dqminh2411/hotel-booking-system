import { Link, useNavigate } from 'react-router-dom';
import AuthLayout from '../features/auth/components/AuthLayout';
import useAuth from '../features/auth/hooks/useAuth';
import { consumePostAuthRedirect } from '../features/auth/utils/postAuthRedirect';

export default function VerifyEmailPage() {
  const { isAuthenticated, user } = useAuth();
  const navigate = useNavigate();

  function handleContinue() {
    const { redirectTo, state } = consumePostAuthRedirect();
    navigate(redirectTo, { replace: true, state });
  }

  // Happy path: same browser/tab that registered is still open when the
  // user clicks the verification link, so Keycloak resumes the flow and
  // sends them back here already authenticated. The backend profile row
  // (fullName/email/phone/keycloakId) is created in the background by
  // useSyncUserProfile (mounted app-wide), so there's nothing to call here.
  if (isAuthenticated) {
    return (
      <AuthLayout
        eyebrow="Đăng ký thành công"
        title="Xác thực email thành công"
        description="Tài khoản của bạn đã sẵn sàng để sử dụng."
      >
        <div className="rounded-lg border border-green-200 bg-green-50 p-5 text-center">
          <span className="mx-auto grid h-12 w-12 place-items-center rounded-full bg-green-100 text-xl text-green-700">
            ✓
          </span>
          <p className="mt-4 text-sm leading-6 text-green-800">
            Chào mừng {user?.fullName || user?.email} đến với HotelHub! Email của bạn đã được xác thực.
          </p>
        </div>
        <button type="button" onClick={handleContinue} className="primary-button mt-5 w-full">
          Tiếp tục
        </button>
      </AuthLayout>
    );
  }

  // If this page is opened without an active Keycloak session (e.g. the
  // verification link was opened on a different device/browser than the
  // one used to register), Keycloak itself already showed its own
  // confirmation page. Here we just point the user back to login.
  return (
    <AuthLayout
      eyebrow="Xác thực email"
      title="Email đã được xác thực"
      description="Bạn có thể đăng nhập ngay bây giờ để tiếp tục sử dụng HotelHub."
    >
      <Link to="/login" className="primary-button w-full">
        Đến trang đăng nhập
      </Link>
    </AuthLayout>
  );
}
