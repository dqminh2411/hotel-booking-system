import { useState } from 'react';
import { Link } from 'react-router-dom';
import PublicHeader from '@/shared/components/PublicHeader';
import { PageHeader } from '@/shared/components/PageHeader/PageHeader';
import { Pagination } from '@/shared/components/Pagination/Pagination';
import { useDisclosure } from '@/shared/hooks/useDisclosure';
import { usePendingHotels } from '@/features/admin/hooks/usePendingHotels';
import { useAdminHotelMutations } from '@/features/admin/hooks/useAdminHotelMutations';
import { HotelPendingTable } from '@/features/admin/components/HotelPendingTable';
import { UpdateHotelStatusModal } from '@/features/admin/components/UpdateHotelStatusModal';
import { ManageHotelImagesModal } from '@/features/admin/components/ManageHotelImagesModal';
import { HOTEL_UPDATABLE_STATUS } from '@/features/admin/constants/adminOptions';

/**
 * Trang Admin - Khách sạn chờ duyệt.
 * Gom UI cho ca 3 API trong AdminController (hotel-service):
 *  - GET    /api/admin/hotels/pending        -> usePendingHotels (bang + phan trang)
 *  - PATCH  /api/admin/hotels/{id}/status    -> UpdateHotelStatusModal (Duyệt / Từ chối)
 *  - DELETE /api/admin/hotels/images         -> ManageHotelImagesModal (chọn & xóa ảnh)
 */
export function AdminHotelPendingListPage() {
  const list = usePendingHotels();
  const mutations = useAdminHotelMutations();

  const statusModal = useDisclosure();
  const imagesModal = useDisclosure();
  const [selectedHotel, setSelectedHotel] = useState(null);
  const [targetStatus, setTargetStatus] = useState(HOTEL_UPDATABLE_STATUS.APPROVED);

  const openStatusModal = (hotel, status) => {
    setSelectedHotel(hotel);
    setTargetStatus(status);
    mutations.clearError();
    statusModal.open();
  };

  const handleSubmitStatus = async (reason) => {
    if (!selectedHotel) return;
    const ok = await mutations.updateStatus(selectedHotel.hotelId, targetStatus, reason);
    if (ok) {
      statusModal.close();
      setSelectedHotel(null);
      list.reload();
    }
  };

  const openImagesModal = (hotel) => {
    setSelectedHotel(hotel);
    mutations.clearError();
    imagesModal.open();
  };

  const handleDeleteImages = async (imgIds) => {
    if (!selectedHotel || imgIds.length === 0) return false;
    const ok = await mutations.deleteImages(selectedHotel.hotelId, imgIds);
    if (ok) {
      // Anh vua xoa co the la coverImgUrl dang hien thi tren bang -> reload lai list.
      list.reload();
    }
    return ok;
  };

  return (
    <div className="min-h-screen bg-slate-50">
      <PublicHeader />

      <main className="mx-auto max-w-6xl px-4 py-6 md:px-6 lg:px-8">
        <div className="mb-2">
          <Link to="/" className="text-sm font-medium text-blue-700 hover:underline">
            ← Về trang chủ
          </Link>
        </div>

        <PageHeader
          title="Khách sạn chờ duyệt"
          subtitle="Danh sách khách sạn đăng ký vào hệ thống đang ở trạng thái PENDING."
        />

        {list.errorMessage && (
          <div className="mb-4 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
            {list.errorMessage}
          </div>
        )}

        <HotelPendingTable
          data={list.items}
          isLoading={list.isLoading}
          onApprove={(hotel) => openStatusModal(hotel, HOTEL_UPDATABLE_STATUS.APPROVED)}
          onSuspend={(hotel) => openStatusModal(hotel, HOTEL_UPDATABLE_STATUS.SUSPENDED)}
          onManageImages={openImagesModal}
        />

        <Pagination
          page={list.page}
          totalPages={list.totalPages}
          totalElements={list.totalElements}
          onPageChange={list.setPage}
        />

        <UpdateHotelStatusModal
          isOpen={statusModal.isOpen}
          hotel={selectedHotel}
          targetStatus={targetStatus}
          isSubmitting={mutations.isSubmitting}
          errorMessage={mutations.errorMessage}
          onClose={() => {
            statusModal.close();
            setSelectedHotel(null);
          }}
          onSubmit={handleSubmitStatus}
        />

        <ManageHotelImagesModal
          isOpen={imagesModal.isOpen}
          hotel={selectedHotel}
          isSubmitting={mutations.isSubmitting}
          errorMessage={mutations.errorMessage}
          onClose={() => {
            imagesModal.close();
            setSelectedHotel(null);
          }}
          onSubmit={handleDeleteImages}
        />
      </main>
    </div>
  );
}