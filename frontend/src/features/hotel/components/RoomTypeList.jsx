import { useMemo, useState } from 'react';
import EmptyState from '../../../shared/components/EmptyState';
import { formatCurrency } from '../../../shared/utils/formatters';
import { getNightsCount } from '../utils/hotelFormatters';
import RoomTypeCard from './RoomTypeCard';

export default function RoomTypeList({ roomTypes, checkinDate, checkoutDate, onViewDetail, onBook }) {
  const [quantities, setQuantities] = useState({});
  const nights = getNightsCount(checkinDate, checkoutDate);

  function handleQuantityChange(roomTypeId, quantity) {
    setQuantities((prev) => ({ ...prev, [roomTypeId]: quantity }));
  }

  const selections = useMemo(
    () =>
      (roomTypes || [])
        .map((roomType) => ({
          roomTypeId: roomType.roomTypeId,
          name: roomType.name,
          bedCount: roomType.bedCounts,
          totalQuantity: roomType.totalRooms,
          availableRooms: roomType.availableRooms,
          basePricePerNight: roomType.basePricePerNight,
          pricePerNight: roomType.basePricePerNight,
          quantity: quantities[roomType.roomTypeId] || 0,
          subtotal: roomType.basePricePerNight * (quantities[roomType.roomTypeId] || 0) * nights,
        }))
        .filter((selection) => selection.quantity > 0),
    [roomTypes, quantities],
  );

  const totalRoomCount = selections.reduce((sum, selection) => sum + selection.quantity, 0);
  const totalPrice = selections.reduce(
    (sum, selection) => sum + selection.basePricePerNight * selection.quantity * nights,
    0,
  );

  function handleBookClick() {
    if (totalRoomCount === 0 || nights <= 0) return;
    onBook(selections);
  }

  if (!roomTypes || roomTypes.length === 0) {
    return (
      <section id="room-types" className="scroll-mt-6">
        <h2 className="text-xl font-semibold text-slate-900">Loại phòng</h2>
        <div className="mt-4">
          <EmptyState
            title="Không có loại phòng phù hợp"
            description="Thử thay đổi ngày nhận phòng, trả phòng hoặc số lượng khách."
          />
        </div>
      </section>
    );
  }

  return (
    <section id="room-types" className="scroll-mt-6">
      <h2 className="text-xl font-semibold text-slate-900">Loại phòng</h2>

      <div className="mt-4 grid gap-6 lg:grid-cols-[1fr_320px]">
        <div className="space-y-4">
          {roomTypes.map((roomType) => (
            <RoomTypeCard
              key={roomType.roomTypeId}
              roomType={roomType}
              quantity={quantities[roomType.roomTypeId] || 0}
              nights={nights}
              onViewDetail={onViewDetail}
              onQuantityChange={handleQuantityChange}
            />
          ))}
        </div>

        <aside className="lg:sticky lg:top-6 lg:h-fit">
          <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
            <p className="text-sm text-slate-600">
              <span className="text-lg font-bold text-slate-900">{totalRoomCount}</span> phòng đã chọn
            </p>
            <p className="mt-2 text-2xl font-bold text-slate-900">{formatCurrency(totalPrice)}</p>
            <p className="text-xs text-slate-500">
              {nights > 0 ? `Tổng tiền cho ${nights} đêm` : 'Chọn ngày nhận/trả phòng để tính tổng tiền'}
            </p>

            <button
              type="button"
              disabled={totalRoomCount === 0 || nights <= 0}
              onClick={handleBookClick}
              className="accent-button mt-4 w-full"
            >
              Tôi sẽ đặt
            </button>
          </div>
        </aside>
      </div>
    </section>
  );
}
