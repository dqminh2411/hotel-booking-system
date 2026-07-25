export function formatNumber(value) {
  if (value === null || value === undefined) return '-';
  return new Intl.NumberFormat('vi-VN').format(value);
}

export function formatCurrencyLike(value) {
  if (value === null || value === undefined) return '-';
  return new Intl.NumberFormat('vi-VN').format(value);
}

/** Bo cac field undefined/null truoc khi gui request, giu nguyen 0/false. */
export function stripEmpty(obj) {
  const result = { ...obj };
  Object.keys(result).forEach((key) => {
    if (result[key] === undefined) {
      delete result[key];
    }
  });
  return result;
}
