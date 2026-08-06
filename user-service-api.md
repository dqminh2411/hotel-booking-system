# Admin API — Quản lý Tenant & Subscription (user-service)

> Dùng để copy-paste thủ công vào Bruno (hoặc Postman/Insomnia). Mỗi request có: Method, URL, Headers, Query params, Body (nếu có).
>
> **Base URL:** `http://localhost:5000` (không có context-path, port lấy từ `server.port`, mặc định 5000)
>
> **Auth:** Bearer JWT (Keycloak). Tạo 1 Environment trong Bruno với biến `base_url` và `access_token`, rồi dùng `{{base_url}}` / `{{access_token}}` trong các request bên dưới — hoặc thay trực tiếp bằng giá trị thật khi paste.
>
> Header dùng cho **mọi** request (trừ khi ghi chú khác):
> ```
> Authorization: Bearer {{access_token}}
> ```
> Với các request có Body, thêm:
> ```
> Content-Type: application/json
> ```

---

## 1. Admin — Tenants
Base path: `/api/admin/tenants` · Role bắt buộc: **PLATFORM_ADMIN**

### 1.1 Lấy danh sách tenant
- **Method:** `GET`
- **URL:** `{{base_url}}/api/admin/tenants`
- **Query params:**
  | Key | Ví dụ | Bắt buộc | Ghi chú |
  |---|---|---|---|
  | page | 0 | không | min = 0 |
  | size | 10 | không | min = 10, max = 30 |
  | status | ACTIVE | không | `ACTIVE` \| `SUSPENDED` |
  | search | (rỗng) | không | tìm theo tên tenant |
- **Body:** không có

### 1.2 Tạo tenant mới
- **Method:** `POST`
- **URL:** `{{base_url}}/api/admin/tenants`
- **Body (JSON):**
```json
{
  "ownerId": "22222222-2222-2222-2222-222222222222",
  "name": "Khach San Hoang Gia"
}
```
- `ownerId` (UUID, bắt buộc): userId của HOTEL_OWNER sở hữu tenant
- `name` (String, bắt buộc, not blank)

### 1.3 Lấy chi tiết tenant
- **Method:** `GET`
- **URL:** `{{base_url}}/api/admin/tenants/{{tenantId}}`
- **Body:** không có
- Trả về gồm `activeSubscription` nếu tenant đang có gói active

### 1.4 Cập nhật tenant
- **Method:** `PATCH`
- **URL:** `{{base_url}}/api/admin/tenants/{{tenantId}}`
- **Body (JSON):**
```json
{
  "name": "Khach San Hoang Gia Sai Gon",
  "status": "ACTIVE"
}
```
- `name`: not blank, **`@Size(min = 20)` — tên phải ≥ 20 ký tự** (validation hiện tại trong code, nên chú ý khi test, có thể là bug cần rà lại)
- `status`: `ACTIVE` \| `SUSPENDED`

### 1.5 Xoá (mềm) tenant
- **Method:** `DELETE`
- **URL:** `{{base_url}}/api/admin/tenants/{{tenantId}}`
- **Body:** không có
- Xoá mềm (`is_deleted`), không xoá vật lý

---

## 2. Tenant Subscriptions
Base path: `/api/tenants` · Role: **PLATFORM_ADMIN** hoặc **HOTEL_OWNER** (riêng mục 2.5 chỉ PLATFORM_ADMIN)

### 2.1 Lịch sử subscription của 1 tenant
- **Method:** `GET`
- **URL:** `{{base_url}}/api/tenants/{{tenantId}}/subscriptions`
- **Query params:**
  | Key | Ví dụ | Ghi chú |
  |---|---|---|
  | page | 0 | min = 0 |
  | size | 10 | min = 10, max = 30 |
- **Body:** không có
- Sort theo `startedAt` giảm dần. Nếu người gọi là `HOTEL_OWNER`, chỉ xem được tenant của chính mình.

### 2.2 Đăng ký subscription cho tenant
- **Method:** `POST`
- **URL:** `{{base_url}}/api/tenants/subscriptions`
- **Body (JSON):**
```json
{
  "tenantId": "11111111-1111-1111-1111-111111111111",
  "subscriptionId": "33333333-3333-3333-3333-333333333333"
}
```
- `tenantId` (UUID, bắt buộc)
- `subscriptionId` (UUID, bắt buộc): id của Subscription Plan

