# ĐẶC TẢ YÊU CẦU PHẦN MỀM

### (Software Requirements Specification – chuẩn IEEE 830)

## HỆ THỐNG SAAS QUẢN LÝ ĐẶT PHÒNG KHÁCH SẠN ĐA TENANT
### (Hotel Booking SaaS Platform)

- **Phiên bản:** 1.1
- **Ngày phát hành:** 29/06/2026

---

## MỤC LỤC

- [1. Giới thiệu](#1-giới-thiệu)
- [2. Mô tả tổng quát](#2-mô-tả-tổng-quát-overall-description)
- [3. Yêu cầu cụ thể](#3-yêu-cầu-cụ-thể-specific-requirements)
- [4. Phụ lục](#4-phụ-lục)

---

# 1. GIỚI THIỆU

## 1.1. Mục đích (Purpose)

Tài liệu này đặc tả đầy đủ các yêu cầu chức năng và phi chức năng của hệ thống SaaS quản lý đặt phòng khách sạn đa tenant (sau đây gọi là "Hệ thống"). Tài liệu được biên soạn theo chuẩn IEEE 830-1998, làm cơ sở thống nhất để nhóm phát triển, kiểm thử (QA/SQA), quản lý dự án và khách hàng/giảng viên hướng dẫn cùng tham chiếu trong suốt vòng đời phát triển sản phẩm.

Đặc tả này hợp nhất toàn bộ phạm vi của hệ thống — bao gồm cả các chức năng đã được triển khai trong phiên bản hiện tại (tìm kiếm, đặt phòng, thanh toán mock, Saga, Kafka, Eureka, API Gateway, Circuit Breaker...) và các chức năng mở rộng cần phát triển thêm (multi-tenant, quản lý nhân viên, promotion, dashboard, Redis cache, OAuth2, Stripe/VNPay, Firebase Notification, Idempotency, Observability...) — và trình bày như một sản phẩm thống nhất, không phân biệt "cũ" hay "mới".

## 1.2. Phạm vi (Scope)

Hệ thống có tên gọi đề xuất: HotelHub SaaS Platform. Hệ thống cho phép:

- Nhiều chủ khách sạn (tenant) đăng ký, cấu hình và vận hành một hoặc nhiều khách sạn/chuỗi khách sạn trên cùng một nền tảng dùng chung cơ sở hạ tầng (multi-tenant SaaS).

- Khách hàng tìm kiếm, so sánh, đặt phòng và thanh toán trực tuyến theo luồng trải nghiệm tương tự Booking.com (tìm kiếm theo địa điểm/ngày/số khách → xem kết quả & lọc → xem chi tiết khách sạn/phòng → giữ chỗ → thanh toán → **xác nhận đặt phòng tự động**).

- Nhân viên khách sạn vận hành nghiệp vụ tại quầy: quản lý phòng, check-in/check-out, quản lý khách lưu trú. Việc xác nhận đơn đặt phòng **được hệ thống tự động hoá** ngay sau khi thanh toán thành công, không yêu cầu nhân viên xác nhận thủ công.

- Chủ khách sạn quản lý khách sạn, nhân sự, chính sách giá theo mùa/cuối tuần, theo dõi doanh thu và hiệu suất kinh doanh qua dashboard.

- Quản trị viên hệ thống (Platform Admin) kiểm duyệt khách sạn, quản lý tài khoản, quản lý chương trình khuyến mãi toàn hệ thống và theo dõi chỉ số vận hành toàn nền tảng.

Hệ thống được xây dựng theo kiến trúc microservices, hướng API-first, có khả năng mở rộng độc lập theo từng service, đảm bảo các thuộc tính phi chức năng về hiệu năng, độ sẵn sàng, an toàn dữ liệu và tính nhất quán cuối (eventual consistency) thông qua Saga Pattern.

Phạm vi tài liệu KHÔNG bao gồm: thiết kế chi tiết cơ sở dữ liệu (data model chi tiết), thiết kế UI/UX chi tiết (wireframe/mockup), và kế hoạch kiểm thử (Test Plan) — các nội dung này được trình bày trong các tài liệu riêng (SDD, UI Spec, Test Plan).

## 1.3. Định nghĩa, từ viết tắt và thuật ngữ (Definitions, Acronyms, Abbreviations)

| **Thuật ngữ / Viết tắt** | **Giải thích** |
| --- | --- |
| SRS | Software Requirements Specification – Đặc tả yêu cầu phần mềm |
| SaaS | Software as a Service – Phần mềm dạng dịch vụ |
| Tenant | Một chủ thể thuê dùng nền tảng (một chủ khách sạn/chuỗi khách sạn) |
| UC | Use Case – Trường hợp sử dụng |
| FR | Functional Requirement – Yêu cầu chức năng |
| NFR | Non-Functional Requirement – Yêu cầu phi chức năng |
| RBAC | Role-Based Access Control – Kiểm soát truy cập theo vai trò |
| JWT | JSON Web Token – Token xác thực dạng JSON |
| OAuth2 | Chuẩn giao thức ủy quyền (Open Authorization 2.0) |
| Saga Pattern | Mẫu thiết kế quản lý giao dịch phân tán qua nhiều service |
| CQRS | Command Query Responsibility Segregation |
| Idempotency | Tính chất một yêu cầu gửi nhiều lần chỉ tạo ra một hiệu ứng nghiệp vụ |
| SLA | Service Level Agreement – Thỏa thuận mức cam kết dịch vụ |
| RPS | Requests Per Second – Số lượng yêu cầu mỗi giây |
| RTO/RPO | Recovery Time/Point Objective – Mục tiêu thời gian/điểm khôi phục |

## 1.4. Tài liệu tham khảo (References)

- Tài liệu mô tả yêu cầu mở rộng hệ thống SaaS quản lý đặt phòng khách sạn (new-requirements.md), nội bộ dự án, 2026.

- IEEE Std 830-1998, IEEE Recommended Practice for Software Requirements Specifications.

- Luồng nghiệp vụ tham khảo từ nền tảng Booking.com (quy trình tìm kiếm – đặt phòng – thanh toán – xác nhận – hủy phòng công khai).

- Tài liệu kiến trúc Saga Orchestration và Database-per-Service hiện có của hệ thống (Place Booking Service).

- Tài liệu đặc tả OpenAPI của các service hiện hành (User Service, Hotel Service, Booking Service, Payment Service, Notification Service).

## 1.5. Tổng quan tài liệu (Overview)

Phần 2 trình bày mô tả tổng quát về sản phẩm: bối cảnh, các nhóm chức năng chính, đặc điểm người dùng, ràng buộc và giả định. Phần 3 trình bày chi tiết các yêu cầu cụ thể: giao diện ngoài, danh sách use case, yêu cầu chức năng theo từng vai trò/module, kịch bản use case, yêu cầu phi chức năng, yêu cầu thiết kế và các thuộc tính chất lượng hệ thống. Phần 4 trình bày các nội dung bổ sung (ma trận truy vết, phụ lục).

# 2. MÔ TẢ TỔNG QUÁT (OVERALL DESCRIPTION)

## 2.1. Bối cảnh sản phẩm (Product Perspective)

HotelHub là một nền tảng độc lập (không phải module mở rộng của hệ thống khác), được xây dựng mới hoàn toàn theo kiến trúc microservices. Hệ thống đóng vai trò trung gian kết nối ba nhóm tác nhân kinh doanh: khách hàng có nhu cầu lưu trú, chủ khách sạn cung cấp phòng, và đội ngũ vận hành tại khách sạn — tương tự mô hình OTA (Online Travel Agency) như Booking.com, nhưng bổ sung thêm năng lực quản trị vận hành nội bộ khách sạn (PMS – Property Management System) và mô hình kinh doanh SaaS đa tenant.

### 2.1.1. Sơ đồ kiến trúc tổng quan (mô tả)

Kiến trúc gồm 4 lớp: (1) Lớp giao diện người dùng (Customer Web/App, Staff Portal, Owner Dashboard, Admin Console) xây dựng bằng ReactJS/Vite; (2) Lớp API Gateway (Spring Cloud Gateway) làm điểm vào duy nhất, xử lý routing, rate limiting, xác thực JWT đầu vào qua Keycloak JWKS; (3) Lớp các microservices nghiệp vụ (Keycloak IdP, User, Hotel, Booking, Place Booking/Saga Orchestrator, Payment, Promotion, Notification); (4) Lớp hạ tầng dùng chung (Eureka Service Discovery, Kafka Message Broker, Redis Cache & Lock, MinIO Object Storage, PostgreSQL Database-per-Service, Keycloak DB, OpenTelemetry Collector, Jaeger Tracing Backend, Elasticsearch & Kibana cho Audit Logging).

### 2.1.2. Danh sách microservices

| **Service** | **Vai trò** | **Trạng thái** |
| --- | --- | --- |
| API Gateway | Định tuyến request, rate limiting, xác thực JWT đầu vào | Đã có |
| Eureka Server | Service Discovery, đăng ký & tìm kiếm service | Đã có |
| User Service | Xác thực người dùng, Hồ sơ người dùng, vai trò người dùng, Đăng ký, quản lý gói thuê bao (subscription) của tenant | Đã có (mở rộng) |
| Hotel Service | Quản lý khách sạn, loại phòng, phòng, tiện ích, hình ảnh | Đã có (mở rộng) |
| Staff Management Service | Quản lý nhân viên khách sạn, phân quyền nội bộ | Cần bổ sung |
| Booking Service | Quản lý đặt phòng, trạng thái booking, lịch sử | Đã có |
| Place Booking Service | Saga Orchestrator điều phối luồng đặt phòng – thanh toán – tự động xác nhận | Đã có |
| Payment Service | Xử lý thanh toán, tích hợp Stripe/VNPay, refund | Đã có (mở rộng) |
| Promotion Service | Coupon, chương trình giảm giá, campaign | Cần bổ sung |
| Notification Service | Gửi email, push notification (FCM) | Đã có (mở rộng) |

## 2.2. Tóm tắt chức năng sản phẩm (Product Functions)

Các nhóm chức năng chính của hệ thống, được trình bày chi tiết tại Mục 3.2, bao gồm:

- Quản lý tài khoản & xác thực (đăng ký, đăng nhập, OAuth2 Google, quên mật khẩu, RBAC).

- Tìm kiếm & khám phá khách sạn theo nhiều tiêu chí (địa điểm, ngày, số khách, giá, tiện ích, số sao).

- Đặt phòng & thanh toán trực tuyến với **xác nhận tự động** (giữ chỗ, áp mã giảm giá, thanh toán VNPay, idempotency, Saga rollback khi thất bại).

- Quản lý vận hành khách sạn tại quầy (phòng, check-in/check-out, khách lưu trú).

- Quản lý khách sạn & nhân sự cho chủ khách sạn (CRUD khách sạn, nhân viên, phân quyền).

- Quản lý giá động (seasonal/weekend/special pricing).

- Dashboard & báo cáo doanh thu, công suất phòng, booking, khách hàng (theo tenant) và toàn nền tảng (Admin).

- Quản lý khuyến mãi/coupon ở cấp khách sạn và cấp toàn hệ thống.

- Thông báo đa kênh (email, push notification).

- Quản trị nền tảng: kiểm duyệt khách sạn, khóa/mở khóa tài khoản, theo dõi chỉ số toàn hệ thống.

## 2.3. Đặc điểm người dùng (User Characteristics)

| **Vai trò** | **Đặc điểm** | **Mức độ am hiểu công nghệ** |
| --- | --- | --- |
| Khách hàng (Customer) | Người dùng cuối, đặt phòng cho mục đích cá nhân/công việc, dùng web hoặc mobile app | Phổ thông |
| Nhân viên khách sạn (Hotel Staff) | Lễ tân/nhân viên vận hành, sử dụng hệ thống hằng ngày tại quầy | Trung bình, được đào tạo nội bộ |
| Chủ khách sạn (Hotel Owner) | Chủ doanh nghiệp, quan tâm đến doanh thu & vận hành, không chuyên kỹ thuật | Phổ thông đến trung bình |
| Quản trị viên hệ thống (Platform Admin) | Vận hành nền tảng, kiểm duyệt nội dung, hỗ trợ tenant | Cao, hiểu nghiệp vụ đa-tenant |

## 2.4. Ràng buộc (Constraints)

- Hệ thống phải được xây dựng trên nền Java 21, Spring Boot 3, Spring Cloud theo định hướng kiến trúc microservices đã chọn.

- Mỗi service sở hữu cơ sở dữ liệu riêng (Database-per-Service); không truy cập trực tiếp CSDL của service khác.

- Giao tiếp bất đồng bộ giữa các service bắt buộc qua Apache Kafka; giao tiếp đồng bộ qua REST/OpenFeign có áp dụng Circuit Breaker (Resilience4j).

- Giao dịch xuyên nhiều service (đặt phòng – thanh toán) phải tuân theo Saga Pattern (orchestration) thay vì 2-phase commit, và phải tự động chuyển trạng thái booking sang Confirmed khi thanh toán thành công (không có bước xác nhận thủ công của nhân viên).

- Toàn bộ giao tiếp client-server phải qua HTTPS; xác thực dựa trên JWT có thời hạn.

- Tích hợp thanh toán bắt buộc hỗ trợ VNPay.

- Hệ thống phải hỗ trợ vận hành multi-tenant: dữ liệu của một tenant không được lộ sang tenant khác (tenant isolation ở tầng logic/schema).

## 2.5. Giả định và phụ thuộc (Assumptions and Dependencies)

- Giả định người dùng có kết nối Internet ổn định khi sử dụng hệ thống.

- Hệ thống phụ thuộc vào dịch vụ bên thứ ba: Google OAuth2, VNPay, Firebase Cloud Messaging — yêu cầu các dịch vụ này khả dụng và có hợp đồng/API key hợp lệ.

- Giả định hạ tầng triển khai (Docker/Docker Compose hoặc Kubernetes trong tương lai) đáp ứng đủ tài nguyên cho việc mở rộng theo chiều ngang.

- Giả định mỗi chủ khách sạn chịu trách nhiệm về tính chính xác của thông tin khách sạn/phòng mà họ khai báo (nội dung được Admin kiểm duyệt hậu kiểm).

# 3. YÊU CẦU CỤ THỂ (SPECIFIC REQUIREMENTS)

## 3.1. Yêu cầu giao diện ngoài (External Interface Requirements)

### 3.1.1. Giao diện người dùng (User Interfaces)

- Customer Web/Mobile App: ReactJS (web), giao diện responsive, hỗ trợ luồng tìm kiếm – đặt phòng – thanh toán tối giản số bước (tham khảo UX Booking.com: thanh tìm kiếm nổi bật ở trang chủ, bản đồ kết quả, bộ lọc bên trái, trang chi tiết khách sạn dạng gallery + bảng loại phòng).

- Staff Portal: giao diện quản trị gọn nhẹ, tối ưu cho thao tác nhanh tại quầy lễ tân (danh sách booking theo ngày, nút Check-in/Check-out nổi bật).

- Owner Dashboard: giao diện biểu đồ trực quan (doanh thu theo thời gian, công suất phòng, top khách sạn/loại phòng).

- Admin Console: giao diện danh sách + hành động kiểm duyệt (duyệt/từ chối khách sạn, khóa/mở tài khoản).

- Công nghệ UI đề xuất: React + Vite + Material UI/Ant Design, tuân thủ chuẩn responsive và accessibility cơ bản (WCAG AA).

### 3.1.2. Giao diện phần cứng (Hardware Interfaces)

Không có yêu cầu phần cứng chuyên biệt. Hệ thống vận hành trên hạ tầng máy chủ/cloud thông thường; thiết bị đầu cuối là máy tính, điện thoại, máy tính bảng có trình duyệt hoặc ứng dụng di động.

### 3.1.3. Giao diện phần mềm (Software Interfaces)

| **Hệ thống ngoài** | **Mục đích tích hợp** | **Giao thức** |
| --- | --- | --- |
| Google OAuth2 | Đăng nhập bằng tài khoản Google | OAuth2/OpenID Connect |
| VNPay | Thanh toán nội địa Việt Nam | REST API + Return URL/Webhook |
| Firebase Cloud Messaging | Gửi push notification tới app/web | FCM HTTP v1 API |
| SMTP Server | Gửi email xác nhận/hủy/khuyến mãi | SMTP |
| MinIO | Lưu trữ hình ảnh khách sạn, tài liệu | MinIO API |
| Prometheus/Grafana | Thu thập & hiển thị chỉ số vận hành | HTTP metrics scraping |
| ELK/OpenSearch | Tập trung log toàn hệ thống | Beats/Logstash pipeline |

### 3.1.4. Giao diện truyền thông (Communications Interfaces)

- Giao tiếp client–server: HTTPS/REST JSON qua API Gateway.

- Giao tiếp service–service đồng bộ: REST nội bộ qua Eureka Service Discovery, có Circuit Breaker.

- Giao tiếp service–service bất đồng bộ: Apache Kafka (topic theo domain event, ví dụ booking.created, payment.completed, booking.cancelled).

- Cache layer: Redis qua giao thức RESP.

## 3.2. Yêu cầu chức năng (Functional Requirements)

Các yêu cầu chức năng được mã hóa theo định dạng FR-\<Nhóm\>-\<Số thứ tự\>, nhóm theo vai trò/tác nhân nghiệp vụ. Mỗi yêu cầu được trình bày độc lập, có thể kiểm chứng (verifiable), và được gắn với một mã Use Case (UC) tương ứng để phục vụ truy vết và viết test case. Mục 3.2.1 liệt kê toàn bộ use case của hệ thống; Mục 3.2.2 trình bày yêu cầu chức năng chi tiết theo nhóm; Mục 3.2.3 trình bày kịch bản (scenario) của từng use case.

### 3.2.1. Danh sách Use Case hệ thống

| **Mã UC** | **Tên Use Case** | **Mô tả ngắn gọn** |
| --- | --- | --- |
| UC-01 | Đăng ký tài khoản | Khách hàng tạo tài khoản mới bằng email/số điện thoại và mật khẩu. |
| UC-02 | Đăng nhập | Người dùng đăng nhập bằng email/mật khẩu để nhận JWT truy cập hệ thống. |
| UC-03 | Đăng nhập bằng Google | Khách hàng đăng nhập/đăng ký nhanh bằng tài khoản Google (OAuth2). |
| UC-04 | Khôi phục mật khẩu | Người dùng đặt lại mật khẩu khi quên thông qua liên kết gửi qua email. |
| UC-05 | Cập nhật hồ sơ cá nhân | Người dùng chỉnh sửa thông tin cá nhân (họ tên, SĐT, ảnh đại diện, địa chỉ). |
| UC-06 | Kiểm soát truy cập theo vai trò (RBAC) | Hệ thống giới hạn quyền truy cập API theo vai trò của người dùng đăng nhập. |
| UC-07 | Khóa/Mở khóa tài khoản | Admin khóa hoặc mở khóa tài khoản người dùng vi phạm. |
| UC-08 | Tìm kiếm khách sạn | Khách hàng tìm kiếm, lọc và sắp xếp danh sách khách sạn theo nhiều tiêu chí. |
| UC-09 | Xem chi tiết khách sạn | Khách hàng xem thông tin chi tiết khách sạn và các loại phòng kèm giá. |
| UC-10 | Đặt phòng và thanh toán (tự động xác nhận) | Khách hàng đặt phòng, thanh toán trực tuyến; hệ thống tự động xác nhận booking khi thanh toán thành công và tự rollback khi thất bại. |
| UC-11 | Hủy đặt phòng & hoàn tiền | Khách hàng hủy booking đã đặt; hệ thống xử lý hoàn tiền theo chính sách hủy. |
| UC-12 | Xem lịch sử đặt phòng | Khách hàng xem lại danh sách và trạng thái các booking đã thực hiện. |
| UC-13 | Nhận thông báo tự động | Hệ thống gửi email/push notification cho các sự kiện booking (xác nhận, hủy, nhắc check-in). |
| UC-14 | Gửi thông báo khuyến mãi | Admin/Owner gửi thông báo khuyến mãi tới nhóm khách hàng mục tiêu. |
| UC-15 | Quản lý phòng | Nhân viên thêm/sửa/xóa phòng và cập nhật trạng thái phòng. |
| UC-16 | Xem danh sách booking | Nhân viên xem danh sách booking theo ngày/trạng thái để xử lý vận hành. |
| UC-17 | Check-in khách | Nhân viên thực hiện check-in cho khách đã có booking ở trạng thái Confirmed. |
| UC-18 | Check-out khách | Nhân viên thực hiện check-out, giải phóng phòng sau khi khách trả phòng. |
| UC-19 | Hủy booking thay khách hàng | Nhân viên hủy booking thay khách hàng trong các trường hợp đặc biệt. |
| UC-20 | Xem thông tin & lịch sử khách hàng | Nhân viên xem hồ sơ và lịch sử lưu trú của khách hàng tại khách sạn. |
| UC-21 | Quản lý khách sạn | Chủ khách sạn thêm/sửa/xóa khách sạn và quản lý hình ảnh khách sạn. |
| UC-22 | Quản lý nhân viên & phân quyền | Chủ khách sạn thêm/sửa/xóa nhân viên và gán quyền hạn. |
| UC-23 | Xem dashboard & thống kê khách sạn | Chủ khách sạn xem doanh thu, công suất phòng, booking, khách hàng theo khách sạn. |
| UC-24 | Cấu hình giá động | Chủ khách sạn cấu hình giá theo mùa, cuối tuần, giá đặc biệt. |
| UC-25 | Kiểm duyệt khách sạn | Admin duyệt, tạm ngưng hoặc kiểm duyệt nội dung khách sạn. |
| UC-26 | Quản lý promotion toàn hệ thống | Admin tạo/quản lý promotion, coupon, campaign áp dụng toàn nền tảng. |
| UC-27 | Xem dashboard hệ thống | Admin xem các chỉ số tổng quan toàn nền tảng. |
| UC-28 | Quản lý subscription tenant | Admin quản lý gói thuê bao của từng tenant. |
| UC-29 | Lấy danh sách loại phòng của khách sạn | Nhân viên xem danh sách các loại phòng đang có tại một khách sạn cụ thể. |
| UC-30 | Xem chi tiết loại phòng | Khách xem chi tiết thông tin một loại phòng (ảnh, giường, tiện ích). |

### 3.2.2. Yêu cầu chức năng theo nhóm

#### 3.2.2.1. Nhóm FR-ACC: Quản lý tài khoản & Xác thực

| **Mã** | **Mô tả yêu cầu** | **Tác nhân** | **Mã UC** |
| --- | --- | --- | --- |
| FR-ACC-01 | Hệ thống phải cho phép Khách hàng đăng ký tài khoản bằng email/số điện thoại và mật khẩu, có xác thực email trước khi kích hoạt. | Customer | UC-01 |
| FR-ACC-02 | Hệ thống phải cho phép đăng nhập bằng email/mật khẩu, trả về cặp Access Token (JWT) và Refresh Token. | Customer/Staff/Owner/Admin | UC-02 |
| FR-ACC-03 | Hệ thống phải cho phép đăng nhập bằng Google OAuth2; nếu email chưa tồn tại thì tự động tạo tài khoản mới, nếu đã tồn tại thì liên kết với tài khoản hiện có. | Customer | UC-03 |
| FR-ACC-04 | Hệ thống phải cho phép khôi phục mật khẩu qua email chứa liên kết đặt lại mật khẩu có thời hạn hiệu lực. | Customer/Staff/Owner | UC-04 |
| FR-ACC-05 | Hệ thống phải cho phép người dùng cập nhật hồ sơ cá nhân (họ tên, số điện thoại, ảnh đại diện, địa chỉ). | Customer/Staff/Owner | UC-05 |
| FR-ACC-06 | Hệ thống phải áp dụng RBAC, phân quyền truy cập API theo vai trò: Customer, Hotel Staff, Hotel Owner, Platform Admin. | Toàn hệ thống | UC-06 |
| FR-ACC-07 | Hệ thống phải cho phép Admin khóa/mở khóa tài khoản người dùng và ghi nhận lý do khóa. | Admin | UC-07 |

#### 3.2.2.2. Nhóm FR-SEARCH: Tìm kiếm khách sạn (tham khảo luồng Booking.com)

Luồng tham khảo: người dùng nhập Điểm đến → chọn Ngày check-in/check-out → chọn Số khách & Số phòng → nhấn Tìm kiếm → hệ thống trả về danh sách khách sạn còn phòng trống phù hợp, sắp xếp mặc định theo độ liên quan, kèm bộ lọc bên cạnh.

| **Mã** | **Mô tả yêu cầu** | **Tác nhân** | **Mã UC** |
| --- | --- | --- | --- |
| FR-SEARCH-01 | Hệ thống phải cho phép tìm kiếm khách sạn theo địa điểm (thành phố, khu vực, tên khách sạn) với gợi ý tự động (autocomplete). | Customer | UC-08 |
| FR-SEARCH-02 | Hệ thống phải cho phép chọn ngày check-in/check-out và chỉ trả về khách sạn/loại phòng còn trống trong khoảng ngày đó. | Customer | UC-08 |
| FR-SEARCH-03 | Hệ thống phải cho phép lọc theo số khách và số phòng, tự động loại các loại phòng không đáp ứng sức chứa. | Customer | UC-08 |
| FR-SEARCH-04 | Hệ thống phải cho phép lọc theo khoảng giá (giá thấp nhất – cao nhất mỗi đêm). | Customer | UC-08 |
| FR-SEARCH-05 | Hệ thống phải cho phép lọc theo tiện ích (wifi, hồ bơi, bãi đỗ xe, bữa sáng, v.v.) — chọn nhiều tiêu chí đồng thời. | Customer | UC-08 |
| FR-SEARCH-06 | Hệ thống phải cho phép lọc theo số sao khách sạn (1-5 sao). | Customer | UC-08 |
| FR-SEARCH-07 | Hệ thống phải cho phép sắp xếp kết quả theo giá (tăng/giảm), đánh giá, hoặc mức độ phù hợp. | Customer | UC-08 |
| FR-SEARCH-08 | Kết quả tìm kiếm phải được lấy ưu tiên từ Redis Cache nếu có sẵn (cache theo tổ hợp địa điểm + ngày + bộ lọc) nhằm giảm tải PostgreSQL. | Hệ thống | UC-08 |

#### 3.2.2.3. Nhóm FR-BOOK: Đặt phòng & Thanh toán (tự động xác nhận)

Luồng tham khảo Booking.com, có điều chỉnh để tự động hoá xác nhận: Xem chi tiết khách sạn → Chọn loại phòng & số lượng → Kiểm tra phòng trống thời gian thực → Nhập thông tin khách lưu trú → Áp mã giảm giá (tuỳ chọn) → Thanh toán → **Hệ thống tự động xác nhận booking ngay khi thanh toán thành công (không qua bước xác nhận thủ công của nhân viên)** → Nhận email/push xác nhận → (tuỳ chọn) Hủy phòng theo chính sách.

| **Mã** | **Mô tả yêu cầu** | **Tác nhân** | **Mã UC** |
| --- | --- | --- | --- |
| FR-BOOK-01 | Hệ thống phải hiển thị trang chi tiết khách sạn gồm: hình ảnh, mô tả, vị trí, tiện ích, đánh giá, danh sách loại phòng kèm giá theo từng ngày trong khoảng tìm kiếm. | Customer | UC-09 |
| FR-BOOK-02 | Hệ thống phải kiểm tra tình trạng phòng trống (availability) theo thời gian thực ngay tại thời điểm khách chọn đặt, tránh overbooking. | Customer | UC-10 |
| FR-BOOK-03 | Hệ thống phải cho phép Khách hàng tạo một yêu cầu đặt phòng (booking) gồm loại phòng, số lượng, ngày nhận/trả phòng, thông tin khách lưu trú. | Customer | UC-10 |
| FR-BOOK-04 | Hệ thống phải cho phép áp dụng mã giảm giá (coupon) hợp lệ vào đơn đặt phòng trước khi thanh toán, tự động tính lại tổng tiền. | Customer | UC-10 |
| FR-BOOK-05 | Hệ thống phải cho phép thanh toán trực tuyến qua VNPay và hỗ trợ chế độ thanh toán mock cho mục đích kiểm thử/demo. | Customer | UC-10 |
| FR-BOOK-06 | Mọi yêu cầu đặt phòng và thanh toán phải mang Idempotency-Key ở HTTP Header; hệ thống phải phát hiện và từ chối xử lý trùng lặp request có cùng khóa trong khoảng thời gian hiệu lực. | Hệ thống | UC-10 |
| FR-BOOK-07 | Hệ thống phải điều phối luồng đặt phòng – giữ chỗ – thanh toán bằng Saga Pattern (Place Booking Service là Orchestrator); booking phải được **tự động chuyển từ trạng thái Pending sang Confirmed ngay khi thanh toán thành công, không cần nhân viên xác nhận thủ công**, và phải đảm bảo rollback (bù trừ) khi một bước thất bại (ví dụ: thanh toán thất bại → tự động hủy giữ chỗ, giải phóng phòng). | Hệ thống | UC-10 |
| FR-BOOK-08 | Hệ thống phải gửi webhook callback xử lý kết quả thanh toán từ VNPay và cập nhật trạng thái booking tương ứng (thành công/thất bại/đang xử lý) một cách tự động. | Hệ thống | UC-10 |
| FR-BOOK-09 | Hệ thống phải lưu lịch sử giao dịch thanh toán (Transaction History) cho mỗi booking, bao gồm trạng thái refund nếu có. | Hệ thống | UC-10 |
| FR-BOOK-10 | Hệ thống phải cho phép Khách hàng hủy đặt phòng theo chính sách hủy của khách sạn (miễn phí/có phí/không hoàn) và tự động kích hoạt luồng refund qua Payment Service nếu đủ điều kiện. | Customer | UC-11 |
| FR-BOOK-11 | Hệ thống phải cho phép Khách hàng xem lịch sử các đặt phòng đã thực hiện, kèm trạng thái hiện tại. | Customer | UC-12 |

#### 3.2.2.4. Nhóm FR-NOTI: Thông báo

| **Mã** | **Mô tả yêu cầu** | **Tác nhân** | **Mã UC** |
| --- | --- | --- | --- |
| FR-NOTI-01 | Hệ thống phải gửi email xác nhận đặt phòng ngay sau khi booking được hệ thống tự động xác nhận thành công. | Hệ thống | UC-13 |
| FR-NOTI-02 | Hệ thống phải gửi email thông báo khi booking bị hủy (do khách hủy hoặc do hệ thống hủy tự động). | Hệ thống | UC-13 |
| FR-NOTI-03 | Hệ thống phải gửi push notification (qua Firebase Cloud Messaging) cho các sự kiện: Booking Confirmed, Booking Cancelled, Check-in Reminder, Promotion. | Hệ thống | UC-13 |
| FR-NOTI-04 | Hệ thống phải cho phép gửi thông báo khuyến mãi theo nhóm khách hàng mục tiêu (segmented push/email). | Admin/Owner | UC-14 |
| FR-NOTI-05 | Việc gửi thông báo phải được thực hiện bất đồng bộ qua Kafka, có cơ chế Retry Queue khi gửi thất bại. | Hệ thống | UC-13 |

#### 3.2.2.5. Nhóm FR-STAFF: Nhân viên khách sạn (Hotel Staff)

> **Lưu ý:** Yêu cầu "Nhân viên xác nhận booking thủ công" đã được loại bỏ khỏi đặc tả này. Việc xác nhận đặt phòng nay được tự động hoá hoàn toàn bởi hệ thống ngay khi thanh toán thành công (xem FR-BOOK-07, UC-10). Nhân viên chỉ còn tiếp nhận booking đã ở trạng thái Confirmed để thực hiện check-in.

| **Mã** | **Mô tả yêu cầu** | **Tác nhân** | **Mã UC** |
| --- | --- | --- | --- |
| FR-STAFF-01 | Hệ thống phải cho phép Nhân viên thêm/sửa/xóa thông tin phòng (số phòng, loại phòng, tầng, mô tả, giá cơ bản). | Hotel Staff | UC-15 |
| FR-STAFF-02 | Hệ thống phải cho phép Nhân viên cập nhật trạng thái phòng (Trống/Đang sử dụng/Đang dọn/Bảo trì). | Hotel Staff | UC-15 |
| FR-STAFF-03 | Hệ thống phải hiển thị danh sách booking theo ngày/trạng thái (đã được hệ thống tự động xác nhận) cho Nhân viên theo dõi và xử lý vận hành. | Hotel Staff | UC-16 |
| FR-STAFF-04 | Hệ thống phải cho phép Nhân viên thực hiện Check-in cho khách có booking ở trạng thái Confirmed, cập nhật trạng thái booking thành "Đang lưu trú" và trạng thái phòng thành "Đang sử dụng". | Hotel Staff | UC-17 |
| FR-STAFF-05 | Hệ thống phải cho phép Nhân viên thực hiện Check-out, cập nhật trạng thái booking thành "Hoàn tất" và giải phóng trạng thái phòng. | Hotel Staff | UC-18 |
| FR-STAFF-06 | Hệ thống phải cho phép Nhân viên hủy booking thay khách hàng trong các trường hợp đặc biệt (có ghi log người thực hiện và lý do). | Hotel Staff | UC-19 |
| FR-STAFF-07 | Hệ thống phải cho phép Nhân viên xem thông tin khách hàng (hồ sơ liên hệ) và lịch sử lưu trú tại khách sạn của mình. | Hotel Staff | UC-20 |

#### 3.2.2.6. Nhóm FR-OWNER: Chủ khách sạn (Hotel Owner)

| **Mã** | **Mô tả yêu cầu** | **Tác nhân** | **Mã UC** |
| --- | --- | --- | --- |
| FR-OWNER-01 | Hệ thống phải cho phép Chủ khách sạn thêm khách sạn mới vào nền tảng (thông tin chờ Admin kiểm duyệt trước khi công khai). | Hotel Owner | UC-21 |
| FR-OWNER-02 | Hệ thống phải cho phép Chủ khách sạn sửa/xóa thông tin khách sạn (mô tả, địa chỉ, tiện ích, chính sách). | Hotel Owner | UC-21 |
| FR-OWNER-03 | Hệ thống phải cho phép Chủ khách sạn quản lý hình ảnh khách sạn (tải lên, xóa, đặt ảnh đại diện) lưu trữ qua MinIO/S3. | Hotel Owner | UC-21 |
| FR-OWNER-04 | Hệ thống phải cho phép Chủ khách sạn thêm/sửa/xóa nhân viên và gán quyền hạn nhân viên theo nhóm chức năng (quản lý phòng, quản lý booking, v.v.). | Hotel Owner | UC-22 |
| FR-OWNER-05 | Hệ thống phải cung cấp Dashboard doanh thu theo khoảng thời gian (ngày/tuần/tháng/năm) cho từng khách sạn thuộc tenant. | Hotel Owner | UC-23 |
| FR-OWNER-06 | Hệ thống phải cung cấp thống kê công suất phòng (occupancy rate) theo khách sạn/loại phòng. | Hotel Owner | UC-23 |
| FR-OWNER-07 | Hệ thống phải cung cấp thống kê booking (số lượng, tỷ lệ hủy, tỷ lệ hoàn tất) theo khoảng thời gian. | Hotel Owner | UC-23 |
| FR-OWNER-08 | Hệ thống phải cung cấp thống kê khách hàng (khách mới/khách quay lại, nguồn đặt phòng). | Hotel Owner | UC-23 |
| FR-OWNER-09 | Hệ thống phải cho phép Chủ khách sạn cấu hình giá theo mùa (Seasonal Pricing) với khoảng ngày hiệu lực và hệ số/giá điều chỉnh. | Hotel Owner | UC-24 |
| FR-OWNER-10 | Hệ thống phải cho phép Chủ khách sạn cấu hình giá cuối tuần (Weekend Pricing) áp dụng tự động vào Thứ Bảy/Chủ Nhật. | Hotel Owner | UC-24 |
| FR-OWNER-11 | Hệ thống phải cho phép Chủ khách sạn tạo giá đặc biệt (Special Pricing) cho sự kiện/ngày cụ thể, có độ ưu tiên cao hơn giá mùa/giá cuối tuần khi trùng ngày. | Hotel Owner | UC-24 |

#### 3.2.2.7. Nhóm FR-ADMIN: Quản trị viên hệ thống (Platform Admin)

| **Mã** | **Mô tả yêu cầu** | **Tác nhân** | **Mã UC** |
| --- | --- | --- | --- |
| FR-ADMIN-01 | Hệ thống phải cho phép Admin duyệt khách sạn mới đăng ký trước khi hiển thị công khai cho khách hàng tìm kiếm. | Admin | UC-25 |
| FR-ADMIN-02 | Hệ thống phải cho phép Admin tạm ngưng (suspend) khách sạn vi phạm chính sách nền tảng, kèm lý do và thông báo tới Chủ khách sạn. | Admin | UC-25 |
| FR-ADMIN-03 | Hệ thống phải cho phép Admin kiểm duyệt nội dung (mô tả, hình ảnh, đánh giá) để gỡ bỏ nội dung vi phạm. | Admin | UC-25 |
| FR-ADMIN-04 | Hệ thống phải cho phép Admin tạo chương trình khuyến mãi (promotion) áp dụng toàn nền tảng, không giới hạn theo một tenant. | Admin | UC-26 |
| FR-ADMIN-05 | Hệ thống phải cho phép Admin quản lý coupon: tạo, sửa, vô hiệu hóa, giới hạn số lần sử dụng/đối tượng áp dụng. | Admin | UC-26 |
| FR-ADMIN-06 | Hệ thống phải cho phép Admin quản lý campaign khuyến mãi theo khoảng thời gian và nhóm khách hàng mục tiêu. | Admin | UC-26 |
| FR-ADMIN-07 | Hệ thống phải cung cấp Dashboard hệ thống hiển thị: tổng số người dùng, tổng số khách sạn, tổng doanh thu toàn nền tảng, tổng số booking, tỷ lệ chuyển đổi (conversion rate). | Admin | UC-27 |
| FR-ADMIN-08 | Hệ thống phải cho phép Admin quản lý gói thuê bao (subscription plan) của từng tenant qua Tenant Service. | Admin | UC-28 |

### 3.2.3. Kịch bản Use Case (Use Case Scenarios)

#### UC-01 — Đăng ký tài khoản

- **Tác nhân:** Customer
- **Tiền điều kiện:** Người dùng chưa có tài khoản trên hệ thống; có địa chỉ email hợp lệ.
- **Kịch bản chính:**
  1. Người dùng chọn "Đăng ký" và nhập email/số điện thoại, mật khẩu, họ tên.
  2. Hệ thống kiểm tra định dạng dữ liệu và kiểm tra email/SĐT chưa tồn tại.
  3. Hệ thống tạo tài khoản ở trạng thái "Chưa xác thực" và gửi email xác thực kèm liên kết kích hoạt.
  4. Người dùng nhấn liên kết trong email để xác thực.
  5. Hệ thống chuyển tài khoản sang trạng thái "Đã kích hoạt" và thông báo đăng ký thành công.
- **Kịch bản ngoại lệ:**
  - Email/SĐT đã tồn tại → hệ thống từ chối, yêu cầu đăng nhập hoặc khôi phục mật khẩu.
  - Mật khẩu không đủ độ mạnh theo chính sách → hệ thống yêu cầu nhập lại.
  - Người dùng không xác thực email trong thời hạn quy định → tài khoản bị xoá/khóa tự động, người dùng phải đăng ký lại.

#### UC-02 — Đăng nhập

- **Tác nhân:** Customer / Hotel Staff / Hotel Owner / Platform Admin
- **Tiền điều kiện:** Người dùng đã có tài khoản hợp lệ và chưa bị khóa.
- **Kịch bản chính:**
  1. Người dùng nhập email và mật khẩu.
  2. Hệ thống xác thực thông tin đăng nhập.
  3. Hệ thống phát hành Access Token (JWT) và Refresh Token, trả về cho client.
  4. Người dùng được chuyển vào giao diện tương ứng với vai trò của mình.
- **Kịch bản ngoại lệ:**
  - Sai email/mật khẩu → hệ thống trả lỗi xác thực, không tiết lộ thông tin nào đúng/sai cụ thể.
  - Tài khoản bị khóa (theo UC-07) → hệ thống từ chối đăng nhập kèm thông báo lý do.
  - Nhập sai quá số lần cho phép → hệ thống tạm khóa đăng nhập trong một khoảng thời gian.

#### UC-03 — Đăng nhập bằng Google

- **Tác nhân:** Customer
- **Tiền điều kiện:** Người dùng có tài khoản Google hợp lệ.
- **Kịch bản chính:**
  1. Người dùng chọn "Đăng nhập với Google".
  2. Hệ thống chuyển hướng tới Google OAuth2 để xác thực và xin quyền truy cập thông tin cơ bản.
  3. Google trả về authorization code/token cho hệ thống.
  4. Hệ thống kiểm tra email từ Google: nếu chưa tồn tại thì tự động tạo tài khoản mới; nếu đã tồn tại thì liên kết với tài khoản hiện có.
  5. Hệ thống phát hành JWT và đăng nhập người dùng vào hệ thống.
- **Kịch bản ngoại lệ:**
  - Người dùng từ chối cấp quyền trên Google → hệ thống hủy luồng đăng nhập, quay lại trang đăng nhập.
  - Email Google đã được liên kết với một tài khoản đăng nhập thường nhưng có xung đột dữ liệu → hệ thống yêu cầu xác minh thêm trước khi liên kết.

#### UC-04 — Khôi phục mật khẩu

- **Tác nhân:** Customer / Hotel Staff / Hotel Owner
- **Tiền điều kiện:** Người dùng có tài khoản đã kích hoạt nhưng quên mật khẩu.
- **Kịch bản chính:**
  1. Người dùng chọn "Quên mật khẩu" và nhập email.
  2. Hệ thống kiểm tra email tồn tại và gửi email chứa liên kết đặt lại mật khẩu có thời hạn.
  3. Người dùng nhấn liên kết, nhập mật khẩu mới.
  4. Hệ thống cập nhật mật khẩu mới và vô hiệu hóa tất cả token đăng nhập cũ.
- **Kịch bản ngoại lệ:**
  - Email không tồn tại trong hệ thống → hệ thống vẫn hiển thị thông báo chung (không tiết lộ email có tồn tại hay không) vì lý do an toàn.
  - Liên kết đặt lại mật khẩu đã hết hạn → hệ thống yêu cầu thực hiện lại từ đầu.

#### UC-05 — Cập nhật hồ sơ cá nhân

- **Tác nhân:** Customer / Hotel Staff / Hotel Owner
- **Tiền điều kiện:** Người dùng đã đăng nhập.
- **Kịch bản chính:**
  1. Người dùng vào trang hồ sơ cá nhân, chỉnh sửa thông tin (họ tên, SĐT, địa chỉ, ảnh đại diện).
  2. Hệ thống kiểm tra hợp lệ dữ liệu nhập.
  3. Hệ thống lưu thông tin mới và hiển thị xác nhận thành công.
- **Kịch bản ngoại lệ:**
  - Dữ liệu không hợp lệ (SĐT sai định dạng...) → hệ thống từ chối lưu, báo lỗi cụ thể từng trường.
  - Ảnh đại diện vượt quá kích thước cho phép → hệ thống từ chối tải lên.

#### UC-06 — Kiểm soát truy cập theo vai trò (RBAC)

- **Tác nhân:** Hệ thống (áp dụng cho mọi vai trò)
- **Tiền điều kiện:** Người dùng đã đăng nhập và có JWT hợp lệ chứa thông tin vai trò.
- **Kịch bản chính:**
  1. Client gửi request kèm JWT tới API Gateway.
  2. Gateway xác thực JWT và trích xuất vai trò người dùng.
  3. Service đích kiểm tra vai trò có đủ quyền truy cập endpoint/tài nguyên được yêu cầu hay không.
  4. Nếu hợp lệ, hệ thống cho phép thực hiện hành động.
- **Kịch bản ngoại lệ:**
  - JWT không hợp lệ/hết hạn → hệ thống trả lỗi 401, yêu cầu đăng nhập lại hoặc refresh token.
  - Vai trò không đủ quyền (ví dụ Customer gọi API quản trị) → hệ thống trả lỗi 403 Forbidden.

#### UC-07 — Khóa/Mở khóa tài khoản

- **Tác nhân:** Platform Admin
- **Tiền điều kiện:** Admin đã đăng nhập; tài khoản mục tiêu tồn tại trên hệ thống.
- **Kịch bản chính:**
  1. Admin tìm và chọn tài khoản cần khóa.
  2. Admin nhập lý do khóa và xác nhận hành động.
  3. Hệ thống chuyển trạng thái tài khoản sang "Bị khóa", vô hiệu hóa toàn bộ token (refresh token) hiện tại của tài khoản đó.
  4. Hệ thống ghi log hành động và (tuỳ chọn) gửi thông báo tới người dùng bị khóa.
- **Kịch bản ngoại lệ:**
  - Admin thao tác mở khóa cho tài khoản chưa từng bị khóa → hệ thống báo trạng thái không hợp lệ.
  - Tài khoản đang trong giao dịch booking/payment khi bị khóa → hệ thống vẫn cho giao dịch đang xử lý hoàn tất theo luồng Saga, chỉ chặn các hành động mới.

#### UC-08 — Tìm kiếm khách sạn

- **Tác nhân:** Customer
- **Tiền điều kiện:** Không yêu cầu đăng nhập.
- **Kịch bản chính:**
  1. Khách hàng nhập địa điểm, ngày check-in/check-out, số khách, số phòng.
  2. Khách hàng (tuỳ chọn) áp thêm bộ lọc: khoảng giá, tiện ích, số sao và chọn cách sắp xếp.
  3. Hệ thống kiểm tra Redis Cache theo tổ hợp tham số tìm kiếm; nếu có cache hợp lệ thì trả kết quả ngay.
  4. Nếu không có cache, hệ thống truy vấn Hotel Service/Booking Service để lấy danh sách khách sạn còn phòng trống phù hợp, lưu kết quả vào Redis Cache rồi trả về.
  5. Hệ thống hiển thị danh sách kết quả theo thứ tự sắp xếp đã chọn.
- **Kịch bản ngoại lệ:**
  - Không có khách sạn nào phù hợp → hệ thống hiển thị thông báo không có kết quả và gợi ý nới rộng tiêu chí.
  - Redis Cache không khả dụng → hệ thống tự động fallback truy vấn trực tiếp PostgreSQL, ghi log cảnh báo.

#### UC-09 — Xem chi tiết khách sạn

- **Tác nhân:** Customer
- **Tiền điều kiện:** Khách sạn đã được Admin duyệt và đang hiển thị công khai.
- **Kịch bản chính:**
  1. Khách hàng chọn một khách sạn từ kết quả tìm kiếm.
  2. Hệ thống hiển thị thông tin chi tiết: hình ảnh, mô tả, vị trí, tiện ích, đánh giá.
  3. Hệ thống hiển thị danh sách loại phòng kèm giá theo từng ngày trong khoảng khách đã tìm kiếm.
- **Kịch bản ngoại lệ:**
  - Khách sạn đã bị Admin tạm ngưng sau khi xuất hiện trong cache tìm kiếm → hệ thống thông báo khách sạn tạm thời không khả dụng.
  - Không còn loại phòng nào trống trong khoảng ngày đã chọn → hệ thống vẫn hiển thị thông tin khách sạn nhưng disable nút đặt phòng.

#### UC-10 — Đặt phòng và thanh toán (tự động xác nhận)

- **Tác nhân:** Customer (chính); Hệ thống/Place Booking Service, Payment Service, Promotion Service, Notification Service (hỗ trợ tự động)
- **Tiền điều kiện:** Khách hàng đã đăng nhập; đã chọn loại phòng, số lượng, ngày nhận/trả phòng tại trang chi tiết khách sạn.
- **Kịch bản chính:**
  1. Khách hàng nhập thông tin khách lưu trú và (tuỳ chọn) áp mã giảm giá; hệ thống gọi Promotion Service kiểm tra hiệu lực coupon và tính lại tổng tiền.
  2. Khách hàng nhấn "Đặt phòng"; client gửi request kèm Idempotency-Key.
  3. Place Booking Service (Saga Orchestrator) kiểm tra availability thời gian thực, giữ chỗ tạm thời (trạng thái Pending) và phát sự kiện yêu cầu thanh toán.
  4. Khách hàng được chuyển tới cổng thanh toán VNPay để thanh toán.
  5. Payment Service nhận kết quả thanh toán qua webhook callback; nếu thành công, phát sự kiện payment.completed.
  6. Saga Orchestrator nhận sự kiện thành công, **tự động chuyển trạng thái booking từ Pending sang Confirmed** mà không cần nhân viên xác nhận thủ công, và lưu Transaction History.
  7. Notification Service gửi email và push notification xác nhận đặt phòng thành công tới khách hàng.
- **Kịch bản ngoại lệ:**
  - Phòng không còn trống tại thời điểm giữ chỗ (do người khác đặt trước) → hệ thống từ chối yêu cầu, thông báo khách hàng chọn lại.
  - Thanh toán thất bại hoặc hết thời gian chờ → Saga Orchestrator tự động thực hiện bước bù trừ: hủy giữ chỗ, giải phóng phòng, chuyển booking sang trạng thái "Đã hủy do thanh toán thất bại", gửi thông báo cho khách hàng.
  - Request đặt phòng bị gửi trùng lặp (cùng Idempotency-Key) → hệ thống phát hiện và trả lại kết quả của lần xử lý đầu tiên, không tạo booking/thanh toán mới.
  - Webhook từ VNPay phản hồi chậm/không đến → hệ thống có cơ chế truy vấn lại trạng thái giao dịch (reconciliation) theo lịch để tránh booking bị "treo" ở Pending quá lâu.
  - Mã giảm giá không còn hiệu lực tại thời điểm thanh toán (hết hạn/hết lượt) → hệ thống tự loại coupon, tính lại tổng tiền và yêu cầu khách hàng xác nhận lại trước khi thanh toán.

#### UC-11 — Hủy đặt phòng & hoàn tiền

- **Tác nhân:** Customer (chính); Payment Service (hỗ trợ tự động)
- **Tiền điều kiện:** Booking đang ở trạng thái Confirmed hoặc Đang lưu trú; chưa check-out.
- **Kịch bản chính:**
  1. Khách hàng chọn booking cần hủy từ lịch sử đặt phòng và xác nhận hủy.
  2. Hệ thống kiểm tra chính sách hủy của khách sạn áp dụng cho booking (miễn phí/có phí/không hoàn) theo thời điểm hủy.
  3. Hệ thống chuyển trạng thái booking sang "Đã hủy" và giải phóng phòng.
  4. Nếu đủ điều kiện hoàn tiền, hệ thống tự động kích hoạt luồng refund qua Payment Service.
  5. Notification Service gửi email/push thông báo hủy phòng thành công tới khách hàng.
- **Kịch bản ngoại lệ:**
  - Booking đã check-in (đang lưu trú) → hệ thống từ chối hủy theo luồng thông thường, yêu cầu liên hệ trực tiếp khách sạn.
  - Refund tới Payment Service thất bại → hệ thống ghi nhận trạng thái "Hoàn tiền đang xử lý/thất bại", lên lịch retry, đồng thời thông báo cho khách hàng và Admin theo dõi.

#### UC-12 — Xem lịch sử đặt phòng

- **Tác nhân:** Customer
- **Tiền điều kiện:** Khách hàng đã đăng nhập và có ít nhất một booking.
- **Kịch bản chính:**
  1. Khách hàng vào trang "Lịch sử đặt phòng".
  2. Hệ thống truy vấn Booking Service lấy danh sách booking của khách hàng kèm trạng thái hiện tại.
  3. Hệ thống hiển thị danh sách, khách hàng có thể chọn xem chi tiết từng booking.
- **Kịch bản ngoại lệ:**
  - Khách hàng chưa từng đặt phòng → hệ thống hiển thị danh sách trống kèm gợi ý tìm khách sạn.

#### UC-13 — Nhận thông báo tự động

- **Tác nhân:** Hệ thống (Notification Service); Customer là người nhận
- **Tiền điều kiện:** Một sự kiện nghiệp vụ (đặt phòng thành công, hủy phòng, sắp đến ngày check-in...) đã xảy ra.
- **Kịch bản chính:**
  1. Service nghiệp vụ liên quan (Booking, Place Booking, Payment) phát sự kiện lên Kafka.
  2. Notification Service tiêu thụ sự kiện, xác định loại thông báo và kênh gửi (email và/hoặc FCM push).
  3. Notification Service gửi thông báo tới khách hàng qua SMTP và/hoặc Firebase Cloud Messaging.
- **Kịch bản ngoại lệ:**
  - Gửi email/push thất bại (timeout, lỗi nhà cung cấp) → message được đưa vào Retry Queue, thử lại theo chính sách backoff; nếu vẫn thất bại sau số lần tối đa thì ghi log lỗi để xử lý thủ công.
  - Khách hàng chưa cấp quyền nhận push notification trên thiết bị → hệ thống chỉ gửi qua email, không báo lỗi cho luồng chính.

#### UC-14 — Gửi thông báo khuyến mãi

- **Tác nhân:** Platform Admin / Hotel Owner
- **Tiền điều kiện:** Đã có chương trình khuyến mãi/coupon được tạo (UC-26) hoặc thông tin khuyến mãi của khách sạn.
- **Kịch bản chính:**
  1. Admin/Owner chọn nhóm khách hàng mục tiêu (toàn bộ, theo khu vực, theo lịch sử đặt phòng...).
  2. Admin/Owner soạn nội dung thông báo và xác nhận gửi.
  3. Hệ thống phát sự kiện lên Kafka; Notification Service gửi email/push tới từng khách hàng trong nhóm mục tiêu.
- **Kịch bản ngoại lệ:**
  - Nhóm khách hàng mục tiêu trống (không khớp điều kiện lọc) → hệ thống cảnh báo trước khi gửi, không phát sự kiện thông báo.

#### UC-15 — Quản lý phòng

- **Tác nhân:** Hotel Staff
- **Tiền điều kiện:** Nhân viên đã đăng nhập và được gán quyền quản lý phòng tại khách sạn của mình.
- **Kịch bản chính:**
  1. Nhân viên chọn thêm/sửa/xóa phòng hoặc cập nhật trạng thái phòng (Trống/Đang sử dụng/Đang dọn/Bảo trì).
  2. Hệ thống kiểm tra hợp lệ dữ liệu và quyền hạn của nhân viên đối với khách sạn liên quan.
  3. Hệ thống lưu thay đổi và cập nhật lại thông tin phòng trên Hotel Service (đồng thời vô hiệu hóa cache liên quan nếu có).
- **Kịch bản ngoại lệ:**
  - Xóa phòng đang có booking hiệu lực trong tương lai → hệ thống từ chối xóa, yêu cầu xử lý booking liên quan trước.
  - Nhân viên không có quyền với khách sạn đang chỉnh sửa → hệ thống trả lỗi 403 Forbidden.

#### UC-16 — Xem danh sách booking

- **Tác nhân:** Hotel Staff
- **Tiền điều kiện:** Nhân viên đã đăng nhập, có ít nhất một booking tại khách sạn được gán.
- **Kịch bản chính:**
  1. Nhân viên chọn xem danh sách booking theo ngày và/hoặc trạng thái.
  2. Hệ thống truy vấn Booking Service trả về danh sách booking đã được hệ thống tự động xác nhận hoặc đang xử lý.
  3. Hệ thống hiển thị danh sách kèm trạng thái hiện tại của từng booking.
- **Kịch bản ngoại lệ:**
  - Không có booking nào khớp bộ lọc đã chọn → hệ thống hiển thị danh sách trống.

#### UC-17 — Check-in khách

- **Tác nhân:** Hotel Staff
- **Tiền điều kiện:** Booking đang ở trạng thái Confirmed (đã được hệ thống tự động xác nhận); đúng ngày check-in.
- **Kịch bản chính:**
  1. Nhân viên tìm booking của khách trong danh sách (theo tên/mã booking).
  2. Nhân viên xác minh thông tin khách lưu trú và nhấn "Check-in".
  3. Hệ thống chuyển trạng thái booking thành "Đang lưu trú" và trạng thái phòng thành "Đang sử dụng".
- **Kịch bản ngoại lệ:**
  - Booking chưa ở trạng thái Confirmed (ví dụ vẫn đang Pending do thanh toán chưa hoàn tất) → hệ thống từ chối check-in, thông báo lý do.
  - Khách đến check-in sớm/muộn so với ngày đã đặt → hệ thống cảnh báo nhưng vẫn cho phép nhân viên quyết định xử lý theo chính sách khách sạn.

#### UC-18 — Check-out khách

- **Tác nhân:** Hotel Staff
- **Tiền điều kiện:** Booking đang ở trạng thái "Đang lưu trú".
- **Kịch bản chính:**
  1. Nhân viên chọn booking cần check-out.
  2. Nhân viên xác nhận hoàn tất lưu trú (kiểm tra phòng, phụ phí nếu có).
  3. Hệ thống chuyển trạng thái booking thành "Hoàn tất" và giải phóng trạng thái phòng về "Đang dọn"/"Trống".
- **Kịch bản ngoại lệ:**
  - Phát sinh phụ phí chưa thanh toán tại thời điểm check-out → hệ thống cho phép nhân viên ghi nhận khoản phụ phí trước khi hoàn tất check-out (xử lý ngoài phạm vi Saga đặt phòng chính).

#### UC-19 — Hủy booking thay khách hàng

- **Tác nhân:** Hotel Staff
- **Tiền điều kiện:** Booking đang ở trạng thái Confirmed hoặc Pending; có yêu cầu hủy đặc biệt từ khách hàng (qua điện thoại, tại quầy...).
- **Kịch bản chính:**
  1. Nhân viên tìm booking cần hủy và chọn "Hủy booking".
  2. Nhân viên nhập lý do hủy.
  3. Hệ thống chuyển trạng thái booking sang "Đã hủy", giải phóng phòng, ghi log nhân viên thực hiện và lý do.
  4. Hệ thống kích hoạt luồng refund (nếu đủ điều kiện) tương tự UC-11.
- **Kịch bản ngoại lệ:**
  - Booking đã ở trạng thái "Đang lưu trú" hoặc "Hoàn tất" → hệ thống từ chối hủy.

#### UC-20 — Xem thông tin & lịch sử khách hàng

- **Tác nhân:** Hotel Staff
- **Tiền điều kiện:** Nhân viên đã đăng nhập; khách hàng có ít nhất một booking tại khách sạn của nhân viên.
- **Kịch bản chính:**
  1. Nhân viên tìm kiếm khách hàng theo tên/mã booking/số điện thoại.
  2. Hệ thống hiển thị hồ sơ liên hệ và lịch sử lưu trú của khách hàng tại khách sạn đó.
- **Kịch bản ngoại lệ:**
  - Khách hàng chưa từng lưu trú tại khách sạn này → hệ thống chỉ hiển thị thông tin liên hệ cơ bản (nếu được phép) không có lịch sử lưu trú.

#### UC-21 — Quản lý khách sạn

- **Tác nhân:** Hotel Owner
- **Tiền điều kiện:** Chủ khách sạn đã đăng nhập.
- **Kịch bản chính:**
  1. Chủ khách sạn thêm khách sạn mới (nhập thông tin, tiện ích, hình ảnh) hoặc chọn sửa/xóa khách sạn hiện có.
  2. Hệ thống lưu thông tin; nếu là khách sạn mới, chuyển trạng thái "Chờ duyệt" và gửi yêu cầu kiểm duyệt tới Admin (UC-25).
  3. Hệ thống xác nhận thao tác thành công.
- **Kịch bản ngoại lệ:**
  - Xóa khách sạn đang có booking hiệu lực trong tương lai → hệ thống từ chối xóa, yêu cầu xử lý booking liên quan trước.
  - Hình ảnh tải lên không đúng định dạng/kích thước → hệ thống từ chối và thông báo lỗi cụ thể.

#### UC-22 — Quản lý nhân viên & phân quyền

- **Tác nhân:** Hotel Owner
- **Tiền điều kiện:** Chủ khách sạn đã đăng nhập và có ít nhất một khách sạn được duyệt.
- **Kịch bản chính:**
  1. Chủ khách sạn thêm nhân viên mới (nhập thông tin liên hệ, gán khách sạn quản lý) hoặc sửa/xóa nhân viên hiện có.
  2. Chủ khách sạn gán quyền hạn nhân viên theo nhóm chức năng (quản lý phòng, quản lý booking...).
  3. Hệ thống tạo/cập nhật tài khoản nhân viên tương ứng và lưu phân quyền.
- **Kịch bản ngoại lệ:**
  - Email nhân viên đã tồn tại trong hệ thống ở vai trò khác → hệ thống cảnh báo xung đột vai trò trước khi xác nhận.

#### UC-23 — Xem dashboard & thống kê khách sạn

- **Tác nhân:** Hotel Owner
- **Tiền điều kiện:** Chủ khách sạn đã đăng nhập và có dữ liệu booking/doanh thu phát sinh.
- **Kịch bản chính:**
  1. Chủ khách sạn chọn khoảng thời gian và khách sạn cần xem báo cáo.
  2. Hệ thống tổng hợp dữ liệu doanh thu, công suất phòng, booking, khách hàng tương ứng.
  3. Hệ thống hiển thị dashboard dạng biểu đồ/bảng số liệu.
- **Kịch bản ngoại lệ:**
  - Không có dữ liệu trong khoảng thời gian đã chọn → hệ thống hiển thị dashboard trống kèm thông báo phù hợp.

#### UC-24 — Cấu hình giá động

- **Tác nhân:** Hotel Owner
- **Tiền điều kiện:** Chủ khách sạn đã đăng nhập và có khách sạn/loại phòng đang hoạt động.
- **Kịch bản chính:**
  1. Chủ khách sạn chọn loại cấu hình giá (Seasonal/Weekend/Special Pricing).
  2. Chủ khách sạn nhập khoảng ngày hiệu lực và giá/hệ số điều chỉnh.
  3. Hệ thống lưu cấu hình và áp dụng tự động vào giá hiển thị khi khách hàng tìm kiếm/đặt phòng trong khoảng ngày tương ứng.
- **Kịch bản ngoại lệ:**
  - Có nhiều cấu hình giá trùng ngày → hệ thống áp dụng theo độ ưu tiên: Special Pricing > Weekend Pricing > Seasonal Pricing.
  - Khoảng ngày cấu hình không hợp lệ (ngày kết thúc trước ngày bắt đầu) → hệ thống từ chối lưu.

#### UC-25 — Kiểm duyệt khách sạn

- **Tác nhân:** Platform Admin
- **Tiền điều kiện:** Có khách sạn ở trạng thái "Chờ duyệt" hoặc bị báo cáo vi phạm.
- **Kịch bản chính:**
  1. Admin xem danh sách khách sạn chờ duyệt/bị báo cáo.
  2. Admin kiểm tra thông tin, hình ảnh, nội dung khách sạn.
  3. Admin duyệt (chuyển trạng thái "Đã duyệt", hiển thị công khai) hoặc từ chối/tạm ngưng kèm lý do.
  4. Hệ thống thông báo kết quả kiểm duyệt tới Chủ khách sạn.
- **Kịch bản ngoại lệ:**
  - Khách sạn bị tạm ngưng khi đang có booking hiệu lực → hệ thống vẫn cho các booking đã Confirmed tiếp tục xử lý, chỉ chặn việc tạo booking mới.

#### UC-26 — Quản lý promotion toàn hệ thống

- **Tác nhân:** Platform Admin
- **Tiền điều kiện:** Admin đã đăng nhập.
- **Kịch bản chính:**
  1. Admin tạo chương trình khuyến mãi/coupon/campaign mới, thiết lập điều kiện áp dụng (thời gian, giới hạn số lượt, đối tượng).
  2. Hệ thống lưu thông tin promotion vào Promotion Service và kích hoạt theo thời gian hiệu lực.
  3. Coupon/campaign khả dụng để khách hàng áp dụng khi đặt phòng (UC-10) trên toàn nền tảng.
- **Kịch bản ngoại lệ:**
  - Admin sửa/vô hiệu hóa coupon đang được áp dụng trong một giao dịch đặt phòng đang xử lý → giao dịch đó vẫn được hoàn tất với điều kiện coupon tại thời điểm áp dụng, thay đổi chỉ ảnh hưởng tới các giao dịch mới.

#### UC-27 — Xem dashboard hệ thống

- **Tác nhân:** Platform Admin
- **Tiền điều kiện:** Admin đã đăng nhập.
- **Kịch bản chính:**
  1. Admin truy cập trang Dashboard hệ thống.
  2. Hệ thống tổng hợp số liệu toàn nền tảng: tổng người dùng, tổng khách sạn, tổng doanh thu, tổng booking, tỷ lệ chuyển đổi.
  3. Hệ thống hiển thị các chỉ số dưới dạng biểu đồ/số liệu tổng quan.
- **Kịch bản ngoại lệ:**
  - Một trong các service nguồn dữ liệu tạm thời không khả dụng → hệ thống hiển thị các chỉ số còn lấy được, đánh dấu rõ chỉ số nào đang lỗi/thiếu dữ liệu.

#### UC-28 — Quản lý subscription tenant

- **Tác nhân:** Platform Admin
- **Tiền điều kiện:** Admin đã đăng nhập; tenant đã đăng ký trên nền tảng.
- **Kịch bản chính:**
  1. Admin chọn tenant cần quản lý gói thuê bao.
  2. Admin xem/điều chỉnh gói thuê bao hiện tại (nâng cấp/hạ cấp/gia hạn).
  3. Hệ thống cập nhật trạng thái subscription của tenant và áp dụng giới hạn/quyền tương ứng.
- **Kịch bản ngoại lệ:**
  - Tenant đang nợ phí thuê bao → hệ thống có thể giới hạn một số chức năng cho tới khi thanh toán được xử lý, theo chính sách kinh doanh được cấu hình.

#### UC-29 — Lấy danh sách loại phòng của khách sạn

- **Tác nhân:** Hotel Staff/Hotel Owner
- **Tiền điều kiện:** Người dùng đã đăng nhập, được phân quyền với khách sạn cần xem; khách sạn tồn tại và chưa bị xóa.
- **Kịch bản chính:**
  1. Người dùng mở chức năng quản lý loại phòng của một khách sạn.
  2. Client gửi `GET /api/hotels/{hotelId}/room-types` qua API Gateway.
  3. Hotel Service kiểm tra quyền truy cập của người dùng đối với khách sạn.
  4. Hotel Service kiểm tra cache theo khóa `hotel_room_types:{hotelId}`.
  5. Nếu cache hợp lệ, hệ thống trả danh sách loại phòng từ cache.
  6. Nếu cache không tồn tại hoặc đã hết hạn, Hotel Service kiểm tra khách sạn và truy vấn toàn bộ loại phòng chưa bị xóa thuộc khách sạn đó.
  7. Hệ thống tổng hợp thông tin tóm tắt của từng loại phòng gồm tên, giá cơ bản, sức chứa, số giường, loại giường, ảnh đại diện và tổng số phòng.
  8. Hotel Service lưu kết quả vào cache và trả `200 OK` cùng danh sách loại phòng.
- **Kịch bản ngoại lệ:**
  - Người dùng không được phân quyền với khách sạn → hệ thống trả `403 Forbidden`.
  - Không tìm thấy khách sạn hoặc khách sạn đã bị xóa → hệ thống trả `404 Not Found`.
  - Khách sạn chưa có loại phòng → hệ thống trả `200 OK` với danh sách rỗng.
  - Redis không khả dụng → Hotel Service truy vấn trực tiếp `hotel_db`, trả kết quả bình thường và ghi log cảnh báo.
  - Tham số `hotelId` không đúng định dạng → hệ thống trả `400 Bad Request`.

#### UC-30 — Xem chi tiết loại phòng

- **Tác nhân:** Customer
- **Tiền điều kiện:** Loại phòng tồn tại, chưa bị xóa và thuộc một khách sạn đang được phép hiển thị.
- **Kịch bản chính:**
  1. Khách hàng chọn một loại phòng từ danh sách loại phòng của khách sạn.
  2. Client gửi `GET /api/room-types/{roomTypeId}` qua API Gateway.
  3. Hotel Service kiểm tra cache theo khóa `room_type_detail:{roomTypeId}`.
  4. Nếu cache hợp lệ, hệ thống trả chi tiết loại phòng từ cache.
  5. Nếu cache không tồn tại hoặc đã hết hạn, Hotel Service truy vấn loại phòng, danh sách tiện ích chưa bị xóa và năm hình ảnh đầu tiên.
  6. Hệ thống tạo dữ liệu phản hồi gồm tên, mô tả, sức chứa, thông tin giường, giá cơ bản, tiện ích và hình ảnh.
  7. Hotel Service lưu kết quả vào cache và trả `200 OK` cùng chi tiết loại phòng.
- **Kịch bản ngoại lệ:**
  - Không tìm thấy loại phòng hoặc loại phòng đã bị xóa → hệ thống trả `404 Not Found`.
  - Khách sạn chứa loại phòng đã bị tạm ngưng → hệ thống trả `404 Not Found` để không công khai tài nguyên.
  - Loại phòng không có tiện ích hoặc hình ảnh → hệ thống vẫn trả `200 OK` với danh sách tương ứng rỗng.
  - Redis không khả dụng → Hotel Service truy vấn trực tiếp `hotel_db`, trả kết quả bình thường và ghi log cảnh báo.
  - Tham số `roomTypeId` không đúng định dạng → hệ thống trả `400 Bad Request`.

## 3.3. Yêu cầu phi chức năng (Non-Functional Requirements)

### 3.3.1. Hiệu năng (Performance Requirements)

| **Mã** | **Yêu cầu** | **Ngưỡng đo** |
| --- | --- | --- |
| NFR-PERF-01 | Thời gian phản hồi API tìm kiếm khách sạn | < 300 ms (P95), có sử dụng Redis Cache |
| NFR-PERF-02 | Thời gian hoàn tất một luồng đặt phòng (từ submit đến xác nhận giữ chỗ) | < 2 giây (P95) |
| NFR-PERF-03 | Thời gian tải Dashboard (Owner/Admin) | < 1 giây (P95) với dữ liệu đã tổng hợp sẵn |
| NFR-PERF-04 | Khả năng chịu tải đồng thời tại API Gateway | Tối thiểu 1.000 request/giây ở mức tải cao điểm, có rate limiting bảo vệ |

### 3.3.2. Khả năng mở rộng (Scalability)

- Tất cả service phải stateless ở tầng ứng dụng để hỗ trợ Horizontal Scaling (chạy nhiều instance song song).

- Mỗi service phải triển khai độc lập (Independent Deployment), không yêu cầu downtime toàn hệ thống khi cập nhật một service.

- Dữ liệu phiên/cache dùng chung (Redis) phải tách khỏi tầng ứng dụng để các instance chia sẻ trạng thái nhất quán.

### 3.3.3. Bảo mật (Security)

- Xác thực bắt buộc bằng JWT cho mọi API yêu cầu đăng nhập; Access Token có thời hạn ngắn, Refresh Token có thời hạn dài hơn và có thể bị revoke.

- Hỗ trợ đăng nhập OAuth2 (Google) tuân thủ chuẩn OpenID Connect.

- Áp dụng RBAC chi tiết tới cấp API endpoint, đảm bảo một vai trò chỉ truy cập đúng phạm vi nghiệp vụ được phép (ví dụ: Hotel Staff chỉ truy cập dữ liệu khách sạn thuộc tenant của mình).

- Áp dụng API Rate Limiting tại API Gateway để chống lạm dụng/DDoS ở mức ứng dụng.

- Toàn bộ giao tiếp phải qua HTTPS/TLS; không truyền dữ liệu nhạy cảm (mật khẩu, token thanh toán) ở dạng plaintext.

- Dữ liệu thanh toán nhạy cảm không được lưu trực tiếp trong hệ thống; tuân thủ mô hình tokenization của VNPay (PCI-DSS scope giảm thiểu).

- Cách ly dữ liệu đa tenant (tenant isolation): mọi truy vấn dữ liệu nghiệp vụ phải kèm điều kiện lọc theo tenant_id ở tầng service/repository.

### 3.3.4. Độ sẵn sàng & độ tin cậy (Availability & Reliability)

- Mục tiêu uptime: 99.9% (tương đương downtime tối đa ~8,76 giờ/năm), không bao gồm bảo trì có lịch trình thông báo trước.

- Áp dụng Retry Pattern có giới hạn số lần và backoff cho các lời gọi service-to-service không đảm bảo tính idempotent tự nhiên.

- Áp dụng Circuit Breaker (Resilience4j) cho các lời gọi tới Payment Service và các dịch vụ bên thứ ba (VNPay/FCM) để tránh hiệu ứng domino khi một thành phần gặp lỗi.

- Áp dụng Kafka Retry Queue/Dead Letter Queue cho các message xử lý thất bại, đảm bảo không mất sự kiện nghiệp vụ quan trọng (booking, payment).

### 3.3.5. Tính nhất quán dữ liệu (Data Consistency)

- Giao dịch đặt phòng – thanh toán phải đảm bảo Eventual Consistency thông qua Saga Pattern, có bước bù trừ (compensating transaction) khi một bước trong saga thất bại, bao gồm cả việc tự động xác nhận booking khi thành công.

- Mọi API thay đổi trạng thái có khả năng bị gọi lại (đặt phòng, thanh toán, hủy phòng) phải đảm bảo Idempotency thông qua Idempotency-Key kết hợp lưu trữ tạm trên Redis.

### 3.3.6. Khả năng bảo trì & vận hành (Maintainability & Operability)

- Toàn bộ service phải đóng gói bằng Docker, có thể khởi chạy cụm phát triển qua Docker Compose.

- Log của toàn hệ thống phải được tập trung qua ELK Stack/OpenSearch, có thể truy vấn theo correlation-id của một request xuyên các service (distributed tracing).

- Chỉ số vận hành (CPU, memory, request rate, error rate, latency) phải được thu thập qua Prometheus và hiển thị trực quan qua Grafana.

### 3.3.7. Khả năng di động & tương thích (Portability)

- Frontend phải tương thích các trình duyệt phổ biến hiện hành (Chrome, Edge, Firefox, Safari, 2 phiên bản gần nhất).

- Backend phải chạy được trên môi trường container hóa độc lập với hạ tầng cloud cụ thể (tránh phụ thuộc cứng vào một nhà cung cấp cloud).

## 3.4. Ràng buộc thiết kế (Design Constraints)

- Kiến trúc: Microservices, API-first, Database-per-Service.

- Saga Orchestration tập trung tại Place Booking Service cho luồng đặt phòng – thanh toán – tự động xác nhận.

- Bắt buộc sử dụng Spring Cloud Gateway làm điểm vào duy nhất cho client (không cho phép client gọi trực tiếp tới service nội bộ).

- Bắt buộc sử dụng Eureka cho Service Discovery (không hardcode địa chỉ service).

- Cache Redis bắt buộc cho: danh sách khách sạn, kết quả tìm kiếm, thông tin phòng, dữ liệu dashboard đã tổng hợp.

## 3.5. Thuộc tính hệ thống phần mềm khác (Other Software System Attributes)

### 3.5.1. Quản lý cấu hình (Configuration Management)

Cấu hình từng service phải tách biệt theo môi trường (dev/staging/production), quản lý qua biến môi trường hoặc Spring Cloud Config, không hard-code thông tin nhạy cảm (secret, API key) trong source code.

### 3.5.2. Khả năng kiểm thử (Testability)

Mỗi service phải hỗ trợ chế độ thanh toán mock (sandbox) để phục vụ kiểm thử tự động (unit test, integration test) không phát sinh giao dịch thật; các luồng nghiệp vụ trọng yếu (đặt phòng tự động xác nhận, thanh toán, hủy phòng, check-in/check-out) phải có bộ test case tương ứng được quản lý trong Test Plan riêng.

# 4. PHỤ LỤC

## 4.1. Ma trận truy vết yêu cầu theo vai trò (Traceability theo Actor)

| **Vai trò** | **Nhóm yêu cầu chức năng liên quan** | **Use Case liên quan** |
| --- | --- | --- |
| Khách hàng (Customer) | FR-ACC-01..05, FR-SEARCH-01..08, FR-BOOK-01..11, FR-NOTI-01..03 | UC-01..05, UC-08..13 |
| Nhân viên khách sạn (Hotel Staff) | FR-ACC-02, FR-ACC-05, FR-STAFF-01..07 | UC-02, UC-05, UC-15..20 |
| Chủ khách sạn (Hotel Owner) | FR-ACC-02, FR-ACC-05, FR-OWNER-01..11 | UC-02, UC-05, UC-21..24 |
| Quản trị viên (Platform Admin) | FR-ACC-06, FR-ACC-07, FR-ADMIN-01..08 | UC-06, UC-07, UC-25..28 |

## 4.2. Bảng đối chiếu hiện trạng – mục tiêu

Bảng dưới tổng hợp việc hợp nhất phạm vi giữa hệ thống hiện tại và phần mở rộng, làm cơ sở lập kế hoạch phát triển theo từng sprint/release; trong đặc tả này cả hai được trình bày như một hệ thống thống nhất ở các mục 3.2 và 3.3.

| **Hạng mục** | **Trạng thái trước khi hợp nhất** |
| --- | --- |
| Tìm kiếm, xem chi tiết, kiểm tra availability, đặt phòng, thanh toán mock, email, API Gateway, Eureka, Kafka, Saga, Database-per-Service, Circuit Breaker cho Payment | Đã triển khai trong hệ thống hiện tại |
| Multi-tenant, quản lý nhân viên, Promotion/Coupon, Dashboard, Redis Cache, OAuth2 Login, Firebase Notification, VNPay thật, Idempotency, Monitoring, Logging, Distributed Tracing | Yêu cầu phát triển bổ sung |

## 4.3. Ghi chú phiên bản

- **Phiên bản 1.0** — Soạn thảo trên cơ sở tài liệu yêu cầu mở rộng nội bộ và luồng nghiệp vụ tham khảo Booking.com.

- **Phiên bản 1.1** -  Bổ sung Mục 3.2.1 (Danh sách Use Case), tái cấu trúc yêu cầu chức năng có gắn mã UC (Mục 3.2.2), và bổ sung Mục 3.2.3 (Kịch bản Use Case) cho toàn bộ 28 use case của hệ thống.
