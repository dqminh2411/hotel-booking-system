import axiosClient from '@/shared/api/axiosClient';

/**
 * Lop goi API "tho" cho Admin - Hotel management (hotel-service).
 * CHI goi dung 3 endpoint duoc cong bo trong AdminController:
 *  - GET    /api/admin/hotels/pending
 *  - PATCH  /api/admin/hotels/{hotelId}/status
 *  - DELETE /api/admin/hotels/images
 * Component KHONG duoc goi axios truc tiep, phai qua lop nay.
 *
 * Ca 3 endpoint deu @PreAuthorize("hasRole('PLATFORM_ADMIN')") - backend se tra 403
 * neu user dang nhap khong co role nay, axiosClient interceptor van xu ly refresh token
 * nhu binh thuong, loi 403 se duoc getApiErrorMessage doc tu response.data.message.
 */
const BASE_PATH = '/api/admin/hotels';

export const adminApi = {
  // GET /api/admin/hotels/pending?page=&size=
  // AdminController rang buoc: page >= 0 (@Min(0)), 10 <= size <= 30 (@Min(10) @Max(30)).
  // Response la ApiResponse<Page<HotelPendingResponse>> nen phai lay .data.data.
  getPendingHotels({ page = 0, size = 10 } = {}) {
    return axiosClient
      .get(`${BASE_PATH}/pending`, { params: { page, size } })
      .then((res) => res.data.data);
  },

  // PATCH /api/admin/hotels/{hotelId}/status
  // Body dung theo HotelUpdateStatusRequest: { hotelStatus, reason }
  updateHotelStatus(hotelId, payload) {
    return axiosClient.patch(`${BASE_PATH}/${hotelId}/status`, payload).then((res) => res.data);
  },

  // DELETE /api/admin/hotels/images
  // Body dung theo HotelImageDelRequest: { hotelId, imgIds }
  // Axios can khai bao rieng { data: payload } vi DELETE mac dinh khong gui body.
  deleteHotelImages(payload) {
    return axiosClient.delete(`${BASE_PATH}/images`, { data: payload }).then((res) => res.data);
  },
};