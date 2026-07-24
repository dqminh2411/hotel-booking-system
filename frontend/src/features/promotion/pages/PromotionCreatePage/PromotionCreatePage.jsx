import { useNavigate } from 'react-router-dom';
import { PageHeader } from '@/shared/components/PageHeader/PageHeader';
import { PromotionForm } from '@/features/promotion/components/PromotionForm/PromotionForm';
import { createEmptyFormValues, toRequestPayload } from '@/features/promotion/components/PromotionForm/promotionFormModel';
import { usePromotionMutations } from '@/features/promotion/hooks/usePromotionMutations';
import { ROUTES } from '@/shared/constants/routes';

/** Trang tao moi Promotion - tai su dung PromotionForm dung chung voi trang Edit. */
export function PromotionCreatePage() {
  const navigate = useNavigate();
  const { isSubmitting, error, create } = usePromotionMutations();

  const handleSubmit = async (values) => {
    const payload = toRequestPayload(values);
    const created = await create(payload);
    if (created) {
      navigate(ROUTES.promotions.detail(created.id));
    }
  };

  return (
    <div>
      <PageHeader title="Tao Promotion" subtitle="Tao moi chuong trinh khuyen mai pham vi He thong (SYSTEM)." />
      <PromotionForm
        initialValues={createEmptyFormValues()}
        isSubmitting={isSubmitting}
        submitError={error}
        submitLabel="Tao promotion"
        onSubmit={handleSubmit}
        onCancel={() => navigate(ROUTES.promotions.list)}
      />
    </div>
  );
}
