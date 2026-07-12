import FilterSidebar from "./FilterSidebar";
import HotelCard from "./HotelCard";
import styles from "./SearchResult.module.css";

/**
 * Bố cục kết quả tìm kiếm: Filter Sidebar (trái) + danh sách khách sạn (phải).
 */
export default function SearchResult({
  hotels,
  loading,
  error,
  totalRaw,
  priceRange,
  onPriceRangeChange,
  selectedAmenities,
  onToggleAmenity,
  sortBy,
  onSortByChange,
  onViewDetail,
}) {
  return (
    <div className={styles.resultLayout}>
      <FilterSidebar
        priceRange={priceRange}
        onPriceRangeChange={onPriceRangeChange}
        selectedAmenities={selectedAmenities}
        onToggleAmenity={onToggleAmenity}
        sortBy={sortBy}
        onSortByChange={onSortByChange}
      />

      <div className={styles.list}>
        <p className={styles.resultCount}>
          {loading
            ? "Đang tìm khách sạn..."
            : `Tìm thấy ${hotels.length}/${totalRaw} khách sạn`}
        </p>

        {error && <p className={styles.error}>{error}</p>}

        {!loading && !error && hotels.length === 0 && (
          <p className={styles.empty}>
            Không có khách sạn nào phù hợp với bộ lọc hiện tại.
          </p>
        )}

        {!loading &&
          hotels.map((hotel) => (
            <HotelCard key={hotel.id} hotel={hotel} onViewDetail={onViewDetail} />
          ))}
      </div>
    </div>
  );
}
