import { useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import PublicHeader from '../shared/components/PublicHeader';
import ErrorState from '../shared/components/ErrorState';
import useHotelDetail from '../features/hotel/hooks/useHotelDetail';
import HotelHeader from '../features/hotel/components/HotelHeader';
import HotelGallery from '../features/hotel/components/HotelGallery';
import HotelOverview from '../features/hotel/components/HotelOverview';
import HotelAmenities from '../features/hotel/components/HotelAmenities';
import HotelPolicies from '../features/hotel/components/HotelPolicies';
import HotelBookingPanel from '../features/hotel/components/HotelBookingPanel';
import HotelDetailSkeleton from '../features/hotel/components/HotelDetailSkeleton';
import RoomTypeList from '../features/hotel/components/RoomTypeList';
import RoomTypeDetailModal from '../features/hotel/components/RoomTypeDetailModal';

export default function HotelDetailPage() {
  const { hotelId } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const [selectedRoomTypeId, setSelectedRoomTypeId] = useState(null);

  const checkinDate = searchParams.get('checkinDate') || '';
  const checkoutDate = searchParams.get('checkoutDate') || '';
  const guestNum = searchParams.get('guestNum') || '';
  const roomNum = searchParams.get('roomNum') || '';

  const { hotel, status, errorMessage, reload } = useHotelDetail(hotelId, {
    checkinDate: checkinDate || undefined,
    checkoutDate: checkoutDate || undefined,
    guestNum: guestNum || undefined,
    roomNum: roomNum || undefined,
  });

  function handleSearch(nextValues) {
    const nextSearchParams = {};
    if (nextValues.checkinDate) nextSearchParams.checkinDate = nextValues.checkinDate;
    if (nextValues.checkoutDate) nextSearchParams.checkoutDate = nextValues.checkoutDate;
    if (nextValues.guestNum) nextSearchParams.guestNum = String(nextValues.guestNum);
    if (nextValues.roomNum) nextSearchParams.roomNum = String(nextValues.roomNum);
    setSearchParams(nextSearchParams);
  }

  function handleBookRoomType(roomType) {
    const bookingParams = new URLSearchParams({
      hotelId,
      roomTypeId: roomType.roomTypeId,
      guestNum: guestNum || String(roomType.maxGuests),
      roomNum: roomNum || '1',
    });
    if (checkinDate) bookingParams.set('checkinDate', checkinDate);
    if (checkoutDate) bookingParams.set('checkoutDate', checkoutDate);

    navigate(`/booking/checkout?${bookingParams.toString()}`);
  }

  return (
    <div className="min-h-screen bg-slate-50">
      <PublicHeader />

      <main className="mx-auto max-w-7xl px-4 py-6 md:px-6 lg:px-8">
        <Link to="/" className="text-sm font-medium text-blue-700 hover:text-blue-800">
          ← Quay lại tìm kiếm
        </Link>

        <div className="mt-4">
          {status === 'loading' && <HotelDetailSkeleton />}

          {status === 'error' && <ErrorState message={errorMessage} onRetry={reload} />}

          {status === 'success' && hotel && (
            <div className="space-y-8">
              <HotelHeader hotel={hotel} />
              <HotelGallery images={hotel.imageUrls} hotelName={hotel.name} />

              <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
                <div className="space-y-8">
                  <HotelOverview description={hotel.description} />
                  <HotelAmenities amenities={hotel.amenities} />
                </div>

                <aside className="lg:sticky lg:top-6 lg:h-fit">
                  <HotelBookingPanel
                    initialValues={{
                      checkinDate,
                      checkoutDate,
                      guestNum: Number(guestNum) || 2,
                      roomNum: Number(roomNum) || 1,
                    }}
                    onSearch={handleSearch}
                  />
                </aside>
              </div>

              <RoomTypeList
                roomTypes={hotel.availableRoomTypes}
                onViewDetail={setSelectedRoomTypeId}
                onBook={handleBookRoomType}
              />

              <HotelPolicies policies={hotel.policies} />
            </div>
          )}
        </div>
      </main>

      <RoomTypeDetailModal roomTypeId={selectedRoomTypeId} onClose={() => setSelectedRoomTypeId(null)} />
    </div>
  );
}
