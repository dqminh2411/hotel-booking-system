import { Table } from '@/shared/components/Table/Table';
import { Button } from '@/shared/components/Button/Button';
import { StatusBadge } from '@/shared/components/StatusBadge/StatusBadge';
import { formatDateTime } from '@/shared/utils/date';
import { formatAddress } from '@/features/hotel/utils/hotelFormatters';
import { getHotelStatusLabel, HOTEL_STATUS_TONE } from '@/features/admin/constants/adminOptions';

const PLACEHOLDER_IMAGE =
  'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=200&h=140&fit=crop';

/**
 * Bang danh sach khach san PENDING (GET /api/admin/hotels/pending).
 * Chi hien thi dung nhung field co trong HotelPendingResponse: hotelId, name,
 * tenantId, address, coverImgUrl, status, createdAt.
 *
 * Props: data, isLoading, onApprove(row), onSuspend(row), onManageImages(row)
 */
export function HotelPendingTable({ data, isLoading, onApprove, onSuspend, onManageImages }) {
  const columns = [
    {
      key: 'cover',
      header: '',
      width: '84px',
      render: (row) => (
        <img
          src={row.coverImgUrl || PLACEHOLDER_IMAGE}
          alt={row.name}
          className="h-12 w-16 rounded-md object-cover bg-slate-100"
          onError={(e) => {
            e.currentTarget.src = PLACEHOLDER_IMAGE;
          }}
        />
      ),
    },
    {
      key: 'name',
      header: 'Khách sạn',
      render: (row) => (
        <div className="flex flex-col gap-0.5">
          <span className="font-bold text-sky-900">{row.name}</span>
          <span className="text-xs text-slate-500">
            {formatAddress(row.address) || 'Chưa cập nhật địa chỉ'}
          </span>
        </div>
      ),
    },
    {
      key: 'tenantId',
      header: 'Tenant ID',
      render: (row) => <span className="font-mono text-xs text-slate-500">{row.tenantId}</span>,
    },
    {
      key: 'status',
      header: 'Trạng thái',
      render: (row) => (
        <StatusBadge label={getHotelStatusLabel(row.status)} tone={HOTEL_STATUS_TONE[row.status] ?? 'neutral'} />
      ),
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
      width: '360px',
      render: (row) => (
        <div className="flex flex-wrap justify-end gap-2">
          <Button size="sm" variant="secondary" onClick={() => onManageImages(row)}>
            Quản lý ảnh
          </Button>
          <Button size="sm" variant="danger" onClick={() => onSuspend(row)}>
            Từ chối
          </Button>
          <Button size="sm" variant="primary" onClick={() => onApprove(row)}>
            Duyệt
          </Button>
        </div>
      ),
    },
  ];

  return (
    <Table
      columns={columns}
      data={data}
      rowKey={(row) => row.hotelId}
      isLoading={isLoading}
      emptyMessage="Không có khách sạn nào đang chờ duyệt"
    />
  );
}