### 2.3 Chi tiết 1 tenant subscription
- **Method:** `GET`
- **URL:** `{{base_url}}/api/tenants/subscriptions/{{tenantSubscriptionId}}`
- **Body:** không có

### 2.4 Cập nhật trạng thái tenant subscription
- **Method:** `PATCH`
- **URL:** `{{base_url}}/api/tenants/subscriptions/{{tenantSubscriptionId}}`
- **Body (JSON):**
```json
{
  "status": "CANCELLED"
}
```
- `status` (bắt buộc): `ACTIVE` \| `EXPIRED` \| `CANCELLED` \| `SUSPENDED`

### 2.5 Xoá tenant subscription
- **Method:** `DELETE`
- **URL:** `{{base_url}}/api/tenants/subscriptions/{{tenantSubscriptionId}}`
- **Body:** không có
- Chỉ **PLATFORM_ADMIN** (khác các mục 2.1–2.4 vì HOTEL_OWNER không được xoá)

---

## 3. Admin — Subscription Plans
Base path: `/api/admin/subscription-plans` · Role bắt buộc: **PLATFORM_ADMIN**

### 3.1 Lấy danh sách subscription plan
- **Method:** `GET`
- **URL:** `{{base_url}}/api/admin/subscription-plans`
- **Query params:**
  | Key | Ví dụ | Ghi chú |
  |---|---|---|
  | page | 0 | min = 0 |
  | size | 10 | min = 10, max = 30 |
  | search | (rỗng) | tìm theo code/name |
  | billingCycle | MONTHLY | `MONTHLY` \| `YEARLY` |
- **Body:** không có

### 3.2 Tạo subscription plan
- **Method:** `POST`
- **URL:** `{{base_url}}/api/admin/subscription-plans`
- **Body (JSON):**
```json
{
  "code": "PRO_MONTHLY",
  "name": "Goi Pro",
  "description": "Goi danh cho khach san vua va nho",
  "price": 499000.00,
  "billingCycle": "MONTHLY"
}
```
- `code` (bắt buộc, ≤ 50 ký tự)
- `name` (bắt buộc, ≤ 255 ký tự)
- `description` (≤ 500 ký tự)
- `price` (bắt buộc, ≥ 0, tối đa 12 chữ số nguyên + 2 chữ số thập phân)
- `billingCycle` (bắt buộc): `MONTHLY` \| `YEARLY`

### 3.3 Chi tiết subscription plan
- **Method:** `GET`
- **URL:** `{{base_url}}/api/admin/subscription-plans/{{subscriptionPlanId}}`
- **Body:** không có

### 3.4 Cập nhật subscription plan
- **Method:** `PATCH`
- **URL:** `{{base_url}}/api/admin/subscription-plans/{{subscriptionPlanId}}`
- **Body (JSON):**
```json
{
  "name": "Goi Pro (Cap nhat)",
  "description": "Goi danh cho khach san vua va nho - da tang han muc",
  "price": 599000.00,
  "billingCycle": "MONTHLY"
}
```
- Tất cả field đều **optional** (partial update): `name` (≤255), `description` (≤500), `price` (≥0), `billingCycle` (`MONTHLY`\|`YEARLY`)

### 3.5 Xoá subscription plan
- **Method:** `DELETE`
- **URL:** `{{base_url}}/api/admin/subscription-plans/{{subscriptionPlanId}}`
- **Body:** không có

---

## Ghi chú chung khi test bằng Bruno
1. Tạo Collection mới, thêm 3 Folder tương ứng: `Admin - Tenants`, `Tenant Subscriptions`, `Admin - Subscription Plans`.
2. Tạo Environment với 2 biến `base_url` và `access_token`, lấy `access_token` từ Keycloak (endpoint `/realms/hotel-booking-system/protocol/openid-connect/token`).
3. Set Auth kiểu **Bearer Token** ở cấp Collection để không phải khai báo lại ở từng request.
4. Response luôn có dạng chung `ApiResponse`:
```json
{
  "code": 200,
  "message": "...",
  "data": { }
}
```