export function formatCurrency(amount) {
  const value = Number(amount);
  if (Number.isNaN(value)) return '—';

  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(value);
}

export function formatDateVi(dateInput) {
  if (!dateInput) return '';

  const date = dateInput instanceof Date ? dateInput : new Date(dateInput);
  if (Number.isNaN(date.getTime())) return '';

  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(date);
}

export function countNights(checkinDate, checkoutDate) {
  if (!checkinDate || !checkoutDate) return 0;

  const checkin = new Date(checkinDate);
  const checkout = new Date(checkoutDate);
  const diffMs = checkout.getTime() - checkin.getTime();

  if (Number.isNaN(diffMs) || diffMs <= 0) return 0;

  return Math.round(diffMs / (1000 * 60 * 60 * 24));
}
