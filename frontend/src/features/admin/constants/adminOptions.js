import { getHotelStatusLabel } from '@/features/hotel/utils/hotelFormatters';

/**
 * AdminServiceImpl#updateHotelStatus chi cho phep chuyen hotel PENDING sang 1 trong
 * 2 trang thai nay (xem: `if (!List.of(HotelStatus.APPROVED, HotelStatus.SUSPENDED)...`),
 * nen UI cung chi cung cap dung 2 hanh dong nay - khong cho chon lai PENDING.
 */
export const HOTEL_UPDATABLE_STATUS = {
  APPROVED: 'APPROVED',
  SUSPENDED: 'SUSPENDED',
};

// Mau badge dung chung voi shared/components/StatusBadge, khop voi
// HOTEL_STATUS_LABELS da co san trong features/hotel/utils/hotelFormatters.js.
export const HOTEL_STATUS_TONE = {
  PENDING: 'warning',
  APPROVED: 'success',
  SUSPENDED: 'danger',
};

export { getHotelStatusLabel };