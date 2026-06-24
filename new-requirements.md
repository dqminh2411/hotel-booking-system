# Yêu cầu mở rộng hệ thống SaaS quản lý đặt phòng khách sạn

## 1. Giới thiệu hệ thống

### 1.1 Mục tiêu

Xây dựng nền tảng SaaS quản lý khách sạn đa tenant cho phép nhiều chủ khách sạn đăng ký và vận hành khách sạn trên cùng hệ thống.

Hệ thống hỗ trợ:

* Khách hàng tìm kiếm và đặt phòng trực tuyến
* Nhân viên khách sạn vận hành quy trình check-in/check-out
* Chủ khách sạn quản lý khách sạn, nhân viên, doanh thu
* Quản trị viên quản lý toàn bộ nền tảng

### 1.2 Đặc điểm SaaS

* Multi-tenant
* Subscription-based
* Khả năng mở rộng độc lập từng service
* Hỗ trợ nhiều khách sạn và chuỗi khách sạn
* Dashboard theo dõi thống kê doanh thu, công suất phòng, booking, khách hàng
* API-first Architecture

---

# 2. Các vai trò người dùng và chức năng

## 2.1 Khách hàng (Customer)

### Quản lý tài khoản

* Đăng ký tài khoản
* Đăng nhập
* Đăng nhập Google OAuth2
* Quên mật khẩu
* Cập nhật hồ sơ cá nhân

### Tìm kiếm khách sạn

* Tìm kiếm theo địa điểm
* Tìm kiếm theo ngày check-in/check-out
* Tìm kiếm theo số khách
* Tìm kiếm theo số phòng
* Tìm kiếm theo khoảng giá
* Tìm kiếm theo tiện ích
* Tìm kiếm theo số sao
* Sắp xếp theo giá

### Đặt phòng

* Xem chi tiết khách sạn
* Xem loại phòng
* Kiểm tra phòng trống
* Đặt phòng
* Thanh toán online
* Áp dụng mã giảm giá
* Hủy đặt phòng
* Xem lịch sử đặt phòng

### Thông báo

* Email xác nhận
* Email hủy phòng
* Push Notification
* Thông báo khuyến mãi

---

## 2.2 Nhân viên khách sạn (Hotel Staff)

### Quản lý phòng

* Thêm phòng
* Sửa thông tin phòng
* Xóa phòng
* Quản lý trạng thái phòng

### Quản lý đặt phòng

* Xem danh sách booking
* Xác nhận booking
* Check-in khách
* Check-out khách
* Hủy booking

### Quản lý khách hàng

* Xem thông tin khách
* Lịch sử lưu trú

---

## 2.3 Chủ khách sạn (Hotel Owner)

### Quản lý khách sạn

* Thêm khách sạn
* Sửa thông tin khách sạn
* Xóa khách sạn
* Quản lý hình ảnh khách sạn
* Quản lý tiện ích

### Quản lý nhân viên

* Thêm nhân viên
* Sửa nhân viên
* Xóa nhân viên
* Gán quyền nhân viên

### Quản lý doanh thu

* Dashboard doanh thu
* Thống kê công suất phòng
* Thống kê booking
* Thống kê khách hàng

### Quản lý giá

* Seasonal Pricing
* Weekend Pricing
* Special Pricing

---

## 2.4 Quản trị viên hệ thống (System Admin)

### Quản lý người dùng

* Khóa tài khoản
* Mở khóa tài khoản
* Phân quyền

### Quản lý khách sạn

* Duyệt khách sạn mới
* Tạm ngưng khách sạn vi phạm
* Kiểm duyệt nội dung

### Quản lý Promotion

* Tạo promotion toàn hệ thống
* Quản lý coupon
* Quản lý campaign

### Dashboard hệ thống

* Tổng số người dùng
* Tổng số khách sạn
* Tổng doanh thu
* Tổng booking
* Tỷ lệ chuyển đổi

---

# 3. Kiến trúc Microservices mục tiêu

## 3.1 Các service hiện có

Các service đã được triển khai trong phiên bản hiện tại gồm: 

