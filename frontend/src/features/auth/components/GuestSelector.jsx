import { useState } from "react";
import styles from "./GuestSelector.module.css";

const MIN_GUEST = 1;

/**
 * Bộ chọn số khách dạng [-] số [+], cho phép click vào số để nhập trực tiếp.
 */
export default function GuestSelector({ value, onChange }) {
  const [isEditing, setIsEditing] = useState(false);
  const [draftValue, setDraftValue] = useState(String(value));

  const clamp = (num) => (Number.isFinite(num) && num >= MIN_GUEST ? num : MIN_GUEST);

  const handleDecrease = () => {
    onChange(clamp(value - 1));
  };

  const handleIncrease = () => {
    onChange(clamp(value + 1));
  };

  const startEditing = () => {
    setDraftValue(String(value));
    setIsEditing(true);
  };

  const commitDraft = () => {
    const parsed = parseInt(draftValue, 10);
    onChange(clamp(parsed));
    setIsEditing(false);
  };

  return (
    <div className={styles.wrapper}>
      <button
        type="button"
        className={styles.stepButton}
        onClick={handleDecrease}
        disabled={value <= MIN_GUEST}
        aria-label="Giảm số khách"
      >
        −
      </button>

      {isEditing ? (
        <input
          type="number"
          className={styles.input}
          value={draftValue}
          autoFocus
          min={MIN_GUEST}
          onChange={(e) => setDraftValue(e.target.value)}
          onBlur={commitDraft}
          onKeyDown={(e) => {
            if (e.key === "Enter") commitDraft();
          }}
        />
      ) : (
        <span className={styles.value} onClick={startEditing} role="button" tabIndex={0}>
          {value}
        </span>
      )}

      <button
        type="button"
        className={styles.stepButton}
        onClick={handleIncrease}
        aria-label="Tăng số khách"
      >
        +
      </button>
    </div>
  );
}
