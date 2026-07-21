import { useEffect } from 'react';

// Hiển thị khi BE trả về code=409 từ POST /place-booking:
// "Bạn đang có một đơn đặt phòng tương tự trong vòng 5 phút trước"
// kèm forceToken (Helpler.generateForceToken). Nếu người dùng bấm "Xác nhận
// đặt lại", FE gửi lại đúng payload cũ kèm forceToken để BE bỏ qua bước
// kiểm tra trùng (Helpler.isDuplicateRequest so khớp forceToken).
export default function DuplicateBookingModal({ open, message, hint, loading, onConfirm, onCancel }) {
  useEffect(() => {
    if (!open) return undefined;

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [open]);

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-md rounded-xl bg-white p-6 shadow-lg">
        <div className="flex items-start gap-3">
          <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-amber-100 text-xl text-amber-700">
            ⚠️
          </span>
          <div>
            <h3 className="text-lg font-semibold text-slate-900">Yêu cầu đặt phòng trùng lặp</h3>
            <p className="mt-1 text-sm text-slate-600">
              {message || 'Bạn đang có một đơn đặt phòng tương tự trong vòng 5 phút trước.'}
            </p>
          </div>
        </div>

        <div className="mt-4 rounded-md bg-slate-50 p-3 text-sm text-slate-600">
          {hint || 'Nếu đây đúng là yêu cầu bạn muốn thực hiện, hãy xác nhận để tiếp tục đặt phòng mới.'}
        </div>

        <div className="mt-6 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <button
            type="button"
            onClick={onCancel}
            disabled={loading}
            className="rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-700 disabled:opacity-60"
          >
            Hủy, kiểm tra lại
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={loading}
            className="primary-button justify-center disabled:opacity-60"
          >
            {loading ? 'Đang xử lý...' : 'Xác nhận đặt lại'}
          </button>
        </div>
      </div>
    </div>
  );
}