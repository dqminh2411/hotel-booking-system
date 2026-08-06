import { Table } from '@/shared/components/Table/Table';
import { Button } from '@/shared/components/Button/Button';
import { StatusBadge } from '@/shared/components/StatusBadge/StatusBadge';
import { formatDateTime } from '@/shared/utils/date';
import { getTenantStatusLabel, TENANT_STATUS_TONE } from '@/features/admin/constants/tenantOptions';
import { TrashIcon } from '@/shared/components/AdminLayout/AdminIcons';

/**
 * Bang danh sach tenant (GET /api/admin/tenants).
 * Chi hien dung field co trong TenantResponse: tenantId, name, status, ownerId,
 * ownerEmail, ownerFullname, ownerPhone, createdAt.
 *
 * Props: data, isLoading, onEdit(row), onManageSubscriptions(row), onDelete(row)
 */
export function TenantTable({ data, isLoading, onEdit, onManageSubscriptions, onDelete }) {
  const columns = [
    {
      key: 'tenant',
      header: 'Tenant',
      render: (row) => (
        <div className="flex flex-col gap-0.5">
          <span className="font-bold text-sky-900">{row.name}</span>
          <span className="font-mono text-xs text-slate-400">{row.tenantId}</span>
        </div>
      ),
    },
    {
      key: 'owner',
      header: 'Chủ sở hữu',
      render: (row) => (
        <div className="flex flex-col gap-0.5">
          <span className="text-sm font-medium text-slate-700">{row.ownerFullname || '-'}</span>
          <span className="text-xs text-slate-500">{row.ownerEmail}</span>
          {row.ownerPhone && <span className="text-xs text-slate-400">{row.ownerPhone}</span>}
        </div>
      ),
    },
    {
      key: 'status',
      header: 'Trạng thái',
      render: (row) => (
        <StatusBadge label={getTenantStatusLabel(row.status)} tone={TENANT_STATUS_TONE[row.status] ?? 'neutral'} />
      ),
    },
    {
      key: 'createdAt',
      header: 'Ngày tạo',
      render: (row) => formatDateTime(row.createdAt),
    },
    {
      key: 'actions',
      header: '',
      align: 'right',
      width: '300px',
      render: (row) => (
        <div className="flex justify-end gap-2">
          <Button size="sm" variant="secondary" onClick={() => onEdit(row)}>
            Sửa
          </Button>
          <Button size="sm" variant="primary" onClick={() => onManageSubscriptions(row)}>
            Quản lý gói
          </Button>
          <button
            type="button"
            onClick={() => onDelete(row)}
            className="grid h-8 w-8 place-items-center rounded-md text-slate-400 hover:bg-red-50 hover:text-red-600"
            aria-label="Xóa tenant"
            title="Xóa tenant"
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
      rowKey={(row) => row.tenantId}
      isLoading={isLoading}
      emptyMessage="Không tìm thấy tenant nào"
    />
  );
}