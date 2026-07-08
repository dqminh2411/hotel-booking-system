import { formatCurrency } from '../../../shared/utils/formatters';

const PLACEHOLDER_IMAGE =
  'https://images.unsplash.com/photo-1611892440504-42a792e24d32?w=600&h=400&fit=crop';

export default function RoomTypeCard({ roomType, onViewDetail, onBook }) {
  const isSoldOut = roomType.availableRooms !== undefined && roomType.availableRooms !== null && roomType.availableRooms <= 0;

  function handleImageError(event) {
    event.currentTarget.src = PLACEHOLDER_IMAGE;
  }

  return (
    <article className="grid gap-4 rounded-lg border border-slate-200 bg-white p-4 shadow-sm md:grid-cols-[220px_1fr_200px]">
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

        <p className="mt-2 text-sm font-medium">
          {isSoldOut ? (
            <span className="text-red-600">Hết phòng trống</span>
          ) : (
            <span>
              {roomType.availableRooms === null || roomType.availableRooms === undefined 
                ? `${roomType.totalRooms} phòng` 
                : roomType.availableRooms === 0 
                  ? "Hết phòng" 
                  : `Còn ${roomType.availableRooms}/${roomType.totalRooms} phòng`
              }
            </span>
          )}
        </p>

        <button
          type="button"
          onClick={() => onViewDetail(roomType.roomTypeId)}
          className="mt-2 text-sm font-semibold text-blue-700 hover:text-blue-800"
        >
          Xem chi tiết phòng
        </button>
      </div>

      <div className="flex flex-col items-start justify-between gap-3 md:items-end">
        <div className="text-left md:text-right">
          <p className="text-xl font-bold text-slate-900">{formatCurrency(roomType.basePricePerNight)}</p>
          <p className="text-xs text-slate-500">mỗi đêm</p>
        </div>
        <button
          type="button"
          disabled={isSoldOut}
          onClick={() => onBook(roomType)}
          className="accent-button w-full md:w-auto"
        >
          {isSoldOut ? 'Hết phòng' : 'Đặt phòng'}
        </button>
      </div>
    </article>
  );
}
