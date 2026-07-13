# Hotel Booking Frontend

Đây là frontend đơn giản mô phỏng giao diện tìm kiếm của Booking.com. Dự án sử dụng React, Vite, và Tailwind CSS.

## Hướng dẫn cài đặt và chạy

1. Cài đặt dependencies:
   ```bash
   npm install
   ```
2. Chạy server phát triển:
   ```bash
   npm run dev
   ```

## Hướng dẫn kết nối Backend (API)

Hiện tại dự án đang sử dụng Mock Data nằm trong `src/data/mockData.js`. Logic tìm kiếm giả lập nằm ở `src/services/hotel.service.js`.

Để kết nối với Backend thật sau này:

1. Mở file `src/api/axios.js` và cập nhật `baseURL` trỏ đến API thật của bạn (ví dụ: `http://localhost:8080`).
2. Mở file `src/services/hotel.service.js` và thay thế nội dung hàm giả lập bằng lời gọi API thông qua Axios.

Ví dụ hàm `searchHotels` kết nối BE:

```javascript
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
```

Các params được UI truyền xuống bao gồm: `locationCode`, `checkinDate`, `checkoutDate`, `guestNum`, `roomNum`, `minPrice`, `maxPrice`, `amenityIds`, `sortBy`.

3. Đảm bảo dữ liệu khách sạn BE trả về có các trường ánh xạ đúng với những gì UI đang hiển thị trong `HotelCard.jsx` (như `name`, `address`, `stars`, `rating`, `lowestPrice`, `topAmenities`...).