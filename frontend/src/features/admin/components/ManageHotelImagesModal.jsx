import { useEffect, useState } from 'react';
import { Modal } from '@/shared/components/Modal/Modal';
import { Button } from '@/shared/components/Button/Button';
import { ConfirmDialog } from '@/shared/components/ConfirmDialog/ConfirmDialog';
import { Loading } from '@/shared/components/Loading/Loading';
import { EmptyState } from '@/shared/components/EmptyState/EmptyState';
import { useDisclosure } from '@/shared/hooks/useDisclosure';
import { useHotelImages } from '@/features/admin/hooks/useHotelImages';

/**
 * Popup quan ly & xoa anh khach san (DELETE /api/admin/hotels/images).
 * AdminController khong co endpoint rieng de liet ke anh kem id, nen popup nay
 * tai su dung GET /api/hotels/{hotelId} (xem useHotelImages) chi de LAY id anh hien co,
 * viec XOA van goi dung endpoint admin (DELETE /api/admin/hotels/images).
 *
 * Props: isOpen, hotel, isSubmitting, errorMessage, onClose, onSubmit(imgIds) -> Promise<boolean>
 */
export function ManageHotelImagesModal({ isOpen, hotel, isSubmitting, errorMessage, onClose, onSubmit }) {
  const { images, isLoading, errorMessage: loadError, reload } = useHotelImages(hotel?.hotelId, isOpen);
  const [selectedIds, setSelectedIds] = useState([]);
  const confirmDialog = useDisclosure();

  useEffect(() => {
    if (isOpen) setSelectedIds([]);
  }, [isOpen, hotel?.hotelId]);

  const toggle = (id) => {
    setSelectedIds((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]));
  };

  const handleClose = () => {
    setSelectedIds([]);
    onClose();
  };

  const handleConfirmDelete = async () => {
    const ok = await onSubmit(selectedIds);
    if (ok) {
      confirmDialog.close();
      setSelectedIds([]);
      reload();
    }
  };

  return (
    <>
      <Modal
        isOpen={isOpen}
        onClose={handleClose}
        title={`Quản lý ảnh - ${hotel?.name ?? ''}`}
        width={640}
        footer={
          <>
            <Button variant="secondary" onClick={handleClose}>
              Đóng
            </Button>
            <Button variant="danger" disabled={selectedIds.length === 0} onClick={confirmDialog.open}>
              Xóa ảnh đã chọn ({selectedIds.length})
            </Button>
          </>
        }
      >
        <div className="flex flex-col gap-4">
          {errorMessage && (
            <div className="rounded-md bg-red-50 px-4 py-3 text-sm text-red-700">{errorMessage}</div>
          )}
          {loadError && (
            <div className="rounded-md bg-red-50 px-4 py-3 text-sm text-red-700">{loadError}</div>
          )}

          {isLoading ? (
            <Loading label="Đang tải danh sách ảnh..." />
          ) : images.length === 0 ? (
            <EmptyState message="Khách sạn chưa có ảnh nào" />
          ) : (
            <div className="grid grid-cols-[repeat(auto-fill,minmax(140px,1fr))] gap-3">
              {images.map((img) => {
                const checked = selectedIds.includes(img.id);
                return (
                  <label
                    key={img.id}
                    className={`relative block cursor-pointer overflow-hidden rounded-md border-2 transition-colors ${
                      checked ? 'border-red-500' : 'border-transparent'
                    }`}
                  >
                    <input
                      type="checkbox"
                      checked={checked}
                      onChange={() => toggle(img.id)}
                      className="absolute left-2 top-2 z-10 h-4 w-4 cursor-pointer"
                    />
                    <img src={img.url} alt="" className="block h-24 w-full bg-slate-100 object-cover" />
                  </label>
                );
              })}
            </div>
          )}
        </div>
      </Modal>

      <ConfirmDialog
        isOpen={confirmDialog.isOpen}
        title="Xóa ảnh khách sạn"
        message={`Bạn có chắc muốn xóa ${selectedIds.length} ảnh đã chọn? Hành động này không thể hoàn tác (ảnh sẽ bị xóa khỏi cả MinIO).`}
        confirmLabel="Xóa"
        danger
        isLoading={isSubmitting}
        onConfirm={handleConfirmDelete}
        onCancel={confirmDialog.close}
      />
    </>
  );
}