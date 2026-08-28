# Phân tích và thiết kế Hotel Service

## 1. Thiết kế dữ liệu (`hotel_db`)

### 1.1. Danh sách bảng và thuộc tính

**Bảng `hotels`**

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | PK | Định danh khách sạn |
| tenant_id | UUID | NOT NULL | Tham chiếu logic tới `tenants.id` ở User Service, không FK vật lý xuyên service |
| name | VARCHAR(255) | NOT NULL | Tên khách sạn |
| description | TEXT | NULL | Mô tả khách sạn |
| address | VARCHAR(500) | NOT NULL | Địa chỉ chi tiết |
| province_code | VARCHAR(2) | FK → provinces.code, NOT NULL | Mã tỉnh/thành phố |
| district_code | VARCHAR(3) | FK → districts.code, NOT NULL | Mã quận/huyện |
| ward_code | VARCHAR(5) | FK → wards.code, NOT NULL | Mã xã/phường |
| status | ENUM | NOT NULL, DEFAULT 'PENDING' | PENDING, APPROVED, SUSPENDED |
| is_deleted | BOOLEAN | NOT NULL, DEFAULT false | Cờ xóa mềm |
| created_at | TIMESTAMP | NOT NULL | Thời điểm tạo |
| updated_at | TIMESTAMP | NOT NULL | Thời điểm cập nhật |

**Bảng `provinces`**

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
| --- | --- | --- | --- |
| code | VARCHAR(2) | PK | Mã tỉnh/thành phố |
| name | VARCHAR(100) | NOT NULL | Tên tiếng Việt |
| name_en | VARCHAR(100) | NULL | Tên tiếng Anh |
| full_name | VARCHAR(255) | NULL | Tên đầy đủ |
| is_deleted | BOOLEAN | NOT NULL, DEFAULT false | Cờ xóa mềm |

**Bảng `districts`**

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
| --- | --- | --- | --- |
| code | VARCHAR(3) | PK | Mã quận/huyện |
| province_code | VARCHAR(2) | FK → provinces.code, NOT NULL | Mã tỉnh/thành phố cha |
| name | VARCHAR(100) | NOT NULL | Tên tiếng Việt |
| name_en | VARCHAR(100) | NULL | Tên tiếng Anh |
| full_name | VARCHAR(255) | NULL | Tên đầy đủ |
| is_deleted | BOOLEAN | NOT NULL, DEFAULT false | Cờ xóa mềm |

**Bảng `wards`**

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
| --- | --- | --- | --- |
| code | VARCHAR(5) | PK | Mã xã/phường |
| district_code | VARCHAR(3) | FK → districts.code, NOT NULL | Mã quận/huyện cha |
| name | VARCHAR(100) | NOT NULL | Tên tiếng Việt |
| name_en | VARCHAR(100) | NULL | Tên tiếng Anh |
| full_name | VARCHAR(255) | NULL | Tên đầy đủ |
| is_deleted | BOOLEAN | NOT NULL, DEFAULT false | Cờ xóa mềm |

**Bảng `hotel_images`**

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | PK | Định danh ảnh |
| hotel_id | UUID | FK → hotels.id, NOT NULL | Khách sạn sở hữu ảnh |
| url | VARCHAR(500) | NOT NULL | URL ảnh |
| is_cover | BOOLEAN | NOT NULL, DEFAULT false | Ảnh đại diện khách sạn |
| created_at | TIMESTAMP | NOT NULL | Thời điểm tạo |

**Bảng `amenities`**

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | PK | Định danh tiện ích |
| name | VARCHAR(100) | UNIQUE, NOT NULL | Wifi, Hồ bơi, Bãi đỗ xe... |
| scope | ENUM | NOT NULL | HOTEL, ROOM_TYPE, BOTH |
| is_deleted | BOOLEAN | NOT NULL, DEFAULT false | Cờ xóa mềm |

**Bảng `hotel_amenities`**

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
| --- | --- | --- | --- |
| hotel_id | UUID | PK, FK → hotels.id | Khách sạn |
| amenity_id | UUID | PK, FK → amenities.id | Tiện ích |
| is_deleted | BOOLEAN | NOT NULL, DEFAULT false | Cờ xóa mềm |

