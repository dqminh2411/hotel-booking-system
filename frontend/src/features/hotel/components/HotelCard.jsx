export default function HotelCard({ hotel }) {
    return (
        <div className="bg-white border rounded p-4 shadow-sm flex flex-col md:flex-row gap-4 mb-4">
            <img src={hotel.coverImageUrl} alt={hotel.name} className="w-full md:w-56 h-48 object-cover rounded" />
            <div className="flex-1 flex flex-col justify-between">
                <div>
                    <div className="flex justify-between items-start">
                        <h3 className="text-xl font-bold text-booking-blue">{hotel.name}</h3>
                        <div className="flex items-center gap-2">
                            <div className="text-right">
                                <p className="font-bold">Tuyệt hảo</p>
                                <p className="text-xs text-gray-500">Đánh giá</p>
                            </div>
                            <div className="bg-booking-blue text-white px-2 py-1 rounded rounded-tl-none font-bold">
                                {'8.8'}
                            </div>
                        </div>
                    </div>
                    <div className="text-yellow-400 text-sm mb-1">
                        {'★★★★☆'}
                    </div>
                    <p className="text-blue-600 text-sm underline cursor-pointer mb-2">{hotel.address.fullAddress} - Xem trên bản đồ</p>
                    
                    <div className="text-sm border-l-2 border-gray-300 pl-2 mb-2">
                        <p className="font-bold">{hotel.cheapestRoomType.name}</p>
                        <p className="text-green-600">✓ Hủy miễn phí</p>
                        <p className="text-green-600">✓ Không cần thanh toán trước</p>
                    </div>
                </div>
            </div>
            <div className="flex flex-col justify-end items-end min-w-[150px]">
                <p className="text-xs text-gray-500">1 đêm, 2 người lớn</p>
                <p className="text-2xl font-bold">VND {hotel.cheapestRoomType.pricePerNight.toLocaleString('vi-VN')}</p>
                <p className="text-xs text-gray-500 mb-4">+VND 0 thuế và phí</p>
                <p className="text-green-600">
                    ✓ Còn {hotel.cheapestRoomType.availableRooms} phòng
                </p>
                <button className="bg-booking-blue text-white px-4 py-2 rounded font-bold hover:bg-blue-800 w-full">
                    Xem phòng trống
                </button>
            </div>
        </div>
    );
}