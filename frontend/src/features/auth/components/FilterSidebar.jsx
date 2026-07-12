import PriceSlider from "./PriceSlider";
import { AMENITIES, PRICE_MIN, PRICE_MAX, SORT_OPTIONS } from "../utils/constants";
import styles from "./FilterSidebar.module.css";

/**
 * Sidebar bộ lọc: khoảng giá, tiện ích, sắp xếp.
 */
export default function FilterSidebar({
  priceRange,
  onPriceRangeChange,
  selectedAmenities,
  onToggleAmenity,
  sortBy,
  onSortByChange,
}) {
  return (
    <aside className={styles.sidebar}>
      <section className={styles.section}>
        <h3 className={styles.sectionTitle}>Khoảng giá</h3>
        <PriceSlider
          min={PRICE_MIN}
          max={PRICE_MAX}
          value={priceRange}
          onChange={onPriceRangeChange}
        />
      </section>

      <section className={styles.section}>
        <h3 className={styles.sectionTitle}>Tiện ích</h3>
        <div className={styles.checkboxList}>
          {AMENITIES.map((amenity) => (
            <label key={amenity.code} className={styles.checkboxItem}>
              <input
                type="checkbox"
                checked={selectedAmenities.includes(amenity.code)}
                onChange={() => onToggleAmenity(amenity.code)}
              />
              <span>{amenity.label}</span>
            </label>
          ))}
        </div>
      </section>

      <section className={styles.section}>
        <h3 className={styles.sectionTitle}>Sắp xếp</h3>
        <select
          className={styles.sortSelect}
          value={sortBy}
          onChange={(e) => onSortByChange(e.target.value)}
        >
          <option value="">Mặc định</option>
          {SORT_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </section>
    </aside>
  );
}
