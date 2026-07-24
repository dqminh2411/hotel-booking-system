import styles from './PromotionForm.module.scss';
import { Select } from '@/shared/components/Select/Select';
import { FormInput } from '@/shared/components/FormInput/FormInput';
import { Button } from '@/shared/components/Button/Button';
import {
  CONDITION_OPERATOR_OPTIONS,
  CONDITION_TYPE_OPTIONS,
} from '@/features/promotion/constants/promotionOptions';
import { createEmptyConditionRow } from './promotionFormModel';

/** Danh sach dieu kien (Condition) - cho phep them nhieu dong, gui len backend duoi dang array. */
export function ConditionFieldArray({ rows, errors, onChange }) {
  const updateRow = (rowId, patch) => {
    onChange(rows.map((r) => (r.rowId === rowId ? { ...r, ...patch } : r)));
  };

  const removeRow = (rowId) => {
    onChange(rows.filter((r) => r.rowId !== rowId));
  };

  return (
    <div>
      {rows.length === 0 ? (
        <div className={styles.emptyRows}>Chua co dieu kien nao. Nhan &quot;Them dieu kien&quot; neu can.</div>
      ) : (
        <div className={styles.rowList}>
          {rows.map((row) => (
            <div className={styles.row} key={row.rowId}>
              <Select
                label="Loai dieu kien"
                options={CONDITION_TYPE_OPTIONS}
                value={row.conditionType}
                onChange={(e) => updateRow(row.rowId, { conditionType: e.target.value })}
              />
              <Select
                label="Toan tu"
                options={CONDITION_OPERATOR_OPTIONS}
                value={row.operator}
                onChange={(e) => updateRow(row.rowId, { operator: e.target.value })}
              />
              <FormInput
                label="Gia tri"
                placeholder="Vi du: 3"
                value={row.conditionValue}
                error={errors?.[row.rowId]?.conditionValue}
                onChange={(e) => updateRow(row.rowId, { conditionValue: e.target.value })}
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
        onClick={() => onChange([...rows, createEmptyConditionRow()])}
      >
        + Them dieu kien
      </Button>
    </div>
  );
}
