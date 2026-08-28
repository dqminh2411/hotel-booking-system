import { Table } from '@/shared/components/Table/Table';
import { Button } from '@/shared/components/Button/Button';
import { StatusBadge } from '@/shared/components/StatusBadge/StatusBadge';
import { formatDate, formatDateTime } from '@/shared/utils/date';
import {
  getTenantSubscriptionStatusLabel,
  TENANT_SUBSCRIPTION_STATUS_TONE,
  getBillingCycleLabel,
} from '@/features/admin/constants/tenantOptions';
import { TrashIcon } from '@/shared/components/AdminLayout/AdminIcons';

/**
 * Bang lich su goi dich vu cua 1 tenant (GET /api/admin/tenants/{tenantId}/subscriptions).
 * Chi hien dung field co trong TenantSubscriptionResponse: id, planCode, planName,
 * planDescription, billingCycle, status, startedAt, expiresAt, createdAt.
 *
 * Props: data, isLoading, onChangeStatus(row), onDelete(row)
 */
export function TenantSubscriptionTable({ data, isLoading, onChangeStatus, onDelete }) {
  const columns = [
    {
      key: 'plan',
      header: 'Gói dịch vụ',
      render: (row) => (
        <div className="flex flex-col gap-0.5">
          <span className="font-bold text-sky-900">{row.planName}</span>
          <span className="text-xs text-slate-500">
            {row.planCode} · {getBillingCycleLabel(row.billingCycle)}
          </span>
        </div>
      ),
    },
    {
      key: 'status',
      header: 'Trạng thái',
      render: (row) => (
        <StatusBadge
          label={getTenantSubscriptionStatusLabel(row.status)}
          tone={TENANT_SUBSCRIPTION_STATUS_TONE[row.status] ?? 'neutral'}
        />
      ),
    },
    {
      key: 'startedAt',
      header: 'Bắt đầu',
      render: (row) => formatDate(row.startedAt),
    },
    {
      key: 'expiresAt',
      header: 'Hết hạn',
      render: (row) => formatDate(row.expiresAt),
    },
    {
      key: 'createdAt',
      header: 'Ngày đăng ký',
      render: (row) => formatDateTime(row.createdAt),
    },
    {
      key: 'actions',
      header: '',
      align: 'right',
      width: '220px',
      render: (row) => (
        <div className="flex justify-end gap-2">
          <Button size="sm" variant="secondary" onClick={() => onChangeStatus(row)}>
            Đổi trạng thái
          </Button>
          <button
            type="button"
            onClick={() => onDelete(row)}
            className="grid h-8 w-8 place-items-center rounded-md text-slate-400 hover:bg-red-50 hover:text-red-600"
            aria-label="Xóa gói dịch vụ"
            title="Xóa gói dịch vụ"
          >
            <TrashIcon className="h-4 w-4" />
          </button>
        </div>
      ),
    },
  ];

  return (
    <Table
      columns={columns}
      data={data}
      rowKey={(row) => row.id}
      isLoading={isLoading}
      emptyMessage="Tenant này chưa đăng ký gói dịch vụ nào"
    />
  );
}