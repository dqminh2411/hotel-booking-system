# Frontend - Demo Đặt Phòng Khách Sạn

## Tổng quan

Frontend này là ứng dụng React 1 trang (single page) phục vụ cho hệ thống đặt phòng khách sạn.


## Công nghệ sử dụng

| Thành phần | Lựa chọn |
| --- | --- |
| Language | JavaScript |
| Framework | React 18, React Router, Axios |
| Build tool | Vite 5 |
| Styling | Tailwind CSS |
| Notification | Firebase Messaging |
| Đóng gói | npm |
| Runtime container | Nginx (serve static build) |

## Cấu trúc thư mục

```text

frontend/
├── Dockerfile
├── nginx.conf
├── index.html
├── package.json
├── postcss.config.js
├── tailwind.config.js
├── vite.config.js
├── readme.md
└── src
    ├── app
    │   ├── router.jsx
    │   └── providers.jsx
    ├── shared
    │   ├── api
    │   │   └── axiosClient.js
    │   ├── components
    │   ├── hooks
    │   ├── utils
    │   └── types
    ├── features
    │   ├── auth
    │   ├── hotel-search
    │   ├── hotel-detail
    │   ├── booking
    │   ├── payment
    │   ├── notification
    │   ├── tenant
    │   ├── staff
    │   └── admin
    └── pages
        ├── HomePage.jsx
        ├── LoginPage.jsx
        ├── RegisterPage.jsx
        ├── SearchPage.jsx
        ├── HotelDetailPage.jsx
        └── BookingPage.jsx
```

## Biến môi trường

Frontend sử dụng biến môi trường Vite:

| Biến | Mô tả | Mặc định |
| --- | --- | --- |
| `VITE_API_BASE_URL` | Base URL của API Gateway | `http://localhost:8080` |
| `VITE_GOOGLE_CLIENT_ID` | Google OAuth Web Client ID | Trống |
| `VITE_GOOGLE_AUTH_PATH` | Endpoint đăng nhập Google | `/api/users/oauth/google` |

Ví dụ trong `.env` (tại root project):

```dotenv
VITE_API_BASE_URL=http://localhost:8080
VITE_GOOGLE_CLIENT_ID=your-google-web-client-id
VITE_GOOGLE_AUTH_PATH=/api/users/oauth/google
```

## Chạy local

```bash
cd frontend
npm install
npm run dev
```

Mặc định Vite chạy tại `http://localhost:5173`.

## Chạy bằng Docker Compose

Từ root project:

```bash
docker compose up frontend --build
```

Frontend expose cổng `3000`.

## Luồng API

### 1) Đặt phòng

- Endpoint: `POST /place-booking`
- Payload gửi lên backend:

```json
{
  "userId": "10000000-0000-0000-0000-000000000002",
  "hotelId": "HT-001",
  "roomTypeList": [
    { "roomTypeId": "RT-001", "quantity": 1, "price": 1500000 }
  ],
  "checkin": "2026-06-01",
  "checkout": "2026-06-03",
  "numAdults": 2,
  "totalAmount": 3000000,
  "currency": "VND",
  "paymentMethod": "CREDIT_CARD",
  "paymentToken": "tok_xxxxx",
  "idempotencyKey": "uuid-v4"
}
```

## Quy tắc tính tổng tiền

`totalAmount = tong( base_price_per_night * quantity * so_dem )`

Trong đó:

- `so_dem = checkout - checkin` (theo ngày)
- Tiền tệ cố định: `VND`

## Dữ liệu demo cố định

- Khách sạn: `HT-001` - Marriott Hanoi
- User: `10000000-0000-0000-0000-000000000002` - Tran Thi Binh
- 3 room types: `RT-001`, `RT-002`, `RT-003`

## Ghi chú

- Đây là frontend demo nên thông tin thẻ (card number, CVV, ...) chỉ dùng để nhập UI, không gửi trực tiếp lên backend.
- Frontend tạo token thanh toán giả (`tok_...`) trước khi submit.
- Mỗi lần bấm **Book Now** sẽ tạo `idempotencyKey` mới để phục vụ idempotency phía backend.

