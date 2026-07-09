import { useState } from 'react';

function toDateInputValue(date) {
  return date.toISOString().slice(0, 10);
}

const today = toDateInputValue(new Date());
const tomorrow = toDateInputValue(new Date(Date.now() + 86400000));

export default function HotelBookingPanel({ initialValues = {}, onSearch }) {
  const [checkinDate, setCheckinDate] = useState(initialValues.checkinDate || today);
  const [checkoutDate, setCheckoutDate] = useState(initialValues.checkoutDate || tomorrow);
  const [guestNum, setGuestNum] = useState(initialValues.guestNum || 2);
  const [roomNum, setRoomNum] = useState(initialValues.roomNum || 1);
  const [formError, setFormError] = useState('');

  function handleSubmit(event) {
    event.preventDefault();
    setFormError('');

    const hasOnlyOneDate = Boolean(checkinDate) !== Boolean(checkoutDate);
    if (hasOnlyOneDate) {
      setFormError('Vui lòng chọn đầy đủ ngày nhận phòng và trả phòng.');
      return;
    }

    if (checkinDate && checkinDate < today) {
      setFormError('Ngày nhận phòng không thể là ngày trong quá khứ.');
      return;
    }

    if (checkinDate && checkoutDate && checkoutDate <= checkinDate) {
      setFormError('Ngày trả phòng phải sau ngày nhận phòng.');
      return;
    }

    onSearch({ checkinDate, checkoutDate, guestNum, roomNum });
  }

  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
      <h2 className="text-lg font-semibold text-slate-900">Kiểm tra phòng trống</h2>

      <form onSubmit={handleSubmit} className="mt-4 space-y-3">
        <div>
          <label htmlFor="checkinDate" className="mb-1 block text-sm font-medium text-slate-700">
            Nhận phòng
          </label>
          <input
            id="checkinDate"
            type="date"
            min={today}
            value={checkinDate}
            onChange={(event) => setCheckinDate(event.target.value)}
            className="form-input"
          />
        </div>

        <div>
          <label htmlFor="checkoutDate" className="mb-1 block text-sm font-medium text-slate-700">
            Trả phòng
          </label>
          <input
            id="checkoutDate"
            type="date"
            min={checkinDate || today}
            value={checkoutDate}
            onChange={(event) => setCheckoutDate(event.target.value)}
            className="form-input"
          />
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label htmlFor="guestNum" className="mb-1 block text-sm font-medium text-slate-700">
              Số khách
            </label>
            <input
              id="guestNum"
              type="number"
              min={1}
              value={guestNum}
              onChange={(event) => setGuestNum(Number(event.target.value))}
              className="form-input"
            />
          </div>
          <div>
            <label htmlFor="roomNum" className="mb-1 block text-sm font-medium text-slate-700">
              Số phòng
            </label>
            <input
              id="roomNum"
              type="number"
              min={1}
              value={roomNum}
              onChange={(event) => setRoomNum(Number(event.target.value))}
              className="form-input"
            />
          </div>
        </div>

        {formError && <p className="text-xs text-red-600">{formError}</p>}

        <button type="submit" className="accent-button w-full">
          Tìm phòng trống
        </button>
      </form>
    </div>
  );
}