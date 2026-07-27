import { useNavigate } from 'react-router-dom';
import { useState } from 'react';
import styles from './PromotionListPage.module.scss';
import { PageHeader } from '@/shared/components/PageHeader/PageHeader';
import { Button } from '@/shared/components/Button/Button';
import { Pagination } from '@/shared/components/Pagination/Pagination';
import { ConfirmDialog } from '@/shared/components/ConfirmDialog/ConfirmDialog';
import { useDisclosure } from '@/shared/hooks/useDisclosure';
import { usePromotions } from '@/features/promotion/hooks/usePromotions';
import { usePromotionMutations } from '@/features/promotion/hooks/usePromotionMutations';
import { PromotionFilterBar } from '@/features/promotion/components/PromotionFilterBar';
import { PromotionTable } from '@/features/promotion/components/PromotionTable';
import { ChangeStatusModal } from '@/features/promotion/components/ChangeStatusModal';
import { ROUTES } from '@/shared/constants/routes';

export function PromotionListPage() {
  const navigate = useNavigate();
  const list = usePromotions();
  const mutations = usePromotionMutations();

  const deleteDialog = useDisclosure();
  const statusModal = useDisclosure();
  const [selectedRow, setSelectedRow] = useState(null);

  const handleDeleteClick = (row) => {
    setSelectedRow(row);
    deleteDialog.open();
  };

  const handleConfirmDelete = async () => {
    if (!selectedRow) return;
    const ok = await mutations.remove(selectedRow.id);
    if (ok) {
      deleteDialog.close();
      setSelectedRow(null);
      list.reload();
    }
  };

  const handleChangeStatusClick = (row) => {
    setSelectedRow(row);
    mutations.clearError();
    statusModal.open();
  };

  const handleSubmitStatus = async (status, reason) => {
    if (!selectedRow) return;
    const result = await mutations.changeStatus(selectedRow.id, status, reason);
    if (result) {
      statusModal.close();
      setSelectedRow(null);
      list.reload();
    }
  };

  return (
    <div>
      <PageHeader
        title="Chương trình khuyến mãi"
        subtitle="Quản lý các chương trình khuyến mãi phạm vi Hệ thống (SYSTEM)."
        actions={
          <Button variant="primary" onClick={() => navigate(ROUTES.promotions.create)}>
            + Tạo promotion
          </Button>
        }
      />

      <PromotionFilterBar
        keyword={list.keyword}
        onKeywordChange={list.setKeyword}
        status={list.status}
        onStatusChange={list.setStatus}
      />

      {list.error && (
        <div className={styles.errorBanner}>
          {list.error.status ? `Lỗi ${list.error.status}: ` : ''}
          {list.error.message}
        </div>
      )}

      <PromotionTable
        data={list.items}
        isLoading={list.isLoading}
        onView={(id) => navigate(ROUTES.promotions.detail(id))}
        onEdit={(id) => navigate(ROUTES.promotions.edit(id))}
        onDelete={handleDeleteClick}
        onChangeStatus={handleChangeStatusClick}
      />

      <Pagination
        page={list.page}
        totalPages={list.totalPages}
        totalElements={list.totalElements}
        onPageChange={list.setPage}
      />

      <ConfirmDialog
        isOpen={deleteDialog.isOpen}
        title="Xóa promotion"
        message={`Bạn có chắc muốn xóa promotion "${selectedRow?.name}"? Hành động này sẽ xóa mềm (soft delete).`}
        confirmLabel="Xóa"
        danger
        isLoading={mutations.isSubmitting}
        onConfirm={handleConfirmDelete}
        onCancel={() => {
          deleteDialog.close();
          setSelectedRow(null);
        }}
      />

      <ChangeStatusModal
        isOpen={statusModal.isOpen}
        currentStatus={selectedRow?.status}
        isSubmitting={mutations.isSubmitting}
        error={mutations.error}
        onClose={() => {
          statusModal.close();
          setSelectedRow(null);
        }}
        onSubmit={handleSubmitStatus}
      />
    </div>
  );
}
