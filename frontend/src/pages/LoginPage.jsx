import { useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import AuthLayout from '../features/auth/components/AuthLayout';
import GoogleSignInButton from '../features/auth/components/GoogleSignInButton';
import useAuth from '../features/auth/hooks/useAuth';
import { consumePostAuthRedirect, storePostAuthRedirect } from '../features/auth/utils/postAuthRedirect';

export default function LoginPage() {
  const { login, googleLogin, isAuthenticated } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  // Once Keycloak redirects back here already authenticated, forward the
  // user to wherever they originally meant to go (e.g. back to /checkout).
  useEffect(() => {
    if (!isAuthenticated) return;
    const { redirectTo, state } = consumePostAuthRedirect();
    navigate(redirectTo, { replace: true, state });
  }, [isAuthenticated, navigate]);

  function handleEmailLogin() {
    storePostAuthRedirect(location.state);
    login();
  }

  function handleGoogleLogin() {
    storePostAuthRedirect(location.state);
    googleLogin();
  }

  return (
    <AuthLayout
      eyebrow="Chào mừng trở lại"
      title="Đăng nhập HotelHub"
      description="Tiếp tục để quản lý đặt phòng và khám phá ưu đãi dành cho bạn."
    >
      <div className="space-y-4">
        <button type="button" onClick={handleEmailLogin} className="primary-button w-full">
          Đăng nhập với email
        </button>

        <div className="my-2 flex items-center gap-3" aria-hidden="true">
          <span className="h-px flex-1 bg-slate-200" />
          <span className="text-xs text-slate-500">hoặc</span>
          <span className="h-px flex-1 bg-slate-200" />
        </div>

        <GoogleSignInButton onClick={handleGoogleLogin} />
      </div>

      <p className="mt-6 text-center text-sm text-slate-600">
        Chưa có tài khoản?{' '}
        <Link to="/register" className="font-semibold text-blue-700 hover:text-blue-800">
          Đăng ký miễn phí
        </Link>
      </p>
    </AuthLayout>
  );
}
