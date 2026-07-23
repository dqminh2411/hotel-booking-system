import axiosClient from '../../../shared/api/axiosClient';

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
