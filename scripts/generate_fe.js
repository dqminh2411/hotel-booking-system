const fs = require('fs');
const path = require('path');

const baseDir = path.join(__dirname, 'hotel-booking-frontend-package');

function ensureDir(dir) {
    if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, { recursive: true });
    }
}

ensureDir(baseDir);
ensureDir(path.join(baseDir, 'src'));
ensureDir(path.join(baseDir, 'src/api'));
ensureDir(path.join(baseDir, 'src/data'));
ensureDir(path.join(baseDir, 'src/services'));
ensureDir(path.join(baseDir, 'src/components'));
ensureDir(path.join(baseDir, 'src/pages'));

const files = {
    'package.json': `{
  "name": "hotel-booking-frontend",
  "private": true,
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "axios": "^1.6.0",
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.20.0"
  },
  "devDependencies": {
    "@vitejs/plugin-react": "^4.2.0",
    "autoprefixer": "^10.4.16",
    "postcss": "^8.4.31",
    "tailwindcss": "^3.3.5",
    "vite": "^5.0.0"
  }
}`,
    'vite.config.js': `import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
})`,
    'tailwind.config.js': `/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,jsx}",
  ],
  theme: {
    extend: {
      colors: {
        'booking-blue': '#003580',
        'booking-yellow': '#feba02',
      }
    },
  },
  plugins: [],
}`,
    'postcss.config.js': `export default {
  plugins: {
    tailwindcss: {},
    autoprefixer: {},
  },
}`,
    'index.html': `<!doctype html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Hotel Booking Search</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.jsx"></script>
  </body>
</html>`,
    'README.md': `# Hotel Booking Frontend

Đây là frontend đơn giản mô phỏng giao diện tìm kiếm của Booking.com. Dự án sử dụng React, Vite, và Tailwind CSS.

## Hướng dẫn cài đặt và chạy

1. Cài đặt dependencies:
   \`\`\`bash
   npm install
   \`\`\`
2. Chạy server phát triển:
   \`\`\`bash
   npm run dev
   \`\`\`

## Hướng dẫn kết nối Backend (API)

Hiện tại dự án đang sử dụng Mock Data nằm trong \`src/data/mockData.js\`. Logic tìm kiếm giả lập nằm ở \`src/services/hotel.service.js\`.

Để kết nối với Backend thật sau này:

1. Mở file \`src/api/axios.js\` và cập nhật \`baseURL\` trỏ đến API thật của bạn (ví dụ: \`http://localhost:8080\`).
2. Mở file \`src/services/hotel.service.js\` và thay thế nội dung hàm giả lập bằng lời gọi API thông qua Axios.

Ví dụ hàm \`searchHotels\` kết nối BE:

\`\`\`javascript
import axiosClient from '../api/axios';

export const hotelService = {
    searchHotels: async (params) => {
        try {
            // BE của bạn cần xử lý các query params này
            const response = await axiosClient.get('/api/hotels', { params });
            // Trả về data (giả định BE trả về mảng kết quả)
            return response.data;
        } catch (error) {
            console.error('Error fetching hotels:', error);
            throw error;
        }
    }
}
\`\`\`

Các params được UI truyền xuống bao gồm: \`locationCode\`, \`checkinDate\`, \`checkoutDate\`, \`guestNum\`, \`roomNum\`, \`minPrice\`, \`maxPrice\`, \`amenityIds\`, \`sortBy\`.

3. Đảm bảo dữ liệu khách sạn BE trả về có các trường ánh xạ đúng với những gì UI đang hiển thị trong \`HotelCard.jsx\` (như \`name\`, \`address\`, \`stars\`, \`rating\`, \`lowestPrice\`, \`topAmenities\`...).`,
    'src/main.jsx': `import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.jsx'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)`,
    'src/index.css': `@tailwind base;
@tailwind components;
@tailwind utilities;

body {
  background-color: #f5f5f5;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
}`,
    'src/App.jsx': `import { BrowserRouter, Routes, Route } from 'react-router-dom';
import HomePage from './pages/HomePage';
import SearchResultsPage from './pages/SearchResultsPage';
import Header from './components/Header';

function App() {
  return (
    <BrowserRouter>
      <Header />
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/search" element={<SearchResultsPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;`,
    'src/api/axios.js': `import axios from 'axios';

const axiosClient = axios.create({
    baseURL: 'http://localhost:8080', // TODO: Thay đổi URL này thành URL Backend của bạn
    headers: {
        'Content-Type': 'application/json'
    }
});

// Thêm interceptors nếu cần thiết sau này
axiosClient.interceptors.response.use(
    (response) => response,
    (error) => Promise.reject(error)
);

export default axiosClient;`,
    'src/data/mockData.js': `const cities = ['Hà Nội', 'Hồ Chí Minh', 'Đà Nẵng', 'Hải Phòng', 'Nha Trang', 'Đà Lạt', 'Huế', 'Vũng Tàu', 'Phú Quốc', 'Cần Thơ'];

const amenitiesList = [
    { id: 'a1', name: 'Hồ bơi' },
    { id: 'a2', name: 'Bữa sáng miễn phí' },
    { id: 'a3', name: 'Wifi miễn phí' },
    { id: 'a4', name: 'Bãi đậu xe' },
    { id: 'a5', name: 'Spa' },
    { id: 'a6', name: 'Phòng gym' }
];

const generateMockHotels = () => {
    let hotels = [];
    let idCounter = 1;
    cities.forEach(city => {
        for (let i = 1; i <= 10; i++) {
            hotels.push({
                id: idCounter.toString(),
                name: \`Khách sạn \${city} \${i} Sao\`,
                address: \`Trung tâm \${city}\`,
                locationCode: city,
                stars: Math.floor(Math.random() * 3) + 3, // 3 - 5 sao
                rating: (Math.random() * 2 + 7).toFixed(1), // 7.0 - 9.9
                topAmenities: [amenitiesList[0], amenitiesList[2]],
                lowestPrice: Math.floor(Math.random() * 2000000) + 500000,
                cheapestRoomType: 'Phòng Tiêu Chuẩn',
                image: 'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=500&q=80'
            });
            idCounter++;
        }
    });
    return hotels;
};

export const mockHotels = generateMockHotels();
export const mockCities = cities;
export const mockAmenities = amenitiesList;`,
    'src/services/hotel.service.js': `import { mockHotels } from '../data/mockData';

export const hotelService = {
    searchHotels: async (params) => {
        // GIẢ LẬP LỜI GỌI API
        // Về sau: return await axiosClient.get('/api/hotels', { params }).then(res => res.data);
        return new Promise((resolve) => {
            setTimeout(() => {
                let filtered = [...mockHotels];

                if (params.locationCode) {
                    filtered = filtered.filter(h => h.locationCode.toLowerCase() === params.locationCode.toLowerCase());
                }

                if (params.minPrice) {
                    filtered = filtered.filter(h => h.lowestPrice >= parseInt(params.minPrice));
                }

                if (params.maxPrice) {
                    filtered = filtered.filter(h => h.lowestPrice <= parseInt(params.maxPrice));
                }

                if (params.sortBy) {
                    if (params.sortBy === 'price_asc') {
                        filtered.sort((a, b) => a.lowestPrice - b.lowestPrice);
                    } else if (params.sortBy === 'price_desc') {
                        filtered.sort((a, b) => b.lowestPrice - a.lowestPrice);
                    }
                }

                resolve(filtered);
            }, 500); // Giả lập độ trễ mạng 500ms
        });
    }
};`,
    'src/components/Header.jsx': `import { Link } from 'react-router-dom';

export default function Header() {
    return (
        <header className="bg-booking-blue text-white p-4 flex justify-between items-center shadow-md">
            <div className="container mx-auto flex justify-between items-center">
                <Link to="/" className="text-2xl font-bold">Booking.com Clone</Link>
                <div className="flex gap-4">
                    <button className="bg-white text-booking-blue px-4 py-1 rounded font-semibold">Đăng ký</button>
                    <button className="bg-white text-booking-blue px-4 py-1 rounded font-semibold">Đăng nhập</button>
                </div>
            </div>
        </header>
    );
}`,
    'src/components/SearchBar.jsx': `import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { mockCities } from '../data/mockData';

export default function SearchBar({ initialValues }) {
    const [location, setLocation] = useState(initialValues?.locationCode || '');
    const [checkin, setCheckin] = useState(initialValues?.checkinDate || '');
    const [checkout, setCheckout] = useState(initialValues?.checkoutDate || '');
    const [guests, setGuests] = useState(initialValues?.guestNum || 2);
    const [rooms, setRooms] = useState(initialValues?.roomNum || 1);
    const navigate = useNavigate();

    const handleSearch = () => {
        if (checkin && checkout && new Date(checkin) >= new Date(checkout)) {
            alert('Ngày trả phòng phải sau ngày nhận phòng!');
            return;
        }

        const params = new URLSearchParams();
        if (location) params.append('locationCode', location);
        if (checkin) params.append('checkinDate', checkin);
        if (checkout) params.append('checkoutDate', checkout);
        params.append('guestNum', guests);
        params.append('roomNum', rooms);

        navigate(\`/search?\${params.toString()}\`);
    };

    return (
        <div className="bg-booking-yellow p-1 rounded-md shadow-lg flex flex-col md:flex-row gap-1">
            <div className="flex-1 bg-white flex items-center px-3 py-2 rounded">
                <input 
                    type="text" 
                    placeholder="Bạn muốn đến đâu?" 
                    className="w-full outline-none text-gray-700"
                    value={location}
                    onChange={(e) => setLocation(e.target.value)}
                    list="cities"
                />
                <datalist id="cities">
                    {mockCities.map(city => <option key={city} value={city} />)}
                </datalist>
            </div>
            
            <div className="flex-1 bg-white flex items-center px-3 py-2 rounded gap-2">
                <input 
                    type="date" 
                    className="w-full outline-none text-gray-700" 
                    value={checkin}
                    onChange={(e) => setCheckin(e.target.value)}
                />
                <span>-</span>
                <input 
                    type="date" 
                    className="w-full outline-none text-gray-700"
                    value={checkout}
                    onChange={(e) => setCheckout(e.target.value)}
                />
            </div>

            <div className="flex-1 bg-white flex items-center px-3 py-2 rounded">
                <span className="text-gray-700 w-full">
                    {guests} người lớn · 0 trẻ em · {rooms} phòng
                </span>
            </div>

            <button 
                onClick={handleSearch}
                className="bg-booking-blue text-white px-8 py-2 rounded font-bold text-lg hover:bg-blue-800 transition"
            >
                Tìm
            </button>
        </div>
    );
}`,
    'src/components/FilterSidebar.jsx': `export default function FilterSidebar({ onFilterChange }) {
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
}`,
    'src/components/HotelCard.jsx': `export default function HotelCard({ hotel }) {
    return (
        <div className="bg-white border rounded p-4 shadow-sm flex flex-col md:flex-row gap-4 mb-4">
            <img src={hotel.image} alt={hotel.name} className="w-full md:w-56 h-48 object-cover rounded" />
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
                                {hotel.rating}
                            </div>
                        </div>
                    </div>
                    <div className="text-yellow-400 text-sm mb-1">
                        {'★'.repeat(hotel.stars)}{'☆'.repeat(5 - hotel.stars)}
                    </div>
                    <p className="text-blue-600 text-sm underline cursor-pointer mb-2">{hotel.address} - Xem trên bản đồ</p>
                    
                    <div className="text-sm border-l-2 border-gray-300 pl-2 mb-2">
                        <p className="font-bold">{hotel.cheapestRoomType}</p>
                        <p className="text-green-600">✓ Hủy miễn phí</p>
                        <p className="text-green-600">✓ Không cần thanh toán trước</p>
                    </div>
                </div>
            </div>
            <div className="flex flex-col justify-end items-end min-w-[150px]">
                <p className="text-xs text-gray-500">1 đêm, 2 người lớn</p>
                <p className="text-2xl font-bold">VND {hotel.lowestPrice.toLocaleString('vi-VN')}</p>
                <p className="text-xs text-gray-500 mb-4">+VND 0 thuế và phí</p>
                <button className="bg-booking-blue text-white px-4 py-2 rounded font-bold hover:bg-blue-800 w-full">
                    Xem phòng trống
                </button>
            </div>
        </div>
    );
}`,
    'src/pages/HomePage.jsx': `import SearchBar from '../components/SearchBar';

export default function HomePage() {
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
}`,
    'src/pages/SearchResultsPage.jsx': `import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import SearchBar from '../components/SearchBar';
import FilterSidebar from '../components/FilterSidebar';
import HotelCard from '../components/HotelCard';
import { hotelService } from '../services/hotel.service';

export default function SearchResultsPage() {
    const [searchParams, setSearchParams] = useSearchParams();
    const [hotels, setHotels] = useState([]);
    
    // Parse URL params to Object
    const currentParams = Object.fromEntries([...searchParams]);

    useEffect(() => {
        const fetchHotels = async () => {
            const data = await hotelService.searchHotels(currentParams);
            setHotels(data);
        };
        fetchHotels();
    }, [searchParams]);

    const handleFilterChange = (key, value) => {
        if (value) {
            searchParams.set(key, value);
        } else {
            searchParams.delete(key);
        }
        setSearchParams(searchParams);
    };

    return (
        <div className="bg-gray-100 min-h-screen">
            <div className="bg-booking-blue py-6 px-4">
                <div className="container mx-auto">
                    <SearchBar initialValues={currentParams} />
                </div>
            </div>
            
            <div className="container mx-auto py-6 px-4 flex flex-col md:flex-row gap-4">
                <div className="w-full md:w-auto">
                    <FilterSidebar onFilterChange={handleFilterChange} />
                </div>
                
                <div className="flex-1">
                    <h2 className="text-2xl font-bold mb-4">
                        {currentParams.locationCode 
                            ? \`Tìm thấy \${hotels.length} chỗ nghỉ tại \${currentParams.locationCode}\`
                            : \`Tất cả \${hotels.length} chỗ nghỉ\`}
                    </h2>
                    
                    {hotels.map(hotel => (
                        <HotelCard key={hotel.id} hotel={hotel} />
                    ))}
                    
                    {hotels.length === 0 && (
                        <div className="bg-white p-8 text-center rounded border">
                            Không tìm thấy khách sạn nào phù hợp.
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}`
};

for (const [relativePath, content] of Object.entries(files)) {
    fs.writeFileSync(path.join(baseDir, relativePath), content, 'utf8');
}
console.log('Package created successfully.');
