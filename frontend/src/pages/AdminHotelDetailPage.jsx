import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import PublicHeader from '../shared/components/PublicHeader';
import ErrorState from '../shared/components/ErrorState';
import { Button } from '@/shared/components/Button/Button';
import { useDisclosure } from '@/shared/hooks/useDisclosure';
import { useAdminHotelDetail } from '@/features/admin/hooks/useAdminHotelDetail';
import HotelHeader from '../features/hotel/components/HotelHeader';
import HotelGallery from '../features/hotel/components/HotelGallery';
import HotelOverview from '../features/hotel/components/HotelOverview';
import HotelAmenities from '../features/hotel/components/HotelAmenities';
import HotelPolicies from '../features/hotel/components/HotelPolicies';
import HotelDetailSkeleton from '../features/hotel/components/HotelDetailSkeleton';
import RoomTypeDetailModal from '../features/hotel/components/RoomTypeDetailModal';
import { AdminRoomTypeList } from '@/features/admin/components/AdminRoomTypeList';
import { UpdateHotelStatusModal } from '@/features/admin/components/UpdateHotelStatusModal';
import { ManageHotelImagesModal } from '@/features/admin/components/ManageHotelImagesModal';
import { useAdminHotelMutations } from '@/features/admin/hooks/useAdminHotelMutations';
import { HOTEL_UPDATABLE_STATUS } from '@/features/admin/constants/adminOptions';
import { ROUTES } from '@/shared/constants/routes';

/**
 * Trang Admin - Chi tiết khách sạn (chờ duyệt).
 * Layout gần giống HotelDetailPage.jsx (cùng các block Header/Gallery/Overview/
 * Amenities/Policies và RoomTypeDetailModal khi xem chi tiết 1 loại phòng), NHƯNG:
 *  - Gọi GET /api/admin/hotels/{hotelId} qua useAdminHotelDetail (KHÔNG dùng
 *    GET /api/hotels/{hotelId} public) vì endpoint public chỉ trả về hotel có
 *    status = APPROVED, trong khi trang này cần xem cả hotel đang PENDING/SUSPENDED.
 *  - Không truyền checkinDate/checkoutDate/guestNum/roomNum (chỉ xem, không đặt phòng).
 *  - Không có HotelBookingPanel / RoomTypeList kiểu đặt phòng, thay bằng
 *    AdminRoomTypeList (chỉ xem, không chọn số lượng/đặt).
 *  - Thay nút "Đặt ngay" trên HotelHeader bằng 3 nút action quản lý:
 *    Quản lý ảnh / Từ chối / Duyệt (tái sử dụng đúng các modal + hook đã có
 *    ở AdminHotelPendingListPage trước đây).
 */
export default function AdminHotelDetailPage() {
  const { hotelId } = useParams();
  const navigate = useNavigate();

  const [selectedRoomTypeId, setSelectedRoomTypeId] = useState(null);
  const [targetStatus, setTargetStatus] = useState(HOTEL_UPDATABLE_STATUS.APPROVED);

  const { hotel, status, errorMessage, reload } = useAdminHotelDetail(hotelId);
  const mutations = useAdminHotelMutations();
  const statusModal = useDisclosure();
  const imagesModal = useDisclosure();

  function openStatusModal(nextStatus) {
    setTargetStatus(nextStatus);
    mutations.clearError();
    statusModal.open();
  }

  async function handleSubmitStatus(reason) {
    if (!hotelId) return;
    const ok = await mutations.updateStatus(hotelId, targetStatus, reason);
    if (ok) {
      statusModal.close();
      // Sau khi duyệt/từ chối, khách sạn không còn ở trạng thái PENDING nữa
      // nên quay lại danh sách chờ duyệt.
      navigate(ROUTES.admin.hotelsPending);
    }
  }

  function openImagesModal() {
    mutations.clearError();
    imagesModal.open();
  }

  async function handleDeleteImages(imgIds) {
    if (!hotelId || imgIds.length === 0) return false;
    const ok = await mutations.deleteImages(hotelId, imgIds);
    if (ok) {
      // Anh vua xoa co the la coverImgUrl/anh trong gallery dang hien thi -> reload lai.
      reload();
    }
    return ok;
  }

  const actionButtons = (
    <div className="flex flex-wrap gap-2">
      <Button size="sm" variant="secondary" onClick={openImagesModal}>
        Quản lý ảnh
      </Button>
      <Button
        size="sm"
        variant="danger"
        onClick={() => openStatusModal(HOTEL_UPDATABLE_STATUS.SUSPENDED)}
      >
        Từ chối
      </Button>
      <Button
        size="sm"
        variant="primary"
        onClick={() => openStatusModal(HOTEL_UPDATABLE_STATUS.APPROVED)}
      >
        Duyệt
      </Button>
    </div>
  );

  return (
    <div className="min-h-screen bg-slate-50">
      <PublicHeader />

      <main className="mx-auto max-w-7xl px-4 py-6 md:px-6 lg:px-8">
        <Link
          to={ROUTES.admin.hotelsPending}
          className="text-sm font-medium text-blue-700 hover:text-blue-800"
        >
          ← Quay lại danh sách chờ duyệt
        </Link>

        <div className="mt-4">
          {status === 'loading' && <HotelDetailSkeleton />}

          {status === 'error' && <ErrorState message={errorMessage} onRetry={reload} />}

          {status === 'success' && hotel && (
            <div className="space-y-8">
              <HotelHeader hotel={hotel} actions={actionButtons} />
              <HotelGallery images={hotel.imageUrls} hotelName={hotel.name} />

              <div className="space-y-8">
                <HotelOverview description={hotel.description} />
                <HotelAmenities amenities={hotel.amenities} />
              </div>

              <AdminRoomTypeList
                roomTypes={hotel.availableRoomTypes}
                onViewDetail={setSelectedRoomTypeId}
              />

              <HotelPolicies policies={hotel.policies} />
            </div>
          )}
        </div>
      </main>

      <RoomTypeDetailModal roomTypeId={selectedRoomTypeId} onClose={() => setSelectedRoomTypeId(null)} />

      <UpdateHotelStatusModal
        isOpen={statusModal.isOpen}
        hotel={hotel}
        targetStatus={targetStatus}
        isSubmitting={mutations.isSubmitting}
        errorMessage={mutations.errorMessage}
        onClose={statusModal.close}
        onSubmit={handleSubmitStatus}
      />

      <ManageHotelImagesModal
        isOpen={imagesModal.isOpen}
        hotel={hotel}
        isSubmitting={mutations.isSubmitting}
        errorMessage={mutations.errorMessage}
        onClose={imagesModal.close}
        onSubmit={handleDeleteImages}
      />
    </div>
  );
}