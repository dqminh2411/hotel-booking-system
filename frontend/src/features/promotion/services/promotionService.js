import { promotionApi } from '@/features/promotion/api/promotionApi';

/**
 * Lop service - noi dat logic orchestration/nghiep vu phia FE (neu co).
 * Loi Axios da duoc chuan hoa thanh AppError ngay tai interceptor cua
 * axiosClient (xem shared/api/axiosClient.js), nen o day chi can goi lai
 * promotionApi va de loi (da la AppError) tiep tuc di len hooks/component.
 * Business rule that su van do Backend quyet dinh, FE khong tu suy doan.
 */
export const promotionService = {
  list(params) {
    return promotionApi.getPromotions(params);
  },

  getById(id) {
    return promotionApi.getPromotion(id);
  },

  create(payload) {
    return promotionApi.createPromotion(payload);
  },

  update(id, payload) {
    return promotionApi.updatePromotion(id, payload);
  },

  changeStatus(id, status, reason) {
    return promotionApi.changeStatus(id, { status, reason });
  },

  remove(id) {
    return promotionApi.deletePromotion(id);
  },
};
