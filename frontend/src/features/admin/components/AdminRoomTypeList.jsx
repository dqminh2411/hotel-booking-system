import { EmptyState } from '@/shared/components/EmptyState/EmptyState';
import { formatCurrency } from '@/shared/utils/formatters';

const PLACEHOLDER_IMAGE =
  'https://images.unsplash.com/photo-1611892440504-42a792e24d32?w=600&h=400&fit=crop';

/**
 * Danh sach loai phong cho AdminHotelDetailPage - CHI de xem, khong co filter
 * checkin/checkout va khong cho dat phong (khac RoomTypeList dung o HotelDetailPage).
 * Bam "Xem chi tiết phòng" se mo lai RoomTypeDetailModal (tai su dung nguyen ban
 * component/hook cua trang HotelDetailPage.jsx).
 *
 * Props: roomTypes (RoomTypeResponse[]), onViewDetail(roomTypeId)
 */
export function AdminRoomTypeList({ roomTypes, onViewDetail }) {
  if (!roomTypes || roomTypes.length === 0) {
    return (
      <section>
        <h2 className="text-xl font-semibold text-slate-900">Loại phòng</h2>
        <div className="mt-4">
          <EmptyState message="Khách sạn này chưa khai báo loại phòng nào." />
        </div>
      </section>
    );
  }

  function handleImageError(event) {
    event.currentTarget.src = PLACEHOLDER_IMAGE;
  }

  return (
    <section>
      <h2 className="text-xl font-semibold text-slate-900">Loại phòng</h2>

      <div className="mt-4 space-y-4">
        {roomTypes.map((roomType) => (
          <article
            key={roomType.roomTypeId}
            className="grid gap-4 rounded-lg border border-slate-200 bg-white p-4 shadow-sm md:grid-cols-[220px_1fr_auto]"
          >
            <div className="overflow-hidden rounded-lg bg-slate-200">
              <img
                src={roomType.coverImageUrl || PLACEHOLDER_IMAGE}
                alt={roomType.name}
                onError={handleImageError}
                className="h-40 w-full object-cover md:h-full"
              />
            </div>

            <div>
              <h3 className="text-lg font-semibold text-slate-900">{roomType.name}</h3>
              <ul className="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-sm text-slate-600">
                <li>👤 Tối đa {roomType.maxGuests} khách</li>
                <li>🛏️ {roomType.bedCounts} giường</li>
                {roomType.area ? <li>📐 {roomType.area} m²</li> : null}
              </ul>

              <p className="mt-2 text-sm font-medium text-slate-700">
                {roomType.availableRooms === null || roomType.availableRooms === undefined
                  ? `${roomType.totalRooms} phòng`
                  : `Còn ${roomType.availableRooms}/${roomType.totalRooms} phòng`}
              </p>
            </div>

            <div className="flex flex-row items-center justify-between gap-3 md:flex-col md:items-end md:justify-between">
              <div className="text-left md:text-right">
                <p className="text-xl font-bold text-slate-900">
                  {formatCurrency(roomType.basePricePerNight)}
                </p>
                <p className="text-xs text-slate-500">mỗi đêm</p>
              </div>

              <button
                type="button"
                onClick={() => onViewDetail(roomType.roomTypeId)}
                className="whitespace-nowrap text-sm font-semibold text-blue-700 hover:text-blue-800"
              >
                Xem chi tiết phòng
              </button>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}