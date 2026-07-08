const POLICY_TYPE_LABELS = {
  CANCELATION: 'Chính sách hủy phòng',
  CHECKIN: 'Giờ nhận phòng',
  CHECKOUT: 'Giờ trả phòng',
  SMOKING: 'Quy định hút thuốc',
  PAYMENT: 'Chính sách thanh toán',
  PETS: 'Thú cưng',
  CHILDREN: 'Trẻ em',
};

export function getPolicyTypeLabel(type) {
  return POLICY_TYPE_LABELS[type] || type;
}

const HOTEL_STATUS_LABELS = {
  PENDING: 'Đang chờ duyệt',
  APPROVED: 'Đã xác thực',
  SUSPENDED: 'Tạm ngưng hoạt động',
};

export function getHotelStatusLabel(status) {
  return HOTEL_STATUS_LABELS[status] || status;
}

const HOTEL_STATUS_BADGE_CLASSES = {
  APPROVED: 'border-green-200 bg-green-50 text-green-700',
  PENDING: 'border-amber-200 bg-amber-50 text-amber-700',
  SUSPENDED: 'border-red-200 bg-red-50 text-red-700',
};

export function getHotelStatusBadgeClass(status) {
  return HOTEL_STATUS_BADGE_CLASSES[status] || 'border-slate-200 bg-slate-50 text-slate-600';
}

export function formatAddress(address) {
  if (!address) return '';

  const parts = [
    address.fullAddress,
    address.ward?.name,
    address.district?.name,
    address.province?.name,
  ].filter(Boolean);

  return parts.join(', ');
}