**Bảng `room_types`**

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | PK | Định danh loại phòng |
| hotel_id | UUID | FK → hotels.id, NOT NULL | Khách sạn sở hữu loại phòng |
| name | VARCHAR(255) | NOT NULL | Tên loại phòng |
| description | TEXT | NULL | Mô tả loại phòng |
| base_price_per_night | DECIMAL(12,2) | NOT NULL | Giá cơ bản mỗi đêm |
| max_guests  | SMALLINT | NOT NULL | Số khách tối đa |
| area | INT | NULL | Diện tích (m²) |
| bed_counts | INT | NOT NULL | Số giường |
| quantity | INT | NOT NULL | Tổng số phòng thuộc loại này |
| is_deleted | BOOLEAN | NOT NULL, DEFAULT false | Cờ xóa mềm |
| created_at | TIMESTAMP | NOT NULL | Thời điểm tạo |
| updated_at | TIMESTAMP | NOT NULL | Thời điểm cập nhật |

**Bảng `room_type_images`**

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | PK | Định danh ảnh |
| room_type_id | UUID | FK → room_types.id, NOT NULL | Loại phòng sở hữu ảnh |
| url | VARCHAR(500) | NOT NULL | URL ảnh |
| is_cover | BOOLEAN | NOT NULL, DEFAULT false | Ảnh đại diện loại phòng |
| created_at | TIMESTAMP | NOT NULL | Thời điểm tạo |

**Bảng `room_type_amenities`**

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
| --- | --- | --- | --- |
| room_type_id | UUID | PK, FK → room_types.id | Loại phòng |
| amenity_id | UUID | PK, FK → amenities.id | Tiện ích |
| is_deleted | BOOLEAN | NOT NULL, DEFAULT false | Cờ xóa mềm |

**Bảng `rooms`**

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | PK | Định danh phòng vật lý |
| room_type_id | UUID | FK → room_types.id, NOT NULL | Loại phòng |
| room_number | VARCHAR(20) | NOT NULL | Số phòng |
| floor | SMALLINT | NULL | Tầng |
| status | ENUM | NOT NULL, DEFAULT 'AVAILABLE' | AVAILABLE, OCCUPIED, CLEANING, MAINTENANCE |
| is_deleted | BOOLEAN | NOT NULL, DEFAULT false | Cờ xóa mềm |

> Nên có unique constraint: `(room_type_id, room_number)` hoặc tốt hơn là `(hotel_id, room_number)` nếu thêm `hotel_id` vào bảng `rooms`.

**Bảng `pricing_rules`**

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | PK | Định danh rule giá |
| room_type_id | UUID | FK → room_types.id, NOT NULL | Loại phòng áp dụng |
| type | ENUM | NOT NULL | SEASONAL, WEEKEND, SPECIAL |
| start_date | DATE | NOT NULL | Ngày bắt đầu |
| end_date | DATE | NOT NULL | Ngày kết thúc |
| adjustment_type | ENUM | NOT NULL | FIXED_PRICE, PERCENTAGE_INCREASE, AMOUNT_INCREASE |
| price_value | DECIMAL(12,2) | NOT NULL | Giá cố định hoặc giá trị điều chỉnh |
| priority | SMALLINT | NOT NULL | SPECIAL=3, WEEKEND=2, SEASONAL=1 |
| is_deleted | BOOLEAN | NOT NULL, DEFAULT false | Cờ xóa mềm |

**Bảng `policies`**
| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | PK | Định danh chính sách |
| hotel_id | UUID | FK → hotels.id, NOT NULL | Khách sạn áp dụng |
| type | ENUM | NOT NULL | CANCELATION, CHECKIN, CHECKOUT, SMOKING, PAYMENT, PETS, CHILDREN |
| description | TEXT | NULL | Mô tả chính sách |
| is_deleted | BOOLEAN | NOT NULL, DEFAULT false | Cờ xóa mềm |

### 1.2. Quan hệ

