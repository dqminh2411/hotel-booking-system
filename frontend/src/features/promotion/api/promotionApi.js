import axiosClient from '@/shared/api/axiosClient';

/**
 * Lop goi API "tho" cho Promotion - CHI goi dung 6 endpoint backend da cong bo.
 * Khong tao API moi, khong doi request/response so voi backend.
 * Component KHONG duoc goi axios truc tiep, phai qua lop nay.
 * @see @/features/promotion/types/promotion.types.js - shape cua params/payload/response
 */
const BASE_PATH = '/api/promotions';

export const promotionApi = {
  // GET /api/promotions
  getPromotions(params) {
    return axiosClient
      .get(BASE_PATH, {
        params: {
          keyword: params.keyword || undefined,
          status: params.status || undefined,
          page: params.page ?? 0,
          size: params.size ?? 20,
        },
      })
      .then((res) => res.data);
  },

  // GET /api/promotions/{id}
  getPromotion(id) {
    return axiosClient.get(`${BASE_PATH}/${id}`).then((res) => res.data);
  },

  // POST /api/promotions
  createPromotion(payload) {
    return axiosClient.post(BASE_PATH, payload).then((res) => res.data);
  },

  // PUT /api/promotions/{id}
  updatePromotion(id, payload) {
    return axiosClient.put(`${BASE_PATH}/${id}`, payload).then((res) => res.data);
  },

  // PATCH /api/promotions/{id}/status
  // Controller thuc te doc `status` tu query param (@RequestParam), khong doc request body.
  // OpenAPI lai mo ta request body ChangePromotionStatusRequest{status, reason}.
  // De vua chay dung voi backend hien tai, vua khong lech contract OpenAPI, FE gui:
  //  - status: query param (bat buoc, backend can de hoat dong)
  //  - body: { status, reason } (dung format OpenAPI, backend hien tai se bo qua vi
  //    controller khong khai bao @RequestBody, nhung san sang khi backend cap nhat theo spec)
  changeStatus(id, payload) {
    return axiosClient
      .patch(`${BASE_PATH}/${id}/status`, payload, {
        params: { status: payload.status },
      })
      .then((res) => res.data);
  },

  // DELETE /api/promotions/{id}
  deletePromotion(id) {
    return axiosClient.delete(`${BASE_PATH}/${id}`).then((res) => res.data);
  },
};

/**
 * Lấy thông tin coupon để hiển thị (KHÔNG validate/reserve).
 * @param {string} code - mã coupon
 * @param {number} totalAmount - tổng tiền hiện tại để tính discountAmount
 * @param {string} hotelId - UUID khách sạn
 */
export async function fetchCouponDetail(code, totalAmount, hotelId) {
  const { data } = await axiosClient.get(`/api/promotions/coupons/${code}`, {
    params: { totalAmount, hotelId },
  });
  return data;
}
