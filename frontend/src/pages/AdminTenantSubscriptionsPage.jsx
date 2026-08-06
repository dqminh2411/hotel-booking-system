import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { AdminLayout } from '@/shared/components/AdminLayout/AdminLayout';
import { PageHeader } from '@/shared/components/PageHeader/PageHeader';
import { Pagination } from '@/shared/components/Pagination/Pagination';
import { Button } from '@/shared/components/Button/Button';
import { StatusBadge } from '@/shared/components/StatusBadge/StatusBadge';
import { ConfirmDialog } from '@/shared/components/ConfirmDialog/ConfirmDialog';
import ErrorState from '@/shared/components/ErrorState';
import { Loading } from '@/shared/components/Loading/Loading';
import { useDisclosure } from '@/shared/hooks/useDisclosure';
import { useAdminTenantDetail } from '@/features/admin/hooks/useAdminTenantDetail';
import { useTenantSubscriptions } from '@/features/admin/hooks/useTenantSubscriptions';
import { useTenantSubscriptionMutations } from '@/features/admin/hooks/useTenantSubscriptionMutations';
import { TenantSubscriptionTable } from '@/features/admin/components/TenantSubscriptionTable';
import { CreateTenantSubscriptionModal } from '@/features/admin/components/CreateTenantSubscriptionModal';
import { UpdateTenantSubscriptionStatusModal } from '@/features/admin/components/UpdateTenantSubscriptionStatusModal';
import { formatDate } from '@/shared/utils/date';
import {
  getTenantStatusLabel,
  TENANT_STATUS_TONE,
  getTenantSubscriptionStatusLabel,
  TENANT_SUBSCRIPTION_STATUS_TONE,
  getBillingCycleLabel,
} from '@/features/admin/constants/tenantOptions';
import { ROUTES } from '@/shared/constants/routes';
import { PlusIcon } from '@/shared/components/AdminLayout/AdminIcons';

/**
 * Trang Admin - Quản lý gói dịch vụ của 1 tenant.
 * - Header: thông tin tenant + gói đang dùng (GET /api/admin/tenants/{tenantId} ->
 *   TenantDetailResponse.activeSubscription).
 * - Bảng bên dưới: toàn bộ lịch sử gói (GET /api/admin/tenants/{tenantId}/subscriptions),
 *   kèm hành động Đăng ký gói mới / Đổi trạng thái / Xóa (TenantSubscriptionController).
 */
