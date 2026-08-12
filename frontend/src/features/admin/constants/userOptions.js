/**
 * Hang so hien thi cho domain User (Admin quan ly).
 * Khop voi entity UserStatus (ACTIVE|LOCKED) va enum UserRole trong user-service
 * (dto/UserRole.java - chi dung khi Admin TAO user moi, xem CreateUserAdminRequest).
 */
export const USER_STATUS_OPTIONS = [
  { value: 'ACTIVE', label: 'Đang hoạt động' },
  { value: 'LOCKED', label: 'Đã khóa' },
];

const USER_STATUS_LABELS = {
  ACTIVE: 'Đang hoạt động',
  LOCKED: 'Đã khóa',
};

// Mau badge dung chung voi shared/components/StatusBadge (tone: success|warning|danger|info|neutral)
export const USER_STATUS_TONE = {
  ACTIVE: 'success',
  LOCKED: 'danger',
};

export function getUserStatusLabel(status) {
  return USER_STATUS_LABELS[status] ?? status ?? '-';
}

// UserRole (BE): CUSTOMER | PLATFORM_ADMIN | HOTEL_OWNER | HOTEL_STAFF
export const USER_ROLE_OPTIONS = [
  { value: 'CUSTOMER', label: 'Khách hàng' },
  { value: 'PLATFORM_ADMIN', label: 'Quản trị hệ thống' },
  { value: 'HOTEL_OWNER', label: 'Chủ khách sạn' },
  { value: 'HOTEL_STAFF', label: 'Nhân viên khách sạn' },
];

const USER_ROLE_LABELS = {
  CUSTOMER: 'Khách hàng',
  PLATFORM_ADMIN: 'Quản trị hệ thống',
  HOTEL_OWNER: 'Chủ khách sạn',
  HOTEL_STAFF: 'Nhân viên khách sạn',
};

export function getUserRoleLabel(role) {
  return USER_ROLE_LABELS[role] ?? role;
}