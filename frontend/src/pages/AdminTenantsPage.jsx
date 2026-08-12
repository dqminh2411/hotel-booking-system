import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AdminLayout } from '@/shared/components/AdminLayout/AdminLayout';
import { PageHeader } from '@/shared/components/PageHeader/PageHeader';
import { Pagination } from '@/shared/components/Pagination/Pagination';
import { Button } from '@/shared/components/Button/Button';
import { FormInput } from '@/shared/components/FormInput/FormInput';
import { Select } from '@/shared/components/Select/Select';
import { ConfirmDialog } from '@/shared/components/ConfirmDialog/ConfirmDialog';
import { useDisclosure } from '@/shared/hooks/useDisclosure';
import { useAdminTenants } from '@/features/admin/hooks/useAdminTenants';
import { useAdminTenantMutations } from '@/features/admin/hooks/useAdminTenantMutations';
import { TenantTable } from '@/features/admin/components/TenantTable';
import { TenantFormModal } from '@/features/admin/components/TenantFormModal';
import { TENANT_STATUS_OPTIONS } from '@/features/admin/constants/tenantOptions';
import { ROUTES } from '@/shared/constants/routes';
import { PlusIcon } from '@/shared/components/AdminLayout/AdminIcons';

/**
 * Trang Admin - Quản lý Tenant.
 * Gom UI cho GET /api/admin/tenants (bảng + tìm kiếm + lọc trạng thái + phân trang),
 * cùng tạo/sửa/xóa mềm tenant. Quản lý lịch sử gói dịch vụ của từng tenant nằm ở
 * trang riêng (xem AdminTenantSubscriptionsPage.jsx), điều hướng qua nút "Quản lý gói".
 */
export default function AdminTenantsPage() {
  const navigate = useNavigate();
  const list = useAdminTenants();
  const mutations = useAdminTenantMutations();

  const formModal = useDisclosure();
  const deleteDialog = useDisclosure();

  const [formMode, setFormMode] = useState('create');
  const [selectedTenant, setSelectedTenant] = useState(null);

  function openCreate() {
    setFormMode('create');
    setSelectedTenant(null);
    mutations.clearError();
    formModal.open();
  }

  function openEdit(tenant) {
    setFormMode('edit');
    setSelectedTenant(tenant);
    mutations.clearError();
    formModal.open();
  }

  async function handleFormSubmit(payload) {
    const ok =
      formMode === 'create'
        ? await mutations.createTenant(payload)
        : await mutations.updateTenant(selectedTenant.tenantId, payload);

    if (ok) {
      formModal.close();
      list.reload();
    }
  }

  function openDeleteConfirm(tenant) {
    setSelectedTenant(tenant);
    mutations.clearError();
    deleteDialog.open();
  }

  async function handleDeleteConfirm() {
    const ok = await mutations.deleteTenant(selectedTenant.tenantId);
    if (ok) {
      deleteDialog.close();
      list.reload();
    }
  }

  return (
    <AdminLayout>
      <PageHeader
        title="Tenant & Gói dịch vụ"
        subtitle="Danh sách tenant (chủ khách sạn) trong hệ thống (GET /api/admin/tenants)."
        actions={
          <Button variant="primary" leftIcon={<PlusIcon className="h-4 w-4" />} onClick={openCreate}>
            Tạo tenant
          </Button>
        }
      />

      <div className="mb-4 flex flex-col gap-3 rounded-lg border border-slate-200 bg-white p-4 sm:flex-row sm:items-end">
        <div className="flex-1">
          <FormInput
            label="Tìm kiếm"
            placeholder="Tìm theo tên tenant, email chủ sở hữu..."
            value={list.search}
            onChange={(e) => list.setSearch(e.target.value)}
          />
        </div>
        <div className="sm:w-56">
          <Select
            label="Trạng thái"
            placeholder="Tất cả trạng thái"
            options={TENANT_STATUS_OPTIONS}
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

      <TenantTable
        data={list.items}
        isLoading={list.isLoading}
        onEdit={openEdit}
        onManageSubscriptions={(tenant) => navigate(ROUTES.admin.tenantSubscriptions(tenant.tenantId))}
        onDelete={openDeleteConfirm}
      />

      <Pagination
        page={list.page}
        totalPages={list.totalPages}
        totalElements={list.totalElements}
        onPageChange={list.setPage}
      />

      <TenantFormModal
        isOpen={formModal.isOpen}
        mode={formMode}
        tenant={selectedTenant}
        isSubmitting={mutations.isSubmitting}
        errorMessage={mutations.errorMessage}
        onClose={formModal.close}
        onSubmit={handleFormSubmit}
      />

      <ConfirmDialog
        isOpen={deleteDialog.isOpen}
        title="Xóa tenant"
        message={`Xóa (mềm) tenant "${selectedTenant?.name}"? Hành động này có thể ảnh hưởng tới các khách sạn và gói dịch vụ liên quan.`}
        confirmLabel="Xóa"
        danger
        isLoading={mutations.isSubmitting}
        onConfirm={handleDeleteConfirm}
        onCancel={deleteDialog.close}
      />
    </AdminLayout>
  );
}