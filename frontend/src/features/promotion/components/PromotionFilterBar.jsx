import styles from './PromotionFilterBar.module.scss';
import { FormInput } from '@/shared/components/FormInput/FormInput';
import { Select } from '@/shared/components/Select/Select';
import { PROMOTION_STATUS_OPTIONS } from '@/features/promotion/constants/promotionOptions';

/**
 * Thanh tim kiem + loc trang thai cho Promotion List.
 * Props: keyword, onKeywordChange, status, onStatusChange
 */
export function PromotionFilterBar({
  keyword,
  onKeywordChange,
  status,
  onStatusChange,
}) {
  return (
    <div className={styles.bar}>
      <div className={styles.searchField}>
        <FormInput
          label="Tìm kiếm"
          placeholder="Tìm theo tên promotion..."
          value={keyword}
          onChange={(e) => onKeywordChange(e.target.value)}
        />
      </div>
      <div className={styles.statusField}>
        <Select
          label="Trạng thái"
          placeholder="Tất cả trạng thái"
          options={PROMOTION_STATUS_OPTIONS}
          value={status}
          onChange={(e) => onStatusChange(e.target.value)}
        />
      </div>
    </div>
  );
}
