# Hướng dẫn cài đặt HotelHub Keycloak Theme

Theme này chỉ can thiệp vào **login theme** (đăng nhập, đăng ký, xác thực
email, trang thông báo/lỗi). Nó thay thế giao diện Keycloak mặc định bằng
giao diện giống hệt `AuthLayout.jsx` cũ của frontend, gồm 2 trang chính:

- `login.ftl` — form Email/Mật khẩu + nút "Tiếp tục với Google"
- `register-user-profile.ftl` — form Họ và tên / Email / SĐT / Mật khẩu / Xác nhận mật khẩu
- `login-verify-email.ftl` — trang "Kiểm tra hộp thư" hiện ngay sau khi đăng ký
- `info.ftl` / `error.ftl` — trang thông báo chung (ví dụ khi link xác thực được mở ở thiết bị khác)

> Theme dùng **Declarative User Profile** (mặc định bật từ Keycloak ~22 trở
> lên). Nếu bạn dùng bản Keycloak cũ hơn không có User Profile, cần đổi
> `register-user-profile.ftl` thành `register.ftl` và sửa lại field name
> (`user.attributes.fullName` → field riêng theo cơ chế cũ).

## 1. Copy theme vào Keycloak

**Chạy trực tiếp (không Docker):**

```bash
cp -r hotelhub $KEYCLOAK_HOME/themes/hotelhub
```

**Docker (mount volume, dev nhanh):**

```yaml
services:
  keycloak:
    image: quay.io/keycloak/keycloak:26.0
    volumes:
      - ./keycloak/theme/hotelhub:/opt/keycloak/themes/hotelhub
    command: start-dev
```

**Docker (build vào image, dùng cho production):**

```dockerfile
FROM quay.io/keycloak/keycloak:26.0
COPY keycloak-theme/hotelhub /opt/keycloak/themes/hotelhub
RUN /opt/keycloak/bin/kc.sh build
```

Sau khi copy, **restart Keycloak** để nó nhận theme mới (theme cache có thể
cần tắt khi dev: `spi-theme-static-max-age=-1` và `spi-theme-cache-themes=false`
qua `start-dev` đã tự tắt cache).

## 2. Gán theme cho realm

Admin Console → chọn realm → **Realm settings → Themes**:
- Login theme: `hotelhub`
- Save

**Realm settings → Localization:**
- Bật "Internationalization"
- Default locale: `vi`
- Supported locales: thêm `vi`, `en`

## 3. Cấu hình User Profile (bắt buộc — để có field fullName/phone)

Admin Console → **Realm settings → User profile → JSON editor**, dán nội
dung file [`user-profile.json`](./user-profile.json) đính kèm (đã cấu hình
sẵn `username`, `email`, `fullName`, `phone` là bắt buộc; `firstName`/
`lastName` giữ lại nhưng **không bắt buộc** vì form của mình không hiển thị
2 field đó).

## 4. Cấu hình realm login/registration

Admin Console → **Realm settings → Login**:
- User registration: **ON**
- Email as username: **ON**
- Login with email: **ON**
- Verify email: **ON**
- Forgot password: tuỳ chọn (nếu bật, đảm bảo có trang `login-reset-password.ftl`
  — theme này **không** override trang đó nên sẽ dùng theme cha `keycloak`
  mặc định; đủ dùng nhưng khác giao diện. Có thể custom thêm sau).

## 5. Password Policy (khớp với hint hiển thị trên form đăng ký)

Admin Console → **Authentication → Policies → Password policy**, thêm:
- Minimum Length: `8`
- Uppercase Characters: `1`
- Lowercase Characters: `1`
- Digits: `1`

## 6. Cấu hình Google Identity Provider

Admin Console → **Identity providers → Add provider → Google**:
- Client ID / Client Secret: lấy từ Google Cloud Console (OAuth 2.0 Client ID,
  loại **Web application**, Authorized redirect URI = `<KEYCLOAK_URL>/realms/<realm>/broker/google/endpoint`)
