import { useCallback, useMemo, useState } from "react";
import { searchHotels } from "../services/hotelService";
import { PRICE_MIN, PRICE_MAX } from "../utils/constants";

const DEFAULT_PRICE_RANGE = { min: PRICE_MIN, max: PRICE_MAX };

/**
 * Hook quản lý toàn bộ logic của trang Hotel Search:
 * - Gọi hotelService.searchHotels() theo province/checkin/checkout/guest.
 * - Filter kết quả theo khoảng giá + amenities (thực hiện ở frontend).
 * - Sort theo giá.
 */
export function useHotelSearch() {
  // Kết quả "thô" trả về từ service (mới search theo province/checkin/checkout/guest)
  const [rawResults, setRawResults] = useState([]);
  const [lastSearchParams, setLastSearchParams] = useState(null);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [hasSearched, setHasSearched] = useState(false);

  // State cho FilterSidebar
  const [priceRange, setPriceRange] = useState(DEFAULT_PRICE_RANGE);
  const [selectedAmenities, setSelectedAmenities] = useState([]);
  const [sortBy, setSortBy] = useState("");

  /**
   * Được gọi khi bấm nút "Tìm khách sạn" ở SearchForm.
   * @param {{ locationCode: string, checkinDate: string, checkoutDate: string, guestNum: number }} formParams
   */
  const search = useCallback(async (formParams) => {
    setLoading(true);
    setError(null);
    setHasSearched(true);
    // Reset filter/sort mỗi lần search mới
    setPriceRange(DEFAULT_PRICE_RANGE);
    setSelectedAmenities([]);
    setSortBy("");

    try {
      const response = await searchHotels({
        locationCode: formParams.locationCode,
        checkinDate: formParams.checkinDate,
        checkoutDate: formParams.checkoutDate,
        guestNum: formParams.guestNum,
        roomNum: 1,
        page: 1,
        size: 20,
      });
      setRawResults(response.items);
      setLastSearchParams(formParams);
    } catch (err) {
      setError("Không thể tìm kiếm khách sạn. Vui lòng thử lại.");
      setRawResults([]);
    } finally {
      setLoading(false);
    }
  }, []);

  const toggleAmenity = useCallback((code) => {
    setSelectedAmenities((prev) =>
      prev.includes(code) ? prev.filter((c) => c !== code) : [...prev, code]
    );
  }, []);

  // Filter + sort hoàn toàn ở frontend, dựa trên rawResults đã search từ service
  const results = useMemo(() => {
    let items = [...rawResults];

    items = items.filter(
      (hotel) => hotel.price >= priceRange.min && hotel.price <= priceRange.max
    );

    if (selectedAmenities.length > 0) {
      items = items.filter((hotel) =>
        selectedAmenities.every((code) => hotel.amenities.includes(code))
      );
    }

    if (sortBy === "priceAsc") {
      items.sort((a, b) => a.price - b.price);
    } else if (sortBy === "priceDesc") {
      items.sort((a, b) => b.price - a.price);
    }

    return items;
  }, [rawResults, priceRange, selectedAmenities, sortBy]);

  return {
    // search
    search,
    lastSearchParams,
    loading,
    error,
    hasSearched,
    // results
    results,
    totalRaw: rawResults.length,
    // filters
    priceRange,
    setPriceRange,
    selectedAmenities,
    toggleAmenity,
    sortBy,
    setSortBy,
  };
}
