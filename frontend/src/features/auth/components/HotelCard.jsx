import { AMENITIES } from "../utils/constants";
import styles from "./HotelCard.module.css";

const formatCurrency = (price) =>
  price.toLocaleString("vi-VN", { style: "currency", currency: "VND" });

const amenityLabel = (code) => AMENITIES.find((a) => a.code === code)?.label ?? code;

/**
 * Card hiển thị thông tin 1 khách sạn trong danh sách kết quả.
 */
export default function HotelCard({ hotel, onViewDetail }) {
  const visibleAmenities = hotel.amenities.slice(0, 4);

  return (
    <article className={styles.card}>
      <img className={styles.image} src={hotel.image} alt={hotel.name} loading="lazy" />

      <div className={styles.info}>
        <div>
          <h3 className={styles.name}>{hotel.name}</h3>
          <p className={styles.address}>{hotel.address}</p>

          <div className={styles.rating}>⭐ {hotel.rating.toFixed(1)}</div>

          <ul className={styles.amenities}>
            {visibleAmenities.map((code) => (
              <li key={code} className={styles.amenityTag}>
                {amenityLabel(code)}
              </li>
            ))}
          </ul>
        </div>

        <div className={styles.footer}>
          <div className={styles.priceBlock}>
            <span className={styles.priceLabel}>Giá thấp nhất / đêm</span>
            <span className={styles.price}>{formatCurrency(hotel.price)}</span>
          </div>
          <button
            type="button"
            className={styles.detailButton}
            onClick={() => onViewDetail?.(hotel.id)}
          >
            Xem chi tiết
          </button>
        </div>
      </div>
    </article>
  );
}
