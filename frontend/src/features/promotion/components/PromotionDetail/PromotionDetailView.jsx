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
        <h3 className={styles.cardTitle}>Thong tin Promotion</h3>
        <div className={styles.metaGrid}>
          <Meta label="Loai" value={PROMOTION_TYPE_LABEL[promotion.type]} />
          <Meta label="Loai giam gia" value={DISCOUNT_TYPE_LABEL[promotion.discountType]} />
          <Meta
            label="Gia tri giam"
            value={
              promotion.discountType === 'PERCENTAGE'
                ? `${formatNumber(promotion.discountValue)}%`
                : formatNumber(promotion.discountValue)
            }
          />
          <Meta label="Giam toi da" value={formatNumber(promotion.maxDiscountAmount)} />
          <Meta label="Booking toi thieu" value={formatNumber(promotion.minBookingAmount)} />
          <Meta label="So dem toi thieu" value={formatNumber(promotion.minNights)} />
          <Meta label="Ngay bat dau" value={formatDate(promotion.startAt)} />
          <Meta label="Ngay ket thuc" value={formatDate(promotion.endAt)} />
          <Meta label="Tong luot su dung toi da" value={formatNumber(promotion.totalUsageLimit)} />
          <Meta label="Luot su dung / user" value={formatNumber(promotion.perUserUsageLimit)} />
          <Meta label="Da su dung" value={formatNumber(promotion.currentUsageCount)} />
          <Meta label="Cong don (stackable)" value={promotion.stackable ? 'Co' : 'Khong'} />
          <Meta label="Ngay tao" value={formatDateTime(promotion.createdAt)} />
          <Meta label="Cap nhat gan nhat" value={formatDateTime(promotion.updatedAt)} />
        </div>
      </div>

      <div className={styles.card}>
        <h3 className={styles.cardTitle}>Pham vi ap dung (Scopes)</h3>
        {!promotion.scopes || promotion.scopes.length === 0 ? (
          <EmptyState message="Chua co scope nao" />
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
        <h3 className={styles.cardTitle}>Dieu kien (Conditions)</h3>
        {!promotion.conditions || promotion.conditions.length === 0 ? (
          <EmptyState message="Chua co dieu kien nao" />
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
        <h3 className={styles.cardTitle}>Coupon</h3>
        {!promotion.coupons || promotion.coupons.length === 0 ? (
          <EmptyState message="Chua co coupon nao" />
        ) : (
          <div className={styles.list}>
            {promotion.coupons.map((coupon, idx) => (
              <div className={styles.listRow} key={coupon.id ?? idx}>
                <div className={styles.listRowMain}>
                  <span className={styles.listRowTitle}>{coupon.code}</span>
                  <span className={styles.listRowSub}>
                    Da dung: {formatNumber(coupon.currentUsageCount)} / {formatNumber(coupon.usageLimit) || 'Khong gioi han'}
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
