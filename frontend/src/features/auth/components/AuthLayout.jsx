import BrandLogo from '../../../shared/components/BrandLogo';

const benefits = [
  'Lưu thông tin để đặt phòng nhanh hơn',
  'Quản lý mọi chuyến đi tại một nơi',
  'Nhận ưu đãi dành riêng cho thành viên',
];

export default function AuthLayout({ eyebrow, title, description, children }) {
  return (
    <div className="min-h-screen bg-slate-50 lg:grid lg:grid-cols-[minmax(360px,0.85fr)_1.15fr]">
      <aside className="relative hidden overflow-hidden bg-blue-700 px-10 py-12 text-white lg:flex lg:flex-col">
        <BrandLogo inverse />
        <div className="my-auto max-w-md">
          <p className="text-sm font-semibold uppercase tracking-wider text-blue-200">Du lịch theo cách của bạn</p>
          <h2 className="mt-4 text-4xl font-bold leading-tight">Một tài khoản, mọi hành trình.</h2>
          <p className="mt-4 text-base leading-7 text-blue-100">
            Khám phá nơi lưu trú phù hợp và quản lý chuyến đi đơn giản hơn cùng HotelHub.
          </p>
          <ul className="mt-8 space-y-4">
            {benefits.map((benefit) => (
              <li key={benefit} className="flex items-center gap-3 text-sm text-blue-50">
                <span className="grid h-6 w-6 shrink-0 place-items-center rounded-full bg-blue-600" aria-hidden="true">
                  ✓
                </span>
                {benefit}
              </li>
            ))}
          </ul>
        </div>
        <div className="absolute -bottom-24 -right-20 h-72 w-72 rounded-full border-[48px] border-blue-600 opacity-60" />
        <p className="relative text-xs text-blue-200">© 2026 HotelHub. Travel made simple.</p>
      </aside>

      <main className="flex min-h-screen items-center justify-center px-4 py-8 sm:px-6 lg:px-12">
        <div className="w-full max-w-md">
          <div className="mb-8 flex justify-center lg:hidden">
            <BrandLogo />
          </div>
          <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
            <p className="text-xs font-semibold uppercase tracking-wider text-blue-700">{eyebrow}</p>
            <h1 className="mt-2 text-2xl font-bold text-slate-900 md:text-3xl">{title}</h1>
            <p className="mt-2 text-sm leading-6 text-slate-600">{description}</p>
            <div className="mt-7">{children}</div>
          </section>
        </div>
      </main>
    </div>
  );
}
