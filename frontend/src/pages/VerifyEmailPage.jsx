import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import AuthLayout from '../features/auth/components/AuthLayout';
import { verifyEmail } from '../features/auth/services/authService';
import { getApiErrorMessage } from '../shared/api/getApiErrorMessage';

export default function VerifyEmailPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const [status, setStatus] = useState(token ? 'loading' : 'error');
  const [message, setMessage] = useState(
    token ? '' : 'Liên kết xác thực không hợp lệ hoặc thiếu token.',
  );

  useEffect(() => {
    if (!token) return;
    let active = true;

    verifyEmail(token)
      .then(() => {
        if (active) {
          setStatus('success');
          setMessage('Email đã được xác thực. Bạn có thể đăng nhập ngay bây giờ.');
        }
      })
      .catch((error) => {
        if (active) {
          setStatus('error');
          setMessage(getApiErrorMessage(error, 'Không thể xác thực email. Liên kết có thể đã hết hạn.'));
        }
      });

    return () => {
      active = false;
    };
  }, [token]);

  return (
    <AuthLayout
      eyebrow="Xác thực email"
      title={
        status === 'loading'
          ? 'Đang xác thực tài khoản'
          : status === 'success'
            ? 'Xác thực thành công'
            : 'Không thể xác thực'
      }
      description="HotelHub đang kiểm tra liên kết kích hoạt tài khoản của bạn."
    >
      {status === 'loading' && (
        <div className="flex items-center gap-3 rounded-lg border border-blue-100 bg-blue-50 p-4 text-sm text-blue-800">
          <span className="h-5 w-5 animate-spin rounded-full border-2 border-blue-200 border-t-blue-700" />
          Vui lòng chờ trong giây lát...
        </div>
      )}

      {status !== 'loading' && (
        <>
          <div
            role="status"
            className={`rounded-lg border p-4 text-sm ${
              status === 'success'
                ? 'border-green-200 bg-green-50 text-green-700'
                : 'border-red-200 bg-red-50 text-red-700'
            }`}
          >
            {message}
          </div>
          <Link to="/login" className="primary-button mt-5 w-full">
            Đến trang đăng nhập
          </Link>
        </>
      )}
    </AuthLayout>
  );
}
