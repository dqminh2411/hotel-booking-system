import { useCallback, useEffect, useState } from 'react';
import { adminUserService } from '@/features/admin/services/adminUserService';
import { getApiErrorMessage } from '@/shared/api/getApiErrorMessage';

/**
 * Nap chi tiet 1 user (GET /api/admin/users/{userId}) khi can du lieu day du hon
 * ResponseUser (danh sach) - vi du field "address", "roles" chi co o UserResponse.
 * Dung trong UserFormModal khi mo o che do "edit".
 *
 * enabled: chi goi API khi true (tranh goi khi modal dang dong / mode = 'create').
 */
export function useAdminUserDetail(userId, enabled) {
  const [user, setUser] = useState(null);
  const [status, setStatus] = useState('idle'); // idle | loading | success | error
  const [errorMessage, setErrorMessage] = useState('');

  const load = useCallback(async () => {
    if (!enabled || !userId) return;

    setStatus('loading');
    setErrorMessage('');
    try {
      const data = await adminUserService.getUserDetail(userId);
      setUser(data);
      setStatus('success');
    } catch (error) {
      setUser(null);
      setErrorMessage(getApiErrorMessage(error, 'Không thể tải thông tin người dùng. Vui lòng thử lại.'));
      setStatus('error');
    }
  }, [userId, enabled]);

  useEffect(() => {
    if (enabled) {
      load();
    } else {
      setUser(null);
      setStatus('idle');
      setErrorMessage('');
    }
  }, [enabled, load]);

  return { user, status, errorMessage, reload: load };
}