- `provinces` 1–N `districts`.
- `districts` 1–N `wards`.
- `provinces` 1–N `hotels`.
- `districts` 1–N `hotels`.
- `wards` 1–N `hotels`.
- `hotels` 1–N `hotel_images`.
- `hotels` N–N `amenities` qua `hotel_amenities`.
- `hotels` 1–N `room_types`.
- `room_types` 1–N `rooms`.
- `room_types` 1–N `room_type_images`.
- `room_types` N–N `amenities` qua `room_type_amenities`.
- `room_types` 1–N `pricing_rules`.
- `hotels` 1–N `policies`.

### 1.3. ERD

```mermaid
erDiagram
    PROVINCES ||--o{ DISTRICTS : "có nhiều"
    DISTRICTS ||--o{ WARDS : "có nhiều"

    PROVINCES ||--o{ HOTELS : "thuộc tỉnh"
    DISTRICTS ||--o{ HOTELS : "thuộc huyện"
    WARDS ||--o{ HOTELS : "thuộc xã"

    HOTELS ||--o{ HOTEL_IMAGES : "có nhiều ảnh"
    HOTELS ||--o{ HOTEL_AMENITIES : "có tiện ích"
    AMENITIES ||--o{ HOTEL_AMENITIES : "được dùng bởi khách sạn"

    HOTELS ||--o{ ROOM_TYPES : "có nhiều loại phòng"
    ROOM_TYPES ||--o{ ROOMS : "gồm nhiều phòng"
    ROOM_TYPES ||--o{ ROOM_TYPE_IMAGES : "có nhiều ảnh"
    ROOM_TYPES ||--o{ ROOM_TYPE_AMENITIES : "có tiện ích"
    AMENITIES ||--o{ ROOM_TYPE_AMENITIES : "được dùng bởi loại phòng"
    ROOM_TYPES ||--o{ PRICING_RULES : "có cấu hình giá"
    HOTELS ||--o{ POLICIES : "có nhiều chính sách"

    PROVINCES {
        string code PK
        string name
        string name_en
        string full_name
        boolean is_deleted
    }

    DISTRICTS {
        string code PK
        string province_code FK
        string name
        string name_en
        string full_name
        boolean is_deleted
    }

    WARDS {
        string code PK
        string district_code FK
        string name
        string name_en
        string full_name
        boolean is_deleted
    }

    HOTELS {
        UUID id PK
        UUID tenant_id
        string name
        string description
        string address
        string province_code FK
        string district_code FK
        string ward_code FK
        string status
        boolean is_deleted
        timestamp created_at
        timestamp updated_at

    }

    HOTEL_IMAGES {
        UUID id PK
        UUID hotel_id FK
        string url
        boolean is_cover
        timestamp created_at
    }

    POLICIES{
        UUID id PK
        UUID hotel_id FK
        string type
        string description
        boolean is_deleted
    }

    AMENITIES {
        UUID id PK
        string name
        string scope
        boolean is_deleted
    }

    HOTEL_AMENITIES {
        UUID hotel_id PK, FK
        UUID amenity_id PK, FK
        boolean is_deleted
    }

    ROOM_TYPES {
        UUID id PK
        UUID hotel_id FK
        string name
        string description
        decimal base_price_per_night
        int bed_counts
        int quantity
        int max_guests
        int area
        boolean is_deleted
        timestamp created_at
        timestamp updated_at
    }

    ROOM_TYPE_IMAGES {
        UUID id PK
        UUID room_type_id FK
        string url
        boolean is_cover
        timestamp created_at
    }

    ROOM_TYPE_AMENITIES {
        UUID room_type_id PK, FK
        UUID amenity_id PK, FK
        boolean is_deleted
    }

    ROOMS {
        UUID id PK
        UUID room_type_id FK
        string room_number
        smallint floor
        string status
        boolean is_deleted
    }

    PRICING_RULES {
        UUID id PK
        UUID room_type_id FK
        string type
        date start_date
        date end_date
        string adjustment_type
        decimal price_value
        smallint priority
        boolean is_deleted
    }


```

## 2. Tài nguyên API - base path `/api/hotels`

