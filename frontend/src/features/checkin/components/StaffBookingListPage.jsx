import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import ErrorState from '../../../shared/components/ErrorState';
import EmptyState from '../../../shared/components/EmptyState';
import PublicHeader from '../../../shared/components/PublicHeader';
import { formatCurrency, formatDateVi } from '../../../shared/utils/formatters';

const PAGE_SIZE = 10;
const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/**
 * Trang danh sách booking dùng chung cho các màn hình nhân viên (check-in hôm
 * nay / đang lưu trú chờ check-out...). Chỉ khác nhau ở nguồn dữ liệu
 * (fetchList) và vài đoạn text/route - phần UI (bảng, phân trang, đổi khách
 * sạn, các trạng thái loading/error/empty) được tái sử dụng nguyên vẹn.
 *
 * @param {string} hotelId - hotelId hiện tại lấy từ route (useParams ở trang cha).
 * @param {(hotelId: string, page: number, size: number) => Promise<object>} fetchList
 * @param {string} eyebrow - dòng nhỏ phía trên tiêu đề, vd "Nhân viên · Check-in hôm nay".
 * @param {string} title - tiêu đề chính của trang.
 * @param {boolean} [showTodayDate] - có hiển thị ngày hôm nay dưới tiêu đề không.
 * @param {string} idlePlaceholder - hướng dẫn khi chưa có hotelId.
 * @param {string} emptyTitle - tiêu đề khi danh sách rỗng.
 * @param {string} emptyDescription - mô tả khi danh sách rỗng.
 * @param {string} defaultErrorMessage - thông báo lỗi mặc định khi API fail.
 * @param {(trimmedHotelId: string) => string} buildChangeHotelRoute - route điều
 *   hướng tới khi nhân viên đổi hotelId ở ô input (mỗi trang có 1 route riêng).
 * @param {string} badgeClassName - class Tailwind cho badge trạng thái booking.
 * @param {(booking: object) => string} [buildRowLink] - route khi bấm vào 1
 *   booking, mặc định trỏ tới trang chi tiết booking.
 */
