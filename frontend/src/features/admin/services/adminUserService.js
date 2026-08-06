import { adminUserApi } from '@/features/admin/api/adminUserApi';

/**
 * Lop service - noi dat logic orchestration/nghiep vu phia FE cho User cua Admin.
 * Cac hook goi qua lop nay thay vi goi thang adminUserApi.
 */
export const adminUserService = {
  listUsers(params) {
    return adminUserApi.getUsers(params);
  },

  getUserDetail(userId) {
    return adminUserApi.getUserDetail(userId);
  },

  createUser(payload) {
    return adminUserApi.createUser({
      email: payload.email?.trim(),
      password: payload.password,
      phone: payload.phone?.trim() || undefined,
      fullName: payload.fullName?.trim(),
      address: payload.address?.trim(),
      avatarUrl: payload.avatarUrl?.trim() || undefined,
      role: payload.role || undefined,
    });
  },

  updateUser(userId, payload) {
    return adminUserApi.updateUser(userId, {
      fullName: payload.fullName?.trim() || undefined,
      phone: payload.phone?.trim() || undefined,
      address: payload.address?.trim() || undefined,
      avatarUrl: payload.avatarUrl?.trim() || undefined,
    });
  },

  deleteUser(userId) {
    return adminUserApi.deleteUser(userId);
  },

  lockUser(userId, reason) {
    return adminUserApi.lockUser(userId, { reason: reason?.trim() });
  },

  unlockUser(userId) {
    return adminUserApi.unlockUser(userId);
  },
};