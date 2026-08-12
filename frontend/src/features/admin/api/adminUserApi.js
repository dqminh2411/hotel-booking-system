import axiosClient from '@/shared/api/axiosClient';

/**
 * Lop goi API "tho" cho Admin - User management (user-service, AdminController).
 * CHI goi dung cac endpoint duoc cong bo o phan "/*==== User ====*\/" cua AdminController:
 *  - GET    /api/admin/users
 *  - GET    /api/admin/users/{userId}
 *  - POST   /api/admin/users
 *  - PATCH  /api/admin/users/{userId}
 *  - DELETE /api/admin/users/{userId}
 *  - PATCH  /api/admin/users/{userId}/lock
 *  - PATCH  /api/admin/users/{userId}/unlock
 * Component KHONG duoc goi axios truc tiep, phai qua lop nay.
 *
 * Tat ca deu @PreAuthorize("hasRole('PLATFORM_ADMIN')") - 403 se duoc
 * getApiErrorMessage doc tu response.data.message nhu cac feature admin khac.
 */
const BASE_PATH = '/api/admin/users';

export const adminUserApi = {
  // GET /api/admin/users?page=&size=&status=&search=
  // AdminController rang buoc: page >= 0 (@Min(0)), 10 <= size <= 30 (@Min(10) @Max(30)).
  // Response la ApiResponse<Page<ResponseUser>> nen phai lay .data.data.
  getUsers({ page = 0, size = 10, status, search } = {}) {
    return axiosClient
      .get(BASE_PATH, {
        params: { page, size, status: status || undefined, search: search || undefined },
      })
      .then((res) => res.data.data);
  },

  // GET /api/admin/users/{userId}
  // Response la ApiResponse<UserResponse> - DAY DU HON ResponseUser (co them id, name,
  // address, roles[], updatedAt). Dung de nap du lieu cho form Sua thong tin.
  getUserDetail(userId) {
    return axiosClient.get(`${BASE_PATH}/${userId}`).then((res) => res.data.data);
  },

  // POST /api/admin/users - body dung theo CreateUserAdminRequest.
  createUser(payload) {
    return axiosClient.post(BASE_PATH, payload).then((res) => res.data);
  },

  // PATCH /api/admin/users/{userId} - body dung theo UpdateUserRequest
  // (CHI co fullName/phone/address/avatarUrl - khong the doi email/mat khau/vai tro
  // qua endpoint nay, backend khong nhan cac field do).
  updateUser(userId, payload) {
    return axiosClient.patch(`${BASE_PATH}/${userId}`, payload).then((res) => res.data);
  },

  // DELETE /api/admin/users/{userId} - xoa mem tai khoan nguoi dung.
  deleteUser(userId) {
    return axiosClient.delete(`${BASE_PATH}/${userId}`).then((res) => res.data);
  },

  // PATCH /api/admin/users/{userId}/lock - body dung theo LockRequest { reason }.
  // reason la BAT BUOC (LockRequest co @NotBlank), khac voi hotel (chi bat buoc khi tu choi).
  lockUser(userId, payload) {
    return axiosClient.patch(`${BASE_PATH}/${userId}/lock`, payload).then((res) => res.data);
  },

  // PATCH /api/admin/users/{userId}/unlock - khong nhan body.
  unlockUser(userId) {
    return axiosClient.patch(`${BASE_PATH}/${userId}/unlock`).then((res) => res.data);
  },
};