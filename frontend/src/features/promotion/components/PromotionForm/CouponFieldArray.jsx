import styles from './PromotionForm.module.scss';
import { Select } from '@/shared/components/Select/Select';
import { FormInput } from '@/shared/components/FormInput/FormInput';
import { Button } from '@/shared/components/Button/Button';
import { COUPON_STATUS_OPTIONS } from '@/features/promotion/constants/promotionOptions';
import { createEmptyCouponRow } from './promotionFormModel';

/**
 * Danh sach Coupon - cho phep them nhieu dong.
 * Khi Edit, backend se tu dong sync theo "code" (business key):
 * code trung -> update, code moi -> insert, code bi bo -> soft delete.
 * FE chi can gui dung danh sach coupon hien tai, khong tu xu ly merge.
 */
export function CouponFieldArray({ rows, errors, onChange }) {
  const updateRow = (rowId, patch) => {
    onChange(rows.map((r) => (r.rowId === rowId ? { ...r, ...patch } : r)));
  };

  const removeRow = (rowId) => {
    onChange(rows.filter((r) => r.rowId !== rowId));
  };

  return (
    <div>
      {rows.length === 0 ? (
        <div className={styles.emptyRows}>Chua co coupon nao. Nhan &quot;Them coupon&quot; neu can.</div>
      ) : (
        <div className={styles.rowList}>
          {rows.map((row) => (
            <div className={[styles.row, styles.rowCoupon].join(' ')} key={row.rowId}>
              <FormInput
                label="Coupon code"
                placeholder="VD: SUMMER2026"
                value={row.code}
                error={errors?.[row.rowId]?.code}
                onChange={(e) => updateRow(row.rowId, { code: e.target.value.toUpperCase() })}
              />
              <Select
                label="Trang thai"
                options={COUPON_STATUS_OPTIONS}
                value={row.status}
                onChange={(e) => updateRow(row.rowId, { status: e.target.value })}
              />
              <FormInput
                label="Gioi han su dung"
                type="number"
                min={1}
                placeholder="Khong gioi han"
                value={row.usageLimit ?? ''}
                error={errors?.[row.rowId]?.usageLimit}
                onChange={(e) =>
                  updateRow(row.rowId, {
                    usageLimit: e.target.value === '' ? undefined : Number(e.target.value),
                  })
                }
              />
              <Button
                type="button"
                variant="danger"
                size="sm"
                className={styles.removeBtn}
                onClick={() => removeRow(row.rowId)}
              >
                Xoa
              </Button>
            </div>
          ))}
        </div>
      )}

      <Button
        type="button"
        variant="secondary"
        size="sm"
        className={styles.addBtn}
        onClick={() => onChange([...rows, createEmptyCouponRow()])}
      >
        + Them coupon
      </Button>
    </div>
  );
}
