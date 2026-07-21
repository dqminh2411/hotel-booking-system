import { useState } from 'react';

export default function FilterSidebar({ onFilterChange }) {
    // State cho thanh trượt giá
    const [minPrice, setMinPrice] = useState(0);
    const [maxPrice, setMaxPrice] = useState(10000000); // Giả sử max là 10.000.000 VNĐ

    const handleSortChange = (e) => {
        onFilterChange('sortBy', e.target.value);
    };

    // Áp dụng giá khi người dùng kéo xong (hoặc có thể dùng useEffect có debounce)
    const handleApplyPrice = () => {
        onFilterChange('minPrice', minPrice.toString());
        onFilterChange('maxPrice', maxPrice.toString());
    };

    const handleResetPrice = () => {
        setMinPrice(0);
        setMaxPrice(10000000);
        onFilterChange('minPrice', '');
        onFilterChange('maxPrice', '');
    };

    return (
        <div className="bg-white border rounded p-4 shadow-sm w-full md:w-64">
            <h3 className="font-bold text-lg mb-4">Chọn lọc theo:</h3>
            
            <div className="mb-6">
                <h4 className="font-semibold mb-2">Sắp xếp</h4>
                <select className="w-full border p-2 rounded" onChange={handleSortChange}>
                    <option value="">Lựa chọn hàng đầu</option>
                    <option value="price_asc">Giá tăng dần</option>
                    <option value="price_desc">Giá giảm dần</option>
                </select>
            </div>

            <div className="mb-6">
                <h4 className="font-semibold mb-2">Ngân sách của bạn (mỗi đêm)</h4>
                
                <div className="flex flex-col gap-2 mb-2">
                    <div>
                        <label className="text-sm text-gray-600">Giá tối thiểu: {Number(minPrice).toLocaleString('vi-VN')} VNĐ</label>
                        <input 
                            type="range" 
                            min="0" 
                            max="10000000" 
                            step="100000" 
                            value={minPrice} 
                            onChange={(e) => setMinPrice(Number(e.target.value))}
                            onMouseUp={handleApplyPrice}
                            onTouchEnd={handleApplyPrice}
                            className="w-full"
                        />
                    </div>
                    <div>
                        <label className="text-sm text-gray-600">Giá tối đa: {Number(maxPrice).toLocaleString('vi-VN')} VNĐ</label>
                        <input 
                            type="range" 
                            min="0" 
                            max="10000000" 
                            step="100000" 
                            value={maxPrice} 
                            onChange={(e) => setMaxPrice(Number(e.target.value))}
                            onMouseUp={handleApplyPrice}
                            onTouchEnd={handleApplyPrice}
                            className="w-full"
                        />
                    </div>
                </div>

                <button 
                    onClick={handleResetPrice}
                    className="text-sm text-blue-600 hover:underline mt-1"
                >
                    Khôi phục lại giá
                </button>
            </div>
            
            <div>
                <h4 className="font-semibold mb-2">Tiện ích phổ biến</h4>
                <label className="flex items-center gap-2 mb-1 cursor-pointer">
                    <input type="checkbox" /> Hồ bơi
                </label>
                <label className="flex items-center gap-2 mb-1 cursor-pointer">
                    <input type="checkbox" /> Wifi miễn phí
                </label>
            </div>
        </div>
    );
}