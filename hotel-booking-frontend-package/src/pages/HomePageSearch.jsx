import SearchBar from '../components/SearchBar';

export default function HomePageSearch() {
    return (
        <div>
            <div className="bg-booking-blue text-white pt-16 pb-24 px-4 relative">
                <div className="container mx-auto">
                    <h1 className="text-4xl md:text-5xl font-bold mb-4">Tìm chỗ nghỉ tiếp theo</h1>
                    <p className="text-xl md:text-2xl mb-10">Tìm ưu đãi khách sạn, chỗ nghỉ dạng nhà và nhiều hơn nữa...</p>
                </div>
                
                {/* Search Bar Container */}
                <div className="container mx-auto absolute -bottom-7 left-0 right-0 px-4">
                    <SearchBar />
                </div>
            </div>
            
            <div className="container mx-auto mt-20 p-4">
                <h2 className="text-2xl font-bold mb-4">Ưu đãi</h2>
                <div className="bg-white rounded-lg shadow border p-6 max-w-2xl">
                    <h3 className="font-bold text-lg mb-2">Không điều kiện ràng buộc. An tâm nghỉ dưỡng.</h3>
                    <p className="text-gray-600 mb-4">Tiết kiệm 15% cho một số chỗ nghỉ khi đặt ngay, vi vu liền tay.</p>
                    <button className="bg-booking-blue text-white px-4 py-2 rounded font-semibold">Tiết kiệm ngay</button>
                </div>
            </div>
        </div>
    );
}