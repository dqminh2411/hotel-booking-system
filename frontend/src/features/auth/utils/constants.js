// Danh sách tỉnh/thành hard-code tạm thời.
// Sau này sẽ thay bằng API (VD: GET /api/locations).
export const PROVINCES = [
  { code: "HN", name: "Hà Nội" },
  { code: "HCM", name: "Hồ Chí Minh" },
  { code: "DN", name: "Đà Nẵng" },
  { code: "HP", name: "Hải Phòng" },
  { code: "HUE", name: "Huế" },
  { code: "NT", name: "Nha Trang" },
  { code: "DL", name: "Đà Lạt" },
  { code: "QN", name: "Quảng Ninh" },
  { code: "VT", name: "Vũng Tàu" },
  { code: "CT", name: "Cần Thơ" },
];

// Danh sách tiện ích dùng cho FilterSidebar và HotelCard.
export const AMENITIES = [
  { code: "wifi", label: "Wifi" },
  { code: "pool", label: "Hồ bơi" },
  { code: "parking", label: "Bãi đỗ xe" },
  { code: "aircon", label: "Điều hòa" },
  { code: "breakfast", label: "Buffet sáng" },
  { code: "gym", label: "Gym" },
  { code: "spa", label: "Spa" },
];

// Mốc giá tham khảo hiển thị trên PriceSlider (đơn vị: VND)
export const PRICE_MIN = 0;
export const PRICE_MAX = 5000000;

export const SORT_OPTIONS = [
  { value: "priceAsc", label: "Giá tăng dần" },
  { value: "priceDesc", label: "Giá giảm dần" },
];