| Method | Endpoint | Mô tả | Response Code |
| --- | --- | --- | --- |
| GET | `/api/hotels` | Tìm kiếm khách sạn theo địa điểm/ngày/khách/giá/tiện ích | 200 OK, 400 Bad Request, 404 Not Found |
| GET | `/api/hotels/{id}` | Xem chi tiết khách sạn | 200 OK, 404 Not Found |
| GET | `/api/hotels/{id}/room-types` | Lấy danh sách loại phòng | 200 OK, 404 Not Found |
| GET | `/api/room-types/{id}` | Xem chi tiết loại phòng | 200 OK, 404 Not Found |
| | | | | 
| POST | `/api/hotels` | Owner tạo khách sạn mới | 201 Created, 400 Bad Request, 403 Forbidden |
| PUT | `/api/hotels/{id}` | Owner sửa thông tin khách sạn | 200 OK, 403 Forbidden, 404 Not Found |
| DELETE | `/api/hotels/{id}` | Owner xóa khách sạn | 204 No Content, 403 Forbidden, 409 Conflict (còn booking hiệu lực) |
| POST | `/api/hotels/{id}/images` | Tải lên hình ảnh khách sạn | 201 Created, 400 Bad Request, 413 Payload Too Large |
| DELETE | `/api/hotels/{id}/images/{imageId}` | Xóa hình ảnh | 204 No Content, 404 Not Found |
| PATCH | `/api/hotels/{id}` | Admin duyệt/khóa/kích hoạt khách sạn | 200 OK, 403 Forbidden, 404 Not Found |
| POST | `/api/hotels/{id}/room-types` | Tạo loại phòng | 201 Created, 400 Bad Request |
| PUT | `/api/room-types/{id}` | Cập nhật loại phòng | 200 OK, 404 Not Found |
| GET | `/api/room-types/{id}/availability` | **[internal]** Kiểm tra phòng trống theo khoảng ngày | 200 OK, 404 Not Found |
| POST | `/api/room-types/{id}/reserve` | **[internal]** Giữ chỗ tạm thời (gọi từ Place Booking Service) | 200 OK, 409 Conflict (hết phòng) |
| POST | `/api/room-types/{id}/release` | **[internal]** Giải phóng phòng đã giữ (compensating transaction) | 200 OK, 404 Not Found |
| POST | `/api/rooms` | Staff thêm phòng cụ thể | 201 Created, 400 Bad Request |
| PUT | `/api/rooms/{id}` | Staff sửa thông tin phòng | 200 OK, 404 Not Found |
| PUT | `/api/rooms/{id}/status` | Staff cập nhật trạng thái phòng | 200 OK, 404 Not Found |
| DELETE | `/api/rooms/{id}` | Staff xóa phòng | 204 No Content, 409 Conflict |
| POST | `/api/room-types/{id}/pricing-rules` | Owner tạo giá Seasonal/Weekend/Special | 201 Created, 400 Bad Request |
| GET | `/api/hotels/{id}/occupancy-stats` | Số liệu công suất phòng (phục vụ Owner Dashboard) | 200 OK, 403 Forbidden |
| GET | `/api/hotels/stats` | Tổng số khách sạn toàn nền tảng (phục vụ Admin Dashboard) | 200 OK, 403 Forbidden |


## 3. Sequence diagram theo use case

### 3.1. UC-08 - Tìm kiếm khách sạn

```mermaid
sequenceDiagram
    actor C as Customer
    participant GW as API Gateway
    participant HS as Hotel Service
    participant R as Redis Cache
    participant DB as PostgreSQL hotel_db

    C->>GW: GET /api/hotels?locationCode=&checkinDate=&checkoutDate=&guestNum=&roomNum=&minPrice=&maxPrice=&amenities=&sortBy=
    GW->>HS: Forward GET /api/hotels

    HS->>HS: Validate và chuẩn hóa query params
    note right of HS: checkinDate < checkoutDate<br/>guestNum > 0<br/>roomNum > 0<br/>minPrice <= maxPrice nếu có

    alt Params không hợp lệ
        HS-->>GW: 400 Bad Request
        GW-->>C: 400 Bad Request
    else Params hợp lệ
        HS->>HS: Tạo cache key theo locationCode + dates + guests + rooms + price + amenities
        HS->>R: GET hotel_search:{normalizedParamsHash}

        alt Cache hit
            R-->>HS: SearchHotelDbResult[]
        else Cache miss
            R-->>HS: null

            HS->>DB: 1 query tìm hotel + room_types còn trống + giá + policy + cover image
            note right of DB: Lọc theo locationCode, checkinDate, checkoutDate, <br/>guestNum, roomNum, min/max price,<br/>amenities, APPROVED, is_deleted=false, sắp xếp theo sortBy

            DB-->>HS: SearchHotelDbResult[]
            note right of HS: Mỗi hotel gồm hotelId, name, coverImage,<br/>address, policies, availableRoomTypes[]

            HS->>R: SET hotel_search:{normalizedParamsHash}, TTL ngắn
        end

        HS->>HS: Với mỗi hotel, giữ lại room type rẻ nhất
        note right of HS: Frontend chỉ cần phòng rẻ nhất ở màn tìm kiếm

        HS-->>GW: 200 OK [HotelSearchResponse]
        GW-->>C: 200 OK
    end
```