export default function AdminTenantSubscriptionsPage() {
  const { tenantId } = useParams();

  const { tenant, status: detailStatus, errorMessage: detailError, reload: reloadDetail } =
    useAdminTenantDetail(tenantId);
  const history = useTenantSubscriptions(tenantId);
  const mutations = useTenantSubscriptionMutations();

  const createModal = useDisclosure();
  const statusModal = useDisclosure();
  const deleteDialog = useDisclosure();
  const [selectedSubscription, setSelectedSubscription] = useState(null);

  function refreshAll() {
    reloadDetail();
    history.reload();
  }

  function openCreate() {
    mutations.clearError();
    createModal.open();
  }

  async function handleCreateSubmit(subscriptionId) {
    const ok = await mutations.createSubscription(tenantId, subscriptionId);
    if (ok) {
      createModal.close();
      refreshAll();
    }
  }

  function openStatusModal(subscription) {
    setSelectedSubscription(subscription);
    mutations.clearError();
    statusModal.open();
  }

  async function handleStatusSubmit(newStatus) {
    const ok = await mutations.updateStatus(selectedSubscription.id, newStatus);
    if (ok) {
      statusModal.close();
      refreshAll();
    }
  }

  function openDeleteConfirm(subscription) {
    setSelectedSubscription(subscription);
    mutations.clearError();
    deleteDialog.open();
  }

  async function handleDeleteConfirm() {
    const ok = await mutations.deleteSubscription(selectedSubscription.id);
    if (ok) {
      deleteDialog.close();
      refreshAll();
    }
  }

  const activeSubscription = tenant?.activeSubscription;

  return (
    <AdminLayout>
      <div className="mb-2">
        <Link to={ROUTES.admin.tenants} className="text-sm font-medium text-blue-700 hover:underline">
          ← Quay lại danh sách tenant
        </Link>
      </div>

      {detailStatus === 'loading' && <Loading label="Đang tải thông tin tenant..." />}

      {detailStatus === 'error' && <ErrorState message={detailError} onRetry={reloadDetail} />}

      {detailStatus === 'success' && tenant && (
        <>
          <PageHeader
            title={`Gói dịch vụ - ${tenant.name}`}
            subtitle="Lịch sử đăng ký gói dịch vụ của tenant này."
            actions={
              <Button variant="primary" leftIcon={<PlusIcon className="h-4 w-4" />} onClick={openCreate}>
                Đăng ký gói mới
              </Button>
            }
          />

          <div className="mb-6 rounded-lg border border-slate-200 bg-white p-5">
            <div className="mb-3 flex flex-wrap items-center gap-2">
              <span className="text-sm text-slate-500">Trạng thái tenant:</span>
              <StatusBadge label={getTenantStatusLabel(tenant.status)} tone={TENANT_STATUS_TONE[tenant.status] ?? 'neutral'} />
            </div>

            <h3 className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-400">Gói đang sử dụng</h3>
            {activeSubscription ? (
              <div className="flex flex-wrap items-center gap-x-6 gap-y-2">
                <div>
                  <p className="font-bold text-sky-900">{activeSubscription.planName}</p>
                  <p className="text-xs text-slate-500">{activeSubscription.planCode}</p>
                </div>
                <StatusBadge
                  label={getTenantSubscriptionStatusLabel(activeSubscription.status)}
                  tone={TENANT_SUBSCRIPTION_STATUS_TONE[activeSubscription.status] ?? 'neutral'}
                />
                <span className="text-sm text-slate-500">
                  Bắt đầu: <strong className="text-slate-700">{formatDate(activeSubscription.startedAt)}</strong>
                </span>
                <span className="text-sm text-slate-500">
                  Hết hạn: <strong className="text-slate-700">{formatDate(activeSubscription.expiresAt)}</strong>
                </span>
              </div>
            ) : (
              <p className="text-sm text-slate-400">Tenant chưa có gói dịch vụ nào đang hiệu lực.</p>
            )}
          </div>

          {(history.errorMessage || mutations.errorMessage) && (
            <div className="mb-4 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
              {history.errorMessage || mutations.errorMessage}
            </div>
          )}

          <h3 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-400">
            Lịch sử đăng ký gói
          </h3>

          <TenantSubscriptionTable
            data={history.items}
            isLoading={history.isLoading}
            onChangeStatus={openStatusModal}
            onDelete={openDeleteConfirm}
          />

          <Pagination
            page={history.page}
            totalPages={history.totalPages}
            totalElements={history.totalElements}
            onPageChange={history.setPage}
          />

          <CreateTenantSubscriptionModal
            isOpen={createModal.isOpen}
            tenantId={tenantId}
            tenantName={tenant.name}
            isSubmitting={mutations.isSubmitting}
            errorMessage={mutations.errorMessage}
            onClose={createModal.close}
            onSubmit={handleCreateSubmit}
          />

          <UpdateTenantSubscriptionStatusModal
            isOpen={statusModal.isOpen}
            subscription={selectedSubscription}
            isSubmitting={mutations.isSubmitting}
            errorMessage={mutations.errorMessage}
            onClose={statusModal.close}
            onSubmit={handleStatusSubmit}
          />

          <ConfirmDialog
            isOpen={deleteDialog.isOpen}
            title="Xóa gói dịch vụ"
            message={`Xóa bản ghi gói "${selectedSubscription?.planName}" (${getBillingCycleLabel(
              selectedSubscription?.billingCycle,
            )}) khỏi lịch sử của tenant này?`}
            confirmLabel="Xóa"
            danger
            isLoading={mutations.isSubmitting}
            onConfirm={handleDeleteConfirm}
            onCancel={deleteDialog.close}
          />
        </>
      )}
    </AdminLayout>
  );
}