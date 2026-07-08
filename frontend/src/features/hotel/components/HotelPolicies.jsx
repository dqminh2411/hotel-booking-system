import { getPolicyTypeLabel } from '../utils/hotelFormatters';

export default function HotelPolicies({ policies }) {
  return (
    <section>
      <h2 className="text-xl font-semibold text-slate-900">Chính sách khách sạn</h2>
      {policies && policies.length > 0 ? (
        <div className="mt-3 divide-y divide-slate-200 rounded-lg border border-slate-200 bg-white">
          {policies.map((policy) => (
            <div key={policy.id} className="p-4">
              <h3 className="text-sm font-semibold text-slate-900">{getPolicyTypeLabel(policy.type)}</h3>
              <p className="mt-1 text-sm text-slate-600">{policy.description}</p>
            </div>
          ))}
        </div>
      ) : (
        <p className="mt-3 text-sm text-slate-500">Khách sạn chưa cập nhật chính sách.</p>
      )}
    </section>
  );
}