### 3.2. UC-09 - Xem chi tiết khách sạn

```mermaid
sequenceDiagram
    actor C as Customer
    participant GW as API Gateway
    participant HS as Hotel Service
    participant R as Redis Cache
    participant DB as PostgreSQL hotel_db

    C->>GW: GET /api/hotels/{id}?checkinDate=&checkoutDate=&guestNum=&roomNum=
    GW->>HS: Forward GET /api/hotels/{id}

    HS->>HS: Validate hotelId và query params
    note right of HS: checkinDate/checkOutDate dùng để tính phòng trống<br/>và giá áp dụng theo ngày

    alt Params không hợp lệ
        HS-->>GW: 400 Bad Request
        GW-->>C: 400 Bad Request
    else Params hợp lệ
        HS->>HS: Tạo cache key theo hotelId + dates + guests + rooms
        HS->>R: GET hotel_detail:{hotelId}:{normalizedParamsHash}

        alt Cache hit
            R-->>HS: HotelDetailDbResult
        else Cache miss
            R-->>HS: null

            HS->>DB: 1 query lấy chi tiết hotel + 5 ảnh + policies + amenities + available room types
            note right of DB: availableRoomTypes được lọc theo<br/>checkinDate, checkoutDate, guestNum, roomNum

            alt Không tìm thấy hotel
                DB-->>HS: empty
                HS-->>GW: 404 Not Found
                GW-->>C: 404 Not Found
            else Tìm thấy hotel
                DB-->>HS: HotelDetailDbResult
                note right of HS: hotelId, name, description, address,<br/>images[5], policies[], amenities[],<br/>availableRoomTypes[]

                HS->>R: SET hotel_detail:{hotelId}:{normalizedParamsHash}, TTL ngắn
                HS-->>GW: 200 OK HotelDetailResponse
                GW-->>C: 200 OK
            end
        end
    end
```

### 3.3. UC-29 - Lấy danh sách loại phòng của khách sạn

```mermaid
sequenceDiagram
    actor C as Customer
    participant GW as API Gateway
    participant HS as Hotel Service
    participant R as Redis Cache
    participant DB as PostgreSQL hotel_db

    C->>GW: GET /api/hotels/{id}/room-types
    GW->>HS: Forward GET /api/hotels/{id}/room-types

    HS->>R: GET hotel_room_types:{hotelId}

    alt Cache hit
        R-->>HS: RoomTypeSummaryDbResult[]
        HS-->>GW: 200 OK [RoomTypeSummaryResponse]
        GW-->>C: 200 OK
    else Cache miss
        R-->>HS: null

        HS->>DB: 1 query kiểm tra hotel và lấy toàn bộ room_types của hotel
        note right of DB: Dùng cho mục đích quản lý/xem danh sách loại phòng,<br/>không lọc theo ngày và phòng trống

        alt Không tìm thấy hotel
            DB-->>HS: empty
            HS-->>GW: 404 Not Found
            GW-->>C: 404 Not Found
        else Hotel tồn tại
            DB-->>HS: RoomTypeSummaryDbResult[]
            note right of HS: roomTypeId, name, basePrice, capacity,<br/>bedCount, bedType, coverImage, totalRooms

            HS->>R: SET hotel_room_types:{hotelId}, TTL trung bình
            HS-->>GW: 200 OK [RoomTypeSummaryResponse]
            GW-->>C: 200 OK
        end
    end
```

