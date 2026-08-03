import { Link, useNavigate } from 'react-router-dom';
import PublicHeader from '@/shared/components/PublicHeader';
import { PageHeader } from '@/shared/components/PageHeader/PageHeader';
import { Pagination } from '@/shared/components/Pagination/Pagination';
import { usePendingHotels } from '@/features/admin/hooks/usePendingHotels';
import { HotelPendingTable } from '@/features/admin/components/HotelPendingTable';
import { ROUTES } from '@/shared/constants/routes';

/**
 * Trang Admin - Khách sạn chờ duyệt.
 * Gom UI cho endpoint GET /api/admin/hotels/pending (bảng + phân trang) trong AdminController.
 * Các hành động Duyệt / Từ chối / Quản lý ảnh không còn nằm trực tiếp trên bảng này nữa -
 * bấm "Xem chi tiết" sẽ điều hướng sang AdminHotelDetailPage, nơi hiển thị đầy đủ thông tin
 * khách sạn (tái sử dụng GET /api/hotels/{hotelId}) kèm 3 nút action quản lý.
 */
export function AdminHotelPendingListPage() {
  const list = usePendingHotels();
  const navigate = useNavigate();

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
          onViewDetail={(hotel) => navigate(ROUTES.admin.hotelDetail(hotel.hotelId))}
        />

        <Pagination
          page={list.page}
          totalPages={list.totalPages}
          totalElements={list.totalElements}
          onPageChange={list.setPage}
        />
      </main>
    </div>
  );
}