export default function FilterSidebar({ onFilterChange }) {
    const handleSortChange = (e) => {
        onFilterChange('sortBy', e.target.value);
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
                <label className="flex items-center gap-2 mb-1 cursor-pointer">
                    <input type="radio" name="price" onChange={() => {onFilterChange('minPrice', '0'); onFilterChange('maxPrice', '1000000');}} /> Dưới 1.000.000 VNĐ
                </label>
                <label className="flex items-center gap-2 mb-1 cursor-pointer">
                    <input type="radio" name="price" onChange={() => {onFilterChange('minPrice', '1000000'); onFilterChange('maxPrice', '3000000');}} /> 1.000.000 - 3.000.000 VNĐ
                </label>
                <label className="flex items-center gap-2 mb-1 cursor-pointer">
                    <input type="radio" name="price" onChange={() => {onFilterChange('minPrice', '3000000'); onFilterChange('maxPrice', '');}} /> Trên 3.000.000 VNĐ
                </label>
                <label className="flex items-center gap-2 mb-1 cursor-pointer text-blue-600">
                    <input type="radio" name="price" onChange={() => {onFilterChange('minPrice', ''); onFilterChange('maxPrice', '');}} /> Bỏ lọc giá
                </label>
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