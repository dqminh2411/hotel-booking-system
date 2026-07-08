import EmptyState from '../../../shared/components/EmptyState';
import RoomTypeCard from './RoomTypeCard';

export default function RoomTypeList({ roomTypes, onViewDetail, onBook }) {
  return (
    <section>
      <h2 className="text-xl font-semibold text-slate-900">Loại phòng</h2>

      {roomTypes && roomTypes.length > 0 ? (
        <div className="mt-4 space-y-4">
          {roomTypes.map((roomType) => (
            <RoomTypeCard
              key={roomType.roomTypeId}
              roomType={roomType}
              onViewDetail={onViewDetail}
              onBook={onBook}
            />
          ))}
        </div>
      ) : (
        <div className="mt-4">
          <EmptyState
            title="Không có loại phòng phù hợp"
            description="Thử thay đổi ngày nhận phòng, trả phòng hoặc số lượng khách."
          />
        </div>
      )}
    </section>
  );
}
