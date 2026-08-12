import { formatAddress, getHotelStatusBadgeClass, getHotelStatusLabel } from '../utils/hotelFormatters';

function scrollToRoomTypes() {
  document.getElementById('room-types')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

export default function HotelHeader({ hotel, actions }) {
  return (
    <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
      <div className="flex flex-col gap-2">
        <div className="flex flex-wrap items-center gap-3">
          <h1 className="text-2xl font-bold text-slate-900 md:text-3xl">{hotel.name}</h1>
          <span
            className={`inline-flex items-center rounded-full border px-2.5 py-1 text-xs font-semibold ${getHotelStatusBadgeClass(
              hotel.status,
            )}`}
          >
            {getHotelStatusLabel(hotel.status)}
          </span>
        </div>
        <p className="flex items-center gap-1.5 text-sm text-slate-600">
          <span aria-hidden="true">📍</span>
          {formatAddress(hotel.address) || 'Chưa cập nhật địa chỉ'}
        </p>
      </div>

      {actions ? (
        <div className="shrink-0">{actions}</div>
      ) : (
        <button type="button" onClick={scrollToRoomTypes} className="accent-button shrink-0">
          Đặt ngay
        </button>
      )}
    </div>
  );
}