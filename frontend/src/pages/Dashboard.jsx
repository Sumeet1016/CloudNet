import { useEffect, useState } from 'react'
import api from '../api/axios'
import StatCard from '../components/StatCard'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts'

function formatBytes(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let i = 0
  let val = bytes
  while (val >= 1024 && i < units.length - 1) {
    val /= 1024
    i++
  }
  return `${val.toFixed(1)} ${units[i]}`
}

const ALERT_STYLES = {
  WARNING: 'bg-amber-50 border-amber-200 text-amber-800',
  CRITICAL: 'bg-red-50 border-red-200 text-red-800',
}

export default function Dashboard() {
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get('/dashboard/stats')
      .then((res) => setStats(res.data))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <p className="text-gray-500">Loading dashboard...</p>
  if (!stats) return <p className="text-gray-500">No data available yet.</p>

  const chartData = Object.entries(stats.backupsByProvider || {}).map(([name, count]) => ({ name, count }))
  const alerts = stats.quotaAlerts || []

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold text-gray-800">Dashboard</h2>

      {alerts.length > 0 && (
        <div className="space-y-2">
          {alerts.map((a) => (
            <div
              key={a.id}
              className={`flex items-start justify-between border rounded-lg px-4 py-3 text-sm ${ALERT_STYLES[a.severity] || 'bg-gray-50 border-gray-200'}`}
            >
              <div>
                <p className="font-semibold">
                  {a.severity} · {a.providerName}
                </p>
                <p className="text-xs mt-0.5">{a.message}</p>
              </div>
              <span className="text-xs opacity-70">
                {a.percentUsed.toFixed(1)}% · {formatBytes(a.usedBytes)} / {formatBytes(a.quotaBytes)}
              </span>
            </div>
          ))}
        </div>
      )}

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <StatCard label="Total Backups" value={stats.totalBackups} />
        <StatCard label="Successful" value={stats.successfulBackups} accent="text-green-600" />
        <StatCard label="Failed" value={stats.failedBackups} accent="text-red-600" />
        <StatCard label="Storage Used" value={formatBytes(stats.physicalStorageBytes ?? stats.totalStorageUsedBytes)} />
        <StatCard label="Logical Size" value={formatBytes(stats.logicalStorageBytes)} />
        <StatCard label="Dedup Savings" value={formatBytes(stats.dedupSavingsBytes)} accent="text-purple-600" />
        <StatCard label="Dedup Ratio" value={`${stats.dedupRatio ?? 1}×`} accent="text-purple-600" />
        <StatCard label="Unique Blobs" value={stats.uniqueBlobs ?? 0} />
        <StatCard label="Connected Providers" value={stats.connectedProviders} />
        <StatCard label="Active Schedules" value={stats.activeSchedules} />
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
        <h3 className="font-semibold text-gray-700 mb-4">Backups by Provider</h3>
        {chartData.length === 0 ? (
          <p className="text-sm text-gray-400">No backups yet. Run your first backup to see stats here.</p>
        ) : (
          <ResponsiveContainer width="100%" height={250}>
            <BarChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" fontSize={12} />
              <YAxis allowDecimals={false} fontSize={12} />
              <Tooltip />
              <Bar dataKey="count" fill="#3b6fe0" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        )}
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
        <h3 className="font-semibold text-gray-700 mb-4">Recent Activity</h3>
        {(!stats.recentActivity || stats.recentActivity.length === 0) ? (
          <p className="text-sm text-gray-400">No activity yet.</p>
        ) : (
          <ul className="divide-y divide-gray-100">
            {stats.recentActivity.map((log, idx) => (
              <li key={idx} className="py-2 text-sm flex justify-between">
                <span className="text-gray-700">{log.action?.replaceAll('_', ' ')}</span>
                <span className="text-gray-400">{log.details}</span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}