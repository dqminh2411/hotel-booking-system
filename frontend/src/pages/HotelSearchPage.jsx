import SearchForm from "../features/auth/components/SearchForm";
import SearchResult from "../features/auth/components/SearchResult";
import { useHotelSearch } from "../features/auth/hooks/useHotelSearch";
import styles from "./HotelSearchPage.module.css";

export default function HotelSearchPage() {
  const {
    search,
    loading,
    error,
    hasSearched,
    results,
    totalRaw,
    priceRange,
    setPriceRange,
    selectedAmenities,
    toggleAmenity,
    sortBy,
    setSortBy,
  } = useHotelSearch();

  const handleViewDetail = (hotelId) => {
    // TODO: điều hướng sang trang chi tiết khách sạn khi có route thật
    console.log("Xem chi tiết khách sạn:", hotelId);
  };

  return (
    <div className={styles.page}>
      <div className={styles.hero}>
        <div className={styles.container}>
          <h1 className={styles.title}>Tìm khách sạn phù hợp với bạn</h1>
          <SearchForm onSearch={search} loading={loading} />
        </div>
      </div>

      {hasSearched && (
        <div className={styles.container}>
          <SearchResult
            hotels={results}
            loading={loading}
            error={error}
            totalRaw={totalRaw}
            priceRange={priceRange}
            onPriceRangeChange={setPriceRange}
            selectedAmenities={selectedAmenities}
            onToggleAmenity={toggleAmenity}
            sortBy={sortBy}
            onSortByChange={setSortBy}
            onViewDetail={handleViewDetail}
          />
        </div>
      )}
    </div>
  );
}