### 3.4. UC-30 - Xem chi tiết loại phòng


```mermaid
sequenceDiagram
    actor C as Customer
    participant GW as API Gateway
    participant HS as Hotel Service
    participant R as Redis Cache
    participant DB as PostgreSQL hotel_db

    C->>GW: GET /api/room-types/{id}
    GW->>HS: Forward GET /api/room-types/{id}

    HS->>R: GET room_type_detail:{roomTypeId}

    alt Cache hit
        R-->>HS: RoomTypeDetailDbResult
        HS-->>GW: 200 OK RoomTypeDetailResponse
        GW-->>C: 200 OK
    else Cache miss
        R-->>HS: null

        HS->>DB: 1 query lấy chi tiết room_type + amenities + 5 ảnh đầu
        note right of DB: Lọc room_type is_deleted=false<br/>amenities is_deleted=false<br/>images phân trang: limit 5

        alt Không tìm thấy room_type
            DB-->>HS: empty
            HS-->>GW: 404 Not Found
            GW-->>C: 404 Not Found
        else Tìm thấy room_type
            DB-->>HS: RoomTypeDetailDbResult
            note right of HS: roomTypeId, name, description,<br/>amenities[], images[5]

            HS->>R: SET room_type_detail:{roomTypeId}, TTL trung bình
            HS-->>GW: 200 OK RoomTypeDetailResponse
            GW-->>C: 200 OK
        end
    end
```

## UC-31 — Tạo khách sạn mới (đã chỉnh sửa)

```mermaid
sequenceDiagram
    autonumber

    actor Owner as HOTEL_OWNER
    participant Keycloak
    participant Client as Postman / Frontend
    participant Gateway
    participant Security as Spring Security (hotel-service)
    participant Controller as HotelController
    participant Service as HotelServiceImpl
    participant Utils as SecurityUtils
    participant Location as Province/District/Ward Repository
    participant Amenity as AmenityRepository
    participant HotelRepo as HotelRepository
    participant DB as PostgreSQL
    participant Event as ApplicationEventPublisher
    participant Listener as HotelKafkaEventPublisher
    participant Kafka

    Owner->>Keycloak: POST /protocol/openid-connect/token (username, password, client_id)
    Keycloak-->>Owner: JWT Access Token (chứa realm_access.roles, sub=tenantId)

    Owner->>Client: Nhập thông tin khách sạn, bấm "Tạo"
    Client->>Gateway: POST /api/hotels (Authorization: Bearer JWT)

    Gateway->>Gateway: Validate chữ ký + hạn dùng JWT (JWKS)
    alt Token không hợp lệ ở Gateway
        Gateway-->>Client: 401 Unauthorized
    else Token hợp lệ
        Gateway->>Security: Forward request (giữ nguyên header Authorization)
        Security->>Security: Tự re-validate JWT lần 2 (JWKS riêng của hotel-service)

        alt Token không hợp lệ ở hotel-service
            Security-->>Client: 401 Unauthorized
        else Token hợp lệ
            Security->>Controller: (AOP) kiểm tra hasRole('HOTEL_OWNER')

            alt Thiếu role HOTEL_OWNER
                Security-->>Client: 403 ACCESS_DENIED
            else Có role HOTEL_OWNER
                Controller->>Service: createHotel(request)

                Service->>Utils: getCurrentTenantId()
                Utils-->>Service: tenantId (đọc từ claim sub)

                Service->>Location: Kiểm tra provinceCode/districtCode/wardCode

                alt Mã địa giới không hợp lệ
                    Location-->>Service: not found hoặc sai cây phân cấp
                    Service-->>Controller: throw AppException 400
                    Controller-->>Client: 400 INVALID_LOCATION_HIERARCHY
                else Địa giới hợp lệ
                    Location-->>Service: OK

                    Service->>DB: BEGIN TRANSACTION

                    loop Mỗi amenity trong request
                        alt amenity có id
                            Service->>Amenity: findById(id)
                            alt Không tồn tại hoặc đã xóa
                                Amenity-->>Service: empty
                                Service-->>Controller: throw AppException 400
                                Controller-->>Client: 400 AMENITY_NOT_FOUND
                            else Tồn tại
                                Amenity-->>Service: Amenity có sẵn
                            end
                        else amenity chỉ có name
                            Service->>Amenity: findByNameIgnoreCase(name)
                            alt Đã tồn tại tên này
                                Amenity-->>Service: Amenity có sẵn dùng lại
                            else Chưa tồn tại
                                Service->>Amenity: save(new Amenity)
                                Amenity-->>Service: Amenity mới
                            end
                        end
                        Service->>DB: INSERT hotel_amenities
                    end

                    Service->>Service: processPolicies - tạo lần lượt các policies
                    Service->>Service: processRoomTypes - tạo lần lượt các room_types

                    Service->>HotelRepo: save(hotel status PENDING, tenant_id)
                    HotelRepo->>DB: INSERT hotels
                    DB-->>HotelRepo: OK

                    Service->>DB: COMMIT TRANSACTION
                    DB-->>Service: Committed

                    Service->>Event: publishEvent(HotelCreatedEvent)
                    Service-->>Controller: CreateHotelResponse
                    Controller-->>Client: 201 Created

                    Note over Event,Listener: Chạy SAU KHI transaction commit thành công
                    Event->>Listener: onHotelCreated(event)
                    Listener->>Kafka: send review-hotel-command
                    Kafka-->>Listener: ACK bất đồng bộ
                end
            end
        end
    end
```

