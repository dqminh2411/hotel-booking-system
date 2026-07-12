import styles from "./PriceSlider.module.css";

/**
 * Thanh kéo khoảng giá (min - max) dùng 2 input[type=range] chồng lên nhau.
 *
 * @param {number} min - giá trị nhỏ nhất có thể chọn
 * @param {number} max - giá trị lớn nhất có thể chọn
 * @param {number} step
 * @param {{min: number, max: number}} value - giá trị đang chọn
 * @param {(value: {min: number, max: number}) => void} onChange
 */
export default function PriceSlider({ min, max, step = 50000, value, onChange }) {
  const handleMinChange = (e) => {
    const nextMin = Math.min(Number(e.target.value), value.max - step);
    onChange({ ...value, min: Math.max(min, nextMin) });
  };

  const handleMaxChange = (e) => {
    const nextMax = Math.max(Number(e.target.value), value.min + step);
    onChange({ ...value, max: Math.min(max, nextMax) });
  };

  const formatPrice = (price) => {
    if (price >= 1000000) {
      const val = price / 1000000;
      return `${val % 1 === 0 ? val : val.toFixed(1)} triệu`;
    }
    if (price >= 1000) {
      return `${Math.round(price / 1000)}k`;
    }
    return `${price}đ`;
  };

  // % vị trí của 2 handle để tô màu đoạn giữa (track đã chọn)
  const minPercent = ((value.min - min) / (max - min)) * 100;
  const maxPercent = ((value.max - min) / (max - min)) * 100;

  return (
    <div className={styles.wrapper}>
      <div className={styles.track}>
        <div
          className={styles.trackRange}
          style={{ left: `${minPercent}%`, right: `${100 - maxPercent}%` }}
        />
        <input
          type="range"
          className={styles.rangeInput}
          min={min}
          max={max}
          step={step}
          value={value.min}
          onChange={handleMinChange}
          aria-label="Giá tối thiểu"
        />
        <input
          type="range"
          className={styles.rangeInput}
          min={min}
          max={max}
          step={step}
          value={value.max}
          onChange={handleMaxChange}
          aria-label="Giá tối đa"
        />
      </div>

      <div className={styles.labels}>
        <span>{formatPrice(value.min)}</span>
        <span>{formatPrice(value.max)}{value.max >= max ? "+" : ""}</span>
      </div>
    </div>
  );
}
