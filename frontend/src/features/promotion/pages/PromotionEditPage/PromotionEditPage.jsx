import { useNavigate, useParams } from 'react-router-dom';
import { PageHeader } from '@/shared/components/PageHeader/PageHeader';
import { Loading } from '@/shared/components/Loading/Loading';
import { EmptyState } from '@/shared/components/EmptyState/EmptyState';
import { PromotionForm } from '@/features/promotion/components/PromotionForm/PromotionForm';
import { fromResponseToFormValues, toRequestPayload } from '@/features/promotion/components/PromotionForm/promotionFormModel';
import { usePromotion } from '@/features/promotion/hooks/usePromotion';
import { usePromotionMutations } from '@/features/promotion/hooks/usePromotionMutations';
import { ROUTES } from '@/shared/constants/routes';

/**
 * Trang chinh sua Promotion - nap du lieu qua GET /api/promotions/{id},
 * submit qua PUT /api/promotions/{id}.
 * Scope: Replace, Condition: Replace, Coupon: Sync - tat ca do Backend xu ly,
 * FE chi gui dung danh sach hien tai trong form.
 */
export function PromotionEditPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { data, isLoading, error } = usePromotion(id);
  const { isSubmitting, error: submitError, update } = usePromotionMutations();

  const handleSubmit = async (values) => {
    if (!id) return;
    const payload = toRequestPayload(values);
    const updated = await update(id, payload);
    if (updated) {
      navigate(ROUTES.promotions.detail(updated.id));
    }
  };

  return (
    <div>
      <PageHeader title="Chỉnh sửa Promotion" subtitle={data ? data.name : undefined} />

      {isLoading && <Loading />}

      {!isLoading && error && (
        <EmptyState message={`${error.status ? `Lỗi ${error.status}: ` : ''}${error.message}`} />
      )}

      {!isLoading && data && (
        <PromotionForm
          initialValues={fromResponseToFormValues(data)}
          isSubmitting={isSubmitting}
          submitError={submitError}
          submitLabel="Lưu thay đổi"
          onSubmit={handleSubmit}
          onCancel={() => navigate(ROUTES.promotions.detail(id ?? ''))}
        />
      )}
    </div>
  );
}
