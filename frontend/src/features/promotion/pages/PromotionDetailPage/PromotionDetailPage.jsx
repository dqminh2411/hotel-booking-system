import { Link, useNavigate, useParams } from 'react-router-dom';
import styles from './PromotionDetailPage.module.scss';
import { PageHeader } from '@/shared/components/PageHeader/PageHeader';
import { Button } from '@/shared/components/Button/Button';
import { Loading } from '@/shared/components/Loading/Loading';
import { EmptyState } from '@/shared/components/EmptyState/EmptyState';
import { ConfirmDialog } from '@/shared/components/ConfirmDialog/ConfirmDialog';
import { useDisclosure } from '@/shared/hooks/useDisclosure';
import { usePromotion } from '@/features/promotion/hooks/usePromotion';
import { usePromotionMutations } from '@/features/promotion/hooks/usePromotionMutations';
import { PromotionDetailView } from '@/features/promotion/components/PromotionDetail/PromotionDetailView';
import { ChangeStatusModal } from '@/features/promotion/components/ChangeStatusModal';
import { ROUTES } from '@/shared/constants/routes';

/** Trang chi tiet Promotion: Promotion -> Scopes -> Conditions -> Coupons. */
export function PromotionDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { data, isLoading, error, reload } = usePromotion(id);
  const mutations = usePromotionMutations();

  const deleteDialog = useDisclosure();
  const statusModal = useDisclosure();

  const handleConfirmDelete = async () => {
    if (!id) return;
    const ok = await mutations.remove(id);
    if (ok) {
      navigate(ROUTES.promotions.list);
    }
  };

  const handleOpenStatusModal = () => {
    mutations.clearError();
    statusModal.open();
  };

  const handleSubmitStatus = async (status, reason) => {
    if (!id) return;
    const result = await mutations.changeStatus(id, status, reason);
    if (result) {
      statusModal.close();
      reload();
    }
  };

  return (
    <div>
      <Link to={ROUTES.promotions.list} className={styles.backLink}>
        ‹ Quay lai danh sach
      </Link>

      <PageHeader
        title="Chi tiet Promotion"
        actions={
          data && (
            <>
              <Button variant="secondary" onClick={() => navigate(ROUTES.promotions.edit(data.id))}>
                Sua
              </Button>
              <Button variant="ghost" onClick={handleOpenStatusModal}>
                Doi trang thai
              </Button>
              <Button variant="danger" onClick={deleteDialog.open}>
                Xoa
              </Button>
            </>
          )
        }
      />

      {isLoading && <Loading />}

      {!isLoading && error && (
        <EmptyState message={`${error.status ? `Loi ${error.status}: ` : ''}${error.message}`} />
      )}

      {!isLoading && data && <PromotionDetailView promotion={data} />}

      <ConfirmDialog
        isOpen={deleteDialog.isOpen}
        title="Xoa promotion"
        message={`Ban co chac muon xoa promotion "${data?.name}"? Hanh dong nay se xoa mem (soft delete).`}
        confirmLabel="Xoa"
        danger
        isLoading={mutations.isSubmitting}
        onConfirm={handleConfirmDelete}
        onCancel={deleteDialog.close}
      />

      <ChangeStatusModal
        isOpen={statusModal.isOpen}
        currentStatus={data?.status}
        isSubmitting={mutations.isSubmitting}
        error={mutations.error}
        onClose={statusModal.close}
        onSubmit={handleSubmitStatus}
      />
    </div>
  );
}
