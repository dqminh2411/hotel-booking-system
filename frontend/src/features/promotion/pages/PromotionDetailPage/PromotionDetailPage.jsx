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
        ‹ Quay lại danh sách
      </Link>

      <PageHeader
        title="Chi tiết Promotion"
        actions={
          data && (
            <>
              <Button variant="secondary" onClick={() => navigate(ROUTES.promotions.edit(data.id))}>
                Sửa
              </Button>
              <Button variant="ghost" onClick={handleOpenStatusModal}>
                Đổi trạng thái
              </Button>
              <Button variant="danger" onClick={deleteDialog.open}>
                Xóa
              </Button>
            </>
          )
        }
      />

      {isLoading && <Loading />}

      {!isLoading && error && (
        <EmptyState message={`${error.status ? `Lỗi ${error.status}: ` : ''}${error.message}`} />
      )}

      {!isLoading && data && <PromotionDetailView promotion={data} />}

      <ConfirmDialog
        isOpen={deleteDialog.isOpen}
        title="Xóa promotion"
        message={`Bạn có chắc muốn xóa promotion "${data?.name}"? Hành động này sẽ xóa mềm (soft delete).`}
        confirmLabel="Xóa"
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
