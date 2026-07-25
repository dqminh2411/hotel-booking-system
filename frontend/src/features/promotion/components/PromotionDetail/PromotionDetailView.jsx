import styles from './PromotionDetailView.module.scss';
import { StatusBadge } from '@/shared/components/StatusBadge/StatusBadge';
import { EmptyState } from '@/shared/components/EmptyState/EmptyState';
import { formatDateTime, formatDate } from '@/shared/utils/date';
import { formatNumber } from '@/shared/utils/format';
import {
  CONDITION_OPERATOR_LABEL,
  CONDITION_TYPE_LABEL,
  COUPON_STATUS_TONE,
  DISCOUNT_TYPE_LABEL,
  PROMOTION_STATUS_LABEL,
  PROMOTION_STATUS_TONE,
  PROMOTION_TYPE_LABEL,
  SCOPE_TYPE_LABEL,
} from '@/features/promotion/constants/promotionOptions';

/**
 * Hien thi chi tiet Promotion theo dung thu tu: Promotion -> Scopes -> Conditions -> Coupons.
 * Props: promotion (PromotionResponse - xem promotion.types.js)
 */
export function PromotionDetailView({ promotion }) {
  return (
    <div>
      <div className={[styles.card, styles.headerCard].join(' ')}>
        <div>
          <h2 className={styles.name}>{promotion.name}</h2>
          {promotion.description && <p className={styles.description}>{promotion.description}</p>}
        </div>
        <StatusBadge
          label={PROMOTION_STATUS_LABEL[promotion.status]}
          tone={PROMOTION_STATUS_TONE[promotion.status]}
        />
      </div>

      <div className={styles.card}>
        <h3 className={styles.cardTitle}>Thông tin Promotion</h3>
        <div className={styles.metaGrid}>
          <Meta label="Loại" value={PROMOTION_TYPE_LABEL[promotion.type]} />
          <Meta label="Loại giảm giá" value={DISCOUNT_TYPE_LABEL[promotion.discountType]} />
          <Meta
            label="Giá trị giảm"
            value={
              promotion.discountType === 'PERCENTAGE'
                ? `${formatNumber(promotion.discountValue)}%`
                : formatNumber(promotion.discountValue)
            }
          />
          <Meta label="Giảm tối đa" value={formatNumber(promotion.maxDiscountAmount)} />
          <Meta label="Booking tối thiểu" value={formatNumber(promotion.minBookingAmount)} />
          <Meta label="Số đêm tối thiểu" value={formatNumber(promotion.minNights)} />
          <Meta label="Ngày bắt đầu" value={formatDate(promotion.startAt)} />
          <Meta label="Ngày kết thúc" value={formatDate(promotion.endAt)} />
          <Meta label="Tổng lượt sử dụng tối đa" value={formatNumber(promotion.totalUsageLimit)} />
          <Meta label="Lượt sử dụng / người dùng" value={formatNumber(promotion.perUserUsageLimit)} />
          <Meta label="Đã sử dụng" value={formatNumber(promotion.currentUsageCount)} />
          <Meta label="Cộng dồn (stackable)" value={promotion.stackable ? 'Có' : 'Không'} />
          <Meta label="Ngày tạo" value={formatDateTime(promotion.createdAt)} />
          <Meta label="Cập nhật gần nhất" value={formatDateTime(promotion.updatedAt)} />
        </div>
      </div>

      <div className={styles.card}>
        <h3 className={styles.cardTitle}>Phạm vi áp dụng (Scopes)</h3>
        {!promotion.scopes || promotion.scopes.length === 0 ? (
          <EmptyState message="Chưa có scope nào" />
        ) : (
          <div className={styles.list}>
            {promotion.scopes.map((scope, idx) => (
              <div className={styles.listRow} key={scope.id ?? idx}>
                <div className={styles.listRowMain}>
                  <span className={styles.listRowTitle}>{SCOPE_TYPE_LABEL[scope.scopeType]}</span>
                  {scope.scopeRefId && <span className={styles.listRowSub}>Ref: {scope.scopeRefId}</span>}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className={styles.card}>
        <h3 className={styles.cardTitle}>Điều kiện (Conditions)</h3>
        {!promotion.conditions || promotion.conditions.length === 0 ? (
          <EmptyState message="Chưa có điều kiện nào" />
        ) : (
          <div className={styles.list}>
            {promotion.conditions.map((cond, idx) => (
              <div className={styles.listRow} key={cond.id ?? idx}>
                <div className={styles.listRowMain}>
                  <span className={styles.listRowTitle}>{CONDITION_TYPE_LABEL[cond.conditionType]}</span>
                  <span className={styles.listRowSub}>
                    {CONDITION_OPERATOR_LABEL[cond.operator]} {cond.conditionValue ?? '-'}
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className={styles.card}>
        <h3 className={styles.cardTitle}>Mã giảm giá (Coupon)</h3>
        {!promotion.coupons || promotion.coupons.length === 0 ? (
          <EmptyState message="Chưa có coupon nào" />
        ) : (
          <div className={styles.list}>
            {promotion.coupons.map((coupon, idx) => (
              <div className={styles.listRow} key={coupon.id ?? idx}>
                <div className={styles.listRowMain}>
                  <span className={styles.listRowTitle}>{coupon.code}</span>
                  <span className={styles.listRowSub}>
                    Đã dùng: {formatNumber(coupon.currentUsageCount)} / {formatNumber(coupon.usageLimit) || 'Không giới hạn'}
                  </span>
                </div>
                <StatusBadge label={coupon.status} tone={COUPON_STATUS_TONE[coupon.status]} />
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function Meta({ label, value }) {
  return (
    <div className={styles.metaItem}>
      <span className={styles.metaLabel}>{label}</span>
      <span className={styles.metaValue}>{value}</span>
    </div>
  );
}