export default function StaffBookingListPage({
  hotelId,
  fetchList,
  eyebrow,
  title,
  showTodayDate = false,
  idlePlaceholder,
  emptyTitle,
  emptyDescription,
  defaultErrorMessage,
  buildChangeHotelRoute,
  badgeClassName = 'inline-flex items-center rounded-full border border-amber-200 bg-amber-50 px-2.5 py-1 text-xs font-semibold text-amber-700',
  buildRowLink = (booking) => `/bookings/${booking.bookingId}`,
}) {
  const navigate = useNavigate();

  const [page, setPage] = useState(0);
  const [pageData, setPageData] = useState({ content: [], totalPages: 0, totalElements: 0, last: true });
  const [status, setStatus] = useState(hotelId ? 'loading' : 'idle');
  const [error, setError] = useState('');
  const [hotelIdInput, setHotelIdInput] = useState(hotelId || '');
  const [hotelIdInputError, setHotelIdInputError] = useState('');

  const loadPage = useCallback(
    async (targetPage) => {
      if (!hotelId) {
        setStatus('idle');
        return;
      }
      setStatus('loading');
      setError('');

      try {
        const data = await fetchList(hotelId, targetPage, PAGE_SIZE);
        setPageData({
          content: data?.content || [],
          totalPages: data?.totalPages ?? 0,
          totalElements: data?.totalElements ?? 0,
          last: data?.last ?? true,
        });
        setStatus('success');
      } catch (err) {
        setError(err.response?.data?.message || err.message || defaultErrorMessage);
        setStatus('error');
      }
    },
    [hotelId, fetchList, defaultErrorMessage],
  );

  useEffect(() => {
    setPage(0);
    loadPage(0);
  }, [loadPage]);

  function goToPage(nextPage) {
    setPage(nextPage);
    loadPage(nextPage);
  }

  function handleChangeHotel(event) {
    event.preventDefault();
    const trimmed = hotelIdInput.trim();
    if (!trimmed || trimmed === hotelId) return;

    // hotelId sai định dạng UUID sẽ khiến BE ném lỗi 500 chung chung (Spring
    // không có handler riêng cho MethodArgumentTypeMismatchException ở
    // @PathVariable UUID) - chặn sớm ở FE để hiện lỗi rõ ràng hơn.
    if (!UUID_REGEX.test(trimmed)) {
      setHotelIdInputError('Mã khách sạn không đúng định dạng UUID.');
      return;
    }

    setHotelIdInputError('');
    navigate(buildChangeHotelRoute(trimmed));
  }

  return (
    <div className="min-h-screen bg-slate-50">
      <PublicHeader />

      <main className="mx-auto max-w-5xl px-4 py-6 md:px-6 lg:px-8">
        <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-sm font-medium text-blue-700">{eyebrow}</p>
            <h1 className="text-2xl font-bold text-slate-900">{title}</h1>
            {showTodayDate && <p className="mt-1 text-sm text-slate-600">{formatDateVi(new Date())}</p>}
          </div>

          <form onSubmit={handleChangeHotel} className="flex items-start gap-2">
            <div>
              <input
                value={hotelIdInput}
                onChange={(event) => {
                  setHotelIdInput(event.target.value);
                  setHotelIdInputError('');
                }}
                placeholder="Mã khách sạn (hotelId)"
                className="form-input w-64 text-sm"
              />
              {hotelIdInputError && <p className="mt-1 text-xs text-red-600">{hotelIdInputError}</p>}
            </div>
            <button type="submit" className="secondary-button whitespace-nowrap">
              Xem
            </button>
          </form>
        </div>

        {status === 'idle' && <EmptyState title="Chưa chọn khách sạn" description={idlePlaceholder} />}

        {status === 'loading' && (
          <div className="rounded-lg border border-slate-200 bg-white p-8 text-center text-sm text-slate-600">
            Đang tải danh sách...
          </div>
        )}

        {status === 'error' && <ErrorState message={error} onRetry={() => loadPage(page)} />}

        {status === 'success' && pageData.content.length === 0 && (
          <EmptyState title={emptyTitle} description={emptyDescription} />
        )}

        {status === 'success' && pageData.content.length > 0 && (
          <>
            <div className="overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm">
              {pageData.content.map((booking) => (
                <Link
                  key={booking.bookingId}
                  to={buildRowLink(booking)}
                  className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 p-4 text-sm last:border-b-0 hover:bg-slate-50"
                >
                  <div className="min-w-[200px]">
                    <p className="font-semibold text-slate-900">{booking.customerName || 'Đang cập nhật'}</p>
                    <p className="text-slate-500">{booking.customerEmail}</p>
                  </div>

                  <div className="min-w-[160px]">
                    <p className="text-slate-500">Nhận phòng · Trả phòng</p>
                    <p className="font-medium text-slate-800">
                      {formatDateVi(booking.checkinDate)} → {formatDateVi(booking.checkoutDate)}
                    </p>
                  </div>

                  <div className="min-w-[80px]">
                    <p className="text-slate-500">Số khách</p>
                    <p className="font-medium text-slate-800">{booking.numAdults ?? 0}</p>
                  </div>

                  <div className="min-w-[120px]">
                    <p className="text-slate-500">Tổng tiền</p>
                    <p className="font-medium text-slate-800">{formatCurrency(booking.totalAmount)}</p>
                  </div>

                  <span className={badgeClassName}>{booking.status}</span>
                </Link>
              ))}
            </div>

            <div className="mt-4 flex items-center justify-between text-sm text-slate-600">
              <span>
                Trang {pageData.totalPages === 0 ? 0 : page + 1}/{pageData.totalPages} · {pageData.totalElements}{' '}
                booking
              </span>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => goToPage(page - 1)}
                  disabled={page === 0}
                  className="rounded-md border border-slate-300 bg-white px-3 py-1.5 font-semibold text-slate-700 disabled:opacity-50"
                >
                  ← Trước
                </button>
                <button
                  type="button"
                  onClick={() => goToPage(page + 1)}
                  disabled={pageData.last}
                  className="rounded-md border border-slate-300 bg-white px-3 py-1.5 font-semibold text-slate-700 disabled:opacity-50"
                >
                  Sau →
                </button>
              </div>
            </div>
          </>
        )}
      </main>
    </div>
  );
}