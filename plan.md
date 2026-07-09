# Plan: Phát Triển API Search & Filter Khách Sạn (T10 + T11)

## 📋 Phân Tích Yêu Cầu

### T10 - Search Cơ Bản (Basic Search & Hotel Detail)
Xây dựng API tìm kiếm khách sạn theo 3 tiêu chí cơ bản:
- Địa điểm (location code: province/district/ward)
- Ngày check-in/check-out
- Số lượng khách & số lượng phòng

**Output**: Danh sách khách sạn phù hợp + loại phòng rẻ nhất

### T11 - Advanced Filtering & Sorting
Mở rộng T10 với các filter nâng cao:
- Lọc theo khoảng giá (minPrice - maxPrice)
- Lọc theo tiện ích (amenities)
- Sắp xếp kết quả (giá tăng/giảm, đánh giá)

**Design Principle**: Dễ mở rộng → dùng **JPA Specification** hoặc **QueryDSL**

---

## 🏗️ Kiến Trúc Hiện Tại (Key Points)

### Database Schema
- **hotels**: khách sạn chính
- **provinces/districts/wards**: vị trí địa lý
- **room_types**: loại phòng + `base_price_per_night`
- **rooms**: phòng vật lý
- **pricing_rules**: giá đặc biệt (SEASONAL/WEEKEND/SPECIAL)
- **hotel_amenities** (N-N): tiện ích khách sạn
- **room_type_amenities** (N-N): tiện ích loại phòng

### Hiện Tại Chưa Có
- Search API endpoint
- Repository queries cho tìm kiếm
- Filter/Sort logic
- Cache strategy (Redis)

### API Gateway & Discovery
- API Gateway tại port 8080
- Eureka Discovery tại port 8761
- Hotel Service tại port 5003

---

## 🛣️ Luồng Xử Lý Tìm Kiếm (Theo Design Doc)

```
1. Client gửi GET /api/hotels?locationCode=&checkinDate=&...
   ↓
2. Controller validate params
   - checkinDate < checkoutDate ✓
   - guestNum > 0 ✓
   - roomNum > 0 ✓
   - minPrice <= maxPrice (nếu có) ✓
   ↓
3. Service kiểm tra cache (Redis)
   - Nếu hit: trả về kết quả
   - Nếu miss: query DB
   ↓
4. Query 1 lần lấy:
   - Danh sách hotels phù hợp location
   - Room types còn trống (dựa trên checkin/checkout)
   - Giá cơ bản + pricing rules
   - Cover image
   - Amenities (T11)
   ↓
5. Filter & Transform:
   - Giữ lại room type rẻ nhất per hotel
   - Sắp xếp (T11)
   ↓
6. Cache kết quả (TTL ngắn)
   ↓
7. Trả về HotelSearchResponse
```

---

## 📝 Todos

### Phase 1: Foundation & T10 (Search Cơ Bản)

#### 1.1 Understanding Current Code
- [ ] Đọc Hotel entity & RoomType entity
- [ ] Đọc HotelRepository hiện tại
- [ ] Hiểu cách @Query/@JpaRepository hoạt động
- [ ] Hiểu Spring Data JPA Specifications (cho phase 2)

#### 1.2 Design DTOs & Requests
- [ ] Tạo HotelSearchRequest DTO (locationCode, checkinDate, checkoutDate, guestNum, roomNum)
- [ ] Tạo HotelSearchResponse DTO (danh sách hotels + rẻ nhất room type)
- [ ] Tạo RoomTypeSummaryDTO (roomTypeId, name, basePrice, maxGuests, image)

#### 1.3 Build Search Logic (T10)
- [ ] Tạo HotelSearchRepository (hoặc extend HotelRepository)
- [ ] Viết query tìm hotels theo location
- [ ] Viết query lấy room types trống (sub-query hoặc loop)
- [ ] Viết HotelSearchService
  - Validate input
  - Combine results
  - Transform sang DTO

#### 1.4 Create Controller & Endpoint
- [ ] Tạo HotelSearchController
- [ ] Endpoint: GET /api/hotels/search
- [ ] Error handling & response codes

#### 1.5 Test T10
- [ ] Unit test repository queries
- [ ] Integration test endpoint
- [ ] Manual test qua curl/Postman

### Phase 2: Advanced Features (T11)

#### 2.1 Extend Search DTOs
- [ ] Thêm fields vào HotelSearchRequest: minPrice, maxPrice, amenityIds, sortBy
- [ ] Validate minPrice <= maxPrice

