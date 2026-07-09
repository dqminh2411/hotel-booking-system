export default function HotelDetailSkeleton() {
  return (
    <div className="animate-pulse space-y-6">
      <div className="space-y-2">
        <div className="h-8 w-2/3 rounded bg-slate-200" />
        <div className="h-4 w-1/3 rounded bg-slate-200" />
      </div>

      <div className="grid grid-cols-4 gap-2 md:h-80">
        <div className="col-span-4 h-56 rounded-lg bg-slate-200 md:col-span-2 md:row-span-2 md:h-full" />
        <div className="hidden h-full rounded-lg bg-slate-200 md:block" />
        <div className="hidden h-full rounded-lg bg-slate-200 md:block" />
        <div className="hidden h-full rounded-lg bg-slate-200 md:block" />
        <div className="hidden h-full rounded-lg bg-slate-200 md:block" />
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
        <div className="space-y-4">
          <div className="h-4 w-full rounded bg-slate-200" />
          <div className="h-4 w-5/6 rounded bg-slate-200" />
          <div className="h-4 w-2/3 rounded bg-slate-200" />
        </div>
        <div className="h-64 rounded-lg bg-slate-200" />
      </div>

      <div className="h-40 rounded-lg bg-slate-200" />
    </div>
  );
}
