import { MOCK_HOTELS } from "../utils/mockHotels";

// import axios from "axios"; // Bỏ comment khi chuyển sang API thật

const MOCK_DELAY_MS = 400;

/**
 * Tìm kiếm khách sạn.
 *
 * Giữ nguyên interface (tên hàm + shape của params/response) để sau này
 * chỉ cần thay phần thân hàm bằng:
 *
 *   const { data } = await axios.get("/api/hotels", { params });
 *   return data;
 *
 * mà KHÔNG cần sửa bất kỳ component / hook nào đang gọi hàm này.
 *
 * @param {Object} params
 * @param {string} params.locationCode  - mã tỉnh/thành (VD: "HN")
 * @param {string} params.checkinDate   - "YYYY-MM-DD"
 * @param {string} params.checkoutDate  - "YYYY-MM-DD"
 * @param {number} params.guestNum      - số khách
 * @param {number} [params.roomNum]     - số phòng
 * @param {number} [params.minPrice]
 * @param {number} [params.maxPrice]
 * @param {string[]} [params.amenities] - danh sách mã tiện ích
 * @param {string} [params.sortBy]      - "priceAsc" | "priceDesc"
 * @param {number} [params.page]
 * @param {number} [params.size]
 * @returns {Promise<{items: Array, total: number, page: number, size: number}>}
 */
export function searchHotels(params = {}) {
  const {
    locationCode,
    minPrice,
    maxPrice,
    amenities = [],
    sortBy,
    page = 1,
    size = 20,
  } = params;

  return new Promise((resolve) => {
    setTimeout(() => {
      let items = [...MOCK_HOTELS];

      // --- Search theo province (locationCode) ---
      // checkinDate / checkoutDate / guestNum / roomNum hiện chưa có dữ liệu
      // tồn phòng để lọc thật, nhưng vẫn nhận trong params để giữ đúng
      // interface cho API thật sau này.
      if (locationCode) {
        items = items.filter((hotel) => hotel.province === locationCode);
      }

      // --- Filter theo khoảng giá ---
      if (typeof minPrice === "number") {
        items = items.filter((hotel) => hotel.price >= minPrice);
      }
      if (typeof maxPrice === "number") {
        items = items.filter((hotel) => hotel.price <= maxPrice);
      }

      // --- Filter theo amenities (khách sạn phải có đủ tất cả tiện ích đã chọn) ---
      if (amenities.length > 0) {
        items = items.filter((hotel) =>
          amenities.every((code) => hotel.amenities.includes(code))
        );
      }

      // --- Sort ---
      if (sortBy === "priceAsc") {
        items.sort((a, b) => a.price - b.price);
      } else if (sortBy === "priceDesc") {
        items.sort((a, b) => b.price - a.price);
      }

      // --- Phân trang (giữ interface cho sau này) ---
      const total = items.length;
      const start = (page - 1) * size;
      const paged = items.slice(start, start + size);

      resolve({ items: paged, total, page, size });
    }, MOCK_DELAY_MS);
  });
}
