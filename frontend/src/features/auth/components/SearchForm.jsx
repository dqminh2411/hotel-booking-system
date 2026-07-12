import { useState } from "react";
import GuestSelector from "./GuestSelector";
import { PROVINCES } from "../utils/constants";
import styles from "./SearchForm.module.css";

const todayStr = () => new Date().toISOString().split("T")[0];

/**
 * Form tìm kiếm khách sạn: tỉnh/thành, ngày check-in/out, số khách.
 * @param {(params: {locationCode: string, checkinDate: string, checkoutDate: string, guestNum: number}) => void} onSearch
 */
export default function SearchForm({ onSearch, loading }) {
  const [locationCode, setLocationCode] = useState(PROVINCES[0].code);
  const [checkinDate, setCheckinDate] = useState(todayStr());
  const [checkoutDate, setCheckoutDate] = useState(todayStr());
  const [guestNum, setGuestNum] = useState(1);

  const handleCheckinChange = (e) => {
    const nextCheckin = e.target.value;
    setCheckinDate(nextCheckin);

    // Nếu check-in mới lớn hơn check-out hiện tại -> tự reset check-out = check-in
    if (nextCheckin > checkoutDate) {
      setCheckoutDate(nextCheckin);
    }
  };

  const handleCheckoutChange = (e) => {
    const nextCheckout = e.target.value;
    // Không cho phép chọn check-out nhỏ hơn check-in (phòng vệ thêm ngoài thuộc tính `min`)
    if (nextCheckout < checkinDate) {
      setCheckoutDate(checkinDate);
      return;
    }
    setCheckoutDate(nextCheckout);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    onSearch({ locationCode, checkinDate, checkoutDate, guestNum });
  };

  return (
    <form className={styles.searchBox} onSubmit={handleSubmit}>
      <div className={styles.field}>
        <label className={styles.label} htmlFor="province">
          Tỉnh / Thành phố
        </label>
        <select
          id="province"
          className={styles.select}
          value={locationCode}
          onChange={(e) => setLocationCode(e.target.value)}
        >
          {PROVINCES.map((province) => (
            <option key={province.code} value={province.code}>
              {province.name}
            </option>
          ))}
        </select>
      </div>

      <div className={styles.field}>
        <label className={styles.label} htmlFor="checkin">
          Nhận phòng
        </label>
        <input
          id="checkin"
          type="date"
          className={styles.dateInput}
          value={checkinDate}
          min={todayStr()}
          onChange={handleCheckinChange}
        />
      </div>

      <div className={styles.field}>
        <label className={styles.label} htmlFor="checkout">
          Trả phòng
        </label>
        <input
          id="checkout"
          type="date"
          className={styles.dateInput}
          value={checkoutDate}
          min={checkinDate}
          onChange={handleCheckoutChange}
        />
      </div>

      <div className={styles.field}>
        <span className={styles.label}>Số khách</span>
        <GuestSelector value={guestNum} onChange={setGuestNum} />
      </div>

      <button type="submit" className={styles.searchButton} disabled={loading}>
        {loading ? "Đang tìm..." : "Tìm khách sạn"}
      </button>
    </form>
  );
}