---

## UC-32 — Upload ảnh khách sạn (đã chỉnh sửa)

```mermaid
sequenceDiagram
    autonumber

    actor Owner as HOTEL_OWNER
    participant Client
    participant Gateway
    participant Security as Spring Security (hotel-service)
    participant Controller
    participant Service as HotelImageServiceImpl
    participant HotelRepo
    participant Utils as SecurityUtils
    participant Validator as ImageValidationUtils
    participant ImageRepo as HotelImageRepository
    participant MinIO
    participant DB

    Owner->>Client: Chọn nhiều ảnh, bấm Upload
    Client->>Gateway: POST /api/hotels/{id}/images (multipart, Bearer JWT)
    Gateway->>Gateway: Validate JWT (JWKS)

    alt Token không hợp lệ
        Gateway-->>Client: 401 Unauthorized
    else Token hợp lệ
        Gateway->>Security: Forward request
        Security->>Security: Tự re-validate JWT

        alt Thiếu role HOTEL_OWNER
            Security-->>Client: 403 ACCESS_DENIED
        else Có role HOTEL_OWNER
            Security->>Controller: uploadHotelImages()
            Controller->>Service: uploadImages(hotelId, files)

            Service->>HotelRepo: findById(hotelId), chưa xóa mềm

            alt Không tìm thấy hotel
                HotelRepo-->>Service: empty
                Service-->>Controller: throw AppException 404
                Controller-->>Client: 404 HOTEL_NOT_FOUND
            else Tìm thấy hotel
                HotelRepo-->>Service: Hotel

                Service->>Utils: getCurrentTenantId()
                Utils-->>Service: tenantId

                alt tenantId khác hotel.tenantId
                    Service-->>Controller: throw AppException 403
                    Controller-->>Client: 403 NOT_HOTEL_OWNER
                else Đúng chủ sở hữu
                    Service->>Validator: validate từng file trong batch

                    alt Có file sai định dạng hoặc qua 5MB
                        Validator-->>Service: throw AppException 400
                        Service-->>Controller: propagate
                        Controller-->>Client: 400 IMAGE_TYPE_NOT_ALLOWED hoặc IMAGE_TOO_LARGE
                        note over Service,MinIO: Chưa có object nào lên MinIO ở bước này
                    else Toàn bộ file hợp lệ
                        Service->>ImageRepo: tìm cover hiện tại của hotel
                        ImageRepo-->>Service: đã có cover hay chưa

                        loop Mỗi file hợp lệ
                            Service->>Service: Sinh objectName hotelId slash UUID chấm ext
                            Service->>MinIO: uploadFile(bucket, objectName, stream)

                            alt Upload lỗi mạng hoặc quyền bucket
                                MinIO-->>Service: Error
                                Service->>MinIO: rollback xóa các object đã lỡ upload trong batch
                                Service-->>Controller: throw AppException 502
                                Controller-->>Client: 502 lỗi upload, không lưu DB dòng nào
                            else Upload thành công
                                MinIO-->>Service: url
                            end
                        end

                        Service->>ImageRepo: saveAll(entities)
                        note right of ImageRepo: Ảnh đầu tiên batch = cover chỉ khi hotel chưa có cover
                        ImageRepo->>DB: INSERT hotel_images nhiều dòng
                        DB-->>ImageRepo: OK
                        ImageRepo-->>Service: saved entities

                        Service-->>Controller: List HotelImageResponse
                        Controller-->>Client: 201 Created
                    end
                end
            end
        end
    end
```

