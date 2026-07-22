import { useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import AuthLayout from '../features/auth/components/AuthLayout';
import GoogleSignInButton from '../features/auth/components/GoogleSignInButton';
import useAuth from '../features/auth/hooks/useAuth';
import { consumePostAuthRedirect, storePostAuthRedirect } from '../features/auth/utils/postAuthRedirect';

export default function RegisterPage() {
  const { register, googleLogin, isAuthenticated } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  // Keycloak logs the user in automatically right after a successful
  // registration, so we land back here already authenticated.
  useEffect(() => {
    if (!isAuthenticated) return;
    const { redirectTo, state } = consumePostAuthRedirect();
    navigate(redirectTo, { replace: true, state });
  }, [isAuthenticated, navigate]);

  function handleEmailRegister() {
    storePostAuthRedirect(location.state);
    // If the same browser/tab is still around when the user clicks the
    // verification link in their email, Keycloak resumes this flow and
    // lands them back on /verify-email already logged in. If they open the
    // link elsewhere, they'll see Keycloak's own confirmation page instead
    // and can just log in normally afterwards — useSyncUserProfile takes
    // care of creating the backend profile row either way.
    register({ redirectUri: `${window.location.origin}/verify-email` });
  }

  function handleGoogleRegister() {
    storePostAuthRedirect(location.state);
    // Same Google identity provider flow works for first-time sign-up too;
    // Keycloak creates the account automatically on first login via Google.
    googleLogin();
  }

  return (
    <AuthLayout
      eyebrow="Tạo tài khoản"
      title="Bắt đầu cùng HotelHub"
      description="Đăng ký để lưu hành trình và quản lý các đặt phòng của bạn."
    >
      <div className="space-y-4">
        <button type="button" onClick={handleEmailRegister} className="primary-button w-full">
          Đăng ký với email
        </button>

        <div className="my-2 flex items-center gap-3" aria-hidden="true">
          <span className="h-px flex-1 bg-slate-200" />
          <span className="text-xs text-slate-500">hoặc</span>
          <span className="h-px flex-1 bg-slate-200" />
        </div>

        <GoogleSignInButton onClick={handleGoogleRegister} />
      </div>

      <p className="mt-6 text-center text-sm text-slate-600">
        Đã có tài khoản?{' '}
        <Link to="/login" className="font-semibold text-blue-700 hover:text-blue-800">
          Đăng nhập
        </Link>
      </p>
    </AuthLayout>
  );
}
