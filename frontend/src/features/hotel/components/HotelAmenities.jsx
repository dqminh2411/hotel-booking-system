export default function HotelAmenities({ amenities }) {
  return (
    <section>
      <h2 className="text-xl font-semibold text-slate-900">Tiện nghi</h2>
      {amenities && amenities.length > 0 ? (
        <ul className="mt-3 grid grid-cols-2 gap-2.5 sm:grid-cols-3">
          {amenities.map((amenity) => (
            <li
              key={amenity.id}
              className="flex items-center gap-2 rounded-md border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700"
            >
              <span className="text-blue-700" aria-hidden="true">
                ✓
              </span>
              {amenity.name}
            </li>
          ))}
        </ul>
      ) : (
        <p className="mt-3 text-sm text-slate-500">Khách sạn chưa cập nhật tiện nghi.</p>
      )}
    </section>
  );
}
