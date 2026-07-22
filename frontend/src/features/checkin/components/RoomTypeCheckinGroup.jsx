import { getRequiredQuantity } from '../utils/checkinUtils';

export default function RoomTypeCheckinGroup({ roomType, availableRooms, isLoading, selectedRoomIds, onToggleRoom }) {
  const required = getRequiredQuantity(roomType);
  const selectedCount = selectedRoomIds.size;
  const isSatisfied = selectedCount === required && required > 0;
  const rooms = availableRooms || [];
  const notEnoughRooms = !isLoading && rooms.length < required;

  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <p className="font-semibold text-slate-900">{roomType.name}</p>
          <p className="text-xs text-slate-500">
            {roomType.bedCount} giường · Cần chọn {required} phòng
          </p>
        </div>
        <span
          className={`inline-flex items-center rounded-full border px-2.5 py-1 text-xs font-semibold ${
            isSatisfied
              ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
              : 'border-amber-200 bg-amber-50 text-amber-700'
          }`}
        >
          Đã chọn {selectedCount}/{required}
        </span>
      </div>

      {isLoading && <p className="mt-3 text-sm text-slate-500">Đang tải danh sách phòng trống...</p>}

      {notEnoughRooms && (
        <p className="mt-3 rounded-md border border-red-200 bg-red-50 p-2 text-xs text-red-700">
          Chỉ còn {rooms.length} phòng trống (AVAILABLE), chưa đủ {required} phòng để check-in loại phòng này. Vui
          lòng dọn/thả thêm phòng ở hotel-service rồi tải lại danh sách.
        </p>
      )}

      {!isLoading && rooms.length > 0 && (
        <div className="mt-3 flex flex-wrap gap-2">
          {rooms.map((room) => {
            const isSelected = selectedRoomIds.has(room.roomId);
            const isDisabled = !isSelected && selectedCount >= required;

            return (
              <button
                key={room.roomId}
                type="button"
                disabled={isDisabled}
                onClick={() => onToggleRoom(roomType.roomTypeId, room.roomId)}
                className={`rounded-md border px-3 py-2 text-left text-sm font-semibold transition-colors ${
                  isSelected
                    ? 'border-amber-400 bg-amber-400 text-slate-900'
                    : isDisabled
                      ? 'cursor-not-allowed border-slate-200 bg-slate-100 text-slate-400'
                      : 'border-slate-300 bg-white text-slate-700 hover:border-amber-400 hover:bg-amber-50'
                }`}
              >
                <span className="block">Phòng {room.roomNumber}</span>
                {room.floor ? (
                  <span className={`block text-[11px] font-normal ${isSelected ? 'text-slate-700' : 'text-slate-500'}`}>
                    Tầng {room.floor}
                  </span>
                ) : null}
              </button>
            );
          })}
        </div>
      )}

      {!isLoading && rooms.length === 0 && (
        <p className="mt-3 text-sm text-slate-500">Không còn phòng trống nào cho loại phòng này.</p>
      )}
    </div>
  );
}