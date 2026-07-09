export function getApiErrorMessage(error, fallbackMessage) {
  if (!error.response) {
    return 'Không thể kết nối tới máy chủ. Vui lòng kiểm tra kết nối và thử lại.';
  }

  const data = error.response.data;
  if (typeof data?.message === 'string' && data.message.trim()) {
    return data.message;
  }

  if (typeof data?.error === 'string' && data.error.trim()) {
    return data.error;
  }

  return fallbackMessage;
}