* API Gateway
* Eureka Server
* User Service
* Hotel Service
* Booking Service
* Place Booking Service (Saga Orchestrator)
* Payment Service
* Notification Service
* Kafka
* PostgreSQL Database per Service

---

## 3.2 Các service cần bổ sung

| Service                  | Mục đích                    |
| ------------------------ | --------------------------- |
| Auth Service             | Authentication, OAuth2, JWT |
| Staff Management Service | Quản lý nhân viên           |
| Promotion Service        | Coupon, discount            |
| Analytics Service        | Dashboard và báo cáo        |
| Tenant Service           | Quản lý tenant SaaS         |

---

# 4. Công nghệ đề xuất

## Backend

* Java 21
* Spring Boot 3
* Spring Cloud
* Spring Security
* Spring Data JPA
* Resilience4j

## Frontend

* React
* Vite
* Material UI hoặc Ant Design

## Database

* PostgreSQL

## Messaging

* Apache Kafka

## Service Discovery

* Netflix Eureka

## API Gateway

* Spring Cloud Gateway

## Cache

* Redis

## Authentication

* JWT
* OAuth2
* Google Login

## Payment

* Stripe
* VNPay

## Notification

* Firebase Cloud Messaging (FCM)
* Email SMTP

## File Storage

* MinIO hoặc AWS S3

## Monitoring

* Prometheus
* Grafana

## Logging

* ELK Stack
* OpenSearch

## Container

* Docker
* Docker Compose


---

# 5. Yêu cầu phi chức năng

## Hiệu năng

* Tìm kiếm khách sạn < 300ms
* Booking < 2 giây
* Dashboard < 1 giây

## Khả năng mở rộng

* Horizontal Scaling
* Stateless Services
* Independent Deployment

## Bảo mật

* JWT Authentication
* OAuth2
* RBAC
* API Rate Limiting
* HTTPS

## Độ sẵn sàng

* 99.9% uptime
* Retry Pattern
* Circuit Breaker
* Kafka Retry Queue

## Tính nhất quán

* Saga Pattern
* Idempotency
* Eventual Consistency

---

# 6. Các yêu cầu mở rộng kỹ thuật

## Redis Cache

Mục tiêu:

* Cache danh sách khách sạn
* Cache kết quả tìm kiếm
* Cache thông tin phòng
* Cache dashboard

Lợi ích:

* Giảm tải PostgreSQL
* Giảm thời gian phản hồi

---

## Idempotency

Áp dụng cho:

* Đặt phòng
* Thanh toán
* Hủy phòng

Giải pháp:

* Idempotency-Key Header
* Redis Storage
* Duplicate Request Detection

Ví dụ:

```http
POST /place-booking

Idempotency-Key:
8f4a7bca-fd31-45d5-9f95-111222333444
```

---

## Firebase Push Notification

Bổ sung cho Notification Service:

* Booking Confirmed
* Booking Cancelled
* Promotion
* Check-in Reminder

---

## Google Login

Bổ sung cho User Service/Auth Service:

* OAuth2 Google
* Tự động tạo tài khoản
* Liên kết tài khoản hiện có

---

## Stripe/VNPay Integration

Bổ sung cho Payment Service:

* Thanh toán thật
* Refund
* Webhook Callback
* Transaction History

---

# 7. So sánh với hệ thống hiện tại

## Đã hoàn thành trong hệ thống hiện tại

* Tìm kiếm khách sạn cơ bản
* Xem chi tiết khách sạn
* Kiểm tra availability
* Đặt phòng
* Thanh toán mock
* Gửi email
* API Gateway
* Eureka Discovery
* Kafka Event Driven
* Saga Orchestration
* Database Per Service
* Circuit Breaker cho Payment Service   

---

## Cần phát triển thêm

### Nghiệp vụ

* Multi-tenant SaaS
* Quản lý khách sạn, phòng
* Quản lý nhân viên
* Check-in/check-out
* Promotion
* Coupon
* Dashboard

### Kỹ thuật

* Redis Cache
* OAuth2 Login
* Firebase Notification
* Stripe/VNPay
* Idempotency
* Monitoring
* Logging
* Distributed Tracing


