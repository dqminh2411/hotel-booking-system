import { useEffect, useState } from 'react';
import { Modal } from '@/shared/components/Modal/Modal';
import { Button } from '@/shared/components/Button/Button';
import { FormInput } from '@/shared/components/FormInput/FormInput';
import { Textarea } from '@/shared/components/Textarea/Textarea';
import { Select } from '@/shared/components/Select/Select';
import { StatusBadge } from '@/shared/components/StatusBadge/StatusBadge';
import { useAdminUserDetail } from '@/features/admin/hooks/useAdminUserDetail';
import { USER_ROLE_OPTIONS, getUserRoleLabel } from '@/features/admin/constants/userOptions';

const PHONE_PATTERN = /^0\d{9}$/;

const EMPTY_FORM = {
  email: '',
  password: '',
  phone: '',
  fullName: '',
  address: '',
  avatarUrl: '',
  role: '',
};

/**
 * Modal Tạo mới / Sửa người dùng.
 * - mode = 'create': goi POST /api/admin/users - dung CreateUserAdminRequest
 *   (email, password, phone?, fullName, address, avatarUrl?, role?).
 * - mode = 'edit': goi PATCH /api/admin/users/{userId} - dung UpdateUserRequest
 *   (CHI fullName/phone/address/avatarUrl - khong doi duoc email/mat khau/vai tro).
 *   Khi mo, nap chi tiet qua GET /api/admin/users/{userId} de co du lieu hien tai
 *   (email/roles hien thi read-only, cac field con lai de sua).
 *
 * Props: isOpen, mode ('create'|'edit'), userId (bat buoc khi edit), isSubmitting,
 *        errorMessage, onClose, onSubmit(payload)
 */
export function UserFormModal({ isOpen, mode, userId, isSubmitting, errorMessage, onClose, onSubmit }) {
  const isEdit = mode === 'edit';
  const detail = useAdminUserDetail(userId, isOpen && isEdit);

  const [form, setForm] = useState(EMPTY_FORM);
  const [fieldErrors, setFieldErrors] = useState({});

  useEffect(() => {
    if (!isOpen) return;
    setFieldErrors({});

    if (mode === 'create') {
      setForm(EMPTY_FORM);
    }
  }, [isOpen, mode]);

  // Khi nap xong chi tiet user (mode edit), do du lieu vao form.
  useEffect(() => {
    if (isEdit && detail.status === 'success' && detail.user) {
      setForm({
        email: detail.user.email || '',
        password: '',
        phone: detail.user.phone || '',
        fullName: detail.user.fullName || '',
        address: detail.user.address || '',
        avatarUrl: detail.user.avatarUrl || '',
        role: '',
      });
    }
  }, [isEdit, detail.status, detail.user]);

  function updateField(name, value) {
    setForm((prev) => ({ ...prev, [name]: value }));
    setFieldErrors((prev) => ({ ...prev, [name]: undefined }));
  }

  function validate() {
    const errors = {};

    if (form.phone && !PHONE_PATTERN.test(form.phone)) {
      errors.phone = 'Số điện thoại phải gồm 10 chữ số và bắt đầu bằng số 0';
    }

    if (!isEdit) {
      // Rang buoc theo CreateUserAdminRequest
      if (!form.email.trim()) errors.email = 'Email không được để trống';
      if (!form.password || form.password.length < 8) errors.password = 'Mật khẩu phải có ít nhất 8 ký tự';
      if (!form.fullName.trim() || form.fullName.trim().length < 10) {
        errors.fullName = 'Họ tên phải có ít nhất 10 ký tự';
      }
      if (!form.address.trim()) errors.address = 'Địa chỉ không được để trống';
    } else if (form.fullName && form.fullName.trim().length > 255) {
      errors.fullName = 'Họ tên không được vượt quá 255 ký tự';
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  }

  function handleSubmit() {
    if (!validate()) return;
    onSubmit(form);
  }

  const isLoadingDetail = isEdit && detail.status === 'loading';

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={isEdit ? 'Sửa thông tin người dùng' : 'Tạo người dùng mới'}
      width={520}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
            Hủy
          </Button>
          <Button variant="primary" isLoading={isSubmitting} onClick={handleSubmit} disabled={isLoadingDetail}>
            {isEdit ? 'Lưu thay đổi' : 'Tạo người dùng'}
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-4">
        {errorMessage && (
          <div className="rounded-md bg-red-50 px-4 py-3 text-sm text-red-700">{errorMessage}</div>
        )}
        {isEdit && detail.status === 'error' && (
          <div className="rounded-md bg-red-50 px-4 py-3 text-sm text-red-700">{detail.errorMessage}</div>
        )}
        {isLoadingDetail && <p className="text-sm text-slate-400">Đang tải thông tin người dùng...</p>}

        {!isLoadingDetail && (
          <>
            {isEdit && (
              <div className="flex flex-wrap items-center gap-2 rounded-md bg-slate-50 px-3 py-2">
                <span className="text-sm text-slate-500">{form.email}</span>
                {detail.user?.roles?.map((role) => (
                  <StatusBadge key={role} label={getUserRoleLabel(role)} tone="info" />
                ))}
              </div>
            )}

            {!isEdit && (
              <FormInput
                label="Email"
                type="email"
                required
                placeholder="ten@example.com"
                value={form.email}
                onChange={(e) => updateField('email', e.target.value)}
                error={fieldErrors.email}
              />
            )}

            {!isEdit && (
              <FormInput
                label="Mật khẩu"
                type="password"
                required
                placeholder="Tối thiểu 8 ký tự"
                value={form.password}
                onChange={(e) => updateField('password', e.target.value)}
                error={fieldErrors.password}
              />
            )}

            <FormInput
              label="Họ và tên"
              required={!isEdit}
              placeholder="Nguyễn Văn A"
              value={form.fullName}
              onChange={(e) => updateField('fullName', e.target.value)}
              error={fieldErrors.fullName}
              helpText={!isEdit ? 'Ít nhất 10 ký tự' : undefined}
            />

            <FormInput
              label="Số điện thoại"
              placeholder="0912345678"
              value={form.phone}
              onChange={(e) => updateField('phone', e.target.value)}
              error={fieldErrors.phone}
            />

            <Textarea
              label="Địa chỉ"
              required={!isEdit}
              placeholder="Số nhà, đường, quận/huyện, tỉnh/thành phố"
              value={form.address}
              onChange={(e) => updateField('address', e.target.value)}
              error={fieldErrors.address}
            />

            <FormInput
              label="Ảnh đại diện (URL)"
              placeholder="https://..."
              value={form.avatarUrl}
              onChange={(e) => updateField('avatarUrl', e.target.value)}
              error={fieldErrors.avatarUrl}
            />

            {!isEdit && (
              <Select
                label="Vai trò"
                placeholder="-- Không chọn --"
                options={USER_ROLE_OPTIONS}
                value={form.role}
                onChange={(e) => updateField('role', e.target.value)}
              />
            )}
          </>
        )}
      </div>
    </Modal>
  );
}