- Alias: giữ mặc định `google` (theme dùng `p.alias == 'google'` để hiện icon Google;
  nếu đổi alias khác, nút vẫn hoạt động nhưng sẽ không hiện icon Google, chỉ hiện text)
- Store tokens: tuỳ nhu cầu (không bắt buộc cho luồng hiện tại)

Với **First Login Flow** mặc định của IDP Google, tài khoản sẽ được tạo tự
động khi đăng nhập Google lần đầu (email tự động verified vì Google đã xác
thực). fullName/phone của user Google sẽ không có `phone`, và `fullName`
sẽ được frontend fallback sang claim `name` chuẩn OIDC (given_name +
family_name) — xem phần 8 bên dưới.

## 7. Protocol Mappers — đưa fullName/phone vào token

Vì `fullName` và `phone` là **custom attribute** (không phải claim chuẩn
OIDC), cần thêm 2 protocol mapper để chúng xuất hiện trong ID Token/Access
Token/UserInfo — frontend đọc các claim này để hiển thị và để gọi
`POST /api/users`.

Admin Console → **Clients → frontend-client → Client scopes →
frontend-client-dedicated → Add mapper → By configuration → User Attribute**:

| Mapper | Name | User Attribute | Token Claim Name | Claim JSON Type | Add to ID token | Add to access token | Add to userinfo |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | fullName | `fullName` | `fullName` | String | ✅ | ✅ | ✅ |
| 2 | phone | `phone` | `phone` | String | ✅ | ✅ | ✅ |

(Có thể tạo ở **Client scopes → email** hoặc tạo client scope riêng
`profile-extra` rồi gán vào `frontend-client` nếu muốn tái sử dụng cho
client khác.)

## 8. Client `frontend-client`

Đảm bảo cấu hình như đã thống nhất trước đó:
- Client authentication: **OFF** (public client)
- Standard flow: **ON**
- Direct access grants: tắt (không cần vì không dùng Resource Owner Password flow)
- Advanced → Proof Key for Code Exchange Code Challenge Method: **S256**
- Valid redirect URIs: `http://localhost:5173/*` (và domain production tương ứng)
- Valid post logout redirect URIs: `http://localhost:5173/*`
- Web origins: `http://localhost:5173` (hoặc `+`)

## 9. Test nhanh luồng

1. Vào frontend → `/register` → bấm "Đăng ký với email" → được chuyển sang
   `register-user-profile.ftl` (theme mới) → điền đủ form → submit.
2. Keycloak tạo user (chưa verified) → hiện trang `login-verify-email.ftl`
   ("Kiểm tra hộp thư của bạn").
3. Mở email, bấm link xác thực **trên cùng trình duyệt/tab đã đăng ký** →
   Keycloak verify xong → tự động hoàn tất login → redirect về
   `http://localhost:5173/verify-email` với auth code → frontend exchange
   token → `VerifyEmailPage` hiện "Xác thực email thành công".
4. `useSyncUserProfile` (chạy ngầm toàn app) gọi `POST /api/users` với
   `{ keycloakId, fullName, email, phone }` để tạo record trong User DB.

> **Lưu ý quan trọng:** nếu người dùng mở link xác thực trên **thiết bị/
> trình duyệt khác** với nơi đã đăng ký, Keycloak sẽ không thể "resume"
> phiên cũ để redirect thẳng về frontend — nó chỉ hiển thị trang `info.ftl`
> của chính Keycloak (đã custom giao diện tương tự) báo xác thực thành
> công, và người dùng cần tự quay lại app rồi đăng nhập bình thường. Khi họ
> đăng nhập, `useSyncUserProfile` vẫn sẽ tự tạo record ở `/api/users` như
> bình thường — nên hệ thống vẫn đúng dữ liệu dù rơi vào trường hợp này.

## 10. Backend cần có

- `POST /api/users` — nhận `{ keycloakId, fullName, email, phone }`, nên
  **idempotent theo `keycloakId`** (upsert / bỏ qua nếu đã tồn tại) vì
  frontend có thể gọi lại (mỗi phiên trình duyệt gọi 1 lần).
