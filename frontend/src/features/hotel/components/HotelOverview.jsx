export default function HotelOverview({ description }) {
  return (
    <section>
      <h2 className="text-xl font-semibold text-slate-900">Giới thiệu</h2>
      <p className="mt-3 whitespace-pre-line text-sm leading-6 text-slate-700">
        {description || 'Khách sạn chưa cập nhật mô tả chi tiết.'}
      </p>
    </section>
  );
}
