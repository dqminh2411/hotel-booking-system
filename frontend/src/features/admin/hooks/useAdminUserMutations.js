import { useCallback, useState } from 'react';
import { adminUserService } from '@/features/admin/services/adminUserService';
import { getApiErrorMessage } from '@/shared/api/getApiErrorMessage';

/** Hook gom cac thao tac ghi cho User: tao / sua / khóa / mở khóa / xóa mềm. */
export function useAdminUserMutations() {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const run = useCallback(async (action, fallbackMessage) => {
    setIsSubmitting(true);
    setErrorMessage('');
    try {
      await action();
      return true;
    } catch (err) {
      setErrorMessage(getApiErrorMessage(err, fallbackMessage));
      return false;
    } finally {
      setIsSubmitting(false);
    }
  }, []);

  const createUser = useCallback(
    (payload) => run(() => adminUserService.createUser(payload), 'Không thể tạo người dùng. Vui lòng thử lại.'),
    [run],
  );

  const updateUser = useCallback(
    (userId, payload) =>
      run(() => adminUserService.updateUser(userId, payload), 'Không thể cập nhật người dùng. Vui lòng thử lại.'),
    [run],
  );

  const deleteUser = useCallback(
    (userId) => run(() => adminUserService.deleteUser(userId), 'Không thể xóa người dùng. Vui lòng thử lại.'),
    [run],
  );

  const lockUser = useCallback(
    (userId, reason) =>
      run(() => adminUserService.lockUser(userId, reason), 'Không thể khóa người dùng. Vui lòng thử lại.'),
    [run],
  );

  const unlockUser = useCallback(
    (userId) => run(() => adminUserService.unlockUser(userId), 'Không thể mở khóa người dùng. Vui lòng thử lại.'),
    [run],
  );

  return {
    isSubmitting,
    errorMessage,
    createUser,
    updateUser,
    deleteUser,
    lockUser,
    unlockUser,
    clearError: () => setErrorMessage(''),
  };
}