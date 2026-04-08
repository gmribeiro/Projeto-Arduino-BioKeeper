import { useEffect, useState } from 'react';
import MetricCard from './components/MetricCard';

const metrics = [
  { title: 'Temperature', key: 'temperature', unit: '°C', accent: 'from-orange-400 to-amber-400' },
  { title: 'Humidity', key: 'humidity', unit: '%', accent: 'from-cyan-400 to-blue-500' },
  { title: 'Soil Moisture', key: 'soil_moisture', unit: '%', accent: 'from-emerald-400 to-teal-500' },
  { title: 'UV Index', key: 'uv_index', unit: '', accent: 'from-violet-400 to-fuchsia-500' }
];

function App() {
  const [data, setData] = useState(null);
  const [history, setHistory] = useState([]);
  const [status, setStatus] = useState('Loading...');

  useEffect(() => {
    async function fetchSensorData() {
      try {
        const latest = await fetch('/api/sensors/latest');
        const latestData = await latest.json();
        const historyRes = await fetch('/api/sensors/history');
        const historyData = await historyRes.json();
        setData(latestData);
        setHistory(historyData);
        setStatus('Live data updated');
      } catch (error) {
        console.error(error);
        setStatus('Unable to fetch sensor data.');
      }
    }

    fetchSensorData();
    const interval = setInterval(fetchSensorData, 20000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="min-h-screen bg-slate-950 px-6 py-8 text-slate-100">
      <div className="mx-auto max-w-7xl">
        <header className="mb-8 flex flex-col gap-4 rounded-3xl border border-slate-800 bg-slate-900/90 p-8 shadow-soft backdrop-blur-sm">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p className="text-sm uppercase tracking-[0.32em] text-slate-400">Biokeeper</p>
              <h1 className="text-4xl font-semibold text-white">Vegetable Maintenance Dashboard</h1>
            </div>
            <div className="rounded-3xl bg-slate-800/80 px-5 py-3 text-sm text-slate-300 shadow-inner">
              {status}
            </div>
          </div>
          <p className="max-w-2xl text-slate-400">Monitor temperature, humidity, soil moisture and UV cycle status in real time. Designed for ESP32-powered plant cabinets and vegetable growth systems.</p>
        </header>

        <section className="grid gap-6 xl:grid-cols-2">
          <div className="grid gap-6 sm:grid-cols-2">
            {metrics.map((metric) => (
              <MetricCard
                key={metric.key}
                title={metric.title}
                value={data ? data[metric.key] : '--'}
                unit={metric.unit}
                accent={metric.accent}
              />
            ))}
          </div>

          <div className="rounded-3xl border border-slate-800 bg-slate-900/90 p-6 shadow-soft">
            <div className="mb-4 flex items-center justify-between">
              <div>
                <h2 className="text-xl font-semibold text-white">System Status</h2>
                <p className="text-sm text-slate-400">Current actuator and environment state.</p>
              </div>
            </div>
            <div className="space-y-4">
              <div className="rounded-3xl bg-slate-950/80 p-5">
                <p className="text-sm uppercase tracking-[0.24em] text-slate-500">Fan</p>
                <p className="mt-2 text-3xl font-semibold text-emerald-400">{data ? (data.fan_power ? 'Active' : 'Idle') : '--'}</p>
              </div>
              <div className="rounded-3xl bg-slate-950/80 p-5">
                <p className="text-sm uppercase tracking-[0.24em] text-slate-500">UV Light</p>
                <p className="mt-2 text-3xl font-semibold text-violet-400">{data ? (data.uv_active ? 'On' : 'Off') : '--'}</p>
              </div>
              <div className="rounded-3xl bg-slate-950/80 p-5">
                <p className="text-sm uppercase tracking-[0.24em] text-slate-500">Last Update</p>
                <p className="mt-2 text-xl font-medium text-slate-200">{data ? new Date(data.reading_time).toLocaleString() : '--'}</p>
              </div>
            </div>
          </div>
        </section>

        <section className="mt-8 rounded-3xl border border-slate-800 bg-slate-900/90 p-6 shadow-soft">
          <div className="mb-6 flex items-center justify-between">
            <div>
              <h2 className="text-xl font-semibold text-white">Recent Telemetry</h2>
              <p className="text-sm text-slate-400">Last 24 readings from the ESP32 sensors.</p>
            </div>
          </div>
          <div className="grid gap-4 md:grid-cols-2">
            <article className="rounded-3xl bg-slate-950/80 p-5">
              <h3 className="text-sm font-semibold uppercase tracking-[0.24em] text-slate-500">Temperature trend</h3>
              <p className="mt-4 text-3xl font-semibold text-orange-300">{history.length ? `${history[history.length - 1].temperature.toFixed(1)}°C` : '--'}</p>
            </article>
            <article className="rounded-3xl bg-slate-950/80 p-5">
              <h3 className="text-sm font-semibold uppercase tracking-[0.24em] text-slate-500">Humidity trend</h3>
              <p className="mt-4 text-3xl font-semibold text-cyan-300">{history.length ? `${history[history.length - 1].humidity.toFixed(1)}%` : '--'}</p>
            </article>
          </div>
          <div className="mt-6 overflow-hidden rounded-3xl border border-slate-800 bg-slate-950/80">
            <table className="min-w-full border-separate border-spacing-0 text-left text-sm text-slate-300">
              <thead className="bg-slate-900/90 text-slate-400">
                <tr>
                  <th className="px-5 py-4">Time</th>
                  <th className="px-5 py-4">Temp</th>
                  <th className="px-5 py-4">Humidity</th>
                  <th className="px-5 py-4">Soil</th>
                  <th className="px-5 py-4">UV</th>
                </tr>
              </thead>
              <tbody>
                {history.slice(-8).reverse().map((row, index) => (
                  <tr key={index} className="border-t border-slate-800/80">
                    <td className="px-5 py-4 text-slate-300">{new Date(row.reading_time).toLocaleTimeString()}</td>
                    <td className="px-5 py-4">{row.temperature.toFixed(1)}°C</td>
                    <td className="px-5 py-4">{row.humidity.toFixed(1)}%</td>
                    <td className="px-5 py-4">{row.soil_moisture.toFixed(1)}%</td>
                    <td className="px-5 py-4">{row.uv_index.toFixed(1)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </div>
  );
}

export default App;
