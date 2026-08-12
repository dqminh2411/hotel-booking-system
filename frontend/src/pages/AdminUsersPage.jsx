import { useState } from 'react';
import { AdminLayout } from '@/shared/components/AdminLayout/AdminLayout';
import { PageHeader } from '@/shared/components/PageHeader/PageHeader';
import { Pagination } from '@/shared/components/Pagination/Pagination';
import { Button } from '@/shared/components/Button/Button';
import { FormInput } from '@/shared/components/FormInput/FormInput';
import { Select } from '@/shared/components/Select/Select';
import { ConfirmDialog } from '@/shared/components/ConfirmDialog/ConfirmDialog';
import { useDisclosure } from '@/shared/hooks/useDisclosure';
import { useAdminUsers } from '@/features/admin/hooks/useAdminUsers';
import { useAdminUserMutations } from '@/features/admin/hooks/useAdminUserMutations';
import { UserTable } from '@/features/admin/components/UserTable';
import { UserFormModal } from '@/features/admin/components/UserFormModal';
import { LockUserModal } from '@/features/admin/components/LockUserModal';
import { USER_STATUS_OPTIONS } from '@/features/admin/constants/userOptions';
import { PlusIcon } from '@/shared/components/AdminLayout/AdminIcons';

/**
 * Trang Admin - Quản lý người dùng.
 * Gom UI cho GET /api/admin/users (bảng + tìm kiếm + lọc trạng thái + phân trang),
 * cùng các thao tác ghi: tạo mới, sửa, khóa/mở khóa, xóa mềm (đều thuộc AdminController
 * phần "/*==== User ====*\/" của user-service).
 */
export default function AdminUsersPage() {
  const list = useAdminUsers();
  const mutations = useAdminUserMutations();

  const formModal = useDisclosure();
  const lockModal = useDisclosure();
  const unlockDialog = useDisclosure();
  const deleteDialog = useDisclosure();

  const [formMode, setFormMode] = useState('create');
  const [selectedUser, setSelectedUser] = useState(null);

  function openCreate() {
    setFormMode('create');
    setSelectedUser(null);
    mutations.clearError();
    formModal.open();
  }

  function openEdit(user) {
    setFormMode('edit');
    setSelectedUser(user);
    mutations.clearError();
    formModal.open();
  }

  async function handleFormSubmit(payload) {
    const ok =
      formMode === 'create'
        ? await mutations.createUser(payload)
        : await mutations.updateUser(selectedUser.userId, payload);

    if (ok) {
      formModal.close();
      list.reload();
    }
  }

  function openLock(user) {
    setSelectedUser(user);
    mutations.clearError();
    lockModal.open();
  }

  async function handleLockSubmit(reason) {
    const ok = await mutations.lockUser(selectedUser.userId, reason);
    if (ok) {
      lockModal.close();
      list.reload();
    }
  }

  function openUnlockConfirm(user) {
    setSelectedUser(user);
    mutations.clearError();
    unlockDialog.open();
  }

  async function handleUnlockConfirm() {
    const ok = await mutations.unlockUser(selectedUser.userId);
    if (ok) {
      unlockDialog.close();
      list.reload();
    }
  }

  function openDeleteConfirm(user) {
    setSelectedUser(user);
    mutations.clearError();
    deleteDialog.open();
  }

  async function handleDeleteConfirm() {
    const ok = await mutations.deleteUser(selectedUser.userId);
    if (ok) {
      deleteDialog.close();
      list.reload();
    }
  }

  return (
    <AdminLayout>
      <PageHeader
        title="Quản lý người dùng"
        subtitle="Danh sách tài khoản người dùng trong hệ thống (GET /api/admin/users)."
        actions={
          <Button variant="primary" leftIcon={<PlusIcon className="h-4 w-4" />} onClick={openCreate}>
            Tạo người dùng
          </Button>
        }
      />

      <div className="mb-4 flex flex-col gap-3 rounded-lg border border-slate-200 bg-white p-4 sm:flex-row sm:items-end">
        <div className="flex-1">
          <FormInput
            label="Tìm kiếm"
            placeholder="Tìm theo email, họ tên..."
            value={list.search}
            onChange={(e) => list.setSearch(e.target.value)}
          />
        </div>
        <div className="sm:w-56">
          <Select
            label="Trạng thái"
            placeholder="Tất cả trạng thái"
            options={USER_STATUS_OPTIONS}
            value={list.status}
            onChange={(e) => list.setStatus(e.target.value)}
          />
        </div>
      </div>

      {list.errorMessage && (
        <div className="mb-4 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          {list.errorMessage}
        </div>
      )}

      <UserTable
        data={list.items}
        isLoading={list.isLoading}
        onEdit={openEdit}
        onLock={openLock}
        onUnlock={openUnlockConfirm}
        onDelete={openDeleteConfirm}
      />

      <Pagination
        page={list.page}
        totalPages={list.totalPages}
        totalElements={list.totalElements}
        onPageChange={list.setPage}
      />

      <UserFormModal
        isOpen={formModal.isOpen}
        mode={formMode}
        userId={selectedUser?.userId}
        isSubmitting={mutations.isSubmitting}
        errorMessage={mutations.errorMessage}
        onClose={formModal.close}
        onSubmit={handleFormSubmit}
      />

      <LockUserModal
        isOpen={lockModal.isOpen}
        user={selectedUser}
        isSubmitting={mutations.isSubmitting}
        errorMessage={mutations.errorMessage}
        onClose={lockModal.close}
        onSubmit={handleLockSubmit}
      />

      <ConfirmDialog
        isOpen={unlockDialog.isOpen}
        title="Mở khóa tài khoản"
        message={`Mở khóa tài khoản của "${selectedUser?.fullname || selectedUser?.email}"?`}
        confirmLabel="Mở khóa"
        isLoading={mutations.isSubmitting}
        onConfirm={handleUnlockConfirm}
        onCancel={unlockDialog.close}
      />

      <ConfirmDialog
        isOpen={deleteDialog.isOpen}
        title="Xóa người dùng"
        message={`Xóa (mềm) tài khoản của "${selectedUser?.fullname || selectedUser?.email}"? Hành động này có thể ảnh hưởng tới dữ liệu liên quan.`}
        confirmLabel="Xóa"
        danger
        isLoading={mutations.isSubmitting}
        onConfirm={handleDeleteConfirm}
        onCancel={deleteDialog.close}
      />
    </AdminLayout>
  );
}