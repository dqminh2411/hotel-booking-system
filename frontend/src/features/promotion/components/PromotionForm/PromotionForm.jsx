import { useState } from 'react';
import styles from './PromotionForm.module.scss';
import { FormInput } from '@/shared/components/FormInput/FormInput';
import { Textarea } from '@/shared/components/Textarea/Textarea';
import { Select } from '@/shared/components/Select/Select';
import { Button } from '@/shared/components/Button/Button';
import {
  DISCOUNT_TYPE_OPTIONS,
  PROMOTION_STATUS_OPTIONS,
  PROMOTION_TYPE_OPTIONS,
} from '@/features/promotion/constants/promotionOptions';
import { ConditionFieldArray } from './ConditionFieldArray';
import { CouponFieldArray } from './CouponFieldArray';
import { validatePromotionForm, hasFormErrors } from './validatePromotionForm';

/**
 * Form dung chung cho Create va Edit Promotion.
 * FE chi validate co ban (required/number/date). Business rule (vi du
 * startAt < endAt, percentage <= 100, coupon code trung...) do Backend
 * xu ly - loi 400 tu server se duoc hien thi o cardError ben duoi.
 *
 * Props: initialValues, isSubmitting, submitError, submitLabel, onSubmit, onCancel
 */
export function PromotionForm({
  initialValues,
  isSubmitting,
  submitError,
  submitLabel,
  onSubmit,
  onCancel,
}) {
  const [values, setValues] = useState(initialValues);
  const [errors, setErrors] = useState({});

  const setField = (key, value) => {
    setValues((prev) => ({ ...prev, [key]: value }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const nextErrors = validatePromotionForm(values);
    setErrors(nextErrors);
    if (hasFormErrors(nextErrors)) return;
    onSubmit(values);
  };

  return (
    <form onSubmit={handleSubmit} noValidate>
      {submitError && (
        <div className={styles.formError}>
          {submitError.status ? `Loi ${submitError.status}: ` : ''}
          {submitError.message}
        </div>
      )}

      <section className={styles.card}>
        <h3 className={styles.cardTitle}>Thong tin Promotion</h3>
        <p className={styles.cardDesc}>Thong tin co ban cua chuong trinh khuyen mai.</p>

        <div className={styles.grid2}>
          <div className={styles.fullSpan}>
            <FormInput
              label="Ten promotion"
              required
              value={values.name}
              error={errors.name}
              maxLength={150}
              onChange={(e) => setField('name', e.target.value)}
            />
          </div>

          <div className={styles.fullSpan}>
            <Textarea
              label="Mo ta"
              value={values.description}
              error={errors.description}
              maxLength={2000}
              onChange={(e) => setField('description', e.target.value)}
            />
          </div>

          <Select
            label="Loai promotion"
            required
            options={PROMOTION_TYPE_OPTIONS}
            value={values.type}
            onChange={(e) => setField('type', e.target.value)}
          />

          <Select
            label="Trang thai"
            required
            options={PROMOTION_STATUS_OPTIONS}
            value={values.status}
            onChange={(e) => setField('status', e.target.value)}
          />

          <FormInput
            label="Ngay bat dau"
            type="date"
            required
            value={values.startAt}
            error={errors.startAt}
            onChange={(e) => setField('startAt', e.target.value)}
          />

          <FormInput
            label="Ngay ket thuc"
            type="date"
            required
            value={values.endAt}
            error={errors.endAt}
            onChange={(e) => setField('endAt', e.target.value)}
          />
        </div>
      </section>

      <section className={styles.card}>
        <h3 className={styles.cardTitle}>Giam gia &amp; Gioi han</h3>
        <p className={styles.cardDesc}>Cach tinh giam gia va cac gioi han su dung.</p>

        <div className={styles.grid3}>
          <Select
            label="Loai giam gia"
            required
            options={DISCOUNT_TYPE_OPTIONS}
            value={values.discountType}
            onChange={(e) => setField('discountType', e.target.value)}
          />
          <FormInput
            label={values.discountType === 'PERCENTAGE' ? 'Gia tri giam (%)' : 'Gia tri giam'}
            type="number"
            required
            min={0}
            step="0.01"
            value={values.discountValue}
            error={errors.discountValue}
            onChange={(e) => setField('discountValue', e.target.value)}
          />
          <FormInput
            label="Giam toi da"
            type="number"
            min={0}
            step="0.01"
            value={values.maxDiscountAmount}
            error={errors.maxDiscountAmount}
            onChange={(e) => setField('maxDiscountAmount', e.target.value)}
          />
          <FormInput
            label="Gia tri booking toi thieu"
            type="number"
            min={0}
            step="0.01"
            value={values.minBookingAmount}
            error={errors.minBookingAmount}
            onChange={(e) => setField('minBookingAmount', e.target.value)}
          />
          <FormInput
            label="So dem toi thieu"
            type="number"
            min={1}
            value={values.minNights}
            error={errors.minNights}
            onChange={(e) => setField('minNights', e.target.value)}
          />
          <FormInput
            label="Tong luot su dung toi da"
            type="number"
            min={1}
            value={values.totalUsageLimit}
            error={errors.totalUsageLimit}
            onChange={(e) => setField('totalUsageLimit', e.target.value)}
          />
          <FormInput
            label="Luot su dung / user"
            type="number"
            min={1}
            value={values.perUserUsageLimit}
            error={errors.perUserUsageLimit}
            onChange={(e) => setField('perUserUsageLimit', e.target.value)}
          />
          <label className={styles.checkboxRow}>
            <input
              type="checkbox"
              className={styles.checkbox}
              checked={values.stackable}
              onChange={(e) => setField('stackable', e.target.checked)}
            />
            Cho phep cong don (stackable)
          </label>
        </div>
      </section>

      <section className={styles.card}>
        <h3 className={styles.cardTitle}>Pham vi ap dung (Scope)</h3>
        <p className={styles.cardDesc}>Sprint hien tai chi ho tro pham vi He thong.</p>
        <div className={styles.scopeInfo}>
          Promotion nay ap dung o pham vi <strong>&nbsp;He thong (SYSTEM)&nbsp;</strong> - cac pham vi
          Hotel / Room Type / User Segment se duoc ho tro o sprint sau.
        </div>
      </section>

      <section className={styles.card}>
        <h3 className={styles.cardTitle}>Dieu kien (Condition)</h3>
        <p className={styles.cardDesc}>Tuy chon - co the them nhieu dieu kien ap dung promotion.</p>
        <ConditionFieldArray
          rows={values.conditions}
          errors={errors.conditions}
          onChange={(rows) => setField('conditions', rows)}
        />
      </section>

      <section className={styles.card}>
        <h3 className={styles.cardTitle}>Coupon</h3>
        <p className={styles.cardDesc}>Tuy chon - co the them nhieu coupon cho promotion nay.</p>
        <CouponFieldArray
          rows={values.coupons}
          errors={errors.coupons}
          onChange={(rows) => setField('coupons', rows)}
        />
      </section>

      <div className={styles.formActions}>
        <Button type="button" variant="secondary" onClick={onCancel} disabled={isSubmitting}>
          Huy
        </Button>
        <Button type="submit" variant="primary" isLoading={isSubmitting}>
          {submitLabel}
        </Button>
      </div>
    </form>
  );
}