---

## UC-33 — Xóa ảnh khách sạn (đã chỉnh sửa)

```mermaid
sequenceDiagram
    autonumber

    actor User as HOTEL_OWNER hoac PLATFORM_ADMIN
    participant Client
    participant Gateway
    participant Security as Spring Security (hotel-service)
    participant Controller
    participant Service as HotelImageServiceImpl
    participant HotelRepo
    participant Utils as SecurityUtils
    participant ImageRepo as HotelImageRepository
    participant MinIO
    participant DB

    User->>Client: Bấm Xóa ảnh
    Client->>Gateway: DELETE /api/hotels/{id}/images/{imageId} (Bearer JWT)
    Gateway->>Gateway: Validate JWT (JWKS)

    alt Token không hợp lệ
        Gateway-->>Client: 401 Unauthorized
    else Token hợp lệ
        Gateway->>Security: Forward request
        Security->>Security: Tự re-validate JWT

        alt Không có role HOTEL_OWNER lẫn PLATFORM_ADMIN
            Security-->>Client: 403 ACCESS_DENIED
        else Có 1 trong 2 role
            Security->>Controller: deleteHotelImage()
            Controller->>Service: deleteImages(hotelId, imageId)

            Service->>HotelRepo: findById(hotelId), chưa xóa mềm

            alt Không tìm thấy hotel
                HotelRepo-->>Service: empty
                Service-->>Controller: throw AppException 404
                Controller-->>Client: 404 HOTEL_NOT_FOUND
            else Tìm thấy hotel
                HotelRepo-->>Service: Hotel

                Service->>Utils: hasRole PLATFORM_ADMIN

                alt Là PLATFORM_ADMIN
                    note over Service: Bỏ qua ownership check
                else Là HOTEL_OWNER thường
                    Service->>Utils: getCurrentTenantId()
                    Utils-->>Service: tenantId

                    alt tenantId khác hotel.tenantId
                        Service-->>Controller: throw AppException 403
                        Controller-->>Client: 403 NOT_HOTEL_OWNER
                    end
                end

                Service->>ImageRepo: findById(imageId), đúng hotelId, chưa xóa mềm

                alt Không tìm thấy ảnh
                    ImageRepo-->>Service: empty
                    Service-->>Controller: throw AppException 404
                    Controller-->>Client: 404 IMAGE_NOT_FOUND
                else Tìm thấy ảnh
                    ImageRepo-->>Service: HotelImage objectName isCover

                    Service->>DB: BEGIN TRANSACTION
                    Service->>ImageRepo: save is_deleted true, is_cover false

                    alt Ảnh vừa xóa đang là cover
                        Service->>ImageRepo: tìm ảnh còn lại cũ nhất
                        alt Còn ảnh khác
                            ImageRepo-->>Service: ảnh cũ nhất còn lại
                            Service->>ImageRepo: save is_cover true
                        else Không còn ảnh nào
                            note over Service: Hotel tạm thời không có cover
                        end
                    end

                    Service->>MinIO: deleteFile(bucket, objectName)

                    alt Xóa MinIO thất bại
                        MinIO-->>Service: Error
                        Service->>DB: ROLLBACK TRANSACTION
                        note over DB: Hủy toàn bộ thay đổi ở trên
                        Service-->>Controller: throw AppException 502
                        Controller-->>Client: 502 IMAGE_DELETE_FAILED
                    else Xóa MinIO thành công
                        MinIO-->>Service: OK
                        Service->>DB: COMMIT TRANSACTION
                        Service-->>Controller: void
                        Controller-->>Client: 204 No Content
                    end
                end
            end
        end
    end
```
