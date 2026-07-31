import { adminApi } from '@/features/admin/api/adminApi';

/**
 * Lop service - noi dat logic orchestration/nghiep vu phia FE (neu co).
 * Cac hook goi qua lop nay thay vi goi thang adminApi, giup de thay doi logic
 * (vi du gop nhieu request) ma khong dong cham toi component.
 */
export const adminService = {
  listPendingHotels(params) {
    return adminApi.getPendingHotels(params);
  },

  updateHotelStatus(hotelId, hotelStatus, reason) {
    return adminApi.updateHotelStatus(hotelId, {
      hotelStatus,
      reason: reason ? reason.trim() : undefined,
    });
  },

  deleteHotelImages(hotelId, imgIds) {
    return adminApi.deleteHotelImages({ hotelId, imgIds });
  },
};