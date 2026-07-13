import { useEffect, useState } from 'react';
import useRoomTypeDetail from '../hooks/useRoomTypeDetail';
import { formatCurrency } from '../../../shared/utils/formatters';
import ErrorState from '../../../shared/components/ErrorState';

const PLACEHOLDER_IMAGE =
  'https://images.unsplash.com/photo-1611892440504-42a792e24d32?w=900&h=700&fit=crop';

function AmenityChip({ label }) {
  return (
    <span className="inline-flex items-center gap-1.5 rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs font-medium text-slate-700">
      {label}
    </span>
  );
}

export default function RoomTypeDetailModal({ roomTypeId, onClose }) {
  const { roomType, status, errorMessage } = useRoomTypeDetail(roomTypeId);
  const [activeImageIndex, setActiveImageIndex] = useState(0);

  useEffect(() => {
    if (!roomTypeId) return undefined;

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [roomTypeId]);

  useEffect(() => {
    setActiveImageIndex(0);
  }, [roomType?.roomTypeId]);

  if (!roomTypeId) return null;

  const images = roomType?.images && roomType.images.length > 0
    ? roomType.images
    : [{ id: 'placeholder', url: PLACEHOLDER_IMAGE }];
  const hasMultipleImages = images.length > 1;
  const activeImage = images[activeImageIndex] || images[0];

  function handleImageError(event) {
    event.currentTarget.src = PLACEHOLDER_IMAGE;
  }

  function goToPrevImage(event) {
    event.stopPropagation();
    setActiveImageIndex((current) => (current - 1 + images.length) % images.length);
  }

  function goToNextImage(event) {
    event.stopPropagation();
    setActiveImageIndex((current) => (current + 1) % images.length);
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="relative max-h-[90vh] w-full max-w-4xl overflow-hidden rounded-xl bg-white shadow-lg">
        <button
          type="button"
          onClick={onClose}
          aria-label="Đóng"
          className="absolute right-3 top-3 z-20 flex h-8 w-8 items-center justify-center rounded-full bg-white text-slate-600 shadow-md hover:bg-slate-100"
        >
          ✕
        </button>

        {status === 'loading' && (
          <div className="grid animate-pulse gap-0 md:grid-cols-2">
            <div className="h-64 bg-slate-200 md:h-[420px]" />
            <div className="space-y-3 p-5">
              <div className="h-6 w-2/3 rounded bg-slate-200" />
              <div className="h-4 w-1/3 rounded bg-slate-200" />
              <div className="h-4 w-full rounded bg-slate-200" />
              <div className="h-4 w-full rounded bg-slate-200" />
            </div>
          </div>
        )}

        {status === 'error' && (
          <div className="p-5">
            <ErrorState message={errorMessage} />
          </div>
        )}

        {status === 'success' && roomType && (
          <div className="grid max-h-[90vh] overflow-y-auto md:grid-cols-2 md:overflow-visible">
            <div className="flex flex-col bg-slate-100 md:sticky md:top-0 md:h-[90vh]">
              <div className="relative h-64 shrink-0 sm:h-80 md:h-[70%]">
                <img
                  src={activeImage.url}
                  alt={roomType.name}
                  onError={handleImageError}
                  className="h-full w-full object-cover"
                />

                {hasMultipleImages && (
                  <>
                    <button
                      type="button"
                      onClick={goToPrevImage}
                      aria-label="Ảnh trước"
                      className="absolute left-2 top-1/2 flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-full bg-white/90 text-slate-700 shadow hover:bg-white"
                    >
                      ‹
                    </button>
                    <button
                      type="button"
                      onClick={goToNextImage}
                      aria-label="Ảnh sau"
                      className="absolute right-2 top-1/2 flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-full bg-white/90 text-slate-700 shadow hover:bg-white"
                    >
                      ›
                    </button>
                    <div className="absolute bottom-2 left-1/2 flex -translate-x-1/2 gap-1.5">
                      {images.map((image, index) => (
                        <span
                          key={image.id}
                          className={`h-1.5 w-1.5 rounded-full ${
                            index === activeImageIndex ? 'bg-white' : 'bg-white/50'
                          }`}
                        />
                      ))}
                    </div>
                  </>
                )}
              </div>

              {hasMultipleImages && (
                <div className="flex gap-2 overflow-x-auto p-2">
                  {images.map((image, index) => (
                    <button
                      key={image.id}
                      type="button"
                      onClick={() => setActiveImageIndex(index)}
                      className={`h-14 w-16 shrink-0 overflow-hidden rounded-md ${
                        index === activeImageIndex ? 'ring-2 ring-blue-700' : 'opacity-80 hover:opacity-100'
                      }`}
                    >
                      <img
                        src={image.url}
                        alt={`${roomType.name} - ảnh ${index + 1}`}
                        onError={handleImageError}
                        className="h-full w-full object-cover"
                      />
                    </button>
                  ))}
                </div>
              )}
            </div>

            <div className="flex flex-col p-5">
              <h3 className="text-xl font-semibold text-slate-900">{roomType.name}</h3>

              <div className="mt-3 flex flex-wrap gap-2">
                {roomType.area ? <AmenityChip label={`📐 ${roomType.area} m²`} /> : null}
                <AmenityChip label={`👤 Tối đa ${roomType.maxGuests} khách`} />
                <AmenityChip label={`🛏️ ${roomType.bedCounts} giường`} />
                {(roomType.amenities || []).slice(0, 4).map((amenity) => (
                  <AmenityChip key={amenity.id} label={`✓ ${amenity.name}`} />
                ))}
              </div>

              <dl className="mt-4 divide-y divide-slate-100 border-y border-slate-100 text-sm">
                <div className="flex items-center justify-between py-2">
                  <dt className="text-slate-500">Giá phòng</dt>
                  <dd className="font-medium text-slate-900">
                    {roomType.basePricePerNight? `${formatCurrency(roomType.basePricePerNight)} / đêm` : 'Chưa cập nhật'}
                  </dd>
                </div>
                <div className="flex items-center justify-between py-2">
                  <dt className="text-slate-500">Kích thước phòng</dt>
                  <dd className="font-medium text-slate-900">
                    {roomType.area ? `${roomType.area} m²` : 'Chưa cập nhật'}
                  </dd>
                </div>
                <div className="flex items-center justify-between py-2">
                  <dt className="text-slate-500">Số giường</dt>
                  <dd className="font-medium text-slate-900">{roomType.bedCounts} giường</dd>
                </div>
                <div className="flex items-center justify-between py-2">
                  <dt className="text-slate-500">Sức chứa tối đa</dt>
                  <dd className="font-medium text-slate-900">{roomType.maxGuests} khách</dd>
                </div>
                <div className="flex items-center justify-between py-2">
                  <dt className="text-slate-500">Tổng số phòng loại này</dt>
                  <dd className="font-medium text-slate-900">{roomType.totalRooms} phòng</dd>
                </div>
              </dl>

              {roomType.description && (
                <p className="mt-4 whitespace-pre-line text-sm leading-6 text-slate-700">{roomType.description}</p>
              )}

              {roomType.amenities && roomType.amenities.length > 0 && (
                <div className="mt-4">
                  <h4 className="text-sm font-semibold text-slate-900">Tiện nghi phòng</h4>
                  <ul className="mt-2 grid grid-cols-2 gap-x-4 gap-y-1.5 text-sm text-slate-700">
                    {roomType.amenities.map((amenity) => (
                      <li key={amenity.id} className="flex items-center gap-1.5">
                        <span className="text-blue-700" aria-hidden="true">
                          ✓
                        </span>
                        {amenity.name}
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}