import { formatCurrency } from '../../../shared/utils/formatters';

const PLACEHOLDER_IMAGE =
  'https://images.unsplash.com/photo-1611892440504-42a792e24d32?w=600&h=400&fit=crop';

export default function RoomTypeCard({ roomType, quantity, nights, onViewDetail, onQuantityChange }) {
  const maxQuantity = roomType.availableRooms ?? roomType.totalRooms;
  const isSoldOut = maxQuantity <= 0;
  const subtotal = roomType.basePricePerNight * quantity * nights;

  const hasSelection = quantity > 0;
  const showSubtotal = hasSelection && nights > 0;
  const showDatesWarning = hasSelection && nights === 0;

  function handleImageError(event) {
    event.currentTarget.src = PLACEHOLDER_IMAGE;
  }

  function handleQuantityInput(event) {
    const digitsOnly = event.target.value.replace(/[^0-9]/g, '');
    const withoutLeadingZeros = digitsOnly.replace(/^0+(?=\d)/, '');

    if (withoutLeadingZeros === '') {
      onQuantityChange(roomType.roomTypeId, 0);
      return;
    }

    const parsed = Number(withoutLeadingZeros);
    const clamped = Math.min(maxQuantity, parsed);
    onQuantityChange(roomType.roomTypeId, clamped);
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
                : `Còn ${roomType.availableRooms}/${roomType.totalRooms} phòng`}
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

        <div className="w-full md:w-28">
          <label
            htmlFor={`quantity-${roomType.roomTypeId}`}
            className="mb-1 block text-sm font-medium text-slate-700"
          >
            Số lượng
          </label>
          <input
            id={`quantity-${roomType.roomTypeId}`}
            type="text"
            inputMode="numeric"
            pattern="[0-9]*"
            value={quantity}
            disabled={isSoldOut}
            onChange={handleQuantityInput}
            className="form-input"
          />
          <p className="mt-1 text-xs text-slate-500">Tối đa {maxQuantity} phòng</p>
        </div>

        <div className="text-left md:text-right">
          <p className={`text-sm font-semibold text-slate-900 ${showSubtotal ? '' : 'invisible'}`}>
            {formatCurrency(subtotal)}
          </p>
          <p
            className={`text-xs ${showDatesWarning ? 'text-amber-600' : 'text-slate-500'} ${
              hasSelection ? '' : 'invisible'
            }`}
          >
            {showDatesWarning ? 'Chọn ngày nhận/trả phòng để tính' : `${quantity} phòng × ${nights} đêm`}
          </p>
        </div>
      </div>
    </article>
  );
}