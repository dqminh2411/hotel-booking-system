# Git Convention

## 1. Chiến lược Branch

| Branch       | Mục đích                           |
| ------------ | ---------------------------------- |
| `main`       | Phiên bản ổn định, dùng để release |
| `develop`    | Nhánh tích hợp chung của nhóm      |
| `feature/*`  | Phát triển chức năng mới           |
| `fix/*`      | Sửa lỗi                            |
| `refactor/*` | Cải thiện mã nguồn                 |
| `docs/*`     | Cập nhật tài liệu                  |
| `hotfix/*`   | Sửa lỗi khẩn cấp trên production   |

### Quy tắc đặt tên Branch

```text
<type>/<service>-<mo-ta-ngan>
```

Ví dụ:

```text
feature/booking-service-create-booking
feature/payment-service-vnpay
fix/hotel-service-room-inventory
refactor/place-booking-service-saga
docs/api-documentation
```

---

## 2. Quy tắc Commit

Dự án sử dụng chuẩn **Conventional Commits**.

Cấu trúc:

```text
<type>(<scope>): <description>
```

### Các loại commit

* `feat` : Thêm chức năng mới
* `fix` : Sửa lỗi
* `refactor` : Cải thiện code, không thay đổi nghiệp vụ
* `docs` : Cập nhật tài liệu
* `test` : Thêm hoặc sửa test
* `chore` : Cấu hình, dependency, build...
* `perf` : Tối ưu hiệu năng
* `style` : Chỉ thay đổi định dạng code
* `ci` : Cập nhật CI/CD

### Scope khuyến nghị

```text
booking-service
hotel-service
user-service
payment-service
place-booking-service
notification-service
common
database
docker
kafka
redis
```

Ví dụ:

```text
feat(booking-service): tạo chức năng đặt phòng
fix(hotel-service): sửa lỗi kiểm tra phòng trống
refactor(payment-service): đơn giản hóa luồng thanh toán
docs(common): cập nhật tài liệu API
```

---

## 3. Quy trình làm việc

```bash
git checkout develop
git pull origin develop

git checkout -b feature/booking-service-create-booking

# Coding

git add .
git commit -m "feat(booking-service): tạo chức năng đặt phòng"

git push origin feature/booking-service-create-booking
```

Sau khi hoàn thành, tạo **Pull Request** vào nhánh `develop`.

---

## 4. Quy tắc Pull Request

Mỗi Pull Request cần:

* Tiêu đề rõ ràng.
* Chỉ giải quyết **một chức năng hoặc một lỗi**.
* Build và test thành công.
* Được ít nhất **01 thành viên review** trước khi merge.
* Ghi rõ service bị ảnh hưởng nếu thay đổi nhiều service.

Ví dụ tiêu đề:

```text
feat(booking-service): bổ sung luồng tạo booking
```

---

## 5. Quy tắc Merge

* Không push trực tiếp lên `main`.
* Không push trực tiếp lên `develop`.
* Chỉ merge thông qua Pull Request.
* Giải quyết conflict trước khi merge.
* Xóa branch sau khi merge nếu không còn sử dụng.

---

## 6. Quy tắc khi làm việc với Microservices

Nếu thay đổi:

* API → cập nhật tài liệu API.
* Database → cập nhật migration (nếu có).
* Kafka Event → cập nhật cả Producer và Consumer.
* DTO dùng chung → cập nhật tất cả service liên quan.

Trong Pull Request cần ghi rõ các service bị ảnh hưởng.

---

## 7. Trước khi Push Code

Kiểm tra:

* Build thành công.
* Test thành công.
* Không còn log/debug tạm thời.
* Không commit file chứa thông tin nhạy cảm.
* Commit message đúng convention.

---

## 8. Không được Commit

```text
target/
.idea/
.vscode/
*.log
.env
application-secret.yml
service-account.json
```

---

## 9. Release

Quy trình release:

```text
develop → main
```

Sau khi merge tạo tag:

```text
v1.0.0
v1.1.0
v1.1.1
```

---

## 10. Tóm tắt

### Branch

```text
feature/booking-service-create-booking
fix/payment-service-payment-timeout
docs/api-gateway
```

### Commit

```text
feat(booking-service): tạo chức năng đặt phòng
fix(hotel-service): sửa lỗi kiểm tra phòng trống
docs(common): cập nhật tài liệu API
```

### Nguyên tắc chung

* Một branch chỉ thực hiện một công việc.
* Commit nhỏ, rõ ràng, đúng mục đích.
* Luôn cập nhật `develop` trước khi bắt đầu làm.
* Luôn sử dụng Pull Request để merge.
* Giữ lịch sử Git sạch, dễ đọc và dễ truy vết.