#### 2.2 Implement Filtering
- [ ] Lọc theo price range (dựa trên base_price_per_night từ room_types)
- [ ] Lọc theo amenities (JOIN với hotel_amenities)
- [ ] Tối ưu query để lấy 1 lần thay vì N lần

#### 2.3 Implement Sorting
- [ ] Sắp xếp theo giá (ASC/DESC)
- [ ] Sắp xếp theo rating (T11 extension)
- [ ] Support dynamic sort fields

#### 2.4 Cache Strategy
- [ ] Thêm Redis integration (nếu project chưa có)
- [ ] Cache key: location + dates + filters + sort
- [ ] TTL strategy (ngắn, vì dữ liệu thay đổi thường xuyên)

#### 2.5 Test T11
- [ ] Unit test filters
- [ ] Integration test combinations
- [ ] Performance test (verify < 300ms requirement)

---

## 🎯 Learning Objectives

### Kiến Thức Backend Java Bạn Sẽ Học
1. **Spring Data JPA Specifications** - cách xây dựng query động
2. **Query Optimization** - 1 query tốt hơn N queries (N+1 problem)
3. **Cache Strategy** - khi nào cache, TTL, invalidation
4. **DTO Pattern** - separation between entity & API response
5. **Error Handling** - validation, exception mapping
6. **Testing** - unit vs integration test

### Interview Topics
- Làm thế nào filter millions của records hiệu quả?
- Spring Data JPA Specification vs Query vs Native SQL?
- N+1 query problem & cách fix?
- Cache invalidation strategies?

### Common Junior Mistakes
- ❌ N+1 queries (query hotel, sau đó loop query room_types)
- ❌ Hard-coded SQL (không reusable)
- ❌ Loading toàn bộ data để filter trong memory
- ❌ Quên validate input (allow SQL injection)
- ❌ Caching mà không có invalidation strategy

---

## 📌 Key Design Decisions

### Why JPA Specification (not Query)?
- Reusable: có thể combine nhiều filter
- Testable: dễ unit test
- Type-safe: không string-based queries
- Dễ mở rộng sau này

### Why 1 Query (not N+1)?
- Performance: < 300ms requirement
- DB efficiency: giảm tải network
- Consistency: snapshot dữ liệu tại 1 thời điểm

### Availability Check Logic
- Query rooms booked trong khoảng [checkinDate, checkoutDate]
- So sánh với quantity của room_type
- Có sẵn nếu: available = quantity - booked > 0

---

## 📚 Files You'll Work On

### Existing Files
- `Hotel.java` - entity
- `RoomType.java` - entity
- `HotelRepository.java` - repository (extend + thêm queries)

### Files You'll Create/Modify

#### T10
- `dto/HotelSearchRequest.java` - NEW
- `dto/HotelSearchResponse.java` - NEW
- `dto/RoomTypeSummaryDTO.java` - NEW
- `repository/HotelSearchRepository.java` - NEW (or extend existing)
- `service/HotelSearchService.java` - NEW
- `controller/HotelSearchController.java` - NEW

#### T11
- Modify `dto/HotelSearchRequest.java` - thêm price, amenities, sort
- Modify `service/HotelSearchService.java` - thêm filter logic
- `config/JpaSpecificationConfig.java` - NEW (if needed)
- `specification/HotelSearchSpecification.java` - NEW (if using Specification)

---

## ✅ Success Criteria

### T10
- [ ] GET /api/hotels/search?locationCode=...&checkinDate=...&checkoutDate=...&guestNum=...&roomNum=...
- [ ] Response time < 300ms
- [ ] Correct availability check (no double-booking)
- [ ] Return cheapest room type per hotel
- [ ] Proper error codes (400, 404)

### T11
- [ ] Filter by price, amenities works
- [ ] Sort by price ASC/DESC works
- [ ] Still < 300ms with filters
- [ ] Easy to add more filters in future

---

## 🚀 Next Steps

1. **Read Understanding Current Code** → hỏi nếu chưa hiểu entity/repository
2. **Design DTOs** → I'll review before you code
3. **Implement T10 step by step** → mỗi bước hỏi trước khi code
4. **Test T10** → verify before moving to T11
5. **T11** → apply learning từ T10

---

## 📖 Resources & Refs

- **Sequence Diagram**: docs/ad_hotel_service.md (UC-08, UC-09, UC-29, UC-30)
- **Database Schema**: docs/ad_hotel_service.md (section 1.1 - 1.3)
- **API Spec**: docs/ad_hotel_service.md (section 2)
- **Architecture**: docs/architecture.md
- **Spring Data JPA**: https://spring.io/projects/spring-data-jpa
- **Specification pattern**: Spring Data JPA docs / Baeldung tutorials

