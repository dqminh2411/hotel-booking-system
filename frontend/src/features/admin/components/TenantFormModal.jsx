import { useEffect, useState } from 'react';
import { Modal } from '@/shared/components/Modal/Modal';
import { Button } from '@/shared/components/Button/Button';
import { FormInput } from '@/shared/components/FormInput/FormInput';
import { Select } from '@/shared/components/Select/Select';
import { TENANT_STATUS_OPTIONS } from '@/features/admin/constants/tenantOptions';

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

const EMPTY_FORM = { ownerId: '', name: '', status: 'ACTIVE' };

/**
 * Modal Tạo mới / Sửa tenant.
 * - mode = 'create': goi POST /api/admin/tenants - dung CreateTenantRequest
 *   { ownerId, name }. ownerId phai la userId (UUID) cua mot tai khoan role
 *   HOTEL_OWNER da ton tai - man hinh nay chua co bo chon nguoi dung nen Admin
 *   nhap truc tiep UUID (co the lay tu trang Quan ly nguoi dung).
 * - mode = 'edit': goi PATCH /api/admin/tenants/{tenantId} - dung UpdateTenantRequest
 *   { name, status }. LUU Y: name o day bat buoc >= 20 ky tu (@Size(min = 20) trong
 *   UpdateTenantRequest), khac voi luc tao moi (chi @NotBlank, khong gioi han).
 *   Khong doi duoc ownerId sau khi tao (backend khong nhan field nay o update).
 *
 * Props: isOpen, mode ('create'|'edit'), tenant (row hien tai, dung khi edit),
 *        isSubmitting, errorMessage, onClose, onSubmit(payload)
 */
export function TenantFormModal({ isOpen, mode, tenant, isSubmitting, errorMessage, onClose, onSubmit }) {
  const isEdit = mode === 'edit';
  const [form, setForm] = useState(EMPTY_FORM);
  const [fieldErrors, setFieldErrors] = useState({});

  useEffect(() => {
    if (!isOpen) return;
    setFieldErrors({});

    if (isEdit && tenant) {
      setForm({ ownerId: tenant.ownerId || '', name: tenant.name || '', status: tenant.status || 'ACTIVE' });
    } else {
      setForm(EMPTY_FORM);
    }
  }, [isOpen, isEdit, tenant]);

  function updateField(name, value) {
    setForm((prev) => ({ ...prev, [name]: value }));
    setFieldErrors((prev) => ({ ...prev, [name]: undefined }));
  }

  function validate() {
    const errors = {};

    if (!isEdit) {
      if (!form.ownerId.trim()) {
        errors.ownerId = 'Owner ID không được để trống';
      } else if (!UUID_PATTERN.test(form.ownerId.trim())) {
        errors.ownerId = 'Owner ID phải là một UUID hợp lệ';
      }
      if (!form.name.trim()) errors.name = 'Tên doanh nghiệp không được để trống';
    } else if (!form.name.trim() || form.name.trim().length < 20) {
      errors.name = 'Tên phải có ít nhất 20 ký tự khi cập nhật';
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  }

  function handleSubmit() {
    if (!validate()) return;
    onSubmit(form);
  }

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={isEdit ? 'Sửa tenant' : 'Tạo tenant mới'}
      width={480}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
            Hủy
          </Button>
          <Button variant="primary" isLoading={isSubmitting} onClick={handleSubmit}>
            {isEdit ? 'Lưu thay đổi' : 'Tạo tenant'}
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-4">
        {errorMessage && (
          <div className="rounded-md bg-red-50 px-4 py-3 text-sm text-red-700">{errorMessage}</div>
        )}

        {!isEdit && (
          <FormInput
            label="Owner ID (UUID chủ sở hữu)"
            required
            placeholder="00000000-0000-0000-0000-000000000000"
            value={form.ownerId}
            onChange={(e) => updateField('ownerId', e.target.value)}
            error={fieldErrors.ownerId}
            helpText="Là userId của một tài khoản có vai trò HOTEL_OWNER, có thể tra ở trang Quản lý người dùng."
          />
        )}

        <FormInput
          label="Tên doanh nghiệp"
          required
          placeholder="Ví dụ: Công ty TNHH Khách sạn ABC"
          value={form.name}
          onChange={(e) => updateField('name', e.target.value)}
          error={fieldErrors.name}
          helpText={isEdit ? 'Ít nhất 20 ký tự' : undefined}
        />

        {isEdit && (
          <Select
            label="Trạng thái"
            options={TENANT_STATUS_OPTIONS}
            value={form.status}
            onChange={(e) => updateField('status', e.target.value)}
          />
        )}
      </div>
    </Modal>
  );
}