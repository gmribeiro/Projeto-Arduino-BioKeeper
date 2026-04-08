function MetricCard({ title, value, unit, accent }) {
  return (
    <div className={`rounded-3xl border border-slate-800 bg-slate-900/90 p-6 shadow-soft transition hover:-translate-y-1 hover:border-slate-700 ${accent}`}>
      <div className="flex items-center justify-between gap-4">
        <div>
          <p className="text-sm uppercase tracking-[0.24em] text-slate-500">{title}</p>
          <p className="mt-4 text-4xl font-semibold text-white">{typeof value === 'number' ? value.toFixed(1) : value}{unit}</p>
        </div>
      </div>
    </div>
  );
}

export default MetricCard;
