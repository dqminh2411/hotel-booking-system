import { Table } from '@/shared/components/Table/Table';
import { Button } from '@/shared/components/Button/Button';
import { StatusBadge } from '@/shared/components/StatusBadge/StatusBadge';
import { formatDateTime } from '@/shared/utils/date';
import { getUserStatusLabel, USER_STATUS_TONE } from '@/features/admin/constants/userOptions';
import { LockIcon, UnlockIcon, TrashIcon } from '@/shared/components/AdminLayout/AdminIcons';

/**
 * Bang danh sach nguoi dung (GET /api/admin/users).
 * Chi hien dung field co trong ResponseUser: userId, email, phone, fullname (chu "n"
 * thuong - LUU Y khac voi UserResponse.fullName o man hinh chi tiet/sua), avatarUrl,
 * status, createdAt.
 *
 * Props: data, isLoading, onEdit(row), onLock(row), onUnlock(row), onDelete(row)
 */
export function UserTable({ data, isLoading, onEdit, onLock, onUnlock, onDelete }) {
  const columns = [
    {
      key: 'user',
      header: 'Người dùng',
      render: (row) => (
        <div className="flex items-center gap-3">
          {row.avatarUrl ? (
            <img
              src={row.avatarUrl}
              alt={row.fullname}
              className="h-10 w-10 rounded-full object-cover bg-slate-100"
              onError={(e) => {
                e.currentTarget.style.display = 'none';
              }}
            />
          ) : (
            <span className="grid h-10 w-10 place-items-center rounded-full bg-sky-100 text-sm font-semibold text-sky-700">
              {(row.fullname || row.email || '?').trim().charAt(0).toUpperCase()}
            </span>
          )}
          <div className="flex flex-col gap-0.5">
            <span className="font-bold text-sky-900">{row.fullname || '(Chưa cập nhật tên)'}</span>
            <span className="text-xs text-slate-500">{row.email}</span>
          </div>
        </div>
      ),
    },
    {
      key: 'phone',
      header: 'Số điện thoại',
      render: (row) => <span className="text-sm text-slate-600">{row.phone || '-'}</span>,
    },
    {
      key: 'status',
      header: 'Trạng thái',
      render: (row) => (
        <StatusBadge label={getUserStatusLabel(row.status)} tone={USER_STATUS_TONE[row.status] ?? 'neutral'} />
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
      width: '260px',
      render: (row) => (
        <div className="flex justify-end gap-2">
          <Button size="sm" variant="secondary" onClick={() => onEdit(row)}>
            Sửa
          </Button>
          {row.status === 'LOCKED' ? (
            <Button size="sm" variant="primary" leftIcon={<UnlockIcon className="h-4 w-4" />} onClick={() => onUnlock(row)}>
              Mở khóa
            </Button>
          ) : (
            <Button size="sm" variant="danger" leftIcon={<LockIcon className="h-4 w-4" />} onClick={() => onLock(row)}>
              Khóa
            </Button>
          )}
          <button
            type="button"
            onClick={() => onDelete(row)}
            className="grid h-8 w-8 place-items-center rounded-md text-slate-400 hover:bg-red-50 hover:text-red-600"
            aria-label="Xóa người dùng"
            title="Xóa người dùng"
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
      rowKey={(row) => row.userId}
      isLoading={isLoading}
      emptyMessage="Không tìm thấy người dùng nào"
    />
  );
}