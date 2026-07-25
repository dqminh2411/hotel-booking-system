import styles from './PromotionTable.module.scss';
import { Table } from '@/shared/components/Table/Table';
import { Button } from '@/shared/components/Button/Button';
import { StatusBadge } from '@/shared/components/StatusBadge/StatusBadge';
import { formatDate } from '@/shared/utils/date';
import { formatNumber } from '@/shared/utils/format';
import {
  DISCOUNT_TYPE_LABEL,
  PROMOTION_STATUS_LABEL,
  PROMOTION_STATUS_TONE,
  PROMOTION_TYPE_LABEL,
} from '@/features/promotion/constants/promotionOptions';

/**
 * Bang danh sach Promotion.
 * Luu y: API GET /api/promotions chi tra ve mot so field co ban cho moi
 * dong (xem PromotionServiceImpl#toPromotionResponse), nen bang nay CHI
 * hien thi: Name, Type, Discount, Status, Start Date, End Date - dung
 * nhung gi backend dam bao co trong response danh sach.
 *
 * Props: data, isLoading, onView, onEdit, onDelete, onChangeStatus
 */
export function PromotionTable({ data, isLoading, onView, onEdit, onDelete, onChangeStatus }) {
  const columns = [
    {
      key: 'name',
      header: 'Tên',
      render: (row) => (
        <button type="button" className={styles.name} onClick={() => onView(row.id)}>
          {row.name}
        </button>
      ),
    },
    {
      key: 'type',
      header: 'Loại',
      render: (row) => PROMOTION_TYPE_LABEL[row.type],
    },
    {
      key: 'discount',
      header: 'Giảm giá',
      render: (row) =>
        row.discountType === 'PERCENTAGE'
          ? `${formatNumber(row.discountValue)}% (${DISCOUNT_TYPE_LABEL[row.discountType]})`
          : `${formatNumber(row.discountValue)} (${DISCOUNT_TYPE_LABEL[row.discountType]})`,
    },
    {
      key: 'status',
      header: 'Trạng thái',
      render: (row) => <StatusBadge label={PROMOTION_STATUS_LABEL[row.status]} tone={PROMOTION_STATUS_TONE[row.status]} />,
    },
    {
      key: 'startAt',
      header: 'Ngày bắt đầu',
      render: (row) => formatDate(row.startAt),
    },
    {
      key: 'endAt',
      header: 'Ngày kết thúc',
      render: (row) => formatDate(row.endAt),
    },
    {
      key: 'actions',
      header: '',
      align: 'right',
      width: '320px',
      render: (row) => (
        <div className={styles.actions}>
          <Button size="sm" variant="secondary" onClick={() => onView(row.id)}>
            Xem
          </Button>
          <Button size="sm" variant="secondary" onClick={() => onEdit(row.id)}>
            Sửa
          </Button>
          <Button size="sm" variant="ghost" onClick={() => onChangeStatus(row)}>
            Đổi trạng thái
          </Button>
          <Button size="sm" variant="danger" onClick={() => onDelete(row)}>
            Xóa
          </Button>
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
      emptyMessage="Chưa có chương trình khuyến mãi nào"
    />
  );
}
